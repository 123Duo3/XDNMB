package ink.duo3.fogisland.shared.storage.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "posting_draft",
    indices = [
        Index("type"),
        Index("threadId"),
        Index("updatedAt")
    ]
)
data class PostingDraftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val threadId: Long?,
    val forumId: Long?,
    val threadTitle: String,
    val name: String,
    val email: String,
    val title: String,
    val contentText: String,
    val useWatermark: Boolean,
    val imagePath: String?,
    val imageFileName: String?,
    val imageMimeType: String?,
    val updatedAt: Long
)
