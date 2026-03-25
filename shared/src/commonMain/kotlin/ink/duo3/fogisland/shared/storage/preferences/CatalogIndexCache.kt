package ink.duo3.fogisland.shared.storage.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ink.duo3.fogisland.shared.model.CatalogSource
import ink.duo3.fogisland.shared.model.CatalogType
import ink.duo3.fogisland.shared.model.ForumGroup
import ink.duo3.fogisland.shared.model.SiteNotice
import ink.duo3.fogisland.shared.model.Timeline
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CatalogIndexCache(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val CACHED_CATALOG_INDEX = stringPreferencesKey("cached_catalog_index")
        private val LAST_SELECTED_SOURCE = stringPreferencesKey("last_selected_source")
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun read(): CatalogIndexSnapshot {
        val preferences = dataStore.data.firstOrNull() ?: return CatalogIndexSnapshot()
        val raw = preferences[CACHED_CATALOG_INDEX]
        if (raw.isNullOrBlank()) {
            return CatalogIndexSnapshot()
        }

        return runCatching {
            json.decodeFromString<CatalogIndexSnapshot>(raw)
        }.getOrDefault(CatalogIndexSnapshot())
    }

    suspend fun readLastSelectedSource(): PersistedCatalogSource? {
        val raw = dataStore.data.firstOrNull()?.get(LAST_SELECTED_SOURCE)
        if (raw.isNullOrBlank()) {
            return null
        }

        return runCatching {
            json.decodeFromString<PersistedCatalogSource>(raw)
        }.getOrNull()
    }

    suspend fun writeLastSelectedSource(source: CatalogSource) {
        dataStore.edit { preferences ->
            preferences[LAST_SELECTED_SOURCE] = json.encodeToString(
                PersistedCatalogSource(
                    type = source.type,
                    id = source.id
                )
            )
        }
    }

    suspend fun updateForumGroups(forumGroups: List<ForumGroup>) {
        dataStore.edit { preferences ->
            val snapshot = decodeSnapshot(preferences)
            preferences[CACHED_CATALOG_INDEX] = json.encodeToString(
                snapshot.copy(forumGroups = forumGroups)
            )
        }
    }

    suspend fun updateTimelines(timelines: List<Timeline>) {
        dataStore.edit { preferences ->
            val snapshot = decodeSnapshot(preferences)
            preferences[CACHED_CATALOG_INDEX] = json.encodeToString(
                snapshot.copy(timelines = timelines)
            )
        }
    }

    suspend fun updateSiteNotice(siteNotice: SiteNotice?) {
        dataStore.edit { preferences ->
            val snapshot = decodeSnapshot(preferences)
            preferences[CACHED_CATALOG_INDEX] = json.encodeToString(
                snapshot.copy(siteNotice = siteNotice)
            )
        }
    }

    private fun decodeSnapshot(preferences: Preferences): CatalogIndexSnapshot {
        val raw = preferences[CACHED_CATALOG_INDEX]
        if (raw.isNullOrBlank()) {
            return CatalogIndexSnapshot()
        }

        return runCatching {
            json.decodeFromString<CatalogIndexSnapshot>(raw)
        }.getOrDefault(CatalogIndexSnapshot())
    }
}

@Serializable
data class CatalogIndexSnapshot(
    val forumGroups: List<ForumGroup> = emptyList(),
    val timelines: List<Timeline> = emptyList(),
    val siteNotice: SiteNotice? = null
)

@Serializable
data class PersistedCatalogSource(
    val type: CatalogType,
    val id: Long
)
