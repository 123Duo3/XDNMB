package ink.duo3.fogisland.shared.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LastPostDto(
    @Serializable(with = FlexibleLongSerializer::class)
    @SerialName("id") val id: Long,
    @Serializable(with = FlexibleNullableLongSerializer::class)
    @SerialName("resto") val resto: Long? = null,
    @SerialName("now") val postedAtRaw: String? = null,
    @SerialName("user_hash") val userHash: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("content") val content: String? = null,
    @Serializable(with = FlexibleNullableBooleanSerializer::class)
    @SerialName("sage") val sage: Boolean? = null,
    @Serializable(with = FlexibleNullableBooleanSerializer::class)
    @SerialName("admin") val admin: Boolean? = null
)
