package com.hulu.etsplus

import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FallbackTapActionExecutor : AutomationActionExecutor {
    override suspend fun tap(x: Int, y: Int): Boolean = withContext(Dispatchers.IO) {
        AutomationLog.info("FallbackTap", "tap start x=$x y=$y")
        if (x < 0 || y < 0) {
            AutomationLog.error("FallbackTap", "rejected negative coordinate")
            return@withContext false
        }
        if (AccessibilityActionExecutor.tap(x, y)) {
            AutomationLog.info("FallbackTap", "accessibility success")
            return@withContext true
        }

        val command = "input tap $x $y; echo FE_TAP_OK"
        if (ShizukuManager.isShizukuRunning() &&
            ShizukuManager.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        ) {
            val output = runCatching { ShizukuManager.execCommand(command) }.getOrNull()
            if (output?.contains(TAP_MARKER) == true) {
                AutomationLog.info("FallbackTap", "Shizuku success")
                return@withContext true
            }
            AutomationLog.warn("FallbackTap", "Shizuku failed output=${output?.take(80)}")
        }

        if (RootManager.isRootAvailable()) {
            val output = runCatching { RootManager.execAsRoot(command) }.getOrNull()
            if (output?.contains(TAP_MARKER) == true) {
                AutomationLog.info("FallbackTap", "Root success")
                return@withContext true
            }
            AutomationLog.warn("FallbackTap", "Root failed output=${output?.take(80)}")
        }

        AutomationLog.error("FallbackTap", "all tap executors failed")
        false
    }

    private const val TAP_MARKER = "FE_TAP_OK"
}
