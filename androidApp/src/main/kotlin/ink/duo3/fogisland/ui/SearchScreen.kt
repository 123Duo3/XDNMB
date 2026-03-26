package ink.duo3.fogisland.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.LocalTimeSettings
import ink.duo3.fogisland.shared.model.DirectThreadShortcut
import ink.duo3.fogisland.shared.model.ErrorPresentation
import ink.duo3.fogisland.shared.model.ForumGroup
import ink.duo3.fogisland.shared.model.SearchHit
import ink.duo3.fogisland.shared.model.SearchHitType
import ink.duo3.fogisland.shared.model.buildForumNameMap
import ink.duo3.fogisland.shared.model.resolveForumName
import ink.duo3.fogisland.shared.model.toNmbTimeFormatOptions
import ink.duo3.fogisland.shared.util.formatNmbPostedAtText
import ink.duo3.fogisland.shared.util.resolveNmbDisplayAuthor
import ink.duo3.fogisland.shared.util.resolveNmbDisplayTitle
import ink.duo3.fogisland.ui.components.ErrorMessageCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    forumGroups: List<ForumGroup>,
    query: String,
    results: List<SearchHit>,
    directThreadShortcut: DirectThreadShortcut?,
    recentSearches: List<String>,
    isSearching: Boolean,
    error: ErrorPresentation?,
    onMenuClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onQuerySubmit: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onDirectThreadClick: (Long) -> Unit,
    onResultClick: (SearchHit) -> Unit
) {
    val forumNameById = remember(forumGroups) { buildForumNameMap(forumGroups) }
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun submitSearch() {
        onQuerySubmit(query)
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    BackHandler(enabled = query.isNotBlank()) {
        onQueryChange("")
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
            .clip(RoundedCornerShape(28.dp)),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("搜索")
                        Text(
                            text = when {
                                query.isBlank() -> "搜索已缓存的串和回复"
                                isSearching -> "正在搜索…"
                                else -> "共 ${results.size} 条结果"
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
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    label = { Text("搜索缓存") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    placeholder = { Text("标题、正文、回复") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = { submitSearch() }
                    ),
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            Row {
                                IconButton(onClick = ::submitSearch) {
                                    Icon(Icons.Default.Search, contentDescription = "加入最近搜索")
                                }
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "清空搜索词")
                                }
                            }
                        }
                    }
                )
            }

            if (query.isBlank() && recentSearches.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "最近搜索",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = onClearRecentSearches) {
                            Text("清空")
                        }
                    }
                }
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentSearches, key = { it }) { recentQuery ->
                            AssistChip(
                                onClick = {
                                    onQueryChange(recentQuery)
                                    onQuerySubmit(recentQuery)
                                },
                                label = { Text(recentQuery) }
                            )
                        }
                    }
                }
            }

            error?.let { errorState ->
                item {
                    ErrorMessageCard(
                        error = errorState,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            if (query.isBlank()) {
                item {
                    Text(
                        text = "只搜索已经缓存到本地的内容。先浏览板块和串，搜索结果才会逐渐变多。",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else if (isSearching) {
                item {
                    Text(
                        text = "正在搜索本地缓存…",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else if (results.isEmpty()) {
                item {
                    Text(
                        text = "没有找到匹配内容。",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            directThreadShortcut?.let { shortcut ->
                item(key = "direct-thread-${shortcut.threadId}") {
                    DirectThreadShortcutCard(
                        shortcut = shortcut,
                        forumName = resolveForumName(shortcut.forumId, forumNameById),
                        onClick = { onDirectThreadClick(shortcut.threadId) }
                    )
                }
            }

            items(
                items = results,
                key = { hit -> "${hit.type}:${hit.threadId}:${hit.postId}" }
            ) { hit ->
                SearchResultCard(
                    hit = hit,
                    query = query,
                    forumName = resolveForumName(hit.forumId, forumNameById),
                    onClick = { onResultClick(hit) }
                )
            }
        }
    }
}

@Composable
private fun DirectThreadShortcutCard(
    shortcut: DirectThreadShortcut,
    forumName: String?,
    onClick: () -> Unit
) {
    val timeSettings = LocalTimeSettings.current
    val timeFormatOptions = remember(timeSettings) { timeSettings.toNmbTimeFormatOptions() }
    val displayTitle = resolveNmbDisplayTitle(shortcut.title) ?: "串 No.${shortcut.threadId}"
    val displayAuthor = resolveNmbDisplayAuthor(shortcut.userHash, shortcut.name)
    val postedAtText = formatNmbPostedAtText(
        epochMillis = shortcut.postedAtEpochMillis,
        options = timeFormatOptions
    )
    val previewText = when {
        shortcut.preview.isNotBlank() -> shortcut.preview
        shortcut.isCached -> "(空内容)"
        else -> "本地暂无缓存，打开后会直接尝试加载这个串。"
    }

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
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

            listOfNotNull(displayAuthor, postedAtText)
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" · ")
                ?.let { metaText ->
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            Text(
                text = previewText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (shortcut.isCached) 6 else 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onClick,
                    label = { Text("前往串") }
                )
                forumName?.let { name ->
                    AssistChip(
                        onClick = onClick,
                        label = { Text(name) }
                    )
                }
                AssistChip(
                    onClick = onClick,
                    label = { Text("No.${shortcut.threadId}") }
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    hit: SearchHit,
    query: String,
    forumName: String?,
    onClick: () -> Unit
) {
    val timeSettings = LocalTimeSettings.current
    val timeFormatOptions = remember(timeSettings) { timeSettings.toNmbTimeFormatOptions() }
    val displayTitle = resolveNmbDisplayTitle(hit.title)
        ?: when (hit.type) {
            SearchHitType.THREAD -> "串 No.${hit.threadId}"
            SearchHitType.POST -> "回复 No.${hit.postId}"
        }
    val displayAuthor = resolveNmbDisplayAuthor(hit.userHash, hit.name)
    val postedAtText = formatNmbPostedAtText(
        epochMillis = hit.postedAtEpochMillis,
        options = timeFormatOptions
    )

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = highlightQuery(
                    text = displayTitle,
                    query = query,
                    highlightColor = MaterialTheme.colorScheme.primary
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            listOfNotNull(displayAuthor, postedAtText)
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" · ")
                ?.let { metaText ->
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            Text(
                text = highlightQuery(
                    text = hit.preview.ifBlank { "(空内容)" },
                    query = query,
                    highlightColor = MaterialTheme.colorScheme.primary
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(if (hit.type == SearchHitType.THREAD) "主串" else "回复")
                    }
                )
                forumName?.let { name ->
                    AssistChip(
                        onClick = onClick,
                        label = { Text(name) }
                    )
                }
                hit.page?.takeIf { hit.type == SearchHitType.POST }?.let { page ->
                    AssistChip(
                        onClick = onClick,
                        label = { Text("第${page}页") }
                    )
                }
                AssistChip(
                    onClick = onClick,
                    label = { Text("No.${hit.threadId}") }
                )
            }
        }
    }
}

private fun highlightQuery(
    text: String,
    query: String,
    highlightColor: androidx.compose.ui.graphics.Color
): AnnotatedString {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) {
        return AnnotatedString(text)
    }

    val ranges = buildList {
        var searchStart = 0
        while (searchStart < text.length) {
            val matchIndex = text.indexOf(normalizedQuery, startIndex = searchStart, ignoreCase = true)
            if (matchIndex < 0) {
                break
            }
            add(matchIndex until (matchIndex + normalizedQuery.length))
            searchStart = matchIndex + normalizedQuery.length
        }
    }
    if (ranges.isEmpty()) {
        return AnnotatedString(text)
    }

    return buildAnnotatedString {
        append(text)
        ranges.forEach { range ->
            addStyle(
                style = SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.SemiBold
                ),
                start = range.first,
                end = range.last + 1
            )
        }
    }
}
