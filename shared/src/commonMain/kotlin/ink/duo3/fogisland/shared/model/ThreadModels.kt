package ink.duo3.fogisland.shared.model

import ink.duo3.fogisland.shared.storage.db.entity.PostEntity
import ink.duo3.fogisland.shared.storage.db.entity.ThreadEntity
import ink.duo3.fogisland.shared.storage.db.entity.ThreadReadProgressEntity

data class ThreadDetail(
    val thread: ThreadEntity?,
    val posts: List<PostEntity>,
    val progress: ThreadReadProgressEntity?
)

enum class SearchHitType {
    THREAD,
    POST
}

data class SearchHit(
    val type: SearchHitType,
    val threadId: Long,
    val postId: Long? = null,
    val title: String,
    val preview: String
)
