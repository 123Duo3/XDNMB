package ink.duo3.fogisland.shared.storage.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual class DatabaseFactory {
    actual fun createBuilder(): RoomDatabase.Builder<AppDatabase> {
        val databasePath = File(
            System.getProperty("java.io.tmpdir"),
            "fogisland-desktop.db"
        ).absolutePath

        return Room.databaseBuilder<AppDatabase>(
            name = databasePath,
            factory = { AppDatabaseConstructor.initialize() }
        )
    }
}
