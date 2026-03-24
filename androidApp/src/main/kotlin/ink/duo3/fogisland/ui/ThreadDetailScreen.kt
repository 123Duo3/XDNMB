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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.LocalTimeSettings
import ink.duo3.fogisland.shared.model.toNmbTimeFormatOptions
import ink.duo3.fogisland.shared.model.ThreadDetail
import ink.duo3.fogisland.shared.util.NmbTimeFormatOptions
import ink.duo3.fogisland.shared.util.formatNmbPostedAtText
import ink.duo3.fogisland.shared.util.resolveNmbDisplayAuthor
import ink.duo3.fogisland.shared.util.resolveNmbDisplayTitle
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
    errorMessage: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onProgressChanged: (Int, Int) -> Unit
) {
    val thread = detail.thread
    val posts = detail.posts
    val progress = detail.progress
    val listState = rememberLazyListState()
    var restoredProgressKey by remember { mutableStateOf<Pair<Long?, Long>?>(null) }
    val timeSettings = LocalTimeSettings.current
    val timeFormatOptions = remember(timeSettings) { timeSettings.toNmbTimeFormatOptions() }

    LaunchedEffect(thread?.id, posts.size, progress?.updatedAt) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            errorMessage?.let { message ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            thread?.let { root ->
                item(key = "thread-${root.id}") {
                    PostCard(
                        title = resolveNmbDisplayTitle(root.title),
                        author = resolveNmbDisplayAuthor(root.userHash, root.name),
                        postedAtEpochMillis = root.postedAtEpochMillis,
                        content = root.contentText,
                        timeFormatOptions = timeFormatOptions
                    )
                }
            }

            items(
                items = posts,
                key = { post -> "${post.threadId}-${post.id}" }
            ) { post ->
                PostCard(
                    title = resolveNmbDisplayTitle(post.title),
                    author = resolveNmbDisplayAuthor(post.userHash, post.name),
                    postedAtEpochMillis = post.postedAtEpochMillis,
                    content = post.contentText,
                    timeFormatOptions = timeFormatOptions
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
private fun PostCard(
    title: String?,
    author: String?,
    postedAtEpochMillis: Long?,
    content: String,
    timeFormatOptions: NmbTimeFormatOptions
) {
    val postedAtText = formatNmbPostedAtText(
        epochMillis = postedAtEpochMillis,
        options = timeFormatOptions
    )
    val metaText = listOfNotNull(author, postedAtText)
        .joinToString(separator = " · ")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (metaText.isNotBlank()) {
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = content.ifBlank { "(空内容)" },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
