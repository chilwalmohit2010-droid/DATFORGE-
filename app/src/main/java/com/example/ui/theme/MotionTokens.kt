package com.example.ui.theme

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

/**
 * Centralized iOS 27-Inspired Motion System for DAYFORGE.
 * Provides physically consistent spring curves, interactive tactile feedback,
 * and high-refresh-rate 120Hz optimized compositor animations.
 */
object MotionTokens {

    // Natural iOS curve constants
    val IosDecelerateEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val IosStandardEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
    val IosAccelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    // Standard durations
    const val DURATION_INSTANT = 100
    const val DURATION_FAST = 180
    const val DURATION_STANDARD = 240
    const val DURATION_MEDIUM = 320
    const val DURATION_PAGE = 260

    // Core Spring Specs
    fun <T> fastSpring(
        stiffness: Float = 500f,
        dampingRatio: Float = 0.84f
    ): SpringSpec<T> = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )

    fun <T> standardSpring(
        stiffness: Float = 380f,
        dampingRatio: Float = 0.86f
    ): SpringSpec<T> = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )

    fun <T> sheetSpring(
        stiffness: Float = 340f,
        dampingRatio: Float = 0.88f
    ): SpringSpec<T> = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )

    fun <T> tabIndicatorSpring(
        stiffness: Float = 420f,
        dampingRatio: Float = 0.82f
    ): SpringSpec<T> = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )

    fun <T> subtleBounceSpring(
        stiffness: Float = 450f,
        dampingRatio: Float = 0.78f
    ): SpringSpec<T> = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )
}

/**
 * Reusable iOS tactile spring press interaction.
 * Uses compositor-only graphicsLayer (scaleX, scaleY, alpha)
 * so it never triggers UI reflows or recomposition stutters on 120Hz screens.
 */
fun Modifier.iosTactilePress(
    scaleDown: Float = 0.972f,
    pressedAlpha: Float = 0.94f,
    isReducedMotion: Boolean = false,
    enableHaptic: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    LaunchedEffect(isPressed) {
        if (isPressed && enableHaptic) {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (_: Exception) {}
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (!isReducedMotion && isPressed) scaleDown else 1f,
        animationSpec = MotionTokens.standardSpring(),
        label = "iosTactileScale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (!isReducedMotion && isPressed) pressedAlpha else 1f,
        animationSpec = MotionTokens.fastSpring(),
        label = "iosTactileAlpha"
    )

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            alpha = animatedAlpha
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
