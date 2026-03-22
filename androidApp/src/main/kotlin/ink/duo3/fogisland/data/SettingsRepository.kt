package ink.duo3.fogisland.data

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

data class ThemeSettings(
    val followSystemAppearance: Boolean = true,
    val useDarkMode: Boolean = false,
    val useMonet: Boolean = true,
    val monetSeed: Int = 0
)

val LocalThemeSettings = staticCompositionLocalOf { ThemeSettings() }

val FOLLOW_SYSTEM_APPEARANCE = booleanPreferencesKey("follow_system_appearance")
val USE_DARK_MODE = booleanPreferencesKey("use_dark_mode")
val USE_MONET = booleanPreferencesKey("use_monet")
val MONET_SEED = intPreferencesKey("monet_seed")

val Context.themeSettingsFlow: Flow<ThemeSettings>
    get() = dataStore.data.map { preferences ->
        val followSystem = preferences[FOLLOW_SYSTEM_APPEARANCE] ?: true
        val useDark = preferences[USE_DARK_MODE] ?: false
        val useMonetVal = preferences[USE_MONET] ?: false
        val monetSeedVal = preferences[MONET_SEED] ?: 0
        ThemeSettings(followSystem, useDark, useMonetVal, monetSeedVal)
    }

suspend fun Context.updateFollowSystemAppearance(follow: Boolean) {
    dataStore.edit { preferences ->
        preferences[FOLLOW_SYSTEM_APPEARANCE] = follow
    }
}

suspend fun Context.updateUseDarkMode(useDark: Boolean) {
    dataStore.edit { preferences ->
        preferences[USE_DARK_MODE] = useDark
    }
}

suspend fun Context.updateUseMonet(useMonet: Boolean) {
    dataStore.edit { preferences ->
        preferences[USE_MONET] = useMonet
    }
}

suspend fun Context.updateMonetSeed(seed: Int) {
    dataStore.edit { preferences ->
        preferences[MONET_SEED] = seed
    }
}
