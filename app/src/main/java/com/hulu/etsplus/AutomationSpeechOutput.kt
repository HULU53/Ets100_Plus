package com.hulu.etsplus

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AutomationSpeechOutput(context: Context) : SpeechOutput {
    private val appContext = context.applicationContext
    private val initializationMutex = Mutex()

    @Volatile
    private var textToSpeech: TextToSpeech? = null

    @Volatile
    private var initialized = false

    @Volatile
    private var initializationFailed = false

    override suspend fun speak(text: String): Boolean {
        if (text.isBlank()) return false
        if (!ensureInitialized()) return false
        val tts = textToSpeech ?: return false
        AutomationLog.info("Speech", "speak start chars=${text.length}")
        val audioRestoreState = PrivilegedAudioController.applyForSpeech(appContext)

        return try {
            suspendCancellableCoroutine { continuation ->
                val completed = AtomicBoolean(false)
                val utteranceId = "fe_automation_${System.nanoTime()}"

                fun finish(result: Boolean) {
                    if (completed.compareAndSet(false, true) && continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                tts.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(utteranceId: String?) {
                        AutomationLog.info("Speech", "speak done id=$utteranceId")
                        finish(true)
                    }

                        @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        AutomationLog.error("Speech", "speak error id=$utteranceId")
                        finish(false)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        AutomationLog.error(
                            "Speech",
                            "speak error id=$utteranceId code=$errorCode"
                        )
                        finish(false)
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        AutomationLog.warn(
                            "Speech",
                            "speak stopped id=$utteranceId interrupted=$interrupted"
                        )
                        finish(false)
                        }
                    }
                )

                continuation.invokeOnCancellation {
                    tts.stop()
                }

                val params = Bundle().apply {
                    putInt(
                        TextToSpeech.Engine.KEY_PARAM_STREAM,
                        android.media.AudioManager.STREAM_MUSIC
                    )
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
                }
                val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                if (result == TextToSpeech.ERROR) {
                    finish(false)
                }
            }
        } finally {
            PrivilegedAudioController.restore(appContext, audioRestoreState)
        }
    }

    override fun stop() {
        AutomationLog.info("Speech", "stop requested")
        textToSpeech?.stop()
    }

    fun shutdown() {
        AutomationLog.info("Speech", "shutdown")
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        initialized = false
    }

    private suspend fun ensureInitialized(): Boolean {
        if (initialized) return true
        if (initializationFailed) return false

        return initializationMutex.withLock {
            if (initialized) return@withLock true
            if (initializationFailed) return@withLock false

            var engine: TextToSpeech? = null
            val initializedSuccessfully = suspendCancellableCoroutine { continuation ->
                engine = TextToSpeech(appContext) { status ->
                    if (continuation.isActive) {
                        continuation.resume(status == TextToSpeech.SUCCESS)
                    }
                }
            }

            val tts = engine
            if (!initializedSuccessfully || tts == null) {
                AutomationLog.error("Speech", "TTS initialization failed")
                initializationFailed = true
                tts?.shutdown()
                return@withLock false
            }

            val languageResult = tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
            if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                tts.setLanguage(Locale.getDefault())
            }
            tts.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            textToSpeech = tts
            initialized = true
            AutomationLog.info("Speech", "TTS initialized")
            true
        }
    }
}
