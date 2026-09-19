package com.hulu.etsplus

import android.os.Build

object AutomationCrashHandler {
    @Volatile
    private var installed = false

    fun install() {
        if (installed) return
        installed = true
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                AutomationLog.crash(
                    scope = "Crash",
                    message = buildCrashContext(thread),
                    throwable = throwable
                )
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun buildCrashContext(thread: Thread): String {
        return buildString {
            append("Uncaught exception")
            append(" | thread=").append(thread.name)
            append(" | process=").append(BuildConfig.APPLICATION_ID)
            append(" | version=").append(BuildConfig.VERSION_NAME)
            append("(").append(BuildConfig.VERSION_CODE).append(")")
            append(" | android=").append(Build.VERSION.RELEASE)
            append(" | sdk=").append(Build.VERSION.SDK_INT)
            append(" | manufacturer=").append(Build.MANUFACTURER)
            append(" | model=").append(Build.MODEL)
            append(" | abi=").append(Build.SUPPORTED_ABIS.joinToString(","))
        }
    }
}
