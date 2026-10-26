package ru.mobui.kanji.feature.practice

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.*
import ru.mobui.kanji.domain.practice.*

class PracticeScreenTest {
    private fun SemanticsNodeInteraction.bringIntoViewIfNeeded(): SemanticsNodeInteraction =
        if (isDisplayed()) this else performScrollTo()

    @get:Rule val compose = createComposeRule()
    private val task = PracticeTask("test-word", "Библиотека", listOf(
        KanjiTask("first", KanjiId("u56f3"), EntryMode.User),
        KanjiTask("second", KanjiId("u66f8"), EntryMode.User),
        KanjiTask("third", KanjiId("u9928"), EntryMode.Prefilled),
    ))
    private val references = KanjiReferenceSource {
        ReferenceDrawing(listOf(listOf(ReferencePoint(.2f, .2f), ReferencePoint(.8f, .8f))))
    }

    @Test fun checkIndicatorsExposeIndependentResultsInsteadOfAggregateStatus() {
        val state = PracticeUiState(PracticeSession(task).state, currentCheckReport = CheckReport(
            CheckVerdict.Incorrect, checks = listOf(
                IndividualCheckResult(CheckKind.Readability, CheckVerdict.Correct, "日"),
                IndividualCheckResult(CheckKind.Shape, CheckVerdict.Incorrect, matchPercent = 68f),
                IndividualCheckResult(CheckKind.StrokeWriting, null),
            ),
        ))
        compose.setContent { MaterialTheme { PracticeScreen(state, {}, {}) } }
        compose.onNodeWithTag("check-indicator-Readability").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Пройдена: 日"))
        compose.onNodeWithTag("check-indicator-Shape").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Не пройдена: 68%"))
        compose.onNodeWithTag("check-indicator-StrokeWriting").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Выключено: 0"))
        compose.onNodeWithText("Знак 1 из 3").assertDoesNotExist()
        compose.onNodeWithText("Неверно").assertDoesNotExist()
    }

    @Test fun mismatchedReadabilityStillShowsRecognizedCharacterWithoutStatusSymbols() {
        val report = CheckReport(CheckVerdict.Incorrect, checks = listOf(
            IndividualCheckResult(CheckKind.Readability, CheckVerdict.Incorrect, "月"),
            IndividualCheckResult(CheckKind.Shape, CheckVerdict.Incorrect, matchPercent = 68f),
            IndividualCheckResult(CheckKind.StrokeWriting, CheckVerdict.Correct),
        ))
        compose.setContent { MaterialTheme { CheckerTestingStatuses(report, false, strokeCount = 4) } }
        compose.onNodeWithTag("check-indicator-Readability").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Не пройдена: 月"))
        compose.onNodeWithTag("check-indicator-Shape").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Не пройдена: 68%"))
        compose.onNodeWithTag("check-indicator-StrokeWriting").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Пройдена: 4"))
    }

