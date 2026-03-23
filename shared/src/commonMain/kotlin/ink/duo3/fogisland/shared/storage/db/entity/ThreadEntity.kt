package ink.duo3.fogisland.shared.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "threads")
data class ThreadEntity(
    @PrimaryKey val id: Long,
    val forumId: Long?,
    val userHash: String,
    val name: String,
    val title: String,
    val contentHtml: String,
    val contentText: String,
    val image: String,
    val ext: String,
    val postedAt: String,
    val sage: Int,
    val admin: Int,
    val hide: Int,
    val replyCount: Int,
    val remainReplies: Int?,
    val refreshedAt: Long = 0L
)
