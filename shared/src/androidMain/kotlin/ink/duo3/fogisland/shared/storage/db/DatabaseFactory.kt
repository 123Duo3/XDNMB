@file:JvmName("DatabaseFactoryAndroid")

package ink.duo3.fogisland.shared.storage.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.jvm.JvmName

actual class DatabaseFactory(private val context: Context) {
    actual fun createBuilder(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = context.getDatabasePath("fogisland.db")
        return Room.databaseBuilder<AppDatabase>(
            context = context.applicationContext,
            name = dbFile.absolutePath
        )
    }
}

actual val databaseQueryDispatcher: CoroutineDispatcher = Dispatchers.IO
