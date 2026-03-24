package ink.duo3.fogisland.shared.storage.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscription_threads")
data class SubscriptionThreadEntity(
    @PrimaryKey val threadId: Long,
    val forumId: Long?,
    val userHash: String,
    val name: String,
    val title: String,
    val contentText: String,
    val image: String,
    val ext: String,
    val postedAtEpochMillis: Long?,
    val replyCount: Int,
    val remainReplies: Int?,
    val page: Int,
    val positionInPage: Int,
    val refreshedAt: Long = 0L
)
