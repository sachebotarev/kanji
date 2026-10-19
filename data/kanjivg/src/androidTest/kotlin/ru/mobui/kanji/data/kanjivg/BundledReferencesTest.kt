package ru.mobui.kanji.data.kanjivg

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.domain.practice.KanjiId

class BundledReferencesTest {
    @Test fun mapsHookMetadataConservatively() {
        val required = ru.mobui.kanji.domain.handwriting.HookRequirement.Required
        val optional = ru.mobui.kanji.domain.handwriting.HookRequirement.Optional
        val unknown = ru.mobui.kanji.domain.handwriting.HookRequirement.Unknown
        val plain = ru.mobui.kanji.domain.handwriting.HookRequirement.NotRequired
        assertEquals(required, KanjiVgStrokeFeatures.hookRequirement("㇚"))
        assertEquals(required, KanjiVgStrokeFeatures.hookRequirement("㇚a/㇆"))
        assertEquals(optional, KanjiVgStrokeFeatures.hookRequirement("㇚/㇑"))
        assertEquals(plain, KanjiVgStrokeFeatures.hookRequirement("㇔/㇀"))
        assertEquals(unknown, KanjiVgStrokeFeatures.hookRequirement(null))
        assertEquals(unknown, KanjiVgStrokeFeatures.hookRequirement("㇚/６"))
        assertEquals(unknown, KanjiVgStrokeFeatures.hookRequirement("㇚q"))
    }

    @Test fun everyBundledCanonicalGlyphLoadsOffline() = runBlocking {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        val source = BundledKanjiReferenceSource(assets, Dispatchers.IO)
        val names = assets.list("kanjivg")!!.filter { it.endsWith(".svg") }
        assertEquals(6984, names.size)
        for (name in names) {
            try {
                val drawing = source.load(KanjiId("u" + name.removeSuffix(".svg")))
                assertNotNull(name, drawing)
                assertEquals(name, drawing!!.strokes.size, drawing.strokeNumbers.size)
            } catch (e: Exception) { throw AssertionError("Cannot load $name", e) }
        }
        val firstNumber = source.load(KanjiId("u56f3"))!!.strokeNumbers.first()
        assertEquals(1, firstNumber.number)
        assertEquals(13.50f / 109f, firstNumber.x, .0001f)
        assertEquals(27.13f / 109f, firstNumber.y, .0001f)
        assertNull(source.load(KanjiId("unknown")))
    }
}
