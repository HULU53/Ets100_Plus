package com.hulu.etsplus

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object AppCoroutineScope {
    private val crashHandler = CoroutineExceptionHandler { _, throwable ->
        AutomationLog.crash(
            scope = "Coroutine",
            message = "Uncaught application coroutine exception",
            throwable = throwable
        )
    }

    val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + crashHandler
    )
}