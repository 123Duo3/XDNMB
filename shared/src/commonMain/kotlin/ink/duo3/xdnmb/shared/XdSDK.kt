package ink.duo3.xdnmb.shared

import ink.duo3.xdnmb.shared.data.cache.Database
import ink.duo3.xdnmb.shared.data.cache.DatabaseDriverFactory
import ink.duo3.xdnmb.shared.data.entity.*
import ink.duo3.xdnmb.shared.network.XdApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.periodUntil
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class XdSDK(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = Database(databaseDriverFactory)
    private val api = XdApi()
    private val imageCDN = "https://image.nmb.best/"

    @Throws(Exception::class)
    suspend fun getTimelineList(): List<ForumGroup> =
        withContext(Dispatchers.Default) {
            val timelineList = api.getTimelineList()
            return@withContext listOf(
                ForumGroup(
                    id = "-1",
                    name = "时间线",
                    sort = "-1",
                    status = "n",
                    forums = timelineList.map {
                        Forum(
                            id = it.id.toString(),
                            fgroup = "-1",
                            sort = "2",
                            name = it.name,
                            showName = it.display_name,
                            msg = it.notice,
                            interval = null,
                            safeMode = null,
                            autoDelete = null,
                            threadCount = null,
                            permissionLevel = null,
                            forumFuseId = null,
                            createdAt = null,
                            updateAt = null,
                            status = null
                        )
                    }
                )
            )
        }

    @Throws(Exception::class)
    suspend fun getForumList(forceReload: Boolean): List<ForumGroup> =
        withContext(Dispatchers.Default) {
            val cachedForumList = database.getAllForums()
            val forumList = if (cachedForumList.isNotEmpty() && !forceReload) {
                cachedForumList
            } else {
                api.getForumList().also {
                    database.clearForumDatabase()
                    database.createForumList(it)
                }
            }

            return@withContext getTimelineList() + forumList.map { forumGroup ->
                if (forumGroup.id == "4") {
                    forumGroup.copy(forums = forumGroup.forums.filterNot { it.id == "-1" && it.name == "时间线" })
                } else {
                    forumGroup
                }
            }
        }

    @Throws(Exception::class)
    suspend fun getTimeLine(forumId: Int, forceReload: Boolean, page: Int): List<Thread> =
        withContext(Dispatchers.Default) {
            var cachedThreadList = database.getAllTimeLine()
            if (cachedThreadList.isNotEmpty() && !forceReload) {
                val nextPage = api.getTimeLine(forumId, page)
                database.createTimeLine(nextPage)
                cachedThreadList = database.getAllTimeLine()
                return@withContext cachedThreadList
            } else {
                return@withContext api.getTimeLine(forumId, page).onEach { thread ->
                    thread.forumName = getForumName(thread.fid!!)
                }.also {
                    database.clearTimeLine()
                    database.createTimeLine(it)
                }
            }
        }

    @Throws(Exception::class)
    suspend fun getForumThreads(forumId: Int, forceReload: Boolean, page: Int): List<Thread> =
        withContext(Dispatchers.Default) {
            var cachedThreadList = database.getThreadsByForumId(forumId)
            val cookie = database.getSelectedCookie()
            return@withContext if (cachedThreadList.isNotEmpty() && !forceReload) {
                val nextPage = api.getTreadList(cookie?.cookie , forumId, page)
                database.createThreads(nextPage)
                cachedThreadList = database.getThreadsByForumId(forumId)
                return@withContext cachedThreadList
            } else {
                api.getTreadList(cookie?.cookie, forumId, page).also {
                    database.clearThreadsByForumId(forumId)
                    database.createThreads(it)
                }
            }
        }

    @Throws(Exception::class)
    suspend fun getReply(threadId: Int, page: Int): Thread =
        withContext(Dispatchers.Default) {
            val cookie = database.getSelectedCookie()
            return@withContext api.getReply(cookie?.cookie, threadId, page).apply {
                this.replies?.onEach {
                    it.page = page
                }
            }
        }

    @Throws(Exception::class)
    suspend fun addHistory(thread: Thread) {
        database.createHistory(thread)
    }

    @Throws(Exception::class)
    suspend fun getHistory(): List<Thread> =
        withContext(Dispatchers.Default) {
            return@withContext database.getHistory()
        }

    @Throws(Exception::class)
    suspend fun getCookies(): List<Cookie> =
        withContext(Dispatchers.Default) {
            return@withContext database.getAllCookie()
        }

    suspend fun addCookie(cookie: Cookie) {
        withContext(Dispatchers.Default) {
            val cachedCookies = database.getAllCookie()
            if (cachedCookies.isEmpty()) {
                database.createCookie(cookie.copy(selected = true))
            } else {
                database.createCookie(cookie)
            }
        }
    }

    suspend fun deleteCookie(cookie: Cookie) {
        withContext(Dispatchers.Default) {
            database.clearCookieByName(cookie.cookie)
        }
    }

    @Throws(Exception::class)
    suspend fun getSelectedCookie(): Cookie? =
        withContext(Dispatchers.Default) {
            return@withContext database.getSelectedCookie()
        }

    @Throws(Exception::class)
    suspend fun getCurrentNotice(): Notice? =
        withContext(Dispatchers.Default) {
            val cachedNotice = database.getLastNotice()
            val currentNotice = api.getNotice()
            var result: Notice? = if (
                cachedNotice?.content != currentNotice.content
                || cachedNotice.date != currentNotice.date
                || cachedNotice.dismissed == false
            ){
                currentNotice.also {
                    database.createNotice(it)
                }
            } else {
                null
            }
            return@withContext result
        }

    @Throws(Exception::class)
    suspend fun dismissCurrentNotice() {
        val currentNotice = database.getLastNotice()
        database.createNotice(currentNotice!!.copy(dismissed = true))
    }

    fun imgToUrl(img: String, ext: String, isThumb: Boolean): String {
        var imageType = "image/"
        if (isThumb) {
            imageType = "thumb/"
        }
        return imageCDN + imageType + img + ext
    }

    fun getForumName(forumId: Int): String {
        return database.getForumName(forumId.toString())
    }

    /**
     * 将时间字符串格式化为友好的相对时间
     *
     * @param originalTime 要格式化的时间字符串，格式如"2021-02-21(日)18:35:24"
     * @param inThread 是否显示具体时间（小时和分钟）
     * @return 格式化后的时间字符串，例如"1小时前"或"昨天 12:30"或"2021年2月21日 12:30"
     */
    fun formatTime(originalTime: String, inThread: Boolean): String {
        val timeZone = TimeZone.of("UTC+08:00")
        val originalTimeInISO = originalTime.replace(Regex("\\((.+?)\\)"), "T")
        val time = LocalDateTime.parse(originalTimeInISO)
        val currentInstant = Clock.System.now()
        val current = currentInstant.toLocalDateTime(timeZone)
        val timeInstant = time.toInstant(timeZone)

        val date = time.date.atTime(12, 0, 0, 0).toInstant(timeZone)
        val currentDate = currentInstant.toLocalDateTime(timeZone).date
            .atTime(12, 0, 0, 0).toInstant(timeZone)
        val diffInDay = date.periodUntil(currentDate, timeZone)
        val duration = currentInstant - timeInstant
        var result: String

        result = if (diffInDay.days < 1 && diffInDay.months == 0 && diffInDay.years == 0) {
            if (duration.inWholeHours < 1) {
                if (duration.inWholeMinutes < 1) {
                    duration.inWholeSeconds.toString() + "秒前"
                } else {
                    duration.inWholeMinutes.toString() + "分钟前"
                }
            } else {
                duration.inWholeHours.toString() + "小时前"
            }
        } else if (diffInDay.months == 0) {
            when (diffInDay.days) {
                -2 -> "后天"
                -1 -> "明天"
                1 -> "昨天"
                2 -> "前天"
                else -> time.monthNumber.toString() + "月" + time.dayOfMonth + "日"
            }
        } else {
            if (time.year == current.year) {
                time.monthNumber.toString() + "月" + time.dayOfMonth + "日"
            } else {
                time.year.toString() + "年" + time.monthNumber + "月" + time.dayOfMonth + "日"
            }
        }

        if (inThread) {
            if (diffInDay.days >= 1 || diffInDay.months != 0) {
                result =
                    result + " " + time.hour.let { if (it < 10) "0$it" else it } + ":" + time.minute.let { if (it < 10) "0$it" else it }
            } else if (duration.inWholeHours >= 1) {
                result = time.hour.let { if (it < 10) "0$it" else it.toString() } + ":" + time.minute.let { if (it < 10) "0$it" else it }
            }
        }

        return result
    }

    /**
     * 将公告中的时间转化为易于阅读的形式
     *
     * @param originalTime 要格式化的时间，如"20210221180000"
     * @return 格式化后的字符串
     */
    fun formatTime(originalTime: Long): String {
        val timeZone = TimeZone.of("UTC+08:00")
        val originalTimeInISO = StringBuilder(originalTime.toString())
            .insert(4,'-')
            .insert(7,'-')
            .insert(10,'T')
            .insert(13,':')
            .insert(16,":0")
            .toString()
        val time = LocalDateTime.parse(originalTimeInISO)
        val currentInstant = Clock.System.now()
        val current = currentInstant.toLocalDateTime(timeZone)

        val date = time.date.atTime(12, 0, 0, 0).toInstant(timeZone)
        val currentDate = currentInstant.toLocalDateTime(timeZone).date
            .atTime(12, 0, 0, 0).toInstant(timeZone)
        val diffInDay = date.periodUntil(currentDate, timeZone)

        var result: String = if (diffInDay.months == 0 && time.year == current.year) {
            when (diffInDay.days) {
                -2 -> "后天"
                -1 -> "明天"
                0 -> "今天"
                1 -> "昨天"
                2 -> "前天"
                else -> time.monthNumber.toString() + "月" + time.dayOfMonth + "日"
            }
        } else {
            if (time.year == current.year) {
                time.monthNumber.toString() + "月" + time.dayOfMonth + "日"
            } else {
                time.year.toString() + "年" + time.monthNumber + "月" + time.dayOfMonth + "日"
            }
        }

        return result
    }
}