package ink.duo3.fogisland.shared.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ink.duo3.fogisland.shared.storage.db.entity.ThreadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreads(threads: List<ThreadEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ThreadEntity)

    @Query(
        """
        SELECT threads.* FROM catalog_entries
        INNER JOIN threads ON threads.id = catalog_entries.threadId
        WHERE catalog_entries.catalogType = :catalogType AND catalog_entries.catalogId = :catalogId
        ORDER BY catalog_entries.page ASC, catalog_entries.position ASC
        """
    )
    fun observeCatalog(catalogType: String, catalogId: Long): Flow<List<ThreadEntity>>

    @Query("SELECT * FROM threads WHERE id = :id")
    suspend fun getThreadById(id: Long): ThreadEntity?

    @Query("SELECT * FROM threads WHERE id = :id")
    fun observeThreadById(id: Long): Flow<ThreadEntity?>

    @Query(
        """
        SELECT * FROM threads
        WHERE title LIKE '%' || :query || '%'
           OR contentText LIKE '%' || :query || '%'
        ORDER BY refreshedAt DESC
        """
    )
    suspend fun searchThreads(query: String): List<ThreadEntity>
}
