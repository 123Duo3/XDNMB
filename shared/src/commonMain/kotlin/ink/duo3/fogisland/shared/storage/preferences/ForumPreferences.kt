package ink.duo3.fogisland.shared.storage.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import ink.duo3.fogisland.shared.model.ForumTimeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ForumPreferences(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val USE_UTC_PLUS_8_TIME = booleanPreferencesKey("use_utc_plus_8_time")
    }

    val timeSettingsFlow: Flow<ForumTimeSettings> = dataStore.data.map { preferences ->
        ForumTimeSettings(
            useUtcPlus8Time = preferences[USE_UTC_PLUS_8_TIME] ?: true
        )
    }

    suspend fun updateUseUtcPlus8Time(useUtcPlus8Time: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_UTC_PLUS_8_TIME] = useUtcPlus8Time
        }
    }
}
