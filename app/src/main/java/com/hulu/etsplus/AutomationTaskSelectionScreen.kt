package com.hulu.etsplus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class SelectableQuestion(
    val paper: ETS100AnswerReader.Paper,
    val question: ETS100AnswerReader.Question,
    val indexInPaper: Int,
    val key: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationTaskSelectionScreen(
    onBack: () -> Unit,
    onTasksConfirmed: (List<AutomationTask>) -> Unit
) {
    val context = LocalContext.current
    val papers = remember { AutomationTaskFactory.collectAvailablePapers(context) }
    val selectableQuestions = remember(papers) {
        papers.flatMap { paper ->
            paper.sections
                .flatMap { it.questions }
                .mapIndexedNotNull { index, question ->
                    if (question.answers.isEmpty()) {
                        null
                    } else {
                        SelectableQuestion(
                            paper = paper,
                            question = question,
                            indexInPaper = index,
                            key = "${paper.dataFileName}:${paper.paperId}:${question.order}:$index"
                        )
                    }
                }
        }
    }
    val selectedKeys = remember { mutableStateMapOf<String, Boolean>() }
    val selectedAnswers = remember { mutableStateMapOf<String, String>() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("选择本次答案", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val tasks = selectableQuestions.mapNotNull { item ->
                        if (selectedKeys[item.key] != true) return@mapNotNull null
                        val selectedAnswer = selectedAnswers[item.key]
                            ?: item.question.answers.firstOrNull()
                            ?: return@mapNotNull null
                        AutomationTaskFactory.createTask(
                            paper = item.paper,
                            question = item.question,
                            questionIndex = item.indexInPaper,
                            selectedAnswer = selectedAnswer
                        )
                    }
                    AutomationLog.info(
                        "TaskSelection",
                        "confirmed selectable=${selectableQuestions.size}, tasks=${tasks.size}"
                    )
                    onTasksConfirmed(tasks)
                },
                enabled = selectedKeys.values.any { it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("生成任务队列")
            }
        }
    ) { paddingValues ->
        if (selectableQuestions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "当前没有可选择的答案",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "请先在答题页读取本地或云端试卷",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = selectableQuestions,
                key = { it.key }
            ) { item ->
                val selected = selectedKeys[item.key] == true
                val answer = selectedAnswers[item.key] ?: item.question.answers.first()
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedKeys[item.key] = !selected
                        },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = {
                                    selectedKeys[item.key] = it
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.paper.title}  Q${item.question.displayOrder ?: item.indexInPaper + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = item.question.typeName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = item.question.question,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "选择答案版本",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        item.question.answers.forEach { candidate ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedKeys[item.key] = true
                                        selectedAnswers[item.key] = candidate
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                RadioButton(
                                    selected = answer == candidate,
                                    onClick = {
                                        selectedKeys[item.key] = true
                                        selectedAnswers[item.key] = candidate
                                    }
                                )
                                Text(
                                    text = candidate,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(top = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
