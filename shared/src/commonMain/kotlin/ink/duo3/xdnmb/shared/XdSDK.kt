package ink.duo3.xdnmb.shared

import ink.duo3.xdnmb.shared.data.cache.Database
import ink.duo3.xdnmb.shared.data.cache.DatabaseDriverFactory
import ink.duo3.xdnmb.shared.data.entity.ForumGroup
import ink.duo3.xdnmb.shared.data.entity.Thread
import ink.duo3.xdnmb.shared.network.XdApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime

class XdSDK(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = Database(databaseDriverFactory)
    private val api = XdApi()
    private val imageCDN = "https://image.nmb.best/"

    @Throws(Exception::class)
    suspend fun getForumList(forceReload: Boolean): List<ForumGroup> =
        withContext(Dispatchers.Default) {
            val cachedForumList = database.getAllForums()
            return@withContext if (cachedForumList.isNotEmpty() && !forceReload) {
                cachedForumList
            } else {
                api.getForumList().also {
                    database.clearForumDatabase()
                    database.createForumList(it)
                }
            }
        }

    @Throws(Exception::class)
    suspend fun getTimeLine(forceReload: Boolean, page: Int): List<Thread> =
        withContext(Dispatchers.Default) {
            var cachedThreadList = database.getAllTimeLine()
            return@withContext if (cachedThreadList.isNotEmpty() && !forceReload) {
                val nextPage = api.getTimeLine(page)
                database.createTimeLine(nextPage)
                cachedThreadList = database.getAllTimeLine()
                return@withContext cachedThreadList
            } else {
                api.getTimeLine(page).also {
                    database.clearTimeLine()
                    database.createTimeLine(it)
                }
            }
        }

    fun imgToUrl(img: String, ext:String, isThumb: Boolean): String{
        var imageType = "image/"
        if (isThumb) {
            imageType = "thumb/"
        }
        return imageCDN + imageType + img + ext
    }

    fun getForumName(forumId: Int): String {
        return database.getForumName(forumId.toString())
    }

    fun formatTime(originalTime: String, inThread: Boolean): String {

    }

}