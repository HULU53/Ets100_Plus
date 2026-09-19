package com.hulu.etsplus.miuixcompat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator as MiuixCircularProgressIndicator
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator as MiuixLinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.Colors as MiuixColors
import top.yukonga.miuix.kmp.theme.LocalContentColor as MiuixLocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.TextStyles as MiuixTextStyles

object MaterialTheme {
    val colorScheme: ReadColorScheme
        @Composable get() = ReadColorScheme(MiuixTheme.colorScheme)

    val typography: ReadTypography
        @Composable get() = ReadTypography(MiuixTheme.textStyles)
}

class ReadColorScheme(colors: MiuixColors) {
    val primary = colors.primary
    val onPrimary = colors.onPrimary
    val primaryContainer = colors.primaryContainer
    val onPrimaryContainer = colors.onPrimaryContainer
    val secondary = colors.secondary
    val onSecondary = colors.onSecondary
    val secondaryContainer = colors.secondaryContainer
    val onSecondaryContainer = colors.onSecondaryContainer
    val tertiary = colors.tertiaryContainer
    val onTertiary = colors.onTertiaryContainer
    val error = colors.error
    val onError = colors.onError
    val errorContainer = colors.errorContainer
    val onErrorContainer = colors.onErrorContainer
    val background = colors.background
    val onBackground = colors.onBackground
    val surface = colors.surface
    val onSurface = colors.onSurface
    val surfaceVariant = colors.surfaceVariant
    val onSurfaceVariant = colors.onSurfaceVariantSummary
    val surfaceContainerLow = colors.surfaceContainerHigh
    val surfaceContainer = colors.surfaceContainer
    val surfaceContainerHigh = colors.surfaceContainerHigh
    val surfaceContainerHighest = colors.surfaceContainerHighest
    val outline = colors.outline
    val outlineVariant = colors.dividerLine
}

class ReadTypography(styles: MiuixTextStyles) {
    val titleLarge = styles.title1.copy(fontWeight = FontWeight.Bold)
    val titleMedium = styles.title3
    val titleSmall = styles.title4
    val bodyLarge = styles.body1
    val bodyMedium = styles.body1
    val bodySmall = styles.body2
    val labelLarge = styles.button
    val labelMedium = styles.footnote1
    val labelSmall = styles.footnote2
}

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.clip(shape).background(color),
        propagateMinConstraints = true,
    ) {
        CompositionLocalProvider(MiuixLocalContentColor provides contentColor) {
            content()
        }
    }
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    MiuixButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
}

data class ReadTextButtonColors(val contentColor: Color)

object ButtonDefaults {
    @Composable
    fun textButtonColors(contentColor: Color = MiuixTheme.colorScheme.primary): ReadTextButtonColors {
        return ReadTextButtonColors(contentColor)
    }
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ReadTextButtonColors = ButtonDefaults.textButtonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    CompositionLocalProvider(MiuixLocalContentColor provides colors.contentColor) {
        MiuixButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.buttonColors(
                color = Color.Transparent,
                contentColor = colors.contentColor,
            ),
            content = content,
        )
    }
}

data class ReadProgressIndicatorColors(val color: Color)

object ProgressIndicatorDefaults {
    @Composable
    fun colors(color: Color = MiuixTheme.colorScheme.primary): ReadProgressIndicatorColors {
        return ReadProgressIndicatorColors(color)
    }
}

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
    progress: (() -> Float)? = null,
) {
    MiuixCircularProgressIndicator(
        modifier = modifier,
        progress = progress?.invoke(),
        strokeWidth = strokeWidth,
        colors = top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults.progressIndicatorColors(
            foregroundColor = color,
        ),
    )
}

@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.primary,
    trackColor: Color = MiuixTheme.colorScheme.surfaceVariant,
    progress: (() -> Float)? = null,
) {
    MiuixLinearProgressIndicator(
        modifier = modifier,
        progress = progress?.invoke(),
        colors = top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults.progressIndicatorColors(
            foregroundColor = color,
            backgroundColor = trackColor,
        ),
    )
}

data class ReadFabElevation(val elevation: Dp)

object FloatingActionButtonDefaults {
    fun elevation(
        defaultElevation: Dp = 0.dp,
        pressedElevation: Dp = 0.dp,
        focusedElevation: Dp = 0.dp,
        hoveredElevation: Dp = 0.dp,
    ): ReadFabElevation = ReadFabElevation(defaultElevation)
}

