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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.LocalTimeSettings
import ink.duo3.fogisland.data.ensureSubscriptionUuid
import ink.duo3.fogisland.data.subscriptionUuidFlow
import ink.duo3.fogisland.data.updateSubscriptionUuid
import ink.duo3.fogisland.shared.model.ErrorPresentation
import ink.duo3.fogisland.shared.model.ForumGroup
import ink.duo3.fogisland.shared.model.buildForumNameMap
import ink.duo3.fogisland.shared.model.resolveForumName
import ink.duo3.fogisland.shared.model.toNmbTimeFormatOptions
import ink.duo3.fogisland.shared.storage.db.entity.SubscriptionThreadEntity
import ink.duo3.fogisland.shared.storage.preferences.isSubscriptionUuidFormatValid
import ink.duo3.fogisland.shared.storage.preferences.normalizeSubscriptionUuidInput
import ink.duo3.fogisland.shared.util.formatNmbPostedAtText
import ink.duo3.fogisland.shared.util.resolveNmbDisplayTitle
import ink.duo3.fogisland.ui.components.ErrorMessageCard
import ink.duo3.fogisland.ui.components.SubscriptionUuidEditorDialog
import ink.duo3.fogisland.ui.components.normalizeSubscriptionUuidFieldValue
import ink.duo3.fogisland.ui.components.subscriptionUuidTextFieldValue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    forumGroups: List<ForumGroup>,
    threads: List<SubscriptionThreadEntity>,
    loadedPage: Int,
    isLoading: Boolean,
    error: ErrorPresentation?,
    onMenuClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onLoadMore: () -> Unit,
    onThreadClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val forumNameById = remember(forumGroups) { buildForumNameMap(forumGroups) }
    val subscriptionUuid by context.subscriptionUuidFlow.collectAsState(initial = null)
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var isSubscriptionUuidDialogVisible by rememberSaveable { mutableStateOf(false) }
    var subscriptionUuidDraft by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(subscriptionUuidTextFieldValue(""))
    }
    var subscriptionUuidError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        context.ensureSubscriptionUuid()
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
                    IconButton(
                        onClick = {
                            subscriptionUuidDraft = subscriptionUuidTextFieldValue(
                                subscriptionUuid.orEmpty()
                            )
                            subscriptionUuidError = null
                            isSubscriptionUuidDialogVisible = true
                        }
                    ) {
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
                key = { thread -> thread.threadId }
            ) { thread ->
                SubscriptionThreadCard(
                    thread = thread,
                    forumName = resolveForumName(thread.forumId, forumNameById),
                    onClick = { onThreadClick(thread.threadId) },
                    onDeleteClick = { onDeleteClick(thread.threadId) }
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
            draft = subscriptionUuidDraft,
            errorMessage = subscriptionUuidError,
            onDraftChange = {
                val normalizedValue = normalizeSubscriptionUuidFieldValue(it)
                subscriptionUuidDraft = normalizedValue
                subscriptionUuidError = when {
                    normalizedValue.text.isBlank() -> null
                    isSubscriptionUuidFormatValid(normalizedValue.text) -> null
                    else -> "订阅 ID 格式无效"
                }
            },
            onDismissRequest = {
                isSubscriptionUuidDialogVisible = false
                subscriptionUuidError = null
            },
            onConfirm = {
                val normalizedValue = normalizeSubscriptionUuidInput(subscriptionUuidDraft.text)
                if (!isSubscriptionUuidFormatValid(normalizedValue)) {
                    subscriptionUuidError = "订阅 ID 格式无效"
                    return@SubscriptionUuidEditorDialog
                }
                scope.launch {
                    runCatching {
                        context.updateSubscriptionUuid(normalizedValue)
                    }.onSuccess {
                        isSubscriptionUuidDialogVisible = false
                        subscriptionUuidError = null
                        onRefreshClick()
                    }.onFailure { throwable ->
                        subscriptionUuidError = throwable.message
                            ?.ifBlank { "保存订阅 ID 失败" }
                            ?: "保存订阅 ID 失败"
                    }
                }
            }
        )
    }
}

@Composable
private fun SubscriptionThreadCard(
    thread: SubscriptionThreadEntity,
    forumName: String?,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
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
                postedAtText?.let { time ->
                    AssistChip(
                        onClick = onClick,
                        label = { Text(time) }
                    )
                }
                AssistChip(
                    onClick = onClick,
                    label = { Text("No.${thread.threadId}") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onDeleteClick) {
                    Text("取消订阅")
                }
            }
        }
    }
}
