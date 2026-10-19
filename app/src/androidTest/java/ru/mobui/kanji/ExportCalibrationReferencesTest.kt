package ru.mobui.kanji

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import ru.mobui.kanji.data.kanjivg.BundledKanjiReferenceSource
import ru.mobui.kanji.domain.practice.KanjiId

/** Exports the same sampled reference paths used by production for offline calibration. */
class ExportCalibrationReferencesTest {
    @Test fun exportReferences() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = BundledKanjiReferenceSource(context.assets, Dispatchers.IO)
        context.filesDir.resolve("calibration-references.tsv").bufferedWriter().use { writer ->
            for (file in context.assets.list("kanjivg").orEmpty()) {
                val code = file.substringBefore('.').trimStart('0')
                val drawing = source.load(KanjiId("u$code")) ?: continue
                writer.appendLine(listOf(code, drawing.hookRequirements.joinToString(",") { it.name },
                    drawing.strokes.joinToString(";") { s -> s.joinToString(" ") { "${it.x},${it.y}" } }).joinToString("\t"))
            }
        }
    }
}
