package ink.duo3.fogisland.shared.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ThreadDto(
    @Serializable(with = FlexibleLongSerializer::class)
    @SerialName("id") val id: Long,
    @Serializable(with = FlexibleNullableLongSerializer::class)
    @SerialName("fid") val forumId: Long? = null,
    @SerialName("user_hash") val userHash: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("img") val image: String? = null,
    @SerialName("ext") val imageExtension: String? = null,
    @SerialName("now") val postedAtRaw: String? = null,
    @Serializable(with = FlexibleNullableIntSerializer::class)
    @SerialName("sage") val sage: Int? = null,
    @Serializable(with = FlexibleNullableIntSerializer::class)
    @SerialName("admin") val admin: Int? = null,
    @Serializable(with = FlexibleNullableIntSerializer::class)
    @SerialName("ReplyCount") val replyCount: Int? = null,
    @Serializable(with = FlexibleNullableIntSerializer::class)
    @SerialName("RemainReplies") val remainReplies: Int? = null,
    @Serializable(with = FlexibleNullableIntSerializer::class)
    @SerialName("Hide") val hide: Int? = null,
    @SerialName("Replies") val replies: List<PostDto> = emptyList()
)

@Serializable
data class PostDto(
    @Serializable(with = FlexibleLongSerializer::class)
    @SerialName("id") val id: Long,
    @Serializable(with = FlexibleNullableLongSerializer::class)
    @SerialName("fid") val forumId: Long? = null,
    @Serializable(with = FlexibleNullableIntSerializer::class)
    @SerialName("ReplyCount") val replyCount: Int? = null,
    @SerialName("user_hash") val userHash: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("img") val image: String? = null,
    @SerialName("ext") val imageExtension: String? = null,
    @SerialName("now") val postedAtRaw: String? = null,
    @Serializable(with = FlexibleNullableIntSerializer::class)
    @SerialName("sage") val sage: Int? = null,
    @Serializable(with = FlexibleNullableIntSerializer::class)
    @SerialName("admin") val admin: Int? = null,
    @Serializable(with = FlexibleNullableIntSerializer::class)
    @SerialName("Hide") val hide: Int? = null
)