    @Test fun clickingEachCheckOpensOnlyItsDetailsAndCanDismiss() {
        val reference = ReferenceDrawing(List(4) { listOf(ReferencePoint(.2f, .2f), ReferencePoint(.8f, .8f)) })
        val ink = WritingSample(List(4) { InkStroke(listOf(InkPoint(.2f, .2f, 0), InkPoint(.8f, .8f, 100)), InkTool.Finger) })
        val initial = PracticeSession(task).state
        val practice = initial.copy(cells = initial.cells.mapIndexed { index, cell ->
            if (index == 0) cell.copy(sample = ink, reference = reference) else cell
        })
        val report = CheckReport(CheckVerdict.Incorrect, checks = listOf(
            IndividualCheckResult(CheckKind.Readability, CheckVerdict.Incorrect, "月"),
            IndividualCheckResult(CheckKind.Shape, CheckVerdict.Correct, matchPercent = 92f),
            IndividualCheckResult(CheckKind.StrokeWriting, CheckVerdict.Incorrect, failures = listOf(
                CheckFailure.StrokeOrder(listOf(StrokeOrderMismatch(2, 3), StrokeOrderMismatch(3, 2))),
            )),
        ))
        compose.setContent { MaterialTheme { PracticeScreen(PracticeUiState(practice, currentCheckReport = report), {}, {}) } }
        compose.onNodeWithTag("check-indicator-Readability").performClick()
        compose.onNodeWithTag("check-details-dialog").assertIsDisplayed()
        compose.onNodeWithText("Распознано").assertIsDisplayed()
        compose.onNodeWithText("Задано").assertDoesNotExist()
        compose.onNodeWithText("月").assertIsDisplayed()
        compose.onNodeWithTag("close-check-details").performClick()
        compose.onNodeWithTag("check-indicator-Shape").performClick()
        compose.onNodeWithText("Соответствие").assertIsDisplayed()
        compose.onNodeWithTag("close-check-details").performClick()
        compose.onNodeWithTag("check-indicator-StrokeWriting").performClick()
        compose.onNodeWithText("4 из 4").assertIsDisplayed()
        compose.onNodeWithText("Нарушен").assertIsDisplayed()
        compose.onNodeWithText("Порядок").assertIsDisplayed()
        compose.onNodeWithTag("close-check-details").performClick()
        compose.onNodeWithTag("check-details-dialog").assertDoesNotExist()
    }

    @Test fun incompleteWritingDoesNotClaimThatOrderAndDirectionPassed() {
        val initial = PracticeSession(task).state
        val reference = ReferenceDrawing(List(4) { listOf(ReferencePoint(.2f, .2f), ReferencePoint(.8f, .8f)) })
        val cell = initial.selected!!.copy(reference = reference)
        val report = CheckReport(CheckVerdict.Incomplete, checks = listOf(
            IndividualCheckResult(CheckKind.StrokeWriting, CheckVerdict.Incomplete),
        ))
        compose.setContent { MaterialTheme { CheckDetailsDialog(CheckKind.StrokeWriting, report, cell, {}) } }
        compose.onAllNodesWithText("Не проверено").assertCountEquals(2)
        compose.onNodeWithText("Пройдена").assertDoesNotExist()
    }

    @Test fun headerShowsOnlyElapsedTimeWithoutLimitAndNoInstructionOrButtons() {
        val initial = PracticeSession(task).state.copy(elapsedMs = 12_800)
        compose.setContent { MaterialTheme { PracticeScreen(PracticeUiState(initial, debugEnabled = true), {}, {}) } }
        compose.onNodeWithTag("header-timer").assertTextEquals("00:12")
        compose.onNodeWithTag("open-checker-settings").assertDoesNotExist()
        compose.onNodeWithTag("open-debug").assertDoesNotExist()
        compose.onNodeWithTag("compensated-drawing").assertDoesNotExist()
        compose.onNodeWithText("Выберите клетку и пишите").assertDoesNotExist()
        compose.onNodeWithTag("reading-hints-space").assertExists()
    }

    @Test fun countdownShowsTimeOnlyAndReachesZeroAtDeadline() {
        val initial = PracticeSession(task.copy(timeLimitMs = 100_000)).state
        val elapsed = mutableStateOf(80_001L)
        compose.setContent { MaterialTheme { PracticeScreen(PracticeUiState(initial.copy(elapsedMs = elapsed.value)), {}, {}) } }
        compose.onNodeWithTag("header-timer").assertTextEquals("00:20")
        compose.runOnIdle { elapsed.value = 99_999 }
        compose.onNodeWithTag("header-timer").assertTextEquals("00:01")
        compose.runOnIdle { elapsed.value = 100_000 }
        compose.onNodeWithTag("header-timer").assertTextEquals("00:00")
    }

