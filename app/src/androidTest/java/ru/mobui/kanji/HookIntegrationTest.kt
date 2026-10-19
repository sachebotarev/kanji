package ru.mobui.kanji

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.checker.shape.*
import ru.mobui.kanji.data.kanjivg.BundledKanjiReferenceSource
import ru.mobui.kanji.domain.handwriting.*
import ru.mobui.kanji.domain.practice.*

class HookIntegrationTest {
    @Test fun previouslyRejectedReferenceEndingsAreAccepted() = runBlocking {
        val source = BundledKanjiReferenceSource(InstrumentationRegistry.getInstrumentation().targetContext.assets, Dispatchers.IO)
        val matcher = ShapeMatcher(RecognitionSettings(enabledCompensations = emptySet(), checkStrokeStructure = false))
        val failures = mutableListOf<String>()
        for (code in listOf("034c1", "04fca", "05287", "052e0", "05506", "05546", "05618", "05653", "05700", "0582a", "0589f", "058ba", "05b7a", "05be5", "05cfb", "05dbc", "05ed6", "05edb", "05eec", "0609b", "0616e", "06174", "061ba", "061ca", "0622e", "0622f", "06233", "063a2", "0644e", "0647a", "0649a", "064d4", "064da", "064e2", "0651c", "066dc", "066e6", "0671b", "068ad", "06939", "06a1b", "06a58", "06a90", "06ac2", "06ae8", "06b54", "06b5f", "06d5a", "06dd5", "06df1", "06e5b", "06fb3", "06fb9", "06ffe", "07018", "07136", "071e0", "071ff", "07210", "072a7", "0737b", "07425", "07463", "0751a", "07627", "076b4", "076e7", "07766", "077bb", "078aa", "079b1", "07a46", "07ae3", "07bea", "07c37", "07cc2", "07cf4", "07cf6", "07dbe", "07e23", "07e46", "07e8f", "07e9b", "07f45", "07f55", "07fb2", "08000", "08188", "0819a", "081a0", "081bd", "081c8", "081da", "08207", "0826b", "084fc", "08606", "0864e", "08650", "08652", "08654", "08655", "0865a", "0865b", "0865c", "0865e", "0865f", "08667", "08763", "087fe", "08836", "088d4", "0892b", "08936", "08956", "08b14", "08b2c", "08b4e", "08b6b", "08d0d", "08e94", "08f64", "09021", "09035", "0905e", "0907d", "091a4", "091aa", "091b5", "091c1", "09356", "093d0", "09462", "0946a", "09675", "09678", "09694", "09871", "09955", "099ff", "09a62", "09b32", "09b3b", "09bf1", "09c78", "09df8", "09ef7", "0f936")) {
            val drawing = requireNotNull(source.load(KanjiId("u$code")))
            for ((index, stroke) in drawing.strokes.withIndex()) {
                if (drawing.hookRequirements[index] != HookRequirement.Required) continue
                val reference = ReferenceDrawing(listOf(stroke), hookRequirements = listOf(HookRequirement.Required))
                val sample = WritingSample(listOf(InkStroke(stroke.mapIndexed { i, p -> InkPoint(p.x, p.y, i.toLong()) }, InkTool.Finger)))
                if (matcher.compare(sample, reference).strokeChecks.missingHooks.isNotEmpty()) failures.add("$code:${index + 1}")
            }
        }
        assertEquals(emptyList<String>(), failures)
    }

    @Test fun everyRequiredHookAcceptsExactBundledGeometry() = runBlocking {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        val source = BundledKanjiReferenceSource(assets, Dispatchers.IO)
        val matcher = ShapeMatcher(RecognitionSettings(enabledCompensations = emptySet(), checkStrokeStructure = false))
        val failures = mutableListOf<String>()
        var checked = 0
        for ((glyphIndex, name) in assets.list("kanjivg").orEmpty().filter { it.endsWith(".svg") }.withIndex()) {
            if (glyphIndex % 1000 == 0) android.util.Log.i("HookCalibration", "glyphs=$glyphIndex hooks=$checked")
            val drawing = requireNotNull(source.load(KanjiId("u" + name.removeSuffix(".svg"))))
            for ((index, stroke) in drawing.strokes.withIndex()) {
                if (drawing.hookRequirements[index] != HookRequirement.Required) continue
                val reference = ReferenceDrawing(listOf(stroke), hookRequirements = listOf(HookRequirement.Required))
                val sample = WritingSample(listOf(InkStroke(stroke.mapIndexed { i, p -> InkPoint(p.x, p.y, i.toLong()) }, InkTool.Finger)))
                checked++
                if (matcher.compare(sample, reference).strokeChecks.missingHooks.isNotEmpty()) failures.add("$name:${index + 1}")
            }
        }
        android.util.Log.i("HookCalibration", "checked=$checked failures=${failures.size}")
        assertEquals("Exact hook geometry rejected", emptyList<String>(), failures)
    }

    @Test fun allHookFamiliesAcceptTheirRealGeometryAndVerticalHookCannotBeOmitted() = runBlocking {
        val source = BundledKanjiReferenceSource(InstrumentationRegistry.getInstrumentation().targetContext.assets, Dispatchers.IO)
        val settings = RecognitionSettings(enabledCompensations = emptySet(), checkStrokeStructure = false, requiredMatchPercent = 1f)
        val matcher = ShapeMatcher(settings)
        fun sample(stroke: List<ReferencePoint>) = WritingSample(listOf(InkStroke(stroke.mapIndexed { i, p ->
            InkPoint(p.x, p.y, i.toLong())
        }, InkTool.Finger)))
        val failures = mutableListOf<String>()
        for (code in listOf("0629c", "09824", "06b05", "09f4b", "07af8", "05788", "05b5b", "07dcf", "05c3b", "05b55", "04e01", "05c0f", "06c34")) {
            val drawing = requireNotNull(source.load(KanjiId("u$code")))
            assertTrue(code, HookRequirement.Required in drawing.hookRequirements)
            for ((index, stroke) in drawing.strokes.withIndex()) {
                if (drawing.hookRequirements[index] != HookRequirement.Required) continue
                val reference = ReferenceDrawing(listOf(stroke), hookRequirements = listOf(HookRequirement.Required))
                for (line in listOf(stroke, stroke.reversed())) {
                    val result = matcher.compare(sample(line), reference)
                    if (result.strokeChecks.missingHooks.isNotEmpty()) failures.add("$code stroke ${index + 1}")
                }
            }
        }
        assertEquals("Exact reference rejected", emptyList<String>(), failures)
        val small = requireNotNull(source.load(KanjiId("u5c0f")))
        assertEquals(HookRequirement.Required, small.hookRequirements[0])
        val hook = small.strokes[0]
        val reference = ReferenceDrawing(listOf(hook), hookRequirements = listOf(HookRequirement.Required))
        val bottom = hook.indices.maxBy { hook[it].y }
        val missing = matcher.compare(sample(hook.take(bottom + 1)), reference)
        assertEquals(listOf(1), missing.strokeChecks.missingHooks)
        assertFalse(missing.isAccepted(settings))
    }
}
