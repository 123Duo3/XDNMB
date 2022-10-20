package ink.duo3.xdnmb.data.model

import kotlinx.serialization.Serializable

@Serializable
class ForumList: MutableList<Thread> by ArrayList()

@Serializable
data class Forum(
    val forums: List<ForumDetail>,
    val id: String,
    val name: String,
    val sort: String,
    val status: String
)

@Serializable
data class ForumDetail(
    var createdAt: String?= null,
    var fgroup: String?,
    val id: String,
    var interval: String?,
    var msg: String?,
    var name: String?,
    var showName: String?,
    var sort: String,
    var status: String?,
    var updateAt: String?
)
