package com.hulu.etsplus

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    onThemeChanged: ((AppTheme) -> Unit)? = null,
    onDarkModeChanged: ((Boolean) -> Unit)? = null,
    onAutoDarkModeChanged: ((Boolean) -> Unit)? = null,
    onDynamicColorChanged: ((Boolean) -> Unit)? = null,
    onUiStyleChanged: ((UiStyle) -> Unit)? = null,
    onBlurEnabledChanged: ((Boolean) -> Unit)? = null,
    onPredictiveBackChanged: ((PredictiveBackMode) -> Unit)? = null
) {
    if (LocalUiStyle.current == UiStyle.Miuix) {
        MiuixThemeSettingsScreen(
            onBack = onBack,
            onThemeChanged = onThemeChanged,
            onDarkModeChanged = onDarkModeChanged,
            onAutoDarkModeChanged = onAutoDarkModeChanged,
            onDynamicColorChanged = onDynamicColorChanged,
            onUiStyleChanged = onUiStyleChanged,
            onPredictiveBackChanged = onPredictiveBackChanged,
        )
        return
    }

    val currentTheme = ThemeManager.getSavedTheme()
    var selectedTheme by remember { mutableStateOf(currentTheme) }
    var isDarkMode by remember { mutableStateOf(ThemeManager.getSavedDarkMode()) }
    var isAutoDarkMode by remember { mutableStateOf(ThemeManager.getSavedAutoDarkMode()) }
    var useDynamicColor by remember { mutableStateOf(ThemeManager.getSavedDynamicColor()) }
    var currentUiStyle by remember { mutableStateOf(ThemeManager.getSavedUiStyle()) }
    var blurEnabled by remember { mutableStateOf(ThemeManager.getSavedBlurEnabled()) }
    var homeIconColor by remember { mutableIntStateOf(ThemeManager.getSavedHomeIconColor()) }
    var homeIconColorInput by remember(homeIconColor) {
        mutableStateOf(homeIconColor.toHexColorString())
    }
    var homeIconColorError by remember { mutableStateOf(false) }
    var predictiveBackMode by remember { mutableStateOf(SettingsManager.getPredictiveBackMode()) }
    var showPredictiveBackDialog by remember { mutableStateOf(false) }
    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val systemDarkMode = isSystemInDarkTheme()
    val effectiveDarkMode = if (isAutoDarkMode) systemDarkMode else isDarkMode
    
    val colorThemes = ThemeManager.getColorThemes()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("主题设置", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Palette, null)
                    }
                }
            )
        }
    ) { p ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(p)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                "界面风格",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                UiStyle.entries.forEachIndexed { index, uiStyle ->
                    SegmentedButton(
                        selected = currentUiStyle == uiStyle,
                        onClick = {
                            currentUiStyle = uiStyle
                            ThemeManager.saveUiStyle(uiStyle)
                            onUiStyleChanged?.invoke(uiStyle)
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = UiStyle.entries.size
                        ),
                        label = { Text(uiStyle.displayName) }
                    )
                }
            }

            ListItem(
                headlineContent = { Text("界面模糊") },
                supportingContent = {
                    Text(if (blurEnabled) "为 Miuix 顶部栏和卡片启用背景模糊" else "减少动画和渲染开销")
                },
                leadingContent = { Icon(Icons.Default.Wallpaper, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = blurEnabled,
                        onCheckedChange = { checked ->
                            blurEnabled = checked
                            ThemeManager.saveBlurEnabled(checked)
                            onBlurEnabledChanged?.invoke(checked)
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "主界面图标",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(homeIconColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_eplus_foreground),
                        contentDescription = "主界面图标预览",
                        colorFilter = ColorFilter.tint(homeIconContentColor(homeIconColor)),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = homeIconColorInput,
                        onValueChange = { value ->
                            homeIconColorInput = value
                            homeIconColorError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("颜色代码") },
                        placeholder = { Text("#2196F3") },
                        singleLine = true,
                        isError = homeIconColorError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        )
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val parsedColor = parseHexColor(homeIconColorInput)
                                if (parsedColor == null) {
                                    homeIconColorError = true
                                } else {
                                    homeIconColor = parsedColor
                                    ThemeManager.saveHomeIconColor(parsedColor)
                                }
                            }
                        ) {
                            Text("应用")
                        }
                        OutlinedButton(
                            onClick = {
                                homeIconColor = ThemeManager.defaultHomeIconColor
                                homeIconColorInput = homeIconColor.toHexColorString()
                                homeIconColorError = false
                                ThemeManager.saveHomeIconColor(homeIconColor)
                            }
                        ) {
                            Text("默认")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            
            Text(
                "颜色主题", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                "选择白天和夜间模式共用的主色调",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(colorThemes) { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = selectedTheme == theme,
                        enabled = !(useDynamicColor && dynamicColorAvailable),
                        onClick = {
                            selectedTheme = theme
                            ThemeManager.saveTheme(theme)
                            onThemeChanged?.invoke(theme)
                        }
                    )
                }
            }

            ListItem(
                headlineContent = { Text("从壁纸提取主题色") },
                supportingContent = {
                    Text(
                        if (dynamicColorAvailable) {
                            "使用系统 Material You 动态配色"
                        } else {
                            "需要 Android 12 或更高版本"
                        }
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.Wallpaper, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = useDynamicColor && dynamicColorAvailable,
                        enabled = dynamicColorAvailable,
                        onCheckedChange = { checked ->
                            useDynamicColor = checked
                            ThemeManager.saveDynamicColor(checked)
                            onDynamicColorChanged?.invoke(checked)
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                "显示模式", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                "在当前彩色主题上切换白天或夜间",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            ListItem(
                headlineContent = { Text("跟随系统") },
                supportingContent = { Text(if (systemDarkMode) "当前系统为夜间模式" else "当前系统为日间模式") },
                leadingContent = {
                    Icon(Icons.Default.Sync, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = isAutoDarkMode,
                        onCheckedChange = { checked ->
                            isAutoDarkMode = checked
                            ThemeManager.saveAutoDarkMode(checked)
                            onAutoDarkModeChanged?.invoke(checked)
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                SegmentedButton(
                    selected = !isDarkMode,
                    enabled = !isAutoDarkMode,
                    onClick = {
                        isDarkMode = false
                        ThemeManager.saveDarkMode(false)
                        onDarkModeChanged?.invoke(false)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = themeModeSegmentedButtonColors(),
                    icon = { Icon(Icons.Default.LightMode, contentDescription = null) },
                    label = { Text("日间") }
                )
                SegmentedButton(
                    selected = isDarkMode,
                    enabled = !isAutoDarkMode,
                    onClick = {
                        isDarkMode = true
                        ThemeManager.saveDarkMode(true)
                        onDarkModeChanged?.invoke(true)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = themeModeSegmentedButtonColors(),
                    icon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                    label = { Text("夜间") }
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "返回手势",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                "切换缩放、渐变、系统默认或关闭动画",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            ListItem(
                headlineContent = { Text("预见性返回动画") },
                supportingContent = {
                    Text("${predictiveBackMode.label} · ${predictiveBackMode.description}")
                },
                leadingContent = {
                    Icon(Icons.Default.Sync, contentDescription = null)
                },
                trailingContent = {
                    Text(
                        predictiveBackMode.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable { showPredictiveBackDialog = true }
            )
            
            Spacer(Modifier.height(88.dp))
        }
    }

    if (showPredictiveBackDialog) {
        AlertDialog(
            onDismissRequest = { showPredictiveBackDialog = false },
            title = { Text("预见性返回动画") },
            text = {
                Column {
                    PredictiveBackMode.entries.forEach { mode ->
                        ListItem(
                            headlineContent = { Text(mode.label) },
                            supportingContent = { Text(mode.description) },
                            trailingContent = {
                                if (predictiveBackMode == mode) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                predictiveBackMode = mode
                                SettingsManager.savePredictiveBackMode(mode)
                                showPredictiveBackDialog = false
                                onPredictiveBackChanged?.invoke(mode)
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPredictiveBackDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

internal fun parseHexColor(value: String): Int? {
    val normalized = value.trim().removePrefix("#")
    val rgb = when (normalized.length) {
        6 -> normalized
        else -> return null
    }
    return rgb.toLongOrNull(16)
        ?.takeIf { it in 0x000000L..0xFFFFFFL }
        ?.let { (0xFF000000L or it).toInt() }
}

internal fun Int.toHexColorString(): String {
    return "#%06X".format(this and 0xFFFFFF)
}

@Composable
fun ThemeCard(
    theme: AppTheme,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .graphicsLayer {
                alpha = if (enabled) 1f else 0.45f
            }
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 主色调预览圆
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(theme.primary)
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = theme.onPrimary,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                theme.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun themeModeSegmentedButtonColors(): SegmentedButtonColors {
    return SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledActiveContainerColor = MaterialTheme.colorScheme.primaryContainer,
        disabledActiveContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        disabledInactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        disabledInactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
    )
}
