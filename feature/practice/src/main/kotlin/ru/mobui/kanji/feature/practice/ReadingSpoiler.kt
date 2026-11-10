package ru.mobui.kanji.feature.practice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Telegram-style reading spoiler over the whole reading band. Dense speckles only — no haze blob.
 * Uses a pre-rendered frame loop (8 frames @ 11 FPS) to eliminate mathematical particle loops during display.
 * Reveal is one-way for the current word; the host records hint usage.
 */
@Composable
internal fun ReadingSpoiler(
    reading: String,
    revealed: Boolean,
    enabled: Boolean,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hiddenLabel = stringResource(R.string.reading_hidden)
    val revealedLabel = stringResource(R.string.reading_revealed)
    val haptics = LocalHapticFeedback.current
    val particleColor = MaterialTheme.colorScheme.onSurface
    // Snap closed on a new word; only animate when the learner opens the spoiler.
    val revealProgress = remember(reading) { Animatable(if (revealed) 0f else 1f) }
    LaunchedEffect(reading, revealed) {
        if (revealed) {
            if (revealProgress.value > 0f) {
                revealProgress.animateTo(0f, animationSpec = tween(durationMillis = REVEAL_DURATION_MILLIS))
            }
        } else {
            revealProgress.snapTo(1f)
        }
    }
    val progress = revealProgress.value
    val canReveal = enabled && !revealed
    val interactionSource = remember { MutableInteractionSource() }
    val reveal = {
        if (canReveal) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onReveal()
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp)
            .heightIn(min = 44.dp)
            .testTag("word-reading")
            .then(
                if (canReveal) {
                    // No ripple — it clashes with the spoiler grain.
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = reveal,
                    )
                } else {
                    Modifier
                },
            )
            .clearAndSetSemantics {
                contentDescription = if (revealed) reading else hiddenLabel
                stateDescription = if (revealed) revealedLabel else hiddenLabel
                if (canReveal) {
                    role = Role.Button
                    onClick(label = hiddenLabel) {
                        reveal()
                        true
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            reading,
            style = MaterialTheme.typography.titleMedium,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 1f - progress),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        if (progress > SPOILER_VISIBILITY_THRESHOLD) {
            SpoilerNoise(
                progress = progress,
                revealed = revealed,
                particleColor = particleColor,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
internal fun SpoilerNoise(
    progress: Float,
    revealed: Boolean,
    particleColor: Color,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var frameIndex by remember { mutableIntStateOf(0) }

    // While closed, cycle through pre-rendered frames at ~11 FPS (90ms per frame).
    LaunchedEffect(revealed) {
        if (!revealed) {
            while (isActive) {
                delay(SPOILER_FRAME_INTERVAL_MILLIS)
                frameIndex = (frameIndex + 1) % SPOILER_FRAME_COUNT
            }
        }
    }

    val frames = remember(size.width, size.height, particleColor) {
        if (size.width > 0 && size.height > 0) {
            runCatching {
                renderSpoilerFrames(
                    width = size.width,
                    height = size.height,
                    particleColor = particleColor,
                )
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
    }

    Canvas(
        modifier = modifier.onSizeChanged { size = it },
    ) {
        if (frames.isNotEmpty()) {
            val frame = frames[frameIndex % frames.size]
            drawImage(
                image = frame,
                alpha = progress,
            )
        }
    }
}

private fun renderSpoilerFrames(
    width: Int,
    height: Int,
    particleColor: Color,
): List<ImageBitmap> {
    val paint = Paint()
    val softX = (width * SOFT_EDGE_WIDTH_FRACTION).coerceAtLeast(SOFT_EDGE_MIN_X)
    val softY = (height * SOFT_EDGE_HEIGHT_FRACTION).coerceAtLeast(SOFT_EDGE_MIN_Y)
    val wFloat = width.toFloat()
    val hFloat = height.toFloat()

    return List(SPOILER_FRAME_COUNT) { frameIndex ->
        val bitmap = ImageBitmap(width, height)
        val canvas = GraphicsCanvas(bitmap)
        val phase = frameIndex.toFloat() / SPOILER_FRAME_COUNT
        val twinkleBase = phase * (Math.PI * TWINKLE_BASE_FACTOR).toFloat()

        for (particle in spoilerParticles) {
            val edge = softEdge(
                x = particle.x * wFloat,
                y = particle.y * hFloat,
                width = wFloat,
                height = hFloat,
                softX = softX,
                softY = softY,
            )
            if (edge <= SOFT_EDGE_ALPHA_THRESHOLD) continue

            val wobble = twinkleBase + particle.spin
            val ox = cos(wobble) * particle.drift
            val oy = sin(wobble * DRIFT_Y_MULTIPLIER) * particle.drift
            val px = particle.x * wFloat + ox
            val py = particle.y * hFloat + oy
            val twinkle =
                TWINKLE_MIN_ALPHA +
                    TWINKLE_DEPTH_FACTOR * ((sin(wobble * TWINKLE_SPEED_MULTIPLIER) + 1f) * TWINKLE_NORMALIZE_FACTOR)

            paint.color = particleColor.copy(alpha = particle.alpha * edge * twinkle)
            canvas.drawCircle(Offset(px, py), particle.radius, paint)
        }
        bitmap
    }
}

/** Mild rim fade so the block edge is not a hard cut; interior stays fully dense. */
private fun softEdge(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    softX: Float,
    softY: Float,
): Float {
    val left = (x / softX).coerceIn(0f, 1f)
    val right = ((width - x) / softX).coerceIn(0f, 1f)
    val top = (y / softY).coerceIn(0f, 1f)
    val bottom = ((height - y) / softY).coerceIn(0f, 1f)
    return minOf(left, right, top, bottom)
}

private data class SpoilerParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val drift: Float,
    val spin: Float,
    val alpha: Float,
)

private val spoilerParticles: List<SpoilerParticle> by lazy {
    val random = Random(READING_SPOILER_SEED)
    List(PARTICLE_COUNT) {
        SpoilerParticle(
            x = random.nextFloat(),
            y = random.nextFloat(),
            radius = PARTICLE_BASE_RADIUS + random.nextFloat() * PARTICLE_RADIUS_SPREAD,
            drift = PARTICLE_BASE_DRIFT + random.nextFloat() * PARTICLE_DRIFT_SPREAD,
            spin = random.nextFloat() * (Math.PI * TWO_PI_FACTOR).toFloat(),
            alpha = PARTICLE_BASE_ALPHA + random.nextFloat() * PARTICLE_ALPHA_SPREAD,
        )
    }
}

private const val PARTICLE_COUNT = 1100
private const val READING_SPOILER_SEED = 0x52EAD1A6
private const val SPOILER_FRAME_COUNT = 8
private const val SPOILER_FRAME_INTERVAL_MILLIS = 90L
internal const val REVEAL_DURATION_MILLIS = 380
internal const val SPOILER_VISIBILITY_THRESHOLD = 0.01f

private const val PARTICLE_BASE_RADIUS = 0.9f
private const val PARTICLE_RADIUS_SPREAD = 1.7f
private const val PARTICLE_BASE_DRIFT = 0.2f
private const val PARTICLE_DRIFT_SPREAD = 0.55f
private const val PARTICLE_BASE_ALPHA = 0.65f
private const val PARTICLE_ALPHA_SPREAD = 0.35f

private const val SOFT_EDGE_WIDTH_FRACTION = 0.08f
private const val SOFT_EDGE_HEIGHT_FRACTION = 0.22f
private const val SOFT_EDGE_MIN_X = 8f
private const val SOFT_EDGE_MIN_Y = 6f
private const val SOFT_EDGE_ALPHA_THRESHOLD = 0.05f

private const val TWO_PI_FACTOR = 2.0

private const val TWINKLE_BASE_FACTOR = 2f
private const val TWINKLE_SPEED_MULTIPLIER = 2.3f
private const val TWINKLE_DEPTH_FACTOR = 0.3f
private const val TWINKLE_MIN_ALPHA = 0.7f
private const val TWINKLE_NORMALIZE_FACTOR = 0.5f
private const val DRIFT_Y_MULTIPLIER = 1.31f
