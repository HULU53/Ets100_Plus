package com.hulu.etsplus

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build

/**
 * Collects process exit information retained by Android after a crash, ANR, native
 * signal, or low-memory kill. The trace is written on the next process start.
 */
object AutomationExitInfoCollector {
    private const val MAX_TRACE_CHARS = 240_000
    private const val MAX_EXIT_RECORDS = 20

    fun collect(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        if (!SettingsManager.isAutomationLoggingEnabled()) return

        val activityManager = context.getSystemService(ActivityManager::class.java)
            ?: return
        val lastTimestamp = SettingsManager.getLastExitInfoTimestamp()
        val exitInfoList = runCatching {
            activityManager.getHistoricalProcessExitReasons(
                context.packageName,
                0,
                MAX_EXIT_RECORDS
            )
        }.getOrElse {
            AutomationLog.error(
                "ExitInfo",
                "failed to read historical process exit reasons",
                it
            )
            return
        }

        val newRecords = exitInfoList
            .filter { info -> info.timestamp > lastTimestamp }
            .sortedBy { info -> info.timestamp }

        var newestTimestamp = lastTimestamp
        newRecords.forEach { info ->
            val trace = readTrace(info)
            AutomationLog.crashTrace(
                scope = "ExitInfo",
                message = buildExitMessage(context, info),
                stackTrace = trace
            )
            newestTimestamp = maxOf(newestTimestamp, info.timestamp)
        }
        if (newestTimestamp > lastTimestamp) {
            SettingsManager.saveLastExitInfoTimestamp(newestTimestamp)
        }
    }

    private fun readTrace(info: ApplicationExitInfo): String {
        return runCatching {
            info.traceInputStream
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { reader -> reader.readText() }
                .orEmpty()
                .take(MAX_TRACE_CHARS)
        }.getOrElse { error ->
            "Failed to read exit trace: ${error::class.java.name}: ${error.message}"
        }
    }

    private fun buildExitMessage(
        context: Context,
        info: ApplicationExitInfo
    ): String {
        return buildString {
            append("Process exit captured")
            append(" | reason=").append(exitReasonName(info.reason))
            append("(").append(info.reason).append(")")
            append(" | description=").append(info.description.orEmpty())
            append(" | process=").append(info.processName.orEmpty())
            append(" | package=").append(context.packageName)
            append(" | timestamp=").append(info.timestamp)
            append(" | importance=").append(info.importance)
            append(" | pss=").append(info.pss)
            append(" | rss=").append(info.rss)
            append(" | sdk=").append(Build.VERSION.SDK_INT)
            append(" | manufacturer=").append(Build.MANUFACTURER)
            append(" | model=").append(Build.MODEL)
        }
    }

    private fun exitReasonName(reason: Int): String {
        return when (reason) {
            ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
            ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
            ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
            ApplicationExitInfo.REASON_CRASH -> "JAVA_CRASH"
            ApplicationExitInfo.REASON_CRASH_NATIVE -> "NATIVE_CRASH"
            ApplicationExitInfo.REASON_ANR -> "ANR"
            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
            ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION_CHANGE"
            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
            ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
            ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED"
            ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED"
            ApplicationExitInfo.REASON_OTHER -> "OTHER"
            else -> "UNKNOWN"
        }
    }
}
