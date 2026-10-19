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
        val button = compose.onNodeWithTag("compensated-drawing")
        button.bringIntoViewIfNeeded().assertIsNotEnabled()
        compose.onNodeWithTag("practice-pad").performTouchInput { down(center); moveTo(Offset(center.x + 20, center.y)); up() }
        compose.waitUntil(5_000) { vm.uiState.value.debugReport?.compensatedDrawing != null }
        button.bringIntoViewIfNeeded().assertIsEnabled().performClick().assertIsOn()
        compose.runOnIdle { assertTrue(vm.uiState.value.showCompensatedDrawing) }
        val pixels = compose.onNodeWithTag("practice-pad").captureToImage().toPixelMap()
        assertTrue((0 until pixels.width).any { x -> (0 until pixels.height).any { y ->
            val color = pixels[x, y]
            color.red > .9f && color.green < .1f && color.blue < .1f
        } })
        compose.onNodeWithTag("practice-pad").performTouchInput { down(center) }
        compose.runOnIdle { assertFalse(vm.uiState.value.showCompensatedDrawing) }
        button.assertIsOff().assertIsNotEnabled()
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
        compose.onNodeWithTag("check-order").bringIntoViewIfNeeded().performClick().assertIsSelected()
        compose.runOnIdle { assertTrue(settings.value.checkStrokeOrder); assertFalse(settings.value.checkStrokeDirection) }
        compose.onNodeWithTag("check-direction").bringIntoViewIfNeeded().performClick().assertIsSelected()
        compose.runOnIdle { assertTrue(settings.value.checkStrokeOrder); assertTrue(settings.value.checkStrokeDirection) }
        compose.onNodeWithTag("reset-checker-settings").performClick()
        compose.waitUntil(5_000) { vm.uiState.value.practice.cells[0].status == EntryStatus.Correct }
        compose.runOnIdle { assertFalse(settings.value.checkStrokeOrder); assertFalse(settings.value.checkStrokeDirection) }
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
        compose.onNodeWithText("Ввод").assertIsDisplayed()
        compose.onNodeWithContentDescription("Знак 1, Не проверен").assertExists()
        compose.onNodeWithTag("practice-pad").performTouchInput { cancel() }
        compose.onNodeWithContentDescription("Знак 1, Верно").assertExists()
        compose.onNodeWithText("Далее").assertIsNotEnabled()
        compose.onNodeWithTag("next-kanji").bringIntoViewIfNeeded().performClick()
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
        compose.onNodeWithText("Ещё не закончен").assertIsDisplayed()
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
        compose.onNodeWithTag("open-debug").bringIntoViewIfNeeded().performClick()
        compose.onNodeWithTag("practice-debug-feedback").assertIsDisplayed()
            .assertTextContains("Компенсация поворота:", substring = true)
            .assertTextContains("Коррекция масштаба X:", substring = true)
            .assertTextContains("Скорректировано концов: 2", substring = true)
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
        compose.onNodeWithTag("next-kanji").bringIntoViewIfNeeded().performClick()
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
        compose.runOnIdle { word.value = "Электроэнцефалография" }
        compose.onNodeWithText("Электроэнцефалография").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            val results = mutableListOf<TextLayoutResult>()
            assertTrue(action(results))
            assertTrue(results.first().lineCount >= 2)
        }
        val moved = compose.onNodeWithTag("practice-pad").fetchSemanticsNode().boundsInRoot
        assertEquals(original.top, moved.top, .5f)
        assertEquals(original.bottom, moved.bottom, .5f)
        compose.onNodeWithText("Осталось 05:00").assertIsDisplayed()
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
