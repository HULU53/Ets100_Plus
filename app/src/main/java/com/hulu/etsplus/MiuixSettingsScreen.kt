package com.hulu.etsplus

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
internal fun MiuixSettingsScreen() {
    val context = LocalContext.current
    var showAbout by remember { mutableStateOf(false) }
    var automationLoggingEnabled by remember {
        mutableStateOf(SettingsManager.isAutomationLoggingEnabled())
    }

    Scaffold(topBar = { SmallTopAppBar(title = "E+") }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = "设置", style = MiuixTheme.textStyles.title1, fontWeight = FontWeight.Bold)
            Text(
                text = "配置应用行为与个性化选项",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )

            MiuixSettingsGroup {
                ArrowPreference(
                    title = "运行授权",
                    summary = "配置 Shizuku、Root 或其他模式",
                    onClick = { context.startActivity(ActivationActivity.createIntent(context)) },
                )
                ArrowPreference(
                    title = "激活状态",
                    summary = "查看当前激活模式与运行授权",
                    onClick = { context.startActivity(ActivationActivity.createIntent(context)) },
                )
                ArrowPreference(
                    title = "通用设置",
                    summary = "语言、时区等常规选项",
                    onClick = { context.startActivity(GeneralSettingsActivity.createIntent(context)) },
                )
                ArrowPreference(
                    title = "主题",
                    summary = "界面风格、色号与明暗模式",
                    onClick = { context.startActivity(ThemeSettingsActivity.createIntent(context)) },
                )
                ArrowPreference(
                    title = "悬浮窗设置",
                    summary = "修改悬浮窗参数",
                    onClick = { context.startActivity(TestFeature1Activity.createIntent(context)) },
                )
            }

            MiuixSettingsGroup {
                ArrowPreference(
                    title = "关于 E+",
                    summary = "应用信息与致谢",
                    onClick = { showAbout = true },
                )
                ArrowPreference(
                    title = "法律信息与使用守则",
                    summary = "使用前请阅读并遵守",
                    onClick = { context.startActivity(LegalActivity.createIntent(context)) },
                )
                SwitchPreference(
                    checked = automationLoggingEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            SettingsManager.saveAutomationLoggingEnabled(true)
                            automationLoggingEnabled = true
                            AutomationLog.info("Settings", "automation logging enabled")
                        } else {
                            AutomationLog.info("Settings", "automation logging disabled")
                            AutomationLog.clearTaskContext()
                            SettingsManager.saveAutomationLoggingEnabled(false)
                            automationLoggingEnabled = false
                        }
                    },
                    title = "自动化日志",
                    summary = if (automationLoggingEnabled) {
                        "记录模块日志、崩溃堆栈和 EplusDebug.txt"
                    } else {
                        "已关闭，不再写入任何自动化日志"
                    },
                )
            }
        }
    }

    if (showAbout) {
        MiuixAboutDialog(onDismiss = { showAbout = false })
    }
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
private fun MiuixAboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            insideMargin = PaddingValues(24.dp),
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.surfaceContainer,
                contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_eplus),
                    contentDescription = "应用图标",
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)),
                )
                Text(text = "E+", style = MiuixTheme.textStyles.title1, fontWeight = FontWeight.Bold)
                Text(text = "版本: ${BuildConfig.VERSION_NAME}", style = MiuixTheme.textStyles.body2)
                Text(text = "作者: Hulu53", style = MiuixTheme.textStyles.body2)
                Text(text = "开源许可证：GPL-3.0", style = MiuixTheme.textStyles.body2)
                Text(text = "上游：Fuck_ets100 by rzsgsfm (laststudio)", style = MiuixTheme.textStyles.body2)
                Text(
                    text = "源码：https://github.com/HULU53/Ets100_Plus/",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://github.com/HULU53/Ets100_Plus/")
                            )
                        )
                    }
                )
                Card(
                    cornerRadius = 14.dp,
                    insideMargin = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                    ),
                    onClick = onDismiss,
                ) {
                    Text(text = "关闭", style = MiuixTheme.textStyles.button)
                }
            }
        }
    }
}