package ink.duo3.fogisland.shared.model

data class NmbPost(
    val id: Long,
    val threadId: Long = id,
    val remoteId: Long = id,
    val forumId: Long?,
    val replyCount: Int?,
    val userHash: String,
    val name: String?,
    val title: String?,
    val contentHtml: String,
    val contentText: String,
    val image: String?,
    val ext: String?,
    val postedAtEpochMillis: Long?,
    val sage: Boolean,
    val admin: Boolean,
    val hide: Boolean,
    val isTips: Boolean,
    val isPoster: Boolean = true,
    val isThread: Boolean = true,
    val page: Int? = null,
    val positionInPage: Int = 0,
    val remainReplies: Int? = null,
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
    val thread: NmbPost?,
    val posts: List<NmbPost>,
    val progress: ThreadReadProgress?
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
    val name: String?,
    val title: String?,
    val preview: String,
    val postedAtEpochMillis: Long?,
    val sage: Boolean,
    val admin: Boolean,
    val hide: Boolean,
    val page: Int?,
    val refreshedAt: Long
)

data class DirectThreadShortcut(
    val threadId: Long,
    val forumId: Long?,
    val userHash: String,
    val name: String?,
    val title: String?,
    val preview: String,
    val postedAtEpochMillis: Long?,
    val isCached: Boolean
)
