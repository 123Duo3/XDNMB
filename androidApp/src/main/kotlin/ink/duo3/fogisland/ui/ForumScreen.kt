package ink.duo3.fogisland.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.LocalTimeSettings
import ink.duo3.fogisland.shared.model.buildForumNameMap
import ink.duo3.fogisland.shared.model.resolveForumName
import ink.duo3.fogisland.shared.model.toNmbTimeFormatOptions
import ink.duo3.fogisland.shared.storage.db.entity.ThreadEntity
import ink.duo3.fogisland.shared.util.formatNmbPostedAtText
import ink.duo3.fogisland.shared.util.resolveNmbDisplayTitle
import ink.duo3.fogisland.ui.forum.ForumBrowseUiState

@Composable
fun ForumScreen(
    state: ForumBrowseUiState,
    onMenuClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onLoadMore: () -> Unit,
    onThreadClick: (Long) -> Unit
) {
    val source = state.currentSource
    val forumNameById = buildForumNameMap(state.forumGroups)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                actions = {
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onMenuClick)
                            .padding(start = 4.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                        Column(
                            modifier = Modifier.padding(top = 12.dp)
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
                    }
                    IconButton(onClick = onRefreshClick) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                when {
                    state.isLoadingIndex && source == null -> {
                        Text(
                            text = "正在加载板块和时间线…",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    source == null -> {
                        Text(
                            text = "从左侧菜单选择一个板块或时间线。",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "公告",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = siteNotice.contentText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            state.errorMessage?.let { errorMessage ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            items(
                items = state.threads,
                key = { thread -> thread.id }
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
    thread: ThreadEntity,
    forumName: String?,
    onClick: () -> Unit
) {
    val timeSettings = LocalTimeSettings.current
    val postedAtText = formatNmbPostedAtText(
        epochMillis = thread.postedAtEpochMillis,
        options = remember(timeSettings) { timeSettings.toNmbTimeFormatOptions() }
    )
    val displayTitle = resolveNmbDisplayTitle(thread.title)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            displayTitle?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = thread.contentText.ifBlank { "(空内容)" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                forumName?.let { name ->
                    AssistChip(
                        onClick = onClick,
                        label = { Text(name) }
                    )
                }
                AssistChip(
                    onClick = onClick,
                    label = { Text("No.${thread.id}") }
                )
                AssistChip(
                    onClick = onClick,
                    label = { Text("回复 ${thread.replyCount}") }
                )
                postedAtText?.let { text ->
                    AssistChip(
                        onClick = onClick,
                        label = { Text(text) }
                    )
                }
            }
        }
    }
}
