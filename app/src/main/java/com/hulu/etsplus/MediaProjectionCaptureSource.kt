package com.hulu.etsplus

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object MediaProjectionCaptureSource : CaptureSource {
    @Volatile
    private var mediaProjection: MediaProjection? = null

    @Volatile
    private var virtualDisplay: VirtualDisplay? = null

    @Volatile
    private var imageReader: ImageReader? = null

    private var width = 0
    private var height = 0
    private var densityDpi = 0
    private val captureRequested = AtomicBoolean(false)
    private val lastFrameConvertedAtMs = AtomicLong(0L)

    @Volatile
    private var pendingCapture: CancellableContinuation<AutomationFrame?>? = null

    fun isReady(): Boolean = mediaProjection != null && imageReader != null

    fun setPermission(context: Context, resultCode: Int, data: Intent?): Boolean {
        if (resultCode != android.app.Activity.RESULT_OK || data == null) {
            AutomationLog.warn(
                "MediaProjection",
                "permission denied resultCode=$resultCode data=${data != null}"
            )
            return false
        }
        release()

        return try {
            val manager = context.getSystemService(MediaProjectionManager::class.java)
            val projection = manager.getMediaProjection(resultCode, data) ?: return false
            val appContext = context.applicationContext
            val metrics = resolveDisplayMetrics(appContext)
            width = metrics.widthPixels
            height = metrics.heightPixels
            densityDpi = metrics.densityDpi

            val reader = ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                2
            )
            reader.setOnImageAvailableListener(
                { source ->
                    val image = source.acquireLatestImage()
                        ?: return@setOnImageAvailableListener
                    try {
                        if (!captureRequested.get()) {
                            return@setOnImageAvailableListener
                        }
                        val now = System.currentTimeMillis()
                        val previousFrameTime = lastFrameConvertedAtMs.get()
                        if (now - previousFrameTime < MIN_CAPTURE_INTERVAL_MS) {
                            return@setOnImageAvailableListener
                        }

                        val bitmap = imageToBitmap(image, width, height)
                            ?: return@setOnImageAvailableListener
                        captureRequested.set(false)
                        lastFrameConvertedAtMs.set(now)
                        val frame = AutomationFrame(
                            bitmap = maskOverlayBounds(bitmap),
                            timestampMs = now,
                            screenWidth = width,
                            screenHeight = height
                        )
                        AutomationLog.info(
                            "MediaProjection",
                            "single frame captured ${width}x$height"
                        )
                        val continuation = pendingCapture
                        pendingCapture = null
                        if (continuation?.isActive == true) {
                            continuation.resume(frame)
                        }
                    } finally {
                        image.close()
                    }
                },
                Handler(Looper.getMainLooper())
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                projection.registerCallback(
                    object : MediaProjection.Callback() {
                        override fun onStop() {
                            release()
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            }

            virtualDisplay = projection.createVirtualDisplay(
                "FeAutomationCapture",
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                null
            )
            mediaProjection = projection
            imageReader = reader
            AutomationLog.info(
                "MediaProjection",
                "capture ready ${width}x${height} density=$densityDpi"
            )
            true
        } catch (e: Exception) {
            AutomationLog.error("MediaProjection", "set permission failed", e)
            release()
            false
        }
    }

    override suspend fun capture(): AutomationFrame? {
        if (!isReady()) {
            AutomationLog.warn("MediaProjection", "capture requested before ready")
            return null
        }
        val elapsed = System.currentTimeMillis() - lastFrameConvertedAtMs.get()
        if (elapsed < MIN_CAPTURE_INTERVAL_MS) {
            delay(MIN_CAPTURE_INTERVAL_MS - elapsed)
        }
        if (pendingCapture != null) {
            AutomationLog.warn("MediaProjection", "capture ignored, another request active")
            return null
        }

        AutomationLog.info("MediaProjection", "single capture requested")
        return suspendCancellableCoroutine { continuation ->
            pendingCapture = continuation
            captureRequested.set(true)
            continuation.invokeOnCancellation {
                captureRequested.set(false)
                if (pendingCapture === continuation) {
                    pendingCapture = null
                }
            }
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    if (pendingCapture === continuation) {
                        pendingCapture = null
                        captureRequested.set(false)
                        if (continuation.isActive) {
                            AutomationLog.warn(
                                "MediaProjection",
                                "single capture timed out"
                            )
                            continuation.resume(null)
                        }
                    }
                },
                CAPTURE_TIMEOUT_MS
            )
        }
    }

    fun release() {
        AutomationLog.info("MediaProjection", "release")
        captureRequested.set(false)
        lastFrameConvertedAtMs.set(0L)
        val continuation = pendingCapture
        pendingCapture = null
        if (continuation?.isActive == true) {
            continuation.resume(null)
        }
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
        val projection = mediaProjection
        mediaProjection = null
        runCatching { projection?.stop() }
    }

    private fun imageToBitmap(image: Image, expectedWidth: Int, expectedHeight: Int): Bitmap? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * expectedWidth
        val paddedWidth = expectedWidth + rowPadding / pixelStride
        val padded = Bitmap.createBitmap(
            paddedWidth,
            expectedHeight,
            Bitmap.Config.ARGB_8888
        )
        buffer.rewind()
        padded.copyPixelsFromBuffer(buffer)
        if (paddedWidth == expectedWidth) return ensureMutable(padded)
        val cropped = Bitmap.createBitmap(padded, 0, 0, expectedWidth, expectedHeight)
        padded.recycle()
        return ensureMutable(cropped)
    }

    private fun maskOverlayBounds(bitmap: Bitmap): Bitmap {
        val bounds = AutomationOverlayState.bounds ?: return bitmap
        val mutableBitmap = ensureMutable(bitmap)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.drawRect(bounds, paint)
        return mutableBitmap
    }

    private fun ensureMutable(bitmap: Bitmap): Bitmap {
        return if (bitmap.isMutable) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: bitmap
        }
    }

    private fun resolveDisplayMetrics(context: Context): DisplayMetrics {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val manager = context.getSystemService(WindowManager::class.java)
            val bounds = manager.currentWindowMetrics.bounds
            return DisplayMetrics().apply {
                widthPixels = bounds.width()
                heightPixels = bounds.height()
                densityDpi = context.resources.displayMetrics.densityDpi
                density = context.resources.displayMetrics.density
            }
        }
        @Suppress("DEPRECATION")
        return context.resources.displayMetrics
    }

    private const val MIN_CAPTURE_INTERVAL_MS = 1_000L
    private const val CAPTURE_TIMEOUT_MS = 1_500L
}

object CompositeCaptureSource : CaptureSource {
    override suspend fun capture(): AutomationFrame? {
        AccessibilityCaptureSource.capture()?.let { return it }
        return MediaProjectionCaptureSource.capture()
    }
}
