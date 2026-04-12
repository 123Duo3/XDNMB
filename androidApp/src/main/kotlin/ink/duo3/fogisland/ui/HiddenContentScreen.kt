package ink.duo3.fogisland.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.shared.model.ErrorPresentation
import ink.duo3.fogisland.shared.model.ForumGroup
import ink.duo3.fogisland.shared.model.HiddenTimelineForumFilter
import ink.duo3.fogisland.shared.model.Timeline
import ink.duo3.fogisland.shared.model.buildForumNameMap
import ink.duo3.fogisland.shared.model.resolveForumName
import ink.duo3.fogisland.ui.components.ErrorMessageCard
import ink.duo3.fogisland.ui.components.settings.SettingItem
import ink.duo3.fogisland.ui.components.settings.SettingItemGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenContentScreen(
    forumGroups: List<ForumGroup>,
    timelines: List<Timeline>,
    hiddenThreadIds: Set<Long>,
    hiddenTimelineForumFilters: Set<HiddenTimelineForumFilter>,
    error: ErrorPresentation?,
    onBack: () -> Unit,
    onUnhideThreadClick: (Long) -> Unit,
    onUnhideTimelineForumClick: (Long, Long) -> Unit,
) {
    val forumNameById = remember(forumGroups) { buildForumNameMap(forumGroups) }
    val timelineNameById = remember(timelines) {
        timelines.associate { it.id to it.displayName }
    }
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val sortedHiddenThreadIds = remember(hiddenThreadIds) { hiddenThreadIds.sorted() }
    val sortedHiddenTimelineForumFilters = remember(hiddenTimelineForumFilters) {
        hiddenTimelineForumFilters.sortedWith(
            compareBy<HiddenTimelineForumFilter> { it.timelineId }
                .thenBy { it.forumId }
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
                        Text("屏蔽管理")
                        Text(
                            text = "共 ${sortedHiddenThreadIds.size} 个串，${sortedHiddenTimelineForumFilters.size} 个时间线板块",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
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

            if (sortedHiddenThreadIds.isEmpty() && sortedHiddenTimelineForumFilters.isEmpty()) {
                item {
                    Text(
                        text = "目前没有已屏蔽的串或时间线板块。",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            if (sortedHiddenThreadIds.isNotEmpty()) {
                item {
                    SettingItemGroup(title = "屏蔽的串") {
                        sortedHiddenThreadIds.forEach { threadId ->
                            SettingItem(
                                title = { Text("No.$threadId") },
                                description = { Text("恢复后会重新显示在板块页和时间线里。") },
                                endAction = {
                                    TextButton(onClick = { onUnhideThreadClick(threadId) }) {
                                        Text("恢复")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (sortedHiddenTimelineForumFilters.isNotEmpty()) {
                item {
                    SettingItemGroup(title = "时间线屏蔽板块") {
                        sortedHiddenTimelineForumFilters.forEach { filter ->
                            SettingItem(
                                title = {
                                    Text(
                                        resolveForumName(filter.forumId, forumNameById)
                                            ?: "板块 ${filter.forumId}"
                                    )
                                },
                                description = {
                                    Text(
                                        timelineNameById[filter.timelineId]
                                            ?.let { "时间线：$it" }
                                            ?: "时间线 ${filter.timelineId}"
                                    )
                                },
                                endAction = {
                                    TextButton(
                                        onClick = {
                                            onUnhideTimelineForumClick(
                                                filter.timelineId,
                                                filter.forumId
                                            )
                                        }
                                    ) {
                                        Text("恢复")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
