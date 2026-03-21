package ink.duo3.fogisland.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch

@Composable
fun FogIslandApp() {

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val backStack = remember { mutableStateListOf<String>("Forum") }
    val currentRoute = backStack.lastOrNull() ?: "Forum"

    BackHandler(enabled = drawerState.isOpen || backStack.size > 1) {
        if (drawerState.isOpen) {
            scope.launch {
                drawerState.close()
            }
        } else if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    ModalNavigationDrawer(
        modifier = Modifier.fillMaxSize(),
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    Modifier.padding(12.dp, 8.dp)
                ) {
                    Text(
                        "雾岛",
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                    NavigationDrawerItem(
                        label = { Text("订阅", style = MaterialTheme.typography.labelLarge) },
                        icon = { Icon(Icons.Filled.Bookmarks, null) },
                        selected = false,
                        onClick = { /* Handle click */ }
                    )
                    NavigationDrawerItem(
                        label = { Text("历史", style = MaterialTheme.typography.labelLarge) },
                        icon = { Icon(Icons.Default.History, null) },
                        selected = false,
                        onClick = { /* Handle click */ }
                    )
                    NavigationDrawerItem(
                        label = { Text("发言", style = MaterialTheme.typography.labelLarge) },
                        icon = { Icon(Icons.AutoMirrored.Filled.Comment, null) },
                        selected = false,
                        onClick = { /* Handle click */ }
                    )
                    HorizontalDivider(
                        Modifier.padding(16.dp, 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        //TODO: Forum List
                    }
                    HorizontalDivider(
                        Modifier.padding(16.dp, 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    NavigationDrawerItem(
                        label = { Text("设置", style = MaterialTheme.typography.labelLarge) },
                        icon = { Icon(Icons.Filled.Settings, null) },
                        selected = currentRoute == "Settings",
                        onClick = {
                            if (currentRoute != "Settings") {
                                backStack.clear()
                                backStack.add("Forum")
                                backStack.add("Settings")
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
        ) { key ->
            when (key) {
                "Forum" -> NavEntry(key) {
                    ForumScreen(
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }
                "Settings" -> NavEntry(key) {
                    SettingsScreen(
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }
                else -> NavEntry(key) { }
            }
        }
    }
}