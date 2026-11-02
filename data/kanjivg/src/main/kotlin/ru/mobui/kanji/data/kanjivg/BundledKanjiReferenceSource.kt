package ru.mobui.kanji.data.kanjivg

import android.content.res.AssetManager
import android.graphics.PathMeasure
import android.util.Xml
import androidx.core.graphics.PathParser
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import ru.mobui.kanji.domain.handwriting.HookRequirement
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.ReferencePoint
import ru.mobui.kanji.domain.handwriting.ReferenceStrokeNumber
import ru.mobui.kanji.domain.practice.KanjiId
import ru.mobui.kanji.domain.practice.KanjiReferenceSource

/** Loads trusted bundled SVG assets only. No recognition and no network requests. */
class BundledKanjiReferenceSource(
    private val assets: AssetManager,
    private val ioDispatcher: CoroutineDispatcher,
) : KanjiReferenceSource {
    private val availableFiles by lazy { assets.list("kanjivg").orEmpty().toSet() }
    private val cache = ConcurrentHashMap<KanjiId, ReferenceDrawing>()

    override suspend fun load(id: KanjiId): ReferenceDrawing? {
        cache[id]?.let { return it }
        return withContext(ioDispatcher) {
            cache[id]?.let { return@withContext it }
            val code =
                id.value
                    .removePrefix("u")
                    .takeIf { id.value.startsWith("u") && it.matches(Regex("[0-9a-fA-F]{4,6}")) }
                    ?.toIntOrNull(UNICODE_HEX_RADIX) ?: return@withContext null
            val file =
                "kanjivg/${code.toString(UNICODE_HEX_RADIX).padStart(GLYPH_FILE_CODE_WIDTH, '0')}.svg"
            if (file.substringAfterLast('/') !in availableFiles) return@withContext null
            val drawing =
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
                            viewport =
                                parser
                                    .getAttributeValue(null, "viewBox")
                                    ?.trim()
                                    ?.split(Regex("[ ,]+"))
                                    ?.map(String::toFloat)
                            require(
                                viewport?.size == VIEWPORT_COMPONENTS &&
                                    viewport[2] > 0 &&
                                    viewport[VIEWPORT_HEIGHT_INDEX] > 0,
                            )
                        }
                        if (
                            parser.name == "g" &&
                                parser
                                    .getAttributeValue(null, "id")
                                    ?.startsWith("kvg:StrokePaths_") == true
                        ) {
                            strokeDepth = parser.depth
                        }
                        if (
                            parser.name == "g" &&
                                parser
                                    .getAttributeValue(null, "id")
                                    ?.startsWith("kvg:StrokeNumbers_") == true
                        ) {
                            numberDepth = parser.depth
                        }
                        if (numberDepth != null && parser.name == "text") {
                            val matrix =
                                requireNotNull(parser.getAttributeValue(null, "transform"))
                                    .removePrefix("matrix(")
                                    .removeSuffix(")")
                                    .trim()
                                    .split(Regex("[ ,]+"))
                                    .map(String::toFloat)
                            require(
                                matrix.size == TRANSLATION_MATRIX_COMPONENTS &&
                                    matrix.take(VIEWPORT_COMPONENTS) == listOf(1f, 0f, 0f, 1f),
                            )
                            val area = requireNotNull(viewport)
                            numbers.add(
                                ReferenceStrokeNumber(
                                    parser.nextText().trim().toInt(),
                                    (matrix[VIEWPORT_COMPONENTS] - area[0]) / area[2],
                                    (matrix[GLYPH_FILE_CODE_WIDTH] - area[1]) /
                                        area[VIEWPORT_HEIGHT_INDEX],
                                ),
                            )
                        }
                        if (strokeDepth != null) {
                            require(parser.getAttributeValue(null, "transform") == null) {
                                "Transformed assets need preprocessing"
                            }
                            if (parser.name == "path") {
                                hooks.add(
                                    KanjiVgStrokeFeatures.hookRequirement(
                                        parser.getAttributeValue(
                                            "https://kanjivg.tagaini.net/",
                                            "type",
                                        )
                                            ?: parser.getAttributeValue(
                                                "http://kanjivg.tagaini.net/",
                                                "type",
                                            )
                                            ?: parser.getAttributeValue(
                                                "http://kanjivg.tagaini.net",
                                                "type",
                                            ),
                                    ),
                                )
                                val area = requireNotNull(viewport)
                                require(area[2] == area[VIEWPORT_HEIGHT_INDEX]) {
                                    "Expected a square glyph viewport"
                                }
                                val path =
                                    requireNotNull(
                                        PathParser.createPathFromPathData(
                                            parser.getAttributeValue(null, "d"),
                                        ),
                                    )
                                val measure = PathMeasure(path, false)
                                val count =
                                    ceil(measure.length / PATH_SAMPLE_SPACING)
                                        .toInt()
                                        .coerceAtLeast(1)
                                val position = FloatArray(2)
                                strokes.add(
                                    (0..count).map { index ->
                                        check(
                                            measure.getPosTan(
                                                measure.length * index / count,
                                                position,
                                                null,
                                            ),
                                        )
                                        ReferencePoint(
                                            (position[0] - area[0]) / area[2],
                                            (position[1] - area[1]) / area[VIEWPORT_HEIGHT_INDEX],
                                        )
                                    },
                                )
                                require(!measure.nextContour()) {
                                    "One contour per stroke is required"
                                }
                            }
                        }
                    } else if (event == XmlPullParser.END_TAG && parser.depth == strokeDepth) {
                        strokeDepth = null
                    }
                    if (event == XmlPullParser.END_TAG && parser.depth == numberDepth)
                        numberDepth = null
                    event = parser.next()
                }
                require(strokes.isNotEmpty())
                ReferenceDrawing(strokes, numbers, hooks)
            }
            cache[id] = drawing
            drawing
        }
    }
}

private const val UNICODE_HEX_RADIX = 16
private const val GLYPH_FILE_CODE_WIDTH = 5
private const val VIEWPORT_COMPONENTS = 4
private const val VIEWPORT_HEIGHT_INDEX = 3
private const val TRANSLATION_MATRIX_COMPONENTS = 6
private const val PATH_SAMPLE_SPACING = 1.5f
