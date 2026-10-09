package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

/**
 * iOS-inspired tactile spring press interaction modifier.
 * Uses compositor-friendly graphicsLayer transform (scaleX/scaleY)
 * avoiding relayout/reflow for 120Hz smooth response.
 */
@Composable
fun Modifier.tactilePress(
    scaleDown: Float = 0.972f,
    isReducedMotion: Boolean = false,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (_: Exception) {}
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (!isReducedMotion && isPressed) scaleDown else 1f,
        animationSpec = MotionTokens.standardSpring(),
        label = "tactilePressScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )
}

/**
 * Liquid Glass Background with cinematic dark mountain wallpaper,
 * deep navy ambient base, and atmospheric Electric Cyan & Purple nebula glows.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF04060C))
    ) {
        // 1. Cinematic dark moody mountain wallpaper
        Image(
            painter = painterResource(id = R.drawable.img_cinematic_mountain_bg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.32f },
            contentScale = ContentScale.Crop
        )

        // 2. Atmospheric luminous radial glows & dark glass gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val width = size.width
                    val height = size.height

                    // Deep vignette gradient
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0x99050811),
                                Color(0x66080E1C),
                                Color(0xDE030509)
                            )
                        )
                    )

                    // Electric Cyan luminous horizon glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x3800F0FF), Color(0x0C00F0FF), Color.Transparent),
                            center = Offset(width * 0.18f, height * 0.20f),
                            radius = width * 0.85f
                        ),
                        center = Offset(width * 0.18f, height * 0.20f),
                        radius = width * 0.85f
                    )

                    // Violet / Purple nebula atmospheric glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x30A855F7), Color(0x08A855F7), Color.Transparent),
                            center = Offset(width * 0.88f, height * 0.42f),
                            radius = width * 0.75f
                        ),
                        center = Offset(width * 0.88f, height * 0.42f),
                        radius = width * 0.75f
                    )

                    // Subtle Magenta / Blue glow at lower edge
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x2438BDF8), Color(0x06D946EF), Color.Transparent),
                            center = Offset(width * 0.35f, height * 0.88f),
                            radius = width * 0.70f
                        ),
                        center = Offset(width * 0.35f, height * 0.88f),
                        radius = width * 0.70f
                    )
                }
        )

        // 3. Page content
        content()
    }
}

/**
 * Reusable Liquid Glass Surface component with dynamic luminous border,
 * inner glass reflection, soft shadow, and tactile spring motion.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    backgroundColor: Color = Color(0x8C0E1526),
    borderColor: Color = Color(0x3DFFFFFF),
    borderGlowColor: Color = Color(0x2638BDF8),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    isReducedMotion: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    LaunchedEffect(isPressed) {
        if (isPressed && onClick != null) {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (_: Exception) {}
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (!isReducedMotion && isPressed && onClick != null) 0.976f else 1f,
        animationSpec = MotionTokens.standardSpring(),
        label = "glassSurfaceScale"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(
                            Color(0x8CFFFFFF),
                            Color(0x4000F0FF),
                            Color(0x20FFFFFF)
                        )
                    } else {
                        listOf(
                            Color(0x59FFFFFF),
                            borderGlowColor,
                            Color(0x14FFFFFF)
                        )
                    }
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x1F38BDF8),
                            Color.Transparent,
                            Color(0x0F000000)
                        )
                    )
                )
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0x8C11182B),
    borderColor: Color = Color(0x38FFFFFF),
    cornerRadius: Dp = 22.dp,
    elevation: Dp = 0.dp,
    isReducedMotion: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    LaunchedEffect(isPressed) {
        if (isPressed && onClick != null) {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (_: Exception) {}
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (!isReducedMotion && onClick != null && isPressed) 0.978f else 1f,
        animationSpec = MotionTokens.standardSpring(),
        label = "glassCardScale"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(
                            Color(0x8CFFFFFF),
                            Color(0x4038BDF8),
                            Color(0x1AFFFFFF)
                        )
                    } else {
                        listOf(
                            Color(0x59FFFFFF),
                            Color(0x2638BDF8),
                            Color(0x10FFFFFF)
                        )
                    }
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        tonalElevation = elevation
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x1AFFFFFF),
                            Color.Transparent,
                            Color(0x0C000000)
                        )
                    )
                )
                .padding(16.dp),
            content = content
        )
    }
}

/**
 * iOS-style Primary Glass Button with electric cyan/blue gradient and subtle glow.
 */
