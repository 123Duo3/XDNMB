package ink.duo3.fogisland.shared.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "thread_read_progress")
data class ThreadReadProgressEntity(
    @PrimaryKey val threadId: Long,
    val lastReadPage: Int = 1,
    val lastReadPostId: Long? = null,
    val lastVisibleItemIndex: Int = 0,
    val lastVisibleItemOffset: Int = 0,
    val updatedAt: Long = 0L
)
