package com.hulu.etsplus

import kotlin.math.abs
import kotlin.math.max

data class LocatedOption(
    val x: Int,
    val y: Int,
    val score: Float,
    val label: String,
    val matchedText: String
)

private data class ScoredTextBox(
    val box: RecognizedTextBox,
    val score: Float
)

object OptionLocator {
    private val correctAnswerPattern = Regex("""正确答案\s*[:：]\s*([A-Za-z])""")
    private val optionPattern = Regex("""(?m)^\s*([A-Za-z])[.、．)]\s*(.+?)\s*$""")
    private val punctuationPattern = Regex("""[\s\p{Punct}，。！？、：；（）【】《》“”‘’]+""")

    fun locate(
        task: AutomationTask,
        textBoxes: List<RecognizedTextBox>,
        screenWidth: Int,
        screenHeight: Int
    ): LocatedOption? {
        if (textBoxes.isEmpty()) {
            AutomationLog.warn("OptionLocator", "no text boxes")
            return null
        }
        val validBoxes = textBoxes.filter { box ->
            isValidBox(box, screenWidth, screenHeight)
        }
        if (validBoxes.isEmpty()) {
            AutomationLog.warn(
                "OptionLocator",
                "no valid on-screen boxes screen=${screenWidth}x$screenHeight"
            )
            return null
        }
        if (!matchesCurrentQuestion(task.questionText, validBoxes)) {
            AutomationLog.warn(
                "OptionLocator",
                "question mismatch, click aborted question=${task.questionText.take(80)}"
            )
            return null
        }
        val options = parseOptions(task.selectedAnswer)
        val correctLabel = correctAnswerPattern.find(task.selectedAnswer)
            ?.groupValues
            ?.getOrNull(1)
            ?.uppercase()
            ?: task.choiceLabels.lastOrNull()
            ?: run {
                AutomationLog.warn("OptionLocator", "correct label not found")
                return null
            }
        val targetOption = options.firstOrNull { it.first == correctLabel }
        val targetText = targetOption?.second.orEmpty()
        val normalizedTargetText = normalize(targetText)
        AutomationLog.debug(
            "OptionLocator",
            "label=$correctLabel, textChars=${targetText.length}, boxes=${textBoxes.size}"
        )

        val candidates = buildList<ScoredTextBox> {
            validBoxes.forEach { box ->
                add(scoreBox(box, correctLabel, normalizedTargetText))
            }
            for (first in validBoxes) {
                for (second in validBoxes) {
                    if (first === second) continue
                    if (!boxesAreOnSameLine(first, second)) continue
                    val combined = RecognizedTextBox(
                        text = "${first.text} ${second.text}".trim(),
                        left = minOf(first.left, second.left),
                        top = minOf(first.top, second.top),
                        right = maxOf(first.right, second.right),
                        bottom = maxOf(first.bottom, second.bottom),
                        confidence = minOf(first.confidence, second.confidence)
                    )
                    add(scoreBox(combined, correctLabel, normalizedTargetText))
                }
            }
        }

        val best = candidates
            .filter { it.score >= MIN_ACCEPT_SCORE }
            .maxByOrNull { it.score }
            ?: run {
                AutomationLog.warn("OptionLocator", "no candidate above threshold")
                return null
            }
        AutomationLog.info(
            "OptionLocator",
            "matched label=${correctLabel} score=${best.score} x=${(best.box.left + best.box.right) / 2} " +
                "y=${(best.box.top + best.box.bottom) / 2}"
        )
        return LocatedOption(
            x = (best.box.left + best.box.right) / 2,
            y = (best.box.top + best.box.bottom) / 2,
            score = best.score,
            label = correctLabel,
            matchedText = best.box.text
        )
    }

    private fun parseOptions(answer: String): List<Pair<String, String>> {
        return optionPattern.findAll(answer)
            .map { match ->
                match.groupValues[1].uppercase() to match.groupValues[2].trim()
            }
            .toList()
    }

    private fun scoreBox(
        box: RecognizedTextBox,
        label: String,
        normalizedTargetText: String
    ): ScoredTextBox {
        val normalized = normalize(box.text)
        val normalizedLabel = label.lowercase()
        val startsWithLabel = normalized.startsWith(normalizedLabel) ||
            box.text.trimStart().startsWith(label, ignoreCase = true)
        val containsLabel = normalized.contains(normalizedLabel)
        val containsText = normalizedTargetText.isNotEmpty() && (
            normalized.contains(normalizedTargetText) ||
                normalizedTargetText.contains(normalized)
            )
        val score = when {
            startsWithLabel && containsText -> 1f
            containsText -> 0.82f
            startsWithLabel -> 0.72f
            containsLabel -> 0.45f
            else -> 0f
        }
        return ScoredTextBox(box = box, score = score)
    }

    private fun boxesAreOnSameLine(first: RecognizedTextBox, second: RecognizedTextBox): Boolean {
        val firstCenter = (first.top + first.bottom) / 2
        val secondCenter = (second.top + second.bottom) / 2
        val sameRow = abs(firstCenter - secondCenter) <=
            max(12, max(first.bottom - first.top, second.bottom - second.top) / 2)
        val horizontallyNear = when {
            first.right <= second.left -> second.left - first.right <= 160
            second.right <= first.left -> first.left - second.right <= 160
            else -> true
        }
        return sameRow && horizontallyNear
    }

    private fun isValidBox(
        box: RecognizedTextBox,
        screenWidth: Int,
        screenHeight: Int
    ): Boolean {
        if (screenWidth <= 0 || screenHeight <= 0) return false
        return box.left >= 0 &&
            box.top >= 0 &&
            box.right > box.left &&
            box.bottom > box.top &&
            box.right <= screenWidth &&
            box.bottom <= screenHeight
    }

    private fun matchesCurrentQuestion(
        questionText: String,
        textBoxes: List<RecognizedTextBox>
    ): Boolean {
        val target = normalize(questionText)
        if (target.length < 4) return true
        val screenText = normalize(textBoxes.joinToString(" ") { it.text })
        if (screenText.contains(target) || target.contains(screenText)) {
            return true
        }
        val bestSimilarity = textBoxes.maxOfOrNull { box ->
            similarity(normalize(box.text), target)
        } ?: 0f
        AutomationLog.debug(
            "OptionLocator",
            "question similarity=${"%.2f".format(bestSimilarity)}"
        )
        return bestSimilarity >= MIN_QUESTION_SIMILARITY
    }

    private fun similarity(first: String, second: String): Float {
        if (first.isEmpty() || second.isEmpty()) return 0f
        if (first == second) return 1f
        if (first.contains(second) || second.contains(first)) return 1f
        val maxLength = max(first.length, second.length)
        val distance = levenshteinDistance(first, second)
        return 1f - distance.toFloat() / maxLength.toFloat()
    }

    private fun levenshteinDistance(first: String, second: String): Int {
        var previous = IntArray(second.length + 1) { it }
        var current = IntArray(second.length + 1)
        for (i in 1..first.length) {
            current[0] = i
            for (j in 1..second.length) {
                val substitutionCost = if (first[i - 1] == second[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + substitutionCost
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[second.length]
    }

    private fun normalize(value: String): String {
        return value
            .lowercase()
            .replace("\u200B", "")
            .replace(punctuationPattern, "")
    }

    private const val MIN_ACCEPT_SCORE = 0.6f
    private const val MIN_QUESTION_SIMILARITY = 0.62f
}
