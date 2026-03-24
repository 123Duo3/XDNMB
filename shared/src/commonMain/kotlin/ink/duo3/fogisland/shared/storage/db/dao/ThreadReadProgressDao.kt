package ink.duo3.fogisland.shared.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ink.duo3.fogisland.shared.model.ReadHistoryEntry
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

    @Query("DELETE FROM thread_read_progress WHERE threadId = :threadId")
    suspend fun deleteByThreadId(threadId: Long)

    @Query("DELETE FROM thread_read_progress")
    suspend fun deleteAll()

    @Query(
        """
        SELECT
            threads.id AS threadId,
            threads.forumId AS forumId,
            threads.userHash AS userHash,
            threads.name AS name,
            threads.title AS title,
            threads.contentText AS contentText,
            threads.postedAtEpochMillis AS postedAtEpochMillis,
            threads.replyCount AS replyCount,
            thread_read_progress.lastReadPage AS lastReadPage,
            thread_read_progress.lastReadPostId AS lastReadPostId,
            thread_read_progress.updatedAt AS lastReadAt
        FROM thread_read_progress
        INNER JOIN threads ON threads.id = thread_read_progress.threadId
        ORDER BY thread_read_progress.updatedAt DESC
        """
    )
    fun observeReadHistory(): Flow<List<ReadHistoryEntry>>
}
