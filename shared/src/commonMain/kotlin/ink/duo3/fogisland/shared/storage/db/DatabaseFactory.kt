package ink.duo3.fogisland.shared.storage.db

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

expect class DatabaseFactory {
    fun createBuilder(): RoomDatabase.Builder<AppDatabase>
}

fun DatabaseFactory.createDatabase(): AppDatabase {
    return createBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(true)
        .build()
}
