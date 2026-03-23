package ink.duo3.fogisland.ui.forum

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ink.duo3.fogisland.shared.model.CatalogSource
import ink.duo3.fogisland.shared.model.ForumGroup
import ink.duo3.fogisland.shared.model.SiteNotice
import ink.duo3.fogisland.shared.model.ThreadDetail
import ink.duo3.fogisland.shared.model.Timeline
import ink.duo3.fogisland.shared.model.cacheKey
import ink.duo3.fogisland.shared.model.resolveCatalogSource
import ink.duo3.fogisland.shared.repository.RepositoryProvider
import ink.duo3.fogisland.shared.storage.db.entity.ThreadEntity
import ink.duo3.fogisland.shared.util.calculateNmbThreadMaxPage
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class ForumBrowseUiState(
    val isLoadingIndex: Boolean = true,
    val isLoadingCatalog: Boolean = false,
    val isLoadingThread: Boolean = false,
    val activeThreadId: Long? = null,
    val forumGroups: List<ForumGroup> = emptyList(),
    val timelines: List<Timeline> = emptyList(),
    val currentSource: CatalogSource? = null,
    val threads: List<ThreadEntity> = emptyList(),
    val loadedCatalogPage: Int = 0,
    val siteNotice: SiteNotice? = null,
    val threadDetail: ThreadDetail = ThreadDetail(null, emptyList(), null),
    val loadedThreadPage: Int = 0,
    val hasReachedReplyEnd: Boolean = false,
    val errorMessage: String? = null
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
    private var catalogObservationJob: Job? = null
    private var threadObservationJob: Job? = null

    init {
        hydrateCachedIndex()
        refreshIndex()
    }

    private fun hydrateCachedIndex() {
        viewModelScope.launch {
            val cachedIndex = repository.getCachedCatalogIndex()
            val lastSelectedSource = repository.getLastSelectedSource()
            if (cachedIndex.forumGroups.isEmpty() && cachedIndex.timelines.isEmpty()) {
                return@launch
            }

            _uiState.update { state ->
                state.copy(
                    isLoadingIndex = false,
                    forumGroups = cachedIndex.forumGroups,
                    timelines = cachedIndex.timelines,
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
    }

    fun refreshIndex() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingIndex = it.forumGroups.isEmpty() && it.timelines.isEmpty(),
                    errorMessage = null
                )
            }

            supervisorScope {
                val existingNotice = _uiState.value.siteNotice
                val forumsDeferred = async { runCatching { repository.getForumList() } }
                val timelinesDeferred = async { runCatching { repository.getTimelineList() } }
                val noticeDeferred = async { runCatching { repository.getSiteNotice() } }

                val forumGroupsResult = forumsDeferred.await()
                val timelinesResult = timelinesDeferred.await()
                val siteNotice = noticeDeferred.await().getOrElse { existingNotice }

                val failure = forumGroupsResult.exceptionOrNull() ?: timelinesResult.exceptionOrNull()
                if (failure != null) {
                    _uiState.update {
                        it.copy(
                            isLoadingIndex = false,
                            siteNotice = siteNotice,
                            errorMessage = failure.message ?: "加载板块失败"
                        )
                    }
                    return@supervisorScope
                }

                val forumGroups = forumGroupsResult.getOrThrow()
                val timelines = timelinesResult.getOrThrow()
                val lastSelectedSource = repository.getLastSelectedSource()
                val source = resolveCatalogSource(
                    preferredSource = _uiState.value.currentSource,
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
                        siteNotice = siteNotice
                    )
                }
                if (source != null) {
                    openSource(source, forceRefresh = catalogPageCache[source.cacheKey()] == null)
                }
            }
        }
    }

    fun openSource(source: CatalogSource, forceRefresh: Boolean = false) {
        val cacheKey = source.cacheKey()
        viewModelScope.launch { repository.saveLastSelectedSource(source) }
        _uiState.update {
            it.copy(
                currentSource = source,
                loadedCatalogPage = catalogPageCache[cacheKey] ?: 0,
                errorMessage = null
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

    fun openThread(threadId: Long, forceRefresh: Boolean = false) {
        prepareThreadState(threadId = threadId, forceRefresh = forceRefresh)
        observeThread(threadId)

        viewModelScope.launch {
            val progress = repository.getReadProgress(threadId)
            val targetPage = (progress?.lastReadPage ?: 1).coerceAtLeast(1)
            val loadedPage = threadPageCache[threadId] ?: 0

            runCatching {
                refreshThreadPages(
                    threadId = threadId,
                    forceRefresh = forceRefresh,
                    loadedPage = loadedPage,
                    targetPage = targetPage
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
                        errorMessage = throwable.message ?: "加载串失败"
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
            _uiState.update { it.copy(isLoadingThread = true, errorMessage = null) }
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
                        errorMessage = throwable.message ?: "加载更多回复失败"
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
                errorMessage = null
            )
        }
    }

    private suspend fun refreshThreadPages(
        threadId: Long,
        forceRefresh: Boolean,
        loadedPage: Int,
        targetPage: Int
    ): Pair<Int, Boolean> {
        val refreshThroughPage = if (forceRefresh) {
            maxOf(loadedPage, targetPage, 1)
        } else {
            targetPage
        }
        val pageRange = if (!forceRefresh && loadedPage >= targetPage) {
            IntRange.EMPTY
        } else if (forceRefresh) {
            1..refreshThroughPage
        } else {
            (loadedPage + 1)..targetPage
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

    private fun loadCatalogPage(source: CatalogSource, page: Int) {
        val cacheKey = source.cacheKey()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCatalog = true, errorMessage = null) }
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
                        errorMessage = throwable.message ?: "加载串列表失败"
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
