package ink.duo3.fogisland.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ink.duo3.fogisland.shared.model.ErrorPresentation
import ink.duo3.fogisland.shared.model.CatalogSource
import ink.duo3.fogisland.shared.model.CatalogThread
import ink.duo3.fogisland.shared.model.DirectThreadShortcut
import ink.duo3.fogisland.shared.model.ForumGroup
import ink.duo3.fogisland.shared.model.PostingDraftEntry
import ink.duo3.fogisland.shared.model.PostingHistoryEntry
import ink.duo3.fogisland.shared.model.PostingHistoryType
import ink.duo3.fogisland.shared.model.ReadHistoryEntry
import ink.duo3.fogisland.shared.model.ReplyPostRequest
import ink.duo3.fogisland.shared.model.ReplyPostResult
import ink.duo3.fogisland.shared.model.SearchHit
import ink.duo3.fogisland.shared.model.SiteNotice
import ink.duo3.fogisland.shared.model.SubscriptionThread
import ink.duo3.fogisland.shared.model.ThreadDetail
import ink.duo3.fogisland.shared.model.ThreadPostRequest
import ink.duo3.fogisland.shared.model.ThreadPostResult
import ink.duo3.fogisland.shared.model.Timeline
import ink.duo3.fogisland.shared.model.cacheKey
import ink.duo3.fogisland.shared.model.resolveCatalogSource
import ink.duo3.fogisland.shared.network.api.toErrorPresentation
import ink.duo3.fogisland.shared.repository.RepositoryProvider
import ink.duo3.fogisland.shared.util.calculateNmbThreadMaxPage
import ink.duo3.fogisland.shared.util.parseNmbThreadIdInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class ForumBrowseUiState(
    val isLoadingIndex: Boolean = true,
    val isLoadingCatalog: Boolean = false,
    val isLoadingSubscriptions: Boolean = false,
    val isLoadingThread: Boolean = false,
    val isPostingThread: Boolean = false,
    val isPostingReply: Boolean = false,
    val activeThreadId: Long? = null,
    val forumGroups: List<ForumGroup> = emptyList(),
    val timelines: List<Timeline> = emptyList(),
    val currentSource: CatalogSource? = null,
    val threads: List<CatalogThread> = emptyList(),
    val subscriptionThreads: List<SubscriptionThread> = emptyList(),
    val readHistory: List<ReadHistoryEntry> = emptyList(),
    val postingHistory: List<PostingHistoryEntry> = emptyList(),
    val postingDrafts: List<PostingDraftEntry> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<SearchHit> = emptyList(),
    val directThreadShortcut: DirectThreadShortcut? = null,
    val recentSearches: List<String> = emptyList(),
    val loadedCatalogPage: Int = 0,
    val loadedSubscriptionPage: Int = 0,
    val siteNotice: SiteNotice? = null,
    val threadDetail: ThreadDetail = ThreadDetail(null, emptyList(), null),
    val loadedThreadPage: Int = 0,
    val hasReachedReplyEnd: Boolean = false,
    val error: ErrorPresentation? = null,
    val subscriptionError: ErrorPresentation? = null,
    val historyError: ErrorPresentation? = null,
    val postingHistoryError: ErrorPresentation? = null,
    val isSearching: Boolean = false,
    val searchError: ErrorPresentation? = null,
    val postThreadError: ErrorPresentation? = null,
    val postedThreadResult: ThreadPostResult? = null,
    val postReplyError: ErrorPresentation? = null,
    val postedReplyResult: ReplyPostResult? = null
) {
    val currentThread = threadDetail.thread
    val currentPosts = threadDetail.posts

    val canLoadMoreReplies: Boolean
        get() {
            val thread = currentThread ?: return false
            if (hasReachedReplyEnd) {
                return false
            }

            val maxReplyPage = calculateNmbThreadMaxPage(thread.replyCount)
            return loadedThreadPage < maxReplyPage
        }
}

class ForumBrowseViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.provideForumRepository(application)
    private val _uiState = MutableStateFlow(ForumBrowseUiState())
    val uiState: StateFlow<ForumBrowseUiState> = _uiState.asStateFlow()

    private val catalogPageCache = mutableMapOf<String, Int>()
    private val threadPageCache = mutableMapOf<Long, Int>()
    private val threadEndReachedCache = mutableMapOf<Long, Boolean>()
    private var subscriptionLoadedPage = 0
    private var currentSubscriptionUuid: String? = null
    private var catalogObservationJob: Job? = null
    private var subscriptionObservationJob: Job? = null
    private var readHistoryObservationJob: Job? = null
    private var postingDraftObservationJob: Job? = null
    private var postingHistoryObservationJob: Job? = null
    private var threadObservationJob: Job? = null
    private var searchJob: Job? = null
    private var recentSearchObservationJob: Job? = null

    init {
        observeSubscriptionUuid()
        observeRecentSearches()
        observePostingDrafts()
        viewModelScope.launch {
            runCatching { repository.cleanupExpiredCache() }
            runCatching { repository.cleanupExpiredReadHistory() }
        }
        viewModelScope.launch {
            hydrateCachedIndex()
            refreshIndexInternal()
        }
    }

    private suspend fun hydrateCachedIndex() {
        val cachedIndex = repository.getCachedCatalogIndex()
        val lastSelectedSource = repository.getLastSelectedSource()
        if (
            cachedIndex.forumGroups.isEmpty() &&
            cachedIndex.timelines.isEmpty() &&
            cachedIndex.siteNotice == null
        ) {
            return
        }

        _uiState.update { state ->
            state.copy(
                isLoadingIndex = false,
                forumGroups = cachedIndex.forumGroups,
                timelines = cachedIndex.timelines,
                siteNotice = cachedIndex.siteNotice,
                currentSource = resolveCatalogSource(
                    preferredSource = state.currentSource,
                    forumGroups = cachedIndex.forumGroups,
                    timelines = cachedIndex.timelines,
                    lastSelectedSource = lastSelectedSource
                )
            )
        }

        _uiState.value.currentSource?.let(::observeCatalog)
    }

    fun refreshIndex() {
        viewModelScope.launch { refreshIndexInternal() }
    }

    private suspend fun refreshIndexInternal() {
        _uiState.update {
            it.copy(
                isLoadingIndex = it.forumGroups.isEmpty() && it.timelines.isEmpty(),
                error = null
            )
        }

        supervisorScope {
            val existingNotice = _uiState.value.siteNotice
            val forumsDeferred = async { runCatching { repository.getForumList() } }
            val timelinesDeferred = async { runCatching { repository.getTimelineList() } }
            val noticeDeferred = async { runCatching { repository.getSiteNotice() } }

            val forumGroupsResult = forumsDeferred.await()
            val timelinesResult = timelinesDeferred.await()

            val failure = forumGroupsResult.exceptionOrNull() ?: timelinesResult.exceptionOrNull()
            if (failure != null) {
                val siteNotice = noticeDeferred.await().getOrElse { existingNotice }
                _uiState.update {
                    it.copy(
                        isLoadingIndex = false,
                        siteNotice = siteNotice,
                        error = failure.toErrorPresentation("加载板块失败")
                    )
                }
                return@supervisorScope
            }

            val previousSource = _uiState.value.currentSource
            val forumGroups = forumGroupsResult.getOrThrow()
            val timelines = timelinesResult.getOrThrow()
            val lastSelectedSource = repository.getLastSelectedSource()
            val source = resolveCatalogSource(
                preferredSource = previousSource,
                forumGroups = forumGroups,
                timelines = timelines,
                lastSelectedSource = lastSelectedSource
            )
            _uiState.update {
                it.copy(
                    isLoadingIndex = false,
                    forumGroups = forumGroups,
                    timelines = timelines,
                    currentSource = source,
                    siteNotice = existingNotice
                )
            }

            if (source == null) {
                val refreshedNotice = noticeDeferred.await().getOrElse { existingNotice }
                if (refreshedNotice != existingNotice) {
                    _uiState.update { it.copy(siteNotice = refreshedNotice) }
                }
                return@supervisorScope
            }

            val cacheKey = source.cacheKey()
            val sourceChanged = previousSource != source
            if (sourceChanged || catalogObservationJob == null) {
                observeCatalog(source)
            }
            if (sourceChanged) {
                repository.saveLastSelectedSource(source)
            }
            if (catalogPageCache[cacheKey] == null) {
                loadCatalogPage(source, 1)
            }

            val refreshedNotice = noticeDeferred.await().getOrElse { existingNotice }
            if (refreshedNotice != existingNotice) {
                _uiState.update { it.copy(siteNotice = refreshedNotice) }
            }
        }
    }

    fun openSource(source: CatalogSource, forceRefresh: Boolean = false) {
        val cacheKey = source.cacheKey()
        viewModelScope.launch {
            repository.saveLastSelectedSource(source)
        }
        _uiState.update {
            it.copy(
                currentSource = source,
                loadedCatalogPage = catalogPageCache[cacheKey] ?: 0,
                error = null
            )
        }
        observeCatalog(source)

        if (forceRefresh || (catalogPageCache[cacheKey] ?: 0) == 0) {
            loadCatalogPage(source, 1)
        }
    }

    fun loadMoreCatalog() {
        val source = _uiState.value.currentSource ?: return
        val nextPage = (catalogPageCache[source.cacheKey()] ?: 0) + 1
        loadCatalogPage(source, nextPage)
    }

    fun openSubscriptions(forceRefresh: Boolean = false) {
        observeSubscriptions()
        _uiState.update {
            it.copy(
                isLoadingSubscriptions = forceRefresh || subscriptionLoadedPage == 0,
                subscriptionError = null,
                loadedSubscriptionPage = subscriptionLoadedPage
            )
        }
        if (forceRefresh || subscriptionLoadedPage == 0) {
            loadSubscriptionsPage(1)
        }
    }

    fun openReadHistory() {
        observeReadHistory()
        _uiState.update { it.copy(historyError = null) }
    }

    fun openPostingHistory() {
        observePostingHistory()
        _uiState.update { it.copy(postingHistoryError = null) }
    }

    fun openSearch() {
        _uiState.update { it.copy(searchError = null) }
    }

    fun submitSearchQuery(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            return
        }

        viewModelScope.launch {
            repository.recordRecentSearch(normalizedQuery)
        }

        runSearch(
            inputQuery = normalizedQuery,
            clearExistingResults = false,
            debounceMillis = 0L
        )
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            repository.clearRecentSearches()
        }
    }

    fun submitThreadPost(request: ThreadPostRequest, draftId: Long?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPostingThread = true,
                    postThreadError = null,
                    postedThreadResult = null
                )
            }

            runCatching {
                repository.postThread(request)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isPostingThread = false,
                        postThreadError = null,
                        postedThreadResult = result
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isPostingThread = false,
                        postThreadError = throwable.toErrorPresentation("发串失败")
                    )
                }
            }
        }
    }

    fun clearPostThreadError() {
        _uiState.update { it.copy(postThreadError = null) }
    }

    fun consumePostedThreadResult() {
        _uiState.update { it.copy(postedThreadResult = null) }
    }

    fun submitReplyPost(request: ReplyPostRequest, draftId: Long?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPostingReply = true,
                    postReplyError = null,
                    postedReplyResult = null
                )
            }

            runCatching {
                repository.postReply(request)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isPostingReply = false,
                        postReplyError = null,
                        postedReplyResult = result
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isPostingReply = false,
                        postReplyError = throwable.toErrorPresentation("回帖失败")
                    )
                }
            }
        }
    }

    fun clearPostReplyError() {
        _uiState.update { it.copy(postReplyError = null) }
    }

    fun consumePostedReplyResult() {
        _uiState.update { it.copy(postedReplyResult = null) }
    }

    fun updateSearchQuery(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            searchJob?.cancel()
            _uiState.update {
                it.copy(
                    searchQuery = query,
                    searchResults = emptyList(),
                    directThreadShortcut = null,
                    isSearching = false,
                    searchError = null
                )
            }
            return
        }

        runSearch(
            inputQuery = query,
            clearExistingResults = true,
            debounceMillis = 180L
        )
    }

    fun deleteReadHistoryEntry(threadId: Long) {
        viewModelScope.launch {
            runCatching {
                repository.deleteReadHistoryEntry(threadId)
            }.onSuccess {
                _uiState.update { it.copy(historyError = null) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(historyError = throwable.toErrorPresentation("删除阅读记录失败"))
                }
            }
        }
    }

    fun clearReadHistory() {
        viewModelScope.launch {
            runCatching {
                repository.clearReadHistory()
            }.onSuccess {
                _uiState.update { it.copy(historyError = null) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(historyError = throwable.toErrorPresentation("清空阅读记录失败"))
                }
            }
        }
    }

    fun deletePostingHistoryEntry(id: Long) {
        viewModelScope.launch {
            runCatching {
                repository.deletePostingHistoryEntry(id)
            }.onSuccess {
                _uiState.update { it.copy(postingHistoryError = null) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(postingHistoryError = throwable.toErrorPresentation("删除发言记录失败"))
                }
            }
        }
    }

    fun clearPostingHistory(type: PostingHistoryType) {
        viewModelScope.launch {
            runCatching {
                repository.clearPostingHistory(type)
            }.onSuccess {
                _uiState.update { it.copy(postingHistoryError = null) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(postingHistoryError = throwable.toErrorPresentation("清空发言记录失败"))
                }
            }
        }
    }

    fun deletePostingDraft(id: Long) {
        viewModelScope.launch {
            runCatching {
                repository.deletePostingDraft(id)
            }.onSuccess {
                _uiState.update { it.copy(postingHistoryError = null) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(postingHistoryError = throwable.toErrorPresentation("删除草稿失败"))
                }
            }
        }
    }

    fun clearPostingDrafts() {
        viewModelScope.launch {
            runCatching {
                repository.clearPostingDrafts()
            }.onSuccess {
                _uiState.update { it.copy(postingHistoryError = null) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(postingHistoryError = throwable.toErrorPresentation("清空草稿箱失败"))
                }
            }
        }
    }

    fun refreshSubscriptions() {
        openSubscriptions(forceRefresh = true)
    }

    fun loadMoreSubscriptions() {
        loadSubscriptionsPage(subscriptionLoadedPage + 1)
    }

    fun addSubscription(threadId: Long) {
        viewModelScope.launch {
            runCatching {
                repository.addSubscription(threadId)
            }.onSuccess {
                subscriptionLoadedPage = 1
                _uiState.update {
                    it.copy(
                        loadedSubscriptionPage = subscriptionLoadedPage,
                        subscriptionError = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(error = throwable.toErrorPresentation("订阅失败"))
                }
            }
        }
    }

    fun deleteSubscription(threadId: Long) {
        viewModelScope.launch {
            runCatching {
                repository.deleteSubscription(threadId)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoadingSubscriptions = false,
                        subscriptionError = throwable.toErrorPresentation("取消订阅失败")
                    )
                }
            }
        }
    }

    fun openThread(
        threadId: Long,
        forceRefresh: Boolean = false,
        targetPage: Int? = null
    ) {
        prepareThreadState(threadId = threadId, forceRefresh = forceRefresh)
        observeThread(threadId)

        viewModelScope.launch {
            val progress = repository.getReadProgress(threadId)
            val resolvedTargetPage = (targetPage ?: progress?.lastReadPage ?: 1).coerceAtLeast(1)
            val persistedLoadedPage = repository.getCachedThreadLoadedPage(threadId)
            val loadedPage = threadPageCache[threadId]
                ?.let { minOf(it, persistedLoadedPage) }
                ?: persistedLoadedPage
            threadPageCache[threadId] = loadedPage

            runCatching {
                refreshThreadPages(
                    threadId = threadId,
                    forceRefresh = forceRefresh,
                    loadedPage = loadedPage,
                    targetPage = resolvedTargetPage
                )
            }.onSuccess { (lastLoadedPage, hasReachedReplyEnd) ->
                val resolvedLoadedPage = if (forceRefresh) {
                    lastLoadedPage
                } else {
                    maxOf(loadedPage, lastLoadedPage)
                }
                threadPageCache[threadId] = resolvedLoadedPage
                threadEndReachedCache[threadId] = hasReachedReplyEnd
                _uiState.update {
                    it.copy(
                        isLoadingThread = false,
                        loadedThreadPage = resolvedLoadedPage,
                        hasReachedReplyEnd = hasReachedReplyEnd
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoadingThread = false,
                        error = throwable.toErrorPresentation("加载串失败")
                    )
                }
            }
        }
    }

    fun loadMoreReplies() {
        if (!_uiState.value.canLoadMoreReplies) {
            return
        }

        val threadId = _uiState.value.currentThread?.id ?: return
        val nextPage = (threadPageCache[threadId] ?: 0) + 1
        val maxReplyPage = calculateNmbThreadMaxPage(_uiState.value.currentThread?.replyCount ?: 0)

        if (nextPage > maxReplyPage) {
            threadEndReachedCache[threadId] = true
            _uiState.update { it.copy(hasReachedReplyEnd = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingThread = true, error = null) }
            runCatching {
                repository.refreshThread(threadId, nextPage)
            }.onSuccess { result ->
                threadPageCache[threadId] = result.page
                if (result.reachedEnd) {
                    threadEndReachedCache[threadId] = true
                }
                _uiState.update {
                    it.copy(
                        isLoadingThread = false,
                        loadedThreadPage = result.page,
                        hasReachedReplyEnd = threadEndReachedCache[threadId] == true
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoadingThread = false,
                        error = throwable.toErrorPresentation("加载更多回复失败")
                    )
                }
            }
        }
    }

    fun saveThreadProgress(threadId: Long, itemIndex: Int, itemOffset: Int) {
        val state = _uiState.value
        val thread = state.currentThread
            ?.takeIf { state.activeThreadId == threadId && it.id == threadId }
            ?: return
        val posts = state.currentPosts
        val anchorPost = posts.getOrNull(itemIndex - 1)
        val page = anchorPost?.page ?: 1

        viewModelScope.launch {
            repository.updateReadProgress(
                threadId = thread.id,
                page = page,
                postId = anchorPost?.id,
                itemIndex = itemIndex,
                itemOffset = itemOffset
            )
        }
    }

    private fun prepareThreadState(threadId: Long, forceRefresh: Boolean) {
        _uiState.update { state ->
            val currentThread = state.currentThread?.takeIf { it.id == threadId }
            state.copy(
                isLoadingThread = true,
                activeThreadId = threadId,
                threadDetail = currentThread?.let {
                    state.threadDetail
                } ?: ThreadDetail(null, emptyList(), null),
                loadedThreadPage = threadPageCache[threadId] ?: 0,
                hasReachedReplyEnd = isReplyEndReached(
                    threadReplyCount = currentThread?.replyCount,
                    loadedPage = threadPageCache[threadId] ?: 0,
                    cachedReachedEnd = threadEndReachedCache[threadId] == true && !forceRefresh
                ),
                error = null
            )
        }
    }

    private suspend fun refreshThreadPages(
        threadId: Long,
        forceRefresh: Boolean,
        loadedPage: Int,
        targetPage: Int
    ): Pair<Int, Boolean> {
        val pageRange = when {
            forceRefresh -> 1..maxOf(loadedPage, targetPage, 1)
            loadedPage > 0 -> loadedPage..maxOf(loadedPage, targetPage)
            else -> 1..targetPage
        }

        var lastLoadedPage = loadedPage
        var hasReachedReplyEnd = if (forceRefresh) {
            false
        } else {
            threadEndReachedCache[threadId] == true
        }
        var knownMaxPage = Int.MAX_VALUE

        for (page in pageRange) {
            if (page > knownMaxPage) {
                hasReachedReplyEnd = true
                break
            }

            val result = repository.refreshThread(threadId, page)
            lastLoadedPage = result.page
            knownMaxPage = result.maxPage
            if (result.reachedEnd) {
                hasReachedReplyEnd = true
                break
            }
        }

        return lastLoadedPage to hasReachedReplyEnd
    }

    private fun observeCatalog(source: CatalogSource) {
        val cacheKey = source.cacheKey()
        catalogObservationJob?.cancel()
        catalogObservationJob = viewModelScope.launch {
            repository.observeCatalog(source).collect { threads ->
                _uiState.update { state ->
                    state.copy(
                        threads = threads,
                        loadedCatalogPage = catalogPageCache[cacheKey] ?: state.loadedCatalogPage
                    )
                }
            }
        }
    }

    private fun observeThread(threadId: Long) {
        threadObservationJob?.cancel()
        threadObservationJob = viewModelScope.launch {
            repository.observeThreadDetail(threadId).collect { detail ->
                _uiState.update { state ->
                    state.copy(
                        threadDetail = detail,
                        loadedThreadPage = threadPageCache[threadId] ?: state.loadedThreadPage,
                        hasReachedReplyEnd = isReplyEndReached(
                            threadReplyCount = detail.thread?.replyCount,
                            loadedPage = threadPageCache[threadId] ?: state.loadedThreadPage,
                            cachedReachedEnd = threadEndReachedCache[threadId] == true
                        )
                    )
                }
            }
        }
    }

    private fun observeSubscriptionUuid() {
        viewModelScope.launch {
            repository.observeSubscriptionUuid().collect { uuid ->
                if (currentSubscriptionUuid == null) {
                    currentSubscriptionUuid = uuid
                    return@collect
                }

                if (uuid == currentSubscriptionUuid) {
                    return@collect
                }

                currentSubscriptionUuid = uuid
                subscriptionLoadedPage = 0
                _uiState.update {
                    it.copy(
                        subscriptionThreads = emptyList(),
                        loadedSubscriptionPage = 0,
                        isLoadingSubscriptions = false,
                        subscriptionError = null
                    )
                }
            }
        }
    }

    private fun observeRecentSearches() {
        if (recentSearchObservationJob != null) {
            return
        }

        recentSearchObservationJob = viewModelScope.launch {
            repository.observeRecentSearches().collect { recentSearches ->
                _uiState.update { it.copy(recentSearches = recentSearches) }
            }
        }
    }

    private fun runSearch(
        inputQuery: String,
        clearExistingResults: Boolean,
        debounceMillis: Long
    ) {
        val normalizedQuery = inputQuery.trim()
        val directThreadId = parseNmbThreadIdInput(normalizedQuery)
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = inputQuery,
                searchResults = if (clearExistingResults) emptyList() else it.searchResults,
                directThreadShortcut = directThreadId?.let { threadId ->
                    DirectThreadShortcut(
                        threadId = threadId,
                        forumId = null,
                        userHash = "",
                        name = "",
                        title = "",
                        preview = "",
                        postedAtEpochMillis = null,
                        isCached = false
                    )
                },
                isSearching = true,
                searchError = null
            )
        }

        searchJob = viewModelScope.launch {
            if (debounceMillis > 0L) {
                delay(debounceMillis)
            }

            try {
                val directThreadShortcut = repository.resolveDirectThreadShortcut(normalizedQuery)
                val results = repository.searchCachedContent(normalizedQuery)
                _uiState.update {
                    it.copy(
                        searchQuery = normalizedQuery,
                        searchResults = results,
                        directThreadShortcut = directThreadShortcut,
                        isSearching = false,
                        searchError = null
                    )
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    throw throwable
                }
                _uiState.update {
                    it.copy(
                        searchResults = emptyList(),
                        isSearching = false,
                        searchError = throwable.toErrorPresentation("搜索缓存失败")
                    )
                }
            }
        }
    }

    private fun observeSubscriptions() {
        if (subscriptionObservationJob != null) {
            return
        }

        subscriptionObservationJob = viewModelScope.launch {
            repository.observeSubscriptions().collect { threads ->
                _uiState.update { state ->
                    state.copy(
                        subscriptionThreads = threads,
                        loadedSubscriptionPage = subscriptionLoadedPage
                    )
                }
            }
        }
    }

    private fun observeReadHistory() {
        if (readHistoryObservationJob != null) {
            return
        }

        readHistoryObservationJob = viewModelScope.launch {
            repository.observeReadHistory().collect { entries ->
                _uiState.update { state ->
                    state.copy(readHistory = entries)
                }
            }
        }
    }

    private fun observePostingHistory() {
        if (postingHistoryObservationJob != null) {
            return
        }

        postingHistoryObservationJob = viewModelScope.launch {
            repository.observePostingHistory().collect { entries ->
                _uiState.update { state ->
                    state.copy(postingHistory = entries)
                }
            }
        }
    }

    private fun observePostingDrafts() {
        if (postingDraftObservationJob != null) {
            return
        }

        postingDraftObservationJob = viewModelScope.launch {
            repository.observePostingDrafts().collect { entries ->
                _uiState.update { state ->
                    state.copy(postingDrafts = entries)
                }
            }
        }
    }

    private fun loadCatalogPage(source: CatalogSource, page: Int) {
        val cacheKey = source.cacheKey()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCatalog = true, error = null) }
            runCatching {
                repository.refreshCatalog(source, page)
            }.onSuccess {
                catalogPageCache[cacheKey] = maxOf(catalogPageCache[cacheKey] ?: 0, page)
                _uiState.update {
                    it.copy(
                        isLoadingCatalog = false,
                        loadedCatalogPage = catalogPageCache[cacheKey] ?: page
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoadingCatalog = false,
                        error = throwable.toErrorPresentation("加载串列表失败")
                    )
                }
            }
        }
    }

    private fun loadSubscriptionsPage(page: Int) {
        if (page <= 0) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingSubscriptions = true,
                    subscriptionError = null
                )
            }
            runCatching {
                repository.refreshSubscriptions(page)
            }.onSuccess {
                subscriptionLoadedPage = maxOf(subscriptionLoadedPage, page)
                _uiState.update {
                    it.copy(
                        isLoadingSubscriptions = false,
                        loadedSubscriptionPage = subscriptionLoadedPage
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoadingSubscriptions = false,
                        subscriptionError = throwable.toErrorPresentation("加载订阅失败")
                    )
                }
            }
        }
    }

    private fun isReplyEndReached(
        threadReplyCount: Int?,
        loadedPage: Int,
        cachedReachedEnd: Boolean
    ): Boolean {
        if (cachedReachedEnd) {
            return true
        }

        val replyCount = threadReplyCount ?: return false
        return loadedPage >= calculateNmbThreadMaxPage(replyCount)
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ForumBrowseViewModel(
                    checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                )
            }
        }
    }
}
