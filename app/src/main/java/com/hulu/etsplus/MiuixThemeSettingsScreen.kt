package com.hulu.etsplus

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MiuixThemeSettingsScreen(
    onBack: () -> Unit,
    onThemeChanged: ((AppTheme) -> Unit)? = null,
    onDarkModeChanged: ((Boolean) -> Unit)? = null,
    onAutoDarkModeChanged: ((Boolean) -> Unit)? = null,
    onDynamicColorChanged: ((Boolean) -> Unit)? = null,
    onUiStyleChanged: ((UiStyle) -> Unit)? = null,
    onBlurEnabledChanged: ((Boolean) -> Unit)? = null,
    onPredictiveBackChanged: ((PredictiveBackMode) -> Unit)? = null,
) {
    var selectedTheme by remember { mutableStateOf(ThemeManager.getSavedTheme()) }
    var isDarkMode by remember { mutableStateOf(ThemeManager.getSavedDarkMode()) }
    var isAutoDarkMode by remember { mutableStateOf(ThemeManager.getSavedAutoDarkMode()) }
    var useDynamicColor by remember { mutableStateOf(ThemeManager.getSavedDynamicColor()) }
    var currentUiStyle by remember { mutableStateOf(ThemeManager.getSavedUiStyle()) }
    var blurEnabled by remember { mutableStateOf(ThemeManager.getSavedBlurEnabled()) }
    var homeIconColor by remember { mutableIntStateOf(ThemeManager.getSavedHomeIconColor()) }
    var homeIconColorInput by remember(homeIconColor) { mutableStateOf(homeIconColor.toHexColorString()) }
    var homeIconColorError by remember { mutableStateOf(false) }
    var predictiveBackMode by remember { mutableStateOf(SettingsManager.getPredictiveBackMode()) }
    var showPredictiveBackDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "主题设置",
                navigationIcon = {
                    Text(
                        text = "返回",
                        modifier = Modifier.clickable(onClick = onBack).padding(12.dp),
                        color = MiuixTheme.colorScheme.primary,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MiuixSectionTitle("界面风格")
            MiuixSettingsGroup {
                UiStyle.entries.forEach { style ->
                    ArrowPreference(
                        title = style.displayName,
                        summary = when (style) {
                            UiStyle.Material -> "使用现有 Material 3 界面"
                            UiStyle.Miuix -> "使用运行时 Miuix 组件"
                        },
                        endActions = {
                            if (currentUiStyle == style) Text("当前", color = MiuixTheme.colorScheme.primary)
                        },
                        onClick = {
                            currentUiStyle = style
                            ThemeManager.saveUiStyle(style)
                            onUiStyleChanged?.invoke(style)
                        },
                    )
                }
            }

            MiuixSettingsGroup {
                SwitchPreference(
                    checked = blurEnabled,
                    onCheckedChange = { checked ->
                        blurEnabled = checked
                        ThemeManager.saveBlurEnabled(checked)
                        onBlurEnabledChanged?.invoke(checked)
                    },
                    title = "界面模糊（实验性）",
                    summary = if (blurEnabled) {
                        "为 Miuix 顶部栏和卡片启用背景模糊"
                    } else {
                        "减少动画和渲染开销"
                    },
                )
            }

            MiuixSectionTitle("颜色主题")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ThemeManager.getColorThemes().forEach { theme ->
                    MiuixThemeCard(
                        theme = theme,
                        selected = selectedTheme == theme,
                        onClick = {
                            selectedTheme = theme
                            ThemeManager.saveTheme(theme)
                            onThemeChanged?.invoke(theme)
                        },
                    )
                }
            }

            MiuixSettingsGroup {
                SwitchPreference(
                    checked = useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    onCheckedChange = { checked ->
                        useDynamicColor = checked
                        ThemeManager.saveDynamicColor(checked)
                        onDynamicColorChanged?.invoke(checked)
                    },
                    title = "从壁纸提取主题色",
                    summary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        "使用系统动态配色"
                    } else {
                        "需要 Android 12 或更高版本"
                    },
                )
            }

            MiuixSectionTitle("显示模式")
            MiuixSettingsGroup {
                SwitchPreference(
                    checked = isAutoDarkMode,
                    onCheckedChange = { checked ->
                        isAutoDarkMode = checked
                        ThemeManager.saveAutoDarkMode(checked)
                        onAutoDarkModeChanged?.invoke(checked)
                    },
                    title = "跟随系统",
                    summary = "自动使用系统的日间或夜间模式",
                )
                ArrowPreference(
                    title = "日间",
                    enabled = !isAutoDarkMode,
                    endActions = {
                        if (!isDarkMode) Text("当前", color = MiuixTheme.colorScheme.primary)
                    },
                    onClick = {
                        isDarkMode = false
                        ThemeManager.saveDarkMode(false)
                        onDarkModeChanged?.invoke(false)
                    },
                )
                ArrowPreference(
                    title = "夜间",
                    enabled = !isAutoDarkMode,
                    endActions = {
                        if (isDarkMode) Text("当前", color = MiuixTheme.colorScheme.primary)
                    },
                    onClick = {
                        isDarkMode = true
                        ThemeManager.saveDarkMode(true)
                        onDarkModeChanged?.invoke(true)
                    },
                )
            }

            MiuixSectionTitle("主界面图标")
            MiuixSettingsGroup {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(homeIconColor), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "E+",
                            color = homeIconContentColor(homeIconColor),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("颜色代码", style = MiuixTheme.textStyles.body2)
                        Card(
                            cornerRadius = 10.dp,
                            insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            colors = CardDefaults.defaultColors(
                                color = MiuixTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                            ),
                        ) {
                            BasicTextField(
                                value = homeIconColorInput,
                                onValueChange = {
                                    homeIconColorInput = it
                                    homeIconColorError = false
                                },
                                singleLine = true,
                                textStyle = MiuixTheme.textStyles.body1.copy(
                                    color = if (homeIconColorError) {
                                        MiuixTheme.colorScheme.error
                                    } else {
                                        MiuixTheme.colorScheme.onSurfaceContainerHigh
                                    },
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ArrowPreference(
                                title = "应用",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val parsed = parseHexColor(homeIconColorInput)
                                    if (parsed == null) {
                                        homeIconColorError = true
                                    } else {
                                        homeIconColor = parsed
                                        ThemeManager.saveHomeIconColor(parsed)
                                    }
                                },
                            )
                            ArrowPreference(
                                title = "默认",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    homeIconColor = ThemeManager.defaultHomeIconColor
                                    homeIconColorInput = homeIconColor.toHexColorString()
                                    homeIconColorError = false
                                    ThemeManager.saveHomeIconColor(homeIconColor)
                                },
                            )
                        }
                    }
                }
            }

            MiuixSectionTitle("返回手势")
            MiuixSettingsGroup {
                ArrowPreference(
                    title = "预见性返回动画",
                    summary = "${predictiveBackMode.label} · ${predictiveBackMode.description}",
                    onClick = { showPredictiveBackDialog = true },
                )
            }
        }
    }

    if (showPredictiveBackDialog) {
        Dialog(onDismissRequest = { showPredictiveBackDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
                insideMargin = PaddingValues(vertical = 8.dp),
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.surfaceContainer,
                    contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
                ),
            ) {
                PredictiveBackMode.entries.forEach { mode ->
                    ArrowPreference(
                        title = mode.label,
                        summary = mode.description,
                        endActions = {
                            if (predictiveBackMode == mode) Text("当前", color = MiuixTheme.colorScheme.primary)
                        },
                        onClick = {
                            predictiveBackMode = mode
                            SettingsManager.savePredictiveBackMode(mode)
                            showPredictiveBackDialog = false
                            onPredictiveBackChanged?.invoke(mode)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MiuixSectionTitle(text: String) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.title3,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun MiuixSettingsGroup(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(vertical = 6.dp),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainer,
            contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
        ),
    ) {
        content()
    }
}

@Composable
private fun MiuixThemeCard(
    theme: AppTheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.width(92.dp),
        cornerRadius = 14.dp,
        insideMargin = PaddingValues(12.dp),
        colors = CardDefaults.defaultColors(
            color = if (selected) {
                MiuixTheme.colorScheme.primaryContainer
            } else {
                MiuixTheme.colorScheme.surfaceContainer
            },
            contentColor = if (selected) {
                MiuixTheme.colorScheme.onPrimaryContainer
            } else {
                MiuixTheme.colorScheme.onSurfaceContainer
            },
        ),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.size(36.dp).background(theme.primary, CircleShape))
            Text(theme.displayName, style = MiuixTheme.textStyles.body2)
        }
    }
}