package ink.duo3.fogisland.shared.model

import ink.duo3.fogisland.shared.storage.db.entity.PostEntity
import ink.duo3.fogisland.shared.storage.db.entity.ThreadEntity
import ink.duo3.fogisland.shared.storage.db.entity.ThreadReadProgressEntity

data class ThreadDetail(
    val thread: ThreadEntity?,
    val posts: List<PostEntity>,
    val progress: ThreadReadProgressEntity?
)

data class ReadHistoryEntry(
    val threadId: Long,
    val forumId: Long?,
    val userHash: String,
    val name: String,
    val title: String,
    val contentText: String,
    val postedAtEpochMillis: Long?,
    val replyCount: Int,
    val lastReadPage: Int,
    val lastReadPostId: Long?,
    val lastReadAt: Long
)

enum class SearchHitType {
    THREAD,
    POST
}

data class SearchHit(
    val type: SearchHitType,
    val threadId: Long,
    val postId: Long? = null,
    val forumId: Long?,
    val userHash: String,
    val name: String,
    val title: String,
    val preview: String,
    val postedAtEpochMillis: Long?,
    val page: Int?,
    val refreshedAt: Long
)

data class DirectThreadShortcut(
    val threadId: Long,
    val forumId: Long?,
    val userHash: String,
    val name: String,
    val title: String,
    val preview: String,
    val postedAtEpochMillis: Long?,
    val isCached: Boolean
)
