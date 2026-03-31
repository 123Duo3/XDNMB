package ink.duo3.fogisland.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.LocalTimeSettings
import ink.duo3.fogisland.shared.model.ErrorPresentation
import ink.duo3.fogisland.shared.model.NmbPost
import ink.duo3.fogisland.shared.model.toNmbTimeFormatOptions
import ink.duo3.fogisland.shared.model.ThreadDetail
import ink.duo3.fogisland.shared.util.NmbLinkTarget
import ink.duo3.fogisland.shared.util.NmbTimeFormatOptions
import ink.duo3.fogisland.shared.util.formatNmbPostedAtText
import ink.duo3.fogisland.ui.components.ErrorMessageCard
import ink.duo3.fogisland.ui.components.FogIslandPreviewColumn
import ink.duo3.fogisland.ui.components.NmbPostFlatItem
import ink.duo3.fogisland.ui.components.NmbPreviewSamples
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ThreadDetailScreen(
    detail: ThreadDetail,
    forumName: String?,
    loadedPage: Int,
    isLoading: Boolean,
    canLoadMore: Boolean,
    error: ErrorPresentation?,
    focusPostId: Long? = null,
    focusPage: Int? = null,
    onBack: () -> Unit,
    onReply: () -> Unit,
    onSubscribe: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onProgressChanged: (Int, Int) -> Unit,
    onImageClick: (String, String?) -> Unit,
    onLinkClick: (NmbLinkTarget) -> Unit
) {
    val thread = detail.thread
    val posts = detail.posts
    val progress = detail.progress
    val listState = rememberLazyListState()
    var restoredFocusKey by remember { mutableStateOf<Triple<Long?, Long?, Int?>?>(null) }
    var restoredProgressKey by remember { mutableStateOf<Pair<Long?, Long>?>(null) }
    val timeSettings = LocalTimeSettings.current
    val timeFormatOptions = remember(timeSettings) { timeSettings.toNmbTimeFormatOptions() }
    val locatedFocusPostIndex = remember(posts, focusPostId) {
        focusPostId?.let { postId ->
            posts.indexOfFirst { it.id == postId || it.remoteId == postId }.takeIf { it >= 0 }
        }
    }
    val shouldSuppressProgressRestore = focusPage != null || locatedFocusPostIndex != null
    val handleRichTextLinkClick = remember(thread, posts, onLinkClick) {
        { target: NmbLinkTarget ->
            when (target) {
                is NmbLinkTarget.ExternalUrl -> onLinkClick(target)
                is NmbLinkTarget.Thread -> onLinkClick(target)
                is NmbLinkTarget.PostReference -> {
                    val currentThread = thread
                    val targetPostId = target.postId
                    val shouldOpenInCurrentThread = currentThread?.let { root ->
                        root.remoteId == targetPostId || posts.any { post -> post.remoteId == targetPostId }
                    } == true

                    currentThread
                        ?.takeIf { shouldOpenInCurrentThread }
                        ?.let {
                            onLinkClick(
                                NmbLinkTarget.Thread(
                                    threadId = it.id,
                                    targetPostId = targetPostId
                                )
                            )
                        }
                        ?: onLinkClick(target)
                }
            }
        }
    }

    LaunchedEffect(thread?.id, posts.size, focusPostId, focusPage) {
        val threadId = thread?.id ?: return@LaunchedEffect
        if (focusPostId == null && focusPage == null) {
            return@LaunchedEffect
        }

        val focusKey = Triple(threadId, focusPostId, focusPage)
        if (restoredFocusKey == focusKey) {
            return@LaunchedEffect
        }

        val targetIndex = focusPostId
            ?.let { locatedFocusPostIndex?.plus(1) }
            ?: focusPage?.let { page ->
                posts.indexOfFirst { it.page == page }.takeIf { it >= 0 }?.plus(1)
            }
            ?: return@LaunchedEffect

        listState.scrollToItem(index = targetIndex.coerceAtLeast(0))
        restoredFocusKey = focusKey
    }

    LaunchedEffect(thread?.id, posts.size, progress?.updatedAt) {
        if (shouldSuppressProgressRestore) {
            return@LaunchedEffect
        }
        val threadId = thread?.id ?: return@LaunchedEffect
        val readProgress = progress ?: return@LaunchedEffect
        val restoreKey = threadId to readProgress.updatedAt
        if (restoredProgressKey == restoreKey) {
            return@LaunchedEffect
        }

        val targetIndex = when (val postId = readProgress.lastReadPostId) {
            null -> readProgress.lastVisibleItemIndex
            else -> posts.indexOfFirst { it.id == postId }.takeIf { it >= 0 }?.plus(1)
                ?: readProgress.lastVisibleItemIndex
        }

        listState.scrollToItem(
            index = targetIndex.coerceAtLeast(0),
            scrollOffset = readProgress.lastVisibleItemOffset
        )
        restoredProgressKey = restoreKey
    }

    LaunchedEffect(thread?.id, posts.size) {
        if (thread == null) {
            return@LaunchedEffect
        }

        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
            .map { it.first to it.second }
            .distinctUntilChanged()
            .debounce(300)
            .collect { (index, offset) ->
                onProgressChanged(index, offset)
            }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(thread?.title?.ifBlank { "串 No.${thread.id}" } ?: "加载中…")
                        forumName?.takeIf { it.isNotBlank() }?.let { boardName ->
                            Text(
                                text = boardName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        thread?.let {
                            Text(
                                text = "已加载到第 $loadedPage 页",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onReply,
                        enabled = thread != null
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "回帖")
                    }
                    IconButton(
                        onClick = onSubscribe,
                        enabled = thread != null
                    ) {
                        Icon(Icons.Default.Bookmarks, contentDescription = "订阅")
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            error?.let { errorState ->
                item {
                    ErrorMessageCard(
                        error = errorState,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            thread?.let { root ->
                item(key = "thread-${root.id}") {
                    ThreadDetailPostItem(
                        post = root,
                        onImageClick = onImageClick,
                        onLinkClick = handleRichTextLinkClick
                    )
                }
            }

            items(
                items = posts,
                key = { post -> "${post.threadId}-${post.id}" }
            ) { post ->
                ThreadDetailPostItem(
                    post = post,
                    onImageClick = onImageClick,
                    onLinkClick = handleRichTextLinkClick
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Button(
                        onClick = onLoadMore,
                        enabled = canLoadMore && !isLoading
                    ) {
                        Text(
                            when {
                                isLoading -> "加载中…"
                                canLoadMore -> "加载更多回复"
                                else -> "已加载全部回复"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadDetailPostItem(
    post: NmbPost,
    onImageClick: (String, String?) -> Unit,
    onLinkClick: ((NmbLinkTarget) -> Unit)? = null
) {
    NmbPostFlatItem(
        post = post,
        onImageClick = onImageClick,
        onLinkClick = onLinkClick
    )
}

@Preview(name = "Thread Detail Items", widthDp = 412, heightDp = 900)
@Composable
private fun ThreadDetailPostItemPreview() {
    FogIslandPreviewColumn(verticalSpacingDp = 0) {
        ThreadDetailPostItem(
            post = NmbPreviewSamples.forumThread,
            onImageClick = { _, _ -> },
            onLinkClick = null
        )
        ThreadDetailPostItem(
            post = NmbPreviewSamples.replyPost,
            onImageClick = { _, _ -> },
            onLinkClick = null
        )
    }
}

@Preview(name = "Thread Detail Screen", widthDp = 412, heightDp = 960)
@Composable
private fun ThreadDetailScreenPreview() {
    FogIslandPreviewColumn {
        ThreadDetailScreen(
            detail = ink.duo3.fogisland.shared.model.ThreadDetail(
                thread = NmbPreviewSamples.forumThread,
                posts = listOf(NmbPreviewSamples.replyPost),
                progress = null
            ),
            forumName = "欢乐恶搞",
            loadedPage = 2,
            isLoading = false,
            canLoadMore = true,
            error = null,
            onBack = {},
            onReply = {},
            onSubscribe = {},
            onRefresh = {},
            onLoadMore = {},
            onProgressChanged = { _, _ -> },
            onImageClick = { _, _ -> },
            onLinkClick = {}
        )
    }
}
