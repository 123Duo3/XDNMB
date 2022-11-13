package ink.duo3.xdnmb.shared.data.entity

import kotlinx.serialization.Serializable

@Serializable
data class Cookie(
    val cookie: String,
    val name: String,
    val rank: Int?= 1,
    val selected: Boolean?= false
)