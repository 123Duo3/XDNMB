package ink.duo3.fogisland.shared.storage.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ink.duo3.fogisland.shared.model.ForumTimeSettings
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

private const val SUBSCRIPTION_UUID_LENGTH = 12
private const val MAX_RECENT_SEARCHES = 8
private const val SUBSCRIPTION_UUID_ALLOWED_CHARS =
    "0123456789abcdefghijklmnopqrstuvwxyz"

fun generateSubscriptionUuid(): String {
    return buildString {
        repeat(SUBSCRIPTION_UUID_LENGTH) {
            append(
                SUBSCRIPTION_UUID_ALLOWED_CHARS[
                    Random.nextInt(SUBSCRIPTION_UUID_ALLOWED_CHARS.length)
                ]
            )
        }
    }
}

fun normalizeSubscriptionUuidInput(uuid: String): String {
    return uuid.trim().lowercase()
}

fun isSubscriptionUuidFormatValid(uuid: String): Boolean {
    return uuid.isNotEmpty() && uuid.all { it.isSubscriptionUuidChar() }
}

fun normalizeSubscriptionUuid(uuid: String?): String? {
    val normalizedUuid = uuid
        ?.let(::normalizeSubscriptionUuidInput)
        ?.takeIf { it.isNotEmpty() }
        ?: return null
    return normalizedUuid.takeIf(::isSubscriptionUuidFormatValid)
}

private fun Char.isSubscriptionUuidChar(): Boolean {
    return this in 'a'..'z' || isDigit() || this == '-'
}

class ForumPreferences(
    private val dataStore: DataStore<Preferences>
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    companion object {
        private val USE_UTC_PLUS_8_TIME = booleanPreferencesKey("use_utc_plus_8_time")
        private val USE_PRECISE_TIME = booleanPreferencesKey("use_precise_time")
        private val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        private val SUBSCRIPTION_UUID = stringPreferencesKey("subscription_uuid")
        private val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
    }

    val timeSettingsFlow: Flow<ForumTimeSettings> = dataStore.data.map { preferences ->
        ForumTimeSettings(
            useUtcPlus8Time = preferences[USE_UTC_PLUS_8_TIME] ?: true,
            usePreciseTime = preferences[USE_PRECISE_TIME] ?: false,
            showSeconds = preferences[SHOW_SECONDS] ?: false
        )
    }

    val subscriptionUuidFlow: Flow<String?> = dataStore.data.map { preferences ->
        normalizeSubscriptionUuid(preferences[SUBSCRIPTION_UUID])
    }

    val recentSearchesFlow: Flow<List<String>> = dataStore.data.map { preferences ->
        decodeRecentSearches(preferences[RECENT_SEARCHES])
    }

    suspend fun updateUseUtcPlus8Time(useUtcPlus8Time: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_UTC_PLUS_8_TIME] = useUtcPlus8Time
        }
    }

    suspend fun updateUsePreciseTime(usePreciseTime: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_PRECISE_TIME] = usePreciseTime
        }
    }

    suspend fun updateShowSeconds(showSeconds: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_SECONDS] = showSeconds
        }
    }

    suspend fun getOrCreateSubscriptionUuid(): String {
        val cachedUuid = normalizeSubscriptionUuid(
            dataStore.data.firstOrNull()?.get(SUBSCRIPTION_UUID)
        )
        if (cachedUuid != null) {
            return cachedUuid
        }

        val generatedUuid = generateSubscriptionUuid()
        dataStore.edit { preferences ->
            preferences[SUBSCRIPTION_UUID] = normalizeSubscriptionUuid(
                preferences[SUBSCRIPTION_UUID]
            ) ?: generatedUuid
        }

        return normalizeSubscriptionUuid(
            dataStore.data.firstOrNull()?.get(SUBSCRIPTION_UUID)
        ) ?: generatedUuid
    }

    suspend fun updateSubscriptionUuid(uuid: String) {
        val normalizedUuid = normalizeSubscriptionUuid(uuid)
            ?: throw IllegalArgumentException("订阅 ID 格式无效")
        dataStore.edit { preferences ->
            preferences[SUBSCRIPTION_UUID] = normalizedUuid
        }
    }

    suspend fun recordRecentSearch(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            return
        }

        dataStore.edit { preferences ->
            val current = decodeRecentSearches(preferences[RECENT_SEARCHES])
            val updated = buildList {
                add(normalizedQuery)
                addAll(current.filterNot { it == normalizedQuery })
            }.take(MAX_RECENT_SEARCHES)
            preferences[RECENT_SEARCHES] = json.encodeToString(updated)
        }
    }

    suspend fun clearRecentSearches() {
        dataStore.edit { preferences ->
            preferences.remove(RECENT_SEARCHES)
        }
    }

    private fun decodeRecentSearches(value: String?): List<String> {
        if (value.isNullOrBlank()) {
            return emptyList()
        }
        return runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), value)
        }.getOrDefault(emptyList())
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_RECENT_SEARCHES)
    }
}
