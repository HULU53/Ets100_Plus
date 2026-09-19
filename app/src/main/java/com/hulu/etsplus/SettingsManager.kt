package com.hulu.etsplus

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 设置管理器
 * 负责保存和读取应用的各种设置选项喵~
 */
object SettingsManager {
    
    private const val PREFS_NAME = "fe_settings"
    private const val KEY_ACTIVATION_MODE = "activation_mode"
    private const val KEY_DEBUG_MODE = "debug_mode"  // 调试模式开关
    private const val KEY_FORCE_READ_MODE = "force_read_mode"  // 强执读取模式
    private const val KEY_HIDE_DEBUG_BUTTON = "hide_debug_button"  // 隐藏调试按钮
    private const val KEY_LEGAL_ACCEPTED = "legal_accepted"
    private const val KEY_AOSP_PREDICTIVE_BACK = "aosp_predictive_back"
    private const val KEY_PREDICTIVE_BACK_MODE = "predictive_back_mode"
    private const val KEY_FLOATING_WINDOW_WIDTH_DP = "floating_window_width_dp"
    private const val KEY_FLOATING_WINDOW_HEIGHT_DP = "floating_window_height_dp"
    private const val KEY_FLOATING_WINDOW_MARGIN_DP = "floating_window_margin_dp"
    private const val KEY_FLOATING_WINDOW_ALPHA = "floating_window_alpha"
    private const val KEY_FLOATING_WINDOW_EDGE = "floating_window_edge"
    private const val KEY_FLOATING_WINDOW_LOCK_DRAG = "floating_window_lock_drag"
    private const val KEY_FLOATING_WINDOW_DEBUG_BOUNDS = "floating_window_debug_bounds"
    private const val KEY_SPEECH_FORCE_VIA_PRIVILEGE = "speech_force_via_privilege"
    private const val KEY_SPEECH_TARGET_VOLUME = "speech_target_volume"
    private const val KEY_AUTOMATION_LOGGING_ENABLED = "automation_logging_enabled"
    private const val KEY_LAST_EXIT_INFO_TIMESTAMP = "last_exit_info_timestamp"
    
    private lateinit var prefs: SharedPreferences
    
    /**
     * 初始化设置管理器，在 Application 启动时调用
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 保存激活模式设置
     */
    fun saveActivationMode(mode: ActivationMode) {
        prefs.edit {
            putString(KEY_ACTIVATION_MODE, mode.name)
        }
    }
    
    /**
     * 读取保存的激活模式设置
     * 返回 null 表示用户尚未选择激活模式
     */
    fun getSavedActivationMode(): ActivationMode? {
        val modeName = prefs.getString(KEY_ACTIVATION_MODE, null) ?: return null
        return try {
            ActivationMode.valueOf(modeName)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
    
    /**
     * 检查用户是否已经选择了激活模式
     */
    fun hasUserSelectedMode(): Boolean {
        return prefs.contains(KEY_ACTIVATION_MODE)
    }
    
    /**
     * 清除保存的激活模式设置
     */
    fun clearActivationMode() {
        prefs.edit {
            remove(KEY_ACTIVATION_MODE)
        }
    }
    
    /**
     * 喵~ 保存调试模式开关（默认开启）
     */
    fun saveDebugMode(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_DEBUG_MODE, enabled)
        }
    }
    
    /**
     * 喵~ 获取调试模式开关（默认 true）
     */
    fun getDebugMode(): Boolean {
        return prefs.getBoolean(KEY_DEBUG_MODE, true)
    }
    
