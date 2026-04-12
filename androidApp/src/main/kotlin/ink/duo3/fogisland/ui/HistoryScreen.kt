package ink.duo3.fogisland.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.shared.model.ErrorPresentation
import ink.duo3.fogisland.shared.model.ForumGroup
import ink.duo3.fogisland.shared.model.NmbPost
import ink.duo3.fogisland.shared.model.buildForumNameMap
import ink.duo3.fogisland.shared.model.resolveForumName
import ink.duo3.fogisland.ui.components.ErrorMessageCard
import ink.duo3.fogisland.ui.components.preview.FogIslandPreviewColumn
import ink.duo3.fogisland.ui.components.ListItemAnimatedVisibility
import ink.duo3.fogisland.ui.components.post.NmbPostCard
import ink.duo3.fogisland.ui.components.preview.NmbPreviewSamples

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    forumGroups: List<ForumGroup>,
    history: List<NmbPost>,
    error: ErrorPresentation?,
    onMenuClick: () -> Unit,
    onThreadClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onClearAllClick: () -> Unit,
    onImageClick: (String, String?) -> Unit,
    onThreadLongClick: ((Long) -> Unit)? = null
) {
    val forumNameById = remember(forumGroups) { buildForumNameMap(forumGroups) }
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var deletingThreadId by rememberSaveable { mutableStateOf<Long?>(null) }
    var isClearAllDialogVisible by rememberSaveable { mutableStateOf(false) }
    var displayedHistory by remember { mutableStateOf(history) }
    var removingThreadIds by remember { mutableStateOf(emptySet<Long>()) }

    LaunchedEffect(history, error) {
        displayedHistory = mergeAnimatedPosts(
            current = displayedHistory,
            source = history,
            removingIds = removingThreadIds
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("历史")
                        Text(
                            text = if (displayedHistory.isEmpty()) "还没有阅读记录" else "共 ${displayedHistory.size} 条阅读记录",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isClearAllDialogVisible = true },
                        enabled = displayedHistory.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "清空历史")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                scrollBehavior = topAppBarScrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp),
            contentPadding = innerPadding
        ) {
            error?.let { errorState ->
                item {
                    ErrorMessageCard(
                        error = errorState,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            if (displayedHistory.isEmpty()) {
                item {
                    Text(
                        text = "开始阅读串之后，这里会按最近阅读时间记录历史。",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            items(
                items = displayedHistory,
                key = { thread -> thread.id }
            ) { thread ->
                val visibilityState = remember(thread.id) {
                    MutableTransitionState(thread.id !in removingThreadIds)
                }
                LaunchedEffect(thread.id, removingThreadIds) {
                    visibilityState.targetState = thread.id !in removingThreadIds
                }
                LaunchedEffect(
                    thread.id,
                    removingThreadIds,
                    visibilityState.currentState,
                    visibilityState.targetState
                ) {
                    if (
                        thread.id in removingThreadIds &&
                        !visibilityState.currentState &&
                        !visibilityState.targetState
                    ) {
                        displayedHistory = displayedHistory.filterNot { it.id == thread.id }
                        removingThreadIds = removingThreadIds - thread.id
                        onDeleteClick(thread.id)
                    }
                }
                ListItemAnimatedVisibility(visibleState = visibilityState) {
                    HistoryThreadCard(
                        thread = thread,
                        forumName = resolveForumName(thread.forumId, forumNameById),
                        onClick = { onThreadClick(thread.id) },
                        onImageClick = onImageClick,
                        onLongClick = {
                            onThreadLongClick?.invoke(thread.id) ?: run {
                                deletingThreadId = thread.id
                            }
                        }
                    )
                }
            }
        }
    }

    deletingThreadId?.let { threadId ->
        AlertDialog(
            onDismissRequest = { deletingThreadId = null },
            title = { Text("删除阅读记录") },
            text = { Text("确定删除这条阅读记录吗？这不会删除本地缓存的串内容。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingThreadId = null
                        removingThreadIds = removingThreadIds + threadId
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingThreadId = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (isClearAllDialogVisible) {
        AlertDialog(
            onDismissRequest = { isClearAllDialogVisible = false },
            title = { Text("清空阅读历史") },
            text = { Text("确定清空全部阅读记录吗？这不会删除本地缓存的串内容。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isClearAllDialogVisible = false
                        onClearAllClick()
                    }
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { isClearAllDialogVisible = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private fun mergeAnimatedPosts(
    current: List<NmbPost>,
    source: List<NmbPost>,
    removingIds: Set<Long>
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

                post.id in removingIds -> add(post)
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
private fun HistoryThreadCard(
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

@Preview(name = "History Thread Card", widthDp = 412, heightDp = 520)
@Composable
private fun HistoryThreadCardPreview() {
    FogIslandPreviewColumn {
        HistoryThreadCard(
            thread = NmbPreviewSamples.historyThread,
            forumName = "欢乐恶搞",
            onClick = {},
            onImageClick = { _, _ -> }
        )
    }
}

@Preview(name = "History Screen", widthDp = 412, heightDp = 960)
@Composable
private fun HistoryScreenPreview() {
    FogIslandPreviewColumn {
        HistoryScreen(
            forumGroups = NmbPreviewSamples.forumGroups,
            history = listOf(NmbPreviewSamples.historyThread),
            error = null,
            onMenuClick = {},
            onThreadClick = {},
            onDeleteClick = {},
            onClearAllClick = {},
            onImageClick = { _, _ -> }
        )
    }
}
