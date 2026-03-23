package ink.duo3.fogisland.shared.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoticeDto(
    @SerialName("content") val content: String? = null,
    @SerialName("date") val date: Long? = null,
    @SerialName("enable") val enabled: Boolean? = null
)
