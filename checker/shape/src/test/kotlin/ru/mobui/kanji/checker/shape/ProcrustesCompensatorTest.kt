package ru.mobui.kanji.checker.shape

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ProcrustesCompensatorTest {
    private val compensator = ProcrustesCompensator(samplesPerStroke = 32)

    private val sampleStroke1 = listOf(ShapePoint(10f, 10f), ShapePoint(50f, 10f), ShapePoint(50f, 50f))
    private val sampleStroke2 = listOf(ShapePoint(20f, 30f), ShapePoint(80f, 30f))
    private val reference = listOf(sampleStroke1, sampleStroke2)
    private val identityAssignment = listOf(0, 1)

    @Test
    fun testIdentityTransformWhenInputEqualsReference() {
        val result = compensator.compensate(reference, reference, identityAssignment)
        assertEquals(0f, result.translationX, 1e-4f)
        assertEquals(0f, result.translationY, 1e-4f)
        assertEquals(1f, result.scaleFactor, 1e-4f)
        assertEquals(0f, result.rotationDegrees, 1e-4f)
        assertEquals(0f, result.rootMeanSquaredError, 1e-4f)
    }

    @Test
    fun testPureTranslationRecovery() {
        val dx = 15f
        val dy = -25f
        val shiftedInput = reference.map { stroke -> stroke.map { ShapePoint(it.x + dx, it.y + dy) } }

        val result = compensator.compensate(shiftedInput, reference, identityAssignment)

        assertEquals(-dx, result.translationX, 1e-3f)
        assertEquals(-dy, result.translationY, 1e-3f)
        assertEquals(1f, result.scaleFactor, 1e-3f)
        assertEquals(0f, result.rotationDegrees, 1e-3f)
        assertEquals(0f, result.rootMeanSquaredError, 1e-3f)

        // Verifies transformed strokes match reference
        for ((refStroke, resStroke) in reference.zip(result.strokes)) {
            for ((pRef, pRes) in refStroke.zip(resStroke)) {
                assertEquals(pRef.x, pRes.x, 1e-3f)
                assertEquals(pRef.y, pRes.y, 1e-3f)
            }
        }
    }

    @Test
    fun testPureScalingRecovery() {
        val scale = 0.5f
        val scaledInput = reference.map { stroke -> stroke.map { ShapePoint(it.x * scale, it.y * scale) } }

        val result = compensator.compensate(scaledInput, reference, identityAssignment)

        assertEquals(2.0f, result.scaleFactor, 1e-3f)
        assertEquals(0f, result.rotationDegrees, 1e-3f)
        assertEquals(0f, result.rootMeanSquaredError, 1e-3f)

        for ((refStroke, resStroke) in reference.zip(result.strokes)) {
            for ((pRef, pRes) in refStroke.zip(resStroke)) {
                assertEquals(pRef.x, pRes.x, 1e-2f)
                assertEquals(pRef.y, pRes.y, 1e-2f)
            }
        }
    }

    @Test
    fun testPureRotationRecovery() {
        val angleDeg = 30f
        val rad = angleDeg * PI.toFloat() / 180f
        val cosA = cos(rad)
        val sinA = sin(rad)

        val rotatedInput = reference.map { stroke ->
            stroke.map { p -> ShapePoint(p.x * cosA - p.y * sinA, p.x * sinA + p.y * cosA) }
        }

        val result = compensator.compensate(rotatedInput, reference, identityAssignment)

        // Note: Inverse rotation of -30 deg aligns input back to reference
        assertEquals(-angleDeg, result.rotationDegrees, 1e-2f)
        assertEquals(1.0f, result.scaleFactor, 1e-3f)
        assertEquals(0f, result.rootMeanSquaredError, 1e-3f)

        for ((refStroke, resStroke) in reference.zip(result.strokes)) {
            for ((pRef, pRes) in refStroke.zip(resStroke)) {
                assertEquals(pRef.x, pRes.x, 1e-2f)
                assertEquals(pRef.y, pRes.y, 1e-2f)
            }
        }
    }

    @Test
    fun testCombinedTranslationRotationScalingRecovery() {
        val dx = 10f
        val dy = 20f
        val scale = 0.8f
        val angleDeg = 15f
        val rad = angleDeg * PI.toFloat() / 180f
        val cosA = cos(rad)
        val sinA = sin(rad)

        val transformedInput = reference.map { stroke ->
            stroke.map { p ->
                val sx = p.x * scale
                val sy = p.y * scale
                val rx = sx * cosA - sy * sinA + dx
                val ry = sx * sinA + sy * cosA + dy
                ShapePoint(rx, ry)
            }
        }

        val result = compensator.compensate(transformedInput, reference, identityAssignment)

        assertEquals(1f / scale, result.scaleFactor, 1e-2f)
        assertEquals(-angleDeg, result.rotationDegrees, 1e-1f)
        assertEquals(0f, result.rootMeanSquaredError, 1e-2f)

        for ((refStroke, resStroke) in reference.zip(result.strokes)) {
            for ((pRef, pRes) in refStroke.zip(resStroke)) {
                assertEquals(pRef.x, pRes.x, 1e-2f)
                assertEquals(pRef.y, pRes.y, 1e-2f)
            }
        }
    }
}
