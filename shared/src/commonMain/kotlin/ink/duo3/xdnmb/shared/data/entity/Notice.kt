package ink.duo3.xdnmb.shared.data.entity

import kotlinx.serialization.Serializable

@Serializable
data class Notice(
    val content: String,
    val date: Long,
    val enable: Boolean,
    var dismissed: Boolean?= false
)
