package ink.duo3.fogisland.shared.storage.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    }

    val cookiesFlow: Flow<List<CookieProfile>> = dataStore.data.map { preferences ->
        val raw = preferences[ALL_COOKIES]
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                json.decodeFromString<List<CookieProfile>>(raw)
            }.getOrDefault(emptyList())
        }
    }

    val activeRequestCookieIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[ACTIVE_REQUEST_COOKIE]
    }

    val activePostCookieIdFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[ACTIVE_POST_COOKIE]
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

    suspend fun saveCookie(
        label: String,
        value: String,
        id: String = kotlin.time.Clock.System.now().toEpochMilliseconds().toString()
    ) {
        dataStore.edit { preferences ->
            val existing = decodeCookies(preferences).filterNot { it.id == id }
            val updated = existing + CookieProfile(
                id = id,
                label = label,
                value = value
            )
            preferences[ALL_COOKIES] = json.encodeToString(updated)

            if (preferences[ACTIVE_REQUEST_COOKIE] == null) {
                preferences[ACTIVE_REQUEST_COOKIE] = id
            }
            if (preferences[ACTIVE_POST_COOKIE] == null) {
                preferences[ACTIVE_POST_COOKIE] = id
            }
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
        return activeRequestCookieFlow.firstOrNull()?.value
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let(::normalizeCookieHeader)
    }

    suspend fun getActivePostCookieHeader(): String? {
        return activePostCookieFlow.firstOrNull()?.value
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let(::normalizeCookieHeader)
    }

    private fun decodeCookies(preferences: Preferences): List<CookieProfile> {
        val raw = preferences[ALL_COOKIES]
        if (raw.isNullOrBlank()) {
            return emptyList()
        }
        return runCatching {
            json.decodeFromString<List<CookieProfile>>(raw)
        }.getOrDefault(emptyList())
    }

    private fun normalizeCookieHeader(rawValue: String): String {
        return if (rawValue.contains("=")) rawValue else "userhash=$rawValue"
    }
}

@Serializable
data class CookieProfile(
    val id: String,
    val label: String,
    val value: String
)
