package ink.duo3.fogisland.shared.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import ink.duo3.fogisland.shared.network.api.NmbApiClient
import ink.duo3.fogisland.shared.storage.appleApplicationSupportFilePath
import ink.duo3.fogisland.shared.storage.db.DatabaseFactory
import ink.duo3.fogisland.shared.storage.db.createDatabase
import ink.duo3.fogisland.shared.storage.preferences.CatalogIndexCache
import ink.duo3.fogisland.shared.storage.preferences.CookieManager
import ink.duo3.fogisland.shared.storage.preferences.ForumPreferences
import okio.Path.Companion.toPath

/**
 * Single Kotlin entry point for Apple platforms to obtain a fully-wired [ForumRepository].
 *
 * Mirrors the Android `RepositoryProvider`. Swift code calls
 * `RepositoryProvider.shared.forumRepository` to get the singleton.
 */
object RepositoryProvider {
    private val dataStore: DataStore<Preferences> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        createSharedDataStore()
    }

    private val forumPreferences: ForumPreferences by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ForumPreferences(dataStore)
    }

    val forumRepository: ForumRepository by lazy(LazyThreadSafetyMode.PUBLICATION) { build() }

    val forumBridge: AppleForumBridge by lazy(LazyThreadSafetyMode.PUBLICATION) {
        AppleForumBridge(forumRepository, forumPreferences)
    }

    private fun build(): ForumRepository {
        val database = DatabaseFactory().createDatabase()
        val cookieManager = CookieManager(dataStore)
        val catalogIndexCache = CatalogIndexCache(dataStore)
        val apiClient = NmbApiClient(cookieManager)

        return ForumRepository(
            apiClient = apiClient,
            threadDao = database.threadDao(),
            postDao = database.postDao(),
            forumRefreshDao = database.forumRefreshDao(),
            subscriptionThreadDao = database.subscriptionThreadDao(),
            postingDraftDao = database.postingDraftDao(),
            postingHistoryDao = database.postingHistoryDao(),
            threadReadProgressDao = database.threadReadProgressDao(),
            catalogIndexCache = catalogIndexCache,
            forumPreferences = forumPreferences
        )
    }

    private fun createSharedDataStore(): DataStore<Preferences> {
        val path = appleApplicationSupportFilePath("shared_prefs.preferences_pb")
        return PreferenceDataStoreFactory.createWithPath(produceFile = { path.toPath() })
    }
}
