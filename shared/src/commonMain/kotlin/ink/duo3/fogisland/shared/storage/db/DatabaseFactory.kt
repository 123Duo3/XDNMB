package ink.duo3.fogisland.shared.storage.db

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

expect class DatabaseFactory {
    fun createBuilder(): RoomDatabase.Builder<AppDatabase>
}

expect val databaseQueryDispatcher: kotlinx.coroutines.CoroutineDispatcher

fun DatabaseFactory.createDatabase(): AppDatabase {
    return createBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(databaseQueryDispatcher)
        .fallbackToDestructiveMigration(true)
        .build()
}
