package ink.duo3.fogisland.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import ink.duo3.fogisland.shared.model.CatalogSource
import ink.duo3.fogisland.shared.model.CatalogType
import ink.duo3.fogisland.shared.model.ThreadDetail
import ink.duo3.fogisland.shared.model.buildForumDisplayNameMap
import ink.duo3.fogisland.shared.model.resolveForumDisplayName
import ink.duo3.fogisland.shared.model.toCatalogSource
import ink.duo3.fogisland.ui.components.NavigationItemGroup
import ink.duo3.fogisland.ui.forum.ForumBrowseViewModel
import kotlinx.coroutines.launch

@Composable
fun FogIslandApp() {
    val viewModel = viewModel<ForumBrowseViewModel>(factory = ForumBrowseViewModel.factory())
    val state by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack = remember { mutableStateListOf<AppRoute>(AppRoute.Catalog) }
    val expandedGroups = remember { mutableStateMapOf<Long, Boolean>() }
    val currentRoute = backStack.lastOrNull() ?: AppRoute.Catalog
    val isCatalogRoute = currentRoute == AppRoute.Catalog
    val forumDisplayNameById = remember(state.forumGroups) { buildForumDisplayNameMap(state.forumGroups) }

    fun showCatalog() {
        backStack.clear()
        backStack.add(AppRoute.Catalog)
    }

    ModalNavigationDrawer(
        modifier = Modifier.fillMaxSize(),
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerState = drawerState) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp, 8.dp)
                ) {
                    Text(
                        text = "雾岛",
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                    NavigationDrawerItem(
                        label = { Text("订阅", style = MaterialTheme.typography.labelLarge) },
                        icon = { Icon(Icons.Filled.Bookmarks, null) },
                        selected = false,
                        onClick = { }
                    )
                    NavigationDrawerItem(
                        label = { Text("历史", style = MaterialTheme.typography.labelLarge) },
                        icon = { Icon(Icons.Default.History, null) },
                        selected = false,
                        onClick = { }
                    )
                    NavigationDrawerItem(
                        label = { Text("发言", style = MaterialTheme.typography.labelLarge) },
                        icon = { Icon(Icons.AutoMirrored.Filled.Comment, null) },
                        selected = false,
                        onClick = { }
                    )
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
                        if (state.timelines.isNotEmpty()) {
                            val timelineExpanded = expandedGroups[-1L] ?: false
                            NavigationItemGroup(
                                label = { Text("时间线", style = MaterialTheme.typography.labelLarge) },
                                selected = isCatalogRoute && state.currentSource?.type == CatalogType.TIMELINE,
                                expanded = timelineExpanded,
                                modifier = Modifier,
                                onExpandStateChange = { expandedGroups[-1L] = it }
                            ) {
                                state.timelines.forEach { timeline ->
                                    val source = timeline.toCatalogSource()
                                    NavigationDrawerItem(
                                        label = {
                                            Text(
                                                timeline.displayName,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        },
                                        selected = isCatalogRoute && state.currentSource == source,
                                        onClick = {
                                            viewModel.openSource(source)
                                            showCatalog()
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )
                                }
                            }
                        }
                        state.forumGroups.forEach { group ->
                            val expanded = expandedGroups[group.id] ?: false
                            val groupSelected = isCatalogRoute &&
                                state.currentSource?.type == CatalogType.FORUM &&
                                group.forums.any { it.id == state.currentSource?.id }
                            NavigationItemGroup(
                                label = { Text(group.name, style = MaterialTheme.typography.labelLarge) },
                                selected = groupSelected,
                                expanded = expanded,
                                modifier = Modifier,
                                onExpandStateChange = { expandedGroups[group.id] = it }
                            ) {
                                group.forums.forEach { forum ->
                                    val source = forum.toCatalogSource(group)
                                    NavigationDrawerItem(
                                        label = {
                                            Text(
                                                forum.displayName,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        },
                                        selected = isCatalogRoute && state.currentSource == source,
                                        onClick = {
                                            viewModel.openSource(source)
                                            showCatalog()
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    NavigationDrawerItem(
                        label = { Text("设置", style = MaterialTheme.typography.labelLarge) },
                        icon = { Icon(Icons.Filled.Settings, null) },
                        selected = currentRoute == AppRoute.Settings,
                        onClick = {
                            if (currentRoute != AppRoute.Settings) {
                                backStack.clear()
                                backStack.add(AppRoute.Catalog)
                                backStack.add(AppRoute.Settings)
                            }
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize(),
        ) { key: AppRoute ->
            when (key) {
                AppRoute.Catalog -> NavEntry(key) {
                    ForumScreen(
                        state = state,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onRefreshClick = {
                            state.currentSource?.let { source ->
                                viewModel.openSource(source, forceRefresh = true)
                            }
                        },
                        onLoadMore = { viewModel.loadMoreCatalog() },
                        onThreadClick = { threadId ->
                            viewModel.openThread(threadId)
                            backStack.add(AppRoute.Thread(threadId))
                        }
                    )
                }

                is AppRoute.Thread -> NavEntry(key) {
                    LaunchedEffect(key.threadId) {
                        if (state.activeThreadId != key.threadId) {
                            viewModel.openThread(key.threadId)
                        }
                    }
                    val isRouteThreadActive = state.activeThreadId == key.threadId
                    val routeDetail = if (state.currentThread?.id == key.threadId) {
                        state.threadDetail
                    } else {
                        ThreadDetail(null, emptyList(), null)
                    }
                    ThreadDetailScreen(
                        detail = routeDetail,
                        forumName = resolveForumDisplayName(routeDetail.thread?.forumId, forumDisplayNameById),
                        loadedPage = if (isRouteThreadActive) state.loadedThreadPage else 0,
                        isLoading = isRouteThreadActive && state.isLoadingThread,
                        canLoadMore = isRouteThreadActive && state.canLoadMoreReplies,
                        errorMessage = if (isRouteThreadActive) state.errorMessage else null,
                        onBack = {
                            if (backStack.isNotEmpty()) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onRefresh = {
                            viewModel.openThread(key.threadId, forceRefresh = true)
                        },
                        onLoadMore = { viewModel.loadMoreReplies() },
                        onProgressChanged = { index, offset ->
                            viewModel.saveThreadProgress(key.threadId, index, offset)
                        }
                    )
                }

                AppRoute.Settings -> NavEntry(key) {
                    SettingsScreen(
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }
            }
        }
    }
}

private sealed interface AppRoute {
    data object Catalog : AppRoute
    data class Thread(val threadId: Long) : AppRoute
    data object Settings : AppRoute
}
