package ink.duo3.fogisland.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Bookmarks
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ink.duo3.fogisland.data.LocalThemeSettings
import ink.duo3.fogisland.data.LocalTimeSettings
import ink.duo3.fogisland.data.ensureSubscriptionUuid
import ink.duo3.fogisland.data.subscriptionUuidFlow
import ink.duo3.fogisland.data.updateFollowSystemAppearance
import ink.duo3.fogisland.data.updateMonetSeed
import ink.duo3.fogisland.data.updateShowSeconds
import ink.duo3.fogisland.data.updateSubscriptionUuid
import ink.duo3.fogisland.data.updateUseDarkMode
import ink.duo3.fogisland.data.updateUseMonet
import ink.duo3.fogisland.data.updateUsePreciseTime
import ink.duo3.fogisland.data.updateUseUtcPlus8Time
import ink.duo3.fogisland.shared.storage.preferences.isSubscriptionUuidFormatValid
import ink.duo3.fogisland.shared.storage.preferences.normalizeSubscriptionUuidInput
import ink.duo3.fogisland.ui.components.SettingItem
import ink.duo3.fogisland.ui.components.SettingItemGroup
import ink.duo3.fogisland.ui.components.SettingItemWithSwitch
import ink.duo3.fogisland.ui.components.SubscriptionUuidEditorDialog
import ink.duo3.fogisland.ui.components.normalizeSubscriptionUuidFieldValue
import ink.duo3.fogisland.ui.components.subscriptionUuidTextFieldValue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onMenuClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeSettings = LocalThemeSettings.current
    val timeSettings = LocalTimeSettings.current
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
                ),
                scrollBehavior = topAppBarScrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp),
            contentPadding = innerPadding
        ) {
            item {
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

            item {
                SettingItemGroup(title = "时间") {
                    SettingItemWithSwitch(
                        title = { Text("以 UTC+8 显示时间") },
                        description = { Text("关闭后按设备本地时区显示。") },
                        checked = timeSettings.useUtcPlus8Time,
                        onCheckedChange = { scope.launch { context.updateUseUtcPlus8Time(it) } }
                    )
                    SettingItemWithSwitch(
                        title = { Text("使用精确时间") },
                        description = { Text("关闭后使用相对时间。") },
                        checked = timeSettings.usePreciseTime,
                        onCheckedChange = { scope.launch { context.updateUsePreciseTime(it) } }
                    )
                    AnimatedVisibility(visible = timeSettings.usePreciseTime) {
                        SettingItemWithSwitch(
                            title = { Text("显示秒") },
                            description = { Text("仅在精确时间下生效。") },
                            checked = timeSettings.showSeconds,
                            onCheckedChange = { scope.launch { context.updateShowSeconds(it) } }
                        )
                    }
                }
            }

            item {
                SettingItemGroup(title = "订阅") {
                    SettingItem(
                        title = { Text("订阅 ID") },
                        description = {
                            subscriptionUuid?.let { uuid ->
                                Text(
                                    text = uuid,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            } ?: Text("用于区分订阅列表。尚未生成。")
                        },
                        icon = { Icon(Icons.Default.Bookmarks, contentDescription = null) },
                        onClick = {
                            subscriptionUuidDraft = subscriptionUuidTextFieldValue(
                                subscriptionUuid.orEmpty()
                            )
                            subscriptionUuidError = null
                            isSubscriptionUuidDialogVisible = true
                        }
                    )
                }
            }

            item {
                CookieSettingsSection()
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
                val normalizedValue = normalizeSubscriptionUuidInput(
                    subscriptionUuidDraft.text
                )
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
