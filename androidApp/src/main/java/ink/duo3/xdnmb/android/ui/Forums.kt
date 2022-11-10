package ink.duo3.xdnmb.android.ui

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ink.duo3.xdnmb.android.R
import ink.duo3.xdnmb.android.ui.component.HtmlText
import ink.duo3.xdnmb.android.ui.component.NavigationItemGroup
import ink.duo3.xdnmb.android.viewmodel.AppViewModel
import ink.duo3.xdnmb.shared.data.entity.Forum
import kotlinx.coroutines.launch

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumsDisplay(
    forumList: AppViewModel.ForumListState,
    threadList: AppViewModel.ThreadListState,
    onForumSelect: (id: String) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf("-1") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.statusBarsPadding()) {
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .weight(1f)
                    ) {
                        Text(
                            text = "雾岛",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(28.dp, 16.dp)
                        )
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.bookmarks_black_24dp),
                                    contentDescription = ""
                                )
                            },
                            label = {
                                Text(text = "订阅")
                            },
                            selected = "subscribe" == selectedItem,
                            onClick = {
                                scope.launch { drawerState.close() }
                                //selectedItem.value = "subscribe"
                            },
                            modifier = Modifier
                                .padding(12.dp, 0.dp)
                        )
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.history_black_24dp),
                                    contentDescription = ""
                                )
                            },
                            label = {
                                Text(text = "历史")
                            },
                            selected = "history" == selectedItem,
                            onClick = {
                                scope.launch { drawerState.close() }
                                //selectedItem.value = "history"
                            },
                            modifier = Modifier
                                .padding(12.dp, 0.dp)
                        )
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.comment_black_24dp),
                                    contentDescription = ""
                                )
                            },
                            label = {
                                Text(text = "发言")
                            },
                            selected = "speech" == selectedItem,
                            onClick = {
                                scope.launch { drawerState.close() }
                                //selectedItem.value = "speech"
                            },
                            modifier = Modifier
                                .padding(12.dp, 0.dp)
                        )
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = ""
                                )
                            },
                            label = {
                                Text(text = "搜索")
                            },
                            selected = "search" == selectedItem,
                            onClick = {
                                scope.launch { drawerState.close() }
                                //selectedItem.value = "search"
                            },
                            modifier = Modifier
                                .padding(12.dp, 0.dp)
                        )

                        Divider(Modifier.padding(28.dp, 16.dp))

                        Forums(
                            forumList = forumList,
                            selectedForum = selectedItem,
                            onForumSelect = {
                                onForumSelect(it)
                                selectedItem = it
                                scope.launch {
                                    drawerState.close()
                                }
                            }
                        )
                    }
                    Divider(Modifier.padding(bottom = 16.dp, start = 28.dp, end = 28.dp))
                    NavigationDrawerItem(
                        icon = {
                            Icon(imageVector = Icons.Outlined.Settings, contentDescription = "")
                        },
                        label = {
                            Text(text = "设置")
                        },
                        selected = "settings" == selectedItem,
                        onClick = {
                            scope.launch { drawerState.close() }
                            //selectedItem.value = "settings"
                        },
                        modifier = Modifier
                            .padding(12.dp, 0.dp)
                    )
                    if (WindowInsets.systemBars.asPaddingValues()
                            .calculateBottomPadding() == 0.dp
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        },
        content = {
            ForumThread(
                onClickMenu = { scope.launch { drawerState.open() } },
                threadList = threadList,
                forumId = selectedItem.toInt(),
                forumName = "不知道该怎么拿"
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Forums(
    forumList: AppViewModel.ForumListState,
    selectedForum: String,
    onForumSelect: (id: String) -> Unit
) {
    Crossfade(targetState = forumList) { list ->
        when (list) {
            AppViewModel.ForumListState.Loading -> Text("Loading")
            is AppViewModel.ForumListState.Error -> Text("Error: ${list.message}")
            is AppViewModel.ForumListState.OK -> Column {
                list.forums.forEach { forumGroup ->
                    var expanded by remember { mutableStateOf(false) }
                    NavigationItemGroup(
                        label = { HtmlText(html = forumGroup.name) },
                        selected = remember(forumGroup, selectedForum) {
                            forumGroup.forums.any { it.id == selectedForum }
                        },
                        expanded = expanded,
                        modifier = Modifier.fillMaxWidth(),
                        onExpandStateChange = { expanded = it }
                    ) {
                        forumGroup.forums.forEach { forum: Forum ->
                            val name = if (forum.showName?.isEmpty() == true) {
                                forum.name
                            } else {
                                forum.showName
                            } ?: "时间线"

                            NavigationDrawerItem(
                                label = {
                                    HtmlText(html = name)
                                },
                                selected = forum.id == selectedForum,
                                onClick = {
                                    onForumSelect(forum.id)
                                },
                                modifier = Modifier
                                    .padding(16.dp, 0.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}