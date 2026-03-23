package ink.duo3.fogisland.shared.repository

import ink.duo3.fogisland.shared.model.CatalogSource
import ink.duo3.fogisland.shared.model.CatalogType
import ink.duo3.fogisland.shared.model.ForumBoard
import ink.duo3.fogisland.shared.model.ForumGroup
import ink.duo3.fogisland.shared.model.SearchHit
import ink.duo3.fogisland.shared.model.SearchHitType
import ink.duo3.fogisland.shared.model.SiteNotice
import ink.duo3.fogisland.shared.model.ThreadDetail
import ink.duo3.fogisland.shared.model.Timeline
import ink.duo3.fogisland.shared.network.api.NmbApiClient
import ink.duo3.fogisland.shared.network.model.ForumBoardDto
import ink.duo3.fogisland.shared.network.model.ForumGroupDto
import ink.duo3.fogisland.shared.network.model.PostDto
import ink.duo3.fogisland.shared.network.model.ThreadDto
import ink.duo3.fogisland.shared.network.model.TimelineDto
import ink.duo3.fogisland.shared.storage.db.dao.CatalogDao
import ink.duo3.fogisland.shared.storage.db.dao.PostDao
import ink.duo3.fogisland.shared.storage.db.dao.ThreadDao
import ink.duo3.fogisland.shared.storage.db.dao.ThreadReadProgressDao
import ink.duo3.fogisland.shared.storage.db.entity.CatalogEntryEntity
import ink.duo3.fogisland.shared.storage.db.entity.PostEntity
import ink.duo3.fogisland.shared.storage.db.entity.ThreadEntity
import ink.duo3.fogisland.shared.storage.db.entity.ThreadReadProgressEntity
import ink.duo3.fogisland.shared.storage.preferences.CatalogIndexCache
import ink.duo3.fogisland.shared.storage.preferences.CatalogIndexSnapshot
import ink.duo3.fogisland.shared.util.calculateNmbThreadMaxPage
import ink.duo3.fogisland.shared.util.htmlToPlainText
import ink.duo3.fogisland.shared.util.isNmbTipsPost
import ink.duo3.fogisland.shared.util.normalizeNmbStoredName
import ink.duo3.fogisland.shared.util.normalizeNmbStoredTitle
import ink.duo3.fogisland.shared.util.resolveNmbDisplayTitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ForumRepository(
    private val apiClient: NmbApiClient,
    private val threadDao: ThreadDao,
    private val postDao: PostDao,
    private val catalogDao: CatalogDao,
    private val threadReadProgressDao: ThreadReadProgressDao,
    private val catalogIndexCache: CatalogIndexCache
) {
    data class ThreadRefreshResult(
        val page: Int,
        val realReplyCount: Int,
        val totalReplyCount: Int
    ) {
        val maxPage: Int
            get() = calculateNmbThreadMaxPage(totalReplyCount)

        val reachedEnd: Boolean
            get() = realReplyCount == 0 || page >= maxPage
    }

    suspend fun getForumList(): List<ForumGroup> {
        return apiClient.getForumList().mapNotNull { group ->
            val groupId = group.id.toLongOrNull() ?: return@mapNotNull null
            ForumGroup(
                id = groupId,
                name = group.name,
                sort = group.sort.toIntOrNull() ?: Int.MAX_VALUE,
                status = group.status,
                forums = group.forums.mapNotNull { forum ->
                    forum.toForumBoard(groupId)
                }.sortedBy { it.sort ?: Int.MAX_VALUE }
            )
        }.sortedBy { it.sort }.also { forumGroups ->
            catalogIndexCache.updateForumGroups(forumGroups)
        }
    }

    suspend fun getTimelineList(): List<Timeline> {
        return apiClient.getTimelineList().map { timeline ->
            Timeline(
                id = timeline.id,
                name = timeline.name,
                displayName = timeline.displayName?.takeIf { it.isNotBlank() } ?: timeline.name,
                notice = timeline.notice.orEmpty(),
                maxPage = timeline.maxPage
            )
        }.also { timelines ->
            catalogIndexCache.updateTimelines(timelines)
        }
    }

    suspend fun getCachedCatalogIndex(): CatalogIndexSnapshot {
        return catalogIndexCache.read()
    }

    suspend fun getLastSelectedSource(): CatalogSource? {
        val source = catalogIndexCache.readLastSelectedSource() ?: return null
        return CatalogSource(
            type = source.type,
            id = source.id,
            title = "",
            subtitle = null
        )
    }

    suspend fun saveLastSelectedSource(source: CatalogSource) {
        catalogIndexCache.writeLastSelectedSource(source)
    }

    suspend fun getSiteNotice(): SiteNotice? {
        val notice = apiClient.getNotice()
        if (notice.enabled != true) {
            return null
        }

        val contentHtml = notice.content.orEmpty().trim()
        if (contentHtml.isEmpty()) {
            return null
        }

        return SiteNotice(
            contentHtml = contentHtml,
            contentText = htmlToPlainText(contentHtml),
            publishedAt = notice.date
        )
    }

    suspend fun refreshCatalog(source: CatalogSource, page: Int) {
        val timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val threads = when (source.type) {
            CatalogType.FORUM -> apiClient.getForumThreads(source.id, page)
            CatalogType.TIMELINE -> apiClient.getTimelineThreads(source.id, page)
        }

        catalogDao.deletePage(source.type.name, source.id, page)
        threadDao.insertThreads(threads.map { it.toThreadEntity(timestamp) })
        val cachedReplies = threads.flatMap { thread ->
            thread.replies
                .filterNot { it.isTipsPost() }
                .mapIndexed { index, reply ->
                    reply.toPostEntity(
                        threadId = thread.id,
                        fallbackForumId = thread.fid,
                        page = null,
                        position = index,
                        refreshedAt = timestamp
                    )
                }
        }
        postDao.insertPosts(cachedReplies)
        catalogDao.insertEntries(
            threads.mapIndexed { index, thread ->
                CatalogEntryEntity(
                    catalogType = source.type.name,
                    catalogId = source.id,
                    threadId = thread.id,
                    page = page,
                    position = index,
                    refreshedAt = timestamp
                )
            }
        )
    }

    suspend fun refreshThread(threadId: Long, page: Int): ThreadRefreshResult {
        val timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val thread = apiClient.getThreadDetails(threadId, page)
        val realReplies = thread.replies.filterNot { it.isTipsPost() }
        val totalReplyCount = thread.replyCount ?: 0
        val maxPage = calculateNmbThreadMaxPage(totalReplyCount)

        threadDao.insertThread(thread.toThreadEntity(timestamp))
        postDao.deleteThreadPage(threadId, page)
        postDao.insertPosts(
            thread.replies.mapIndexed { index, reply ->
                reply.toPostEntity(
                    threadId = threadId,
                    fallbackForumId = thread.fid,
                    page = page,
                    position = index,
                    refreshedAt = timestamp
                )
            }
        )
        postDao.deleteThreadPagesAfter(threadId, maxPage)

        return ThreadRefreshResult(
            page = page,
            realReplyCount = realReplies.size,
            totalReplyCount = totalReplyCount
        )
    }

    fun observeCatalog(source: CatalogSource): Flow<List<ThreadEntity>> {
        return threadDao.observeCatalog(source.type.name, source.id)
    }

    fun observeThreadDetail(threadId: Long): Flow<ThreadDetail> {
        return combine(
            threadDao.observeThreadById(threadId),
            postDao.observePostsByThread(threadId),
            threadReadProgressDao.observeByThreadId(threadId)
        ) { thread, posts, progress ->
            ThreadDetail(
                thread = thread,
                posts = posts,
                progress = progress
            )
        }
    }

    suspend fun getReadProgress(threadId: Long): ThreadReadProgressEntity? {
        return threadReadProgressDao.getByThreadId(threadId)
    }

    suspend fun updateReadProgress(
        threadId: Long,
        page: Int,
        postId: Long?,
        itemIndex: Int,
        itemOffset: Int
    ) {
        threadReadProgressDao.upsert(
            ThreadReadProgressEntity(
                threadId = threadId,
                lastReadPage = page,
                lastReadPostId = postId,
                lastVisibleItemIndex = itemIndex,
                lastVisibleItemOffset = itemOffset,
                updatedAt = kotlin.time.Clock.System.now().toEpochMilliseconds()
            )
        )
    }

    suspend fun searchCachedContent(query: String): List<SearchHit> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            return emptyList()
        }

        val threadHits = threadDao.searchThreads(normalizedQuery).map { thread ->
            SearchHit(
                type = SearchHitType.THREAD,
                threadId = thread.id,
                title = resolveNmbDisplayTitle(thread.title).orEmpty(),
                preview = thread.contentText
            )
        }

        val postHits = postDao.searchPosts(normalizedQuery).map { post ->
            SearchHit(
                type = SearchHitType.POST,
                threadId = post.threadId,
                postId = post.id,
                title = resolveNmbDisplayTitle(post.title).orEmpty(),
                preview = post.contentText
            )
        }

        return (threadHits + postHits).distinctBy { "${it.type}:${it.threadId}:${it.postId}" }
    }

    private fun ForumBoardDto.toForumBoard(groupId: Long): ForumBoard? {
        val forumId = id.toLongOrNull() ?: return null
        if (forumId <= 0L) {
            return null
        }

        val resolvedName = displayName?.takeIf { it.isNotBlank() } ?: name
        return ForumBoard(
            id = forumId,
            groupId = groupId,
            name = name,
            displayName = resolvedName,
            noticeHtml = notice.orEmpty(),
            noticeText = htmlToPlainText(notice),
            sort = sort?.toIntOrNull(),
            threadCount = threadCount?.toIntOrNull(),
            permissionLevel = permissionLevel?.toIntOrNull(),
            status = status
        )
    }

    private fun ThreadDto.toThreadEntity(refreshedAt: Long): ThreadEntity {
        val contentHtml = content.orEmpty()
        return ThreadEntity(
            id = id,
            forumId = fid,
            userHash = userHash.orEmpty(),
            name = normalizeNmbStoredName(name),
            title = normalizeNmbStoredTitle(title),
            contentHtml = contentHtml,
            contentText = htmlToPlainText(contentHtml),
            image = img.orEmpty(),
            ext = ext.orEmpty(),
            postedAt = now.orEmpty(),
            sage = sage ?: 0,
            admin = admin ?: 0,
            hide = hide ?: 0,
            replyCount = replyCount ?: 0,
            remainReplies = remainReplies,
            refreshedAt = refreshedAt
        )
    }

    private fun PostDto.toPostEntity(
        threadId: Long,
        fallbackForumId: Long?,
        page: Int?,
        position: Int,
        refreshedAt: Long
    ): PostEntity {
        val contentHtml = content.orEmpty()
        val isTips = isTipsPost()
        return PostEntity(
            threadId = threadId,
            id = storageId(page = page, position = position, isTips = isTips),
            remoteId = id,
            forumId = fid ?: fallbackForumId,
            replyCount = replyCount,
            userHash = userHash.orEmpty(),
            name = normalizeNmbStoredName(name),
            title = normalizeNmbStoredTitle(title),
            contentHtml = contentHtml,
            contentText = htmlToPlainText(contentHtml),
            image = img.orEmpty(),
            ext = ext.orEmpty(),
            postedAt = now.orEmpty(),
            sage = sage ?: 0,
            admin = admin ?: 0,
            hide = hide ?: 0,
            isTips = isTips,
            page = page,
            positionInPage = position,
            refreshedAt = refreshedAt
        )
    }

    private fun PostDto.isTipsPost(): Boolean {
        return isNmbTipsPost(
            userHash = userHash,
            remotePostId = id,
            postedAtRaw = now
        )
    }

    private fun PostDto.storageId(page: Int?, position: Int, isTips: Boolean): Long {
        if (!isTips) {
            return id
        }

        return page?.let { -it.toLong() } ?: (Long.MIN_VALUE + position)
    }
}
