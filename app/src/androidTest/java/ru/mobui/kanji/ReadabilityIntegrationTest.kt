package ru.mobui.kanji

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.checker.readability.DigitalInkKanjiChecker
import ru.mobui.kanji.domain.handwriting.*
import ru.mobui.kanji.domain.practice.*

/** Offline by default. A diagnostic run may explicitly opt into model download. */
class ReadabilityIntegrationTest {
    @Test fun downloadedJapaneseModelRecognizesInk() = runBlocking {
        val checker = DigitalInkKanjiChecker()
        if (!checker.isModelDownloaded()) {
            val allowDownload = androidx.test.platform.app.InstrumentationRegistry.getArguments()
                .getString("allowModelDownload") == "true"
            org.junit.Assume.assumeTrue("Japanese model is not downloaded", allowDownload)
            assertTrue("Japanese model download failed", checker.downloadModel())
        }
        val stroke = InkStroke(listOf(InkPoint(.2f, .5f, 0), InkPoint(.8f, .5f, 100)), InkTool.Finger)
        val report = checker.checkWithDetails(CheckRequest("probe", "probe", KanjiId("u4e00"), 0, WritingSample(listOf(stroke))))
        assertNotEquals(CheckVerdict.Unavailable, report.verdict)
        assertTrue("Recognizer returned no candidates", !report.metrics["recognizedText"].isNullOrEmpty())
        // An isolated horizontal stroke may be classified as 一 or the katakana long-vowel mark.
        // This smoke test checks SDK execution, not recognition quality on ambiguous ink.
        assertTrue(report.verdict == CheckVerdict.Correct || report.verdict == CheckVerdict.Incorrect)
    }
}
