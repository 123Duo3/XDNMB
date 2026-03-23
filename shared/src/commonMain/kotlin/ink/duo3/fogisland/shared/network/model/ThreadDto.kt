package ink.duo3.fogisland.shared.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ThreadDto(
    @SerialName("id") val id: Long,
    @SerialName("fid") val fid: Long? = null,
    @SerialName("user_hash") val userHash: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("img") val img: String? = null,
    @SerialName("ext") val ext: String? = null,
    @SerialName("now") val now: String? = null,
    @SerialName("sage") val sage: Int? = null,
    @SerialName("admin") val admin: Int? = null,
    @SerialName("ReplyCount") val replyCount: Int? = null,
    @SerialName("RemainReplies") val remainReplies: Int? = null,
    @SerialName("Hide") val hide: Int? = null,
    @SerialName("Replies") val replies: List<PostDto> = emptyList()
)

@Serializable
data class PostDto(
    @SerialName("id") val id: Long,
    @SerialName("fid") val fid: Long? = null,
    @SerialName("ReplyCount") val replyCount: Int? = null,
    @SerialName("user_hash") val userHash: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("img") val img: String? = null,
    @SerialName("ext") val ext: String? = null,
    @SerialName("now") val now: String? = null,
    @SerialName("sage") val sage: Int? = null,
    @SerialName("admin") val admin: Int? = null,
    @SerialName("Hide") val hide: Int? = null
)
