package ink.duo3.fogisland.shared.repository

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import ink.duo3.fogisland.shared.network.api.NmbApiClient
import ink.duo3.fogisland.shared.storage.db.DatabaseFactory
import ink.duo3.fogisland.shared.storage.db.createDatabase
import ink.duo3.fogisland.shared.storage.preferences.CatalogIndexCache
import ink.duo3.fogisland.shared.storage.preferences.CookieManager
import ink.duo3.fogisland.shared.storage.preferences.ForumPreferences

val Context.sharedDataStore by preferencesDataStore(name = "shared_prefs")

object RepositoryProvider {
    @Volatile
    private var forumRepository: ForumRepository? = null
    @Volatile
    private var forumPreferences: ForumPreferences? = null

    fun provideForumRepository(context: Context): ForumRepository {
        return forumRepository ?: synchronized(this) {
            forumRepository ?: createForumRepository(context).also { forumRepository = it }
        }
    }

    fun provideForumPreferences(context: Context): ForumPreferences {
        return forumPreferences ?: synchronized(this) {
            forumPreferences ?: ForumPreferences(context.sharedDataStore).also {
                forumPreferences = it
            }
        }
    }

    private fun createForumRepository(context: Context): ForumRepository {
        val database = DatabaseFactory(context).createDatabase()
        val cookieManager = CookieManager(context.sharedDataStore)
        val catalogIndexCache = CatalogIndexCache(context.sharedDataStore)
        val apiClient = NmbApiClient(cookieManager)
        
        return ForumRepository(
            apiClient = apiClient,
            threadDao = database.threadDao(),
            postDao = database.postDao(),
            catalogDao = database.catalogDao(),
            threadReadProgressDao = database.threadReadProgressDao(),
            catalogIndexCache = catalogIndexCache
        )
    }
}