    /**
     * 喵~ 保存强执读取模式开关（默认关闭）
     */
    fun saveForceReadMode(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_FORCE_READ_MODE, enabled)
        }
    }
    
    /**
     * 喵~ 获取强执读取模式开关（默认 false）
     */
    fun getForceReadMode(): Boolean {
        return prefs.getBoolean(KEY_FORCE_READ_MODE, false)
    }
    
    /**
     * 保存隐藏调试按钮开关（默认 true - 默认隐藏调试按钮）
     */
    fun saveHideDebugButton(hide: Boolean) {
        prefs.edit {
            putBoolean(KEY_HIDE_DEBUG_BUTTON, hide)
        }
    }
    
    /**
     * 获取隐藏调试按钮开关（默认 true - 调试按钮默认隐藏）
     */
    fun getHideDebugButton(): Boolean {
        return prefs.getBoolean(KEY_HIDE_DEBUG_BUTTON, true)
    }

    fun saveLegalAccepted(accepted: Boolean) {
        prefs.edit {
            putBoolean(KEY_LEGAL_ACCEPTED, accepted)
        }
    }

    fun hasAcceptedLegal(): Boolean {
        return prefs.getBoolean(KEY_LEGAL_ACCEPTED, false)
    }

    fun savePredictiveBackMode(mode: PredictiveBackMode) {
        prefs.edit {
            putString(KEY_PREDICTIVE_BACK_MODE, mode.name)
            putBoolean(KEY_AOSP_PREDICTIVE_BACK, mode == PredictiveBackMode.AOSP)
        }
    }

    fun getPredictiveBackMode(): PredictiveBackMode {
        val modeName = prefs.getString(KEY_PREDICTIVE_BACK_MODE, null)
        if (modeName != null) {
            return runCatching { PredictiveBackMode.valueOf(modeName) }
                .getOrDefault(PredictiveBackMode.AOSP)
        }

        return if (prefs.getBoolean(KEY_AOSP_PREDICTIVE_BACK, true)) {
            PredictiveBackMode.AOSP
        } else {
            PredictiveBackMode.SLIDE
        }
    }

    fun saveAospPredictiveBackEnabled(enabled: Boolean) {
        savePredictiveBackMode(if (enabled) PredictiveBackMode.AOSP else PredictiveBackMode.SLIDE)
    }

    fun isAospPredictiveBackEnabled(): Boolean {
        return getPredictiveBackMode() == PredictiveBackMode.AOSP
    }

    fun saveFloatingWindowConfig(config: FloatingWindowConfig) {
        prefs.edit {
            putInt(KEY_FLOATING_WINDOW_WIDTH_DP, config.widthDp)
            putInt(KEY_FLOATING_WINDOW_HEIGHT_DP, config.heightDp)
            putInt(KEY_FLOATING_WINDOW_MARGIN_DP, config.marginDp)
            putFloat(KEY_FLOATING_WINDOW_ALPHA, config.alpha)
            putString(KEY_FLOATING_WINDOW_EDGE, config.edge.name)
            putBoolean(KEY_FLOATING_WINDOW_LOCK_DRAG, config.lockDraggingWhileRunning)
            putBoolean(KEY_FLOATING_WINDOW_DEBUG_BOUNDS, config.showDebugBounds)
        }
    }

    fun getFloatingWindowConfig(): FloatingWindowConfig {
        val default = FloatingWindowConfig()
        val edge = prefs.getString(KEY_FLOATING_WINDOW_EDGE, default.edge.name)
            ?.let { raw -> runCatching { FloatingWindowEdge.valueOf(raw) }.getOrNull() }
            ?: default.edge
        return FloatingWindowConfig(
            widthDp = prefs.getInt(KEY_FLOATING_WINDOW_WIDTH_DP, default.widthDp)
                .coerceIn(FloatingWindowConfig.MIN_WIDTH_DP, FloatingWindowConfig.MAX_WIDTH_DP),
            heightDp = prefs.getInt(KEY_FLOATING_WINDOW_HEIGHT_DP, default.heightDp)
                .coerceIn(FloatingWindowConfig.MIN_HEIGHT_DP, FloatingWindowConfig.MAX_HEIGHT_DP),
            marginDp = prefs.getInt(KEY_FLOATING_WINDOW_MARGIN_DP, default.marginDp)
                .coerceIn(FloatingWindowConfig.MIN_MARGIN_DP, FloatingWindowConfig.MAX_MARGIN_DP),
            alpha = prefs.getFloat(KEY_FLOATING_WINDOW_ALPHA, default.alpha)
                .coerceIn(FloatingWindowConfig.MIN_ALPHA, FloatingWindowConfig.MAX_ALPHA),
            edge = edge,
            lockDraggingWhileRunning = prefs.getBoolean(
                KEY_FLOATING_WINDOW_LOCK_DRAG,
                default.lockDraggingWhileRunning
            ),
            showDebugBounds = prefs.getBoolean(
                KEY_FLOATING_WINDOW_DEBUG_BOUNDS,
                default.showDebugBounds
            )
        )
    }

    fun saveSpeechAudioConfig(config: SpeechAudioConfig) {
        prefs.edit {
            putBoolean(KEY_SPEECH_FORCE_VIA_PRIVILEGE, config.forceViaPrivilege)
            putInt(
                KEY_SPEECH_TARGET_VOLUME,
                config.targetVolumePercent.coerceIn(
                    SpeechAudioConfig.MIN_VOLUME_PERCENT,
                    SpeechAudioConfig.MAX_VOLUME_PERCENT
                )
            )
        }
    }

    fun getSpeechAudioConfig(): SpeechAudioConfig {
        return SpeechAudioConfig(
            forceViaPrivilege = prefs.getBoolean(
                KEY_SPEECH_FORCE_VIA_PRIVILEGE,
                false
            ),
            targetVolumePercent = prefs.getInt(
                KEY_SPEECH_TARGET_VOLUME,
                SpeechAudioConfig.DEFAULT_VOLUME_PERCENT
            ).coerceIn(
                SpeechAudioConfig.MIN_VOLUME_PERCENT,
                SpeechAudioConfig.MAX_VOLUME_PERCENT
            )
        )
    }

    fun saveAutomationLoggingEnabled(enabled: Boolean) {
        if (::prefs.isInitialized) {
            prefs.edit {
                putBoolean(KEY_AUTOMATION_LOGGING_ENABLED, enabled)
            }
        }
    }

    fun isAutomationLoggingEnabled(): Boolean {
        return if (::prefs.isInitialized) {
            prefs.getBoolean(KEY_AUTOMATION_LOGGING_ENABLED, true)
        } else {
            true
        }
    }

    fun getLastExitInfoTimestamp(): Long {
        return if (::prefs.isInitialized) {
            prefs.getLong(KEY_LAST_EXIT_INFO_TIMESTAMP, 0L)
        } else {
            0L
        }
    }

    fun saveLastExitInfoTimestamp(timestamp: Long) {
        if (::prefs.isInitialized) {
            prefs.edit {
                putLong(KEY_LAST_EXIT_INFO_TIMESTAMP, timestamp)
            }
        }
    }
}

enum class PredictiveBackMode(
    val label: String,
    val description: String
) {
    AOSP("缩放", "使用缩放式预见性返回动画"),
    KERNELSU_CLASSIC("渐变", "使用渐变式预见性返回动画"),
    SLIDE("滑动", "使用系统自带预见性返回动画"),
    NONE("无", "关闭自定义预见性返回动画")
}
