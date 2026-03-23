package ink.duo3.fogisland.shared.model

data class SiteNotice(
    val contentHtml: String,
    val contentText: String,
    val publishedAt: Long?
)
