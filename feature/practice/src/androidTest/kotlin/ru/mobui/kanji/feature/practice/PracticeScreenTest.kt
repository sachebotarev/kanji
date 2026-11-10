package ru.mobui.kanji.feature.practice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.bottom
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.height
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.top
import androidx.compose.ui.test.up
import androidx.compose.ui.test.width
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.ComparisonDrawing
import ru.mobui.kanji.domain.handwriting.ComparisonPoint
import ru.mobui.kanji.domain.handwriting.InkPoint
import ru.mobui.kanji.domain.handwriting.InkStroke
import ru.mobui.kanji.domain.handwriting.InkTool
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.ReferencePoint
import ru.mobui.kanji.domain.handwriting.WritingSample
import ru.mobui.kanji.domain.practice.AnswerStatus
import ru.mobui.kanji.domain.practice.CheckCompensation
import ru.mobui.kanji.domain.practice.CheckFailure
import ru.mobui.kanji.domain.practice.CheckKind
import ru.mobui.kanji.domain.practice.CheckReport
import ru.mobui.kanji.domain.practice.CheckRequest
import ru.mobui.kanji.domain.practice.CheckVerdict
import ru.mobui.kanji.domain.practice.CompensationStage
import ru.mobui.kanji.domain.practice.EntryMode
import ru.mobui.kanji.domain.practice.EntryStatus
import ru.mobui.kanji.domain.practice.HintOverlayStyle
import ru.mobui.kanji.domain.practice.IndividualCheckResult
import ru.mobui.kanji.domain.practice.KanjiChecker
import ru.mobui.kanji.domain.practice.KanjiId
import ru.mobui.kanji.domain.practice.KanjiReferenceSource
import ru.mobui.kanji.domain.practice.KanjiTask
import ru.mobui.kanji.domain.practice.PracticeResult
import ru.mobui.kanji.domain.practice.HandwritingTraining
import ru.mobui.kanji.domain.practice.PracticeTask
import ru.mobui.kanji.domain.practice.RecognitionSettings
import ru.mobui.kanji.domain.practice.RotationCompensation
import ru.mobui.kanji.domain.practice.StretchCompensation
import ru.mobui.kanji.domain.practice.StrokeOrderMismatch

@Suppress("LargeClass")
class PracticeScreenTest {
    private fun SemanticsNodeInteraction.bringIntoViewIfNeeded(): SemanticsNodeInteraction =
        if (isDisplayed()) this else performScrollTo()

    @get:Rule val compose = createComposeRule()
    private val task =
        PracticeTask(
            "test-word",
            "Библиотека",
            listOf(
                KanjiTask("first", KanjiId("u56f3"), EntryMode.User),
                KanjiTask("second", KanjiId("u66f8"), EntryMode.User),
                KanjiTask("third", KanjiId("u9928"), EntryMode.Prefilled),
            ),
            reading = "としょかん",
        )
    private val references = KanjiReferenceSource {
        ReferenceDrawing(listOf(listOf(ReferencePoint(.2f, .2f), ReferencePoint(.8f, .8f))))
    }

