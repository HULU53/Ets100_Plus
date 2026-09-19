package com.hulu.etsplus

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Executes the prepared task queue. Choice execution is completed in the OCR stage;
 * until then it reports a clear unsupported result instead of silently skipping.
 */
object AutomationOrchestrator {
    private val crashHandler = CoroutineExceptionHandler { _, throwable ->
        AutomationLog.crash(
            scope = "Orchestrator",
            message = "Uncaught orchestrator coroutine exception",
            throwable = throwable
        )
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + crashHandler
    )

    @Volatile
    private var sessionGeneration = 0L

    private var sessionJob: Job? = null

    @Synchronized
    fun start() {
        val tasks = AutomationController.snapshot.value.tasks
        AutomationLog.info("Orchestrator", "start requested tasks=${tasks.size}")
        if (tasks.isEmpty()) {
            AutomationLog.warn("Orchestrator", "start rejected, no tasks")
            AutomationController.fail("尚未选择答案")
            return
        }

        cancelCurrentWork(markStopped = false)
        AutomationController.start()
        val generation = ++sessionGeneration
        sessionJob = scope.launch {
            runSession(generation)
        }
    }

    @Synchronized
    fun stop() {
        AutomationLog.info("Orchestrator", "stop requested")
        cancelCurrentWork(markStopped = true)
    }

    @Synchronized
    fun previous() {
        val wasRunning = AutomationController.snapshot.value.state == AutomationSessionState.RUNNING
        AutomationLog.info("Orchestrator", "previous requested running=$wasRunning")
        cancelCurrentWork(markStopped = false)
        if (AutomationController.previous() && wasRunning) {
            start()
        }
    }

    @Synchronized
    fun next() {
        val wasRunning = AutomationController.snapshot.value.state == AutomationSessionState.RUNNING
        AutomationLog.info("Orchestrator", "next requested running=$wasRunning")
        cancelCurrentWork(markStopped = false)
        if (AutomationController.next() && wasRunning) {
            start()
        }
    }

    private fun cancelCurrentWork(markStopped: Boolean) {
        AutomationLog.debug("Orchestrator", "cancel current work markStopped=$markStopped")
        sessionGeneration++
        sessionJob?.cancel()
        sessionJob = null
        AutomationModuleRegistry.modules.speechOutput?.stop()
        if (markStopped) {
            AutomationController.stop()
        }
    }

    private suspend fun runSession(generation: Long) {
        while (scope.isActive && generation == sessionGeneration) {
            val snapshot = AutomationController.snapshot.value
            if (snapshot.state != AutomationSessionState.RUNNING) return
            val task = snapshot.currentTask ?: return
            AutomationLog.setTaskContext(
                task = task,
                index = snapshot.currentIndex,
                total = snapshot.tasks.size
            )
            AutomationLog.info(
                "Orchestrator",
                "task index=${snapshot.currentIndex} type=${task.type} title=${task.title.take(40)}"
            )

            if (!waitForRequiredModules(task, generation)) {
                if (generation == sessionGeneration) {
                    AutomationController.fail("执行模块未就绪")
                }
                return
            }

            val success = when (task.type) {
                AutomationTaskType.SPEECH -> executeSpeech(task)
                AutomationTaskType.CHOICE -> executeChoice(task)
            }

            if (!success) {
                AutomationLog.error("Orchestrator", "task failed type=${task.type}")
                if (generation == sessionGeneration) {
                    AutomationController.fail("任务执行失败：${task.questionText.take(40)}")
                }
                return
            }

            if (generation != sessionGeneration) return
            if (!AutomationController.next()) {
                AutomationLog.info("Orchestrator", "queue completed")
                AutomationController.stop()
                return
            }
            delay(300)
            AutomationController.ready("当前任务已完成，点击开始执行下一题")
            AutomationLog.info(
                "Orchestrator",
                "task completed, advanced without auto-start index=" +
                    AutomationController.snapshot.value.currentIndex
            )
            return
        }
    }

    private suspend fun executeSpeech(task: AutomationTask): Boolean {
        val speechOutput = AutomationModuleRegistry.modules.speechOutput
        if (speechOutput == null) {
            AutomationLog.error("Orchestrator", "speech module missing")
            AutomationController.updateMessage("朗读模块尚未接入")
            return false
        }
        AutomationController.updateMessage("正在朗读：${task.title}")
        val result = speechOutput.speak(task.selectedAnswer)
        AutomationLog.info("Orchestrator", "speech result=$result")
        return result
    }

    private suspend fun waitForRequiredModules(
        task: AutomationTask,
        generation: Long
    ): Boolean {
        repeat(25) {
            if (generation != sessionGeneration) return false
            val modules = AutomationModuleRegistry.modules
            val ready = when (task.type) {
                AutomationTaskType.SPEECH -> modules.speechOutput != null
                AutomationTaskType.CHOICE -> {
                    modules.captureSource != null &&
                        modules.textRecognitionSource != null &&
                        modules.actionExecutor != null
                }
            }
            if (ready) return true
            delay(100)
        }
        return false
    }

    private suspend fun executeChoice(task: AutomationTask): Boolean {
        val modules = AutomationModuleRegistry.modules
        val captureSource = modules.captureSource
        val recognizer = modules.textRecognitionSource
        val actionExecutor = modules.actionExecutor
        if (captureSource == null || recognizer == null || actionExecutor == null) {
            AutomationLog.error("Orchestrator", "choice modules missing")
            AutomationController.updateMessage("选择题执行模块尚未完整接入")
            return false
        }

        AutomationController.updateMessage("正在识别当前题目")
        val frame = captureSource.capture() ?: run {
            AutomationLog.error("Orchestrator", "capture returned null")
            AutomationController.updateMessage("无法截取当前屏幕")
            return false
        }
        val textBoxes = recognizer.recognize(frame)
        AutomationLog.info("Orchestrator", "recognized boxes=${textBoxes.size}")
        val target = OptionLocator.locate(
            task = task,
            textBoxes = textBoxes,
            screenWidth = frame.screenWidth,
            screenHeight = frame.screenHeight
        ) ?: run {
            AutomationLog.warn("Orchestrator", "option target not located")
            AutomationController.updateMessage("未定位到选项 ${task.choiceLabels.joinToString("/")}")
            return false
        }
        AutomationController.updateMessage(
            "点击选项 ${target.label}（匹配度 ${(target.score * 100).toInt()}%）"
        )
        val clicked = actionExecutor.tap(target.x, target.y)
        AutomationLog.info("Orchestrator", "tap result=$clicked target=${target.label}")
        if (!clicked) {
            AutomationController.updateMessage("点击执行失败")
        }
        return clicked
    }
}
