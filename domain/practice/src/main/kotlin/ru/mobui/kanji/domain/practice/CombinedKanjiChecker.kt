package ru.mobui.kanji.domain.practice

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

enum class CheckKind { Readability, Shape, StrokeWriting }
data class IndividualCheckResult(val kind: CheckKind, val verdict: CheckVerdict?, val details: String? = null, val matchPercent: Float? = null, val failures: List<CheckFailure> = emptyList())

/** All providers see the same ink and settings snapshot. Disabled providers are never called. */
class CombinedKanjiChecker(
    private val readability: KanjiChecker,
    private val shape: (RecognitionSettings) -> KanjiChecker,
    private val writing: KanjiChecker,
    private val settings: () -> RecognitionSettings,
) : KanjiChecker {
    override suspend fun check(request: CheckRequest) = checkWithDetails(request).verdict
    override suspend fun checkWithDetails(request: CheckRequest): CheckReport = coroutineScope {
        val config = settings()
        val providers = listOf(
            Triple(CheckKind.Readability, config.readabilityEnabled, readability),
            Triple(CheckKind.Shape, config.shapeEnabled, shape(config)),
            Triple(CheckKind.StrokeWriting, config.strokeWritingEnabled, writing),
        )
        val reports = providers.map { (kind, enabled, checker) -> async {
            kind to if (!enabled) null else try { checker.checkWithDetails(request) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { CheckReport(CheckVerdict.Unavailable) }
        } }.map { it.await() }
        val active = reports.mapNotNull { it.second }
        val verdict = when {
            active.isEmpty() -> CheckVerdict.Incomplete
            active.any { it.verdict == CheckVerdict.Unavailable } -> CheckVerdict.Unavailable
            active.any { it.verdict == CheckVerdict.Incomplete } -> CheckVerdict.Incomplete
            active.any { it.verdict == CheckVerdict.Incorrect } -> CheckVerdict.Incorrect
            else -> CheckVerdict.Correct
        }
        val geometry = reports.first { it.first == CheckKind.Shape }.second
        CheckReport(verdict, active.flatMap { it.failures },
            geometry?.compensation ?: CheckCompensation(), geometry?.metrics.orEmpty(), geometry?.compensatedDrawing,
            reports.map { (kind, report) -> IndividualCheckResult(kind, report?.verdict, report?.metrics?.get("recognizedText"), report?.matchPercent, report?.failures.orEmpty()) },
            matchPercent = geometry?.matchPercent)
    }
}
