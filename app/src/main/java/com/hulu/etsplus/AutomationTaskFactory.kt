package com.hulu.etsplus

import android.content.Context

object AutomationTaskFactory {
    private val choiceAnswerPattern = Regex("""正确答案\s*[:：]\s*([A-Za-z])""")
    private val optionLinePattern = Regex("""(?m)^\s*([A-Za-z])[.、．)]\s*(.+?)\s*$""")

    fun collectAvailablePapers(context: Context): List<ETS100AnswerReader.Paper> {
        val local = ReadPageStateStore.loadLocal(context)?.papers.orEmpty()
        val cloud = ReadPageStateStore.loadCloud(context)
            ?.downloadedPapers
            ?.values
            ?.flatten()
            .orEmpty()
        val papers = (local + cloud)
            .distinctBy { paper ->
                "${paper.paperId}:${paper.title}:${paper.dataFileName}"
            }
        AutomationLog.info(
            "TaskFactory",
            "collect papers local=${local.size}, cloud=${cloud.size}, merged=${papers.size}"
        )
        return papers
    }

    fun createTasksForPaper(paper: ETS100AnswerReader.Paper): List<AutomationTask> {
        val tasks = paper.sections
            .flatMap { it.questions }
            .mapIndexedNotNull { index, question ->
                val selectedAnswer = question.answers.firstOrNull() ?: return@mapIndexedNotNull null
                createTask(
                    paper = paper,
                    question = question,
                    questionIndex = index,
                    selectedAnswer = selectedAnswer
                )
            }
        AutomationLog.info(
            "TaskFactory",
            "createTasksForPaper title=${paper.title}, tasks=${tasks.size}, paperId=${paper.paperId}"
        )
        return tasks
    }
    fun createTask(
        paper: ETS100AnswerReader.Paper,
        question: ETS100AnswerReader.Question,
        questionIndex: Int,
        selectedAnswer: String
    ): AutomationTask {
        val taskId = buildString {
            append(paper.dataFileName)
            append(':')
            append(paper.paperId)
            append(':')
            append(question.displayOrder ?: question.order)
            append(':')
            append(questionIndex)
        }
        val task = AutomationTask(
            id = taskId,
            type = resolveTaskType(question),
            title = paper.title,
            questionText = question.question,
            selectedAnswer = selectedAnswer,
            choiceLabels = extractChoiceLabels(selectedAnswer)
        )
        AutomationLog.debug(
            "TaskFactory",
            "task id=$taskId type=${task.type} labels=${task.choiceLabels.joinToString(",")}"
        )
        return task
    }

    fun resolveTaskType(question: ETS100AnswerReader.Question): AutomationTaskType {
        val answer = question.answers.joinToString("\n")
        val isChoice = choiceAnswerPattern.containsMatchIn(answer) ||
            question.typeName.contains("选择")
        return if (isChoice) AutomationTaskType.CHOICE else AutomationTaskType.SPEECH
    }

    fun extractChoiceLabels(answer: String): List<String> {
        return optionLinePattern.findAll(answer)
            .map { it.groupValues[1].uppercase() }
            .distinct()
            .toList()
    }
}
