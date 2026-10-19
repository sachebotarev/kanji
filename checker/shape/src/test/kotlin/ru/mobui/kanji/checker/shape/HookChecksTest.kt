package ru.mobui.kanji.checker.shape

import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.*
import ru.mobui.kanji.domain.practice.*

class HookChecksTest {
    private val hook = listOf(ShapePoint(.5f, .1f), ShapePoint(.5f, .85f), ShapePoint(.44f, .8f))
    private val straight = hook.take(2)
    private fun sample(stroke: List<ShapePoint>) = WritingSample(listOf(InkStroke(stroke.mapIndexed { i, p ->
        InkPoint(p.x, p.y, i.toLong())
    }, InkTool.Finger)))
    private val reference = ReferenceDrawing(listOf(hook.map { ReferencePoint(it.x, it.y) }),
        hookRequirements = listOf(HookRequirement.Required))

    @Test fun missingHookFailsEvenWithPermissiveCoverageAndAllOptionalChecksOff() {
        val settings = RecognitionSettings(requiredMatchPercent = 1f, checkStrokeStructure = false,
            enabledCompensations = emptySet())
        val result = ShapeMatcher(settings).compare(sample(straight), reference)
        assertEquals(listOf(1), result.strokeChecks.missingHooks)
        assertFalse(result.isAccepted(settings))
        assertTrue(ShapeMatcher(settings).compare(sample(hook), reference).isAccepted(settings))
        assertTrue(ShapeMatcher(settings).compare(sample(hook.reversed()), reference).isAccepted(settings))
    }

    @Test fun smallNoiseCannotReplaceHookAndDirectionIsStillCheckedSeparately() {
        val checker = HookChecker()
        val almostStraight = straight + ShapePoint(.499f, .849f)
        assertFalse(checker.hasHook(almostStraight, hook))
        val scaled = hook.map { ShapePoint(it.x * .7f + .1f, it.y * .7f + .1f) }
        assertTrue(checker.hasHook(scaled, hook))
        val result = ShapeMatcher(RecognitionSettings(checkStrokeDirection = true, enabledCompensations = emptySet()))
            .compare(sample(hook.reversed()), reference)
        assertTrue(result.strokeChecks.missingHooks.isEmpty())
        assertEquals(listOf(1), result.strokeChecks.reversedStrokes)
    }

    @Test fun shallowHookIsMandatoryButAFlatReferenceDoesNotInventOne() {
        val checker = HookChecker()
        val shallow = listOf(ShapePoint(.5f, .1f), ShapePoint(.5f, .8f), ShapePoint(.55f, .88f))
        assertTrue(checker.hasDistinctHook(shallow))
        assertTrue(checker.hasHook(shallow, shallow))
        assertFalse(checker.hasHook(shallow.take(2), shallow))
        assertFalse(checker.hasDistinctHook(straight))
        assertTrue(checker.hasHook(straight, straight))
        val result = StrokeChecks(StrokeCorrespondence()).check(listOf(straight), listOf(straight),
            RecognitionSettings(checkStrokeStructure = false), requiredHooks = setOf(0))
        assertTrue(result.missingHooks.isEmpty())
        assertEquals(listOf(1), result.ambiguousReferenceHooks)
    }

    @Test fun cleanupPreservesProtectedStrokeEvenWhenHookWouldFitRemovalBudget() {
        val tiny = listOf(ShapePoint(.5f, .1f), ShapePoint(.5f, .85f), ShapePoint(.485f, .85f))
        assertTrue(StrokeEndpointCompensator().compensateGeometry(listOf(tiny), .05f).correctedEnds > 0)
        val protected = StrokeEndpointCompensator().compensateGeometry(listOf(tiny), .05f, setOf(0))
        assertEquals(tiny, protected.strokes.single())
        assertEquals(0, protected.correctedEnds)
        assertEquals(tiny, JitterCompensator(5f).compensate(listOf(tiny), setOf(0)).strokes.single())
    }

    @Test fun hookGateCannotBeBypassedByCleanupAndSurvivesReordering() {
        val horizontal = listOf(ShapePoint(.1f, .2f), ShapePoint(.9f, .2f))
        val checks = StrokeChecks(StrokeCorrespondence())
        val settings = RecognitionSettings(checkStrokeStructure = false)
        assertTrue(checks.check(listOf(horizontal, hook.reversed()), listOf(hook, horizontal), settings,
            requiredHooks = setOf(0)).missingHooks.isEmpty())
        assertEquals(listOf(1), checks.check(listOf(horizontal, straight), listOf(hook, horizontal), settings,
            requiredHooks = setOf(0)).missingHooks)
        val enabled = settings.copy(requiredMatchPercent = 1f, jitterTolerancePercent = 5f,
            enabledCompensations = CompensationStage.entries.toSet())
        assertFalse(ShapeMatcher(enabled).compare(sample(straight), reference).isAccepted(enabled))
    }
}
