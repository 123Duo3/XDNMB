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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.LocalTimeSettings
import ink.duo3.fogisland.shared.model.ErrorPresentation
import ink.duo3.fogisland.shared.model.ForumGroup
import ink.duo3.fogisland.shared.model.PostingDraftEntry
import ink.duo3.fogisland.shared.model.PostingDraftType
import ink.duo3.fogisland.shared.model.PostingHistoryEntry
import ink.duo3.fogisland.shared.model.PostingHistoryType
import ink.duo3.fogisland.shared.model.buildForumNameMap
import ink.duo3.fogisland.shared.model.resolveForumName
import ink.duo3.fogisland.shared.model.toNmbTimeFormatOptions
import ink.duo3.fogisland.shared.util.formatNmbPostedAtText
import ink.duo3.fogisland.ui.components.ErrorMessageCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostingHistoryScreen(
    forumGroups: List<ForumGroup>,
    history: List<PostingHistoryEntry>,
    drafts: List<PostingDraftEntry>,
    error: ErrorPresentation?,
    onMenuClick: () -> Unit,
    onEntryClick: (PostingHistoryEntry) -> Unit,
    onDraftClick: (PostingDraftEntry) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onDeleteDraftClick: (Long) -> Unit,
    onClearAllClick: (PostingHistoryType) -> Unit,
    onClearAllDraftsClick: () -> Unit
) {
    val forumNameById = remember(forumGroups) { buildForumNameMap(forumGroups) }
    val threadHistory = remember(history) { history.filter { it.type == PostingHistoryType.THREAD } }
    val replyHistory = remember(history) { history.filter { it.type == PostingHistoryType.REPLY } }
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var deletingEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingDraftId by rememberSaveable { mutableStateOf<Long?>(null) }
    var isClearAllDialogVisible by rememberSaveable { mutableStateOf(false) }
    val visibleHistory = when (selectedTabIndex) {
        0 -> threadHistory
        1 -> replyHistory
        else -> emptyList()
    }
    val hasVisibleContent = when (selectedTabIndex) {
        0 -> threadHistory.isNotEmpty()
        1 -> replyHistory.isNotEmpty()
        else -> drafts.isNotEmpty()
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
                        Text("发言")
                        Text(
                            text = if (history.isEmpty() && drafts.isEmpty()) {
                                "还没有发言或草稿"
                            } else {
                                "${threadHistory.size} 条发串 · ${replyHistory.size} 条回帖 · ${drafts.size} 条草稿"
                            },
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
                        enabled = hasVisibleContent
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = if (selectedTabIndex == 2) "清空草稿箱" else "清空发言历史"
                        )
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
            modifier = Modifier.fillMaxSize(),
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

            item("posting_history_tabs") {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("发串 ${threadHistory.size}") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("回帖 ${replyHistory.size}") }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("草稿 ${drafts.size}") }
                    )
                }
            }

            if (history.isEmpty() && drafts.isEmpty()) {
                item("posting_history_empty_hint") {
                    Text(
                        text = "发串、回帖或编辑草稿后，这里会记录本地历史和草稿箱。",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else if (selectedTabIndex == 2 && drafts.isEmpty()) {
                item {
                    Text(
                        text = "还没有草稿。",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (selectedTabIndex != 2 && visibleHistory.isEmpty()) {
                item {
                    Text(
                        text = if (selectedTabIndex == 0) "还没有发串记录。" else "还没有回帖记录。",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(
                    items = visibleHistory,
                    key = { entry -> entry.id }
                ) { entry ->
                    PostingHistoryCard(
                        entry = entry,
                        forumName = resolveForumName(entry.forumId, forumNameById),
                        onClick = if (entry.threadId != null) {
                            { onEntryClick(entry) }
                        } else {
                            null
                        },
                        onDeleteClick = { deletingEntryId = entry.id }
                    )
                }

                if (selectedTabIndex == 2) {
                    items(
                        items = drafts,
                        key = { entry -> entry.id }
                    ) { entry ->
                        PostingDraftCard(
                            entry = entry,
                            forumName = resolveForumName(entry.forumId, forumNameById),
                            onClick = { onDraftClick(entry) },
                            onDeleteClick = { deletingDraftId = entry.id }
                        )
                    }
                }
            }
        }
    }

    deletingEntryId?.let { entryId ->
        AlertDialog(
            onDismissRequest = { deletingEntryId = null },
            title = { Text("删除发言记录") },
            text = { Text("确定删除这条发言记录吗？这不会删除串内容，也不会撤回已发送的帖子。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingEntryId = null
                        onDeleteClick(entryId)
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEntryId = null }) {
                    Text("取消")
                }
            }
        )
    }

    deletingDraftId?.let { draftId ->
        AlertDialog(
            onDismissRequest = { deletingDraftId = null },
            title = { Text("删除草稿") },
            text = { Text("确定删除这条草稿吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingDraftId = null
                        onDeleteDraftClick(draftId)
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingDraftId = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (isClearAllDialogVisible) {
        AlertDialog(
            onDismissRequest = { isClearAllDialogVisible = false },
            title = { Text(if (selectedTabIndex == 2) "清空草稿箱" else "清空发言历史") },
            text = {
                Text(
                    if (selectedTabIndex == 2) {
                        "确定清空全部草稿吗？删除后无法恢复。"
                    } else {
                        "确定清空当前标签下的全部发言记录吗？这不会删除串内容，也不会撤回已发送的帖子。"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isClearAllDialogVisible = false
                        if (selectedTabIndex == 2) {
                            onClearAllDraftsClick()
                        } else {
                            onClearAllClick(
                                if (selectedTabIndex == 0) {
                                    PostingHistoryType.THREAD
                                } else {
                                    PostingHistoryType.REPLY
                                }
                            )
                        }
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

@Composable
private fun PostingDraftCard(
    entry: PostingDraftEntry,
    forumName: String?,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val timeSettings = LocalTimeSettings.current
    val timeFormatOptions = remember(timeSettings) { timeSettings.toNmbTimeFormatOptions() }
    val displayTitle = entry.title.takeIf { it.isNotBlank() }
        ?: entry.threadTitle.takeIf { it.isNotBlank() }
        ?: when (entry.type) {
            PostingDraftType.THREAD -> "发串草稿"
            PostingDraftType.REPLY -> entry.threadId?.let { "回复至串 No.$it" } ?: "回帖草稿"
        }
    val displayAuthor = entry.name.takeIf { it.isNotBlank() }
    val updatedAtText = formatNmbPostedAtText(
        epochMillis = entry.updatedAt,
        options = timeFormatOptions
    )
    val metadataText = buildList {
        forumName?.takeIf { it.isNotBlank() }?.let(::add)
        if (entry.hasImage) {
            add("含图片")
        }
        if (entry.type == PostingDraftType.REPLY) {
            entry.threadId?.let { add("串 No.$it") }
        }
    }.joinToString(" · ")
    val previewText = entry.contentText.ifBlank {
        if (entry.hasImage) "仅图片" else "空草稿"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    displayAuthor?.let { author ->
                        Text(
                            text = author,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(onClick = onDeleteClick) {
                    Text("删除")
                }
            }

            if (metadataText.isNotBlank()) {
                Text(
                    text = metadataText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = previewText,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = updatedAtText.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PostingHistoryCard(
    entry: PostingHistoryEntry,
    forumName: String?,
    onClick: (() -> Unit)?,
    onDeleteClick: () -> Unit
) {
    val timeSettings = LocalTimeSettings.current
    val timeFormatOptions = remember(timeSettings) { timeSettings.toNmbTimeFormatOptions() }
    val displayTitle = entry.title
        ?: entry.threadTitle
        ?: when (entry.type) {
            PostingHistoryType.THREAD -> entry.threadId?.let { "串 No.$it" } ?: "发串记录"
            PostingHistoryType.REPLY -> entry.threadId?.let { "回复至串 No.$it" } ?: "回帖记录"
        }
    val displayAuthor = entry.name
    val createdAtText = formatNmbPostedAtText(
        epochMillis = entry.createdAt,
        options = timeFormatOptions
    )
    val metadataText = buildList {
        forumName?.takeIf { it.isNotBlank() }?.let(::add)
        if (entry.hasImage) {
            add("含图片")
        }
        entry.threadId?.let { add("串 No.$it") }
        if (entry.type == PostingHistoryType.REPLY) {
            entry.postId?.let { add("回复 No.$it") }
        }
    }.joinToString(" · ")

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            displayAuthor?.let { author ->
                Text(
                    text = author,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            entry.contentText.takeIf { it.isNotBlank() }?.let { content ->
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (metadataText.isNotEmpty()) {
                Text(
                    text = metadataText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            createdAtText?.let { time ->
                Text(
                    text = "发送于 $time",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onDeleteClick) {
                    Text("删除记录")
                }
            }
        }
    }
}
