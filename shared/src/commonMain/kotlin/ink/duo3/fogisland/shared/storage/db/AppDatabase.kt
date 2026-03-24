package ink.duo3.fogisland.shared.storage.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import ink.duo3.fogisland.shared.storage.db.dao.CatalogDao
import ink.duo3.fogisland.shared.storage.db.dao.PostDao
import ink.duo3.fogisland.shared.storage.db.dao.ThreadDao
import ink.duo3.fogisland.shared.storage.db.dao.ThreadReadProgressDao
import ink.duo3.fogisland.shared.storage.db.entity.CatalogEntryEntity
import ink.duo3.fogisland.shared.storage.db.entity.PostEntity
import ink.duo3.fogisland.shared.storage.db.entity.ThreadEntity
import ink.duo3.fogisland.shared.storage.db.entity.ThreadReadProgressEntity

@Database(
    entities = [
        ThreadEntity::class,
        PostEntity::class,
        CatalogEntryEntity::class,
        ThreadReadProgressEntity::class
    ],
    version = 3
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun threadDao(): ThreadDao
    abstract fun postDao(): PostDao
    abstract fun catalogDao(): CatalogDao
    abstract fun threadReadProgressDao(): ThreadReadProgressDao
}

@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
