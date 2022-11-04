package ink.duo3.xdnmb.shared.data.entity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

@Serializable
data class ForumGroup(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("sort")
    val sort: String,
    @SerialName("status")
    val status: String,
    @SerialName("forums")
    val forums: List<Forum>
)

@Serializable
data class Forum(
    @SerialName("id")
    val id: String,
    @SerialName("fgroup")
    val fgroup: String?,
    @SerialName("sort")
    val sort: String?,
    @SerialName("name")
    val name: String,
    @SerialName("showName")
    val showName: String?,
    @SerialName("msg")
    val msg: String?,
    @SerialName("interval")
    val interval: String?, // post interval (s)
    @SerialName("safe_mode")
    val safeMode: String?=null,
    @SerialName("auto_delete")
    val autoDelete: String?=null,
    @SerialName("thread_count")
    val threadCount: String?,
    @SerialName("permission_level")
    val permissionLevel: String?, // need numbers of cookie
    @SerialName("forum_fuse_id")
    val forumFuseId: String?,
    @SerialName("createdAt")
    val createdAt: String?= null,
    @SerialName("updateAt")
    val updateAt: String?,
    @SerialName("status")
    val status: String?
)
