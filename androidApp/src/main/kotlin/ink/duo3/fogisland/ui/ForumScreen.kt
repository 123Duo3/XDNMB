package ink.duo3.fogisland.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.LocalTimeSettings
import ink.duo3.fogisland.shared.model.CatalogSource
import ink.duo3.fogisland.shared.model.CatalogThread
import ink.duo3.fogisland.shared.model.buildForumNameMap
import ink.duo3.fogisland.shared.model.resolveForumName
import ink.duo3.fogisland.shared.model.toNmbTimeFormatOptions
import ink.duo3.fogisland.shared.util.formatNmbPostedAt
import ink.duo3.fogisland.shared.util.formatNmbPostedAtText
import ink.duo3.fogisland.shared.util.resolveNmbCardNameIdString
import ink.duo3.fogisland.shared.util.resolveNmbDisplayTitle
import ink.duo3.fogisland.ui.components.ErrorMessageCard
import ink.duo3.fogisland.viewmodel.ForumBrowseUiState

@Composable
fun ForumScreen(
    state: ForumBrowseUiState,
    onMenuClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onPostClick: () -> Unit,
    onLoadMore: () -> Unit,
    onThreadClick: (Long) -> Unit
) {
    val source = state.currentSource
    val forumNameById = buildForumNameMap(state.forumGroups)
    val timeSettings = LocalTimeSettings.current
    val timeFormatOptions = remember(timeSettings) { timeSettings.toNmbTimeFormatOptions() }

    Scaffold(
        modifier = Modifier.fillMaxSize()
            .clip(RoundedCornerShape(28.dp)),
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
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            state.siteNotice?.let { siteNotice ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                            ) {
                                Text(
                                    text = "公告",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            siteNotice.publishedAt?.let { publishedAt ->
                                formatNmbPostedAt(
                                    epochMillis = publishedAt,
                                    options = timeFormatOptions
                                )?.dateText?.let { publishedAtText ->
                                    Text(
                                        text = publishedAtText,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            Text(
                                text = siteNotice.contentText,
                                style = MaterialTheme.typography.bodyMedium
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
                items = state.threads,
                key = { thread -> thread.id },

            ) { thread ->
                ThreadCard(
                    thread = thread,
                    forumName = if (source?.type == ink.duo3.fogisland.shared.model.CatalogType.TIMELINE) {
                        resolveForumName(thread.forumId, forumNameById)
                    } else {
                        null
                    },
                    onClick = { onThreadClick(thread.id) }
                )
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
}

@Composable
private fun ThreadCard(
    thread: CatalogThread,
    forumName: String?,
    onClick: () -> Unit
) {
    val timeSettings = LocalTimeSettings.current
    val postedAtText = formatNmbPostedAtText(
        epochMillis = thread.postedAtEpochMillis,
        options = remember(timeSettings) { timeSettings.toNmbTimeFormatOptions() }
    )
    val displayTitle = resolveNmbDisplayTitle(thread.title)
    val displayHash = thread.userHash
    val forumNameAndId = resolveNmbCardNameIdString(forumName, thread.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = displayHash,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.outline,
                )
                postedAtText?.let { text ->
                    Text(
                        modifier = Modifier.weight(1f),
                        text = text,
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Column(
                modifier = Modifier.padding(top = 12.dp)
            ) {
                displayTitle?.let { title ->
                    Text(
                        modifier = Modifier.padding(bottom = 8.dp),
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Text(
                    text = thread.contentText.ifBlank { "(空内容)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = forumNameAndId,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )

                Row(
                    modifier = Modifier
                        .clickable { onClick.invoke() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier.size(14.dp),
                        imageVector = Icons.AutoMirrored.Outlined.Message,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = thread.replyCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
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
                        style = MaterialTheme.typography.titleMedium
                    )
                    source?.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onRefreshClick) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onPostClick,
                containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) {
                Icon(Icons.Default.Add, contentDescription = "发串")
            }
        }
    )
}