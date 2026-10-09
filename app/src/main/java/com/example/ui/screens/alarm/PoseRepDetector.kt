package com.example.ui.screens.alarm

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.models.ChallengeType
import com.example.ui.theme.*
import java.util.concurrent.Executors
import kotlin.math.abs

enum class ExercisePhase {
    DETECTING_USER,
    POSITION_READY,
    DOWN_HOLD,
    REP_SUCCESS
}

class ExerciseRepAnalyzer(
    private val challengeType: ChallengeType,
    private val onPhaseChanged: (ExercisePhase, String) -> Unit,
    private val onRepCompleted: () -> Unit
) : ImageAnalysis.Analyzer {

    private var lastRepTimestamp = 0L
    private var currentPhase = ExercisePhase.DETECTING_USER
    private var baselineCentroidY = 0f
    private var baselineVariance = 0f
    private var frameCount = 0

    // Thresholds
    private val minRepIntervalMs = 650L // Prevents double counting
    private var consecutiveDownFrames = 0
    private var consecutiveUpFrames = 0

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        try {
            val plane = imageProxy.planes.getOrNull(0) ?: run {
                imageProxy.close()
                return
            }

            val buffer = plane.buffer
            val width = imageProxy.width
            val height = imageProxy.height
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            // Sample vertical and horizontal luminance energy
            var weightedY = 0L
            var totalLuminance = 0L
            var varianceSum = 0.0

            val stepX = (width / 32).coerceAtLeast(1)
            val stepY = (height / 32).coerceAtLeast(1)

            var sampleCount = 0

            for (y in 0 until height step stepY) {
                val rowOffset = y * rowStride
                for (x in 0 until width step stepX) {
                    val index = rowOffset + x * pixelStride
                    if (index < buffer.remaining()) {
                        val lum = buffer.get(index).toInt() and 0xFF
                        weightedY += y * lum
                        totalLuminance += lum
                        sampleCount++
                    }
                }
            }

            if (totalLuminance > 0) {
                val currentCentroidY = weightedY.toFloat() / totalLuminance.toFloat()
                val avgLum = totalLuminance.toDouble() / sampleCount

                // Variance calculation for expansion (Jumping jacks)
                val currentVariance = abs(currentCentroidY - avgLum.toFloat())

                frameCount++
                if (frameCount < 10) {
                    // Baseline calibration
                    baselineCentroidY = currentCentroidY
                    baselineVariance = currentVariance
                    currentPhase = ExercisePhase.DETECTING_USER
                    onPhaseChanged(ExercisePhase.DETECTING_USER, "Calibrating position...")
                } else {
                    processMovement(currentCentroidY, currentVariance)
                }
            }
        } catch (e: Exception) {
            Log.w("ExerciseRepAnalyzer", "Analysis error: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    private fun processMovement(centroidY: Float, variance: Float) {
        val now = System.currentTimeMillis()
        val deltaY = centroidY - baselineCentroidY

        when (challengeType) {
            ChallengeType.PUSH_UPS, ChallengeType.SQUATS -> {
                // User lowers down: centroid moves downwards relative to baseline
                val isDown = deltaY > 12f
                val isUp = deltaY < 6f

                when (currentPhase) {
                    ExercisePhase.DETECTING_USER, ExercisePhase.REP_SUCCESS -> {
                        if (isUp) {
                            currentPhase = ExercisePhase.POSITION_READY
                            onPhaseChanged(ExercisePhase.POSITION_READY, "In position. Start movement!")
                        }
                    }
                    ExercisePhase.POSITION_READY -> {
                        if (isDown) {
                            consecutiveDownFrames++
                            if (consecutiveDownFrames >= 2) {
                                currentPhase = ExercisePhase.DOWN_HOLD
                                consecutiveDownFrames = 0
                                onPhaseChanged(ExercisePhase.DOWN_HOLD, "Hold down... now push up!")
                            }
                        }
                    }
                    ExercisePhase.DOWN_HOLD -> {
                        if (isUp) {
                            consecutiveUpFrames++
                            if (consecutiveUpFrames >= 2 && now - lastRepTimestamp >= minRepIntervalMs) {
                                lastRepTimestamp = now
                                currentPhase = ExercisePhase.REP_SUCCESS
                                consecutiveUpFrames = 0
                                onPhaseChanged(ExercisePhase.REP_SUCCESS, "Good rep! Counted +1")
                                onRepCompleted()
                            }
                        }
                    }
                }
            }

            ChallengeType.JUMPING_JACKS -> {
                // Arms and legs expand: variance spikes
                val isExpanded = variance > baselineVariance * 1.25f || abs(deltaY) > 10f
                val isCompact = variance <= baselineVariance * 1.10f

                when (currentPhase) {
                    ExercisePhase.DETECTING_USER, ExercisePhase.REP_SUCCESS -> {
                        if (isCompact) {
                            currentPhase = ExercisePhase.POSITION_READY
                            onPhaseChanged(ExercisePhase.POSITION_READY, "Ready for jumping jack!")
                        }
                    }
                    ExercisePhase.POSITION_READY -> {
                        if (isExpanded) {
                            currentPhase = ExercisePhase.DOWN_HOLD
                            onPhaseChanged(ExercisePhase.DOWN_HOLD, "Jump out wide!")
                        }
                    }
                    ExercisePhase.DOWN_HOLD -> {
                        if (isCompact && now - lastRepTimestamp >= minRepIntervalMs) {
                            lastRepTimestamp = now
                            currentPhase = ExercisePhase.REP_SUCCESS
                            onPhaseChanged(ExercisePhase.REP_SUCCESS, "Jack completed! +1")
                            onRepCompleted()
                        }
                    }
                }
            }

            else -> {
                // Timed challenge does not use pose analysis
            }
        }
    }
}

@Composable
fun CameraRepDetectorView(
    challengeType: ChallengeType,
    onRepDetected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var currentPhase by remember { mutableStateOf(ExercisePhase.DETECTING_USER) }
    var phaseMessage by remember { mutableStateOf("Position yourself in front of camera") }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color(0x6622C55E), Color(0x2638BDF8), Color(0x10FFFFFF))
                ),
                RoundedCornerShape(24.dp)
            )
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // 1. Native Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()
                        .also { analyzer ->
                            analyzer.setAnalyzer(
                                cameraExecutor,
                                ExerciseRepAnalyzer(
                                    challengeType = challengeType,
                                    onPhaseChanged = { phase, message ->
                                        currentPhase = phase
                                        phaseMessage = message
                                    },
                                    onRepCompleted = onRepDetected
                                )
                            )
                        }

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("CameraRepDetector", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Optical Detection Guide Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 16f), 0f)
            )
            // Center body guide box
            drawRoundRect(
                color = when (currentPhase) {
                    ExercisePhase.REP_SUCCESS -> Color(0xCC22C55E)
                    ExercisePhase.DOWN_HOLD -> Color(0xCC06B6D4)
                    ExercisePhase.POSITION_READY -> Color(0x80FFFFFF)
                    else -> Color(0x40FFFFFF)
                },
                topLeft = Offset(size.width * 0.15f, size.height * 0.12f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.70f, size.height * 0.76f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                style = stroke
            )
        }

        // 3. Status Phase Pill
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC0A0E1A))
                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when (currentPhase) {
                                ExercisePhase.REP_SUCCESS -> NeonGreenBright
                                ExercisePhase.DOWN_HOLD -> FocusCyan
                                ExercisePhase.POSITION_READY -> Color(0xFFFBBF24)
                                else -> TextSecondary
                            }
                        )
                )

                Text(
                    text = phaseMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 4. Switch Lens Flip Button
        IconButton(
            onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x80000000))
        ) {
            Icon(
                imageVector = Icons.Filled.FlipCameraAndroid,
                contentDescription = "Switch Camera",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