@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    gradientColors: List<Color> = listOf(NeonCyan, Color(0xFF2563EB))
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .height(52.dp)
            .tactilePress(scaleDown = 0.968f),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.Black
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(gradientColors),
                    shape = RoundedCornerShape(26.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color(0x80FFFFFF), Color(0x26FFFFFF))
                    ),
                    shape = RoundedCornerShape(26.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.Black,
                    strokeWidth = 2.5.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Black
                        )
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * Premium Glass Input Field with dark glass background, subtle border,
 * and electric cyan focused border glow.
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextMuted) },
            leadingIcon = if (leadingIcon != null) {
                { Icon(leadingIcon, contentDescription = null, tint = if (isError) CoralRed else TextSecondary) }
            } else null,
            trailingIcon = trailingIcon,
            isError = isError,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xA60E1628),
                unfocusedContainerColor = Color(0x660A101E),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = if (isError) CoralRed else NeonCyan,
                unfocusedBorderColor = if (isError) Color(0x66EF4444) else Color(0x26FFFFFF),
                cursorColor = NeonCyan
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = CoralRed,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
fun DisciplineScoreRing(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 86.dp,
    strokeWidth: Dp = 8.dp,
    animated: Boolean = true
) {
    val targetProgress = (score.coerceIn(0, 100) / 100f)
    val animatedProgress by animateFloatAsState(
        targetValue = if (animated) targetProgress else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "scoreProgress"
    )

    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "scoreInt"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // Background Track
            drawArc(
                color = Color(0x26FFFFFF),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Ambient Glow Under Arc
            if (animatedProgress > 0f) {
                drawArc(
                    color = Color(0x4022C55E),
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx + 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Neon Green Progress Arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        NeonGreenBright,
                        NeonGreen,
                        NeonGreenBright
                    )
                ),
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "${animatedScore}%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
    }
}

@Composable
fun StreakFlameBadge(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x33A855F7))
            .border(1.dp, Color(0x4DA855F7), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "⚡",
            fontSize = 13.sp
        )
        Text(
            text = "$streakDays DAY${if (streakDays != 1) "S" else ""}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = NeonPurple
        )
    }
}

@Composable
fun WatermarkFooter(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "created by mohit",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted.copy(alpha = 0.85f),
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            letterSpacing = 0.4.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "💚",
            fontSize = 11.sp
        )
    }
}

@Composable
fun CategoryIcon(
    iconName: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val vector = when (iconName.lowercase()) {
        "favorite", "health" -> Icons.Filled.Favorite
        "fitness_center", "fitness" -> Icons.Filled.FitnessCenter
        "bolt", "productivity" -> Icons.Filled.Bolt
        "psychology", "mindset" -> Icons.Filled.Psychology
        "schedule", "routine" -> Icons.Filled.Schedule
        "menu_book", "learning" -> Icons.Filled.MenuBook
        "payments", "finance" -> Icons.Filled.Payments
        "water_drop" -> Icons.Filled.WaterDrop
        "flag" -> Icons.Filled.Flag
        "military_tech" -> Icons.Filled.MilitaryTech
        "bedtime", "sleep" -> Icons.Filled.Bedtime
        else -> Icons.Filled.CheckCircle
    }

    Icon(
        imageVector = vector,
        contentDescription = iconName,
        tint = tint,
        modifier = modifier
    )
}

/**
 * Standardized Liquid Glass Banner for subtle success/error notifications.
 */
