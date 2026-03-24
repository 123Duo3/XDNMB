package ink.duo3.fogisland.shared.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ink.duo3.fogisland.shared.storage.db.entity.SubscriptionThreadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionThreadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreads(threads: List<SubscriptionThreadEntity>)

    @Query("SELECT * FROM subscription_threads ORDER BY page ASC, positionInPage ASC")
    fun observeAll(): Flow<List<SubscriptionThreadEntity>>

    @Query("DELETE FROM subscription_threads")
    suspend fun deleteAll()

    @Query("DELETE FROM subscription_threads WHERE page = :page")
    suspend fun deletePage(page: Int)

    @Query("DELETE FROM subscription_threads WHERE threadId = :threadId")
    suspend fun deleteThread(threadId: Long)
}
