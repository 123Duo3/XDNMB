package ink.duo3.fogisland.shared.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NmbApiErrorDto(
    @SerialName("success") val success: Boolean? = null,
    @SerialName("error") val error: String? = null
)
