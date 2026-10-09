package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlin.math.sin

/**
 * Screen 1: Splash Screen — EXACT Match to Reference Image (Design 5: Black Glass / iOS 27 Vibe)
 *
 * Visual Structure:
 * - Full-screen cinematic dark mountain/night background with reflective lake & trees
 * - Deep black/navy atmosphere with purple/magenta nebula & electric cyan auroral light arc
 * - Center:
 *   - DAYFORGE 3-peak Mountain Logo in crisp white with radiant cyan bloom
 *   - "DAYFORGE" wordmark in bold, tracked uppercase typography
 *   - "Discipline Builds Freedom" tagline in clean white/cool-gray
 * - Bottom:
 *   - Glowing electric cyan rounded pill progress bar filling smoothly over 4.0 seconds (0 -> 100%)
 *   - "Building a Better You..." centered caption text
 * - Supports Reduced Motion and triggers onAnimationComplete() seamlessly.
 */
@Composable
fun DayForgeLiveLaunchAnimation(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier,
    isReducedMotion: Boolean = false
) {
    val context = LocalContext.current
    val prefersReducedMotion = remember(context, isReducedMotion) {
        if (isReducedMotion) true
        else {
            try {
                android.provider.Settings.Global.getFloat(
                    context.contentResolver,
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                    1.0f
                ) == 0f
            } catch (e: Exception) {
                false
            }
        }
    }

    val totalDurationMs = if (prefersReducedMotion) 350 else 4000
    val animTime = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animTime.animateTo(
            targetValue = totalDurationMs.toFloat(),
            animationSpec = tween(
                durationMillis = totalDurationMs,
                easing = LinearEasing
            )
        )
        onAnimationComplete()
    }

    val currentMs = animTime.value
    val overallProgress = (currentMs / totalDurationMs.toFloat()).coerceIn(0f, 1f)

    // Subtle breathing pulse for the auroral glow and logo
    val infiniteTransition = rememberInfiniteTransition(label = "splash_aurora_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    // Phase progress milestones
    val logoScale = if (prefersReducedMotion) 1f else (0.90f + (0.10f * overallProgress.coerceAtMost(0.35f) / 0.35f))
    val contentAlpha = if (prefersReducedMotion) 1f else (overallProgress / 0.15f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF04060C))
            .testTag("dayforge_launch_animation")
    ) {
        // --- 1. Fullscreen Cinematic Mountain Wallpaper ---
        Image(
            painter = painterResource(id = R.drawable.img_cinematic_mountain_bg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.45f },
            contentScale = ContentScale.Crop
        )

        // --- 2. Atmospheric Night Glows, Nebula & Cosmic Ray (Screen 1 Visuals) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Deep vignette gradient
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x9904060C),
                        Color(0x400A1124),
                        Color(0xE6030509)
                    )
                )
            )

            // Upper-left vibrant Electric Cyan auroral beam arcing down
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x5500F0FF).copy(alpha = 0.35f * pulseGlow),
                        Color(0x2000F0FF),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.22f, height * 0.18f),
                    radius = width * 0.90f
                )
            )

            // Right-side deep Violet / Magenta nebula glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x44A855F7).copy(alpha = 0.30f * pulseGlow),
                        Color(0x18D946EF),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.85f, height * 0.32f),
                    radius = width * 0.80f
                )
            )

            // Lower lake reflection cyan tint
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x2238BDF8), Color.Transparent),
                    center = Offset(width * 0.50f, height * 0.85f),
                    radius = width * 0.70f
                )
            )

            // Subtle floating cosmic micro-particles
            val t = currentMs / 1000f
            val p1 = Offset(width * 0.18f + sin(t * 0.9f) * 10f, height * 0.25f - (t * 8f) % 50f)
            val p2 = Offset(width * 0.82f - sin(t * 1.1f) * 12f, height * 0.30f - (t * 10f) % 70f)
            val p3 = Offset(width * 0.52f + sin(t * 0.7f) * 8f, height * 0.48f - (t * 6f) % 40f)
            val p4 = Offset(width * 0.25f + sin(t * 1.2f) * 14f, height * 0.65f - (t * 9f) % 60f)
            val p5 = Offset(width * 0.75f - sin(t * 0.8f) * 10f, height * 0.70f - (t * 7f) % 55f)

            drawCircle(Color(0xCC00F0FF), radius = 2.dp.toPx(), center = p1)
            drawCircle(Color(0xCCA855F7), radius = 2.5.dp.toPx(), center = p2)
            drawCircle(Color(0xEEFFFFFF), radius = 1.8.dp.toPx(), center = p3)
            drawCircle(Color(0xCC38BDF8), radius = 2.dp.toPx(), center = p4)
            drawCircle(Color(0xCCFFFFFF), radius = 1.5.dp.toPx(), center = p5)
        }

        // --- 3. Center Screen 1: Mountain Logo, "DAYFORGE" & Tagline ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    alpha = contentAlpha
                    scaleX = logoScale
                    scaleY = logoScale
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Exact 3-peak Mountain Logo
                DayForgeMountainLogo(
                    size = 92.dp,
                    tint = Color.White,
                    glowColor = Color(0xFF00F0FF)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // "DAYFORGE" Wordmark
                Text(
                    text = "DAYFORGE",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 7.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // "Discipline Builds Freedom" Tagline
                Text(
                    text = "Discipline Builds Freedom",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )
            }
        }

        // --- 4. Bottom Screen 1: Electric Cyan Progress Bar & "Building a Better You..." ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 52.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Rounded Pill Progress Track & Active Glowing Cyan Bar
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x401E293B))
                    .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(3.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val trackWidth = size.width
                    val filledWidth = trackWidth * overallProgress

                    if (filledWidth > 0f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF06B6D4),
                                    Color(0xFF00F0FF),
                                    Color(0xFF38BDF8)
                                )
                            ),
                            size = Size(filledWidth, size.height),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }
            }

            // Caption from Reference Image: "Building a Better You..."
            Text(
                text = "Building a Better You...",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8),
                fontSize = 12.5.sp,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
