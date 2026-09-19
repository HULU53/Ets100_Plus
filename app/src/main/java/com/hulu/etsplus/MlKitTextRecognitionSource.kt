package com.hulu.etsplus

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object MlKitTextRecognitionSource : TextRecognitionSource {
    private val recognizer by lazy {
        TextRecognition.getClient(
            ChineseTextRecognizerOptions.Builder().build()
        )
    }

    override suspend fun recognize(frame: AutomationFrame): List<RecognizedTextBox> {
        AutomationLog.debug(
            "OCR",
            "start bitmap=${frame.bitmap.width}x${frame.bitmap.height}"
        )
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(InputImage.fromBitmap(frame.bitmap, 0))
                .addOnSuccessListener { result ->
                    if (!continuation.isActive) return@addOnSuccessListener
                    val output = mutableListOf<RecognizedTextBox>()
                    result.textBlocks.forEach { block ->
                        block.lines.forEach { line ->
                            val bounds = line.boundingBox ?: return@forEach
                            if (line.text.isBlank()) return@forEach
                            output.add(
                                RecognizedTextBox(
                                    text = line.text,
                                    left = bounds.left,
                                    top = bounds.top,
                                    right = bounds.right,
                                    bottom = bounds.bottom,
                                    confidence = 1f
                                )
                            )
                        }
                    }
                    AutomationLog.info("OCR", "success boxes=${output.size}")
                    continuation.resume(output)
                }
                .addOnFailureListener {
                    AutomationLog.error("OCR", "failure", it)
                    if (continuation.isActive) {
                        continuation.resume(emptyList())
                    }
                }
        }
    }

    fun close() {
        recognizer.close()
    }
}

object HybridTextRecognitionSource : TextRecognitionSource {
    override suspend fun recognize(frame: AutomationFrame): List<RecognizedTextBox> {
        val nodeText = AccessibilityNodeTextSource.recognize(frame)
        if (nodeText.count { it.text.any(Char::isLetterOrDigit) } >= 6) {
            AutomationLog.info("HybridOCR", "use node text boxes=${nodeText.size}")
            return nodeText
        }
        val ocrText = MlKitTextRecognitionSource.recognize(frame)
        val combined = (nodeText + ocrText).distinctBy { box ->
            "${box.text}:${box.left}:${box.top}:${box.right}:${box.bottom}"
        }
        AutomationLog.info(
            "HybridOCR",
            "use combined node=${nodeText.size}, ocr=${ocrText.size}, merged=${combined.size}"
        )
        return combined
    }
}
