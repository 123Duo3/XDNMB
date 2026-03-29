package ink.duo3.fogisland.shared.storage.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import ink.duo3.fogisland.shared.storage.db.dao.ForumRefreshDao
import ink.duo3.fogisland.shared.storage.db.dao.PostingDraftDao
import ink.duo3.fogisland.shared.storage.db.dao.PostingHistoryDao
import ink.duo3.fogisland.shared.storage.db.dao.PostDao
import ink.duo3.fogisland.shared.storage.db.dao.SubscriptionThreadDao
import ink.duo3.fogisland.shared.storage.db.dao.ThreadDao
import ink.duo3.fogisland.shared.storage.db.dao.ThreadReadProgressDao
import ink.duo3.fogisland.shared.storage.db.entity.CatalogEntryEntity
import ink.duo3.fogisland.shared.storage.db.entity.PostingDraftEntity
import ink.duo3.fogisland.shared.storage.db.entity.PostingHistoryEntity
import ink.duo3.fogisland.shared.storage.db.entity.PostEntity
import ink.duo3.fogisland.shared.storage.db.entity.SubscriptionThreadEntity
import ink.duo3.fogisland.shared.storage.db.entity.ThreadEntity
import ink.duo3.fogisland.shared.storage.db.entity.ThreadReadProgressEntity

@Database(
    entities = [
        ThreadEntity::class,
        PostEntity::class,
        CatalogEntryEntity::class,
        ThreadReadProgressEntity::class,
        SubscriptionThreadEntity::class,
        PostingDraftEntity::class,
        PostingHistoryEntity::class
    ],
    version = 12
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun threadDao(): ThreadDao
    abstract fun postDao(): PostDao
    abstract fun forumRefreshDao(): ForumRefreshDao
    abstract fun threadReadProgressDao(): ThreadReadProgressDao
    abstract fun subscriptionThreadDao(): SubscriptionThreadDao
    abstract fun postingDraftDao(): PostingDraftDao
    abstract fun postingHistoryDao(): PostingHistoryDao
}

@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
