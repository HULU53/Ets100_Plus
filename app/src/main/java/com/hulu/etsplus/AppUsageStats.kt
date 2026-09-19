package com.hulu.etsplus

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

object AppUsageStats {
    private const val LAUNCH_COUNT_FILE = "cold_launch_count.txt"
    private const val READ_FILES_FILE = "read_files.json"

    private val lock = Any()
    private var appContext: Context? = null
    private val readFileIds = linkedSetOf<String>()

    private val _launchCount = MutableStateFlow(0)
    val launchCount: StateFlow<Int> = _launchCount.asStateFlow()

    private val _readFileCount = MutableStateFlow(0)
    val readFileCount: StateFlow<Int> = _readFileCount.asStateFlow()

    fun init(context: Context) {
        synchronized(lock) {
            if (appContext != null) return

            val applicationContext = context.applicationContext
            appContext = applicationContext
            _launchCount.value = readLaunchCount(applicationContext)
            readFileIds.clear()
            readFileIds.addAll(readFileIds(applicationContext))
            _readFileCount.value = readFileIds.size
        }
    }

    fun recordColdLaunch() {
        synchronized(lock) {
            val context = appContext ?: return
            val nextCount = (_launchCount.value.toLong() + 1L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            _launchCount.value = nextCount
            writeAtomically(
                File(context.filesDir, LAUNCH_COUNT_FILE),
                nextCount.toString()
            )
        }
    }

    fun recordReadFile(identifier: String) {
        if (identifier.isBlank()) return

        synchronized(lock) {
            val context = appContext ?: return
            if (!readFileIds.add(identifier)) return

            _readFileCount.value = readFileIds.size
            writeAtomically(
                File(context.filesDir, READ_FILES_FILE),
                JSONArray(readFileIds.toList()).toString()
            )
        }
    }

    private fun readLaunchCount(context: Context): Int {
        return runCatching {
            File(context.filesDir, LAUNCH_COUNT_FILE)
                .readText()
                .trim()
                .toLong()
                .coerceIn(0L, Int.MAX_VALUE.toLong())
                .toInt()
        }.getOrDefault(0)
    }

    private fun readFileIds(context: Context): Set<String> {
        val file = File(context.filesDir, READ_FILES_FILE)
        if (!file.isFile) return emptySet()

        return runCatching {
            val json = JSONArray(file.readText())
            buildSet {
                for (index in 0 until json.length()) {
                    json.optString(index)
                        .takeIf { it.isNotBlank() }
                        ?.let(::add)
                }
            }
        }.getOrDefault(emptySet())
    }

    private fun writeAtomically(file: File, content: String) {
        val temporaryFile = File(file.parentFile, "${file.name}.tmp")
        temporaryFile.writeText(content)

        if (file.exists() && !file.delete()) {
            temporaryFile.delete()
            return
        }

        if (!temporaryFile.renameTo(file)) {
            file.writeText(content)
            temporaryFile.delete()
        }
    }
}
