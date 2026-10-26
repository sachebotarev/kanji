package ru.mobui.kanji.domain.practice

import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.WritingSample

/** Provider-neutral key. The reference adapter owns the mapping to its dataset. */
@JvmInline
value class KanjiId(val value: String) { init { require(value.isNotBlank()) } }

enum class EntryMode { User, Prefilled }

data class KanjiTask(val occurrenceId: String, val kanjiId: KanjiId, val mode: EntryMode) {
    init { require(occurrenceId.isNotBlank()) }
}

data class PracticeTask(
    val taskId: String,
    val russianWord: String,
    val kanji: List<KanjiTask>,
    val timeLimitMs: Long? = null,
) {
    init {
        require(taskId.isNotBlank() && russianWord.isNotBlank())
        require(kanji.isNotEmpty())
        require(kanji.map { it.occurrenceId }.distinct().size == kanji.size)
        require(timeLimitMs == null || timeLimitMs > 0)
    }
}

enum class EntryStatus { Empty, Editing, Correct, Incorrect, Prefilled }
enum class AnswerStatus { Correct, Assisted, Skipped, TimedOut }

data class KanjiResult(
    val occurrenceId: String,
    val kanjiId: KanjiId,
    val mode: EntryMode,
    val status: EntryStatus,
    val elapsedMs: Long,
    val sample: WritingSample,
    val hintUsed: Boolean,
)

data class PracticeResult(
    val taskId: String,
    val russianWord: String,
    val status: AnswerStatus,
    val kanji: List<KanjiResult>,
    val elapsedMs: Long,
)

data class CheckRequest(
    val taskId: String,
    val occurrenceId: String,
    val kanjiId: KanjiId,
    val revision: Long,
    val sample: WritingSample,
)

enum class CheckVerdict { Correct, Incorrect, Incomplete, Unavailable }

/** Main-safe implementation supplied by the caller. No recognition is done by UI widgets. */
sealed interface CheckFailure {
    data class KanjiRules(val violations: List<KanjiRuleViolation>) : CheckFailure
    data class RuleConfiguration(val expectedStrokeCount: Int, val actualStrokeCount: Int) : CheckFailure
    data class StrokeOrder(val mismatches: List<StrokeOrderMismatch>) : CheckFailure
    data class MissingHooks(val referenceStrokes: List<Int>) : CheckFailure
    data class StrokeDirection(val reversedStrokes: List<Int>) : CheckFailure
    data class Shear(val compensation: ShearCompensation) : CheckFailure
    data class Budget(val budget: CompensationBudget) : CheckFailure
    data class Rotation(val compensation: RotationCompensation) : CheckFailure
    data class Stretch(val compensation: StretchCompensation) : CheckFailure
    data class Size(val compensation: SizeCompensation) : CheckFailure
    data class Translation(val compensation: TranslationCompensation) : CheckFailure
    data class StrokeCount(val actual: Int, val expected: Int) : CheckFailure
    data class ShapeCoverage(val actualPercent: Float, val requiredPercent: Float) : CheckFailure
}
/** Correction applied for comparison only, relative to the original input geometry. */
data class CheckCompensation(
    val translation: TranslationCompensation = TranslationCompensation(),
    val size: SizeCompensation = SizeCompensation(),
    val stretch: StretchCompensation = StretchCompensation(),
    val rotation: RotationCompensation = RotationCompensation(),
    val jitter: JitterCompensation = JitterCompensation(),
    val budget: CompensationBudget = CompensationBudget(),
    val shear: ShearCompensation = ShearCompensation(),
) {
    val rotationDegrees: Float get() = rotation.degrees
    val scaleX: Float get() = stretch.scaleX
    val scaleY: Float get() = stretch.scaleY
    val failures: List<CheckFailure> get() = buildList {
        if (shear.hasError) add(CheckFailure.Shear(shear))
        if (translation.hasError) add(CheckFailure.Translation(translation))
        if (this@CheckCompensation.size.hasError) add(CheckFailure.Size(this@CheckCompensation.size))
        if (stretch.hasError) add(CheckFailure.Stretch(stretch))
        if (rotation.hasError) add(CheckFailure.Rotation(rotation))
        if (budget.hasError) add(CheckFailure.Budget(budget))
    }
    val isApplied: Boolean get() = shear.isApplied || translation.isApplied || size.isApplied || stretch.isApplied || jitter.isApplied ||
        rotation.isApplied
}
data class CheckReport(
    val verdict: CheckVerdict,
    val failures: List<CheckFailure> = emptyList(),
    val compensation: CheckCompensation = CheckCompensation(),
    val metrics: Map<String, String> = emptyMap(),
    val compensatedDrawing: ru.mobui.kanji.domain.handwriting.ComparisonDrawing? = null,
    val checks: List<IndividualCheckResult> = emptyList(),
    val matchPercent: Float? = null,
)

fun interface KanjiChecker {
    suspend fun check(request: CheckRequest): CheckVerdict
    suspend fun checkWithDetails(request: CheckRequest): CheckReport = CheckReport(check(request))
}

/** Display source, deliberately separate from the handwriting checker. */
fun interface KanjiReferenceSource { suspend fun load(id: KanjiId): ReferenceDrawing? }
