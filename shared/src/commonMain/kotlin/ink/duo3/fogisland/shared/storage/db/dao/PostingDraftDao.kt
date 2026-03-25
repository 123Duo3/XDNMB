package ink.duo3.fogisland.shared.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ink.duo3.fogisland.shared.storage.db.entity.PostingDraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostingDraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PostingDraftEntity): Long

    @Query("SELECT * FROM posting_draft ORDER BY updatedAt DESC, id DESC")
    fun observeAll(): Flow<List<PostingDraftEntity>>

    @Query("SELECT * FROM posting_draft ORDER BY updatedAt DESC, id DESC")
    suspend fun getAll(): List<PostingDraftEntity>

    @Query("SELECT * FROM posting_draft WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PostingDraftEntity?

    @Query("DELETE FROM posting_draft WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM posting_draft")
    suspend fun deleteAll()
}
