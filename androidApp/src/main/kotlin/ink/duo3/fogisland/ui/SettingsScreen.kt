package ink.duo3.fogisland.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.ThemeSettings
import ink.duo3.fogisland.data.themeSettingsFlow
import ink.duo3.fogisland.data.updateFollowSystemAppearance
import ink.duo3.fogisland.data.updateMonetSeed
import ink.duo3.fogisland.data.updateUseDarkMode
import ink.duo3.fogisland.data.updateUseMonet
import ink.duo3.fogisland.ui.components.SettingItem
import ink.duo3.fogisland.ui.components.SettingItemGroup
import ink.duo3.fogisland.ui.components.SettingItemWithSwitch
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onMenuClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeSettings by context.themeSettingsFlow.collectAsState(initial = ThemeSettings())

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            LargeTopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(
                        onClick = onMenuClick,
                        colors = IconButtonDefaults.iconButtonColors().copy(
                            containerColor = MaterialTheme.colorScheme.surfaceBright
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "菜单"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SettingItemGroup(title = "外观") {
                SettingItemWithSwitch(
                    title = { Text("跟随系统外观") },
                    description = { Text("亮暗色模式跟随系统设置。") },
                    icon = { Icon(Icons.Default.BrightnessAuto, contentDescription = null) },
                    checked = themeSettings.followSystemAppearance,
                    onCheckedChange = { scope.launch { context.updateFollowSystemAppearance(it) } }
                )
                AnimatedVisibility(visible = !themeSettings.followSystemAppearance) {
                    SettingItemWithSwitch(
                        title = { Text("深色模式") },
                        icon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                        checked = themeSettings.useDarkMode,
                        onCheckedChange = { scope.launch { context.updateUseDarkMode(it) } }
                    )
                }
                SettingItemWithSwitch(
                    title = { Text("使用莫奈色板") },
                    description = { Text("使用 Material Design 3 的默认色板。") },
                    icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                    checked = themeSettings.useMonet,
                    onCheckedChange = { scope.launch { context.updateUseMonet(it) } }
                )
                
                AnimatedVisibility(visible = themeSettings.useMonet) {
                    val isA12AndAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    SettingItem(
                        title = { Text("自定义主题色") },
                        description = { Text(
                            if (isA12AndAbove) {"跟随系统壁纸，或选择一个主题颜色。"} 
                            else {"选择一个主题颜色。"}
                        ) },
                        bottomAction = {
                            val colors = listOf(
                                0xFFF44336.toInt(), // Red 500
                                0xFFE91E63.toInt(), // Pink 500
                                0xFF9C27B0.toInt(), // Purple 500
                                0xFF673AB7.toInt(), // Deep Purple 500
                                0xFF3F51B5.toInt(), // Indigo 500
                                0xFF2196F3.toInt(), // Blue 500
                                0xFF03A9F4.toInt(), // Light Blue 500
                                0xFF00BCD4.toInt(), // Cyan 500
                                0xFF009688.toInt(), // Teal 500
                                0xFF4CAF50.toInt(), // Green 500
                                0xFF8BC34A.toInt(), // Light Green 500
                                0xFFCDDC39.toInt(), // Lime 500
                                0xFFFFEB3B.toInt(), // Yellow 500
                                0xFFFFC107.toInt(), // Amber 500
                                0xFFFF9800.toInt(), // Orange 500
                                0xFFFF5722.toInt(), // Deep Orange 500
                            )
                            LazyRow(
                                contentPadding = PaddingValues(16.dp, 12.dp, 4.dp, 4.dp)
                            ) {
                                if (isA12AndAbove) {
                                    item {
                                        val isSelected = themeSettings.monetSeed == 0
                                        val borderSize by animateDpAsState(
                                            targetValue = if (isSelected) 3.dp else 0.dp,
                                            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                            label = "colorSelectedAnim"
                                        )
                                        val borderAlpha by animateFloatAsState(
                                            targetValue = if (isSelected) 1f else 0f,
                                            label = "colorSelectedAlpha"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 12.dp)
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .border(
                                                    width = borderSize,
                                                    color = MaterialTheme.colorScheme.primary
                                                        .copy(alpha = borderAlpha),
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    scope.launch { context.updateMonetSeed(0) }
                                            },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Palette,
                                                contentDescription = "跟随系统取色",
                                                tint = if (isSelected)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                                items(colors) { colorInt ->
                                    val isSelected = themeSettings.monetSeed == colorInt
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorInt))
                                            .clickable {
                                                scope.launch { context.updateMonetSeed(colorInt) }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val innerSize by animateDpAsState(
                                            targetValue = if (isSelected) 24.dp else 0.dp,
                                            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                            label = "colorSelectedAnim"
                                        )
                                        val innerAlpha by animateFloatAsState(
                                            targetValue = if (isSelected) 1f else 0f,
                                            label = "colorSelectedAlpha"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(innerSize)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onPrimary
                                                    .copy(alpha = innerAlpha))
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
