package ink.duo3.fogisland.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class ForumGroup(
    val id: Long,
    val name: String,
    val sort: Int,
    val status: String,
    val forums: List<ForumBoard>
)

@Serializable
data class ForumBoard(
    val id: Long,
    val groupId: Long,
    val name: String,
    val displayName: String,
    val noticeHtml: String,
    val noticeText: String,
    val sort: Int?,
    val threadCount: Int?,
    val permissionLevel: Int?,
    val status: String?
)

@Serializable
data class Timeline(
    val id: Long,
    val name: String,
    val displayName: String,
    val notice: String,
    val maxPage: Int?
)

@Serializable
enum class CatalogType {
    FORUM,
    TIMELINE
}

data class CatalogSource(
    val type: CatalogType,
    val id: Long,
    val title: String,
    val subtitle: String? = null
)
