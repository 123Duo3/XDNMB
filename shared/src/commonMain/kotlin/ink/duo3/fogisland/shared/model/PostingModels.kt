package ink.duo3.fogisland.shared.model

data class ThreadPostImage(
    val fileName: String,
    val mimeType: String?,
    val bytes: ByteArray
)

data class ThreadPostRequest(
    val forumId: Long,
    val name: String = "",
    val email: String = "",
    val title: String = "",
    val content: String,
    val useWatermark: Boolean = true,
    val image: ThreadPostImage? = null
)

data class ThreadPostResult(
    val forumId: Long,
    val threadId: Long?,
    val message: String,
    val contentText: String
)

enum class PostingDraftType {
    THREAD,
    REPLY
}

data class PostingDraftEntry(
    val id: Long,
    val type: PostingDraftType,
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
) {
    val hasImage: Boolean
        get() = imagePath != null
}

enum class PostingHistoryType {
    THREAD,
    REPLY
}

data class PostingHistoryEntry(
    val id: Long,
    val type: PostingHistoryType,
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

data class ReplyPostRequest(
    val threadId: Long,
    val name: String = "",
    val email: String = "",
    val title: String = "",
    val content: String,
    val useWatermark: Boolean = true,
    val image: ThreadPostImage? = null
)

data class ReplyPostResult(
    val threadId: Long,
    val postId: Long?,
    val message: String,
    val contentText: String
)
