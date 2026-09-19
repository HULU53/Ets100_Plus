package com.hulu.etsplus

import android.app.Activity
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BorderStyle
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestFeature1Screen(
    onBack: () -> Unit,
    onOpenTaskSelection: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var config by remember { mutableStateOf(SettingsManager.getFloatingWindowConfig()) }
    var hasOverlayPermission by remember {
        mutableStateOf(PermissionsHelper.hasOverlayPermission(context))
    }
    var hasAccessibilityPermission by remember {
        mutableStateOf(AutomationAccessibilityService.isEnabled(context))
    }
    var mediaProjectionReady by remember {
        mutableStateOf(MediaProjectionCaptureSource.isReady())
    }
    val session by AutomationController.snapshot.collectAsState()
    val automationLogs by AutomationLog.entries.collectAsState()
    val logTimeFormatter = remember {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    }
    var speechAudioConfig by remember {
        mutableStateOf(SettingsManager.getSpeechAudioConfig())
    }

    fun updateConfig(updated: FloatingWindowConfig) {
        config = updated
        SettingsManager.saveFloatingWindowConfig(updated)
    }

    fun updateSpeechAudioConfig(updated: SpeechAudioConfig) {
        speechAudioConfig = updated
        SettingsManager.saveSpeechAudioConfig(updated)
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            AutomationOverlayService.show(context)
            val promotionReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                AutomationOverlayService.promoteForMediaProjection()
            mediaProjectionReady = promotionReady &&
                MediaProjectionCaptureSource.setPermission(
                    context,
                    result.resultCode,
                    result.data
                )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = PermissionsHelper.hasOverlayPermission(context)
                hasAccessibilityPermission = AutomationAccessibilityService.isEnabled(context)
                mediaProjectionReady = MediaProjectionCaptureSource.isReady()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("悬浮窗", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TestFeatureSectionTitle("悬浮窗权限")
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                TestFeatureSwitchRow(
                    icon = Icons.Default.SettingsSuggest,
                    title = "允许显示悬浮窗",
                    sub = if (hasOverlayPermission) "已授权" else "需要前往系统设置授权",
                    checked = hasOverlayPermission,
                    onCheckedChange = {
                        if (!hasOverlayPermission) {
                            PermissionsHelper.requestOverlayPermission(context)
                        }
                    }
                )
                TestFeatureSwitchRow(
                    icon = Icons.Default.AccessibilityNew,
                    title = "自动化无障碍服务",
                    sub = if (hasAccessibilityPermission) "已启用" else "需要前往系统设置启用",
                    checked = hasAccessibilityPermission,
                    onCheckedChange = {
                        if (!hasAccessibilityPermission) {
                            AutomationAccessibilityService.openSettings(context)
                        }
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text("屏幕捕获备用通道", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (mediaProjectionReady) "已授权" else "未授权",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            AutomationOverlayService.show(context)
                            val manager = context.getSystemService(
                                MediaProjectionManager::class.java
                            )
                            mediaProjectionLauncher.launch(
                                manager.createScreenCaptureIntent()
                            )
                        }
                    ) {
                        Text(if (mediaProjectionReady) "重新授权" else "授权")
                    }
                }
            }

            TestFeatureSectionTitle("答案任务")
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (session.tasks.isEmpty()) {
                            "尚未选择答案"
                        } else {
                            "已选择 ${session.tasks.size} 道题，当前进度 ${session.progressText}"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (session.message.isNotBlank()) {
                        Text(
                            text = session.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenTaskSelection,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("选择答案")
                        }
                        OutlinedButton(
                            onClick = {
                                AutomationOverlayService.show(context)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("显示悬浮窗")
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                AutomationOverlayService.show(context)
                                AutomationOrchestrator.start()
                            },
                            enabled = session.tasks.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Text("开始", modifier = Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(
                            onClick = {
                                AutomationOrchestrator.stop()
                                AutomationOverlayService.hide(context)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("停止并隐藏")
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TestFeatureSectionTitle("悬浮窗尺寸")
                Text(
                    text = "tips：文本显示不全时尝试调高悬浮窗高度并重启悬浮窗",
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column {
                    TestFeatureSliderRow(
                        icon = Icons.Default.AspectRatio,
                        title = "宽度",
                        valueText = "${config.widthDp} dp",
                        value = config.widthDp.toFloat(),
                        valueRange = FloatingWindowConfig.MIN_WIDTH_DP.toFloat()..
                            FloatingWindowConfig.MAX_WIDTH_DP.toFloat(),
                        steps = 17,
                        onValueChange = {
                            updateConfig(config.copy(widthDp = it.toInt()))
                        }
                    )
                    TestFeatureSliderRow(
                        icon = Icons.Default.ScreenRotation,
                        title = "高度",
                        valueText = "${config.heightDp} dp",
                        value = config.heightDp.toFloat(),
                        valueRange = FloatingWindowConfig.MIN_HEIGHT_DP.toFloat()..
                            FloatingWindowConfig.MAX_HEIGHT_DP.toFloat(),
                        steps = 15,
                        onValueChange = {
                            updateConfig(config.copy(heightDp = it.toInt()))
                        }
                    )
                    TestFeatureSliderRow(
                        icon = Icons.Default.Opacity,
                        title = "透明度",
                        valueText = "${(config.alpha * 100).toInt()}%",
                        value = config.alpha,
                        valueRange = FloatingWindowConfig.MIN_ALPHA..
                            FloatingWindowConfig.MAX_ALPHA,
                        steps = 11,
                        onValueChange = {
                            updateConfig(config.copy(alpha = it))
                        }
                    )
                }
            }

            TestFeatureSectionTitle("停靠与交互")
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column {
                    TestFeatureEdgeSelector(
                        selected = config.edge,
                        onSelected = { updateConfig(config.copy(edge = it)) }
                    )
                    TestFeatureSliderRow(
                        icon = Icons.Default.BorderStyle,
                        title = "贴边间距",
                        valueText = "${config.marginDp} dp",
                        value = config.marginDp.toFloat(),
                        valueRange = FloatingWindowConfig.MIN_MARGIN_DP.toFloat()..
                            FloatingWindowConfig.MAX_MARGIN_DP.toFloat(),
                        steps = 15,
                        onValueChange = {
                            updateConfig(config.copy(marginDp = it.toInt()))
                        }
                    )
                    TestFeatureSwitchRow(
                        icon = Icons.Default.Lock,
                        title = "运行时锁定拖动",
                        sub = "运行中仅允许使用控制按钮",
                        checked = config.lockDraggingWhileRunning,
                        onCheckedChange = {
                            updateConfig(config.copy(lockDraggingWhileRunning = it))
                        }
                    )
                    TestFeatureSwitchRow(
                        icon = Icons.Default.BorderStyle,
                        title = "显示边界辅助线",
                        sub = "用于开发阶段检查位置和触摸区域",
                        checked = config.showDebugBounds,
                        onCheckedChange = {
                            updateConfig(config.copy(showDebugBounds = it))
                        }
                    )
                }
            }

            TestFeatureSectionTitle("朗读音频")
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column {
                    TestFeatureSwitchRow(
                        icon = Icons.Default.PlayArrow,
                        title = "强制播放增强",
                        sub = "尝试通过 Shizuku 或 Root 保持媒体音量和扬声器路由",
                        checked = speechAudioConfig.forceViaPrivilege,
                        onCheckedChange = {
                            updateSpeechAudioConfig(
                                speechAudioConfig.copy(forceViaPrivilege = it)
                            )
                        }
                    )
                    TestFeatureSliderRow(
                        icon = Icons.Default.SettingsSuggest,
                        title = "目标媒体音量",
                        valueText = "${speechAudioConfig.targetVolumePercent}%",
                        value = speechAudioConfig.targetVolumePercent.toFloat(),
                        valueRange = SpeechAudioConfig.MIN_VOLUME_PERCENT.toFloat()..
                            SpeechAudioConfig.MAX_VOLUME_PERCENT.toFloat(),
                        steps = 7,
                        onValueChange = {
                            updateSpeechAudioConfig(
                                speechAudioConfig.copy(targetVolumePercent = it.toInt())
                            )
                        }
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    updateConfig(FloatingWindowConfig())
                    updateSpeechAudioConfig(SpeechAudioConfig())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.RestartAlt, null)
                Text(
                    text = "恢复默认设置",
                    modifier = Modifier.padding(start = 8.dp),
                    fontWeight = FontWeight.Medium
                )
            }

            TestFeatureSectionTitle("运行日志")
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "最近 ${automationLogs.size} 条",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = { AutomationLog.clear() }) {
                            Text("清空")
                        }
                    }
                    if (automationLogs.isEmpty()) {
                        Text(
                            text = "暂无运行日志",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        automationLogs.takeLast(12).forEach { entry ->
                            val taskLabel = entry.task?.let { task ->
                                " #${task.index + 1}/${task.total} ${task.type}"
                            }.orEmpty()
                            Text(
                                text = "${logTimeFormatter.format(entry.timestampMs)} " +
                                    "[${entry.level}] [${entry.scope}]$taskLabel ${entry.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = when (entry.level) {
                                    AutomationLogLevel.ERROR -> MaterialTheme.colorScheme.error
                                    AutomationLogLevel.WARN -> MaterialTheme.colorScheme.tertiary
                                    AutomationLogLevel.INFO -> MaterialTheme.colorScheme.onSurface
                                    AutomationLogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestFeatureSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun TestFeatureSliderRow(
    icon: ImageVector,
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun TestFeatureSwitchRow(
    icon: ImageVector,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (sub.isNotEmpty()) {
                Text(
                    text = sub,
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
private fun TestFeatureEdgeSelector(
    selected: FloatingWindowEdge,
    onSelected: (FloatingWindowEdge) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.BorderStyle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "默认停靠方向",
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingWindowEdge.entries.forEach { edge ->
                FilterChip(
                    selected = selected == edge,
                    onClick = { onSelected(edge) },
                    label = {
                        Text(
                            when (edge) {
                                FloatingWindowEdge.NEAREST -> "最近"
                                FloatingWindowEdge.LEFT -> "左侧"
                                FloatingWindowEdge.RIGHT -> "右侧"
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
