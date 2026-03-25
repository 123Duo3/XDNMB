package ink.duo3.fogisland.shared.storage.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "posting_history",
    indices = [
        Index("threadId"),
        Index("createdAt")
    ]
)
data class PostingHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val threadId: Long?,
    val postId: Long?,
    val forumId: Long?,
    val threadTitle: String,
    val name: String,
    val title: String,
    val contentText: String,
    val hasImage: Boolean,
    val createdAt: Long
)
