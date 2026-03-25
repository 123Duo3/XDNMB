package ink.duo3.fogisland.shared.model

data class CatalogThread(
    val id: Long,
    val forumId: Long?,
    val userHash: String,
    val name: String,
    val title: String,
    val contentHtml: String,
    val contentText: String,
    val image: String,
    val ext: String,
    val postedAtEpochMillis: Long?,
    val sage: Int,
    val admin: Int,
    val hide: Int,
    val replyCount: Int,
    val remainReplies: Int?,
    val refreshedAt: Long
)

data class ThreadPost(
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
    val postedAtEpochMillis: Long?,
    val sage: Int,
    val admin: Int,
    val hide: Int,
    val isTips: Boolean,
    val page: Int?,
    val positionInPage: Int,
    val refreshedAt: Long
)

data class SubscriptionThread(
    val threadId: Long,
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
    val refreshedAt: Long
)

data class ThreadReadProgress(
    val threadId: Long,
    val lastReadPage: Int,
    val lastReadPostId: Long?,
    val lastVisibleItemIndex: Int,
    val lastVisibleItemOffset: Int,
    val updatedAt: Long
)

data class ThreadDetail(
    val thread: CatalogThread?,
    val posts: List<ThreadPost>,
    val progress: ThreadReadProgress?
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
