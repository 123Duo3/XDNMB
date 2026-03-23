package ink.duo3.fogisland.shared.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForumGroupDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("sort") val sort: String,
    @SerialName("status") val status: String,
    @SerialName("forums") val forums: List<ForumBoardDto> = emptyList()
)

@Serializable
data class ForumBoardDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("msg") val notice: String? = null,
    @SerialName("showName") val displayName: String? = null,
    @SerialName("fgroup") val groupId: String? = null,
    @SerialName("sort") val sort: String? = null,
    @SerialName("thread_count") val threadCount: String? = null,
    @SerialName("permission_level") val permissionLevel: String? = null,
    @SerialName("status") val status: String? = null
)

@Serializable
data class TimelineDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("notice") val notice: String? = null,
    @SerialName("max_page") val maxPage: Int? = null
)
