package com.hulu.etsplus

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class HomeRuntimeStatus(
    val hasFilesPerm: Boolean,
    val hasOverlayPerm: Boolean,
    val hasAppListPerm: Boolean,
    val hasRootAvailable: Boolean,
    val hasDirectReadAvailable: Boolean,
    val cloudLoggedIn: Boolean,
    val accessibilityActive: Boolean
)

private object HomeRuntimeStatusStore {
    private const val CACHE_TTL_MS = 30_000L

    var cached = HomeRuntimeStatus(
        hasFilesPerm = false,
        hasOverlayPerm = false,
        hasAppListPerm = false,
        hasRootAvailable = false,
        hasDirectReadAvailable = false,
        cloudLoggedIn = false,
        accessibilityActive = false
    )
    var hasLoaded = false
    var lastRefreshTime = 0L
    var lastMode: ActivationMode? = null

    fun isFresh(): Boolean {
        return hasLoaded && SystemClock.elapsedRealtime() - lastRefreshTime < CACHE_TTL_MS
    }

    fun update(status: HomeRuntimeStatus, mode: ActivationMode? = lastMode) {
        cached = status
        hasLoaded = true
        lastRefreshTime = SystemClock.elapsedRealtime()
        lastMode = mode
    }
}

@Composable
fun HomeScreen(
    mode: ActivationMode,
    shizukuState: ShizukuState,
    homeIconColor: Int,
    onNavigateToActivation: () -> Unit
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val currentModeForRefresh by rememberUpdatedState(mode)
    val launchCount by AppUsageStats.launchCount.collectAsState()
    val readFileCount by AppUsageStats.readFileCount.collectAsState()
    var runtimeStatus by remember {
        mutableStateOf(HomeRuntimeStatusStore.cached)
    }
    var remoteStatus by remember {
        mutableStateOf(FeApplication.remoteStatus)
    }
    
    // 生命周期监听 - 从系统设置返回时自动刷新权限状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    val status = loadHomeRuntimeStatus(appContext, force = true)
                    HomeRuntimeStatusStore.update(status, currentModeForRefresh)
                    runtimeStatus = status
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    LaunchedEffect(mode) {
        val status = loadHomeRuntimeStatus(
            context = appContext,
            force = HomeRuntimeStatusStore.lastMode != mode
        )
        HomeRuntimeStatusStore.update(status, mode)
        runtimeStatus = status
    }

    LaunchedEffect(Unit) {
        FeApplication.remoteStatusFlow.collect { status ->
            remoteStatus = status
        }
    }
    
    val hasAllBasicPermissions = runtimeStatus.hasFilesPerm &&
        runtimeStatus.hasOverlayPerm &&
        runtimeStatus.hasAppListPerm
    
    val isTrulyActivated = when (mode) {
        ActivationMode.SHIZUKU ->
            shizukuState.isRunning && shizukuState.permissionGranted && hasAllBasicPermissions
        ActivationMode.ROOT ->
            hasAllBasicPermissions && runtimeStatus.hasRootAvailable
        ActivationMode.DIRECT_READ ->
            hasAllBasicPermissions && runtimeStatus.hasDirectReadAvailable
        ActivationMode.CLOUD ->
            hasAllBasicPermissions && runtimeStatus.cloudLoggedIn
        ActivationMode.DEFAULT -> false
    }

    if (LocalUiStyle.current == UiStyle.Miuix) {
        MiuixHomeDashboard(
            mode = mode,
            isTrulyActivated = isTrulyActivated,
            accessibilityActive = runtimeStatus.accessibilityActive,
            launchCount = launchCount,
            readFileCount = readFileCount,
            homeIconColor = homeIconColor,
            remoteStatus = remoteStatus,
            onNavigateToActivation = onNavigateToActivation,
        )
    } else {
        HomeDashboard(
            mode = mode,
            isTrulyActivated = isTrulyActivated,
            accessibilityActive = runtimeStatus.accessibilityActive,
            launchCount = launchCount,
            readFileCount = readFileCount,
            homeIconColor = homeIconColor,
            remoteStatus = remoteStatus,
            onNavigateToActivation = onNavigateToActivation,
        )
    }
}

