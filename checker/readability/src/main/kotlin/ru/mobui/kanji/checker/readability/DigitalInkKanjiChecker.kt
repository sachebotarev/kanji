package ru.mobui.kanji.checker.readability

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.google.mlkit.vision.digitalink.recognition.WritingArea
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import ru.mobui.kanji.domain.practice.CheckReport
import ru.mobui.kanji.domain.practice.CheckRequest
import ru.mobui.kanji.domain.practice.CheckVerdict
import ru.mobui.kanji.domain.practice.KanjiChecker

/** Japanese model downloads once; ink recognition then runs on device. No target-text hints. */
class DigitalInkKanjiChecker : KanjiChecker, Closeable {
    @Volatile private var modelDownloaded = false
    @Volatile private var recognizer: DigitalInkRecognizer? = null

    private fun japaneseModel(): DigitalInkRecognitionModel =
        DigitalInkRecognitionModel.builder(
                requireNotNull(DigitalInkRecognitionModelIdentifier.fromLanguageTag("ja")),
            )
            .build()

    suspend fun isModelDownloaded(): Boolean {
        if (modelDownloaded) return true
        val downloaded =
            RemoteModelManager.getInstance().isModelDownloaded(japaneseModel()).awaitResult()
        if (downloaded) modelDownloaded = true
        return downloaded
    }

    suspend fun downloadModel(): Boolean =
        withTimeoutOrNull(MODEL_DOWNLOAD_TIMEOUT_MILLIS) {
            RemoteModelManager.getInstance()
                .download(japaneseModel(), DownloadConditions.Builder().build())
                .awaitResult()
            modelDownloaded = true
            true
        } ?: false

    private fun getRecognizer(model: DigitalInkRecognitionModel): DigitalInkRecognizer =
        recognizer ?: synchronized(this) {
            recognizer ?: DigitalInkRecognition.getClient(
                DigitalInkRecognizerOptions.builder(model).build(),
            ).also { recognizer = it }
        }

    override fun close() {
        synchronized(this) {
            recognizer?.close()
            recognizer = null
        }
    }

    override suspend fun check(request: CheckRequest) = checkWithDetails(request).verdict

    override suspend fun checkWithDetails(request: CheckRequest): CheckReport {
        if (request.sample.strokeCount == 0) return CheckReport(CheckVerdict.Incomplete)
        val codePoint =
            request.kanjiId.value.removePrefix("u").toIntOrNull(UNICODE_HEX_RADIX)?.takeIf {
                Character.isValidCodePoint(it)
            } ?: return CheckReport(CheckVerdict.Unavailable)
        val expected = String(Character.toChars(codePoint))
        return withTimeoutOrNull(RECOGNITION_TIMEOUT_MILLIS) {
            val identifier =
                DigitalInkRecognitionModelIdentifier.fromLanguageTag("ja")
                    ?: return@withTimeoutOrNull CheckReport(CheckVerdict.Unavailable)
            val model = DigitalInkRecognitionModel.builder(identifier).build()
            if (!isModelDownloaded()) {
                return@withTimeoutOrNull CheckReport(CheckVerdict.Unavailable)
            }
            val ink = Ink.builder()
            // Pad timestamps restart per stroke; omit time rather than invent inter-stroke
            // intervals.
            request.sample.strokes.forEach { stroke ->
                val builder = Ink.Stroke.builder()
                stroke.points.forEach {
                    builder.addPoint(
                        Ink.Point.create(it.x * WRITING_AREA_UNITS, it.y * WRITING_AREA_UNITS),
                    )
                }
                ink.addStroke(builder.build())
            }
            val client = getRecognizer(model)
            val result =
                client
                    .recognize(
                        ink.build(),
                        RecognitionContext.builder()
                            // Required by the SDK builder even for a standalone character.
                            .setPreContext("")
                            .setWritingArea(WritingArea(WRITING_AREA_SIZE, WRITING_AREA_SIZE))
                            .build(),
                    )
                    .awaitResult()
            val text = result.candidates.firstOrNull()?.text.orEmpty()
            CheckReport(
                if (text == expected) CheckVerdict.Correct else CheckVerdict.Incorrect,
                metrics = mapOf("recognizedText" to text),
            )
        } ?: CheckReport(CheckVerdict.Unavailable)
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

private const val MODEL_DOWNLOAD_TIMEOUT_MILLIS = 60_000L
private const val UNICODE_HEX_RADIX = 16
private const val RECOGNITION_TIMEOUT_MILLIS = 30_000L
private const val WRITING_AREA_UNITS = 1000
private const val WRITING_AREA_SIZE = 1000f
