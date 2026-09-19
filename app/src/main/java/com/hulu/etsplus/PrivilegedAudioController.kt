package com.hulu.etsplus

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PrivilegedAudioController {
    private const val TAG = "PrivilegedAudioController"

    data class AudioRestoreState(
        val musicVolume: Int,
        val speakerphoneEnabled: Boolean,
        val audioMode: Int
    )

    suspend fun applyForSpeech(context: Context): AudioRestoreState? {
        val config = SettingsManager.getSpeechAudioConfig()
        if (!config.forceViaPrivilege) {
            AutomationLog.debug("Audio", "privileged enhancement disabled")
            return null
        }

        val audioManager = context.getSystemService(AudioManager::class.java)
        val restore = AudioRestoreState(
            musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
            speakerphoneEnabled = audioManager.isSpeakerphoneOn,
            audioMode = audioManager.mode
        )
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = (
            maxVolume * config.targetVolumePercent.coerceIn(
                SpeechAudioConfig.MIN_VOLUME_PERCENT,
                SpeechAudioConfig.MAX_VOLUME_PERCENT
            ) / 100
        ).coerceIn(0, maxVolume)

        runCatching {
            audioManager.mode = AudioManager.MODE_NORMAL
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = true
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        }.onFailure {
            AutomationLog.error("Audio", "AudioManager apply failed", it)
            Log.w(TAG, "无法通过 AudioManager 调整播放状态", it)
        }
        AutomationLog.info(
            "Audio",
            "apply volume=$targetVolume/$maxVolume, restore=$restore"
        )

        withContext(Dispatchers.IO) {
            val command = "cmd media_session volume --stream 3 --set $targetVolume"
            if (ShizukuManager.isShizukuRunning() &&
                ShizukuManager.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                runCatching { ShizukuManager.execCommand(command) }
                AutomationLog.debug("Audio", "Shizuku volume command dispatched")
            }
            if (RootManager.isRootAvailable()) {
                runCatching { RootManager.execAsRoot(command) }
                AutomationLog.debug("Audio", "Root volume command dispatched")
            }
        }
        return restore
    }

    fun restore(context: Context, state: AudioRestoreState?) {
        if (state == null) return
        AutomationLog.info("Audio", "restore volume=${state.musicVolume}")
        val audioManager = context.getSystemService(AudioManager::class.java)
        runCatching {
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                state.musicVolume,
                0
            )
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = state.speakerphoneEnabled
            }
            audioManager.mode = state.audioMode
        }.onFailure {
            AutomationLog.error("Audio", "restore failed", it)
            Log.w(TAG, "恢复音频状态失败", it)
        }
    }
}
