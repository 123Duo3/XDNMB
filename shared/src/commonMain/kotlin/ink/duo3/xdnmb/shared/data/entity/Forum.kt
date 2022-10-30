package ink.duo3.xdnmb.shared.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForumGroup(
    val id: String,
    val name: String,
    val sort: String,
    val status: String,
    val forums: List<Forum>
)

@Serializable
data class Forum(
    val id: String,
    val fgroup: String?,
    val sort: String?,
    val name: String,
    val showName: String?,
    val msg: String?,
    val interval: String?, // post interval (s)
    @SerialName("safe_mode")
    val safeMode: String?,
    @SerialName("auto_delete")
    val autoDelete: String?,
    @SerialName("thread_count")
    val threadCount: String?,
    @SerialName("permission_level")
    val permissionLevel: String?, // need numbers of cookie
    @SerialName("forum_fuse_id")
    val forumFuseId: String?,
    val createdAt: String?= null,
    val updateAt: String?,
    val status: String?
)