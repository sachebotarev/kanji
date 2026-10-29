package ru.mobui.kanji

import ru.mobui.kanji.checker.shape.matching.ShapeMatcher
import ru.mobui.kanji.checker.shape.rules.KanjiRuleDictionary

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.json.JSONArray
import org.json.JSONObject
import ru.mobui.kanji.data.kanjivg.BundledKanjiReferenceSource
import ru.mobui.kanji.domain.handwriting.*
import ru.mobui.kanji.domain.practice.*
import kotlin.math.*

/** Deterministic engineering probe, not a validation set of human handwriting. */
class ShapeCalibrationTest {
    @Test fun collectSyntheticAndConfusableScores() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = BundledKanjiReferenceSource(context.assets, Dispatchers.IO)
        val pairs = listOf("土士", "未末", "日曰", "人入", "人八", "大丈", "牛午", "木不", "王玉", "大太", "口囗", "刀力")
        val characters = pairs.flatMap { it.toList() }.distinct()
        val drawings = characters.associateWith { requireNotNull(source.load(KanjiId("u" + it.code.toString(16)))) }
        val profile = ru.mobui.kanji.domain.practice.RecognitionSettings.touchInput()
        val records = JSONArray()
        val tolerances = listOf(.035f)
        fun sample(strokes: List<List<ReferencePoint>>) = WritingSample(strokes.map { s -> InkStroke(s.mapIndexed { i, p -> InkPoint(p.x, p.y, i.toLong()) }, InkTool.Finger) })
        fun add(input: List<List<ReferencePoint>>, target: Char, correct: Boolean, kind: String) {
            for (tolerance in tolerances) {
                val config = profile.copy(distanceTolerance = tolerance)
                val match = ShapeMatcher(config).compare(sample(input), drawings.getValue(target), KanjiRuleDictionary().rulesFor(KanjiId("u" + target.code.toString(16))))
                records.put(JSONObject().put("target", target.toString()).put("correct", correct).put("kind", kind)
                    .put("threshold", config.requiredMatchPercent.toDouble())
                    .put("budgetLimit", config.maxCompensationBudgetPercent?.toDouble())
                    .put("enabledCompensations", config.enabledCompensations.joinToString { it.name })
                    .put("tolerance", tolerance.toDouble()).put("score", match.matchPercent.toDouble())
                    .put("budget", match.compensation.budget.totalPercent.toDouble())
                    .put("stageErrors", match.compensation.failures.joinToString { it.javaClass.simpleName })
                    .put("dictionaryViolations", match.strokeChecks.ruleViolations.size)
                    .put("strokeCountMatches", match.strokeCountMatches).put("accepted", match.isAccepted(config)))
                android.util.Log.i("ShapeCalibration", records.getJSONObject(records.length() - 1).toString())
            }
        }
        for ((character, drawing) in drawings) {
            add(drawing.strokes, character, true, "exact")
            for ((name, parameters) in listOf(
                "shift" to floatArrayOf(.015f, -.01f, 1f, 0f, 0f),
                "size-5%" to floatArrayOf(0f, 0f, .95f, 0f, 0f),
                "rotation+3" to floatArrayOf(0f, 0f, 1f, 3f, 0f),
                "rotation-3" to floatArrayOf(0f, 0f, 1f, -3f, 0f),
                "shear+3" to floatArrayOf(0f, 0f, 1f, 0f, 3f),
                "shear-3" to floatArrayOf(0f, 0f, 1f, 0f, -3f),
            )) {
                val angle = parameters[3] * PI.toFloat() / 180
                val shear = tan(parameters[4] * PI.toFloat() / 180)
                val transformed = drawing.strokes.map { s -> s.map { p ->
                    val x = (p.x - .5f) * parameters[2]; val y = (p.y - .5f) * parameters[2]
                    Pair(.5f + x * cos(angle) - y * sin(angle) + shear * y + parameters[0],
                        .5f + x * sin(angle) + y * cos(angle) + parameters[1])
                } }
                if (transformed.flatten().all { it.first in 0f..1f && it.second in 0f..1f }) {
                    add(transformed.map { s -> s.map { ReferencePoint(it.first, it.second) } }, character, true, name)
                }
            }
        }
        // Additional independent distortions exercise the whole chain, not only placement.
        for ((character, drawing) in drawings) {
            val points = drawing.strokes.flatten()
            val side = max(points.maxOf { it.x } - points.minOf { it.x }, points.maxOf { it.y } - points.minOf { it.y })
            for ((kind, xFactor, yFactor) in listOf(
                Triple("size+5%", 1.05f, 1.05f), Triple("stretch-x-5%", .95f, 1f),
                Triple("stretch-x+5%", 1.05f, 1f), Triple("stretch-y-5%", 1f, .95f), Triple("stretch-y+5%", 1f, 1.05f),
            )) {
                val changed = drawing.strokes.map { s -> s.map { p -> (.5f + (p.x - .5f) * xFactor) to (.5f + (p.y - .5f) * yFactor) } }
                if (changed.flatten().all { it.first in 0f..1f && it.second in 0f..1f }) add(changed.map { s -> s.map { ReferencePoint(it.first, it.second) } }, character, true, kind)
            }
            val jittered = drawing.strokes.map { s -> s.mapIndexed { index, p ->
                val offset = if (index == 0 || index == s.lastIndex) 0f else sin(index.toFloat() * 1.7f) * .0025f * side
                (p.x + offset) to p.y
            } }
            if (jittered.flatten().all { it.first in 0f..1f && it.second in 0f..1f }) add(jittered.map { s -> s.map { ReferencePoint(it.first, it.second) } }, character, true, "jitter-0.25%")
            val tailed = drawing.strokes.map { s ->
                val end = s.last(); val before = s[s.lastIndex.coerceAtLeast(1) - 1]
                val dx = end.x - before.x; val dy = end.y - before.y; val length = hypot(dx, dy)
                val result = s.map { it.x to it.y }
                if (length <= 0f) result else result + ((end.x - dy / length * .01f * side) to (end.y + dx / length * .01f * side))
            }
            if (tailed.flatten().all { it.first in 0f..1f && it.second in 0f..1f }) add(tailed.map { s -> s.map { ReferencePoint(it.first, it.second) } }, character, true, "tail-1%")
        }
        for (pair in pairs) {
            add(drawings.getValue(pair[0]).strokes, pair[1], false, pair)
            add(drawings.getValue(pair[1]).strokes, pair[0], false, pair.reversed())
        }
        val output = java.io.File(context.getExternalFilesDir(null), "shape-calibration.json")
        output.writeText(records.toString(2))
        android.util.Log.i("ShapeCalibrationSummary", "records=${records.length()}")
        assertTrue(records.length() > 250)
        for (i in 0 until records.length()) {
            val record = records.getJSONObject(i)
            if (!record.getBoolean("correct")) assertFalse(record.toString(), record.getBoolean("accepted"))
            if (record.getString("kind") == "exact") assertTrue(record.toString(), record.getBoolean("accepted"))
        }
    }
}