    @Test fun changingDisplayedWordPreservesInkAndSession() {
        val word = mutableStateOf("Electroencephalography")
        val vm = PracticeViewModel(task, references, KanjiChecker { CheckVerdict.Incorrect })
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}, wordLabel = word.value) } }
        compose.onNodeWithText("Electroencephalography").assertExists()
        compose.onNodeWithTag("practice-pad").performTouchInput {
            down(center); moveTo(Offset(center.x + 20, center.y)); up()
        }
        compose.runOnIdle { word.value = "Электроэнцефалография" }
        compose.onNodeWithText("Электроэнцефалография").assertExists()
        compose.runOnIdle { assertEquals(1, vm.uiState.value.practice.selected!!.sample.strokeCount) }
    }

    @Test fun debugButtonShowsActualCompensatedInkInRedAndClearsOnNewGesture() {
        val drawing = ComparisonDrawing(listOf(listOf(ComparisonPoint(.2f, .2f), ComparisonPoint(.8f, .8f))))
        val checker = object : KanjiChecker {
            override suspend fun check(request: CheckRequest) = CheckVerdict.Incorrect
            override suspend fun checkWithDetails(request: CheckRequest) = CheckReport(CheckVerdict.Incorrect, compensatedDrawing = drawing)
        }
        val vm = PracticeViewModel(task, references, checker, showCheckDiagnostics = true)
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.onNodeWithTag("check-indicator-Shape").performClick()
        val button = compose.onNodeWithTag("compensated-drawing")
        button.bringIntoViewIfNeeded().assertIsNotEnabled()
        compose.onNodeWithTag("close-check-details").performClick()
        compose.onNodeWithTag("practice-pad").performTouchInput { down(center); moveTo(Offset(center.x + 20, center.y)); up() }
        compose.waitUntil(5_000) { vm.uiState.value.debugReport?.compensatedDrawing != null }
        compose.onNodeWithTag("check-indicator-Shape").performClick()
        button.bringIntoViewIfNeeded().assertIsEnabled().performClick().assertIsOn()
        compose.onNodeWithTag("close-check-details").performClick()
        compose.runOnIdle { assertTrue(vm.uiState.value.showCompensatedDrawing) }
        val pixels = compose.onNodeWithTag("practice-pad").captureToImage().toPixelMap()
        assertTrue((0 until pixels.width).any { x -> (0 until pixels.height).any { y ->
            val color = pixels[x, y]
            color.red > .9f && color.green < .1f && color.blue < .1f
        } })
        compose.onNodeWithTag("practice-pad").performTouchInput { down(center) }
        compose.runOnIdle { assertFalse(vm.uiState.value.showCompensatedDrawing) }
        compose.onNodeWithTag("check-details-dialog").assertDoesNotExist()
        compose.onNodeWithTag("practice-pad").performTouchInput { cancel() }
    }

    @Test fun settingsChangeRechecksAcceptedInkAndWritingRulesToggleIndependently() {
        val settings = mutableStateOf(RecognitionSettings())
        val checker = object : KanjiChecker {
            override suspend fun check(request: CheckRequest) = if (settings.value.requiredMatchPercent > 90f) CheckVerdict.Incorrect else CheckVerdict.Correct
        }
        val vm = PracticeViewModel(task, references, checker, showCheckDiagnostics = true)
        compose.setContent { MaterialTheme {
            PracticeRoute(vm, {}, {}, settings.value, { settings.value = it; vm.onCheckerSettingsChanged() },
                { settings.value = RecognitionSettings(); vm.onCheckerSettingsChanged() })
        } }
        compose.onNodeWithTag("practice-pad").performTouchInput { down(center); moveTo(Offset(center.x + 20, center.y)); up() }
        compose.waitUntil(5_000) { vm.uiState.value.practice.cells[0].status == EntryStatus.Correct }
        val oldRevision = vm.uiState.value.practice.cells[0].revision
        val oldInk = vm.uiState.value.practice.cells[0].sample
        compose.onNodeWithTag("check-indicator-Shape").performClick()
        compose.onNodeWithTag("open-checker-settings").bringIntoViewIfNeeded().performClick()
        compose.onNodeWithTag("parameter-match").bringIntoViewIfNeeded().performSemanticsAction(SemanticsActions.SetProgress) { it(95.4f) }
        compose.waitUntil(5_000) { vm.uiState.value.practice.cells[0].status == EntryStatus.Incorrect }
        compose.runOnIdle {
            assertEquals(95f, settings.value.requiredMatchPercent, 0f)
            assertTrue(vm.uiState.value.practice.cells[0].revision > oldRevision)
            assertSame(oldInk, vm.uiState.value.practice.cells[0].sample)
        }
        compose.onNodeWithTag("parameter-jitter").bringIntoViewIfNeeded()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(.56f) }
        compose.runOnIdle { assertEquals(.6f, settings.value.jitterTolerancePercent, .000001f) }
        val rotation = compose.onNodeWithTag("parameter-rotation").bringIntoViewIfNeeded()
        rotation.performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        compose.runOnIdle {
            assertEquals(0f, settings.value.maxRotationDegrees, 0f)
            assertFalse(CompensationStage.Rotation in settings.value.enabledCompensations)
        }
        rotation.performSemanticsAction(SemanticsActions.SetProgress) { it(4f) }
        compose.runOnIdle { assertTrue(CompensationStage.Rotation in settings.value.enabledCompensations) }
        compose.onNodeWithTag("enable-writing-off").bringIntoViewIfNeeded().performClick().assertIsSelected()
        compose.runOnIdle { assertFalse(settings.value.strokeWritingEnabled) }
        compose.onNodeWithTag("enable-readability-off").bringIntoViewIfNeeded().performClick().assertIsSelected()
        compose.runOnIdle { assertFalse(settings.value.readabilityEnabled); assertTrue(settings.value.shapeEnabled) }
        compose.onNodeWithTag("reset-checker-settings").performClick()
        compose.waitUntil(5_000) { vm.uiState.value.practice.cells[0].status == EntryStatus.Correct }
        compose.runOnIdle { assertTrue(settings.value.strokeWritingEnabled); assertTrue(settings.value.readabilityEnabled) }
    }

    @Test fun historiesAreIndependentAndStubDoesNotEnableCompletion() {
        val vm = PracticeViewModel(task, references, KanjiChecker { CheckVerdict.Unavailable })
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.onNodeWithTag("kanji-cell-2").assertIsNotEnabled()
        compose.onNodeWithText("Напишите кандзи").assertDoesNotExist()
        compose.onNodeWithText("Далее").assertIsNotEnabled()
        compose.onNodeWithTag("practice-pad").performTouchInput {
            down(center); moveTo(Offset(center.x + 20, center.y)); up()
        }
        compose.onNodeWithContentDescription("Отменить последнее действие").bringIntoViewIfNeeded().assertIsEnabled()
        compose.waitUntil(5_000) { vm.uiState.value.notice == PracticeNotice.CheckerUnavailable }
        compose.onNodeWithTag("practice-notice").assertIsDisplayed()
        compose.onNodeWithText("Проверить").assertDoesNotExist()
        compose.onNodeWithText("Далее").assertIsNotEnabled()
        compose.onNodeWithTag("kanji-cell-1").bringIntoViewIfNeeded().performClick()
        compose.onNodeWithContentDescription("Отменить последнее действие").bringIntoViewIfNeeded().assertIsNotEnabled()
        compose.onNodeWithTag("kanji-cell-0").bringIntoViewIfNeeded().performClick()
        compose.onNodeWithContentDescription("Отменить последнее действие").bringIntoViewIfNeeded().performClick()
        compose.runOnIdle { assertEquals(EntryStatus.Empty, vm.uiState.value.practice.cells.first().status) }
        compose.onNodeWithTag("practice-notice").assertIsDisplayed()
    }

    @Test fun nextIsEnabledOnlyAfterEveryRequiredSignIsAutomaticallyAccepted() {
        var result: PracticeResult? = null
        val vm = PracticeViewModel(task, references, KanjiChecker { request ->
            if (request.occurrenceId == "first" && request.sample.strokeCount == 1) CheckVerdict.Incorrect
            else CheckVerdict.Correct
        })
        compose.setContent { MaterialTheme { PracticeRoute(vm, { result = it }, {}) } }
        fun drawStroke() {
            compose.onNodeWithTag("practice-pad").bringIntoViewIfNeeded().performTouchInput {
                down(center); moveTo(Offset(center.x + 20, center.y)); up()
            }
        }
        drawStroke()
        compose.waitUntil(5_000) { vm.uiState.value.practice.cells[0].status == EntryStatus.Incorrect }
        compose.onNodeWithContentDescription("Знак 1, Неверно").assertExists()
        compose.onNodeWithTag("practice-debug-feedback").assertDoesNotExist()
        compose.onNodeWithText("Далее").assertIsNotEnabled()
        drawStroke()
        compose.waitUntil(5_000) { vm.uiState.value.practice.cells[0].status == EntryStatus.Correct }
        compose.onNodeWithTag("practice-pad").bringIntoViewIfNeeded().performTouchInput { down(center) }
        compose.onNodeWithText("Ввод").assertDoesNotExist()
        compose.onNodeWithContentDescription("Знак 1, Не проверен").assertExists()
        compose.onNodeWithTag("practice-pad").performTouchInput { cancel() }
        compose.onNodeWithContentDescription("Знак 1, Верно").assertExists()
        compose.onNodeWithText("Далее").assertIsNotEnabled()
        compose.onNodeWithTag("next-kanji").assertDoesNotExist()
        compose.onNodeWithTag("kanji-cell-1").bringIntoViewIfNeeded().performClick()
        drawStroke()
        compose.waitUntil(5_000) { vm.uiState.value.practice.isComplete && !vm.uiState.value.isChecking }
        compose.onNodeWithText("Далее").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(AnswerStatus.Correct, result!!.status) }
    }

    @Test fun incompleteSignStaysNeutralAndDoesNotEnableNext() {
        val vm = PracticeViewModel(task, references, KanjiChecker { CheckVerdict.Incomplete })
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.onNodeWithTag("practice-pad").bringIntoViewIfNeeded().performTouchInput {
            down(center); moveTo(Offset(center.x + 20, center.y)); up()
        }
        compose.waitUntil(5_000) { vm.uiState.value.notice == PracticeNotice.Incomplete }
        compose.onNodeWithTag("checker-testing-statuses").assertIsDisplayed()
        compose.onNodeWithText("Неверно").assertDoesNotExist()
        compose.onNodeWithTag("practice-notice").assertDoesNotExist()
        compose.onNodeWithText("Далее").assertIsNotEnabled()
    }

    @Test fun debugFailureExplainsSelectedRevisionAndClearsOnNewInput() {
        val checker = object : KanjiChecker {
            override suspend fun check(request: CheckRequest) = CheckVerdict.Incorrect
            override suspend fun checkWithDetails(request: CheckRequest) = CheckReport(
                CheckVerdict.Incorrect, listOf(CheckFailure.StrokeCount(request.sample.strokeCount, 5)),
            )
        }
        val vm = PracticeViewModel(task, references, checker, showCheckDiagnostics = true)
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.onNodeWithTag("practice-pad").bringIntoViewIfNeeded().performTouchInput {
            down(center); moveTo(Offset(center.x + 20, center.y)); up()
        }
        compose.waitUntil(5_000) { vm.uiState.value.debugFailures.isNotEmpty() }
        val bounds = compose.onNodeWithTag("practice-pad").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("practice-debug-feedback").assertDoesNotExist()
        compose.onNodeWithTag("check-indicator-Shape").performClick()
        compose.onNodeWithTag("open-debug").bringIntoViewIfNeeded().performClick()
        compose.onNodeWithTag("practice-debug-feedback").assertIsDisplayed()
            .assertTextContains("Штрихов: 1, нужно 5", substring = true)
        compose.onNodeWithTag("close-debug").performClick()
        compose.onNodeWithTag("debug-dialog").assertDoesNotExist()
        assertEquals(bounds, compose.onNodeWithTag("practice-pad").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithTag("practice-pad").performTouchInput { down(center) }
        compose.onNodeWithTag("practice-debug-feedback").assertDoesNotExist()
        compose.onNodeWithTag("practice-pad").performTouchInput { cancel() }
        compose.onNodeWithTag("kanji-cell-1").bringIntoViewIfNeeded().performClick()
        compose.onNodeWithTag("practice-debug-feedback").assertDoesNotExist()
    }

    @Test fun debugCompensationIsVisibleForCorrectAnswerAndClearsWhenSelectionChanges() {
        val checker = object : KanjiChecker {
            override suspend fun check(request: CheckRequest) = CheckVerdict.Correct
            override suspend fun checkWithDetails(request: CheckRequest) = CheckReport(
                CheckVerdict.Correct, compensation = CheckCompensation(rotation = ru.mobui.kanji.domain.practice.RotationCompensation(degrees = -7f), stretch = ru.mobui.kanji.domain.practice.StretchCompensation(scaleX = 1.15f)),
            )
        }
        val vm = PracticeViewModel(task, references, checker, showCheckDiagnostics = true)
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.onNodeWithTag("practice-pad").bringIntoViewIfNeeded().performTouchInput {
            down(center); moveTo(Offset(center.x + 20, center.y)); up()
        }
        compose.waitUntil(5_000) { vm.uiState.value.debugCompensation != null }
        compose.onNodeWithTag("check-indicator-Shape").performClick()
        compose.onNodeWithTag("open-debug").bringIntoViewIfNeeded().performClick()
        compose.onNodeWithTag("practice-debug-feedback").assertIsDisplayed()
            .assertTextContains("Компенсация поворота:", substring = true)
            .assertTextContains("Коррекция масштаба X:", substring = true)
        compose.runOnIdle { assertEquals(EntryStatus.Correct, vm.uiState.value.practice.selected!!.status) }
        compose.onNodeWithTag("debug-drag-handle").performTouchInput { swipeDown() }
        compose.onNodeWithTag("debug-dialog").assertDoesNotExist()
        compose.onNodeWithTag("kanji-cell-1").bringIntoViewIfNeeded().performClick()
        compose.onNodeWithTag("practice-debug-feedback").assertDoesNotExist()
    }

    @Test fun tracingHintTogglesAndAllowsInkUndoAndClearWithoutLosingUsage() {
        val vm = PracticeViewModel(task, references, KanjiChecker { CheckVerdict.Correct }, hintAlgorithm = TraceReferenceHint())
        compose.setContent { MaterialTheme { PracticeRoute(vm, {}, {}) } }
        compose.waitUntil(5_000) { vm.uiState.value.practice.selected?.reference != null }
        val hint = compose.onNodeWithTag("trace-hint")
        hint.bringIntoViewIfNeeded().assertIsOff().performClick().assertIsOn()
        compose.runOnIdle {
            assertNotNull(vm.uiState.value.hint)
            assertTrue(vm.uiState.value.practice.selected!!.hintUsed)
            assertFalse(vm.uiState.value.practice.isComplete)
        }
        compose.onNodeWithTag("practice-pad").assertIsEnabled().bringIntoViewIfNeeded().performTouchInput {
            down(center); moveTo(Offset(center.x + 20, center.y)); up()
        }
        compose.waitUntil(5_000) { vm.uiState.value.practice.selected!!.status == EntryStatus.Correct }
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
        compose.runOnIdle { assertEquals(1, vm.uiState.value.practice.selected!!.sample.strokeCount) }
        hint.performClick().assertIsOff()
        compose.runOnIdle {
            assertNull(vm.uiState.value.hint)
            assertTrue(vm.uiState.value.practice.selected!!.hintUsed)
        }
        hint.performClick().assertIsOn()
        compose.onNodeWithTag("next-kanji").assertDoesNotExist()
        compose.onNodeWithTag("kanji-cell-1").bringIntoViewIfNeeded().performClick()
        compose.runOnIdle {
            assertEquals(1, vm.uiState.value.practice.selectedIndex)
            assertNull(vm.uiState.value.hint)
            assertFalse(vm.uiState.value.practice.selected!!.hintUsed)
        }
    }

    @Test fun multilineTimedPromptDoesNotMoveBottomAnchoredPad() {
        val word = mutableStateOf("Библиотека")
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(392.dp, 844.dp)) {
                    val timed = task.copy(russianWord = word.value, timeLimitMs = 300_000)
                    PracticeScreen(PracticeUiState(PracticeSession(timed).state), {}, {})
                }
            }
        }
        val original = compose.onNodeWithTag("practice-pad").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { word.value = "Электроэнцефалографическое исследование" }
        compose.onNodeWithText("Электроэнцефалографическое исследование").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            val results = mutableListOf<TextLayoutResult>()
            assertTrue(action(results))
            assertTrue(results.first().lineCount >= 2)
        }
        val moved = compose.onNodeWithTag("practice-pad").fetchSemanticsNode().boundsInRoot
        assertEquals(original.top, moved.top, .5f)
        assertEquals(original.bottom, moved.bottom, .5f)
        compose.onNodeWithContentDescription("Осталось 05:00").assertIsDisplayed()
        compose.onNodeWithTag("trace-hint").assertIsDisplayed()
        compose.onNodeWithTag("open-debug").assertDoesNotExist()
    }

    @Test fun backgroundTimeIsIncludedInWordAndSelectedCell() {
        var now = 0L
        val vm = PracticeViewModel(task, references, KanjiChecker { CheckVerdict.Unavailable }, clock = { now })
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

    @Test fun expiredBackgroundDeadlineDeliversTimeoutOnceOnResume() {
        var now = 0L
        var deliveries = 0
        var result: PracticeResult? = null
        val vm = PracticeViewModel(task.copy(timeLimitMs = 5_000), references,
            KanjiChecker { CheckVerdict.Unavailable }, clock = { now })
        compose.setContent { MaterialTheme { PracticeRoute(vm, { deliveries++; result = it }, {}) } }
        compose.runOnIdle {
            now = 1_000
            vm.setActive(false)
            now = 10_000
            vm.setActive(true)
            assertEquals(AnswerStatus.TimedOut, vm.uiState.value.practice.result!!.status)
            assertEquals(5_000L, vm.uiState.value.practice.elapsedMs)
            assertEquals(5_000L, vm.uiState.value.practice.cells.first().elapsedMs)
        }
        compose.runOnIdle {
            vm.setActive(false)
            now = 20_000
            vm.setActive(true)
            vm.deliverResult { deliveries++; result = it }
            assertEquals(1, deliveries)
            assertEquals(AnswerStatus.TimedOut, result!!.status)
            assertEquals(5_000L, result!!.elapsedMs)
        }
    }

    @Test fun skipDeliversTypedResultOnce() {
        var deliveries = 0
        var result: PracticeResult? = null
        val vm = PracticeViewModel(task, references, KanjiChecker { CheckVerdict.Unavailable })
        compose.setContent { MaterialTheme { PracticeRoute(vm, { deliveries++; result = it }, {}) } }
        compose.onNodeWithText("Пропустить").performClick()
        compose.runOnIdle {
            assertEquals(1, deliveries)
            assertEquals("test-word", result!!.taskId)
            assertEquals("Библиотека", result!!.russianWord)
            assertEquals(AnswerStatus.Skipped, result!!.status)
            assertEquals(EntryStatus.Prefilled, result!!.kanji.last().status)
            assertEquals(0L, result!!.kanji.last().elapsedMs)
        }
    }
}
