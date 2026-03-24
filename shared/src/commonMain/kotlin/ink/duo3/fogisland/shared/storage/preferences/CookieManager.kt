package ink.duo3.fogisland.shared.storage.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ink.duo3.fogisland.shared.model.CookieCollection
import ink.duo3.fogisland.shared.model.CookieImportPayload
import ink.duo3.fogisland.shared.model.CookieProfile
import ink.duo3.fogisland.shared.model.MAX_COOKIE_PROFILE_COUNT
import ink.duo3.fogisland.shared.util.parseNmbCookieImportPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

class CookieManager(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val ACTIVE_REQUEST_COOKIE = stringPreferencesKey("active_request_cookie")
        private val ACTIVE_POST_COOKIE = stringPreferencesKey("active_post_cookie")
        private val ALL_COOKIES = stringPreferencesKey("all_cookies")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val cookiesFlow: Flow<List<CookieProfile>> = dataStore.data.map { preferences ->
        decodeCookies(preferences)
    }

    val activeRequestCookieIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[ACTIVE_REQUEST_COOKIE]
    }

    val activePostCookieIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[ACTIVE_POST_COOKIE]
    }

    val cookieCollectionFlow: Flow<CookieCollection> = combine(
        cookiesFlow,
        activeRequestCookieIdFlow,
        activePostCookieIdFlow
    ) { cookies, activeRequestCookieId, activePostCookieId ->
        CookieCollection(
            cookies = cookies,
            activeRequestCookieId = activeRequestCookieId,
            activePostCookieId = activePostCookieId
        )
    }

    val activeRequestCookieFlow: Flow<CookieProfile?> = combine(
        cookiesFlow,
        activeRequestCookieIdFlow
    ) { cookies, activeId ->
        cookies.firstOrNull { it.id == activeId }
    }

    val activePostCookieFlow: Flow<CookieProfile?> = combine(
        cookiesFlow,
        activePostCookieIdFlow
    ) { cookies, activeId ->
        cookies.firstOrNull { it.id == activeId }
    }

    suspend fun importCookie(
        rawPayload: String,
        remark: String? = null
    ): CookieProfile {
        val payload = parseNmbCookieImportPayload(rawPayload)
        return upsertCookie(
            payload = payload,
            remark = remark
        )
    }

    suspend fun saveCookie(
        cookieValue: String,
        accountName: String? = null,
        remark: String = "",
        id: String? = null
    ): CookieProfile {
        return upsertCookie(
            payload = CookieImportPayload(
                cookieValue = cookieValue,
                accountName = accountName
            ),
            remark = remark,
            preferredId = id
        )
    }

    suspend fun updateCookieRemark(id: String, remark: String) {
        dataStore.edit { preferences ->
            val cookies = decodeCookies(preferences).map { cookie ->
                if (cookie.id != id) {
                    cookie
                } else {
                    cookie.copy(
                        remark = remark.trim(),
                        updatedAt = currentTimeMillis()
                    )
                }
            }
            preferences[ALL_COOKIES] = encodeCookies(cookies)
        }
    }

    suspend fun deleteCookie(id: String) {
        dataStore.edit { preferences ->
            val cookies = decodeCookies(preferences).filterNot { it.id == id }
            preferences[ALL_COOKIES] = encodeCookies(cookies)

            if (preferences[ACTIVE_REQUEST_COOKIE] == id) {
                preferences.remove(ACTIVE_REQUEST_COOKIE)
            }
            if (preferences[ACTIVE_POST_COOKIE] == id) {
                preferences.remove(ACTIVE_POST_COOKIE)
            }
        }
    }

    suspend fun moveCookie(id: String, toIndex: Int) {
        dataStore.edit { preferences ->
            val cookies = decodeCookies(preferences).toMutableList()
            val currentIndex = cookies.indexOfFirst { it.id == id }
            if (currentIndex == -1) {
                return@edit
            }

            val targetIndex = toIndex.coerceIn(0, cookies.lastIndex)
            if (currentIndex == targetIndex) {
                return@edit
            }

            val movedCookie = cookies.removeAt(currentIndex)
            cookies.add(targetIndex, movedCookie)
            preferences[ALL_COOKIES] = encodeCookies(
                cookies.mapIndexed { index, cookie ->
                    if (cookie.id == id) {
                        cookie.copy(
                            sortOrder = index,
                            updatedAt = currentTimeMillis()
                        )
                    } else {
                        cookie.copy(sortOrder = index)
                    }
                }
            )
        }
    }

    suspend fun setActiveRequestCookie(id: String?) {
        dataStore.edit { preferences ->
            if (id == null) {
                preferences.remove(ACTIVE_REQUEST_COOKIE)
            } else {
                preferences[ACTIVE_REQUEST_COOKIE] = id
            }
        }
    }

    suspend fun setActivePostCookie(id: String?) {
        dataStore.edit { preferences ->
            if (id == null) {
                preferences.remove(ACTIVE_POST_COOKIE)
            } else {
                preferences[ACTIVE_POST_COOKIE] = id
            }
        }
    }

    suspend fun getActiveRequestCookieHeader(): String? {
        return activeRequestCookieFlow.firstOrNull()?.cookieValue
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let(::normalizeCookieHeader)
    }

    suspend fun getActivePostCookieHeader(): String? {
        return activePostCookieFlow.firstOrNull()?.cookieValue
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let(::normalizeCookieHeader)
    }

    private suspend fun upsertCookie(
        payload: CookieImportPayload,
        remark: String?,
        preferredId: String? = null
    ): CookieProfile {
        var savedCookie: CookieProfile? = null

        dataStore.edit { preferences ->
            val cookies = decodeCookies(preferences)
            val now = currentTimeMillis()
            val normalizedPayload = payload.normalize()
            val matchedCookie = findExistingCookie(
                cookies = cookies,
                payload = normalizedPayload,
                preferredId = preferredId
            )

            if (matchedCookie == null && cookies.size >= MAX_COOKIE_PROFILE_COUNT) {
                throw CookieLimitExceededException(MAX_COOKIE_PROFILE_COUNT)
            }

            val updatedCookie = buildUpdatedCookie(
                existing = matchedCookie,
                payload = normalizedPayload,
                remark = remark,
                preferredId = preferredId,
                updatedAt = now,
                defaultSortOrder = cookies.size
            )
            val updatedCookies = (cookies.filterNot { it.id == updatedCookie.id } + updatedCookie)
                .sortedBy { it.sortOrder }

            preferences[ALL_COOKIES] = encodeCookies(updatedCookies)

            if (preferences[ACTIVE_REQUEST_COOKIE] == null) {
                preferences[ACTIVE_REQUEST_COOKIE] = updatedCookie.id
            }
            if (preferences[ACTIVE_POST_COOKIE] == null) {
                preferences[ACTIVE_POST_COOKIE] = updatedCookie.id
            }

            savedCookie = updatedCookie
        }

        return checkNotNull(savedCookie)
    }

    private fun buildUpdatedCookie(
        existing: CookieProfile?,
        payload: CookieImportPayload,
        remark: String?,
        preferredId: String?,
        updatedAt: Long,
        defaultSortOrder: Int
    ): CookieProfile {
        val createdAt = existing?.createdAt ?: updatedAt
        return CookieProfile(
            id = existing?.id ?: preferredId ?: updatedAt.toString(),
            cookieValue = payload.cookieValue,
            accountName = payload.accountName ?: existing?.accountName,
            remark = remark?.trim() ?: existing?.remark.orEmpty(),
            sortOrder = existing?.sortOrder ?: defaultSortOrder,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun findExistingCookie(
        cookies: List<CookieProfile>,
        payload: CookieImportPayload,
        preferredId: String?
    ): CookieProfile? {
        val normalizedCookieKey = cookieIdentityKey(payload.cookieValue)
        return when {
            preferredId != null -> cookies.firstOrNull { it.id == preferredId }
                ?: cookies.firstOrNull { cookieIdentityKey(it.cookieValue) == normalizedCookieKey }
            else -> cookies.firstOrNull { cookieIdentityKey(it.cookieValue) == normalizedCookieKey }
        }
    }

    private fun decodeCookies(preferences: Preferences): List<CookieProfile> {
        val raw = preferences[ALL_COOKIES]
        if (raw.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching {
            val parsedArray = json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()
            parsedArray.mapIndexedNotNull(::decodeCookieProfile)
                .sortedBy { it.sortOrder }
                .mapIndexed { index, cookie -> cookie.copy(sortOrder = index) }
        }.getOrDefault(emptyList())
    }

    private fun decodeCookieProfile(index: Int, element: JsonElement): CookieProfile? {
        val jsonObject = element as? JsonObject ?: return null
        val cookieValue = jsonObject["cookieValue"].stringOrNull()
            ?: jsonObject["value"].stringOrNull()
            ?: return null

        val now = currentTimeMillis()
        return CookieProfile(
            id = jsonObject["id"].stringOrNull() ?: "${now}_$index",
            cookieValue = cookieValue.trim(),
            accountName = jsonObject["accountName"].stringOrNull()
                ?: jsonObject["name"].stringOrNull(),
            remark = (jsonObject["remark"].stringOrNull()
                ?: jsonObject["label"].stringOrNull()).orEmpty().trim(),
            sortOrder = jsonObject["sortOrder"].intOrNull() ?: index,
            createdAt = jsonObject["createdAt"].longOrNull() ?: now,
            updatedAt = jsonObject["updatedAt"].longOrNull() ?: now
        )
    }

    private fun encodeCookies(cookies: List<CookieProfile>): String {
        return json.encodeToString(
            cookies.sortedBy { it.sortOrder }.mapIndexed { index, cookie ->
                cookie.copy(sortOrder = index)
            }
        )
    }

    private fun normalizeCookieHeader(rawValue: String): String {
        return if (rawValue.contains("=")) rawValue else "userhash=$rawValue"
    }

    private fun cookieIdentityKey(rawValue: String): String {
        return normalizeCookieHeader(rawValue.trim())
    }

    private fun CookieImportPayload.normalize(): CookieImportPayload {
        return CookieImportPayload(
            cookieValue = cookieValue.trim(),
            accountName = accountName?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    private fun JsonElement?.stringOrNull(): String? {
        return (this as? JsonPrimitive)?.contentOrNull
    }

    private fun JsonElement?.intOrNull(): Int? {
        return (this as? JsonPrimitive)?.intOrNull
    }

    private fun JsonElement?.longOrNull(): Long? {
        return (this as? JsonPrimitive)?.longOrNull
    }

    private fun currentTimeMillis(): Long {
        return kotlin.time.Clock.System.now().toEpochMilliseconds()
    }
}

class CookieLimitExceededException(
    maxCookieCount: Int
) : IllegalStateException("最多只能保存 $maxCookieCount 块饼干")
