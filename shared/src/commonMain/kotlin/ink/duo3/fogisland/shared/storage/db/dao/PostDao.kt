package ink.duo3.fogisland.shared.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ink.duo3.fogisland.shared.storage.db.entity.PostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query(
        """
        DELETE FROM posts
        WHERE threadId = :threadId AND page = :page
        """
    )
    suspend fun deleteThreadPage(threadId: Long, page: Int)

    @Query(
        """
        DELETE FROM posts
        WHERE threadId = :threadId AND page IS NOT NULL AND page > :maxPage
        """
    )
    suspend fun deleteThreadPagesAfter(threadId: Long, maxPage: Int)

    @Query(
        """
        SELECT * FROM posts
        WHERE threadId = :threadId AND page IS NOT NULL AND page > 0
        ORDER BY page ASC, positionInPage ASC
        """
    )
    fun observePostsByThread(threadId: Long): Flow<List<PostEntity>>

    @Query(
        """
        SELECT * FROM posts
        WHERE isTips = 0
          AND (
              title LIKE '%' || :query || '%'
              OR contentText LIKE '%' || :query || '%'
          )
        ORDER BY refreshedAt DESC
        """
    )
    suspend fun searchPosts(query: String): List<PostEntity>
}
