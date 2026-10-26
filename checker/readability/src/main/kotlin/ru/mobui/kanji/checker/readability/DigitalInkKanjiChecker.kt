package ru.mobui.kanji.checker.readability

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import ru.mobui.kanji.domain.practice.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Japanese model downloads once; ink recognition then runs on device. No target-text hints. */
class DigitalInkKanjiChecker : KanjiChecker {
    private fun japaneseModel(): DigitalInkRecognitionModel = DigitalInkRecognitionModel.builder(
        requireNotNull(DigitalInkRecognitionModelIdentifier.fromLanguageTag("ja")),
    ).build()

    suspend fun isModelDownloaded(): Boolean = RemoteModelManager.getInstance()
        .isModelDownloaded(japaneseModel()).awaitResult()

    suspend fun downloadModel(): Boolean = withTimeoutOrNull(60_000) {
        RemoteModelManager.getInstance().download(japaneseModel(), DownloadConditions.Builder().build()).awaitResult()
        true
    } ?: false

    override suspend fun check(request: CheckRequest) = checkWithDetails(request).verdict
    override suspend fun checkWithDetails(request: CheckRequest): CheckReport {
        if (request.sample.strokeCount == 0) return CheckReport(CheckVerdict.Incomplete)
        val codePoint = request.kanjiId.value.removePrefix("u").toIntOrNull(16)
            ?.takeIf { Character.isValidCodePoint(it) } ?: return CheckReport(CheckVerdict.Unavailable)
        val expected = String(Character.toChars(codePoint))
        return withTimeoutOrNull(30_000) {
            val identifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("ja")
                ?: return@withTimeoutOrNull CheckReport(CheckVerdict.Unavailable)
            val model = DigitalInkRecognitionModel.builder(identifier).build()
            val manager = RemoteModelManager.getInstance()
            if (!manager.isModelDownloaded(model).awaitResult()) {
                return@withTimeoutOrNull CheckReport(CheckVerdict.Unavailable)
            }
            val ink = Ink.builder()
            // Pad timestamps restart per stroke; omit time rather than invent inter-stroke intervals.
            request.sample.strokes.forEach { stroke ->
                val builder = Ink.Stroke.builder()
                stroke.points.forEach { builder.addPoint(Ink.Point.create(it.x * 1000, it.y * 1000)) }
                ink.addStroke(builder.build())
            }
            val recognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
            try {
                val result = recognizer.recognize(ink.build(), RecognitionContext.builder()
                    // Required by the SDK builder even for a standalone character.
                    .setPreContext("")
                    .setWritingArea(WritingArea(1000f, 1000f)).build()).awaitResult()
                val text = result.candidates.firstOrNull()?.text.orEmpty()
                CheckReport(if (text == expected) CheckVerdict.Correct else CheckVerdict.Incorrect,
                    metrics = mapOf("recognizedText" to text))
            } finally { recognizer.close() }
        } ?: CheckReport(CheckVerdict.Unavailable)
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
