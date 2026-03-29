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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.LocalTimeSettings
import ink.duo3.fogisland.data.ensureSubscriptionUuid
import ink.duo3.fogisland.data.subscriptionUuidFlow
import ink.duo3.fogisland.shared.model.ErrorPresentation
import ink.duo3.fogisland.shared.model.ForumGroup
import ink.duo3.fogisland.shared.model.NmbPost
import ink.duo3.fogisland.shared.model.buildForumNameMap
import ink.duo3.fogisland.shared.model.resolveForumName
import ink.duo3.fogisland.shared.model.toNmbTimeFormatOptions
import ink.duo3.fogisland.shared.util.formatNmbPostedAtText
import ink.duo3.fogisland.ui.components.ErrorMessageCard
import ink.duo3.fogisland.ui.components.FogIslandPreviewColumn
import ink.duo3.fogisland.ui.components.NmbPostCard
import ink.duo3.fogisland.ui.components.NmbPreviewSamples
import ink.duo3.fogisland.ui.components.SubscriptionUuidEditorDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    forumGroups: List<ForumGroup>,
    threads: List<NmbPost>,
    loadedPage: Int,
    isLoading: Boolean,
    error: ErrorPresentation?,
    onMenuClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onLoadMore: () -> Unit,
    onThreadClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onImageClick: (String, String?) -> Unit,
    onThreadLongClick: ((Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val inspectionMode = LocalInspectionMode.current
    val forumNameById = remember(forumGroups) { buildForumNameMap(forumGroups) }
    val subscriptionUuid by if (inspectionMode) {
        remember { mutableStateOf("preview-subscription-uuid") }
    } else {
        context.subscriptionUuidFlow.collectAsState(initial = null)
    }
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var isSubscriptionUuidDialogVisible by rememberSaveable { mutableStateOf(false) }
    var deletingThreadId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(context, inspectionMode) {
        if (!inspectionMode) {
            context.ensureSubscriptionUuid()
        }
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
                        Text("订阅")
                        Text(
                            text = if (loadedPage > 0) "已加载到第${loadedPage}页" else "尚未加载订阅",
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
                    IconButton(onClick = { isSubscriptionUuidDialogVisible = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "修改订阅 ID")
                    }
                    IconButton(onClick = onRefreshClick) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
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
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            error?.let { errorState ->
                item {
                    ErrorMessageCard(
                        error = errorState,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            if (threads.isEmpty()) {
                item {
                    Text(
                        text = if (isLoading) "正在加载订阅…" else "还没有订阅任何串。",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            items(
                items = threads,
                key = { thread -> thread.id }
            ) { thread ->
                SubscriptionThreadCard(
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

            if (threads.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onLoadMore,
                            enabled = !isLoading
                        ) {
                            Text(
                                if (isLoading) "加载中…" else "加载更多"
                            )
                        }
                    }
                }
            }
        }
    }

    if (isSubscriptionUuidDialogVisible) {
        SubscriptionUuidEditorDialog(
            currentUuid = subscriptionUuid,
            onDismissRequest = { isSubscriptionUuidDialogVisible = false },
            onSaved = onRefreshClick
        )
    }

    deletingThreadId?.let { threadId ->
        AlertDialog(
            onDismissRequest = { deletingThreadId = null },
            title = { Text("取消订阅") },
            text = { Text("确定取消订阅这条串吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingThreadId = null
                        onDeleteClick(threadId)
                    }
                ) {
                    Text("取消订阅")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingThreadId = null }) {
                    Text("返回")
                }
            }
        )
    }
}

@Composable
private fun SubscriptionThreadCard(
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
        onLongClick = onLongClick,
        modifier = Modifier
            .padding(horizontal = 16.dp),
        bodyMaxLines = 6,
        bodyOverflow = TextOverflow.Ellipsis
    )
}

@Preview(name = "Subscription Thread Card", widthDp = 412, heightDp = 520)
@Composable
private fun SubscriptionThreadCardPreview() {
    FogIslandPreviewColumn {
        SubscriptionThreadCard(
            thread = NmbPreviewSamples.subscriptionThread,
            forumName = "手游综合",
            onClick = {},
            onImageClick = { _, _ -> }
        )
    }
}

@Preview(name = "Subscription Screen", widthDp = 412, heightDp = 960)
@Composable
private fun SubscriptionScreenPreview() {
    FogIslandPreviewColumn {
        SubscriptionScreen(
            forumGroups = NmbPreviewSamples.forumGroups,
            threads = listOf(NmbPreviewSamples.subscriptionThread),
            loadedPage = 3,
            isLoading = false,
            error = null,
            onMenuClick = {},
            onRefreshClick = {},
            onLoadMore = {},
            onThreadClick = {},
            onDeleteClick = {},
            onImageClick = { _, _ -> }
        )
    }
}
