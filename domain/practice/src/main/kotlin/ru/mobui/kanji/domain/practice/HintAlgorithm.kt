package ru.mobui.kanji.domain.practice

import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.WritingSample

/** A strategy receives neutral geometry, never a KanjiVG asset or UI widget. */
data class HintRequest(val sample: WritingSample, val reference: ReferenceDrawing)
data class HintPresentation(val overlay: ReferenceDrawing, val blocksInput: Boolean)
fun interface HintAlgorithm {
    fun create(request: HintRequest): HintPresentation?
}

/** A pale reference guide for tracing; the answer remains the user's own ink. */
class TraceReferenceHint : HintAlgorithm {
    override fun create(request: HintRequest) = HintPresentation(request.reference, blocksInput = false)
}
