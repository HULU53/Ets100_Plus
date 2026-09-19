package com.hulu.etsplus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    if (LocalUiStyle.current == UiStyle.Miuix) {
        MiuixSettingsScreen()
        return
    }

    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }
    var automationLoggingEnabled by remember {
        mutableStateOf(SettingsManager.isAutomationLoggingEnabled())
    }
    
    Scaffold(
        topBar = { FeTopAppBar(title = "E+") }
    ) { p ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(p)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            Text("设置", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Medium)
            Text("配置应用行为与个性化选项", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))

            // 运行授权设置
            FeOutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsListItem(Icons.Default.Build, "运行授权", "配置 Shizuku、Root 或其他模式") {
                        context.startActivity(ActivationActivity.createIntent(context))
                    }
                    FeThinDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsListItem(Icons.Default.Security, "激活状态", "查看当前激活模式与运行授权") {
                        context.startActivity(ActivationActivity.createIntent(context))
                    }
                    FeThinDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsListItem(Icons.Default.Tune, "通用设置", "语言、时区等常规选项") {
                        context.startActivity(GeneralSettingsActivity.createIntent(context))
                    }
                    FeThinDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsListItem(Icons.Default.Palette, "主题", "彩色主题与明暗模式") {
                        context.startActivity(ThemeSettingsActivity.createIntent(context))
                    }
                    FeThinDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsListItem(Icons.Default.Science, "悬浮窗", "修改悬浮窗参数") {
                        context.startActivity(TestFeature1Activity.createIntent(context))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 其他选项
            FeOutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsListItem(Icons.Default.Info, "关于 E+", "应用信息与致谢", hideChevron = true) {
                        showAboutDialog = true
                    }
                    FeThinDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsListItem(Icons.Default.Gavel, "法律信息与使用守则", "使用前请阅读并遵守", hideChevron = false) {
                        context.startActivity(LegalActivity.createIntent(context))
                    }
                    FeThinDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsSwitchListItem(
                        icon = Icons.Default.BugReport,
                        title = "自动化日志",
                        sub = if (automationLoggingEnabled) {
                            "记录模块日志、崩溃堆栈和 EplusDebug.txt"
                        } else {
                            "已关闭，不再写入任何自动化日志"
                        },
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
                        }
                    )
                }
            }

            Spacer(Modifier.height(88.dp))
        }
    }
    
    // 关于对话框
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun SettingsSwitchListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    sub: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (sub.isNotEmpty()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        FeOutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 关闭按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                
                // 应用图标
                Image(
                    painter = painterResource(id = R.drawable.ic_eplus),
                    contentDescription = "应用图标",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
                
                Spacer(Modifier.height(16.dp))
                
                // 应用名称
                Text(
                    "E+",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(Modifier.height(24.dp))
                
                // 宝贝分为两个卡片喵~
                // 软件信息卡片
                FeOutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "软件信息",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "E+",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "版本: ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "开源许可证：GPL-3.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "上游：Fuck_ets100 by rzsgsfm (laststudio)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "源码：https://github.com/HULU53/Ets100_Plus/",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://github.com/HULU53/Ets100_Plus/")
                                    )
                                )
                            }
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // 关于作者卡片
                FeOutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "关于作者",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Hulu53",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                // 关闭按钮
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("关闭")
                }
            }
        }
    }
}
