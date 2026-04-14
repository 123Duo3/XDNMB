package ink.duo3.fogisland.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.ui.theme.FogIslandTheme
import ink.duo3.fogisland.utils.ProvideContentColorTextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationItemGroupHeader(
    label: @Composable () -> Unit,
    selected: Boolean,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        modifier = modifier,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = LocalContentColor.current.copy(0.08f),
            selectedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        label = label,
        selected = selected,
        onClick = onClick,
        badge = {
            val rotate by animateFloatAsState(if (expanded) 180f else 0f, label = "")
            Icon(
                modifier = Modifier.rotate(rotate),
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "折叠" else "展开"
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationItemGroup(
    label: @Composable () -> Unit,
    selected: Boolean,
    expanded: Boolean,
    modifier: Modifier,
    onExpandStateChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier) {
        NavigationDrawerItem(
            modifier = Modifier,
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = LocalContentColor.current.copy(0.08f),
                selectedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            label = label,
            selected = selected,
            onClick = { onExpandStateChange(!expanded) },
            badge = {
                val rotate by animateFloatAsState(if (expanded) 180f else 0f, label = "")
                Icon(
                    modifier = Modifier.rotate(rotate),
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "展开"
                )
            }
        )

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)) {
                content()
            }
        }
    }
}

@Composable
@Preview
private fun Preview() {
    FogIslandTheme {
        Surface(Modifier.size(412.dp, 600.dp), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                repeat(5) {
                    var expanded by remember { mutableStateOf(false) }
                    NavigationItemGroup(
                        label = { Text("你好", style = MaterialTheme.typography.labelLarge) },
                        selected = false,
                        expanded = expanded,
                        modifier = Modifier.width(200.dp),
                        onExpandStateChange = { expanded = it }
                    ) {
                        NavigationDrawerItem(
                            label = { Text("再见", style = MaterialTheme.typography.labelLarge) },
                            selected = false,
                            onClick = {},
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        )
                    }
                }
            }
        }
    }
}
