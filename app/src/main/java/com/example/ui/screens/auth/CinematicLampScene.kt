package com.example.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Lightweight localized firefly model for the elongated beam.
 */
private data class BeamFirefly(
    val relX: Float,
    val relY: Float,
    val radius: Float,
    val speed: Float,
    val wanderX: Float,
    val wanderY: Float,
    val phase: Float,
    val color: Color
)

/**
 * Realistic Elongated Downward Volumetric Light Beam.
 *
 * Replaces circular/sun-like glows with an authentic downward light cone:
 * - Originates directly underneath the fixed lampshade opening
 * - Narrow near the bulb (~56dp), gradually wider as it travels downward (~260dp)
 * - Visibly longer vertically (~500dp) than it is wide
 * - Gaussian-feathered edges via [Modifier.blur(26.dp)] with ZERO hard geometric borders
 * - Directionally grows DOWNWARD from the lampshade via [TransformOrigin(0.5f, 0f)]
 * - Soft warm-white to champagne amber with very faint olive discipline atmosphere
 * - Localized fireflies that float inside the beam
 */
@Composable
fun DownwardVolumetricBeam(
    beamGrowProgress: Float,
    beamAlpha: Float,
    firefliesAlpha: Float,
    modifier: Modifier = Modifier
) {
    if (beamAlpha <= 0.005f) return

    val infiniteTransition = rememberInfiniteTransition(label = "beam_ambient_motion")
    val particleTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_time"
    )

    val ambientBreathing by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_breathing"
    )

    // 6 delicate fireflies localized strictly inside the elongated beam
    val fireflies = remember {
        val rand = Random(42)
        List(6) { index ->
            val colors = listOf(
                Color(0xFFFFFBEB),
                Color(0xFFFEF3C7),
                Color(0xFFFDE68A)
            )
            BeamFirefly(
                relX = (rand.nextFloat() - 0.5f) * 1.2f,
                relY = 0.18f + rand.nextFloat() * 0.70f,
                radius = 1.2f + rand.nextFloat() * 1.0f,
                speed = 0.04f + rand.nextFloat() * 0.05f,
                wanderX = 8f + rand.nextFloat() * 12f,
                wanderY = 8f + rand.nextFloat() * 10f,
                phase = rand.nextFloat() * 2f * PI.toFloat(),
                color = colors[index % colors.size]
            )
        }
    }

    Box(modifier = modifier) {
        // 1. Soft Elongated Volumetric Beam (Blurred with 26dp Gaussian blur so edges dissolve completely)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleY = beamGrowProgress
                    transformOrigin = TransformOrigin(0.5f, 0f) // Grows directly from shade downward
                    alpha = beamAlpha * ambientBreathing
                }
                .blur(radius = 26.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width

                // Fixed origin directly under the lampshade opening
                val originX = canvasW * 0.5f + 10.dp.toPx()
                val topY = 85.dp.toPx()
                val beamHeight = 500.dp.toPx()
                val bottomY = topY + beamHeight

                val topHalfWidth = 28.dp.toPx()
                val bottomHalfWidth = 130.dp.toPx()

                // PASS 1: Broad soft atmospheric envelope (dissolves into dark background)
                val outerPath = Path().apply {
                    moveTo(originX - topHalfWidth * 1.35f, topY)
                    lineTo(originX + topHalfWidth * 1.35f, topY)
                    lineTo(originX + bottomHalfWidth * 1.35f, bottomY)
                    lineTo(originX - bottomHalfWidth * 1.35f, bottomY)
                    close()
                }
                drawPath(
                    path = outerPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x30FFFBEB),
                            Color(0x14FEF3C7),
                            Color(0x0610B981), // subtle olive atmosphere
                            Color.Transparent
                        ),
                        startY = topY,
                        endY = bottomY
                    )
                )

                // PASS 2: Main visible volumetric light cone
                val mainPath = Path().apply {
                    moveTo(originX - topHalfWidth, topY)
                    lineTo(originX + topHalfWidth, topY)
                    lineTo(originX + bottomHalfWidth, bottomY)
                    lineTo(originX - bottomHalfWidth, bottomY)
                    close()
                }
                drawPath(
                    path = mainPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x60FFFBEB),
                            Color(0x2EFEF3C7),
                            Color(0x0CFDE68A),
                            Color.Transparent
                        ),
                        startY = topY,
                        endY = bottomY
                    )
                )

                // PASS 3: Central concentrated light column
                val corePath = Path().apply {
                    moveTo(originX - topHalfWidth * 0.55f, topY)
                    lineTo(originX + topHalfWidth * 0.55f, topY)
                    lineTo(originX + bottomHalfWidth * 0.55f, bottomY * 0.85f)
                    lineTo(originX - bottomHalfWidth * 0.55f, bottomY * 0.85f)
                    close()
                }
                drawPath(
                    path = corePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x75FFFFFF),
                            Color(0x30FFFBEB),
                            Color.Transparent
                        ),
                        startY = topY,
                        endY = bottomY * 0.85f
                    )
                )

                // LAYER 4: Soft diffuse floor illumination pool where the beam meets the ground
                val floorW = bottomHalfWidth * 2.2f
                val floorH = 50.dp.toPx()
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x18FEF3C7),
                            Color(0x0610B981),
                            Color.Transparent
                        ),
                        center = Offset(originX, bottomY),
                        radius = floorW * 0.5f
                    ),
                    topLeft = Offset(originX - floorW * 0.5f, bottomY - floorH * 0.5f),
                    size = Size(floorW, floorH)
                )
            }
        }

        // 2. Localized Fireflies (Fade in smoothly with firefliesAlpha AFTER beam grows down)
        if (firefliesAlpha > 0.01f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val originX = canvasW * 0.5f + 10.dp.toPx()
                val topY = 85.dp.toPx()
                val beamHeight = 480.dp.toPx()

                for (fly in fireflies) {
                    val cycle = (particleTime * fly.speed + fly.phase) % 1f
                    val curY = topY + (fly.relY * beamHeight) + (sin(cycle * 2 * PI + fly.phase) * fly.wanderY).toFloat()
                    val progressY = ((curY - topY) / beamHeight).coerceIn(0.1f, 1.0f)
                    val currentSpan = (120.dp.toPx() * progressY)

                    val curX = originX + (fly.relX * currentSpan) + (sin(cycle * 2 * PI + fly.phase * 1.3f) * fly.wanderX).toFloat()

                    val pulse = (sin(cycle * 4 * PI + fly.phase) * 0.30f + 0.70f).toFloat()
                    val particleAlpha = (pulse * firefliesAlpha).coerceIn(0f, 1f)

                    drawCircle(
                        color = fly.color.copy(alpha = particleAlpha * 0.28f),
                        radius = fly.radius * 2.2f,
                        center = Offset(curX, curY)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = particleAlpha * 0.90f),
                        radius = fly.radius,
                        center = Offset(curX, curY)
                    )
                }
            }
        }
    }
}

