package ink.duo3.xdnmb.data.cache

import ink.duo3.xdnmb.data.model.Forum
import ink.duo3.xdnmb.data.model.ForumGroup

internal class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = AppDatabase(databaseDriverFactory.createDriver())
    private val dbQuery = database.appDatabaseQueries

    internal fun clearDatabase() {
        dbQuery.transaction {
            dbQuery.removeAllForum()
            dbQuery.removeAllForumDetail()
            dbQuery.removeAllThead()
        }
    }

    internal fun getAllForums(): List<ForumGroup> {
        val forumGroups = dbQuery.selectAllForumGroup(::mapForumGroupSelecting).executeAsList()
        return forumGroups
    }

    private fun mapForumGroupSelecting(
        id: String,
        name: String,
        sort: String,
        status: String
    ): ForumGroup {
        return ForumGroup(
            forums = dbQuery.selectForumByGroupId(id, ::mapForumSelecting).executeAsList(),
            id = id, name = name, sort = sort, status = status
        )
    }

    private fun mapForumSelecting(
        id: String,
        fgroup: String?,
        sort: String?,
        name: String,
        showName: String?,
        msg: String?,
        interval: String?,
        safeMode: String?,
        autoDelete: String?,
        threadCount: String?,
        permissionLevel: String?,
        forumFuseId: String?,
        createdAt: String?,
        updateAt: String?,
        status: String?
    ): Forum {
        return Forum(
            id, fgroup, sort, name, showName, msg, interval, safeMode, autoDelete, threadCount, permissionLevel, forumFuseId, createdAt, updateAt, status
        )
    }


    internal fun createForumList(forumList: List<ForumGroup>) {
        dbQuery.transaction {
            forumList.forEach {forumGroup ->
                insertForumGroup(forumGroup)

                forumGroup.forums.forEach {forum ->
                    insertForum(forum)
                }
            }
        }
    }

    private fun insertForumGroup(forumGroup: ForumGroup) {
        dbQuery.insertForumGroup(
            id = forumGroup.id,
            name = forumGroup.name,
            sort = forumGroup.sort,
            status = forumGroup.status,
        )
    }

    private fun insertForum(forum: Forum) {
        dbQuery.insertForum(
            forum.id,
            forum.fgroup,
            forum.sort,
            forum.name,
            forum.showName,
            forum.msg,
            forum.interval,
            forum.safeMode,
            forum.autoDelete,
            forum.threadCount,
            forum.permissionLevel,
            forum.forumFuseId,
            forum.createdAt,
            forum.updateAt,
            forum.status
        )
    }
}
