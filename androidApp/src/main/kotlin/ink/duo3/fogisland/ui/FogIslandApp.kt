package ink.duo3.fogisland.ui

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.navigation3.ui.NavDisplay
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
import ink.duo3.fogisland.shared.util.NmbLinkTarget
import ink.duo3.fogisland.shared.util.htmlToPlainText
import ink.duo3.fogisland.shared.util.shouldRenderNmbRichText
import ink.duo3.fogisland.ui.components.ForumDrawerItem
import ink.duo3.fogisland.ui.components.NavigationItemGroupHeader
import ink.duo3.fogisland.ui.components.richtext.NmbRichTextText
import ink.duo3.fogisland.utils.openNmbExternalLink
import ink.duo3.fogisland.utils.resolveNmbIntentLinkTarget
import ink.duo3.fogisland.viewmodel.ForumBrowseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@Composable
fun FogIslandApp(
    incomingIntents: Flow<Intent> = emptyFlow()
) {
    val context = LocalContext.current
    val repository = remember(context) { RepositoryProvider.provideForumRepository(context.applicationContext) }
    val viewModel = viewModel<ForumBrowseViewModel>(factory = ForumBrowseViewModel.factory())
    val state by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack = remember { mutableStateListOf<AppRoute>(AppRoute.Catalog) }
    var expandedGroupId by remember { mutableStateOf<Long?>(null) }
    val drawerListState = rememberLazyListState()
    var favoriteDialogSource by remember { mutableStateOf<CatalogSource?>(null) }
    val favoriteItems = remember(state.forumGroups, state.favoriteForumIds, state.timelines, state.favoriteTimelineIds) {
        buildList {
            state.timelines
                .filter { it.id in state.favoriteTimelineIds }
                .forEach { add(it.toCatalogSource()) }
            state.forumGroups.flatMap { group ->
                group.forums
                    .filter { it.id in state.favoriteForumIds }
                    .map { it.toCatalogSource(group) }
            }.forEach { add(it) }
        }
    }

    val favoriteStarBadge: @Composable () -> Unit = {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    }

    val fds = favoriteDialogSource
    if (fds != null) {
        val isFavorite = when (fds.type) {
            CatalogType.FORUM -> fds.id in state.favoriteForumIds
            CatalogType.TIMELINE -> fds.id in state.favoriteTimelineIds
        }
        AlertDialog(
            onDismissRequest = { favoriteDialogSource = null },
            title = { Text(fds.title) },
            text = { Text(if (isFavorite) "从收藏中移除？" else "添加到收藏？") },
            confirmButton = {
                TextButton(onClick = {
                    if (isFavorite) {
                        when (fds.type) {
                            CatalogType.FORUM -> viewModel.removeFavoriteForum(fds.id)
                            CatalogType.TIMELINE -> viewModel.removeFavoriteTimeline(fds.id)
                        }
                    } else {
                        when (fds.type) {
                            CatalogType.FORUM -> viewModel.addFavoriteForum(fds.id)
                            CatalogType.TIMELINE -> viewModel.addFavoriteTimeline(fds.id)
                        }
                    }
                    favoriteDialogSource = null
                }) { Text(if (isFavorite) "移除" else "收藏") }
            },
            dismissButton = {
                TextButton(onClick = { favoriteDialogSource = null }) { Text("取消") }
            }
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val currentRoute = backStack.lastOrNull() ?: AppRoute.Catalog
    val isCatalogRoute = currentRoute == AppRoute.Catalog
    val forumNameById = remember(state.forumGroups) { buildForumNameMap(state.forumGroups) }

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

    fun showHiddenContent() {
        viewModel.clearHiddenContentError()
        if (backStack.lastOrNull() != AppRoute.Settings) {
            backStack.clear()
            backStack.add(AppRoute.Catalog)
            backStack.add(AppRoute.Settings)
        }
        if (backStack.lastOrNull() != AppRoute.HiddenContent) {
            backStack.add(AppRoute.HiddenContent)
        }
    }

    fun showPostThread(defaultForumId: Long?, draftId: Long? = null) {
        backStack.clear()
        backStack.add(AppRoute.Catalog)
        backStack.add(AppRoute.PostThread(defaultForumId, draftId))
    }

    fun showPostReply(threadId: Long, draftId: Long? = null) {
        backStack.add(AppRoute.PostReply(threadId, draftId))
    }

    fun showThread(
        threadId: Long,
        targetPostId: Long? = null,
        targetPage: Int? = null,
        forceRefresh: Boolean = false
    ) {
        viewModel.openThread(
            threadId = threadId,
            forceRefresh = forceRefresh,
            targetPage = targetPage
        )
        val route = AppRoute.Thread(
            threadId = threadId,
            targetPostId = targetPostId,
            targetPage = targetPage
        )
        val currentThreadRoute = backStack.lastOrNull() as? AppRoute.Thread
        if (currentThreadRoute?.threadId == threadId) {
            backStack[backStack.lastIndex] = route
        } else {
            backStack.add(route)
        }
    }

    fun showImageViewer(
        image: String,
        ext: String?
    ) {
        backStack.add(AppRoute.ImageViewer(image = image, ext = ext))
    }

    fun showImageViewer(localImagePath: String) {
        backStack.add(AppRoute.ImageViewer(localImagePath = localImagePath))
    }

    fun handleLinkTarget(target: NmbLinkTarget) {
        scope.launch {
            when (target) {
                is NmbLinkTarget.ExternalUrl -> openNmbExternalLink(context, target.url)
                is NmbLinkTarget.PostReference -> {
                    val resolvedThreadId = repository.resolveThreadIdByPostReference(target.postId)
                    showThread(
                        threadId = resolvedThreadId ?: target.postId,
                        targetPostId = resolvedThreadId
                            ?.takeIf { it != target.postId }
                            ?.let { target.postId }
                    )
                }
                is NmbLinkTarget.Thread -> showThread(
                    threadId = target.threadId,
                    targetPostId = target.targetPostId,
                    targetPage = target.targetPage
                )
            }
        }
    }

    LaunchedEffect(incomingIntents) {
        incomingIntents.collect { intent ->
            resolveNmbIntentLinkTarget(intent)?.let(::handleLinkTarget)
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
                        LaunchedEffect(expandedGroupId) {
                            val targetId = expandedGroupId ?: return@LaunchedEffect
                            val hasTimelines = state.timelines.isNotEmpty()
                            val favoritesOffset = if (favoriteItems.isNotEmpty()) 2 else 0
                            val index = when (targetId) {
                                -2L -> 0
                                -1L -> favoritesOffset
                                else -> {
                                    var idx = favoritesOffset + if (hasTimelines) 2 else 0
                                    for (group in state.forumGroups) {
                                        if (group.id == targetId) break
                                        idx += 2
                                    }
                                    idx
                                }
                            }
                            drawerListState.animateScrollToItem(index)
                        }
                        LazyColumn(
                            state = drawerListState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            if (favoriteItems.isNotEmpty()) {
                                stickyHeader(key = "favorites_header") {
                                    NavigationItemGroupHeader(
                                        label = { DrawerItemLabel("收藏") },
                                        selected = isCatalogRoute && favoriteItems.any { it == state.currentSource },
                                        expanded = expandedGroupId == -2L,
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow),
                                        onClick = { expandedGroupId = if (expandedGroupId == -2L) null else -2L }
                                    )
                                }
                                item(key = "favorites_content") {
                                    AnimatedVisibility(visible = expandedGroupId == -2L) {
                                        Column(Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)) {
                                            favoriteItems.forEach { source ->
                                                ForumDrawerItem(
                                                    label = source.title,
                                                    selected = isCatalogRoute && state.currentSource == source,
                                                    onClick = {
                                                        viewModel.openSource(source)
                                                        showCatalog()
                                                        scope.launch { drawerState.close() }
                                                    },
                                                    onLongClick = { favoriteDialogSource = source },
                                                    badge = favoriteStarBadge
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (state.timelines.isNotEmpty()) {
                                stickyHeader(key = "timeline_header") {
                                    NavigationItemGroupHeader(
                                        label = { DrawerItemLabel("时间线") },
                                        selected = isCatalogRoute && state.currentSource?.type == CatalogType.TIMELINE && state.currentSource !in favoriteItems,
                                        expanded = expandedGroupId == -1L,
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow),
                                        onClick = { expandedGroupId = if (expandedGroupId == -1L) null else -1L }
                                    )
                                }
                                item(key = "timeline_content") {
                                    AnimatedVisibility(visible = expandedGroupId == -1L) {
                                        Column(Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)) {
                                            state.timelines.forEach { timeline ->
                                                val source = timeline.toCatalogSource()
                                                ForumDrawerItem(
                                                    label = timeline.displayName,
                                                    selected = isCatalogRoute && state.currentSource == source,
                                                    onClick = {
                                                        viewModel.openSource(source)
                                                        showCatalog()
                                                        scope.launch { drawerState.close() }
                                                    },
                                                    onLongClick = { favoriteDialogSource = source },
                                                    badge = if (timeline.id in state.favoriteTimelineIds) favoriteStarBadge else null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            state.forumGroups.forEach { group ->
                                val groupSelected = isCatalogRoute &&
                                    state.currentSource?.type == CatalogType.FORUM &&
                                    group.forums.any { it.id == state.currentSource?.id } &&
                                    state.currentSource !in favoriteItems
                                stickyHeader(key = "group_${group.id}") {
                                    NavigationItemGroupHeader(
                                        label = { DrawerItemLabel(group.name) },
                                        selected = groupSelected,
                                        expanded = expandedGroupId == group.id,
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow),
                                        onClick = { expandedGroupId = if (expandedGroupId == group.id) null else group.id }
                                    )
                                }
                                item(key = "group_content_${group.id}") {
                                    AnimatedVisibility(visible = expandedGroupId == group.id) {
                                        Column(Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)) {
                                            group.forums.forEach { forum ->
                                                val source = forum.toCatalogSource(group)
                                                ForumDrawerItem(
                                                    label = forum.displayName,
                                                    selected = isCatalogRoute && state.currentSource == source,
                                                    onClick = {
                                                        viewModel.openSource(source)
                                                        showCatalog()
                                                        scope.launch { drawerState.close() }
                                                    },
                                                    onLongClick = { favoriteDialogSource = source },
                                                    badge = if (forum.id in state.favoriteForumIds) favoriteStarBadge else null
                                                )
                                            }
                                        }
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
            ) { key: AppRoute ->
                when (key) {
                AppRoute.Catalog -> NavEntry(key) {
                    ForumScreen(
                        state = state,
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
                            showThread(threadId)
                        },
                        onHideThreadClick = { threadId ->
                            viewModel.hideThread(threadId)
                        },
                        onHideTimelineForumClick = { timelineId, forumId, onCompleted ->
                            viewModel.hideTimelineForum(
                                timelineId = timelineId,
                                forumId = forumId,
                                onCompleted = onCompleted
                            )
                        },
                        onDismissSiteNotice = { viewModel.dismissSiteNotice() },
                        onDismissSiteNoticeUntilChanged = {
                            viewModel.dismissSiteNoticeUntilChanged()
                        },
                        onNoticeLinkClick = ::handleLinkTarget,
                        onImageClick = { image, ext ->
                            showImageViewer(image = image, ext = ext)
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
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onRefreshClick = { viewModel.refreshSubscriptions() },
                        onLoadMore = { viewModel.loadMoreSubscriptions() },
                        onThreadClick = { threadId ->
                            showThread(threadId)
                        },
                        onDeleteClick = { threadId ->
                            viewModel.deleteSubscription(threadId)
                        },
                        onImageClick = { image, ext ->
                            showImageViewer(image = image, ext = ext)
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
                            showThread(threadId)
                        },
                        onDeleteClick = { threadId ->
                            viewModel.deleteReadHistoryEntry(threadId)
                        },
                        onClearAllClick = {
                            viewModel.clearReadHistory()
                        },
                        onImageClick = { image, ext ->
                            showImageViewer(image = image, ext = ext)
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
                                showThread(
                                    threadId = threadId,
                                    targetPostId = entry.postId
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
                            showThread(threadId)
                        },
                        onResultClick = { hit ->
                            viewModel.submitSearchQuery(state.searchQuery)
                            showThread(
                                threadId = hit.threadId,
                                targetPostId = hit.postId,
                                targetPage = if (hit.type == ink.duo3.fogisland.shared.model.SearchHitType.POST) {
                                    hit.page
                                } else {
                                    null
                                }
                            )
                        }
                    )
                }

                is AppRoute.PostThread -> NavEntry(key) {
                    val postedThreadResult = state.postedThreadResult
                    LaunchedEffect(postedThreadResult) {
                        val result = postedThreadResult ?: return@LaunchedEffect
                        (result.submittedDraftId ?: key.draftId)?.let { draftId ->
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
                            showThread(threadId = threadId, forceRefresh = true)
                        }
                        viewModel.consumePostedThreadResult()
                    }

                    PostComposerScreen(
                        mode = PostComposerMode.Thread(
                            forumGroups = state.forumGroups,
                            initialForumId = key.initialForumId,
                            initialDraft = state.postingDrafts.firstOrNull { it.id == key.draftId },
                            isPosting = state.isPostingThread,
                            error = state.postThreadError,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onClearError = viewModel::clearPostThreadError,
                            onPreviewImage = ::showImageViewer,
                            onSubmit = viewModel::submitThreadPost
                        )
                    )
                }

                is AppRoute.PostReply -> NavEntry(key) {
                    val postedReplyResult = state.postedReplyResult
                    LaunchedEffect(postedReplyResult) {
                        val result = postedReplyResult ?: return@LaunchedEffect
                        (result.submittedDraftId ?: key.draftId)?.let { draftId ->
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
                            showThread(result.threadId)
                        }
                        val refreshThroughPage = if (state.activeThreadId == result.threadId) {
                            state.loadedThreadPage.coerceAtLeast(1)
                        } else {
                            1
                        }
                        showThread(
                            threadId = result.threadId,
                            forceRefresh = true,
                            targetPage = refreshThroughPage
                        )
                        viewModel.consumePostedReplyResult()
                    }

                    val currentThread = state.currentThread?.takeIf { it.id == key.threadId }
                    PostComposerScreen(
                        mode = PostComposerMode.Reply(
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
                            onPreviewImage = ::showImageViewer,
                            onSubmit = viewModel::submitReplyPost
                        )
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
                        onImageClick = { image, ext ->
                            showImageViewer(image = image, ext = ext)
                        },
                        onResolveCachedPostReference = repository::getCachedPostReference,
                        onQueryPostReference = { postId, preferredThreadId ->
                            repository.queryPostReference(
                                postId = postId,
                                preferredThreadId = preferredThreadId
                            )
                        },
                        onLoadCurrentThreadUntilPost = viewModel::loadCurrentThreadUntilPost,
                        onLinkClick = ::handleLinkTarget
                    )
                }

                is AppRoute.ImageViewer -> NavEntry(key) {
                    ImageViewerScreen(
                        image = key.image,
                        ext = key.ext,
                        localImagePath = key.localImagePath,
                        onBack = {
                            if (backStack.isNotEmpty()) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }

                AppRoute.Settings -> NavEntry(key) {
                    SettingsScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        hiddenThreadCount = state.hiddenThreadIds.size,
                        hiddenTimelineForumCount = state.hiddenTimelineForumFilters.size,
                        onHiddenContentClick = ::showHiddenContent
                    )
                }

                AppRoute.HiddenContent -> NavEntry(key) {
                    HiddenContentScreen(
                        forumGroups = state.forumGroups,
                        timelines = state.timelines,
                        hiddenThreadIds = state.hiddenThreadIds,
                        hiddenTimelineForumFilters = state.hiddenTimelineForumFilters,
                        error = state.hiddenContentError,
                        onBack = {
                            if (backStack.isNotEmpty()) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onUnhideThreadClick = { threadId ->
                            viewModel.unhideThread(threadId)
                        },
                        onUnhideTimelineForumClick = { timelineId, forumId ->
                            viewModel.unhideTimelineForum(timelineId, forumId)
                        }
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

@Composable
private fun DrawerItemLabel(text: String) {
    val contentColor = LocalContentColor.current
    if (shouldRenderNmbRichText(text)) {
        NmbRichTextText(
            html = text,
            fallbackText = htmlToPlainText(text),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            interactionsEnabled = false,
            maxLines = 1
        )
        return
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge
    )
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
        val image: String? = null,
        val ext: String? = null,
        val localImagePath: String? = null
    ) : AppRoute
    data object Settings : AppRoute
    data object HiddenContent : AppRoute
}
