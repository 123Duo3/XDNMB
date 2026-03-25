package ink.duo3.fogisland.shared.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import ink.duo3.fogisland.shared.storage.db.entity.CatalogEntryEntity
import ink.duo3.fogisland.shared.storage.db.entity.PostEntity
import ink.duo3.fogisland.shared.storage.db.entity.SubscriptionThreadEntity
import ink.duo3.fogisland.shared.storage.db.entity.ThreadEntity

@Dao
interface ForumRefreshDao {
    @Transaction
    suspend fun replaceCatalogPage(
        catalogType: String,
        catalogId: Long,
        page: Int,
        threads: List<ThreadEntity>,
        posts: List<PostEntity>,
        entries: List<CatalogEntryEntity>
    ) {
        deleteCatalogPage(catalogType, catalogId, page)
        upsertThreads(threads)
        insertCatalogPreviewPosts(posts)
        insertCatalogEntries(entries)
    }

    @Transaction
    suspend fun replaceThreadPage(
        thread: ThreadEntity,
        threadId: Long,
        page: Int,
        maxPage: Int,
        posts: List<PostEntity>
    ) {
        upsertThread(thread)
        deleteThreadPage(threadId, page)
        insertThreadPosts(posts)
        deleteThreadPagesAfter(threadId, maxPage)
    }

    @Transaction
    suspend fun replaceSubscriptionPage(
        page: Int,
        threads: List<SubscriptionThreadEntity>
    ) {
        if (page == 1) {
            deleteAllSubscriptions()
        } else {
            deleteSubscriptionPage(page)
        }
        insertSubscriptionThreads(threads)
    }

    @Upsert
    suspend fun upsertThreads(threads: List<ThreadEntity>)

    @Upsert
    suspend fun upsertThread(thread: ThreadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreadPosts(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCatalogPreviewPosts(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatalogEntries(entries: List<CatalogEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptionThreads(threads: List<SubscriptionThreadEntity>)

    @Query(
        """
        DELETE FROM catalog_entries
        WHERE catalogType = :catalogType AND catalogId = :catalogId AND page = :page
        """
    )
    suspend fun deleteCatalogPage(catalogType: String, catalogId: Long, page: Int)

    @Query(
        """
        DELETE FROM posts
        WHERE threadId = :threadId AND page = :page
        """
    )
    suspend fun deleteThreadPage(threadId: Long, page: Int)

    @Query(
        """
        DELETE FROM posts
        WHERE threadId = :threadId AND page IS NOT NULL AND page > :maxPage
        """
    )
    suspend fun deleteThreadPagesAfter(threadId: Long, maxPage: Int)

    @Query("DELETE FROM subscription_threads")
    suspend fun deleteAllSubscriptions()

    @Query("DELETE FROM subscription_threads WHERE page = :page")
    suspend fun deleteSubscriptionPage(page: Int)
}
