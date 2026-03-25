package ink.duo3.fogisland.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class SiteNotice(
    val contentHtml: String,
    val contentText: String,
    val publishedAt: Long?
)
