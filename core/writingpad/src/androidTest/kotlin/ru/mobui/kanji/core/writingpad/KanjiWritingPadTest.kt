package ru.mobui.kanji.core.writingpad

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.ReferencePoint

class KanjiWritingPadTest {
    @get:Rule val compose = createComposeRule()
    private val events = mutableListOf<PadEvent>()
    private val version = ContentVersion("cell-1", 0)

    private fun show(enabled: Boolean = true) {
        compose.setContent {
            MaterialTheme {
                KanjiWritingPad(
                    PadUiState(version, inputEnabled = enabled),
                    events::add,
                    Modifier.size(240.dp).testTag("pad"),
                )
            }
        }
    }

    @Test
    fun completedGestureEmitsOneNormalizedOrderedStroke() {
        show()
        compose.onNodeWithTag("pad").performTouchInput {
            down(center)
            moveTo(Offset(center.x + width * .2f, center.y))
            up()
        }
        compose.runOnIdle {
            val finished = events.filterIsInstance<PadEvent.StrokeFinished>().single()
            assertEquals(version, finished.baseVersion)
            val points = finished.stroke.points
            assertEquals(.5f, points.first().x, .01f)
            assertEquals(.5f, points.first().y, .01f)
            assertTrue(points.last().x > points.first().x)
            assertTrue(points.all { it.x in 0f..1f && it.y in 0f..1f })
            assertTrue(points.zipWithNext().all { (a, b) -> a.elapsedMs <= b.elapsedMs })
            assertEquals(null, (events.last() as PadEvent.GesturePreview).stroke)
        }
    }

    @Test
    fun cancelledGestureDoesNotCommitAndClearsPreview() {
        show()
        compose.onNodeWithTag("pad").performTouchInput {
            down(center)
            moveTo(Offset(center.x + 10, center.y))
            cancel()
        }
        compose.runOnIdle {
            assertTrue(events.none { it is PadEvent.StrokeFinished })
            assertEquals(null, (events.last() as PadEvent.GesturePreview).stroke)
        }
    }

    @Test
    fun disabledPadEmitsNoInput() {
        show(enabled = false)
        compose.onNodeWithTag("pad").performTouchInput {
            down(center)
            up()
        }
        compose.runOnIdle { assertTrue(events.isEmpty()) }
    }

    @Test
    fun twoFingerSingleTapEmitsUndoRequested() {
        show()
        compose.onNodeWithTag("pad").performTouchInput {
            down(0, Offset(center.x - 30, center.y))
            down(1, Offset(center.x + 30, center.y))
            up(0)
            up(1)
        }
        compose.waitUntil(5_000) { events.any { it is PadEvent.UndoRequested } }
        assertTrue(events.none { it is PadEvent.StrokeFinished })
        assertTrue(events.none { it is PadEvent.ClearRequested })
    }

    @Test
    fun twoFingerDoubleTapEmitsClearRequested() {
        show()
        compose.onNodeWithTag("pad").performTouchInput {
            down(0, Offset(center.x - 30, center.y))
            down(1, Offset(center.x + 30, center.y))
            up(0)
            up(1)
            advanceEventTime(80)
            down(0, Offset(center.x - 30, center.y))
            down(1, Offset(center.x + 30, center.y))
            up(0)
            up(1)
        }
        compose.waitUntil(5_000) { events.any { it is PadEvent.ClearRequested } }
        assertTrue(events.none { it is PadEvent.StrokeFinished })
        assertTrue(events.none { it is PadEvent.UndoRequested })
    }

    @Test
    fun animateAllStrokesEmitsHintPlaybackFinishedAfterHold() {
        val strokeA = listOf(ReferencePoint(0.1f, 0.1f), ReferencePoint(0.9f, 0.1f))
        val strokeB = listOf(ReferencePoint(0.1f, 0.5f), ReferencePoint(0.9f, 0.5f))
        val drawing = ReferenceDrawing(listOf(strokeA, strokeB))
        compose.setContent {
            MaterialTheme {
                KanjiWritingPad(
                    PadUiState(
                        version,
                        overlay = drawing,
                        animateOverlay = true,
                        animateAllStrokes = true,
                        fullHintHoldMillis = 100L,
                    ),
                    events::add,
                    Modifier.size(240.dp).testTag("pad"),
                )
            }
        }
        compose.waitUntil(5_000) { events.any { it is PadEvent.HintPlaybackFinished } }
    }

    @Test
    fun longPressEmitsHintRequestedAndDoesNotCommitStroke() {
        show()
        compose.onNodeWithTag("pad").performTouchInput {
            down(center)
            advanceEventTime(600)
            up()
        }
        compose.waitUntil(5_000) { events.any { it is PadEvent.HintRequested } }
        assertTrue(events.none { it is PadEvent.StrokeFinished })
        assertEquals(null, (events.last() as PadEvent.GesturePreview).stroke)
    }
}
