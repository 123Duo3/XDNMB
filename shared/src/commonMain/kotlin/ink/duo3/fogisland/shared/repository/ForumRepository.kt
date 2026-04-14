package ink.duo3.fogisland.shared.repository

import ink.duo3.fogisland.shared.model.CatalogSource
import ink.duo3.fogisland.shared.model.CatalogType
import ink.duo3.fogisland.shared.model.CacheCleanupTtlPolicy
import ink.duo3.fogisland.shared.model.DirectThreadShortcut
import ink.duo3.fogisland.shared.model.ForumBoard
import ink.duo3.fogisland.shared.model.ForumGroup
import ink.duo3.fogisland.shared.model.HiddenTimelineForumFilter
import ink.duo3.fogisland.shared.model.NmbPost
import ink.duo3.fogisland.shared.model.PostingDraftEntry
import ink.duo3.fogisland.shared.model.PostingDraftType
import ink.duo3.fogisland.shared.model.PostingHistoryEntry
import ink.duo3.fogisland.shared.model.PostingHistoryType
import ink.duo3.fogisland.shared.model.ReplyPostRequest
import ink.duo3.fogisland.shared.model.ReplyPostResult
import ink.duo3.fogisland.shared.model.ResolvedPostReference
import ink.duo3.fogisland.shared.model.SearchHit
import ink.duo3.fogisland.shared.model.SearchHitType
import ink.duo3.fogisland.shared.model.SiteNotice
import ink.duo3.fogisland.shared.model.ThreadDetail
import ink.duo3.fogisland.shared.model.ThreadReadProgress
import ink.duo3.fogisland.shared.model.ThreadPostRequest
import ink.duo3.fogisland.shared.model.ThreadPostResult
import ink.duo3.fogisland.shared.model.Timeline
import ink.duo3.fogisland.shared.network.api.NmbApiClient
import ink.duo3.fogisland.shared.network.api.NmbApiResponseException
import ink.duo3.fogisland.shared.network.model.ForumBoardDto
import ink.duo3.fogisland.shared.network.model.ForumGroupDto
import ink.duo3.fogisland.shared.network.model.PostDto
import ink.duo3.fogisland.shared.network.model.ThreadDto
import ink.duo3.fogisland.shared.network.model.TimelineDto
import ink.duo3.fogisland.shared.storage.db.dao.ForumRefreshDao
import ink.duo3.fogisland.shared.storage.db.dao.PostingDraftDao
import ink.duo3.fogisland.shared.storage.db.dao.PostingHistoryDao
import ink.duo3.fogisland.shared.storage.db.dao.PostDao
import ink.duo3.fogisland.shared.storage.db.dao.SubscriptionThreadDao
import ink.duo3.fogisland.shared.storage.db.dao.ThreadDao
import ink.duo3.fogisland.shared.storage.db.dao.ThreadReadProgressDao
import ink.duo3.fogisland.shared.storage.db.entity.CatalogEntryEntity
import ink.duo3.fogisland.shared.storage.db.entity.PostingDraftEntity
import ink.duo3.fogisland.shared.storage.db.entity.PostingHistoryEntity
import ink.duo3.fogisland.shared.storage.db.entity.PostEntity
import ink.duo3.fogisland.shared.storage.db.entity.SubscriptionThreadEntity
import ink.duo3.fogisland.shared.storage.db.entity.ThreadEntity
import ink.duo3.fogisland.shared.storage.db.entity.ThreadReadProgressEntity
import ink.duo3.fogisland.shared.storage.preferences.CatalogIndexCache
import ink.duo3.fogisland.shared.storage.preferences.CatalogIndexSnapshot
import ink.duo3.fogisland.shared.storage.preferences.ForumPreferences
import ink.duo3.fogisland.shared.util.calculateNmbThreadMaxPage
import ink.duo3.fogisland.shared.util.escapeSqlLikeArgument
import ink.duo3.fogisland.shared.util.htmlToPlainText
import ink.duo3.fogisland.shared.util.isNmbTipsPost
import ink.duo3.fogisland.shared.util.normalizeNmbStoredName
import ink.duo3.fogisland.shared.util.normalizeNmbStoredTitle
import ink.duo3.fogisland.shared.util.parseNmbNoticeDateEpochMillis
import ink.duo3.fogisland.shared.util.parseNmbPostedAtEpochMillis
import ink.duo3.fogisland.shared.util.parseNmbThreadIdInput
import ink.duo3.fogisland.shared.util.NMB_IMAGE_CDN_BASE_URL
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal fun resolveCleanupExpireBefore(
    policy: CacheCleanupTtlPolicy,
    nowEpochMillis: Long
): Long? {
    val ttlMillis = policy.ttlMillis() ?: return null
    return nowEpochMillis - ttlMillis
}