/**
 * Fixed Interactive Physical Lamp Fixture.
 *
 * Anchored in a fixed visual position at the top-center.
 * NEVER moves, NEVER translates, NEVER hides behind the login card.
 *
 * Hierarchy:
 * 1. Tiny warm-white bulb core directly inside shade
 * 2. Very small soft glow immediately around bulb (ONLY circular element)
 * 3. Physical shade, curved stand, base, and dangling cord with brass bead
 */
@Composable
fun CinematicLampFixture(
    lampOn: Boolean,
    bulbProgress: Float,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isCordPulling by remember { mutableStateOf(false) }
    val cordPullOffset by animateFloatAsState(
        targetValue = if (isCordPulling) 13f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        finishedListener = {
            if (isCordPulling) isCordPulling = false
        },
        label = "fixture_cord_pull"
    )

    // Subtle idle breathing pulse on rim when lamp is off
    val infiniteTransition = rememberInfiniteTransition(label = "fixture_idle_pulse")
    val idleRimAlpha by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_rim_alpha"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    isCordPulling = true
                    onToggle()
                }
            )
            .semantics {
                role = Role.Button
                contentDescription = if (lampOn) "DayForge Lamp is On. Tap to turn off" else "DayForge Lamp is Off. Tap to turn on"
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width

            val shadeWidth = 98.dp.toPx()
            val shadeHeight = 58.dp.toPx()
            val shadeCenter = Offset(canvasW * 0.5f + 10.dp.toPx(), 56.dp.toPx())

            val topWidth = shadeWidth * 0.44f
            val bottomWidth = shadeWidth
            val topY = shadeCenter.y - shadeHeight * 0.5f
            val bottomY = shadeCenter.y + shadeHeight * 0.5f

            // 1. Arched Lamp Pole & Base (Anchored on the left, curving overhead)
            val poleBaseX = shadeCenter.x - shadeWidth * 0.65f
            val poleBaseY = bottomY + 42.dp.toPx()

            // Metallic Base
            val baseW = shadeWidth * 0.78f
            val baseH = 8.dp.toPx()
            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF32353E), Color(0xFF1D1F25), Color(0xFF101114))
                ),
                topLeft = Offset(poleBaseX - baseW / 2f, poleBaseY - baseH / 2f),
                size = Size(baseW, baseH)
            )

            // Pole Path
            val polePath = Path().apply {
                moveTo(poleBaseX, poleBaseY)
                cubicTo(
                    poleBaseX + 4f, poleBaseY - 48.dp.toPx(),
                    shadeCenter.x - 18.dp.toPx(), topY - 14.dp.toPx(),
                    shadeCenter.x, topY
                )
            }
            drawPath(
                path = polePath,
                color = Color(0xFF22242B),
                style = Stroke(width = 3.6f, cap = StrokeCap.Round)
            )
            drawPath(
                path = polePath,
                color = Color(0x33CBD5E1),
                style = Stroke(width = 1.1f, cap = StrokeCap.Round)
            )

            // 2. Lampshade Body (Trapezoid dome)
            val shadePath = Path().apply {
                moveTo(shadeCenter.x - topWidth / 2f, topY)
                lineTo(shadeCenter.x + topWidth / 2f, topY)
                lineTo(shadeCenter.x + bottomWidth / 2f, bottomY)
                lineTo(shadeCenter.x - bottomWidth / 2f, bottomY)
                close()
            }

            drawPath(
                path = shadePath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2A2D35),
                        Color(0xFF1B1D23),
                        Color(0xFF111216)
                    ),
                    start = Offset(shadeCenter.x - shadeWidth / 2f, topY),
                    end = Offset(shadeCenter.x + shadeWidth / 2f, bottomY)
                )
            )

            // Top Cap of Shade
            drawOval(
                color = Color(0xFF14161A),
                topLeft = Offset(shadeCenter.x - topWidth / 2f, topY - 2.5f),
                size = Size(topWidth, 5f)
            )

            // 3. Lampshade Bottom Rim
            val rimColor = if (lampOn) {
                Color(0xFFFFFBEB)
            } else {
                Color(0xFFCBD5E1).copy(alpha = idleRimAlpha)
            }
            drawOval(
                color = rimColor,
                topLeft = Offset(shadeCenter.x - bottomWidth / 2f, bottomY - 2.5f),
                size = Size(bottomWidth, 5f),
                style = Stroke(width = if (lampOn) 1.8f else 1.2f)
            )

            // 4. BULB: Small warm-white bright core & small circular glow immediately around bulb
            if (bulbProgress > 0.01f) {
                val bulbCenter = Offset(shadeCenter.x, bottomY)

                // Very small soft circular glow immediately around bulb (tight radius)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x90FFFBEB).copy(alpha = 0.55f * bulbProgress),
                            Color(0x30FEF3C7).copy(alpha = 0.25f * bulbProgress),
                            Color.Transparent
                        ),
                        center = bulbCenter,
                        radius = 24.dp.toPx()
                    ),
                    center = bulbCenter,
                    radius = 24.dp.toPx()
                )

                // Tiny bright warm-white core directly inside shade opening
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF).copy(alpha = bulbProgress),
                            Color(0xFFFFFBEB).copy(alpha = 0.90f * bulbProgress),
                            Color.Transparent
                        ),
                        center = bulbCenter,
                        radius = 10.dp.toPx()
                    ),
                    center = bulbCenter,
                    radius = 10.dp.toPx()
                )

                // Incandescent filament spark
                drawCircle(
                    color = Color.White.copy(alpha = bulbProgress),
                    radius = 4.dp.toPx() * bulbProgress,
                    center = Offset(bulbCenter.x, bulbCenter.y - 2.dp.toPx())
                )
            }

            // 5. Hanging Pull-Cord with Bead
            val cordStartX = shadeCenter.x + 8.dp.toPx()
            val cordStartY = bottomY
            val cordLength = 25.dp.toPx() + cordPullOffset
            val cordEndY = cordStartY + cordLength

            // String
            drawLine(
                color = if (lampOn) Color(0xFFFFFBEB) else Color(0x66CBD5E1),
                start = Offset(cordStartX, cordStartY),
                end = Offset(cordStartX, cordEndY),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round
            )

            // Brass Bead
            val beadRadius = 4.0f
            val beadCenter = Offset(cordStartX, cordEndY + beadRadius)
            drawCircle(
                color = if (lampOn) Color(0xFFFEF3C7) else Color(0xFFC79E2E),
                radius = beadRadius,
                center = beadCenter
            )
            if (!lampOn) {
                drawCircle(
                    color = Color(0xFFD4AF37).copy(alpha = 0.32f * idleRimAlpha),
                    radius = beadRadius + 3f,
                    center = beadCenter
                )
            } else {
                drawCircle(
                    color = Color(0x66FFFBEB),
                    radius = beadRadius + 2.5f,
                    center = beadCenter
                )
            }
        }
    }
}
