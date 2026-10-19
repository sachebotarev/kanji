package ru.mobui.kanji.data.kanjivg

import android.content.res.AssetManager
import android.graphics.PathMeasure
import android.util.Xml
import androidx.core.graphics.PathParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import ru.mobui.kanji.domain.handwriting.HookRequirement
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.ReferencePoint
import ru.mobui.kanji.domain.handwriting.ReferenceStrokeNumber
import ru.mobui.kanji.domain.practice.KanjiId
import ru.mobui.kanji.domain.practice.KanjiReferenceSource
import kotlin.math.ceil

/** Loads trusted bundled SVG assets only. No recognition and no network requests. */
class BundledKanjiReferenceSource(
    private val assets: AssetManager,
    private val ioDispatcher: CoroutineDispatcher,
) : KanjiReferenceSource {
    private val availableFiles by lazy { assets.list("kanjivg").orEmpty().toSet() }
    override suspend fun load(id: KanjiId): ReferenceDrawing? = withContext(ioDispatcher) {
        val code = id.value.removePrefix("u").takeIf { id.value.startsWith("u") && it.matches(Regex("[0-9a-fA-F]{4,6}")) }
            ?.toIntOrNull(16) ?: return@withContext null
        val file = "kanjivg/${code.toString(16).padStart(5, '0')}.svg"
        if (file.substringAfterLast('/') !in availableFiles) return@withContext null
        assets.open(file).use { stream ->
            val parser = Xml.newPullParser()
            parser.setInput(stream, null)
            var strokeDepth: Int? = null
            var numberDepth: Int? = null
            val numbers = mutableListOf<ReferenceStrokeNumber>()
            var viewport: List<Float>? = null
            val strokes = mutableListOf<List<ReferencePoint>>()
            val hooks = mutableListOf<HookRequirement>()
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    if (parser.name == "svg") {
                        viewport = parser.getAttributeValue(null, "viewBox")
                            ?.trim()?.split(Regex("[ ,]+"))?.map(String::toFloat)
                        require(viewport?.size == 4 && viewport[2] > 0 && viewport[3] > 0)
                    }
                    if (parser.name == "g" && parser.getAttributeValue(null, "id")?.startsWith("kvg:StrokePaths_") == true) {
                        strokeDepth = parser.depth
                    }
                    if (parser.name == "g" && parser.getAttributeValue(null, "id")?.startsWith("kvg:StrokeNumbers_") == true) {
                        numberDepth = parser.depth
                    }
                    if (numberDepth != null && parser.name == "text") {
                        val matrix = requireNotNull(parser.getAttributeValue(null, "transform"))
                            .removePrefix("matrix(").removeSuffix(")").trim().split(Regex("[ ,]+" )).map(String::toFloat)
                        require(matrix.size == 6 && matrix.take(4) == listOf(1f, 0f, 0f, 1f))
                        val area = requireNotNull(viewport)
                        numbers.add(ReferenceStrokeNumber(parser.nextText().trim().toInt(),
                            (matrix[4] - area[0]) / area[2], (matrix[5] - area[1]) / area[3]))
                    }
                    if (strokeDepth != null) {
                        require(parser.getAttributeValue(null, "transform") == null) { "Transformed assets need preprocessing" }
                        if (parser.name == "path") {
                            hooks.add(KanjiVgStrokeFeatures.hookRequirement(parser.getAttributeValue("https://kanjivg.tagaini.net/", "type")
                                ?: parser.getAttributeValue("http://kanjivg.tagaini.net/", "type")
                                ?: parser.getAttributeValue("http://kanjivg.tagaini.net", "type")))
                            val area = requireNotNull(viewport)
                            require(area[2] == area[3]) { "Expected a square glyph viewport" }
                            val path = requireNotNull(PathParser.createPathFromPathData(parser.getAttributeValue(null, "d")))
                            val measure = PathMeasure(path, false)
                            val count = ceil(measure.length / 1.5f).toInt().coerceAtLeast(1)
                            val position = FloatArray(2)
                            strokes.add((0..count).map { index ->
                                check(measure.getPosTan(measure.length * index / count, position, null))
                                ReferencePoint((position[0] - area[0]) / area[2], (position[1] - area[1]) / area[3])
                            })
                            require(!measure.nextContour()) { "One contour per stroke is required" }
                        }
                    }
                } else if (event == XmlPullParser.END_TAG && parser.depth == strokeDepth) {
                    strokeDepth = null
                }
                if (event == XmlPullParser.END_TAG && parser.depth == numberDepth) numberDepth = null
                event = parser.next()
            }
            require(strokes.isNotEmpty())
            ReferenceDrawing(strokes, numbers, hooks)
        }
    }
}
