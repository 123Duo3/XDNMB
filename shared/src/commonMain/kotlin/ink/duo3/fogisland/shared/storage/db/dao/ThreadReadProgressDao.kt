package ink.duo3.fogisland.shared.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ink.duo3.fogisland.shared.storage.db.entity.ThreadReadProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadReadProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ThreadReadProgressEntity)

    @Query("SELECT * FROM thread_read_progress WHERE threadId = :threadId")
    fun observeByThreadId(threadId: Long): Flow<ThreadReadProgressEntity?>

    @Query("SELECT * FROM thread_read_progress WHERE threadId = :threadId")
    suspend fun getByThreadId(threadId: Long): ThreadReadProgressEntity?
}
