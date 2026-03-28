package ink.duo3.fogisland.shared.network.model

import kotlinx.serialization.Serializable

@Serializable
data class CdnPathDto(
    val url: String? = null,
    val rate: Double? = null
)
