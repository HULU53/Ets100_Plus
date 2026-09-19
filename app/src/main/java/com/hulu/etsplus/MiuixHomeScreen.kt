package com.hulu.etsplus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import androidx.compose.ui.graphics.RectangleShape
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MiuixHomeDashboard(
    mode: ActivationMode,
    isTrulyActivated: Boolean,
    accessibilityActive: Boolean,
    launchCount: Int,
    readFileCount: Int,
    homeIconColor: Int,
    remoteStatus: RemoteStatus?,
    onNavigateToActivation: () -> Unit,
) {
    val blurEnabled = LocalBlurEnabled.current
    val backgroundColor = MiuixTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            Image(
                painter = painterResource(R.drawable.ic_eplus),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(280.dp)
                    .alpha(0.07f),
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(64.dp))

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 840.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MiuixActivationCard(
                        mode = mode,
                        isTrulyActivated = isTrulyActivated,
                        accessibilityActive = accessibilityActive,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        backdrop = backdrop,
                        blurEnabled = blurEnabled,
                        onClick = onNavigateToActivation,
                    )
                    MiuixCountsCard(
                        launchCount = launchCount,
                        readFileCount = readFileCount,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        backdrop = backdrop,
                        blurEnabled = blurEnabled,
                    )
                    MiuixAnnouncementCard(
                        remoteStatus = remoteStatus,
                        modifier = Modifier.weight(2f).fillMaxWidth(),
                        backdrop = backdrop,
                        blurEnabled = blurEnabled,
                    )
                }
            }
        }

        MiuixHomeTopBar(
            homeIconColor = homeIconColor,
            backdrop = backdrop,
            blurEnabled = blurEnabled,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
@Composable
private fun MiuixHomeTopBar(
    homeIconColor: Int,
    backdrop: Backdrop,
    blurEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = if (blurEnabled) {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = 24f,
                enabled = blurEnabled,
            )
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(homeIconColor)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_eplus_foreground),
                contentDescription = "E+ 应用图标",
                colorFilter = ColorFilter.tint(homeIconContentColor(homeIconColor)),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun MiuixActivationCard(
    mode: ActivationMode,
    isTrulyActivated: Boolean,
    accessibilityActive: Boolean,
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    blurEnabled: Boolean,
    onClick: () -> Unit,
) {
    val activationText = when {
        isTrulyActivated -> "以${mode.activationDisplayName()}模式激活"
        mode == ActivationMode.DEFAULT -> "未激活"
        else -> "以${mode.activationDisplayName()}模式未激活"
    }

    MiuixHomeCard(modifier = modifier, backdrop = backdrop, blurEnabled = blurEnabled, onClick = onClick) {
        Text(
            text = activationText,
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.SemiBold,
            color = if (isTrulyActivated) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
        )
        Text(
            text = if (accessibilityActive) "无障碍权限已激活" else "无障碍权限未激活",
            style = MiuixTheme.textStyles.body2,
            color = if (accessibilityActive) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun MiuixCountsCard(
    launchCount: Int,
    readFileCount: Int,
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    blurEnabled: Boolean,
) {
    MiuixHomeCard(modifier = modifier, backdrop = backdrop, blurEnabled = blurEnabled) {
        Text(
            text = "应用启动总次数=$launchCount",
            style = MiuixTheme.textStyles.title4,
        )
        Text(
            text = "已读取文件数=$readFileCount",
            style = MiuixTheme.textStyles.title4,
        )
    }
}

@Composable
private fun MiuixAnnouncementCard(
    remoteStatus: RemoteStatus?,
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    blurEnabled: Boolean,
) {
    val context = LocalContext.current
    val title = remoteStatus?.announcementTitle?.takeIf { it.isNotBlank() } ?: "公告"
    val message = remoteStatus?.announcementMessage?.takeIf { it.isNotBlank() } ?: "暂无公告"

    MiuixHomeCard(
        modifier = modifier,
        backdrop = backdrop,
        blurEnabled = blurEnabled,
        onClick = {
            context.startActivity(
                RemoteContentActivity.createIntent(
                    context = context,
                    announcementTitle = title,
                    announcementMessage = message,
                    announcementUpdatedAt = remoteStatus?.announcementUpdatedAt.orEmpty(),
                    announcementUrl = remoteStatus?.announcementUrl.orEmpty(),
                    changelogTitle = remoteStatus?.changelogTitle.orEmpty(),
                    changelogSummary = remoteStatus?.changelogSummary.orEmpty(),
                    changelogUrl = remoteStatus?.changelogUrl.orEmpty(),
                ),
            )
        },
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.title4,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = message,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun MiuixHomeCard(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    blurEnabled: Boolean,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.textureBlur(
            backdrop = backdrop,
            shape = RoundedCornerShape(20.dp),
            blurRadius = 24f,
            enabled = blurEnabled,
        ),
        cornerRadius = 20.dp,
        colors = CardDefaults.defaultColors(
            color = if (blurEnabled) {
                MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.76f)
            } else {
                MiuixTheme.colorScheme.surfaceContainer
            },
            contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
        ),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}