    @Test
    fun checkIndicatorsExposeIndependentResultsInsteadOfAggregateStatus() {
        val state =
            PracticeUiState(
                HandwritingTraining(task).state,
                currentCheckReport =
                    CheckReport(
                        CheckVerdict.Incorrect,
                        checks =
                            listOf(
                                IndividualCheckResult(
                                    CheckKind.Readability,
                                    CheckVerdict.Correct,
                                    "日",
                                ),
                                IndividualCheckResult(
                                    CheckKind.Shape,
                                    CheckVerdict.Incorrect,
                                    matchPercent = 68f,
                                ),
                                IndividualCheckResult(CheckKind.StrokeWriting, null),
                            ),
                    ),
            )
        compose.setContent { MaterialTheme { PracticeScreen(state, {}, {}) } }
        compose
            .onNodeWithTag("check-indicator-Readability")
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Пройдена: 日"),
            )
        compose
            .onNodeWithTag("check-indicator-Shape")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Не пройдена: 68%",
                ),
            )
        compose
            .onNodeWithTag("check-indicator-StrokeWriting")
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Выключено: 0"),
            )
        compose.onNodeWithText("Знак 1 из 3").assertDoesNotExist()
        compose.onNodeWithText("Неверно").assertDoesNotExist()
    }

    @Test
    fun mismatchedReadabilityStillShowsRecognizedCharacterWithoutStatusSymbols() {
        val report =
            CheckReport(
                CheckVerdict.Incorrect,
                checks =
                    listOf(
                        IndividualCheckResult(CheckKind.Readability, CheckVerdict.Incorrect, "月"),
                        IndividualCheckResult(
                            CheckKind.Shape,
                            CheckVerdict.Incorrect,
                            matchPercent = 68f,
                        ),
                        IndividualCheckResult(CheckKind.StrokeWriting, CheckVerdict.Correct),
                    ),
            )
        compose.setContent {
            MaterialTheme { CheckerTestingStatuses(report, false, strokeCount = 4) }
        }
        compose
            .onNodeWithTag("check-indicator-Readability")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Не пройдена: 月",
                ),
            )
        compose
            .onNodeWithTag("check-indicator-Shape")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Не пройдена: 68%",
                ),
            )
        compose
            .onNodeWithTag("check-indicator-StrokeWriting")
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Пройдена: 4"),
            )
    }

    @Test
    fun clickingEachCheckOpensOnlyItsDetailsAndCanDismiss() {
        val reference =
            ReferenceDrawing(List(4) { listOf(ReferencePoint(.2f, .2f), ReferencePoint(.8f, .8f)) })
        val ink =
            WritingSample(
                List(4) {
                    InkStroke(
                        listOf(InkPoint(.2f, .2f, 0), InkPoint(.8f, .8f, 100)),
                        InkTool.Finger,
                    )
                },
            )
        val initial = HandwritingTraining(task).state
        val practice =
            initial.copy(
                cells =
                    initial.cells.mapIndexed { index, cell ->
                        if (index == 0) cell.copy(sample = ink, reference = reference) else cell
                    },
            )
        val report =
            CheckReport(
                CheckVerdict.Incorrect,
                compensation =
                    CheckCompensation(
                        budget =
                            ru.mobui.kanji.domain.practice.CompensationBudget(
                                translationPercent = 5f,
                            ),
                    ),
                checks =
                    listOf(
                        IndividualCheckResult(CheckKind.Readability, CheckVerdict.Incorrect, "月"),
                        IndividualCheckResult(
                            CheckKind.Shape,
                            CheckVerdict.Correct,
                            matchPercent = 92f,
                        ),
                        IndividualCheckResult(
                            CheckKind.StrokeWriting,
                            CheckVerdict.Incorrect,
                            failures =
                                listOf(
                                    CheckFailure.StrokeOrder(
                                        listOf(
                                            StrokeOrderMismatch(2, 3),
                                            StrokeOrderMismatch(3, 2),
                                        ),
                                    ),
                                ),
                        ),
                    ),
            )
        compose.setContent {
            MaterialTheme {
                PracticeScreen(PracticeUiState(practice, currentCheckReport = report), {}, {})
            }
        }
        compose.onNodeWithTag("check-indicator-Readability").performClick()
        compose.onNodeWithTag("check-details-dialog").assertIsDisplayed()
        compose.onNodeWithText("Распознано").assertIsDisplayed()
        compose.onNodeWithText("Задано").assertDoesNotExist()
        compose.onNodeWithText("月").assertIsDisplayed()
        compose.onNodeWithTag("close-check-details").performClick()
        compose.onNodeWithTag("check-indicator-Shape").performClick()
        compose.onNodeWithText("Соответствие").assertIsDisplayed()
        compose.onNodeWithText("Компенсация").assertIsDisplayed()
        compose.onNodeWithText("5%").assertIsDisplayed()
        compose.onNodeWithTag("close-check-details").performClick()
        compose.onNodeWithTag("check-indicator-StrokeWriting").performClick()
        compose.onNodeWithText("4 из 4").assertIsDisplayed()
        compose.onNodeWithText("Нарушен").assertIsDisplayed()
        compose.onNodeWithText("Порядок").assertIsDisplayed()
        compose.onNodeWithTag("check-order-detail").assertIsDisplayed()
        compose.onNodeWithText("2-й по счёту оказалась 3-я черта", substring = true).assertIsDisplayed()
        compose.onNodeWithText("3-й по счёту оказалась 2-я черта", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("close-check-details").performClick()
        compose.onNodeWithTag("check-details-dialog").assertDoesNotExist()
    }

    @Test
    fun incompleteWritingDoesNotClaimThatOrderAndDirectionPassed() {
        val initial = HandwritingTraining(task).state
        val reference =
            ReferenceDrawing(List(4) { listOf(ReferencePoint(.2f, .2f), ReferencePoint(.8f, .8f)) })
        val cell = initial.selected!!.copy(reference = reference)
        val report =
            CheckReport(
                CheckVerdict.Incomplete,
                checks =
                    listOf(
                        IndividualCheckResult(CheckKind.StrokeWriting, CheckVerdict.Incomplete),
                    ),
            )
        compose.setContent {
            MaterialTheme { CheckDetailsDialog(CheckKind.StrokeWriting, report, cell, {}) }
        }
        compose.onAllNodesWithText("Не проверено").assertCountEquals(2)
        compose.onNodeWithText("Пройдена").assertDoesNotExist()
    }

    @Test
    fun headerShowsOnlyElapsedTimeWithoutLimitAndNoInstructionOrButtons() {
        val initial = HandwritingTraining(task).state.copy(elapsedMs = 12_800)
        compose.setContent {
            MaterialTheme {
                PracticeScreen(PracticeUiState(initial, debugEnabled = true), {}, {}, onBack = {})
            }
        }
        compose.onNodeWithTag("header-timer").assertTextEquals("00:12")
        compose.onNodeWithTag("practice-back").assertIsDisplayed()
        compose.onNodeWithTag("open-checker-settings").assertDoesNotExist()
        compose.onNodeWithTag("open-debug").assertDoesNotExist()
        compose.onNodeWithTag("compensated-drawing").assertDoesNotExist()
        compose.onNodeWithText("Выберите клетку и пишите").assertDoesNotExist()
        compose
            .onNodeWithTag("word-reading")
            .assert(hasContentDescription("Произношение скрыто. Нажмите, чтобы открыть"))
    }

    @Test
    fun backOpensInterruptDialogAndConfirmLeaves() {
        var left = false
        val vm = PracticeViewModel(task, references, KanjiChecker { CheckVerdict.Unavailable })
        compose.setContent {
            MaterialTheme { PracticeRoute(vm, {}, { left = true }) }
        }
        compose.onNodeWithTag("practice-back").assertIsDisplayed().performClick()
        compose.onNodeWithTag("practice-interrupt-dialog").assertIsDisplayed()
        compose.onNodeWithText("Прогресс сохранится", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("practice-interrupt-stay").performClick()
        compose.onNodeWithTag("practice-interrupt-dialog").assertDoesNotExist()
        compose.runOnIdle { assertFalse(left) }
        compose.onNodeWithTag("practice-back").performClick()
        compose.onNodeWithTag("practice-interrupt-confirm").performClick()
        compose.runOnIdle { assertTrue(left) }
    }

    @Test
    fun countdownShowsTimeOnlyAndReachesZeroAtDeadline() {
        val initial = HandwritingTraining(task.copy(timeLimitMs = 100_000)).state
        val elapsed = mutableStateOf(80_001L)
        compose.setContent {
            MaterialTheme {
                PracticeScreen(PracticeUiState(initial.copy(elapsedMs = elapsed.value)), {}, {})
            }
        }
        compose.onNodeWithTag("header-timer").assertTextEquals("00:20")
        compose.runOnIdle { elapsed.value = 99_999 }
        compose.onNodeWithTag("header-timer").assertTextEquals("00:01")
        compose.runOnIdle { elapsed.value = 100_000 }
        compose.onNodeWithTag("header-timer").assertTextEquals("00:00")
    }

    @Test
    fun changingDisplayedWordPreservesInkAndTraining() {
        val word = mutableStateOf("Electroencephalography")
        val vm = PracticeViewModel(task, references, KanjiChecker { CheckVerdict.Incorrect })
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}, wordLabel = word.value) } }
        compose.onNodeWithText("Electroencephalography").assertExists()
        compose.onNodeWithTag("practice-pad").performTouchInput {
            down(center)
            moveTo(Offset(center.x + 20, center.y))
            up()
        }
        compose.runOnIdle { word.value = "Электроэнцефалография" }
        compose.onNodeWithText("Электроэнцефалография").assertExists()
        compose.runOnIdle {
            assertEquals(1, vm.uiState.value.practice.selected!!.sample.strokeCount)
        }
    }

    @Test
    fun indicatorDialogOmitsCompensatedDrawingControls() {
        val drawing =
            ComparisonDrawing(listOf(listOf(ComparisonPoint(.2f, .2f), ComparisonPoint(.8f, .8f))))
        val checker =
            object : KanjiChecker {
                override suspend fun check(request: CheckRequest) = CheckVerdict.Incorrect

                override suspend fun checkWithDetails(request: CheckRequest) =
                    CheckReport(CheckVerdict.Incorrect, compensatedDrawing = drawing)
            }
        val vm = PracticeViewModel(task, references, checker, showCheckDiagnostics = true)
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.onNodeWithTag("practice-pad").performTouchInput {
            down(center)
            moveTo(Offset(center.x + 20, center.y))
            up()
        }
        compose.waitUntil(5_000) { vm.uiState.value.debugReport?.compensatedDrawing != null }
        compose.onNodeWithTag("check-indicator-Shape").performClick()
        compose.onNodeWithTag("check-details-dialog").assertIsDisplayed()
        compose.onNodeWithTag("compensated-drawing").assertDoesNotExist()
        compose.onNodeWithTag("open-debug").assertDoesNotExist()
        compose.onNodeWithTag("close-check-details").performClick()
    }

    @Test
    fun settingsChangeRechecksAcceptedInkAndWritingRulesToggleIndependently() {
        val settings = mutableStateOf(RecognitionSettings())
        val showSettings = mutableStateOf(false)
        val checker =
            object : KanjiChecker {
                override suspend fun check(request: CheckRequest) =
                    if (settings.value.requiredMatchPercent > 90f) CheckVerdict.Incorrect
                    else CheckVerdict.Correct
            }
        val vm = PracticeViewModel(task, references, checker, showCheckDiagnostics = true)
        compose.setContent {
            MaterialTheme {
                // Keep PRACTICE mounted so lifecycle ticks still recheck after SETTINGS edits.
                Box {
                    PracticeRoute(vm, {}, {})
                    if (showSettings.value) {
                        CheckerSettingsScreen(
                            settings.value,
                            {
                                settings.value = it
                                vm.onCheckerSettingsChanged()
                            },
                            { showSettings.value = false },
                            {
                                settings.value = RecognitionSettings()
                                vm.onCheckerSettingsChanged()
                            },
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("practice-pad").performTouchInput {
            down(center)
            moveTo(Offset(center.x + 20, center.y))
            up()
        }
        compose.waitUntil(5_000) {
            vm.uiState.value.practice.cells[0].status == EntryStatus.Correct
        }
        val oldRevision = vm.uiState.value.practice.cells[0].revision
        val oldInk = vm.uiState.value.practice.cells[0].sample
        compose.runOnIdle { showSettings.value = true }
        compose.onNodeWithTag("parameter-match").bringIntoViewIfNeeded().performSemanticsAction(
            SemanticsActions.SetProgress,
        ) {
            it(95.4f)
        }
        compose.waitUntil(5_000) {
            vm.uiState.value.practice.cells[0].status == EntryStatus.Incorrect
        }
        compose.runOnIdle {
            assertEquals(95f, settings.value.requiredMatchPercent, 0f)
            assertTrue(vm.uiState.value.practice.cells[0].revision > oldRevision)
            assertSame(oldInk, vm.uiState.value.practice.cells[0].sample)
        }
        compose.onNodeWithTag("parameter-jitter").bringIntoViewIfNeeded().performSemanticsAction(
            SemanticsActions.SetProgress,
        ) {
            it(.56f)
        }
        compose.runOnIdle { assertEquals(.6f, settings.value.jitterTolerancePercent, .000001f) }
        val rotation = compose.onNodeWithTag("parameter-rotation").bringIntoViewIfNeeded()
        rotation.performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        compose.runOnIdle {
            assertEquals(0f, settings.value.maxRotationDegrees, 0f)
            assertFalse(CompensationStage.Rotation in settings.value.enabledCompensations)
        }
        rotation.performSemanticsAction(SemanticsActions.SetProgress) { it(4f) }
        compose.runOnIdle {
            assertTrue(CompensationStage.Rotation in settings.value.enabledCompensations)
        }
        compose
            .onNodeWithTag("enable-writing-off")
            .bringIntoViewIfNeeded()
            .performClick()
            .assertIsSelected()
        compose.runOnIdle { assertFalse(settings.value.strokeWritingEnabled) }
        compose
            .onNodeWithTag("enable-readability-off")
            .bringIntoViewIfNeeded()
            .performClick()
            .assertIsSelected()
        compose.runOnIdle {
            assertFalse(settings.value.readabilityEnabled)
            assertTrue(settings.value.shapeEnabled)
        }
        compose.onNodeWithTag("checker-settings-menu").performClick()
        compose.onNodeWithTag("reset-checker-settings").performClick()
        compose.waitUntil(5_000) {
            vm.uiState.value.practice.cells[0].status == EntryStatus.Correct
        }
        compose.runOnIdle {
            assertTrue(settings.value.strokeWritingEnabled)
            assertTrue(settings.value.readabilityEnabled)
        }
    }

    @Test
    fun historiesAreIndependentAndStubDoesNotEnableCompletion() {
        val vm = PracticeViewModel(task, references, KanjiChecker { CheckVerdict.Unavailable })
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.onNodeWithTag("kanji-cell-2").assertIsNotEnabled()
        compose.onNodeWithText("Напишите кандзи").assertDoesNotExist()
        compose.onNodeWithText("Далее").assertIsNotEnabled()
        compose.onNodeWithTag("practice-pad").performTouchInput {
            down(center)
            moveTo(Offset(center.x + 20, center.y))
            up()
        }
        compose
            .onNodeWithContentDescription("Отменить последнее действие")
            .bringIntoViewIfNeeded()
            .assertIsEnabled()
        compose.waitUntil(5_000) { vm.uiState.value.notice == PracticeNotice.CheckerUnavailable }
        compose.onNodeWithTag("practice-notice").assertIsDisplayed()
        compose.onNodeWithText("Проверить").assertDoesNotExist()
        compose.onNodeWithText("Далее").assertIsNotEnabled()
        compose.onNodeWithTag("kanji-cell-1").bringIntoViewIfNeeded().performClick()
        compose
            .onNodeWithContentDescription("Отменить последнее действие")
            .bringIntoViewIfNeeded()
            .assertIsNotEnabled()
        compose.onNodeWithTag("kanji-cell-0").bringIntoViewIfNeeded().performClick()
        compose
            .onNodeWithContentDescription("Отменить последнее действие")
            .bringIntoViewIfNeeded()
            .performClick()
        compose.runOnIdle {
            assertEquals(EntryStatus.Empty, vm.uiState.value.practice.cells.first().status)
        }
        compose.onNodeWithTag("practice-notice").assertIsDisplayed()
    }

    @Test
    fun nextSubmitsCorrectOnlyAfterEveryRequiredSignIsAccepted() {
        var result: PracticeResult? = null
        val vm =
            PracticeViewModel(
                task,
                references,
                KanjiChecker { request ->
                    if (request.occurrenceId == "first" && request.sample.strokeCount == 1)
                        CheckVerdict.Incorrect
                    else CheckVerdict.Correct
                },
            )
        compose.setContent { MaterialTheme { PracticeRoute(vm, { result = it }, {}) } }
        fun drawStroke() {
            compose.onNodeWithTag("practice-pad").bringIntoViewIfNeeded().performTouchInput {
                down(center)
                moveTo(Offset(center.x + 20, center.y))
                up()
            }
        }
        compose.onNodeWithText("Далее").assertIsNotEnabled()
        drawStroke()
        compose.waitUntil(5_000) {
            vm.uiState.value.practice.cells[0].status == EntryStatus.Incorrect
        }
        compose.onNodeWithContentDescription("Знак 1, Неверно").assertExists()
        // Failed check keeps the learner's ink in the strip, not the reference glyph.
        compose.onNodeWithTag("kanji-cell-ink-0", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("kanji-cell-answer-0", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("practice-debug-feedback").assertDoesNotExist()
        drawStroke()
        compose.waitUntil(5_000) {
            vm.uiState.value.practice.cells[0].status == EntryStatus.Correct &&
                vm.uiState.value.practice.selectedIndex == 1
        }
        compose.onNodeWithContentDescription("Знак 1, Верно").assertExists()
        compose.onNodeWithContentDescription("Знак 2, Не введён").assertIsSelected()
        compose.onNodeWithTag("next-kanji").assertDoesNotExist()
        drawStroke()
        compose.waitUntil(5_000) {
            vm.uiState.value.practice.isComplete && !vm.uiState.value.isChecking
        }
        compose.onNodeWithText("Далее").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(AnswerStatus.Correct, result!!.status) }
    }

    @Test
    fun autoAdvanceSubmitsSuccessfulWordWithoutNextTap() {
        var result: PracticeResult? = null
        val vm =
            PracticeViewModel(
                task,
                references,
                KanjiChecker { CheckVerdict.Correct },
                autoAdvanceOnSuccess = { true },
            )
        compose.setContent { MaterialTheme { PracticeRoute(vm, { result = it }, {}) } }
        fun drawStroke() {
            compose.onNodeWithTag("practice-pad").bringIntoViewIfNeeded().performTouchInput {
                down(center)
                moveTo(Offset(center.x + 20, center.y))
                up()
            }
        }
        drawStroke()
        compose.waitUntil(5_000) { vm.uiState.value.practice.selectedIndex == 1 }
        drawStroke()
        compose.waitUntil(5_000) { result?.status == AnswerStatus.Correct }
        compose.runOnIdle {
            assertEquals(AnswerStatus.Correct, result!!.status)
            assertTrue(vm.uiState.value.practice.isComplete)
        }
    }

    @Test
    fun nextStaysDisabledWhileWordIsUnresolved() {
        val vm = PracticeViewModel(task, references, KanjiChecker { CheckVerdict.Incomplete })
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.onNodeWithText("Далее").assertIsNotEnabled()
        compose.onNodeWithTag("practice-pad").bringIntoViewIfNeeded().performTouchInput {
            down(center)
            moveTo(Offset(center.x + 20, center.y))
            up()
        }
        compose.waitUntil(5_000) { vm.uiState.value.notice == PracticeNotice.Incomplete }
        compose.onNodeWithText("Далее").assertIsNotEnabled()
    }

    @Test
    fun incompleteSignStaysNeutralWithoutFailing() {
        val vm = PracticeViewModel(task, references, KanjiChecker { CheckVerdict.Incomplete })
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.onNodeWithTag("practice-pad").bringIntoViewIfNeeded().performTouchInput {
            down(center)
            moveTo(Offset(center.x + 20, center.y))
            up()
        }
        compose.waitUntil(5_000) { vm.uiState.value.notice == PracticeNotice.Incomplete }
        compose.onNodeWithTag("checker-testing-statuses").assertIsDisplayed()
        compose.onNodeWithText("Неверно").assertDoesNotExist()
        compose.onNodeWithTag("practice-notice").assertDoesNotExist()
        compose.onNodeWithText("Далее").assertIsNotEnabled()
    }

    @Test
    fun indicatorDialogHasNoSettingsOrDebugEntry() {
        val checker =
            object : KanjiChecker {
                override suspend fun check(request: CheckRequest) = CheckVerdict.Incorrect

                override suspend fun checkWithDetails(request: CheckRequest) =
                    CheckReport(
                        CheckVerdict.Incorrect,
                        listOf(CheckFailure.StrokeCount(request.sample.strokeCount, 5)),
                    )
            }
        val vm = PracticeViewModel(task, references, checker, showCheckDiagnostics = true)
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.onNodeWithTag("practice-pad").bringIntoViewIfNeeded().performTouchInput {
            down(center)
            moveTo(Offset(center.x + 20, center.y))
            up()
        }
        compose.waitUntil(5_000) { vm.uiState.value.debugFailures.isNotEmpty() }
        compose.onNodeWithTag("check-indicator-Shape").performClick()
        compose.onNodeWithTag("check-details-dialog").assertIsDisplayed()
        compose.onNodeWithTag("open-checker-settings").assertDoesNotExist()
        compose.onNodeWithTag("open-debug").assertDoesNotExist()
        compose.onNodeWithTag("compensated-drawing").assertDoesNotExist()
        compose.onNodeWithTag("close-check-details").performClick()
        compose.onNodeWithTag("check-details-dialog").assertDoesNotExist()
    }

    @Test
    fun debugCompensationStaysOutOfIndicatorDialog() {
        val checker =
            object : KanjiChecker {
                override suspend fun check(request: CheckRequest) = CheckVerdict.Correct

                override suspend fun checkWithDetails(request: CheckRequest) =
                    CheckReport(
                        CheckVerdict.Correct,
                        compensation =
                            CheckCompensation(
                                rotation =
                                    ru.mobui.kanji.domain.practice.RotationCompensation(
                                        degrees = -7f,
                                    ),
                                stretch =
                                    ru.mobui.kanji.domain.practice.StretchCompensation(
                                        scaleX = 1.15f,
                                    ),
                            ),
                    )
            }
        val vm = PracticeViewModel(task, references, checker, showCheckDiagnostics = true)
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.onNodeWithTag("practice-pad").bringIntoViewIfNeeded().performTouchInput {
            down(center)
            moveTo(Offset(center.x + 20, center.y))
            up()
        }
        compose.waitUntil(5_000) {
            vm.uiState.value.practice.cells[0].status == EntryStatus.Correct
        }
        compose.onNodeWithTag("kanji-cell-0").bringIntoViewIfNeeded().performClick()
        compose.waitUntil(5_000) { vm.uiState.value.debugCompensation != null }
        compose.onNodeWithTag("check-indicator-Shape").performClick()
        compose.onNodeWithTag("check-details-dialog").assertIsDisplayed()
        compose.onNodeWithTag("open-debug").assertDoesNotExist()
        compose.onNodeWithTag("practice-debug-feedback").assertDoesNotExist()
        compose.onNodeWithTag("close-check-details").performClick()
    }

    @Test
    fun tracingHintShowsBrieflyAndAllowsInkUndoAndClearWithoutLosingUsage() {
        val vm =
            PracticeViewModel(
                task,
                references,
                // Incorrect avoids auto-advance so hint stays on the same cell for undo/clear.
                KanjiChecker { CheckVerdict.Incorrect },
                // Keep the guide visible while exercising ink/undo/clear.
                fullHintDisplayMillis = { 20_000 },
            )
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.waitUntil(5_000) { vm.uiState.value.practice.selected?.reference != null }
        compose
            .onNodeWithTag("word-reading")
            .assert(hasContentDescription("Произношение скрыто. Нажмите, чтобы открыть"))
        val hint = compose.onNodeWithTag("trace-hint")
        // Full-character control is a button, not a latched checkbox.
        hint.bringIntoViewIfNeeded().performClick()
        compose.runOnIdle {
            assertNotNull(vm.uiState.value.hint)
            assertTrue(vm.uiState.value.practice.selected!!.hintUsed)
            assertFalse(vm.uiState.value.readingRevealed)
            assertFalse(vm.uiState.value.practice.isComplete)
        }
        // Trace hint does not open the reading spoiler.
        compose
            .onNodeWithTag("word-reading")
            .assert(hasContentDescription("Произношение скрыто. Нажмите, чтобы открыть"))
        compose
            .onNodeWithTag("practice-pad")
            .assertIsEnabled()
            .bringIntoViewIfNeeded()
            .performTouchInput {
                down(center)
                moveTo(Offset(center.x + 20, center.y))
                up()
            }
        compose.waitUntil(5_000) {
            vm.uiState.value.practice.selected!!.status == EntryStatus.Incorrect
        }
        compose.runOnIdle {
            assertEquals(1, vm.uiState.value.practice.selected!!.sample.strokeCount)
            assertNotNull(vm.uiState.value.hint)
            vm.setActive(false)
            assertNotNull(vm.uiState.value.hint)
            vm.setActive(true)
        }
        compose.onNodeWithContentDescription("Очистить знак").bringIntoViewIfNeeded().performClick()
        compose.runOnIdle {
            assertEquals(0, vm.uiState.value.practice.selected!!.sample.strokeCount)
            assertNotNull(vm.uiState.value.hint)
        }
        compose.onNodeWithContentDescription("Отменить последнее действие").performClick()
        compose.runOnIdle {
            assertEquals(1, vm.uiState.value.practice.selected!!.sample.strokeCount)
            assertTrue(vm.uiState.value.practice.selected!!.hintUsed)
        }
        compose.onNodeWithTag("next-kanji").assertDoesNotExist()
        compose.onNodeWithTag("kanji-cell-1").bringIntoViewIfNeeded().performClick()
        compose.runOnIdle {
            assertEquals(1, vm.uiState.value.practice.selectedIndex)
            assertNull(vm.uiState.value.hint)
            assertFalse(vm.uiState.value.practice.selected!!.hintUsed)
        }
    }

    @Test
    fun fullCharacterHintAutoHidesAfterConfiguredDelay() {
        val vm =
            PracticeViewModel(
                task,
                references,
                KanjiChecker { CheckVerdict.Incorrect },
                fullHintDisplayMillis = { 200 },
            )
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.waitUntil(5_000) { vm.uiState.value.practice.selected?.reference != null }
        compose.onNodeWithTag("trace-hint").bringIntoViewIfNeeded().performClick()
        compose.runOnIdle { assertNotNull(vm.uiState.value.hint) }
        compose.waitUntil(2_000) { vm.uiState.value.hint == null }
        compose.runOnIdle { assertTrue(vm.uiState.value.practice.selected!!.hintUsed) }
    }

    @Test
    fun tappingReadingSpoilerRevealsPronunciationAndCountsAsHint() {
        val vm =
            PracticeViewModel(
                task,
                references,
                KanjiChecker { CheckVerdict.Incorrect },
            )
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.waitUntil(5_000) { vm.uiState.value.practice.selected?.reference != null }
        compose
            .onNodeWithTag("word-reading")
            .assert(hasContentDescription("Произношение скрыто. Нажмите, чтобы открыть"))
            .performClick()
        compose.runOnIdle {
            assertTrue(vm.uiState.value.readingRevealed)
            assertTrue(vm.uiState.value.practice.readingHintUsed)
            // Reading help is word-level; it must not mark the selected kanji cell.
            assertFalse(vm.uiState.value.practice.selected!!.hintUsed)
            assertNull(vm.uiState.value.hint)
        }
        compose.onNodeWithTag("word-reading").assert(hasContentDescription("としょかん"))
        compose.onNodeWithTag("kanji-cell-1").bringIntoViewIfNeeded().performClick()
        compose.runOnIdle {
            assertEquals(1, vm.uiState.value.practice.selectedIndex)
            assertTrue(vm.uiState.value.readingRevealed)
            assertTrue(vm.uiState.value.practice.readingHintUsed)
            assertFalse(vm.uiState.value.practice.selected!!.hintUsed)
        }
    }

    @Test
    fun longTimedPromptFitsAtMostTwoLinesAboveTools() {
        val word = mutableStateOf("Библиотека")
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(392.dp, 844.dp)) {
                    val timed = task.copy(russianWord = word.value, timeLimitMs = 300_000)
                    PracticeScreen(PracticeUiState(HandwritingTraining(timed).state), {}, {})
                }
            }
        }
        fun padAboveHint() {
            val pad = compose.onNodeWithTag("practice-pad").fetchSemanticsNode().boundsInRoot
            val hint = compose.onNodeWithTag("trace-hint").fetchSemanticsNode().boundsInRoot
            assertTrue(pad.bottom <= hint.top + 1f)
        }
        padAboveHint()
        compose.runOnIdle { word.value = "Электроэнцефалографическое исследование" }
        compose.onNodeWithTag("word-prompt").performSemanticsAction(
            SemanticsActions.GetTextLayoutResult,
        ) { action ->
            val results = mutableListOf<TextLayoutResult>()
            assertTrue(action(results))
            assertTrue(results.first().lineCount <= 2)
        }
        padAboveHint()
        compose.onNodeWithContentDescription("Осталось 05:00").assertIsDisplayed()
        compose.onNodeWithTag("trace-hint").assertIsDisplayed()
        compose.onNodeWithTag("open-debug").assertDoesNotExist()
    }

    @Test
    fun backgroundTimeIsIncludedInWordAndSelectedCell() {
        var now = 0L
        val vm =
            PracticeViewModel(
                task,
                references,
                KanjiChecker { CheckVerdict.Unavailable },
                clock = { now },
            )
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.runOnIdle {
            now = 1_000
            vm.setActive(false)
            assertEquals(1_000L, vm.uiState.value.practice.elapsedMs)
            now = 10_000
            vm.setActive(true)
            now = 11_000
            vm.setActive(false)
            assertEquals(11_000L, vm.uiState.value.practice.elapsedMs)
            assertEquals(11_000L, vm.uiState.value.practice.cells.first().elapsedMs)
        }
    }

    @Test
    fun expiredBackgroundDeadlineDeliversTimeoutOnceOnResume() {
        var now = 0L
        var deliveries = 0
        var result: PracticeResult? = null
        val vm =
            PracticeViewModel(
                task.copy(timeLimitMs = 5_000),
                references,
                KanjiChecker { CheckVerdict.Unavailable },
                clock = { now },
            )
        compose.setContent {
            MaterialTheme {
                PracticeRoute(
                    vm,
                    {
                        deliveries++
                        result = it
                    },
                    {},
                )
            }
        }
        compose.waitUntil(5_000) { vm.uiState.value.practice.selected?.reference != null }
        compose.runOnIdle {
            now = 1_000
            vm.setActive(false)
            now = 10_000
            vm.setActive(true)
            assertNull(vm.uiState.value.practice.result)
            assertTrue(vm.uiState.value.practice.timedOut)
            assertFalse(vm.uiState.value.practice.readingHintUsed)
            assertTrue(vm.uiState.value.readingRevealed)
            assertTrue(vm.uiState.value.practice.isResolved)
            assertEquals(5_000L, vm.uiState.value.practice.elapsedMs)
            assertEquals(5_000L, vm.uiState.value.practice.cells.first().elapsedMs)
            assertTrue(
                vm.uiState.value.practice.cells
                    .filter { it.task.mode == EntryMode.User }
                    .all { it.status == EntryStatus.Incorrect },
            )
            assertNotNull(vm.uiState.value.hint)
            assertEquals(HintOverlayStyle.Exhausted, vm.uiState.value.hint!!.style)
            assertEquals(0, vm.uiState.value.practice.selectedIndex)
        }
        compose.onNodeWithTag("kanji-cell-1").bringIntoViewIfNeeded().performClick()
        compose.runOnIdle {
            assertEquals(1, vm.uiState.value.practice.selectedIndex)
            assertNotNull(vm.uiState.value.hint)
            assertEquals(HintOverlayStyle.Exhausted, vm.uiState.value.hint!!.style)
        }
        compose.onNodeWithTag("kanji-cell-0").bringIntoViewIfNeeded().performClick()
        compose.runOnIdle { assertEquals(0, vm.uiState.value.practice.selectedIndex) }
        compose.onNodeWithText("Далее").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(1, deliveries)
            assertEquals(AnswerStatus.TimedOut, result!!.status)
            assertFalse(result!!.readingHintUsed)
            assertEquals(5_000L, result!!.elapsedMs)
        }
        compose.runOnIdle {
            vm.setActive(false)
            now = 20_000
            vm.setActive(true)
            vm.deliverResult {
                deliveries++
                result = it
            }
            assertEquals(1, deliveries)
        }
    }

    @Test
    fun skipActionStillDeliversTypedResultOnce() {
        var deliveries = 0
        var result: PracticeResult? = null
        val vm = PracticeViewModel(task, references, KanjiChecker { CheckVerdict.Unavailable })
        compose.setContent {
            MaterialTheme {
                PracticeRoute(
                    vm,
                    {
                        deliveries++
                        result = it
                    },
                    {},
                )
            }
        }
        compose.onNodeWithText("Пропустить").assertDoesNotExist()
        compose.runOnIdle { vm.onAction(PracticeAction.Skip) }
        compose.runOnIdle {
            assertEquals(1, deliveries)
            assertEquals("test-word", result!!.taskId)
            assertEquals("Библиотека", result!!.russianWord)
            assertEquals(AnswerStatus.Skipped, result!!.status)
            assertEquals(EntryStatus.Prefilled, result!!.kanji.last().status)
            assertEquals(0L, result!!.kanji.last().elapsedMs)
        }
    }

    @Test
    fun headerShowsProgressAndBottomHasHintInsteadOfSkip() {
        compose.setContent {
            MaterialTheme {
                PracticeScreen(
                    PracticeUiState(HandwritingTraining(task).state),
                    {},
                    {},
                    progressLabel = "2 из 90",
                )
            }
        }
        compose.onNodeWithTag("word-progress").assertTextEquals("Практика 2 из 90")
        compose.onNodeWithText("Практика 2 из 90").assertIsDisplayed()
        compose.onNodeWithTag("trace-hint").assertIsDisplayed()
        compose.onNodeWithText("Пропустить").assertDoesNotExist()
        compose.onNodeWithText("Далее").assertIsDisplayed()
    }

    @Test
    fun translationClueModeHidesTranslationAndShowsReading() {
        val translationTask = task.copy(clueMode = ru.mobui.kanji.domain.practice.TaskClueMode.Translation)
        val vm =
            PracticeViewModel(
                translationTask,
                references,
                KanjiChecker { CheckVerdict.Incorrect },
            )
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.waitUntil(5_000) { vm.uiState.value.practice.selected?.reference != null }
        compose.onNodeWithTag("word-reading").assert(hasContentDescription("としょかん"))
        compose
            .onNodeWithTag("word-prompt")
            .assert(hasContentDescription("Перевод скрыт. Нажмите, чтобы открыть"))
            .performClick()
        compose.runOnIdle {
            assertTrue(vm.uiState.value.translationRevealed)
            assertTrue(vm.uiState.value.practice.translationHintUsed)
            assertFalse(vm.uiState.value.readingRevealed.not())
            assertFalse(vm.uiState.value.practice.readingHintUsed)
        }
    }

    @Test
    fun noneClueModeShowsBothTranslationAndReadingDirectly() {
        val noneTask = task.copy(clueMode = ru.mobui.kanji.domain.practice.TaskClueMode.None)
        val vm =
            PracticeViewModel(
                noneTask,
                references,
                KanjiChecker { CheckVerdict.Incorrect },
            )
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.waitUntil(5_000) { vm.uiState.value.practice.selected?.reference != null }
        compose.onNodeWithTag("word-reading").assert(hasContentDescription("としょかん"))
        compose.onNodeWithTag("word-prompt").assert(hasText("Библиотека"))
        compose.runOnIdle {
            assertTrue(vm.uiState.value.readingRevealed)
            assertTrue(vm.uiState.value.translationRevealed)
            assertFalse(vm.uiState.value.practice.readingHintUsed)
            assertFalse(vm.uiState.value.practice.translationHintUsed)
        }
    }
}
