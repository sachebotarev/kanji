package ru.mobui.kanji.checker.shape

import org.junit.Assume.assumeTrue
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.*
import ru.mobui.kanji.domain.practice.*
import java.io.File

/** Optional diagnostics; no adjustment of production behavior. */
class HookCorpusDiagnosticsTest {
    @Test fun classifyMissingHooks() {
        assumeTrue(System.getenv("KANJI_HOOK_DIAGNOSTICS") == "1")
        val root = File("../../doc/calibration").canonicalFile
        val references = root.resolve("external/references.tsv").readLines().associate { line ->
            val p = line.split('\t')
            p[0] to (p[2].split(';').map { s -> s.split(' ').map {
                val xy = it.split(','); ShapePoint(xy[0].toFloat(), xy[1].toFloat())
            } } to p[1].split(',').map { HookRequirement.valueOf(it) })
        }
        val correspondence = StrokeCorrespondence()
        val hooks = HookChecker()
        root.resolve("tomoe-hook-diagnostics.tsv").bufferedWriter().use { out ->
            out.appendLine("id\treferenceStroke\treferenceDistinct\tinputDistinct\tinputPoints\tinput\treference")
            for (line in root.resolve("tomoe-full-inputs.tsv").readLines()) {
                val p = line.split('\t')
                if (p[1].toInt(16) !in 0x3400..0x9fff) continue
                val (target, requirements) = references.getValue(p[1])
                val raw = p[4].split(';').map { s -> s.split(' ').map {
                    val xy = it.split(','); ShapePoint(xy[0].toFloat(), xy[1].toFloat())
                } }
                if (raw.size != target.size) continue
                val a = correspondence.normalize(raw); val b = correspondence.normalize(target)
                val assignment = correspondence.assign(a, b) ?: continue
                for ((i, j) in assignment.withIndex()) {
                    if (requirements[j] != HookRequirement.Required || hooks.hasHook(a[i], b[j])) continue
                    fun serialize(points: List<ShapePoint>) = points.joinToString(" ") { "${it.x},${it.y}" }
                    out.appendLine(listOf(p[0], j + 1, hooks.hasDistinctHook(b[j]),
                        hooks.hasDistinctHook(a[i]) || hooks.hasDistinctHook(a[i].reversed()),
                        a[i].size, serialize(a[i]), serialize(b[j])).joinToString("\t"))
                }
            }
        }
    }
}
