package ru.mobui.kanji.core.writingpad

import ru.mobui.kanji.domain.handwriting.InkStroke
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.WritingSample

/** The owner must advance revision for every edit, including undo. */
data class ContentVersion(val cellId: String, val revision: Long)

data class PadUiState(
    val version: ContentVersion,
    val content: PadContent = PadContent.Empty,
    val inputEnabled: Boolean = true,
    val overlay: ReferenceDrawing? = null,
    /** When true, the pad draws [overlay] as a one-shot yellow stroke animation. */
    val animateOverlay: Boolean = false,
    /** When true with [animateOverlay], animates all strokes sequentially in accelerated tempo. */
    val animateAllStrokes: Boolean = false,
    /** Duration in milliseconds to hold the complete drawing before fading out. */
    val fullHintHoldMillis: Long = 0L,
    /** When true with a static overlay, draw the guide in error red at full opacity. */
    val overlayAsError: Boolean = false,
    val comparisonOverlay: ru.mobui.kanji.domain.handwriting.ComparisonDrawing? = null,
)

/** Display content is distinct from user input; a reference is never a user answer. */
sealed interface PadContent {
    data object Empty : PadContent

    data class UserInk(val sample: WritingSample) : PadContent

    data class Reference(val drawing: ReferenceDrawing) : PadContent
}

sealed interface PadEvent {
    data class StrokeFinished(
        val baseVersion: ContentVersion,
        val gestureId: Long,
        val stroke: InkStroke,
    ) : PadEvent

    /** Transient input for a thumbnail; never persist or recognize it. */
    data class GesturePreview(
        val baseVersion: ContentVersion,
        val gestureId: Long,
        val stroke: InkStroke?,
    ) : PadEvent

    /** Fired after an animated overlay finishes drawing and fading out. */
    data object HintPlaybackFinished : PadEvent

    /** Two-finger single tap: undo last stroke. */
    data object UndoRequested : PadEvent

    /** Two-finger double tap: clear entire input pad. */
    data object ClearRequested : PadEvent

    /** Long press on input area: trigger hint for current character. */
    data object HintRequested : PadEvent
}
