package ink.duo3.fogisland.shared.storage.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "posts",
    primaryKeys = ["threadId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = ThreadEntity::class,
            parentColumns = ["id"],
            childColumns = ["threadId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("threadId"),
        Index("id")
    ]
)
data class PostEntity(
    val threadId: Long,
    val id: Long,
    val remoteId: Long,
    val forumId: Long?,
    val replyCount: Int?,
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
    val isTips: Boolean,
    val page: Int?,
    val positionInPage: Int,
    val refreshedAt: Long = 0L
)
