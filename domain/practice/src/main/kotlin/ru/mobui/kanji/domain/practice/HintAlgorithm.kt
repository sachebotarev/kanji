package ru.mobui.kanji.domain.practice

import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.WritingSample

/** How PRACTICE presents the character guide. */
enum class HintMode {
    /** Pale full reference under the user's ink (toggle). */
    FullCharacter,
    /** Animate the next missing stroke once, then clear. */
    NextStroke,
}

/** A strategy receives neutral geometry, never a KanjiVG asset or UI widget. */
data class HintRequest(val sample: WritingSample, val reference: ReferenceDrawing)

/** How the pad should play the overlay. */
enum class HintPlayback {
    /** Persistent pale guide under ink. */
    Static,
    /** Draw the overlay stroke once (UI animates), then the host clears the hint. */
    AnimateOnce,
}

enum class HintOverlayStyle {
    /** Pale guide under the learner's ink. */
    Guide,
    /** Full character drawn in error red after the NextStroke budget is spent. */
    Exhausted,
}

data class HintPresentation(
    val overlay: ReferenceDrawing,
    val blocksInput: Boolean,
    val playback: HintPlayback = HintPlayback.Static,
    val style: HintOverlayStyle = HintOverlayStyle.Guide,
)

fun interface HintAlgorithm {
    fun create(request: HintRequest): HintPresentation?
}

fun hintAlgorithmFor(mode: HintMode): HintAlgorithm =
    when (mode) {
        HintMode.FullCharacter -> TraceReferenceHint()
        HintMode.NextStroke -> NextStrokeHint()
    }

/** A pale reference guide for tracing; the answer remains the user's own ink. */
class TraceReferenceHint : HintAlgorithm {
    override fun create(request: HintRequest) =
        HintPresentation(request.reference, blocksInput = false, playback = HintPlayback.Static)
}

/**
 * Shows only the next reference stroke after the strokes the learner already drew. Unavailable when
 * every reference stroke already has a user stroke.
 */
class NextStrokeHint : HintAlgorithm {
    override fun create(request: HintRequest): HintPresentation? {
        val next = request.sample.strokeCount
        val strokes = request.reference.strokes
        if (next >= strokes.size) return null
        return HintPresentation(
            overlay = ReferenceDrawing(listOf(strokes[next])),
            blocksInput = false,
            playback = HintPlayback.AnimateOnce,
        )
    }
}
