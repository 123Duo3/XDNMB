package ink.duo3.xdnmb

import ink.duo3.xdnmb.data.cache.Database
import ink.duo3.xdnmb.data.cache.DatabaseDriverFactory
import ink.duo3.xdnmb.data.model.ForumGroup
import ink.duo3.xdnmb.network.XdApi

class XdSDK(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = Database(databaseDriverFactory)
    private val api = XdApi()

    @Throws(Exception::class)
    suspend fun getForumList(forceReload: Boolean): List<ForumGroup> {
        val cachedForumList = database.getAllForums()
        return if (cachedForumList.isNotEmpty() && !forceReload) {
            cachedForumList
        } else {
            api.getForumList().also {
                database.clearDatabase()
                database.createForumList(it)
            }
        }
    }
}