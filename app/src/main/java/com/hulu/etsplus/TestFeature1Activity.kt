package com.hulu.etsplus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

open class TestFeature1Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applyPredictiveBackWindowTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeManager.init(this)

        setContent {
            var selectingTasks by remember {
                mutableStateOf(
                    intent.getBooleanExtra(EXTRA_SELECT_TASKS, false)
                )
            }
            FeTheme {
                PredictiveBackContent(
                    onBack = {
                        if (selectingTasks) {
                            selectingTasks = false
                        } else {
                            finish()
                        }
                    }
                ) {
                    if (selectingTasks) {
                        AutomationTaskSelectionScreen(
                            onBack = { selectingTasks = false },
                            onTasksConfirmed = { tasks ->
                                AutomationController.prepare(tasks)
                                selectingTasks = false
                            }
                        )
                    } else {
                        TestFeature1Screen(
                            onBack = { finish() },
                            onOpenTaskSelection = { selectingTasks = true }
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_SELECT_TASKS = "select_tasks"

        fun createIntent(context: Context, selectTasks: Boolean = false): Intent {
            return Intent(
                context,
                predictiveBackActivityClass(
                    TestFeature1Activity::class.java,
                    TestFeature1OpaqueActivity::class.java,
                    TestFeature1KernelSuClassicActivity::class.java
                )
            ).putExtra(EXTRA_SELECT_TASKS, selectTasks)
        }
    }
}
