package com.hulu.etsplus

import android.graphics.Bitmap
import android.graphics.Rect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FloatingWindowEdge {
    NEAREST,
    LEFT,
    RIGHT
}

data class FloatingWindowConfig(
    val widthDp: Int = DEFAULT_WIDTH_DP,
    val heightDp: Int = DEFAULT_HEIGHT_DP,
    val marginDp: Int = DEFAULT_MARGIN_DP,
    val alpha: Float = DEFAULT_ALPHA,
    val edge: FloatingWindowEdge = FloatingWindowEdge.NEAREST,
    val lockDraggingWhileRunning: Boolean = true,
    val showDebugBounds: Boolean = false
) {
    companion object {
        const val MIN_WIDTH_DP = 140
        const val MAX_WIDTH_DP = 320
        const val DEFAULT_WIDTH_DP = 208

        const val MIN_HEIGHT_DP = 72
        const val MAX_HEIGHT_DP = 240
        const val DEFAULT_HEIGHT_DP = 112

        const val MIN_MARGIN_DP = 0
        const val MAX_MARGIN_DP = 32
        const val DEFAULT_MARGIN_DP = 8

        const val MIN_ALPHA = 0.4f
        const val MAX_ALPHA = 1f
        const val DEFAULT_ALPHA = 0.94f
    }
}

data class SpeechAudioConfig(
    val forceViaPrivilege: Boolean = false,
    val targetVolumePercent: Int = 100
) {
    companion object {
        const val MIN_VOLUME_PERCENT = 20
        const val MAX_VOLUME_PERCENT = 100
        const val DEFAULT_VOLUME_PERCENT = 100
    }
}

enum class AutomationTaskType {
    CHOICE,
    SPEECH
}

data class AutomationTask(
    val id: String,
    val type: AutomationTaskType,
    val title: String,
    val questionText: String,
    val selectedAnswer: String,
    val choiceLabels: List<String> = emptyList()
)

enum class AutomationSessionState {
    IDLE,
    READY,
    RUNNING,
    STOPPED,
    ERROR
}

data class AutomationSessionSnapshot(
    val state: AutomationSessionState = AutomationSessionState.IDLE,
    val tasks: List<AutomationTask> = emptyList(),
    val currentIndex: Int = 0,
    val message: String = ""
) {
    val currentTask: AutomationTask?
        get() = tasks.getOrNull(currentIndex)

    val progressText: String
        get() = if (tasks.isEmpty()) {
            "0 / 0"
        } else {
            "${currentIndex + 1} / ${tasks.size}"
        }
}

data class AutomationFrame(
    val bitmap: Bitmap,
    val timestampMs: Long,
    val screenWidth: Int,
    val screenHeight: Int
)

data class RecognizedTextBox(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val confidence: Float
)

interface CaptureSource {
    suspend fun capture(): AutomationFrame?
}

interface TextRecognitionSource {
    suspend fun recognize(frame: AutomationFrame): List<RecognizedTextBox>
}

interface AutomationActionExecutor {
    suspend fun tap(x: Int, y: Int): Boolean
}

interface SpeechOutput {
    suspend fun speak(text: String): Boolean
    fun stop()
}

data class AutomationModules(
    val captureSource: CaptureSource? = null,
    val textRecognitionSource: TextRecognitionSource? = null,
    val actionExecutor: AutomationActionExecutor? = null,
    val speechOutput: SpeechOutput? = null
)

/**
 * Module implementations are registered here after the concrete OCR, input and TTS
 * modules are selected. The foundation itself never creates platform implementations.
 */
object AutomationModuleRegistry {
    @Volatile
    private var registeredModules = AutomationModules()

    val modules: AutomationModules
        get() = registeredModules

    fun register(modules: AutomationModules) {
        registeredModules = modules
        AutomationLog.info(
            "ModuleRegistry",
            "register capture=${modules.captureSource != null}, " +
                "ocr=${modules.textRecognitionSource != null}, " +
                "action=${modules.actionExecutor != null}, " +
                "speech=${modules.speechOutput != null}"
        )
    }

    fun clear() {
        registeredModules = AutomationModules()
        AutomationLog.info("ModuleRegistry", "cleared")
    }
}

object AutomationOverlayState {
    @Volatile
    var bounds: Rect? = null

    fun clear() {
        bounds = null
    }
}

/**
 * Session state and queue foundation only. This object intentionally does not capture
 * the screen, click, synthesize speech or start background execution.
 */
object AutomationController {
    private val _snapshot = MutableStateFlow(AutomationSessionSnapshot())
    val snapshot: StateFlow<AutomationSessionSnapshot> = _snapshot.asStateFlow()

    @Synchronized
    fun prepare(tasks: List<AutomationTask>, startIndex: Int = 0) {
        val safeIndex = startIndex.coerceIn(0, (tasks.size - 1).coerceAtLeast(0))
        AutomationLog.beginSession(tasks.size)
        _snapshot.value = AutomationSessionSnapshot(
            state = if (tasks.isEmpty()) AutomationSessionState.IDLE else AutomationSessionState.READY,
            tasks = tasks,
            currentIndex = safeIndex
        )
        AutomationLog.info(
            "Controller",
            "prepare tasks=${tasks.size}, startIndex=$safeIndex"
        )
    }

    @Synchronized
    fun start() {
        val current = _snapshot.value
        _snapshot.value = if (current.tasks.isEmpty()) {
            current.copy(
                state = AutomationSessionState.ERROR,
                message = "No automation tasks"
            )
        } else {
            current.copy(
                state = AutomationSessionState.RUNNING,
                message = ""
            )
        }
        AutomationLog.info(
            "Controller",
            "start state=${_snapshot.value.state}, tasks=${current.tasks.size}"
        )
    }

    @Synchronized
    fun previous(): Boolean {
        val current = _snapshot.value
        if (current.currentIndex <= 0) return false
        _snapshot.value = current.copy(currentIndex = current.currentIndex - 1)
        AutomationLog.debug("Controller", "previous index=${_snapshot.value.currentIndex}")
        return true
    }

    @Synchronized
    fun next(): Boolean {
        val current = _snapshot.value
        if (current.currentIndex >= current.tasks.lastIndex) return false
        _snapshot.value = current.copy(currentIndex = current.currentIndex + 1)
        AutomationLog.debug("Controller", "next index=${_snapshot.value.currentIndex}")
        return true
    }

    @Synchronized
    fun stop() {
        _snapshot.value = _snapshot.value.copy(
            state = AutomationSessionState.STOPPED,
            message = "Stopped"
        )
        AutomationModuleRegistry.modules.speechOutput?.stop()
        AutomationLog.info("Controller", "stop")
    }

    @Synchronized
    fun ready(message: String = "等待开始") {
        _snapshot.value = _snapshot.value.copy(
            state = AutomationSessionState.READY,
            message = message
        )
        AutomationLog.info("Controller", "ready message=$message")
    }

    @Synchronized
    fun fail(message: String) {
        _snapshot.value = _snapshot.value.copy(
            state = AutomationSessionState.ERROR,
            message = message
        )
        AutomationLog.error("Controller", "fail message=$message")
    }

    @Synchronized
    fun updateMessage(message: String) {
        _snapshot.value = _snapshot.value.copy(message = message)
    }

    @Synchronized
    fun clear() {
        AutomationModuleRegistry.modules.speechOutput?.stop()
        _snapshot.value = AutomationSessionSnapshot()
        AutomationLog.info("Controller", "clear")
    }
}
