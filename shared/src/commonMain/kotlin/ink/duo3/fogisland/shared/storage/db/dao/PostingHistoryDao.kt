package ink.duo3.fogisland.shared.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ink.duo3.fogisland.shared.storage.db.entity.PostingHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostingHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PostingHistoryEntity)

    @Query("SELECT * FROM posting_history ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<PostingHistoryEntity>>

    @Query("DELETE FROM posting_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM posting_history WHERE type = :type")
    suspend fun deleteByType(type: String)

    @Query("DELETE FROM posting_history")
    suspend fun deleteAll()
}
