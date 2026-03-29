package ink.duo3.fogisland.ui

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.LocalTimeSettings
import ink.duo3.fogisland.shared.model.CatalogSource
import ink.duo3.fogisland.shared.model.CatalogType
import ink.duo3.fogisland.shared.model.NmbPost
import ink.duo3.fogisland.shared.model.buildForumNameMap
import ink.duo3.fogisland.shared.model.resolveForumName
import ink.duo3.fogisland.shared.model.toNmbTimeFormatOptions
import ink.duo3.fogisland.shared.util.formatNmbPostedAt
import ink.duo3.fogisland.ui.components.ErrorMessageCard
import ink.duo3.fogisland.ui.components.FogIslandPreviewColumn
import ink.duo3.fogisland.ui.components.ListItemAnimatedVisibility
import ink.duo3.fogisland.ui.components.NmbPostCard
import ink.duo3.fogisland.ui.components.NmbPreviewSamples
import ink.duo3.fogisland.ui.components.SiteNoticeCard
import ink.duo3.fogisland.viewmodel.ForumBrowseUiState

@Composable
fun ForumScreen(
    state: ForumBrowseUiState,
    onMenuClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onPostClick: () -> Unit,
    onLoadMore: () -> Unit,
    onThreadClick: (Long) -> Unit,
    onHideThreadClick: (Long) -> Unit,
    onHideTimelineForumClick: (Long, Long, (Boolean) -> Unit) -> Unit,
    onDismissSiteNotice: () -> Unit,
    onDismissSiteNoticeUntilChanged: () -> Unit,
    onImageClick: (String, String?) -> Unit
) {
    val source = state.currentSource
    val timelineId = source
        ?.takeIf { it.type == CatalogType.TIMELINE }
        ?.id
    val sourceKey = remember(source) { source?.let { "${it.type}:${it.id}" } }
    val forumNameById = buildForumNameMap(state.forumGroups)
    val timeSettings = LocalTimeSettings.current
    val timeFormatOptions = remember(timeSettings) { timeSettings.toNmbTimeFormatOptions() }
    var renderedSiteNotice by remember { mutableStateOf(state.siteNotice) }
    var actionTarget by remember { mutableStateOf<NmbPost?>(null) }
    var displayedThreads by remember { mutableStateOf(state.threads) }
    var removingThreadIds by remember { mutableStateOf(emptySet<Long>()) }
    var removingTimelineForumThreadIds by remember { mutableStateOf(emptySet<Long>()) }
    val listState = rememberLazyListState()
    val siteNoticeVisibility = remember { MutableTransitionState(state.siteNotice != null) }

    LaunchedEffect(sourceKey) {
        displayedThreads = state.threads
        removingThreadIds = emptySet()
        removingTimelineForumThreadIds = emptySet()
        actionTarget = null
    }

    LaunchedEffect(state.threads, state.error) {
        displayedThreads = mergeAnimatedPosts(
            current = displayedThreads,
            source = state.threads,
            shouldKeep = { post ->
                post.id in removingThreadIds || post.id in removingTimelineForumThreadIds
            }
        )
    }

    LaunchedEffect(state.siteNotice) {
        if (state.siteNotice != null) {
            renderedSiteNotice = state.siteNotice
        }
        siteNoticeVisibility.targetState = state.siteNotice != null
    }

    LaunchedEffect(
        siteNoticeVisibility.currentState,
        siteNoticeVisibility.targetState,
        renderedSiteNotice,
        state.siteNotice
    ) {
        if (
            !siteNoticeVisibility.currentState &&
            !siteNoticeVisibility.targetState &&
            state.siteNotice == null
        ) {
            renderedSiteNotice = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        bottomBar = {
            ForumBottomAppBar(
                source = source,
                onMenuClick = onMenuClick,
                onRefreshClick = onRefreshClick,
                onPostClick = onPostClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = innerPadding
        ) {
            if (state.isLoadingIndex || source == null) {
                item {
                    val message = when {
                        state.isLoadingIndex && source == null ->
                            "正在加载板块和时间线…"

                        source == null ->
                            "从左侧菜单选择一个板块或时间线。"

                        else -> null
                    }

                    message?.let {
                        Text(
                            text = it,
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            if (renderedSiteNotice != null || siteNoticeVisibility.currentState || siteNoticeVisibility.targetState) {
                item {
                    ListItemAnimatedVisibility(
                        visibleState = siteNoticeVisibility,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        renderedSiteNotice?.let { siteNotice ->
                            SiteNoticeCard(
                                notice = siteNotice,
                                publishedAtText = siteNotice.publishedAt?.let { publishedAt ->
                                    formatNmbPostedAt(
                                        epochMillis = publishedAt,
                                        options = timeFormatOptions
                                    )?.dateText
                                },
                                onDismissClick = onDismissSiteNotice,
                                onDismissPermanentlyClick = onDismissSiteNoticeUntilChanged,
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            state.error?.let { error ->
                item {
                    ErrorMessageCard(
                        error = error,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            items(
                items = displayedThreads,
                key = { thread -> thread.id },
            ) { thread ->
                val visibilityState = remember(thread.id, thread.forumId) {
                    MutableTransitionState(
                        thread.id !in removingThreadIds &&
                            thread.id !in removingTimelineForumThreadIds
                    )
                }
                LaunchedEffect(
                    thread.id,
                    removingThreadIds,
                    removingTimelineForumThreadIds
                ) {
                    visibilityState.targetState =
                        thread.id !in removingThreadIds &&
                            thread.id !in removingTimelineForumThreadIds
                }
                LaunchedEffect(
                    thread.id,
                    thread.threadId,
                    removingThreadIds,
                    removingTimelineForumThreadIds,
                    visibilityState.currentState,
                    visibilityState.targetState
                ) {
                    if (visibilityState.currentState || visibilityState.targetState) {
                        return@LaunchedEffect
                    }

                    if (thread.id in removingThreadIds) {
                        displayedThreads = displayedThreads.filterNot { it.id == thread.id }
                        removingThreadIds = removingThreadIds - thread.id
                        onHideThreadClick(thread.threadId)
                        return@LaunchedEffect
                    }

                    if (thread.id !in removingTimelineForumThreadIds) {
                        return@LaunchedEffect
                    }

                    displayedThreads = displayedThreads.filterNot { it.id == thread.id }
                    removingTimelineForumThreadIds = removingTimelineForumThreadIds - thread.id
                }
                ListItemAnimatedVisibility(visibleState = visibilityState) {
                    ThreadCard(
                        thread = thread,
                        forumName = if (source?.type == ink.duo3.fogisland.shared.model.CatalogType.TIMELINE) {
                            resolveForumName(thread.forumId, forumNameById)
                        } else {
                            null
                        },
                        onClick = { onThreadClick(thread.id) },
                        onImageClick = onImageClick,
                        onLongClick = { actionTarget = thread }
                    )
                }
            }

            if (source != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onLoadMore,
                            enabled = !state.isLoadingCatalog
                        ) {
                            Text(
                                if (state.isLoadingCatalog) "加载中…" else "加载更多"
                            )
                        }
                    }
                }
            }
        }
    }

    actionTarget?.let { thread ->
        val canHideTimelineForum =
            source?.type == CatalogType.TIMELINE &&
                timelineId != null &&
                thread.forumId != null
        val forumName = resolveForumName(thread.forumId, forumNameById)
        AlertDialog(
            onDismissRequest = { actionTarget = null },
            title = { Text("更多操作") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("选择对 No.${thread.remoteId} 的操作")
                    TextButton(
                        onClick = {
                            actionTarget = null
                            removingThreadIds = removingThreadIds + thread.id
                        }
                    ) {
                        Text("屏蔽串")
                    }
                    if (canHideTimelineForum) {
                        TextButton(
                            onClick = {
                                val forumId = requireNotNull(thread.forumId)
                                val resolvedTimelineId = requireNotNull(timelineId)
                                val visibleThreadIds = listState.layoutInfo.visibleItemsInfo
                                    .mapNotNull { item -> item.key as? Long }
                                    .toSet()
                                val animatedThreadIds = displayedThreads
                                    .filter { post ->
                                        post.forumId == forumId && post.id in visibleThreadIds
                                    }
                                    .map { it.id }
                                    .toSet()
                                actionTarget = null
                                removingTimelineForumThreadIds =
                                    removingTimelineForumThreadIds + animatedThreadIds
                                onHideTimelineForumClick(
                                    resolvedTimelineId,
                                    forumId
                                ) { success ->
                                    if (!success) {
                                        removingTimelineForumThreadIds =
                                            removingTimelineForumThreadIds - animatedThreadIds
                                        displayedThreads = mergeAnimatedPosts(
                                            current = displayedThreads,
                                            source = state.threads,
                                            shouldKeep = { post ->
                                                post.id in removingThreadIds ||
                                                    post.id in removingTimelineForumThreadIds
                                            }
                                        )
                                    }
                                }
                            }
                        ) {
                            Text(
                                forumName?.let { "在时间线中屏蔽「$it」" }
                                    ?: "在时间线中屏蔽此板块"
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { actionTarget = null }) {
                    Text("取消")
                }
            }
        )
    }
}

private fun mergeAnimatedPosts(
    current: List<NmbPost>,
    source: List<NmbPost>,
    shouldKeep: (NmbPost) -> Boolean
): List<NmbPost> {
    val sourceById = source.associateBy { it.id }
    val consumedIds = mutableSetOf<Long>()
    return buildList {
        current.forEach { post ->
            when {
                post.id in sourceById -> {
                    add(sourceById.getValue(post.id))
                    consumedIds += post.id
                }

                shouldKeep(post) -> add(post)
            }
        }

        source.forEach { post ->
            if (post.id !in consumedIds) {
                add(post)
            }
        }
    }
}

@Composable
private fun ThreadCard(
    thread: NmbPost,
    forumName: String?,
    onClick: () -> Unit,
    onImageClick: (String, String?) -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    NmbPostCard(
        post = thread,
        forumName = forumName,
        onClick = onClick,
        onImageClick = onImageClick,
        modifier = Modifier,
        onLongClick = onLongClick,
        bodyMaxLines = 6,
        bodyOverflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ForumBottomAppBar(
    source: CatalogSource?,
    onMenuClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onPostClick: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        actions = {
            Row(
                modifier = Modifier
                    .clickable(onClick = onMenuClick)
                    .padding(start = 4.dp, end = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "菜单")
                }
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = source?.title ?: "雾岛",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                IconButton(onClick = onRefreshClick) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onPostClick,
                containerColor = if (isSystemInDarkTheme())
                    BottomAppBarDefaults.bottomAppBarFabColor
                else
                    MaterialTheme.colorScheme.secondaryContainer
                        .copy(0.92f)
                        .compositeOver(MaterialTheme.colorScheme.secondary),
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) {
                Icon(Icons.Default.Add, contentDescription = "发串")
            }
        }
    )
}
