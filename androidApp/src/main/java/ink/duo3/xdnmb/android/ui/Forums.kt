package ink.duo3.xdnmb.android.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ink.duo3.xdnmb.android.ui.component.HtmlText
import ink.duo3.xdnmb.shared.XdSDK
import ink.duo3.xdnmb.shared.data.entity.Forum
import ink.duo3.xdnmb.shared.data.entity.ForumGroup
import ink.duo3.xdnmb.shared.data.entity.Thread
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumsDisplay(forumList: List<ForumGroup>?, sdk: XdSDK, threadList: List<Thread>?) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val selectedItem = remember { mutableStateOf("") }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Crossfade(targetState = forumList?.isNotEmpty()) { screen ->
                    when (screen) {
                        true -> Column(Modifier.verticalScroll(rememberScrollState())) {
                            forumList?.forEach { forumGroup ->
                                var expanded by remember { mutableStateOf(false) }
                                NavigationItemGroup(
                                    label = { HtmlText(html = forumGroup.name) },
                                    selected = remember(
                                        forumGroup,
                                        selectedItem.value
                                    ) { forumGroup.forums.any { it.id == selectedItem.value } },
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
                                            selected = forum.id == selectedItem.value,
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                selectedItem.value = forum.id
                                            },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                    }
                                }
                            }
                        }
                        else -> Text(text = "Null")
                    }
                }
            }
        },
        content = {
            TimeLine ({ scope.launch { drawerState.open() } }, sdk, threadList)
        }
    )
}