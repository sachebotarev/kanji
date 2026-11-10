package ru.mobui.kanji.checker.shape

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import ru.mobui.kanji.checker.shape.matching.ShapeMatch
import ru.mobui.kanji.checker.shape.matching.ShapeMatcher
import ru.mobui.kanji.checker.shape.rules.KanjiRuleDictionary
import ru.mobui.kanji.domain.practice.CheckCompensation
import ru.mobui.kanji.domain.practice.CheckFailure
import ru.mobui.kanji.domain.practice.CheckReport
import ru.mobui.kanji.domain.practice.CheckRequest
import ru.mobui.kanji.domain.practice.CheckVerdict
import ru.mobui.kanji.domain.practice.KanjiChecker
import ru.mobui.kanji.domain.practice.KanjiReferenceSource
import ru.mobui.kanji.domain.practice.KanjiRulesSource

/** Provider-independent adapter. Reference loading and geometry run away from the UI thread. */
class ShapeKanjiChecker(
    private val references: KanjiReferenceSource,
    private val dispatcher: CoroutineDispatcher,
    private val config: ShapeCheckConfig = ShapeCheckConfig(),
    private val diagnosticsEnabled: Boolean = false,
    private val configProvider: () -> ShapeCheckConfig = { config },
    private val ruleSource: KanjiRulesSource = KanjiRuleDictionary(),
) : KanjiChecker {

    override suspend fun check(request: CheckRequest): CheckVerdict =
        checkWithDetails(request).verdict

    override suspend fun checkWithDetails(request: CheckRequest): CheckReport =
        withContext(dispatcher) {
            val config = configProvider()
            val matcher = ShapeMatcher(config)
            val started = System.nanoTime()
            val reference =
                references.load(request.kanjiId)
                    ?: return@withContext CheckReport(CheckVerdict.Unavailable)
            if (request.sample.strokeCount == 0)
                return@withContext CheckReport(
                    CheckVerdict.Incomplete,
                    listOf(CheckFailure.StrokeCount(0, reference.strokes.size)),
                )
            val rules = ruleSource.rulesFor(request.kanjiId).takeIf { config.checkStrokeStructure }
            if (rules != null && rules.referenceStrokeCount != reference.strokes.size)
                return@withContext CheckReport(
                    CheckVerdict.Unavailable,
                    listOf(
                        CheckFailure.RuleConfiguration(
                            rules.referenceStrokeCount,
                            reference.strokes.size,
                        ),
                    ),
                )
            fun report(
                verdict: CheckVerdict,
                score: ShapeMatch? = null,
                failures: List<CheckFailure> = emptyList(),
            ): CheckReport {
                val metrics =
                    if (diagnosticsEnabled) {
                        ShapeCheckDiagnostics(request, reference, config, rules, started)
                            .collect(score)
                    } else emptyMap()

                return CheckReport(
                    verdict,
                    failures,
                    score?.compensation ?: CheckCompensation(),
                    metrics,
                    score?.compensatedDrawing?.takeIf { diagnosticsEnabled },
                    matchPercent = score?.matchPercent,
                )
            }
            ensureActive()
            val score = matcher.compare(request.sample, reference, rules)
            ensureActive()
            if (request.sample.strokeCount < reference.strokes.size)
                return@withContext report(
                    CheckVerdict.Incomplete,
                    score,
                    buildList {
                        addAll(score.compensation.failures)
                        addAll(score.strokeChecks.failures)
                        add(
                            CheckFailure.StrokeCount(
                                request.sample.strokeCount,
                                reference.strokes.size,
                            ),
                        )
                        if (score.matchPercent < config.requiredMatchPercent) {
                            add(
                                CheckFailure.ShapeCoverage(
                                    score.matchPercent,
                                    config.requiredMatchPercent,
                                ),
                            )
                        }
                    },
                )
            val failures = buildList {
                addAll(score.compensation.failures)
                addAll(score.strokeChecks.failures)
                if (!score.strokeCountMatches)
                    add(
                        CheckFailure.StrokeCount(
                            request.sample.strokeCount,
                            reference.strokes.size,
                        ),
                    )
                if (score.matchPercent < config.requiredMatchPercent)
                    add(CheckFailure.ShapeCoverage(score.matchPercent, config.requiredMatchPercent))
            }
            report(
                if (score.isAccepted(config)) CheckVerdict.Correct else CheckVerdict.Incorrect,
                score,
                failures,
            )
        }
}
