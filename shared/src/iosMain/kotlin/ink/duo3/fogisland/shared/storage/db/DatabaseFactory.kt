package ink.duo3.fogisland.shared.storage.db

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class DatabaseFactory {
    @OptIn(ExperimentalForeignApi::class)
    actual fun createBuilder(): RoomDatabase.Builder<AppDatabase> {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        requireNotNull(documentDirectory)
        val dbFilePath = documentDirectory.path + "/fogisland.db"
        
        return Room.databaseBuilder<AppDatabase>(
            name = dbFilePath,
            factory = { AppDatabaseConstructor.initialize() }
        )
    }
}
