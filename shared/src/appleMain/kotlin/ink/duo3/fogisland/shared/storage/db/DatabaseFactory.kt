package ink.duo3.fogisland.shared.storage.db

import androidx.room.Room
import androidx.room.RoomDatabase
import ink.duo3.fogisland.shared.storage.appleApplicationSupportFilePath
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual class DatabaseFactory {
    actual fun createBuilder(): RoomDatabase.Builder<AppDatabase> {
        val dbFilePath = appleApplicationSupportFilePath("fogisland.db")
        
        return Room.databaseBuilder<AppDatabase>(
            name = dbFilePath,
            factory = { AppDatabaseConstructor.initialize() }
        )
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
actual val databaseQueryDispatcher: CoroutineDispatcher =
    Dispatchers.Default.limitedParallelism(4)
