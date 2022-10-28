package ink.duo3.xdnmb.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Threads : MutableList<Thread> by ArrayList()

@Serializable
data class Thread(
    @SerialName("id")
    val id: Int,
    @SerialName("fid")
    val fid: Int,
    @SerialName("ReplyCount")
    var replyCount: Int?,
    @SerialName("img")
    val img: String,
    @SerialName("ext")
    val ext: String, // Image format
    @SerialName("now")
    val time: String, // Time
    @SerialName("user_hash")
    val userHash: String, // Cookie
    @SerialName("name")
    val name: String,
    @SerialName("title")
    val title: String,
    @SerialName("content")
    val content: String,
    @SerialName("sage")
    val sage: Boolean,
    @SerialName("admin")
    val admin: Boolean,
    @SerialName("Hide")
    val hide: Boolean?,
    @SerialName("Replies")
    val replies: List<Thread>?,
    @SerialName("RemainReplies")
    val remainReplies:Int?,
    @SerialName("email")
    val email: String?,
    @SerialName("master")
    var master: Boolean?, // Is poster
    @SerialName("page")
    var page:Int = 1
)
