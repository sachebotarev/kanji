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

class KanjiDictionaryIntegrationTest {
    @Test fun realTemplatesAndWrongPartnersAreDistinguishedEvenWithPermissiveGeometry() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = BundledKanjiReferenceSource(context.assets, Dispatchers.IO)
        val config = ShapeCheckConfig(requiredMatchPercent = 1f, distanceTolerance = .15f,
            maxHorizontalStretchPercent = 1000f, maxVerticalStretchPercent = 1000f)
        val checker = ShapeKanjiChecker(source, Dispatchers.Default, config)
        fun id(c: Char) = KanjiId("u" + c.code.toString(16))
        fun sample(drawing: ReferenceDrawing, reversed: Boolean): WritingSample {
            val lines = if (reversed) drawing.strokes.reversed().map { it.reversed() } else drawing.strokes
            return WritingSample(lines.map { s -> InkStroke(s.mapIndexed { i, p -> InkPoint(p.x, p.y, i.toLong()) }, InkTool.Finger) })
        }
        for (pair in listOf("牛午", "未末", "土士")) for (target in pair) {
            val correct = requireNotNull(source.load(id(target)))
            for (reverse in listOf(false, true)) {
                val report = checker.checkWithDetails(CheckRequest("t", "c", id(target), 0, sample(correct, reverse)))
                assertEquals("$target reverse=$reverse: ${report.failures}", CheckVerdict.Correct, report.verdict)
            }
            val partner = pair.first { it != target }
            val wrong = requireNotNull(source.load(id(partner)))
            val report = checker.checkWithDetails(CheckRequest("t", "c", id(target), 0, sample(wrong, false)))
            assertEquals("$partner must not pass for $target", CheckVerdict.Incorrect, report.verdict)
            assertTrue("Missing dictionary violation for $partner/$target: ${report.failures}", report.failures.any { it is CheckFailure.KanjiRules })
        }
    }
}