class ForumRepository(
    private val apiClient: NmbApiClient,
    private val threadDao: ThreadDao,
    private val postDao: PostDao,
    private val forumRefreshDao: ForumRefreshDao,
    private val subscriptionThreadDao: SubscriptionThreadDao,
    private val postingDraftDao: PostingDraftDao,
    private val postingHistoryDao: PostingHistoryDao,
    private val threadReadProgressDao: ThreadReadProgressDao,
    private val catalogIndexCache: CatalogIndexCache,
    private val forumPreferences: ForumPreferences
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
        val resolvedNotice = if (notice.enabled == true) {
            notice.content.orEmpty()
                .trim()
                .takeIf { it.isNotEmpty() }
                ?.let { contentHtml ->
                    SiteNotice(
                        contentHtml = contentHtml,
                        contentText = htmlToPlainText(contentHtml),
                        publishedAt = notice.date?.let(::parseNmbNoticeDateEpochMillis)
                    )
                }
        } else {
            null
        }

        catalogIndexCache.updateSiteNotice(resolvedNotice)
        return resolvedNotice
    }

    suspend fun getDismissedSiteNoticeContent(): String? {
        return forumPreferences.getDismissedSiteNoticeContent()
    }

    suspend fun updateDismissedSiteNoticeContent(content: String?) {
        forumPreferences.updateDismissedSiteNoticeContent(content)
    }

    suspend fun hideThread(threadId: Long) {
        forumPreferences.addHiddenThreadId(threadId)
    }

    suspend fun unhideThread(threadId: Long) {
        forumPreferences.removeHiddenThreadId(threadId)
    }

    suspend fun hideTimelineForum(timelineId: Long, forumId: Long) {
        forumPreferences.addHiddenTimelineForumFilter(timelineId, forumId)
    }

    suspend fun unhideTimelineForum(timelineId: Long, forumId: Long) {
        forumPreferences.removeHiddenTimelineForumFilter(timelineId, forumId)
    }

    fun observeHiddenThreadIds(): Flow<Set<Long>> {
        return forumPreferences.hiddenThreadIdsFlow
    }

    fun observeHiddenTimelineForumFilters(): Flow<Set<HiddenTimelineForumFilter>> {
        return forumPreferences.hiddenTimelineForumFiltersFlow
    }

    fun observeFavoriteForumIds(): Flow<Set<Long>> {
        return forumPreferences.favoriteForumIdsFlow
    }

    suspend fun addFavoriteForum(forumId: Long) {
        forumPreferences.addFavoriteForumId(forumId)
    }

    suspend fun removeFavoriteForum(forumId: Long) {
        forumPreferences.removeFavoriteForumId(forumId)
    }

    fun observeFavoriteTimelineIds(): Flow<Set<Long>> {
        return forumPreferences.favoriteTimelineIdsFlow
    }

    suspend fun addFavoriteTimeline(timelineId: Long) {
        forumPreferences.addFavoriteTimelineId(timelineId)
    }

    suspend fun removeFavoriteTimeline(timelineId: Long) {
        forumPreferences.removeFavoriteTimelineId(timelineId)
    }

    suspend fun refreshCatalog(
        source: CatalogSource,
        page: Int,
        replaceLoadedPages: Boolean = false
    ) {
        require(!replaceLoadedPages || page == 1) {
            "Replacing loaded catalog pages is only supported from page 1."
        }
        val timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val threads = when (source.type) {
            CatalogType.FORUM -> apiClient.getForumThreads(source.id, page)
            CatalogType.TIMELINE -> apiClient.getTimelineThreads(source.id, page)
        }
        cacheCatalogPage(
            catalogType = source.type.name,
            catalogId = source.id,
            page = page,
            threads = threads,
            refreshedAt = timestamp,
            replaceLoadedPages = replaceLoadedPages
        )
    }

    suspend fun refreshSubscriptions(page: Int) {
        val timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val uuid = forumPreferences.getOrCreateSubscriptionUuid()
        val threads = apiClient.getFeedThreads(uuid = uuid, page = page)
        forumRefreshDao.replaceSubscriptionPage(
            page = page,
            threads = threads.mapIndexed { index, thread ->
                thread.toSubscriptionThreadEntity(
                    page = page,
                    position = index,
                    refreshedAt = timestamp
                )
            }
        )
    }

    fun observeSubscriptions(): Flow<List<NmbPost>> {
        return subscriptionThreadDao.observeAll().map { entries ->
            entries.map { it.toModel() }
        }
    }

    fun observeSubscriptionUuid(): Flow<String?> {
        return forumPreferences.subscriptionUuidFlow
    }

    fun observeRecentSearches(): Flow<List<String>> {
        return forumPreferences.recentSearchesFlow
    }

    suspend fun cleanupExpiredCache() {
        val expireBefore = resolveCleanupExpireBefore(
            policy = forumPreferences.getCacheCleanupTtlPolicy(),
            nowEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds()
        ) ?: return
        forumRefreshDao.pruneExpiredCache(expireBefore)
    }

    suspend fun cleanupExpiredReadHistory() {
        val expireBefore = resolveCleanupExpireBefore(
            policy = forumPreferences.getReadHistoryCleanupTtlPolicy(),
            nowEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds()
        ) ?: return
        threadReadProgressDao.deleteExpired(expireBefore)
    }

    suspend fun getImageCdnFallbackBaseUrl(): String? {
        val preferredBaseUrl = apiClient
            .getPreferredImageCdnBaseUrl()
            ?.trim()
            ?.removeSuffix("/")
            ?: return null
        val hardcodedBaseUrl = NMB_IMAGE_CDN_BASE_URL
            .trim()
            .removeSuffix("/")
        return preferredBaseUrl.takeIf { it != hardcodedBaseUrl }
    }

    suspend fun ensureSubscriptionUuid(): String {
        return forumPreferences.getOrCreateSubscriptionUuid()
    }

    suspend fun updateSubscriptionUuid(uuid: String) {
        forumPreferences.updateSubscriptionUuid(uuid)
        subscriptionThreadDao.deleteAll()
    }

    suspend fun recordRecentSearch(query: String) {
        forumPreferences.recordRecentSearch(query)
    }

    suspend fun clearRecentSearches() {
        forumPreferences.clearRecentSearches()
    }

    suspend fun resolveDirectThreadShortcut(query: String): DirectThreadShortcut? {
        val threadId = parseNmbThreadIdInput(query) ?: return null
        val cachedThread = threadDao.getThreadById(threadId)
        return if (cachedThread != null) {
            DirectThreadShortcut(
                threadId = threadId,
                forumId = cachedThread.forumId,
                userHash = cachedThread.userHash,
                name = cachedThread.name,
                title = cachedThread.title,
                preview = buildPreviewSnippet(
                    title = cachedThread.title,
                    contentText = cachedThread.contentText
                ),
                postedAtEpochMillis = cachedThread.postedAtEpochMillis,
                isCached = true
            )
        } else {
            DirectThreadShortcut(
                threadId = threadId,
                forumId = null,
                userHash = "",
                name = null,
                title = null,
                preview = "",
                postedAtEpochMillis = null,
                isCached = false
            )
        }
    }

    suspend fun addSubscription(threadId: Long): String {
        val uuid = forumPreferences.getOrCreateSubscriptionUuid()
        val result = apiClient.addFeed(uuid = uuid, threadId = threadId)
        refreshSubscriptions(page = 1)
        return result
    }

    suspend fun deleteSubscription(threadId: Long): String {
        val uuid = forumPreferences.getOrCreateSubscriptionUuid()
        val result = apiClient.deleteFeed(uuid = uuid, threadId = threadId)
        subscriptionThreadDao.deleteThread(threadId)
        return result
    }

    suspend fun postThread(request: ThreadPostRequest): ThreadPostResult {
        val result = apiClient.postThread(request)
        runCatching { recordPostedThread(request, result) }
        runCatching {
            refreshCatalog(
                source = CatalogSource(
                    type = CatalogType.FORUM,
                    id = request.forumId,
                    title = "",
                    subtitle = null
                ),
                page = 1
            )
        }
        return result
    }

    suspend fun postReply(request: ReplyPostRequest): ReplyPostResult {
        val result = apiClient.postReply(request)
        runCatching { recordPostedReply(request, result) }
        runCatching {
            refreshThread(request.threadId, page = 1)
        }
        return result
    }

    suspend fun refreshThread(threadId: Long, page: Int): ThreadRefreshResult {
        val timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val thread = apiClient.getThreadDetails(threadId, page)
        val realReplies = thread.replies.filterNot { it.isTipsPost() }
        val totalReplyCount = thread.replyCount ?: 0
        val maxPage = calculateNmbThreadMaxPage(totalReplyCount)
        forumRefreshDao.replaceThreadPage(
            thread = thread.toThreadEntity(timestamp),
            threadId = threadId,
            page = page,
            maxPage = maxPage,
            posts = thread.replies.mapIndexed { index, reply ->
                reply.toPostEntity(
                    threadId = threadId,
                    fallbackForumId = thread.forumId,
                    page = page,
                    position = index,
                    refreshedAt = timestamp
                )
            }
        )

        return ThreadRefreshResult(
            page = page,
            realReplyCount = realReplies.size,
            totalReplyCount = totalReplyCount
        )
    }

    fun observeCatalog(source: CatalogSource): Flow<List<NmbPost>> {
        return combine(
            threadDao.observeCatalog(source.type.name, source.id),
            forumPreferences.hiddenThreadIdsFlow,
            forumPreferences.hiddenTimelineForumFiltersFlow
        ) { entries, hiddenThreadIds, hiddenTimelineForumFilters ->
            entries.map { it.toModel() }
                .filterNot { thread ->
                    thread.threadId in hiddenThreadIds ||
                        (
                            source.type == CatalogType.TIMELINE &&
                                thread.forumId != null &&
                                HiddenTimelineForumFilter(
                                    timelineId = source.id,
                                    forumId = thread.forumId
                                ) in hiddenTimelineForumFilters
                            )
                }
        }
    }

    fun observeThreadDetail(threadId: Long): Flow<ThreadDetail> {
        return combine(
            threadDao.observeThreadById(threadId),
            postDao.observePostsByThread(threadId),
            threadReadProgressDao.observeByThreadId(threadId)
        ) { thread, posts, progress ->
            val threadModel = thread?.toModel()
            ThreadDetail(
                thread = threadModel,
                posts = posts.map { it.toModel(opUserHash = threadModel?.userHash) },
                progress = progress?.toModel()
            )
        }
    }

    suspend fun getCachedThreadLoadedPage(threadId: Long): Int {
        if (threadDao.getThreadById(threadId) == null) {
            return 0
        }
        return postDao.getMaxLoadedPageForThread(threadId) ?: 0
    }

    fun observeReadHistory(): Flow<List<NmbPost>> = threadReadProgressDao.observeReadHistory()

    fun observePostingHistory(): Flow<List<PostingHistoryEntry>> {
        return postingHistoryDao.observeAll().map { entries ->
            entries.map { it.toPostingHistoryEntry() }
        }
    }

    fun observePostingDrafts(): Flow<List<PostingDraftEntry>> {
        return postingDraftDao.observeAll().map { entries ->
            entries.map { it.toPostingDraftEntry() }
        }
    }

    suspend fun getPostingDrafts(): List<PostingDraftEntry> {
        return postingDraftDao.getAll().map { it.toPostingDraftEntry() }
    }

    suspend fun getPostingDraft(id: Long): PostingDraftEntry? {
        return postingDraftDao.getById(id)?.toPostingDraftEntry()
    }

    suspend fun deleteReadHistoryEntry(threadId: Long) {
        threadReadProgressDao.deleteByThreadId(threadId)
    }

    suspend fun clearReadHistory() {
        threadReadProgressDao.deleteAll()
    }

    suspend fun deletePostingHistoryEntry(id: Long) {
        postingHistoryDao.deleteById(id)
    }

    suspend fun clearPostingHistory(type: PostingHistoryType? = null) {
        if (type == null) {
            postingHistoryDao.deleteAll()
        } else {
            postingHistoryDao.deleteByType(type.name)
        }
    }

    suspend fun saveThreadDraft(
        draftId: Long?,
        request: ThreadPostRequest,
        imagePath: String?
    ): Long {
        val timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val insertedId = postingDraftDao.insert(
            PostingDraftEntity(
                id = draftId ?: 0L,
                type = PostingDraftType.THREAD.name,
                threadId = null,
                forumId = request.forumId,
                threadTitle = "",
                name = request.name,
                email = request.email,
                title = request.title,
                contentText = request.content,
                useWatermark = request.useWatermark,
                imagePath = imagePath,
                imageFileName = request.image?.fileName,
                imageMimeType = request.image?.mimeType,
                updatedAt = timestamp
            )
        )
        return if (draftId != null && draftId != 0L) draftId else insertedId
    }

    suspend fun saveReplyDraft(
        draftId: Long?,
        request: ReplyPostRequest,
        imagePath: String?
    ): Long {
        val timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val cachedThread = threadDao.getThreadById(request.threadId)
        val insertedId = postingDraftDao.insert(
            PostingDraftEntity(
                id = draftId ?: 0L,
                type = PostingDraftType.REPLY.name,
                threadId = request.threadId,
                forumId = cachedThread?.forumId,
                threadTitle = cachedThread?.title.orEmpty(),
                name = request.name,
                email = request.email,
                title = request.title,
                contentText = request.content,
                useWatermark = request.useWatermark,
                imagePath = imagePath,
                imageFileName = request.image?.fileName,
                imageMimeType = request.image?.mimeType,
                updatedAt = timestamp
            )
        )
        return if (draftId != null && draftId != 0L) draftId else insertedId
    }

    suspend fun deletePostingDraft(id: Long) {
        postingDraftDao.deleteById(id)
    }

    suspend fun clearPostingDrafts() {
        postingDraftDao.deleteAll()
    }

    suspend fun getReadProgress(threadId: Long): ThreadReadProgress? {
        return threadReadProgressDao.getByThreadId(threadId)?.toModel()
    }

    suspend fun resolveThreadIdByPostReference(postId: Long): Long? {
        threadDao.getThreadById(postId)?.let { return it.id }
        return postDao.getThreadIdByRemoteId(postId)
    }

    suspend fun getCachedPostReference(postId: Long): ResolvedPostReference? {
        threadDao.getThreadById(postId)?.let { thread ->
            return ResolvedPostReference(
                threadId = thread.id,
                post = thread.toModel()
            )
        }

        val post = postDao.getPostByRemoteId(postId) ?: return null
        val opUserHash = threadDao.getThreadById(post.threadId)?.userHash
        return ResolvedPostReference(
            threadId = post.threadId,
            post = post.toModel(opUserHash)
        )
    }

    suspend fun queryPostReference(
        postId: Long,
        preferredThreadId: Long? = null
    ): ResolvedPostReference? {
        getCachedPostReference(postId)?.let { return it }

        preferredThreadId?.let { threadId ->
            val result = runCatching {
                queryPostReferenceInThread(
                    threadId = threadId,
                    postId = postId
                )
            }
            result.getOrNull()?.let { return it }
            result.exceptionOrNull()
                ?.takeUnless { it.isMissingPostReferenceTarget() }
                ?.let { throw it }
        }

        if (preferredThreadId != postId) {
            val result = runCatching {
                queryPostReferenceInThread(
                    threadId = postId,
                    postId = postId
                )
            }
            result.getOrNull()?.let { return it }
            result.exceptionOrNull()
                ?.takeUnless { it.isMissingPostReferenceTarget() }
                ?.let { throw it }
        }

        return getCachedPostReference(postId)
    }

    suspend fun queryPostReferenceInThread(
        threadId: Long,
        postId: Long
    ): ResolvedPostReference? {
        getCachedPostReference(postId)
            ?.takeIf { it.threadId == threadId }
            ?.let { return it }

        var nextPage = if (threadDao.getThreadById(threadId) != null) {
            (postDao.getMaxLoadedPageForThread(threadId) ?: 0) + 1
        } else {
            1
        }.coerceAtLeast(1)
        var knownMaxPage: Int? = null

        while (knownMaxPage == null || nextPage <= knownMaxPage) {
            val result = refreshThread(threadId, nextPage)
            knownMaxPage = result.maxPage

            getCachedPostReference(postId)
                ?.takeIf { it.threadId == threadId }
                ?.let { return it }

            if (result.reachedEnd) {
                break
            }
            nextPage += 1
        }

        return getCachedPostReference(postId)
            ?.takeIf { it.threadId == threadId }
    }

    private fun Throwable.isMissingPostReferenceTarget(): Boolean {
        return this is NmbApiResponseException && presentation.summary.contains("该串不存在")
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
        val escapedQuery = escapeSqlLikeArgument(normalizedQuery)

        val threadHits = threadDao.searchThreads(escapedQuery).map { thread ->
            SearchHit(
                type = SearchHitType.THREAD,
                threadId = thread.id,
                forumId = thread.forumId,
                userHash = thread.userHash,
                name = thread.name,
                title = thread.title,
                preview = buildSearchPreview(
                    title = thread.title,
                    contentText = thread.contentText,
                    query = normalizedQuery
                ),
                postedAtEpochMillis = thread.postedAtEpochMillis,
                sage = thread.sage,
                admin = thread.admin,
                hide = thread.hide,
                page = null,
                refreshedAt = thread.refreshedAt
            )
        }

        val postHits = postDao.searchPosts(escapedQuery).map { post ->
            SearchHit(
                type = SearchHitType.POST,
                threadId = post.threadId,
                postId = post.id,
                forumId = post.forumId,
                userHash = post.userHash,
                name = post.name,
                title = post.title,
                preview = buildSearchPreview(
                    title = post.title,
                    contentText = post.contentText,
                    query = normalizedQuery
                ),
                postedAtEpochMillis = post.postedAtEpochMillis,
                sage = post.sage,
                admin = post.admin,
                hide = post.hide,
                page = post.page,
                refreshedAt = post.refreshedAt
            )
        }

        return (threadHits + postHits)
            .sortedByDescending { it.postedAtEpochMillis ?: it.refreshedAt }
            .distinctBy { "${it.type}:${it.threadId}:${it.postId}" }
    }

    private fun buildSearchPreview(
        title: String?,
        contentText: String,
        query: String
    ): String {
        val normalizedSource = buildPreviewSnippet(
            title = title,
            contentText = contentText
        )
        if (normalizedSource.isEmpty()) {
            return ""
        }
        val matchIndex = normalizedSource.indexOf(query, ignoreCase = true)
        if (matchIndex < 0) {
            return normalizedSource.take(120)
        }

        val start = (matchIndex - 24).coerceAtLeast(0)
        val end = (matchIndex + query.length + 72).coerceAtMost(normalizedSource.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < normalizedSource.length) "…" else ""
        return prefix + normalizedSource.substring(start, end).trim() + suffix
    }

    private fun buildPreviewSnippet(
        title: String?,
        contentText: String
    ): String {
        val source = when {
            contentText.isNotBlank() -> contentText
            !title.isNullOrBlank() -> title
            else -> ""
        }.trim()
        if (source.isEmpty()) {
            return ""
        }

        return source.replace("\n", " ").replace(Regex("\\s+"), " ")
    }

    private suspend fun cacheCatalogPage(
        catalogType: String,
        catalogId: Long,
        page: Int,
        threads: List<ThreadDto>,
        refreshedAt: Long,
        replaceLoadedPages: Boolean = false
    ) {
        val threadEntities = threads.map { it.toThreadEntity(refreshedAt) }
        val cachedReplies = threads.flatMap { thread ->
            thread.replies
                .filterNot { it.isTipsPost() }
                .mapIndexed { index, reply ->
                    reply.toPostEntity(
                        threadId = thread.id,
                        fallbackForumId = thread.forumId,
                        page = null,
                        position = index,
                        refreshedAt = refreshedAt
                    )
                }
        }
        val catalogEntries = threads.mapIndexed { index, thread ->
            CatalogEntryEntity(
                catalogType = catalogType,
                catalogId = catalogId,
                threadId = thread.id,
                page = page,
                position = index,
                refreshedAt = refreshedAt
            )
        }
        if (replaceLoadedPages) {
            forumRefreshDao.replaceCatalogFromFirstPage(
                catalogType = catalogType,
                catalogId = catalogId,
                threads = threadEntities,
                posts = cachedReplies,
                entries = catalogEntries
            )
        } else {
            forumRefreshDao.replaceCatalogPage(
                catalogType = catalogType,
                catalogId = catalogId,
                page = page,
                threads = threadEntities,
                posts = cachedReplies,
                entries = catalogEntries
            )
        }
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
        val isTips = isNmbTipsPost(
            userHash = userHash,
            remotePostId = id,
            postedAtRaw = postedAtRaw
        )
        val normalizedImage = image?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedExt = imageExtension?.trim()?.takeIf { it.isNotEmpty() }
        return ThreadEntity(
            id = id,
            forumId = forumId,
            userHash = userHash.orEmpty(),
            name = normalizeNmbStoredName(name),
            title = normalizeNmbStoredTitle(title),
            contentHtml = contentHtml,
            contentText = htmlToPlainText(contentHtml),
            image = normalizedImage,
            ext = normalizedExt?.takeIf { normalizedImage != null },
            postedAtEpochMillis = postedAtRaw?.let(::parseNmbPostedAtEpochMillis),
            sage = sage ?: false,
            admin = admin ?: false,
            hide = hide ?: false,
            isTips = isTips,
            replyCount = replyCount ?: 0,
            remainReplies = remainReplies,
            refreshedAt = refreshedAt
        )
    }

    private fun ThreadDto.toSubscriptionThreadEntity(
        page: Int,
        position: Int,
        refreshedAt: Long
    ): SubscriptionThreadEntity {
        val contentHtml = content.orEmpty()
        val isTips = isNmbTipsPost(
            userHash = userHash,
            remotePostId = id,
            postedAtRaw = postedAtRaw
        )
        val normalizedImage = image?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedExt = imageExtension?.trim()?.takeIf { it.isNotEmpty() }
        return SubscriptionThreadEntity(
            threadId = id,
            forumId = forumId,
            userHash = userHash.orEmpty(),
            name = normalizeNmbStoredName(name),
            title = normalizeNmbStoredTitle(title),
            contentHtml = contentHtml,
            contentText = htmlToPlainText(contentHtml),
            image = normalizedImage,
            ext = normalizedExt?.takeIf { normalizedImage != null },
            postedAtEpochMillis = postedAtRaw?.let(::parseNmbPostedAtEpochMillis),
            sage = sage ?: false,
            admin = admin ?: false,
            hide = hide ?: false,
            isTips = isTips,
            replyCount = replyCount ?: 0,
            remainReplies = remainReplies,
            page = page,
            positionInPage = position,
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
        val normalizedImage = image?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedExt = imageExtension?.trim()?.takeIf { it.isNotEmpty() }
        return PostEntity(
            threadId = threadId,
            id = storageId(page = page, position = position, isTips = isTips),
            remoteId = id,
            forumId = forumId ?: fallbackForumId,
            replyCount = replyCount,
            userHash = userHash.orEmpty(),
            name = normalizeNmbStoredName(name),
            title = normalizeNmbStoredTitle(title),
            contentHtml = contentHtml,
            contentText = htmlToPlainText(contentHtml),
            image = normalizedImage,
            ext = normalizedExt?.takeIf { normalizedImage != null },
            postedAtEpochMillis = postedAtRaw?.let(::parseNmbPostedAtEpochMillis),
            sage = sage ?: false,
            admin = admin ?: false,
            hide = hide ?: false,
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
            postedAtRaw = postedAtRaw
        )
    }

    private fun PostDto.storageId(page: Int?, position: Int, isTips: Boolean): Long {
        if (!isTips) {
            return id
        }

        return page?.let { -it.toLong() } ?: (Long.MIN_VALUE + position)
    }

    private fun ThreadEntity.toModel(): NmbPost {
        return NmbPost(
            id = id,
            threadId = id,
            remoteId = id,
            forumId = forumId,
            replyCount = replyCount,
            userHash = userHash,
            name = name,
            title = title,
            contentHtml = contentHtml,
            contentText = contentText,
            image = image,
            ext = ext,
            postedAtEpochMillis = postedAtEpochMillis,
            sage = sage,
            admin = admin,
            hide = hide,
            isTips = isTips,
            isPoster = true,
            isThread = true,
            page = null,
            positionInPage = 0,
            remainReplies = remainReplies,
            refreshedAt = refreshedAt
        )
    }

    private fun PostEntity.toModel(opUserHash: String?): NmbPost {
        return NmbPost(
            id = id,
            threadId = threadId,
            remoteId = remoteId,
            forumId = forumId,
            replyCount = replyCount,
            userHash = userHash,
            name = name,
            title = title,
            contentHtml = contentHtml,
            contentText = contentText,
            image = image,
            ext = ext,
            postedAtEpochMillis = postedAtEpochMillis,
            sage = sage,
            admin = admin,
            hide = hide,
            isTips = isTips,
            isPoster = !opUserHash.isNullOrBlank() && userHash == opUserHash,
            isThread = false,
            page = page,
            positionInPage = positionInPage,
            remainReplies = null,
            refreshedAt = refreshedAt
        )
    }

    private fun SubscriptionThreadEntity.toModel(): NmbPost {
        return NmbPost(
            id = threadId,
            threadId = threadId,
            remoteId = threadId,
            forumId = forumId,
            replyCount = replyCount,
            userHash = userHash,
            name = name,
            title = title,
            contentHtml = contentHtml,
            contentText = contentText,
            image = image,
            ext = ext,
            postedAtEpochMillis = postedAtEpochMillis,
            sage = sage,
            admin = admin,
            hide = hide,
            isTips = isTips,
            isPoster = true,
            isThread = true,
            page = page,
            positionInPage = positionInPage,
            remainReplies = remainReplies,
            refreshedAt = refreshedAt
        )
    }

    private fun ThreadReadProgressEntity.toModel(): ThreadReadProgress {
        return ThreadReadProgress(
            threadId = threadId,
            lastReadPage = lastReadPage,
            lastReadPostId = lastReadPostId,
            lastVisibleItemIndex = lastVisibleItemIndex,
            lastVisibleItemOffset = lastVisibleItemOffset,
            updatedAt = updatedAt
        )
    }

    private suspend fun recordPostedThread(
        request: ThreadPostRequest,
        result: ThreadPostResult
    ) {
        postingHistoryDao.insert(
            PostingHistoryEntity(
                type = PostingHistoryType.THREAD.name,
                threadId = result.threadId,
                postId = null,
                forumId = request.forumId,
                threadTitle = normalizeNmbStoredTitle(request.title),
                name = normalizeNmbStoredName(request.name),
                title = normalizeNmbStoredTitle(request.title),
                contentText = result.contentText,
                hasImage = request.image != null,
                createdAt = kotlin.time.Clock.System.now().toEpochMilliseconds()
            )
        )
    }

    private suspend fun recordPostedReply(
        request: ReplyPostRequest,
        result: ReplyPostResult
    ) {
        val cachedThread = threadDao.getThreadById(request.threadId)
        postingHistoryDao.insert(
            PostingHistoryEntity(
                type = PostingHistoryType.REPLY.name,
                threadId = request.threadId,
                postId = result.postId,
                forumId = cachedThread?.forumId,
                threadTitle = cachedThread?.title,
                name = normalizeNmbStoredName(request.name),
                title = normalizeNmbStoredTitle(request.title),
                contentText = result.contentText,
                hasImage = request.image != null,
                createdAt = kotlin.time.Clock.System.now().toEpochMilliseconds()
            )
        )
    }

    private fun PostingHistoryEntity.toPostingHistoryEntry(): PostingHistoryEntry {
        return PostingHistoryEntry(
            id = id,
            type = runCatching { PostingHistoryType.valueOf(type) }
                .getOrDefault(PostingHistoryType.THREAD),
            threadId = threadId,
            postId = postId,
            forumId = forumId,
            threadTitle = threadTitle,
            name = name,
            title = title,
            contentText = contentText,
            hasImage = hasImage,
            createdAt = createdAt
        )
    }

    private fun PostingDraftEntity.toPostingDraftEntry(): PostingDraftEntry {
        return PostingDraftEntry(
            id = id,
            type = runCatching { PostingDraftType.valueOf(type) }
                .getOrDefault(PostingDraftType.THREAD),
            threadId = threadId,
            forumId = forumId,
            threadTitle = threadTitle,
            name = name,
            email = email,
            title = title,
            contentText = contentText,
            useWatermark = useWatermark,
            imagePath = imagePath,
            imageFileName = imageFileName,
            imageMimeType = imageMimeType,
            updatedAt = updatedAt
        )
    }
}