@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MiuixTheme.colorScheme.primaryContainer,
    contentColor: Color = MiuixTheme.colorScheme.onPrimaryContainer,
    shape: Shape = RoundedCornerShape(50),
    elevation: ReadFabElevation = FloatingActionButtonDefaults.elevation(),
    content: @Composable () -> Unit,
) {
    MiuixSurface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = elevation.elevation,
        content = content,
    )
}

@Composable
fun SmallFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MiuixTheme.colorScheme.primaryContainer,
    contentColor: Color = MiuixTheme.colorScheme.onPrimaryContainer,
    shape: Shape = RoundedCornerShape(50),
    elevation: ReadFabElevation = FloatingActionButtonDefaults.elevation(),
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = shape,
        elevation = elevation,
        content = content,
    )
}

data class ReadTopAppBarColors(val containerColor: Color, val scrolledContainerColor: Color)
data class ReadScrollBehavior(val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {})

object TopAppBarDefaults {
    fun exitUntilCollapsedScrollBehavior(): ReadScrollBehavior = ReadScrollBehavior()

    @Composable
    fun topAppBarColors(
        containerColor: Color = MiuixTheme.colorScheme.background,
        scrolledContainerColor: Color = MiuixTheme.colorScheme.surface,
    ): ReadTopAppBarColors = ReadTopAppBarColors(containerColor, scrolledContainerColor)
}

@Composable
fun LargeTopAppBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: ReadTopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: ReadScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
) {
    Surface(color = colors.containerColor, shadowElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon()
            Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) { title() }
            actions()
        }
    }
}

@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        MiuixCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            insideMargin = PaddingValues(20.dp),
            colors = MiuixCardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.surfaceContainer,
                contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
            ),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                icon?.invoke()
                title?.invoke()
                text?.invoke()
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    confirmButton()
                    dismissButton?.invoke()
                }
            }
        }
    }
}

@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (expanded) {
        Dialog(onDismissRequest = onDismissRequest) {
            MiuixCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                insideMargin = PaddingValues(8.dp),
                colors = MiuixCardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.surfaceContainer,
                    contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
                ),
            ) {
                content()
            }
        }
    }
}

@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        leadingIcon?.invoke()
        text()
    }
}

@Composable
fun SingleChoiceSegmentedButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

data class SegmentedButtonColors(
    val activeContainerColor: Color,
    val activeContentColor: Color,
    val inactiveContainerColor: Color,
    val inactiveContentColor: Color,
    val disabledActiveContainerColor: Color,
    val disabledActiveContentColor: Color,
    val disabledInactiveContainerColor: Color,
    val disabledInactiveContentColor: Color,
)

object SegmentedButtonDefaults {
    fun itemShape(index: Int, count: Int): Shape = RoundedCornerShape(12.dp)

    fun colors(
        activeContainerColor: Color,
        activeContentColor: Color,
        inactiveContainerColor: Color,
        inactiveContentColor: Color,
        disabledActiveContainerColor: Color,
        disabledActiveContentColor: Color,
        disabledInactiveContainerColor: Color,
        disabledInactiveContentColor: Color,
    ) = SegmentedButtonColors(
        activeContainerColor,
        activeContentColor,
        inactiveContainerColor,
        inactiveContentColor,
        disabledActiveContainerColor,
        disabledActiveContentColor,
        disabledInactiveContainerColor,
        disabledInactiveContentColor,
    )
}

@Composable
fun RowScope.SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: SegmentedButtonColors = SegmentedButtonDefaults.colors(
        MiuixTheme.colorScheme.primaryContainer,
        MiuixTheme.colorScheme.onPrimaryContainer,
        MiuixTheme.colorScheme.surfaceContainer,
        MiuixTheme.colorScheme.onSurfaceContainer,
        MiuixTheme.colorScheme.primaryContainer,
        MiuixTheme.colorScheme.onPrimaryContainer,
        MiuixTheme.colorScheme.surfaceContainer,
        MiuixTheme.colorScheme.onSurfaceContainer,
    ),
    icon: @Composable () -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    MiuixSurface(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        shape = shape,
        color = if (selected) colors.activeContainerColor else colors.inactiveContainerColor,
        contentColor = if (selected) colors.activeContentColor else colors.inactiveContentColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            content()
        }
    }
}
@Composable
fun Badge(
    containerColor: Color,
    contentColor: Color,
    content: @Composable () -> Unit,
) {
    MiuixSurface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), contentAlignment = Alignment.Center) {
            content()
        }
    }
}
