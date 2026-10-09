package com.example.ui.screens.alarm

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.alarm.ChallengeAlarmManager
import com.example.alarm.ChallengeAlarmService
import com.example.data.models.ChallengeHistoryEntry
import com.example.data.models.ChallengeType
import com.example.ui.components.CategoryIcon
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.WatermarkFooter
import com.example.ui.components.tactilePress
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun ChallengeAlarmScreen(
    alarmId: String,
    alarmName: String,
    challengeType: ChallengeType,
    target: Int,
    linkedHabitId: String? = null,
    videoUriStr: String? = null,
    snoozeMinutes: Int = 5,
    onChallengeFinished: (ChallengeHistoryEntry) -> Unit,
    onStopSafetyClicked: (ChallengeHistoryEntry) -> Unit,
    onSnoozeClicked: () -> Unit
) {
    val context = LocalContext.current

    // Live clock
    var currentTimeStr by remember {
        mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a")))
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a"))
        }
    }

    // Challenge Progress
    var currentProgress by remember { mutableStateOf(0) }
    var isChallengeComplete by remember { mutableStateOf(false) }
    val startTime = remember { System.currentTimeMillis() }

    // Timed Challenge Countdown State
    var isTimerRunning by remember { mutableStateOf(challengeType == ChallengeType.TIMED_CHALLENGE) }
    var timerSecondsRemaining by remember { mutableStateOf(if (challengeType == ChallengeType.TIMED_CHALLENGE) target else 0) }

    // Camera Permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Vibrator for rep feedback
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        } catch (e: Exception) {}
    }

    // Repetition completion handler
    fun onRepCounted() {
        if (isChallengeComplete) return
        triggerHapticFeedback()
        val next = currentProgress + 1
        currentProgress = next
        if (next >= target) {
            isChallengeComplete = true
            // Stop alarm ringing immediately
            val stopIntent = Intent(context, ChallengeAlarmService::class.java).apply {
                action = ChallengeAlarmService.ACTION_STOP_ALARM
            }
            context.startService(stopIntent)

            val entry = ChallengeHistoryEntry(
                id = UUID.randomUUID().toString(),
                userId = "",
                alarmId = alarmId,
                alarmName = alarmName,
                challengeType = challengeType,
                target = target,
                completedAmount = target,
                status = "COMPLETED",
                linkedHabitId = linkedHabitId,
                startedAt = startTime,
                completedAt = System.currentTimeMillis(),
                durationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            )
            onChallengeFinished(entry)
        }
    }

    // Timed challenge countdown loop
    LaunchedEffect(isTimerRunning, challengeType) {
        if (challengeType == ChallengeType.TIMED_CHALLENGE && isTimerRunning) {
            val endTimestamp = System.currentTimeMillis() + (timerSecondsRemaining * 1000L)
            while (isTimerRunning && timerSecondsRemaining > 0) {
                delay(200L)
                val remaining = ((endTimestamp - System.currentTimeMillis() + 999L) / 1000L).toInt()
                timerSecondsRemaining = remaining.coerceAtLeast(0)
                currentProgress = (target - timerSecondsRemaining).coerceAtLeast(0)
                if (timerSecondsRemaining <= 0) {
                    isChallengeComplete = true
                    val stopIntent = Intent(context, ChallengeAlarmService::class.java).apply {
                        action = ChallengeAlarmService.ACTION_STOP_ALARM
                    }
                    context.startService(stopIntent)

                    val entry = ChallengeHistoryEntry(
                        id = UUID.randomUUID().toString(),
                        userId = "",
                        alarmId = alarmId,
                        alarmName = alarmName,
                        challengeType = challengeType,
                        target = target,
                        completedAmount = target,
                        status = "COMPLETED",
                        linkedHabitId = linkedHabitId,
                        startedAt = startTime,
                        completedAt = System.currentTimeMillis(),
                        durationSeconds = target
                    )
                    onChallengeFinished(entry)
                    break
                }
            }
        }
    }

    val progressPercent = if (target > 0) (currentProgress.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "challengeProgress"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Optional Background Custom Video Playback
        if (!videoUriStr.isNullOrBlank()) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        try {
                            setVideoURI(Uri.parse(videoUriStr))
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                mp.setVolume(0f, 0f) // Muted background video
                                start()
                            }
                        } catch (e: Exception) {}
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // Translucent glass overlay over video
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC030712))
            )
        } else {
            LiquidGlassBackground { Box(modifier = Modifier.fillMaxSize()) }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Live Clock & Alarm Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = currentTimeStr,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = NeonGreenBright,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = alarmName.ifBlank { "Morning Warrior Alarm" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Challenge Type Badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x3322C55E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6622C55E))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryIcon(iconName = challengeType.iconName, tint = NeonGreenBright, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${challengeType.displayName} ($target ${challengeType.unit})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreenBright
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Interactive Section: Progress Ring / Camera
            if (isChallengeComplete) {
                // Victory Screen
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0x6622C55E), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0x8C141C2E)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0x3322C55E))
                                .border(2.dp, NeonGreenBright, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Completed",
                                tint = NeonGreenBright,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Text(
                            text = "CHALLENGE CONQUERED! ⚔️",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = NeonGreenBright,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "You successfully completed $target ${challengeType.unit} of ${challengeType.displayName}!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        if (!linkedHabitId.isNullOrBlank()) {
                            Text(
                                text = "✅ Linked habit has been marked completed for today!",
                                style = MaterialTheme.typography.bodySmall,
                                color = FocusCyan,
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            text = "+10 Discipline Points Earned",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                    }
                }
            } else {
                // Active Challenge Flow
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Large Counter Progress Ring
                    Box(
                        modifier = Modifier.size(170.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = Color(0x26FFFFFF),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(NeonGreenBright, FocusCyan, NeonGreenBright)
                                ),
                                startAngle = -90f,
                                sweepAngle = animatedProgress * 360f,
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (challengeType == ChallengeType.TIMED_CHALLENGE) {
                                val mins = timerSecondsRemaining / 60
                                val secs = timerSecondsRemaining % 60
                                Text(
                                    text = String.format("%02d:%02d", mins, secs),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "remaining",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            } else {
                                Text(
                                    text = "$currentProgress",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "/ $target ${challengeType.unit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Exercise Camera Detection or Fallback
                    if (challengeType != ChallengeType.TIMED_CHALLENGE) {
                        if (hasCameraPermission) {
                            CameraRepDetectorView(
                                challengeType = challengeType,
                                onRepDetected = { onRepCounted() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                            )
                        } else {
                            // Camera Request & Explanation Banner
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp)),
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0x66141C2E)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Videocam, null, tint = FocusCyan, modifier = Modifier.size(28.dp))
                                    Text(
                                        text = "Camera Rep Detection",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Enable camera to automatically count your ${challengeType.displayName.lowercase()} reps in real time. Footage is never saved or uploaded.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        fontSize = 12.sp
                                    )
                                    Button(
                                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = FocusCyan,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Enable Camera Detection", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Manual Tap Rep Fallback Button (Always available for safety)
                        Button(
                            onClick = { onRepCounted() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreenBright,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("challenge_manual_rep_button")
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Count Rep (+1 ${challengeType.displayName})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Timed Challenge Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { isTimerRunning = !isTimerRunning },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTimerRunning) Color(0xFFFBBF24) else NeonGreenBright,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(if (isTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow, null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isTimerRunning) "Pause" else "Resume", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Actions: Safety Exit "Stop Challenge" and Snooze
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isChallengeComplete) {
                    if (snoozeMinutes > 0) {
                        OutlinedButton(
                            onClick = onSnoozeClicked,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Filled.Snooze, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Snooze ($snoozeMinutes min)", color = TextPrimary)
                        }
                    }

                    // Clearly Accessible Safety Exit
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0x33EF4444), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x22EF4444)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⚠️ Safety First: Stop immediately if you feel dizzy, ill, or in pain.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFCA5A5),
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = {
                                    // Stop service and report cancelled/stopped
                                    val stopIntent = Intent(context, ChallengeAlarmService::class.java).apply {
                                        action = ChallengeAlarmService.ACTION_STOP_ALARM
                                    }
                                    context.startService(stopIntent)

                                    val entry = ChallengeHistoryEntry(
                                        id = UUID.randomUUID().toString(),
                                        userId = "",
                                        alarmId = alarmId,
                                        alarmName = alarmName,
                                        challengeType = challengeType,
                                        target = target,
                                        completedAmount = currentProgress,
                                        status = "STOPPED",
                                        linkedHabitId = linkedHabitId,
                                        startedAt = startTime,
                                        completedAt = System.currentTimeMillis(),
                                        durationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                                    )
                                    onStopSafetyClicked(entry)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x66EF4444),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("challenge_stop_safety_button")
                            ) {
                                Text("Stop Challenge", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                WatermarkFooter()
            }
        }
    }
}
