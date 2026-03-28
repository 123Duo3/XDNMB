package ink.duo3.fogisland.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import ink.duo3.fogisland.data.draft.cleanupOrphanDraftImages
import ink.duo3.fogisland.data.draft.deleteDraftImage
import ink.duo3.fogisland.shared.model.CatalogSource
import ink.duo3.fogisland.shared.model.CatalogType
import ink.duo3.fogisland.shared.model.ThreadDetail
import ink.duo3.fogisland.shared.model.buildForumNameMap
import ink.duo3.fogisland.shared.model.findCatalogSource
import ink.duo3.fogisland.shared.model.resolveForumName
import ink.duo3.fogisland.shared.model.toCatalogSource
import ink.duo3.fogisland.shared.repository.RepositoryProvider
import ink.duo3.fogisland.ui.components.NavigationItemGroup
import ink.duo3.fogisland.ui.components.imageviewer.ImageViewerPreviewState
import ink.duo3.fogisland.viewmodel.ForumBrowseViewModel
import kotlinx.coroutines.launch

@Composable
fun FogIslandApp() {
    val context = LocalContext.current
    val repository = remember(context) { RepositoryProvider.provideForumRepository(context.applicationContext) }
    val viewModel = viewModel<ForumBrowseViewModel>(factory = ForumBrowseViewModel.factory())
    val state by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack = remember { mutableStateListOf<AppRoute>(AppRoute.Catalog) }
    val expandedGroups = remember { mutableStateMapOf<Long, Boolean>() }
    val snackbarHostState = remember { SnackbarHostState() }
    var imageViewerPreviewState by remember { mutableStateOf<ImageViewerPreviewState?>(null) }
    val currentRoute = backStack.lastOrNull() ?: AppRoute.Catalog
    val isCatalogRoute = currentRoute == AppRoute.Catalog
    val forumNameById = remember(state.forumGroups) { buildForumNameMap(state.forumGroups) }
    val activeImageViewerKey = (currentRoute as? AppRoute.ImageViewer)
        ?.let { route -> route.image to route.ext }
    val navTransitionSpec:
        AnimatedContentTransitionScope<Scene<AppRoute>>.() -> ContentTransform = {
        if (initialState.isImageViewerScene() || targetState.isImageViewerScene()) {
            ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = ExitTransition.None
            )
        } else {
            defaultTransitionSpec<AppRoute>().invoke(this)
        }
    }
    val navPopTransitionSpec:
        AnimatedContentTransitionScope<Scene<AppRoute>>.() -> ContentTransform = {
        if (initialState.isImageViewerScene() || targetState.isImageViewerScene()) {
            ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = ExitTransition.None
            )
        } else {
            defaultPopTransitionSpec<AppRoute>().invoke(this)
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            repository
                .getPostingDrafts()
                .mapNotNull { it.imagePath }
                .toSet()
        }.onSuccess { activePaths ->
            context.cleanupOrphanDraftImages(activePaths)
        }
    }

    fun showSnackbarMessage(message: String) {
        if (message.isBlank()) {
            return
        }
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    fun showCatalog() {
        backStack.clear()
        backStack.add(AppRoute.Catalog)
    }

    fun showSubscriptions() {
        backStack.clear()
        backStack.add(AppRoute.Catalog)
        backStack.add(AppRoute.Subscriptions)
    }

    fun showHistory() {
        backStack.clear()
        backStack.add(AppRoute.Catalog)
        backStack.add(AppRoute.History)
    }

    fun showPostingHistory() {
        backStack.clear()
        backStack.add(AppRoute.Catalog)
        backStack.add(AppRoute.PostingHistory)
    }

    fun showSearch() {
        backStack.clear()
        backStack.add(AppRoute.Catalog)
        backStack.add(AppRoute.Search)
    }

    fun showPostThread(defaultForumId: Long?, draftId: Long? = null) {
        backStack.clear()
        backStack.add(AppRoute.Catalog)
        backStack.add(AppRoute.PostThread(defaultForumId, draftId))
    }

    fun showPostReply(threadId: Long, draftId: Long? = null) {
        backStack.add(AppRoute.PostReply(threadId, draftId))
    }

    fun showImageViewer(
        image: String,
        ext: String?,
        previewState: ImageViewerPreviewState? = null
    ) {
        imageViewerPreviewState = previewState
        backStack.add(AppRoute.ImageViewer(image = image, ext = ext))
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute !is AppRoute.ImageViewer) {
            imageViewerPreviewState = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            modifier = Modifier.fillMaxSize(),
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(drawerState = drawerState) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp, 8.dp)
                    ) {
                        Text(
                            text = "雾岛",
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                        NavigationDrawerItem(
                            label = { Text("订阅", style = MaterialTheme.typography.labelLarge) },
                            icon = { Icon(Icons.Filled.Bookmarks, null) },
                            selected = currentRoute == AppRoute.Subscriptions,
                            onClick = {
                                if (currentRoute != AppRoute.Subscriptions) {
                                    showSubscriptions()
                                }
                                viewModel.openSubscriptions()
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("历史", style = MaterialTheme.typography.labelLarge) },
                            icon = { Icon(Icons.Default.History, null) },
                            selected = currentRoute == AppRoute.History,
                            onClick = {
                                if (currentRoute != AppRoute.History) {
                                    showHistory()
                                }
                                viewModel.openReadHistory()
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("搜索", style = MaterialTheme.typography.labelLarge) },
                            icon = { Icon(Icons.Default.Search, null) },
                            selected = currentRoute == AppRoute.Search,
                            onClick = {
                                if (currentRoute != AppRoute.Search) {
                                    showSearch()
                                }
                                viewModel.openSearch()
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("发言", style = MaterialTheme.typography.labelLarge) },
                            icon = { Icon(Icons.AutoMirrored.Filled.Comment, null) },
                            selected = currentRoute == AppRoute.PostingHistory,
                            onClick = {
                                if (currentRoute != AppRoute.PostingHistory) {
                                    showPostingHistory()
                                }
                                viewModel.openPostingHistory()
                                scope.launch { drawerState.close() }
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp)
                        ) {
                            if (state.timelines.isNotEmpty()) {
                                val timelineExpanded = expandedGroups[-1L] ?: false
                                NavigationItemGroup(
                                    label = { Text("时间线", style = MaterialTheme.typography.labelLarge) },
                                    selected = isCatalogRoute && state.currentSource?.type == CatalogType.TIMELINE,
                                    expanded = timelineExpanded,
                                    modifier = Modifier,
                                    onExpandStateChange = { expandedGroups[-1L] = it }
                                ) {
                                    state.timelines.forEach { timeline ->
                                        val source = timeline.toCatalogSource()
                                        NavigationDrawerItem(
                                            label = {
                                                Text(
                                                    timeline.displayName,
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            },
                                            selected = isCatalogRoute && state.currentSource == source,
                                            onClick = {
                                                viewModel.openSource(source)
                                                showCatalog()
                                                scope.launch { drawerState.close() }
                                            },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                    }
                                }
                            }
                            state.forumGroups.forEach { group ->
                                val expanded = expandedGroups[group.id] ?: false
                                val groupSelected = isCatalogRoute &&
                                    state.currentSource?.type == CatalogType.FORUM &&
                                    group.forums.any { it.id == state.currentSource?.id }
                                NavigationItemGroup(
                                    label = { Text(group.name, style = MaterialTheme.typography.labelLarge) },
                                    selected = groupSelected,
                                    expanded = expanded,
                                    modifier = Modifier,
                                    onExpandStateChange = { expandedGroups[group.id] = it }
                                ) {
                                    group.forums.forEach { forum ->
                                        val source = forum.toCatalogSource(group)
                                        NavigationDrawerItem(
                                            label = {
                                                Text(
                                                    forum.displayName,
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            },
                                            selected = isCatalogRoute && state.currentSource == source,
                                            onClick = {
                                                viewModel.openSource(source)
                                                showCatalog()
                                                scope.launch { drawerState.close() }
                                            },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        NavigationDrawerItem(
                            label = { Text("设置", style = MaterialTheme.typography.labelLarge) },
                            icon = { Icon(Icons.Filled.Settings, null) },
                            selected = currentRoute == AppRoute.Settings,
                            onClick = {
                                if (currentRoute != AppRoute.Settings) {
                                    backStack.clear()
                                    backStack.add(AppRoute.Catalog)
                                    backStack.add(AppRoute.Settings)
                                }
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            }
        ) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = navTransitionSpec,
                popTransitionSpec = navPopTransitionSpec,
            ) { key: AppRoute ->
                when (key) {
                AppRoute.Catalog -> NavEntry(key) {
                    ForumScreen(
                        state = state,
                        activeImageViewerKey = activeImageViewerKey,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onRefreshClick = {
                            state.currentSource?.let { source ->
                                viewModel.openSource(source, forceRefresh = true)
                            }
                        },
                        onPostClick = {
                            val defaultForumId = state.currentSource
                                ?.takeIf { it.type == CatalogType.FORUM }
                                ?.id
                            showPostThread(defaultForumId)
                        },
                        onLoadMore = { viewModel.loadMoreCatalog() },
                        onThreadClick = { threadId ->
                            viewModel.openThread(threadId)
                            backStack.add(AppRoute.Thread(threadId))
                        },
                        onImageClick = { image, ext, previewState ->
                            showImageViewer(image = image, ext = ext, previewState = previewState)
                        }
                    )
                }

                AppRoute.Subscriptions -> NavEntry(key) {
                    LaunchedEffect(Unit) {
                        viewModel.openSubscriptions()
                    }
                    SubscriptionScreen(
                        forumGroups = state.forumGroups,
                        threads = state.subscriptionThreads,
                        loadedPage = state.loadedSubscriptionPage,
                        isLoading = state.isLoadingSubscriptions,
                        error = state.subscriptionError,
                        activeImageViewerKey = activeImageViewerKey,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onRefreshClick = { viewModel.refreshSubscriptions() },
                        onLoadMore = { viewModel.loadMoreSubscriptions() },
                        onThreadClick = { threadId ->
                            viewModel.openThread(threadId)
                            backStack.add(AppRoute.Thread(threadId))
                        },
                        onDeleteClick = { threadId ->
                            viewModel.deleteSubscription(threadId)
                        },
                        onImageClick = { image, ext, previewState ->
                            showImageViewer(image = image, ext = ext, previewState = previewState)
                        }
                    )
                }

                AppRoute.History -> NavEntry(key) {
                    LaunchedEffect(Unit) {
                        viewModel.openReadHistory()
                    }
                    HistoryScreen(
                        forumGroups = state.forumGroups,
                        history = state.readHistory,
                        error = state.historyError,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onThreadClick = { threadId ->
                            viewModel.openThread(threadId)
                            backStack.add(AppRoute.Thread(threadId))
                        },
                        onDeleteClick = { threadId ->
                            viewModel.deleteReadHistoryEntry(threadId)
                        },
                        onClearAllClick = {
                            viewModel.clearReadHistory()
                        }
                    )
                }

                AppRoute.PostingHistory -> NavEntry(key) {
                    LaunchedEffect(Unit) {
                        viewModel.openPostingHistory()
                    }
                    PostingHistoryScreen(
                        forumGroups = state.forumGroups,
                        history = state.postingHistory,
                        drafts = state.postingDrafts,
                        error = state.postingHistoryError,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onEntryClick = { entry ->
                            entry.threadId?.let { threadId ->
                                viewModel.openThread(threadId)
                                backStack.add(
                                    AppRoute.Thread(
                                        threadId = threadId,
                                        targetPostId = entry.postId
                                    )
                                )
                            }
                        },
                        onDraftClick = { entry ->
                            when (entry.type) {
                                ink.duo3.fogisland.shared.model.PostingDraftType.THREAD -> {
                                    backStack.add(AppRoute.PostThread(entry.forumId, entry.id))
                                }
                                ink.duo3.fogisland.shared.model.PostingDraftType.REPLY -> {
                                    entry.threadId?.let { threadId ->
                                        backStack.add(AppRoute.PostReply(threadId, entry.id))
                                    }
                                }
                            }
                        },
                        onDeleteClick = { entryId ->
                            viewModel.deletePostingHistoryEntry(entryId)
                        },
                        onDeleteDraftClick = { draftId ->
                            scope.launch {
                                context.deleteDraftImage(repository.getPostingDraft(draftId)?.imagePath)
                                viewModel.deletePostingDraft(draftId)
                            }
                        },
                        onClearAllClick = { type ->
                            viewModel.clearPostingHistory(type)
                        },
                        onClearAllDraftsClick = {
                            scope.launch {
                                repository.getPostingDrafts().forEach { draft ->
                                    context.deleteDraftImage(draft.imagePath)
                                }
                                viewModel.clearPostingDrafts()
                            }
                        }
                    )
                }

                AppRoute.Search -> NavEntry(key) {
                    LaunchedEffect(Unit) {
                        viewModel.openSearch()
                    }
                    SearchScreen(
                        forumGroups = state.forumGroups,
                        query = state.searchQuery,
                        results = state.searchResults,
                        directThreadShortcut = state.directThreadShortcut,
                        recentSearches = state.recentSearches,
                        isSearching = state.isSearching,
                        error = state.searchError,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onQueryChange = viewModel::updateSearchQuery,
                        onQuerySubmit = viewModel::submitSearchQuery,
                        onClearRecentSearches = viewModel::clearRecentSearches,
                        onDirectThreadClick = { threadId ->
                            viewModel.submitSearchQuery(state.searchQuery)
                            backStack.add(AppRoute.Thread(threadId = threadId))
                        },
                        onResultClick = { hit ->
                            viewModel.submitSearchQuery(state.searchQuery)
                            backStack.add(
                                AppRoute.Thread(
                                    threadId = hit.threadId,
                                    targetPostId = hit.postId,
                                    targetPage = if (hit.type == ink.duo3.fogisland.shared.model.SearchHitType.POST) {
                                        hit.page
                                    } else {
                                        null
                                    }
                                )
                            )
                        }
                    )
                }

                is AppRoute.PostThread -> NavEntry(key) {
                    val postedThreadResult = state.postedThreadResult
                    LaunchedEffect(postedThreadResult) {
                        val result = postedThreadResult ?: return@LaunchedEffect
                        key.draftId?.let { draftId ->
                            context.deleteDraftImage(repository.getPostingDraft(draftId)?.imagePath)
                            viewModel.deletePostingDraft(draftId)
                        }
                        showSnackbarMessage(result.message)
                        val targetSource = findCatalogSource(
                            source = CatalogSource(
                                type = CatalogType.FORUM,
                                id = result.forumId,
                                title = "",
                                subtitle = null
                            ),
                            forumGroups = state.forumGroups,
                            timelines = state.timelines
                        )
                        targetSource?.let { source ->
                            viewModel.openSource(source, forceRefresh = true)
                        }
                        if (backStack.lastOrNull() is AppRoute.PostThread) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                        result.threadId?.let { threadId ->
                            viewModel.openThread(threadId, forceRefresh = true)
                            backStack.add(AppRoute.Thread(threadId = threadId))
                        }
                        viewModel.consumePostedThreadResult()
                    }

                    PostThreadScreen(
                        forumGroups = state.forumGroups,
                        initialForumId = key.initialForumId,
                        initialDraft = state.postingDrafts.firstOrNull { it.id == key.draftId },
                        isPosting = state.isPostingThread,
                        error = state.postThreadError,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onClearError = viewModel::clearPostThreadError,
                        onSubmit = viewModel::submitThreadPost
                    )
                }

                is AppRoute.PostReply -> NavEntry(key) {
                    val postedReplyResult = state.postedReplyResult
                    LaunchedEffect(postedReplyResult) {
                        val result = postedReplyResult ?: return@LaunchedEffect
                        key.draftId?.let { draftId ->
                            context.deleteDraftImage(repository.getPostingDraft(draftId)?.imagePath)
                            viewModel.deletePostingDraft(draftId)
                        }
                        showSnackbarMessage(result.message)
                        if (backStack.lastOrNull() is AppRoute.PostReply) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                        if (backStack.lastOrNull() !is AppRoute.Thread ||
                            (backStack.lastOrNull() as? AppRoute.Thread)?.threadId != result.threadId
                        ) {
                            backStack.add(AppRoute.Thread(result.threadId))
                        }
                        val refreshThroughPage = if (state.activeThreadId == result.threadId) {
                            state.loadedThreadPage.coerceAtLeast(1)
                        } else {
                            1
                        }
                        viewModel.openThread(
                            threadId = result.threadId,
                            forceRefresh = true,
                            targetPage = refreshThroughPage
                        )
                        viewModel.consumePostedReplyResult()
                    }

                    val currentThread = state.currentThread?.takeIf { it.id == key.threadId }
                    PostReplyScreen(
                        threadId = key.threadId,
                        threadTitle = currentThread?.title,
                        forumName = resolveForumName(currentThread?.forumId, forumNameById),
                        initialDraft = state.postingDrafts.firstOrNull { it.id == key.draftId },
                        isPosting = state.isPostingReply,
                        error = state.postReplyError,
                        onBack = {
                            if (backStack.isNotEmpty()) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onClearError = viewModel::clearPostReplyError,
                        onSubmit = viewModel::submitReplyPost
                    )
                }

                is AppRoute.Thread -> NavEntry(key) {
                    LaunchedEffect(key.threadId, key.targetPostId, key.targetPage) {
                        val shouldLoadTargetPage = key.targetPage?.let { targetPage ->
                            state.activeThreadId != key.threadId || state.loadedThreadPage < targetPage
                        } ?: false
                        if (state.activeThreadId != key.threadId || shouldLoadTargetPage) {
                            viewModel.openThread(
                                threadId = key.threadId,
                                targetPage = key.targetPage
                            )
                        }
                    }
                    val isRouteThreadActive = state.activeThreadId == key.threadId
                    val routeDetail = if (state.currentThread?.id == key.threadId) {
                        state.threadDetail
                    } else {
                        ThreadDetail(null, emptyList(), null)
                    }
                    ThreadDetailScreen(
                        detail = routeDetail,
                        forumName = resolveForumName(routeDetail.thread?.forumId, forumNameById),
                        loadedPage = if (isRouteThreadActive) state.loadedThreadPage else 0,
                        isLoading = isRouteThreadActive && state.isLoadingThread,
                        canLoadMore = isRouteThreadActive && state.canLoadMoreReplies,
                        error = if (isRouteThreadActive) state.error else null,
                        activeImageViewerKey = activeImageViewerKey,
                        focusPostId = key.targetPostId,
                        focusPage = key.targetPage,
                        onBack = {
                            if (backStack.isNotEmpty()) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onReply = {
                            routeDetail.thread?.let { thread ->
                                showPostReply(thread.id)
                            }
                        },
                        onSubscribe = {
                            routeDetail.thread?.let { thread ->
                                viewModel.addSubscription(thread.id)
                            }
                        },
                        onRefresh = {
                            viewModel.openThread(key.threadId, forceRefresh = true)
                        },
                        onLoadMore = { viewModel.loadMoreReplies() },
                        onProgressChanged = { index, offset ->
                            viewModel.saveThreadProgress(key.threadId, index, offset)
                        },
                        onImageClick = { image, ext, previewState ->
                            showImageViewer(image = image, ext = ext, previewState = previewState)
                        }
                    )
                }

                is AppRoute.ImageViewer -> NavEntry(key) {
                    ImageViewerScreen(
                        image = key.image,
                        ext = key.ext,
                        previewState = imageViewerPreviewState
                            ?.takeIf { it.image == key.image && it.ext == key.ext },
                        onBack = {
                            imageViewerPreviewState = null
                            if (backStack.isNotEmpty()) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }

                AppRoute.Settings -> NavEntry(key) {
                    SettingsScreen(
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

private sealed interface AppRoute {
    data object Catalog : AppRoute
    data object Subscriptions : AppRoute
    data object History : AppRoute
    data object PostingHistory : AppRoute
    data object Search : AppRoute
    data class PostThread(val initialForumId: Long?, val draftId: Long? = null) : AppRoute
    data class PostReply(val threadId: Long, val draftId: Long? = null) : AppRoute
    data class Thread(
        val threadId: Long,
        val targetPostId: Long? = null,
        val targetPage: Int? = null
    ) : AppRoute
    data class ImageViewer(
        val image: String,
        val ext: String?
    ) : AppRoute
    data object Settings : AppRoute
}

private fun Scene<AppRoute>.isImageViewerScene(): Boolean {
    return key.toString().startsWith("ImageViewer(")
}
