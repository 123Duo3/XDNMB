package ink.duo3.fogisland.shared.storage.db.dao

import androidx.room.Dao
import androidx.room.Query
import ink.duo3.fogisland.shared.storage.db.entity.PostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
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
        SELECT MAX(page) FROM posts
        WHERE threadId = :threadId AND page IS NOT NULL AND page > 0
        """
    )
    suspend fun getMaxLoadedPageForThread(threadId: Long): Int?

    @Query(
        """
        SELECT * FROM posts
        WHERE isTips = 0
          AND (
              title LIKE '%' || :query || '%' ESCAPE '\'
              OR contentText LIKE '%' || :query || '%' ESCAPE '\'
          )
        ORDER BY refreshedAt DESC
        """
    )
    suspend fun searchPosts(query: String): List<PostEntity>
}