@Composable
private fun HomeDashboard(
    mode: ActivationMode,
    isTrulyActivated: Boolean,
    accessibilityActive: Boolean,
    launchCount: Int,
    readFileCount: Int,
    homeIconColor: Int,
    remoteStatus: RemoteStatus?,
    onNavigateToActivation: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        HomeTopBar(homeIconColor = homeIconColor)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeActivationCell(
                    mode = mode,
                    isTrulyActivated = isTrulyActivated,
                    accessibilityActive = accessibilityActive,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    onClick = onNavigateToActivation
                )
                HomeCountsCell(
                    launchCount = launchCount,
                    readFileCount = readFileCount,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                HomeAnnouncementCell(
                    remoteStatus = remoteStatus,
                    modifier = Modifier.weight(2f).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(homeIconColor: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f),
        tonalElevation = 0.dp,
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(homeIconColor)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_eplus_foreground),
                    contentDescription = "E+ 应用图标",
                    colorFilter = ColorFilter.tint(homeIconContentColor(homeIconColor)),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun HomeActivationCell(
    mode: ActivationMode,
    isTrulyActivated: Boolean,
    accessibilityActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val activationText = when {
        isTrulyActivated -> "以${mode.activationDisplayName()}模式激活"
        mode == ActivationMode.DEFAULT -> "未激活"
        else -> "以${mode.activationDisplayName()}模式未激活"
    }

    HomeCell(modifier = modifier, onClick = onClick) {
        Text(
            text = activationText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isTrulyActivated) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        Text(
            text = if (accessibilityActive) {
                "无障碍权限已激活"
            } else {
                "无障碍权限未激活"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (accessibilityActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun HomeCountsCell(
    launchCount: Int,
    readFileCount: Int,
    modifier: Modifier = Modifier
) {
    HomeCell(modifier = modifier) {
        Text(
            text = "应用启动总次数=$launchCount",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "已读取文件数=$readFileCount",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun HomeAnnouncementCell(
    remoteStatus: RemoteStatus?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val announcementTitle = remoteStatus?.announcementTitle
        ?.takeIf { it.isNotBlank() }
        ?: "公告"
    val announcementMessage = remoteStatus?.announcementMessage
        ?.takeIf { it.isNotBlank() }
        ?: "暂无公告"

    HomeCell(
        modifier = modifier,
        onClick = {
            context.startActivity(
                RemoteContentActivity.createIntent(
                    context = context,
                    announcementTitle = announcementTitle,
                    announcementMessage = announcementMessage,
                    announcementUpdatedAt = remoteStatus?.announcementUpdatedAt.orEmpty(),
                    announcementUrl = remoteStatus?.announcementUrl.orEmpty(),
                    changelogTitle = remoteStatus?.changelogTitle.orEmpty(),
                    changelogSummary = remoteStatus?.changelogSummary.orEmpty(),
                    changelogUrl = remoteStatus?.changelogUrl.orEmpty()
                )
            )
        }
    ) {
        Text(
            text = announcementTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = announcementMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HomeCell(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = if (LocalUiStyle.current == UiStyle.Miuix) {
        RoundedCornerShape(26.dp)
    } else {
        RoundedCornerShape(18.dp)
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

internal fun ActivationMode.activationDisplayName(): String = when (this) {
    ActivationMode.SHIZUKU -> "Shizuku"
    ActivationMode.ROOT -> "Root"
    ActivationMode.DIRECT_READ -> "Direct Read"
    ActivationMode.CLOUD -> "云端"
    ActivationMode.DEFAULT -> title
}

private suspend fun loadHomeRuntimeStatus(
    context: android.content.Context,
    force: Boolean
): HomeRuntimeStatus {
    if (!force && HomeRuntimeStatusStore.isFresh()) {
        return HomeRuntimeStatusStore.cached
    }

    return withContext(Dispatchers.IO) {
        HomeRuntimeStatus(
            hasFilesPerm = PermissionsHelper.hasAllFilesAccess(),
            hasOverlayPerm = PermissionsHelper.hasOverlayPermission(context),
            hasAppListPerm = PermissionsHelper.hasAppListPermission(),
            hasRootAvailable = RootManager.isRootAvailable(),
            hasDirectReadAvailable = ZWCHelper.isDirectReadAvailable(),
            cloudLoggedIn = ETS100AuthManager.isLoggedIn(context),
            accessibilityActive = AutomationAccessibilityService.isConnectedAndEnabled(context)
        )
    }
}
