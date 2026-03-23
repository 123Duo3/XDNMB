package ink.duo3.fogisland.shared.storage.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class DatabaseFactory(private val context: Context) {
    actual fun createBuilder(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = context.getDatabasePath("fogisland.db")
        return Room.databaseBuilder<AppDatabase>(
            context = context.applicationContext,
            name = dbFile.absolutePath
        )
    }
}
