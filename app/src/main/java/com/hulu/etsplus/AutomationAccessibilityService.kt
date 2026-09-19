package com.hulu.etsplus

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class AutomationAccessibilityService : AccessibilityService() {
    private val screenshotExecutor: Executor = Executors.newSingleThreadExecutor()

    override fun onServiceConnected() {
        activeInstance = this
        AutomationLog.info("Accessibility", "service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        AutomationLog.info("Accessibility", "service destroyed")
        if (activeInstance === this) {
            activeInstance = null
        }
        screenshotExecutor.let { executor ->
            (executor as? java.util.concurrent.ExecutorService)?.shutdown()
        }
        super.onDestroy()
    }

    suspend fun captureScreen(): AutomationFrame? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            AutomationLog.warn("Accessibility", "takeScreenshot unavailable below Android 11")
            return null
        }
        AutomationLog.debug("Accessibility", "screenshot requested")
        return suspendCancellableCoroutine { continuation ->
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                screenshotExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        AutomationLog.debug("Accessibility", "screenshot callback success")
                        val hardwareBuffer = screenshot.hardwareBuffer
                        val bitmap = try {
                            val hardwareBitmap = Bitmap.wrapHardwareBuffer(
                                hardwareBuffer,
                                screenshot.colorSpace
                            )
                            hardwareBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                        } catch (_: Exception) {
                            null
                        } finally {
                            hardwareBuffer.close()
                        }

                        val masked = bitmap?.let(::maskOverlayBounds)
                        if (bitmap != null && masked !== bitmap) {
                            bitmap.recycle()
                        }
                        AutomationLog.info(
                            "Accessibility",
                            "screenshot result bitmap=${bitmap?.width ?: 0}x${bitmap?.height ?: 0}"
                        )
                        if (continuation.isActive) {
                            continuation.resume(
                                masked?.let {
                                    AutomationFrame(
                                        bitmap = it,
                                        timestampMs = System.currentTimeMillis(),
                                        screenWidth = it.width,
                                        screenHeight = it.height
                                    )
                                }
                            )
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        AutomationLog.error(
                            "Accessibility",
                            "screenshot failed code=$errorCode"
                        )
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }
            )
        }
    }

    fun readScreenText(): List<RecognizedTextBox> {
        val root = rootInActiveWindow ?: return emptyList()
        val output = mutableListOf<RecognizedTextBox>()
        collectNodeText(root, output, 0)
        AutomationLog.debug("Accessibility", "node text boxes=${output.size}")
        return output
    }

    suspend fun tapScreen(x: Int, y: Int): Boolean = withContext(Dispatchers.Main.immediate) {
        AutomationLog.info("Accessibility", "tap requested x=$x y=$y")
        if (!isValidScreenPoint(x, y)) {
            AutomationLog.error(
                "Accessibility",
                "tap rejected invalid screen point x=$x y=$y"
            )
            return@withContext false
        }
        val clickableNode = findClickableNodeAt(x, y)
        if (clickableNode != null && clickableNode.isEnabled) {
            val clicked = clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            val bounds = Rect().also(clickableNode::getBoundsInScreen)
            AutomationLog.info(
                "Accessibility",
                "node click result=$clicked class=${clickableNode.className} bounds=$bounds"
            )
            if (clicked) return@withContext true
        } else {
            AutomationLog.debug("Accessibility", "no clickable node at target coordinate")
        }

        dispatchTapGesture(x, y)
    }

    private suspend fun dispatchTapGesture(x: Int, y: Int): Boolean {
        if (!isValidScreenPoint(x, y)) {
            AutomationLog.error(
                "Accessibility",
                "gesture rejected invalid screen point x=$x y=$y"
            )
            return false
        }
        val gesture = runCatching {
            val path = Path().apply {
                moveTo(x.toFloat(), y.toFloat())
            }
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, 60L))
                .build()
        }.onFailure {
            AutomationLog.error(
                "Accessibility",
                "gesture construction failed x=$x y=$y",
                it
            )
        }.getOrNull() ?: return false
        return suspendCancellableCoroutine { continuation ->
            val callback = object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) {
                        AutomationLog.info("Accessibility", "gesture completed")
                        continuation.resume(true)
                    }
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) {
                        AutomationLog.warn("Accessibility", "gesture cancelled")
                        continuation.resume(false)
                    }
                }
            }
            val dispatched = dispatchGesture(gesture, callback, null)
            AutomationLog.info("Accessibility", "gesture dispatched=$dispatched")
            if (!dispatched && continuation.isActive) {
                continuation.resume(false)
            }
        }
    }

    private fun findClickableNodeAt(x: Int, y: Int): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        var bestMatch: AccessibilityNodeInfo? = null
        var bestArea = Int.MAX_VALUE

        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 40) return
            if (node.isVisibleToUser) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                if (bounds.contains(x, y) && node.isClickable && node.isEnabled) {
                    val area = bounds.width().coerceAtLeast(0) * bounds.height().coerceAtLeast(0)
                    if (area in 1 until bestArea) {
                        bestArea = area
                        bestMatch = node
                    }
                }
            }
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                visit(child, depth + 1)
            }
        }

        visit(root, 0)
        return bestMatch
    }

    private fun isValidScreenPoint(x: Int, y: Int): Boolean {
        val metrics = resources.displayMetrics
        return x >= 0 &&
            y >= 0 &&
            x < metrics.widthPixels &&
            y < metrics.heightPixels
    }

    private fun collectNodeText(
        node: AccessibilityNodeInfo,
        output: MutableList<RecognizedTextBox>,
        depth: Int
    ) {
        if (depth > 40 || output.size >= 500) return
        if (node.isVisibleToUser) {
            val text = buildString {
                append(node.text?.toString().orEmpty())
                if (isNotEmpty() && !node.contentDescription.isNullOrBlank()) {
                    append(' ')
                }
                append(node.contentDescription?.toString().orEmpty())
            }.trim()
            if (text.isNotEmpty()) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                val overlayBounds = AutomationOverlayState.bounds
                val coveredByOverlay = overlayBounds != null &&
                    Rect.intersects(bounds, overlayBounds)
                if (!bounds.isEmpty && !coveredByOverlay) {
                    output.add(
                        RecognizedTextBox(
                            text = text,
                            left = bounds.left,
                            top = bounds.top,
                            right = bounds.right,
                            bottom = bounds.bottom,
                            confidence = 1f
                        )
                    )
                }
            }
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            collectNodeText(child, output, depth + 1)
        }
    }

    private fun maskOverlayBounds(bitmap: Bitmap): Bitmap {
        val bounds = AutomationOverlayState.bounds ?: return bitmap
        val mutableBitmap = if (bitmap.isMutable) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: return bitmap
        }
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.drawRect(bounds, paint)
        return mutableBitmap
    }

    companion object {
        @Volatile
        private var activeInstance: AutomationAccessibilityService? = null

        val instance: AutomationAccessibilityService?
            get() = activeInstance

        fun isEnabled(context: Context): Boolean {
            val expectedComponent = ComponentName(context, AutomationAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ).orEmpty()
            return enabledServices.split(':')
                .mapNotNull { ComponentName.unflattenFromString(it) }
                .any { it == expectedComponent }
        }

        fun isConnectedAndEnabled(context: Context): Boolean {
            return isEnabled(context) && activeInstance != null
        }

        fun openSettings(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

object AccessibilityCaptureSource : CaptureSource {
    override suspend fun capture(): AutomationFrame? {
        return AutomationAccessibilityService.instance?.captureScreen()
    }
}

object AccessibilityNodeTextSource : TextRecognitionSource {
    override suspend fun recognize(frame: AutomationFrame): List<RecognizedTextBox> {
        return AutomationAccessibilityService.instance?.readScreenText().orEmpty()
    }
}

object AccessibilityActionExecutor : AutomationActionExecutor {
    override suspend fun tap(x: Int, y: Int): Boolean {
        return AutomationAccessibilityService.instance?.tapScreen(x, y) == true
    }
}
