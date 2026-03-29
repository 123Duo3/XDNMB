package ink.duo3.fogisland.shared.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ink.duo3.fogisland.shared.model.NmbPost
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

    @Query("DELETE FROM thread_read_progress WHERE updatedAt < :expireBefore")
    suspend fun deleteExpired(expireBefore: Long)

    @Query(
        """
        SELECT
            threads.id AS id,
            threads.id AS threadId,
            threads.id AS remoteId,
            threads.forumId AS forumId,
            threads.replyCount AS replyCount,
            threads.userHash AS userHash,
            threads.name AS name,
            threads.title AS title,
            threads.contentHtml AS contentHtml,
            threads.contentText AS contentText,
            threads.image AS image,
            threads.ext AS ext,
            threads.postedAtEpochMillis AS postedAtEpochMillis,
            threads.sage AS sage,
            threads.admin AS admin,
            threads.hide AS hide,
            threads.isTips AS isTips,
            1 AS isPoster,
            1 AS isThread,
            NULL AS page,
            0 AS positionInPage,
            threads.remainReplies AS remainReplies,
            threads.refreshedAt AS refreshedAt
        FROM thread_read_progress
        INNER JOIN threads ON threads.id = thread_read_progress.threadId
        ORDER BY thread_read_progress.updatedAt DESC
        """
    )
    fun observeReadHistory(): Flow<List<NmbPost>>
}
