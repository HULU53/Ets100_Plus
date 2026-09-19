package com.hulu.etsplus

import android.os.Environment
import android.util.Log
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AutomationLogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

data class AutomationLogEntry(
    val timestampMs: Long,
    val level: AutomationLogLevel,
    val scope: String,
    val message: String,
    val sessionId: String?,
    val task: AutomationLogTaskInfo?,
    val stackTrace: String?
)

data class AutomationLogTaskInfo(
    val id: String,
    val index: Int,
    val total: Int,
    val type: AutomationTaskType,
    val title: String
)

object AutomationLog {
    private const val LOGCAT_TAG = "FeAutomation"
    private const val MAX_ENTRIES = 300
    private const val MAX_FILE_CHARS = 1_000_000
    private const val MAX_STACK_TRACE_CHARS = 120_000
    private const val FILE_NAME = "EplusDebug.txt"
    private val lock = Any()
    private val fileLock = Any()
    private val _entries = MutableStateFlow<List<AutomationLogEntry>>(emptyList())
    private val fileExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "FeAutomationLogWriter").apply {
            isDaemon = true
        }
    }
    private val fileTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())

    @Volatile
    private var sessionId: String? = null

    @Volatile
    private var currentTask: AutomationLogTaskInfo? = null

    val entries: StateFlow<List<AutomationLogEntry>> = _entries.asStateFlow()

    fun beginSession(taskCount: Int) {
        if (!isLoggingEnabled()) return
        sessionId = System.currentTimeMillis().toString()
        currentTask = null
        info("Session", "begin taskCount=$taskCount id=$sessionId")
    }

    fun setTaskContext(task: AutomationTask, index: Int, total: Int) {
        if (!isLoggingEnabled()) return
        currentTask = AutomationLogTaskInfo(
            id = task.id,
            index = index,
            total = total,
            type = task.type,
            title = task.title
        )
        info(
            "Session",
            "current task index=${index + 1}/$total id=${task.id} type=${task.type}"
        )
    }

    fun clearTaskContext() {
        currentTask = null
    }

    fun debug(scope: String, message: String) {
        if (!isLoggingEnabled()) return
        add(AutomationLogLevel.DEBUG, scope, message)
    }

    fun info(scope: String, message: String) {
        if (!isLoggingEnabled()) return
        add(AutomationLogLevel.INFO, scope, message)
    }

    fun warn(scope: String, message: String) {
        if (!isLoggingEnabled()) return
        add(AutomationLogLevel.WARN, scope, message)
    }

    fun error(scope: String, message: String, throwable: Throwable? = null) {
        if (!isLoggingEnabled()) return
        if (throwable != null) {
            val stackTrace = captureStackTrace(throwable)
            val entry = add(
                level = AutomationLogLevel.ERROR,
                scope = scope,
                message = message,
                stackTrace = stackTrace,
                writeLogcat = false,
                writeFile = false
            )
            Log.e(LOGCAT_TAG, "[$scope] $message", throwable)
            prependToFileBlocking(entry)
        } else {
            add(AutomationLogLevel.ERROR, scope, message)
        }
    }

    fun crash(scope: String, message: String, throwable: Throwable) {
        if (!isLoggingEnabled()) return
        val stackTrace = captureStackTrace(throwable)
        val entry = add(
            level = AutomationLogLevel.ERROR,
            scope = scope,
            message = message,
            stackTrace = stackTrace,
            writeLogcat = false,
            writeFile = false
        )
        Log.e(LOGCAT_TAG, "[$scope] $message", throwable)
        prependToFileBlocking(entry)
    }

    fun crashTrace(scope: String, message: String, stackTrace: String) {
        if (!isLoggingEnabled()) return
        val entry = add(
            level = AutomationLogLevel.ERROR,
            scope = scope,
            message = message,
            stackTrace = stackTrace.take(MAX_STACK_TRACE_CHARS),
            writeLogcat = false,
            writeFile = false
        )
        Log.e(LOGCAT_TAG, "[$scope] $message\n$stackTrace")
        prependToFileBlocking(entry)
    }
    fun clear() {
        synchronized(lock) {
            _entries.value = emptyList()
            sessionId = null
            currentTask = null
        }
        fileExecutor.execute {
            synchronized(fileLock) {
                runCatching {
                    resolveLogFile().delete()
                }.onFailure {
                    Log.e(LOGCAT_TAG, "Failed to clear $FILE_NAME", it)
                }
            }
        }
    }

    private fun add(
        level: AutomationLogLevel,
        scope: String,
        message: String,
        stackTrace: String? = null,
        writeLogcat: Boolean = true,
        writeFile: Boolean = true
    ): AutomationLogEntry {
        val taskSnapshot = currentTask
        val entry = AutomationLogEntry(
            timestampMs = System.currentTimeMillis(),
            level = level,
            scope = scope,
            message = message,
            sessionId = sessionId,
            task = taskSnapshot,
            stackTrace = stackTrace
        )
        synchronized(lock) {
            _entries.value = (_entries.value + entry).takeLast(MAX_ENTRIES)
        }
        val logcatMessage = "[$scope] $message"
        if (writeLogcat) {
            when (level) {
                AutomationLogLevel.DEBUG -> Log.d(LOGCAT_TAG, logcatMessage)
                AutomationLogLevel.INFO -> Log.i(LOGCAT_TAG, logcatMessage)
                AutomationLogLevel.WARN -> Log.w(LOGCAT_TAG, logcatMessage)
                AutomationLogLevel.ERROR -> Log.e(LOGCAT_TAG, logcatMessage)
            }
        }
        if (writeFile) {
            prependToFile(entry)
        }
        return entry
    }

    private fun prependToFile(entry: AutomationLogEntry) {
        fileExecutor.execute {
            prependToFileBlocking(entry)
        }
    }

    private fun prependToFileBlocking(entry: AutomationLogEntry) {
        synchronized(fileLock) {
            runCatching {
                val file = resolveLogFile()
                val existing = if (file.exists() && file.length() > 0L) {
                    file.readText(Charsets.UTF_8)
                } else {
                    ""
                }
                val block = formatFileBlock(entry)
                val combined = (block + existing).take(MAX_FILE_CHARS)
                file.writeText(combined, Charsets.UTF_8)
            }.onFailure {
                Log.e(LOGCAT_TAG, "Failed to prepend $FILE_NAME", it)
            }
        }
    }

    private fun formatFileBlock(entry: AutomationLogEntry): String {
        val time = fileTimeFormatter.format(Instant.ofEpochMilli(entry.timestampMs))
        val task = entry.task
        val taskText = if (task == null) {
            "task=none"
        } else {
            "task=${task.index + 1}/${task.total} " +
                "taskId=${task.id} " +
                "taskType=${task.type} " +
                "taskTitle=${task.title.replace('\n', ' ').take(80)}"
        }
        val header = "$time | ${entry.level} | ${entry.scope} | " +
            "session=${entry.sessionId ?: "none"} | $taskText | " +
            entry.message.replace('\n', ' ')
        return buildString {
            append(header)
            if (!entry.stackTrace.isNullOrBlank()) {
                append('\n')
                append(entry.stackTrace)
            }
            append('\n')
        }
    }

    private fun captureStackTrace(throwable: Throwable): String {
        return runCatching {
            throwable.stackTraceToString().take(MAX_STACK_TRACE_CHARS)
        }.getOrElse { fallback ->
            "Failed to format stack trace: ${fallback::class.java.name}: " +
                fallback.message.orEmpty()
        }
    }

    private fun isLoggingEnabled(): Boolean {
        return SettingsManager.isAutomationLoggingEnabled()
    }

    @Suppress("DEPRECATION")
    private fun resolveLogFile(): File {
        val downloads = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        if (!downloads.exists()) {
            downloads.mkdirs()
        }
        return File(downloads, FILE_NAME)
    }
}