@Composable
fun GlassBanner(
    message: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    isReducedMotion: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = message.isNotBlank(),
        enter = if (isReducedMotion) androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(0))
                else androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(250)) + androidx.compose.animation.expandVertically(),
        exit = if (isReducedMotion) androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(0))
               else androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)) + androidx.compose.animation.shrinkVertically(),
        modifier = modifier
    ) {
        val accentColor = if (isError) CoralRed else NeonGreenBright
        val bgColor = if (isError) Color(0x33EF4444) else Color(0x3322C55E)
        val borderColor = if (isError) Color(0x66EF4444) else Color(0x6622C55E)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = bgColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Standardized Liquid Glass Empty State Component.
 */
@Composable
fun GlassEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color(0x40FFFFFF), Color(0x1AFFFFFF), Color(0x0AFFFFFF))
                ),
                RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color(0x80131C2E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x3322C55E))
                    .border(1.dp, Color(0x4D22C55E), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonGreenBright,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )

            if (!actionLabel.isNullOrBlank() && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Exact geometric DAYFORGE 3-peak Mountain Logo matching the reference image.
 * Three sharp white mountain triangles:
 * - Left flank peak
 * - Center prominent peak with inner sharp chevron cutout
 * - Right flank peak
 * With subtle electric cyan glow.
 */
@Composable
fun DayForgeMountainLogo(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    tint: Color = Color.White,
    glowColor: Color = Color(0xFF00F0FF)
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Outer soft glow bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = w * 0.65f
                )
            )

            val strokeWidth = w * 0.052f

            // Left peak
            val leftPath = Path().apply {
                moveTo(w * 0.10f, h * 0.85f)
                lineTo(w * 0.32f, h * 0.42f)
                lineTo(w * 0.50f, h * 0.85f)
            }
            drawPath(
                path = leftPath,
                color = tint,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Right peak
            val rightPath = Path().apply {
                moveTo(w * 0.50f, h * 0.85f)
                lineTo(w * 0.68f, h * 0.44f)
                lineTo(w * 0.90f, h * 0.85f)
            }
            drawPath(
                path = rightPath,
                color = tint,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Center large peak
            val centerPath = Path().apply {
                moveTo(w * 0.20f, h * 0.85f)
                lineTo(w * 0.50f, h * 0.12f)
                lineTo(w * 0.80f, h * 0.85f)
            }
            drawPath(
                path = centerPath,
                color = tint,
                style = Stroke(width = strokeWidth * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Inner chevron peak cutout
            val innerCutoutPath = Path().apply {
                moveTo(w * 0.38f, h * 0.85f)
                lineTo(w * 0.50f, h * 0.52f)
                lineTo(w * 0.62f, h * 0.85f)
            }
            drawPath(
                path = innerCutoutPath,
                color = tint,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun DayForgeBrandingHeader(
    modifier: Modifier = Modifier,
    logoSize: Dp = 68.dp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DayForgeMountainLogo(size = logoSize)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "DAYFORGE",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 6.sp,
            color = Color.White
        )
        Text(
            text = "Discipline Builds Freedom",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

/**
 * Universal Liquid Glass Toast floating feedback overlay.
 * Displays brief success/info/error messages without blocking interactions.
 */
@Composable
fun GlassToastHost(
    message: String?,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    isReducedMotion: Boolean = false,
    onDismiss: () -> Unit = {}
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = if (isReducedMotion) androidx.compose.animation.fadeIn(tween(150))
        else androidx.compose.animation.fadeIn(tween(220)) + androidx.compose.animation.slideInVertically(
            initialOffsetY = { -it / 2 },
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
        ) + scaleIn(initialScale = 0.94f),
        exit = if (isReducedMotion) androidx.compose.animation.fadeOut(tween(150))
        else androidx.compose.animation.fadeOut(tween(180)) + androidx.compose.animation.slideOutVertically(
            targetOffsetY = { -it / 2 }
        ) + scaleOut(targetScale = 0.96f),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 12.dp, start = 20.dp, end = 20.dp)
            .testTag("glass_toast_host")
    ) {
        if (!message.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                if (isError) CoralRed.copy(alpha = 0.7f) else NeonGreenBright.copy(alpha = 0.6f),
                                Color(0x33FFFFFF)
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xF00D1527),
                shadowElevation = 18.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isError) Color(0x33EF4444) else Color(0x2E22C55E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (isError) CoralRed else NeonGreenBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Standard Achievement Celebration Overlay.
 * Appears with polished liquid glass when an achievement unlocks.
 */
@Composable
fun AchievementCelebrationDialog(
    achievementTitle: String,
    achievementDescription: String,
    onDismiss: () -> Unit,
    isReducedMotion: Boolean = false
) {
    val scaleAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (isReducedMotion) tween(0) else spring(
            dampingRatio = 0.76f,
            stiffness = 380f
        ),
        label = "achCelebrateScale"
    )

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .graphicsLayer {
                    scaleX = scaleAnim
                    scaleY = scaleAnim
                }
                .clip(RoundedCornerShape(28.dp))
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color(0x99FBBF24),
                            Color(0x3300F0FF),
                            Color(0x1AFFFFFF)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
                .testTag("achievement_celebration_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xF2080E1C),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0x4DFBBF24), Color(0x15FBBF24), Color.Transparent)
                            )
                        )
                        .border(1.5.dp, Color(0x80FBBF24), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Trophy",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x26FBBF24)
                ) {
                    Text(
                        text = "ACHIEVEMENT UNLOCKED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.6.sp,
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.5.sp
                    )
                }

                Text(
                    text = achievementTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp
                )

                Text(
                    text = achievementDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .tactilePress(scaleDown = 0.95f) { onDismiss() }
                        .testTag("achievement_dismiss_button")
                ) {
                    Text(
                        text = "Forge Ahead",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                }
            }
        }
    }
}


