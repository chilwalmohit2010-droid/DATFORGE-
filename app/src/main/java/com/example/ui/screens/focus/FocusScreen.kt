package com.example.ui.screens.focus

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.components.CommonTopBar
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.WatermarkFooter
import com.example.ui.components.tactilePress
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel
import com.example.util.PermissionManager
import java.util.Locale

@Composable
fun FocusScreen(
    viewModel: DisciplineViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentUser by viewModel.currentUser.collectAsState()
    val todayFocusMinutes by viewModel.todayFocusMinutes.collectAsState()
    val todayFocusSessions by viewModel.todayFocusSessions.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val remainingSeconds by viewModel.timerRemainingSeconds.collectAsState()
    val totalSeconds by viewModel.timerTotalSeconds.collectAsState()
    val focusSettings by viewModel.focusSettings.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()

    var showFocusSettingsSheet by remember { mutableStateOf(false) }
    var showCustomDurationDialog by remember { mutableStateOf(false) }
    var customMinutesInput by remember { mutableStateOf("25") }

    val currentDurationMins = totalSeconds / 60

    var hasUsageAccess by remember {
        mutableStateOf(PermissionManager.hasUsageAccessPermission(context))
    }

    // Lifecycle sync for accurate timestamps on resume & permission refresh
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncFocusTimerWithSystem(context)
                hasUsageAccess = PermissionManager.hasUsageAccessPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Display messages like "Related app isn't available on this device"
    LaunchedEffect(Unit) {
        viewModel.focusEventMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val linkedHabit = remember(focusSettings.linkedHabitId, allHabits) {
        focusSettings.linkedHabitId?.let { id -> allHabits.firstOrNull { it.id == id } }
    }

    val currentMode = when (currentDurationMins) {
        5 -> "Short Break"
        15 -> "Long Break"
        else -> "Focus"
    }

    LiquidGlassBackground {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("focus_screen_column"),
            contentPadding = PaddingValues(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Screen 6 Match: [←]  Focus Mode  [⚙]
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x261E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onBack?.invoke() }
                            .testTag("focus_back_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Focus Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x261E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .tactilePress(scaleDown = 0.90f) { showFocusSettingsSheet = true }
                            .testTag("focus_settings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Focus Settings",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Presets row: 15 / 25 / 45 / 60 / 90 min + Custom
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val presets = listOf(15, 25, 45, 60, 90)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0x660B1220))
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(24.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        presets.forEach { mins ->
                            val isSelected = currentDurationMins == mins
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .tactilePress(scaleDown = 0.93f) {
                                        if (!isTimerRunning) {
                                            viewModel.setFocusPreset(mins)
                                        }
                                    },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) NeonGreen else Color.Transparent
                            ) {
                                Text(
                                    text = "${mins}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Custom chip
                        val isCustom = !presets.contains(currentDurationMins)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .tactilePress(scaleDown = 0.93f) {
                                    if (!isTimerRunning) {
                                        showCustomDurationDialog = true
                                    }
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isCustom) NeonGreen else Color.Transparent
                        ) {
                            Text(
                                text = if (isCustom) "${currentDurationMins}m" else "Custom",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCustom) Color.Black else TextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Native Badges: Linked Habit & Restrictions
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Linked Habit Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x331E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showFocusSettingsSheet = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Flag,
                                    contentDescription = null,
                                    tint = if (linkedHabit != null) NeonGreen else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = linkedHabit?.title ?: "Link Habit",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (linkedHabit != null) TextPrimary else TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Active Restriction Badge
                        if (focusSettings.restrictionEnabled) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x2622C55E),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4D22C55E)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showFocusSettingsSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Shield,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${focusSettings.restrictedApps.size} Restricted",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreen,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Related App Auto-Open Badge
                        if (focusSettings.autoOpenRelatedApp && !focusSettings.relatedAppName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x266366F1),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4D6366F1)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showFocusSettingsSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Launch,
                                        contentDescription = null,
                                        tint = ElectricIndigo,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = focusSettings.relatedAppName ?: "Related App",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricIndigo,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Warning if restriction is enabled but Android usage access is missing
            if (focusSettings.restrictionEnabled && !hasUsageAccess) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x26EF4444),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4DEF4444))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = CoralRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Focus App Restriction requires Android access.",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralRed
                                )
                            }
                            Button(
                                onClick = {
                                    PermissionManager.openUsageAccessSettings(context)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CoralRed,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                            ) {
                                Text("Grant Focus Access", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Circular Timer Display (Isolated for high frame rate 120Hz performance)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IsolatedFocusTimerRing(
                        remainingSeconds = remainingSeconds,
                        totalSeconds = totalSeconds,
                        currentMode = currentMode,
                        isTimerRunning = isTimerRunning
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Main Action Start Button & Reset Button with tactile bounce
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Big solid green action button
                        Button(
                            onClick = {
                                if (isTimerRunning) viewModel.pauseFocusTimer(context)
                                else viewModel.startFocusTimer(context)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .tactilePress(scaleDown = 0.95f)
                                .testTag("focus_toggle_button"),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTimerRunning) CoralRed else NeonGreen,
                                contentColor = if (isTimerRunning) Color.White else Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = if (isTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isTimerRunning) "Pause" else "Start",
                                modifier = Modifier.size(22.dp),
                                tint = if (isTimerRunning) Color.White else Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTimerRunning) "Pause" else "Start",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isTimerRunning) Color.White else Color.Black,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Circular Glass Reset Button
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0x401E293B))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                .tactilePress(scaleDown = 0.88f) { viewModel.resetFocusTimer(context) }
                                .testTag("focus_reset_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Reset Timer",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Today's Focus Glass Card (3 Columns: Focus Time, Sessions, Goal)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .testTag("todays_focus_card"),
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0x8C141C2E),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.verticalGradient(listOf(Color(0x59FFFFFF), Color(0x1AFFFFFF)))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color(0x14FFFFFF), Color.Transparent)))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Today's Focus",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val goalMinutes = 60
                        val goalPct = if (goalMinutes > 0) ((todayFocusMinutes.toFloat() / goalMinutes) * 100).toInt().coerceAtMost(100) else 0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column 1: Focus Time
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${todayFocusMinutes}m",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Focus Time",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Divider(
                                modifier = Modifier
                                    .height(28.dp)
                                    .width(1.dp),
                                color = Color(0x26FFFFFF)
                            )

                            // Column 2: Sessions
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${todayFocusSessions.size}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Sessions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Divider(
                                modifier = Modifier
                                    .height(28.dp)
                                    .width(1.dp),
                                color = Color(0x26FFFFFF)
                            )

                            // Column 3: Goal
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$goalPct%",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (goalPct >= 100) NeonGreen else TextPrimary,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Goal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Watermark Footer
            item {
                WatermarkFooter()
            }
        }
    }

    // Custom Duration Dialog
    if (showCustomDurationDialog) {
        Dialog(onDismissRequest = { showCustomDurationDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xF20F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40FFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Set Custom Duration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter any duration in minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick duration buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(1, 5, 10, 25).forEach { quickMin ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (customMinutesInput == "$quickMin") Color(0x3322C55E) else Color(0x331E293B),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (customMinutesInput == "$quickMin") NeonGreen else Color(0x26FFFFFF)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .tactilePress(scaleDown = 0.90f) { customMinutesInput = "$quickMin" }
                            ) {
                                Text(
                                    text = "${quickMin}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (customMinutesInput == "$quickMin") NeonGreen else TextPrimary,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = customMinutesInput,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }.take(3)
                            customMinutesInput = filtered
                        },
                        label = { Text("Duration (minutes)", color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { showCustomDurationDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                val mins = customMinutesInput.toIntOrNull()?.coerceIn(1, 480) ?: 25
                                viewModel.setCustomFocusDuration(mins)
                                showCustomDurationDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Set Duration", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Step 20 Focus Settings Sheet (Duration, Linked Habit, Restrictions, Related App, Auto-open)
    if (showFocusSettingsSheet) {
        FocusSettingsSheet(
            viewModel = viewModel,
            onDismiss = { showFocusSettingsSheet = false }
        )
    }
}

/**
 * Isolated focus timer ring to prevent full-screen recompositions
 * while preserving smooth 120Hz compositor-level updates.
 */
@Composable
private fun IsolatedFocusTimerRing(
    remainingSeconds: Int,
    totalSeconds: Int,
    currentMode: String,
    isTimerRunning: Boolean
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    val progress = if (totalSeconds > 0) (remainingSeconds.toFloat() / totalSeconds.toFloat()) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 250, easing = LinearOutSlowInEasing),
        label = "focusProgress"
    )

    // Gentle breathing pulse when timer is running
    val infiniteTransition = rememberInfiniteTransition(label = "timerBreathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isTimerRunning) 1.018f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingAnim"
    )

    Box(
        modifier = Modifier
            .size(270.dp)
            .graphicsLayer {
                scaleX = breathingScale
                scaleY = breathingScale
            }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0x3322C55E),
                        Color(0x1A0F172A),
                        Color(0x00000000)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(250.dp)) {
            val strokePx = 13.dp.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // Background track
            drawArc(
                color = Color(0x1FFFFFFF),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress arc with glowing green gradient
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        NeonGreen,
                        NeonGreenBright,
                        NeonGreen
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

        // Glass Inner Disc
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x99182338), Color(0xCC0B1120))
                    )
                )
                .border(1.dp, Color(0x33FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    fontSize = 44.sp,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (currentMode == "Focus") "Focus Time" else currentMode,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}
