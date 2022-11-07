package ink.duo3.xdnmb.android.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
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
import ink.duo3.xdnmb.android.ui.theme.AppTheme

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
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = LocalContentColor.current.copy(0.08f)
            ),
            label = label,
            selected = selected,
            onClick = { onExpandStateChange(!expanded) },
            badge = {
                val rotate by animateFloatAsState(if (expanded) 180f else 0f)
                Icon(
                    modifier = Modifier.rotate(rotate),
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown"
                )
            }
        )

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(start = 16.dp, top = 2.dp, bottom = 2.dp)) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun Preview() {
    AppTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                repeat(5) {
                    var expanded by remember { mutableStateOf(false) }
                    NavigationItemGroup(
                        label = { Text("CNM") },
                        selected = false,
                        expanded = expanded,
                        modifier = Modifier.width(200.dp),
                        onExpandStateChange = { expanded = it }
                    ) {
                        NavigationDrawerItem(
                            label = { HtmlText(html = "hahaha") },
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