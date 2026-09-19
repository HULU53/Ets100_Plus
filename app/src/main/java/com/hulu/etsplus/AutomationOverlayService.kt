package com.hulu.etsplus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AutomationOverlayService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var stateJob: Job? = null
    private var speechOutput: AutomationSpeechOutput? = null

    private var progressText: TextView? = null
    private var statusText: TextView? = null
    private var questionText: TextView? = null
    private var dragHandle: TextView? = null

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        AutomationLog.info("OverlayService", "onCreate")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val speech = AutomationSpeechOutput(this)
        speechOutput = speech
        AutomationModuleRegistry.register(
            AutomationModuleRegistry.modules.copy(
                captureSource = CompositeCaptureSource,
                textRecognitionSource = HybridTextRecognitionSource,
                actionExecutor = FallbackTapActionExecutor,
                speechOutput = speech
            )
        )
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AutomationLog.info("OverlayService", "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            ACTION_HIDE -> {
                hideOverlay()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SHOW -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                showOverlay()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AutomationLog.info(
            "OverlayService",
            "configuration changed ${newConfig.screenWidthDp}x${newConfig.screenHeightDp}dp"
        )
        snapToEdge()
    }

    override fun onDestroy() {
        AutomationLog.info("OverlayService", "onDestroy")
        stateJob?.cancel()
        MediaProjectionCaptureSource.release()
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        layoutParams = null
        AutomationOverlayState.clear()
        AutomationModuleRegistry.register(
            AutomationModuleRegistry.modules.copy(
                captureSource = null,
                textRecognitionSource = null,
                actionExecutor = null,
                speechOutput = null
            )
        )
        speechOutput?.shutdown()
        speechOutput = null
        serviceScope.cancel()
        if (activeInstance === this) {
            activeInstance = null
        }
        super.onDestroy()
    }

    private fun showOverlay() {
        if (overlayView != null) {
            AutomationLog.debug("OverlayService", "show ignored, already visible")
            snapToEdge()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            AutomationLog.error("OverlayService", "overlay permission missing")
            AutomationController.fail("未授予悬浮窗权限")
            stopSelf()
            return
        }

        val config = SettingsManager.getFloatingWindowConfig()
        val root = buildOverlayView(config)
        val params = WindowManager.LayoutParams(
            dp(config.widthDp),
            dp(config.heightDp),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX(config, dp(config.widthDp))
            y = dp(96)
        }

        overlayView = root
        layoutParams = params
        windowManager.addView(root, params)
        AutomationLog.info(
            "OverlayService",
            "overlay added size=${params.width}x${params.height}, x=${params.x}, y=${params.y}"
        )
        root.post {
            updateOverlayBounds()
            snapToEdge()
        }
        observeState()
    }

    private fun hideOverlay() {
        AutomationLog.info("OverlayService", "hide overlay")
        stateJob?.cancel()
        stateJob = null
        MediaProjectionCaptureSource.release()
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        layoutParams = null
        AutomationOverlayState.clear()
    }

    private fun buildOverlayView(config: FloatingWindowConfig): View {
        val padding = dp(10)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            alpha = config.alpha
            background = GradientDrawable().apply {
                setColor(Color.argb(235, 25, 29, 36))
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.argb(120, 255, 255, 255))
            }
            if (config.showDebugBounds) {
                setBackgroundColor(Color.argb(80, 255, 196, 0))
            }
        }

        dragHandle = TextView(this).apply {
            text = "悬浮窗控制台"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(28)
            )
        }
        progressText = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.WHITE)
        }
        statusText = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.LTGRAY)
            maxLines = 1
        }
        questionText = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            maxLines = 2
        }

        val controlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        controlRow.addView(createButton("开始") { AutomationOrchestrator.start() })
        controlRow.addView(createButton("上一题") { AutomationOrchestrator.previous() })
        controlRow.addView(createButton("停止") { AutomationOrchestrator.stop() })
        controlRow.addView(createButton("下一题") { AutomationOrchestrator.next() })

        root.addView(dragHandle)
        root.addView(progressText)
        root.addView(statusText)
        root.addView(questionText)
        root.addView(controlRow)
        configureDragging(root, dragHandle!!)
        return root
    }

    private fun createButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 11f
            minWidth = 0
            minimumWidth = 0
            setPadding(dp(6), 0, dp(6), 0)
            layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f)
            setOnClickListener { action() }
        }
    }

    private fun configureDragging(root: View, handle: View) {
        var startX = 0
        var startY = 0
        var downRawX = 0f
        var downRawY = 0f
        handle.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            val running = AutomationController.snapshot.value.state == AutomationSessionState.RUNNING
            val config = SettingsManager.getFloatingWindowConfig()
            if (running && config.lockDraggingWhileRunning) {
                return@setOnTouchListener false
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    downRawX = event.rawX
                    downRawY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - downRawX).toInt()
                    params.y = startY + (event.rawY - downRawY).toInt()
                    windowManager.updateViewLayout(root, params)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    snapToEdge()
                    true
                }
                else -> false
            }
        }
    }

    private fun observeState() {
        stateJob?.cancel()
        stateJob = serviceScope.launch {
            AutomationController.snapshot.collectLatest { snapshot ->
                progressText?.text = "进度 ${snapshot.progressText}"
                statusText?.text = when (snapshot.state) {
                    AutomationSessionState.IDLE -> snapshot.message.ifBlank { "等待开始" }
                    AutomationSessionState.READY -> "已就绪"
                    AutomationSessionState.RUNNING -> snapshot.message.ifBlank { "运行中" }
                    AutomationSessionState.STOPPED -> "已停止"
                    AutomationSessionState.ERROR -> snapshot.message.ifBlank { "执行失败" }
                }
                questionText?.text = snapshot.currentTask?.questionText.orEmpty()
                updateOverlayBounds()
            }
        }
    }

    private fun snapToEdge() {
        val root = overlayView ?: return
        val params = layoutParams ?: return
        val config = SettingsManager.getFloatingWindowConfig()
        val margin = dp(config.marginDp)
        val maxX = (resources.displayMetrics.widthPixels - params.width - margin)
            .coerceAtLeast(margin)
        val leftDistance = abs(params.x - margin)
        val rightDistance = abs(params.x - maxX)
        params.x = when (config.edge) {
            FloatingWindowEdge.LEFT -> margin
            FloatingWindowEdge.RIGHT -> maxX
            FloatingWindowEdge.NEAREST -> if (leftDistance <= rightDistance) margin else maxX
        }
        params.y = params.y.coerceIn(margin, resources.displayMetrics.heightPixels - params.height - margin)
        windowManager.updateViewLayout(root, params)
        AutomationLog.debug(
            "OverlayService",
            "snap edge=${config.edge}, x=${params.x}, y=${params.y}"
        )
        root.post { updateOverlayBounds() }
    }

    private fun updateOverlayBounds() {
        val root = overlayView ?: return
        if (root.width <= 0 || root.height <= 0) return
        val location = IntArray(2)
        root.getLocationOnScreen(location)
        AutomationOverlayState.bounds = Rect(
            location[0],
            location[1],
            location[0] + root.width,
            location[1] + root.height
        )
    }

    private fun initialX(config: FloatingWindowConfig, widthPx: Int): Int {
        val margin = dp(config.marginDp)
        return when (config.edge) {
            FloatingWindowEdge.RIGHT -> resources.displayMetrics.widthPixels - widthPx - margin
            FloatingWindowEdge.LEFT, FloatingWindowEdge.NEAREST -> margin
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "自动化悬浮窗",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            TestFeature1Activity.createIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_fe_logo)
            .setContentTitle("E+ 自动化悬浮窗")
            .setContentText("悬浮窗正在运行")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val CHANNEL_ID = "fe_automation_overlay"
        private const val NOTIFICATION_ID = 4101
        private const val ACTION_SHOW = "com.hulu.etsplus.action.SHOW_AUTOMATION_OVERLAY"
        private const val ACTION_HIDE = "com.hulu.etsplus.action.HIDE_AUTOMATION_OVERLAY"

        @Volatile
        private var activeInstance: AutomationOverlayService? = null

        fun show(context: Context) {
            AutomationLog.info("OverlayService", "request show")
            val intent = Intent(context, AutomationOverlayService::class.java)
                .setAction(ACTION_SHOW)
            ContextCompat.startForegroundService(context, intent)
        }

        fun hide(context: Context) {
            AutomationLog.info("OverlayService", "request hide")
            val intent = Intent(context, AutomationOverlayService::class.java)
                .setAction(ACTION_HIDE)
            context.startService(intent)
        }

        fun promoteForMediaProjection(): Boolean {
            val service = activeInstance ?: return false
            AutomationLog.info("OverlayService", "promote mediaProjection foreground type")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                service.startForeground(
                    NOTIFICATION_ID,
                    service.buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                service.startForeground(NOTIFICATION_ID, service.buildNotification())
            }
            return true
        }
    }
}
