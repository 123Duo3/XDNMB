package ink.duo3.fogisland.shared.storage.db.dao

import androidx.room.Dao
import androidx.room.Query
import ink.duo3.fogisland.shared.storage.db.entity.SubscriptionThreadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionThreadDao {
    @Query("SELECT * FROM subscription_threads ORDER BY page ASC, positionInPage ASC")
    fun observeAll(): Flow<List<SubscriptionThreadEntity>>

    @Query("DELETE FROM subscription_threads")
    suspend fun deleteAll()

    @Query("DELETE FROM subscription_threads WHERE threadId = :threadId")
    suspend fun deleteThread(threadId: Long)
}
