package ru.mobui.kanji.domain.practice

import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.WritingSample

/** How PRACTICE presents the character guide. */
enum class HintMode {
    /** Pale full reference under the user's ink (toggle). */
    FullCharacter,
    /** Strokes up to the next missing one: earlier strokes at once, the next one animated. */
    NextStroke;

    /** Value stored in `stroke_hint_mode`. */
    val storageKey: String
        get() =
            when (this) {
                FullCharacter -> "full_character"
                NextStroke -> "next_stroke"
            }

    companion object {
        /** Null or unknown keeps the recipe default. */
        fun fromKey(key: String?): HintMode {
            if (key == null) return NextStroke
            return when (key.trim().lowercase()) {
                "next_stroke" -> NextStroke
                "full_character", "full", "full_kanji" -> FullCharacter
                else -> NextStroke
            }
        }
    }
}

/** A strategy receives neutral geometry, never a KanjiVG asset or UI widget. */
data class HintRequest(val sample: WritingSample, val reference: ReferenceDrawing)

/** How the pad should play the overlay. */
enum class HintPlayback {
    /** Persistent pale guide under ink. */
    Static,
    /** Draw the overlay stroke once (UI animates), then the host clears the hint. */
    AnimateOnce,
    /** Draw all strokes sequentially in accelerated tempo, hold for display time, then clear. */
    AnimateAll,
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
 * Shows the reference strokes up to the next missing stroke. The strokes already drawn are shown
 * immediately, and the next stroke is animated. When every reference stroke has already been drawn
 * (and the sign is not correct), returns the full reference with [HintPlayback.AnimateAll] to animate
 * drawing of the whole kanji in accelerated tempo.
 */
class NextStrokeHint : HintAlgorithm {
    override fun create(request: HintRequest): HintPresentation? {
        val next = request.sample.strokeCount
        val strokes = request.reference.strokes
        if (strokes.isEmpty()) return null
        return if (next < strokes.size) {
            HintPresentation(
                overlay = ReferenceDrawing(strokes.take(next + 1)),
                blocksInput = false,
                playback = HintPlayback.AnimateOnce,
            )
        } else {
            HintPresentation(
                overlay = request.reference,
                blocksInput = false,
                playback = HintPlayback.AnimateAll,
            )
        }
    }
}
