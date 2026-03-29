package ink.duo3.fogisland.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.LocalTimeSettings
import ink.duo3.fogisland.shared.model.DirectThreadShortcut
import ink.duo3.fogisland.shared.model.ErrorPresentation
import ink.duo3.fogisland.shared.model.ForumGroup
import ink.duo3.fogisland.shared.model.SearchHit
import ink.duo3.fogisland.shared.model.buildForumNameMap
import ink.duo3.fogisland.shared.model.resolveForumName
import ink.duo3.fogisland.ui.components.ErrorMessageCard
import ink.duo3.fogisland.ui.components.FogIslandPreviewColumn
import ink.duo3.fogisland.ui.components.NmbPostCard
import ink.duo3.fogisland.ui.components.NmbPreviewSamples

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
    val surfacedResultCount = results.size + if (directThreadShortcut != null) 1 else 0

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
            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
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
                                else -> "共 $surfacedResultCount 条结果"
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
            contentPadding = innerPadding
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
                            TextButton(
                                onClick = {
                                    onQueryChange(recentQuery)
                                    onQuerySubmit(recentQuery)
                                }
                            ) {
                                Text(recentQuery)
                            }
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
            } else if (results.isEmpty() && directThreadShortcut == null) {
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No.${shortcut.threadId}",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = if (shortcut.isCached) "本地已缓存" else "直接打开",
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            shortcut.title?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            shortcut.name?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (shortcut.preview.isNotBlank()) {
                Text(
                    text = shortcut.preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (shortcut.isCached) 6 else 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            forumName?.takeIf { it.isNotBlank() }?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
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
    NmbPostCard(
        hit = hit,
        query = query,
        forumName = forumName,
        onClick = onClick,
        modifier = Modifier,
        bodyMaxLines = 6,
        bodyOverflow = TextOverflow.Ellipsis
    )
}

@Preview(name = "Search Cards", widthDp = 412, heightDp = 980)
@Composable
private fun SearchCardsPreview() {
    FogIslandPreviewColumn {
        DirectThreadShortcutCard(
            shortcut = NmbPreviewSamples.directThreadShortcutCached,
            forumName = "综一",
            onClick = {}
        )
        DirectThreadShortcutCard(
            shortcut = NmbPreviewSamples.directThreadShortcutUncached,
            forumName = "综一",
            onClick = {}
        )
        SearchResultCard(
            hit = NmbPreviewSamples.searchThreadHit,
            query = "岛风",
            forumName = "手游综合",
            onClick = {}
        )
        SearchResultCard(
            hit = NmbPreviewSamples.searchReplyHit,
            query = "岛风",
            forumName = "手游综合",
            onClick = {}
        )
    }
}

@Preview(name = "Search Screen", widthDp = 412, heightDp = 960)
@Composable
private fun SearchScreenPreview() {
    FogIslandPreviewColumn {
        SearchScreen(
            forumGroups = NmbPreviewSamples.forumGroups,
            query = "岛风",
            results = listOf(
                NmbPreviewSamples.searchThreadHit,
                NmbPreviewSamples.searchReplyHit
            ),
            directThreadShortcut = NmbPreviewSamples.directThreadShortcutCached,
            recentSearches = listOf("岛风", "测试", "No.123456"),
            isSearching = false,
            error = null,
            onMenuClick = {},
            onQueryChange = {},
            onQuerySubmit = {},
            onClearRecentSearches = {},
            onDirectThreadClick = {},
            onResultClick = {}
        )
    }
}
