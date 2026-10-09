package com.example.ui.screens.water

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.models.WaterEntry
import com.example.data.models.WaterRecord
import com.example.data.models.WaterReminderSettings
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.WatermarkFooter
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt

enum class WaterTab {
    TODAY,
    HISTORY,
    REMINDERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterTrackerSheet(
    viewModel: DisciplineViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val todayRecord by viewModel.todayWaterRecord.collectAsState()
    val userWaterGoal by viewModel.userWaterGoal.collectAsState()
    val todayEntries by viewModel.todayWaterEntries.collectAsState()
    val waterHistory by viewModel.waterHistory.collectAsState()
    val reminderSettings by viewModel.waterReminderSettings.collectAsState()

    var selectedTab by remember { mutableStateOf(WaterTab.TODAY) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showCustomAmountDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<WaterEntry?>(null) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.updateWaterReminderSettings(reminderSettings.copy(isEnabled = true))
        }
    }

    val entriesSum = remember(todayEntries) { todayEntries.sumOf { it.amountMl } }
    val currentMl = maxOf(todayRecord?.currentMl ?: 0, entriesSum).coerceAtLeast(0)

    val targetMl = remember(todayRecord?.targetMl, userWaterGoal) {
        val recordGoal = todayRecord?.targetMl ?: 0
        if (recordGoal > 0) {
            recordGoal
        } else {
            val userSetting = userWaterGoal ?: 0
            if (userSetting > 0) userSetting else 3000
        }
    }
    val hasGoal = targetMl > 0

    // Single source of truth calculation:
    // percentage = (todayIntakeMl / dailyGoalMl) * 100, rounded to nearest int and capped at 100%
    val percentage: Int = if (hasGoal) {
        minOf(100, ((currentMl.toDouble() / targetMl.toDouble()) * 100.0).roundToInt().coerceAtLeast(0))
    } else {
        0
    }
    val progressRatio: Float = if (hasGoal) {
        (percentage / 100f).coerceIn(0f, 1f)
    } else {
        0f
    }
    val remainingMl: Int = if (hasGoal) (targetMl - currentMl).coerceAtLeast(0) else 0

    LiquidGlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // Top Bar (Screen 7: Back Arrow, "Water" centered, Reminder/Settings on right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x261E293B))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .testTag("water_close_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Water",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )

                IconButton(
                    onClick = { showGoalDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x261E293B))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .testTag("water_edit_goal_icon_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Tab Selector: Today / History / Reminders
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x590D121F))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WaterTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val label = when (tab) {
                        WaterTab.TODAY -> "Today"
                        WaterTab.HISTORY -> "History"
                        WaterTab.REMINDERS -> "Reminders"
                    }
                    val icon = when (tab) {
                        WaterTab.TODAY -> Icons.Filled.WaterDrop
                        WaterTab.HISTORY -> Icons.Filled.CalendarMonth
                        WaterTab.REMINDERS -> Icons.Filled.NotificationsActive
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        Brush.horizontalGradient(
                                            listOf(NeonCyanContainer, Color(0xFF0284C7))
                                        )
                                    )
                                } else Modifier.background(Color.Transparent)
                            )
                            .clickable { selectedTab = tab }
                            .padding(vertical = 10.dp)
                            .testTag("water_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Tab Content
            when (selectedTab) {
                WaterTab.TODAY -> {
                    TodayWaterView(
                        currentMl = currentMl,
                        targetMl = targetMl,
                        percentage = percentage,
                        remainingMl = remainingMl,
                        progressRatio = progressRatio,
                        entries = todayEntries,
                        onQuickAdd = { amount -> viewModel.addWater(amount) },
                        onOpenCustom = { showCustomAmountDialog = true },
                        onOpenGoalEdit = { showGoalDialog = true },
                        onEditEntry = { entry -> entryToEdit = entry },
                        onDeleteEntry = { entry -> viewModel.deleteWaterEntry(entry.id, entry.date) }
                    )
                }

                WaterTab.HISTORY -> {
                    WaterHistoryView(
                        history = waterHistory,
                        todayDate = viewModel.todayDateStr.collectAsState().value
                    )
                }

                WaterTab.REMINDERS -> {
                    WaterRemindersView(
                        settings = reminderSettings,
                        onToggleEnabled = { enabled ->
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    showPermissionRationaleDialog = true
                                } else {
                                    viewModel.updateWaterReminderSettings(reminderSettings.copy(isEnabled = true))
                                }
                            } else {
                                viewModel.updateWaterReminderSettings(reminderSettings.copy(isEnabled = false))
                            }
                        },
                        onIntervalChange = { interval ->
                            viewModel.updateWaterReminderSettings(reminderSettings.copy(intervalMinutes = interval))
                        },
                        onTimesChange = { start, end ->
                            viewModel.updateWaterReminderSettings(
                                reminderSettings.copy(startTime = start, endTime = end)
                            )
                        },
                        onSendTestNotification = {
                            viewModel.sendTestWaterNotification()
                        }
                    )
                }
            }

            WatermarkFooter(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        }
    }

    // Dialog: Edit Personal Daily Water Goal
    if (showGoalDialog) {
        EditWaterGoalDialog(
            currentGoalMl = targetMl,
            onDismiss = { showGoalDialog = false },
            onSave = { newGoal ->
                viewModel.setWaterGoal(newGoal)
                showGoalDialog = false
            }
        )
    }

    // Dialog: Add Custom Water Amount
    if (showCustomAmountDialog) {
        CustomWaterAmountDialog(
            onDismiss = { showCustomAmountDialog = false },
            onAdd = { amount ->
                viewModel.addWater(amount)
                showCustomAmountDialog = false
            }
        )
    }

    // Dialog: Edit Existing Water Entry
    entryToEdit?.let { entry ->
        EditWaterEntryDialog(
            entry = entry,
            onDismiss = { entryToEdit = null },
            onSave = { updatedAmount ->
                viewModel.editWaterEntry(entry.id, entry.date, updatedAmount)
                entryToEdit = null
            }
        )
    }

    // Dialog: Notification Permission Rationale
    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            containerColor = GlassDarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hydration Alerts",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = "Discipline needs notification permissions to remind you to hydrate at your selected intervals during your active hours. This ensures you maintain peak energy, focus, and physical discipline.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationaleDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.updateWaterReminderSettings(reminderSettings.copy(isEnabled = true))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationaleDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

// -------------------------------------------------------------------------
// Sub-view: TODAY
// -------------------------------------------------------------------------
@Composable
fun GlassWaterBottleVisual(
    fillRatio: Float,
    modifier: Modifier = Modifier
) {
    val animatedRatio by animateFloatAsState(
        targetValue = fillRatio.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "waterBottleFill"
    )

    Box(
        modifier = modifier
            .width(130.dp)
            .height(210.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val neckWidth = w * 0.38f
            val neckLeft = (w - neckWidth) / 2f
            val neckHeight = h * 0.12f

            val bodyWidth = w * 0.78f
            val bodyLeft = (w - bodyWidth) / 2f
            val bodyTop = h * 0.22f
            val bodyBottom = h * 0.94f
            val cornerR = 24.dp.toPx()

            // 1. Soft cyan ambient glow behind bottle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3300F0FF), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.6f),
                    radius = w * 0.75f
                )
            )

            // 2. Liquid inside bottle
            val liquidMaxHeight = bodyBottom - bodyTop
            val liquidHeight = liquidMaxHeight * animatedRatio
            val liquidTop = bodyBottom - liquidHeight

            if (animatedRatio > 0.02f) {
                val liquidPath = Path().apply {
                    moveTo(bodyLeft + cornerR, bodyBottom)
                    lineTo(bodyLeft + bodyWidth - cornerR, bodyBottom)
                    quadraticBezierTo(bodyLeft + bodyWidth, bodyBottom, bodyLeft + bodyWidth, bodyBottom - cornerR)
                    lineTo(bodyLeft + bodyWidth, liquidTop)
                    lineTo(bodyLeft, liquidTop)
                    lineTo(bodyLeft, bodyBottom - cornerR)
                    quadraticBezierTo(bodyLeft, bodyBottom, bodyLeft + cornerR, bodyBottom)
                    close()
                }

                drawPath(
                    path = liquidPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC38BDF8),
                            Color(0xEE0284C7),
                            Color(0xFF0369A1)
                        ),
                        startY = liquidTop,
                        endY = bodyBottom
                    )
                )

                // Liquid surface meniscus line
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0x66FFFFFF), Color(0xFF00F0FF), Color(0x66FFFFFF))
                    ),
                    start = Offset(bodyLeft, liquidTop),
                    end = Offset(bodyLeft + bodyWidth, liquidTop),
                    strokeWidth = 3.dp.toPx()
                )
            }

            // 3. Glass Bottle Outer Outline & Shading
            val bottlePath = Path().apply {
                moveTo(neckLeft, 0f)
                lineTo(neckLeft + neckWidth, 0f)
                lineTo(neckLeft + neckWidth, neckHeight)
                quadraticBezierTo(neckLeft + neckWidth + (w * 0.15f), bodyTop * 0.7f, bodyLeft + bodyWidth, bodyTop)
                lineTo(bodyLeft + bodyWidth, bodyBottom - cornerR)
                quadraticBezierTo(bodyLeft + bodyWidth, bodyBottom, bodyLeft + bodyWidth - cornerR, bodyBottom)
                lineTo(bodyLeft + cornerR, bodyBottom)
                quadraticBezierTo(bodyLeft, bodyBottom, bodyLeft, bodyBottom - cornerR)
                lineTo(bodyLeft, bodyTop)
                quadraticBezierTo(neckLeft - (w * 0.15f), bodyTop * 0.7f, neckLeft, neckHeight)
                lineTo(neckLeft, 0f)
                close()
            }

            // Glass translucent fill
            drawPath(
                path = bottlePath,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x2EFFFFFF),
                        Color(0x1406B6D4),
                        Color(0x1F000000)
                    )
                )
            )

            // Glass border with luminous cyan/white highlights
            drawPath(
                path = bottlePath,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x99FFFFFF),
                        Color(0x6600F0FF),
                        Color(0x3338BDF8),
                        Color(0x80FFFFFF)
                    )
                ),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Left specular reflection streak
            drawLine(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0x80FFFFFF), Color.Transparent)
                ),
                start = Offset(bodyLeft + 8.dp.toPx(), bodyTop + 14.dp.toPx()),
                end = Offset(bodyLeft + 8.dp.toPx(), bodyBottom - 24.dp.toPx()),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun TodayWaterView(
    currentMl: Int,
    targetMl: Int,
    percentage: Int,
    remainingMl: Int,
    progressRatio: Float,
    entries: List<WaterEntry>,
    onQuickAdd: (Int) -> Unit,
    onOpenCustom: () -> Unit,
    onOpenGoalEdit: () -> Unit,
    onEditEntry: (WaterEntry) -> Unit,
    onDeleteEntry: (WaterEntry) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Screen 7: Glass Bottle & Controls Hero Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glass Hydro Bottle
                GlassWaterBottleVisual(fillRatio = progressRatio)

                Spacer(modifier = Modifier.height(18.dp))

                // Minus / Readout / Plus Row (Screen 7)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Circular Glass Minus Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x261E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onQuickAdd(-100) }
                            .testTag("water_minus_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("−", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.width(28.dp))

                    // Center Amount & Percentage (e.g. 2.2 / 3.0 L  73%)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val currentL = currentMl / 1000f
                        val targetL = if (targetMl > 0) targetMl / 1000f else 3.0f
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f / %.1f L", currentL, targetL),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 22.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00F0FF),
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(28.dp))

                    // Circular Glass Plus Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x261E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onQuickAdd(100) }
                            .testTag("water_plus_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Quick Log Buttons (+100ml, +250ml, +500ml)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(100, 250, 500).forEach { amount ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(21.dp))
                                .background(Color(0x331E293B))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(21.dp))
                                .clickable { onQuickAdd(amount) }
                                .testTag("water_quick_pill_$amount"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${amount}ml",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
        // Main Hero Liquid Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("water_today_hero_card"),
                backgroundColor = Color(0x99101B2E),
                borderColor = GlassGlowCyan
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x3306B6D4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WaterDrop,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TODAY'S INTAKE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonCyan,
                                letterSpacing = 0.8.sp
                            )
                        }

                        // Editable Goal Chip / Set Goal Chip
                        if (targetMl > 0) {
                            val targetLStr = if (targetMl % 1000 == 0) "${targetMl / 1000}.0" else String.format(java.util.Locale.US, "%.1f", targetMl / 1000f)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x3338BDF8))
                                    .border(1.dp, Color(0x4D38BDF8), RoundedCornerShape(10.dp))
                                    .clickable { onOpenGoalEdit() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("water_edit_goal_chip"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Goal: $targetLStr L",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x3306B6D4))
                                    .border(1.dp, Color(0x6606B6D4), RoundedCornerShape(10.dp))
                                    .clickable { onOpenGoalEdit() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("water_set_goal_header_chip"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Set Daily Goal",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Large Hydration Amount
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format("%,d", currentMl),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ml",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Percentage and Remaining
                    if (targetMl > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$percentage% completed",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (percentage >= 100) NeonGreen else NeonCyan
                            )
                            Text(
                                text = "•",
                                color = TextMuted
                            )
                            Text(
                                text = if (remainingMl > 0) "${String.format(java.util.Locale.US, "%,d", remainingMl)} ml left" else "Goal Achieved! 🎉",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenGoalEdit() }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "No daily goal set yet • Tap to set target",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = NeonCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Glowing Linear Progress Bar
                    val animatedBarProgress by animateFloatAsState(
                        targetValue = progressRatio,
                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                        label = "waterLinearProgress"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33FFFFFF))
                    ) {
                        if (targetMl > 0 && animatedBarProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedBarProgress.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(NeonCyan, Color(0xFF38BDF8), NeonGreenBright)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Prompt Banner when no daily goal is set
        if (targetMl <= 0) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("water_set_goal_banner"),
                    backgroundColor = Color(0x660F172A),
                    borderColor = NeonCyan
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Set Daily Water Goal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Customize your target in ml or liters (e.g. 1500 ml, 2.5 L).",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = onOpenGoalEdit,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("water_set_goal_banner_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set Goal", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick Add Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick Log Water",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // +250 ml Glass
                    QuickAddCard(
                        amountMl = 250,
                        iconText = "🥛",
                        label = "Glass",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("water_quick_add_250"),
                        onClick = { onQuickAdd(250) }
                    )

                    // +500 ml Bottle
                    QuickAddCard(
                        amountMl = 500,
                        iconText = "🍶",
                        label = "Bottle",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("water_quick_add_500"),
                        onClick = { onQuickAdd(500) }
                    )

                    // +750 ml Flask
                    QuickAddCard(
                        amountMl = 750,
                        iconText = "🧊",
                        label = "Flask",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("water_quick_add_750"),
                        onClick = { onQuickAdd(750) }
                    )

                    // + Custom ml
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x6618233A))
                            .border(1.dp, GlassBorderStroke, RoundedCornerShape(16.dp))
                            .clickable { onOpenCustom() }
                            .padding(vertical = 12.dp)
                            .testTag("water_quick_add_custom"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "➕", fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Custom",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Today's Logs Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Logs (${entries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (entries.isNotEmpty()) {
                    Text(
                        text = "Tap to edit or remove",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        // Entries List
        if (entries.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0x40131C2E)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WaterDrop,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No water logged yet today",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap +250 ml or +500 ml above to record your hydration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(entries, key = { it.id }) { entry ->
                WaterEntryCard(
                    entry = entry,
                    onEdit = { onEditEntry(entry) },
                    onDelete = { onDeleteEntry(entry) }
                )
            }
        }
    }
}

@Composable
private fun QuickAddCard(
    amountMl: Int,
    iconText: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x6618233A))
            .border(1.dp, GlassBorderStroke, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = iconText, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "+$amountMl ml",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun WaterEntryCard(
    entry: WaterEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(entry.timestamp) { timeFormatter.format(Date(entry.timestamp)) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0x66131C2E),
        cornerRadius = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x2E06B6D4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.WaterDrop,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "+${entry.amountMl} ml",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            // Edit & Delete actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit entry",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete entry",
                        tint = CoralRed.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Sub-view: HISTORY
// -------------------------------------------------------------------------
@Composable
private fun WaterHistoryView(
    history: List<WaterRecord>,
    todayDate: String
) {
    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Hydration History Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your daily intake logs will automatically be recorded here over time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(history, key = { it.date }) { record ->
                val isToday = record.date == todayDate
                val hasGoal = record.targetMl > 0
                val percent = if (hasGoal) {
                    minOf(100, ((record.currentMl.toDouble() / record.targetMl.toDouble()) * 100.0).roundToInt().coerceAtLeast(0))
                } else {
                    0
                }
                val ratio = if (hasGoal) (percent / 100f).coerceIn(0f, 1f) else 0f
                val isMet = hasGoal && record.currentMl >= record.targetMl

                val displayDate = remember(record.date) {
                    try {
                        val parsed = LocalDate.parse(record.date, DateTimeFormatter.ISO_LOCAL_DATE)
                        val today = LocalDate.now()
                        when {
                            parsed == today -> "Today"
                            parsed == today.minusDays(1) -> "Yesterday"
                            else -> parsed.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
                        }
                    } catch (e: Exception) {
                        record.date
                    }
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = if (isToday) Color(0x9914223A) else Color(0x66131C2E),
                    borderColor = if (isMet) Color(0x6622C55E) else GlassBorderSubtle
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = displayDate,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isToday) NeonCyan else TextPrimary
                                )
                                Text(
                                    text = if (hasGoal) "${record.currentMl} / ${record.targetMl} ml" else "${record.currentMl} ml logged",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            // Goal Status Badge
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isMet) Color(0x3322C55E) else if (hasGoal) Color(0x2638BDF8) else Color(0x26FFFFFF))
                                    .border(
                                        1.dp,
                                        if (isMet) Color(0x6622C55E) else if (hasGoal) Color(0x4038BDF8) else Color(0x33FFFFFF),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isMet) "Goal Met 🎯" else if (hasGoal) "$percent%" else "Logged",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMet) NeonGreen else if (hasGoal) NeonCyan else TextSecondary
                                )
                            }
                        }

                        if (hasGoal) {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x26FFFFFF))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isMet) NeonGreen else NeonCyan)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Sub-view: REMINDERS
// -------------------------------------------------------------------------
@Composable
private fun WaterRemindersView(
    settings: WaterReminderSettings,
    onToggleEnabled: (Boolean) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onTimesChange: (String, String) -> Unit,
    onSendTestNotification: () -> Unit
) {
    var showTimeEditDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Toggle Master Switch Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("water_reminders_master_card"),
                backgroundColor = Color(0x8C142038),
                borderColor = if (settings.isEnabled) GlassGlowCyan else GlassBorderSubtle
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (settings.isEnabled) Color(0x3306B6D4) else Color(0x1AFFFFFF)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = if (settings.isEnabled) NeonCyan else TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Hydration Reminders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (settings.isEnabled) "Reminding every ${settings.intervalMinutes}m" else "Reminders are paused",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (settings.isEnabled) NeonCyan else TextMuted
                            )
                        }
                    }

                    Switch(
                        checked = settings.isEnabled,
                        onCheckedChange = { onToggleEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.testTag("water_reminders_toggle_switch")
                    )
                }
            }
        }

        // Interval Selection
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Reminder Interval",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose how frequently you'd like a hydration prompt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val intervals = listOf(30, 60, 90, 120)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        intervals.forEach { interval ->
                            val isSelected = settings.intervalMinutes == interval
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) NeonCyanContainer else Color(0x40131C2E)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonCyan else GlassBorderSubtle,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onIntervalChange(interval) }
                                    .padding(vertical = 12.dp)
                                    .testTag("water_interval_$interval"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$interval min",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Hours Schedule
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Hours Window",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Reminders are only sent during these hours.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        IconButton(onClick = { showTimeEditDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit times",
                                tint = NeonCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start Time
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x40131C2E))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "START TIME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = settings.startTime,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        // End Time
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x40131C2E))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "END TIME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = settings.endTime,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Test Notification Action
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Verify Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Trigger a test notification immediately to confirm alerts appear properly on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onSendTestNotification,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("water_send_test_notification_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x3338BDF8)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6638BDF8))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Test Reminder Notification",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showTimeEditDialog) {
        EditTimesDialog(
            currentStart = settings.startTime,
            currentEnd = settings.endTime,
            onDismiss = { showTimeEditDialog = false },
            onSave = { s, e ->
                onTimesChange(s, e)
                showTimeEditDialog = false
            }
        )
    }
}

// -------------------------------------------------------------------------
// Dialogs: Goal, Custom Amount, Entry Edit, Active Times
// -------------------------------------------------------------------------

@Composable
private fun EditWaterGoalDialog(
    currentGoalMl: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    val initialText = if (currentGoalMl > 0) {
        if (currentGoalMl % 1000 == 0) {
            "${currentGoalMl / 1000} L"
        } else if (currentGoalMl % 100 == 0) {
            "${String.format(java.util.Locale.US, "%.1f", currentGoalMl / 1000f)} L"
        } else {
            "$currentGoalMl ml"
        }
    } else {
        ""
    }

    var goalText by remember { mutableStateOf(initialText) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun parseWaterGoal(input: String): Int? {
        val trimmed = input.trim().lowercase(java.util.Locale.ROOT)
        if (trimmed.isBlank()) return null

        // Check ml suffix
        if (trimmed.endsWith("ml")) {
            val numPart = trimmed.removeSuffix("ml").trim().replace(',', '.')
            val ml = numPart.toDoubleOrNull() ?: return null
            return ml.toInt().takeIf { it in 100..15000 }
        }

        // Check liters suffix
        val suffixes = listOf("liters", "liter", "litres", "litre", "l")
        for (s in suffixes) {
            if (trimmed.endsWith(s)) {
                val numPart = trimmed.removeSuffix(s).trim().replace(',', '.')
                val liters = numPart.toDoubleOrNull() ?: return null
                val ml = (liters * 1000).toInt()
                return ml.takeIf { it in 100..15000 }
            }
        }

        // Plain number without suffix
        val clean = trimmed.replace(',', '.')
        val num = clean.toDoubleOrNull() ?: return null
        return if (num <= 15.0) {
            // Numbers <= 15 are assumed to be in liters (e.g. 2.5 -> 2500 ml, 3 -> 3000 ml)
            (num * 1000).toInt().takeIf { it in 100..15000 }
        } else {
            // Numbers > 15 are assumed to be in ml (e.g. 1500 -> 1500 ml)
            num.toInt().takeIf { it in 100..15000 }
        }
    }

    val parsedGoal = remember(goalText) { parseWaterGoal(goalText) }
    val presets = listOf("1500 ml", "2.0 L", "2.5 L", "3.0 L", "3.5 L", "4.0 L")

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = GlassDarkSurface,
            borderColor = GlassGlowCyan
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x3306B6D4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WaterDrop,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = if (currentGoalMl > 0) "Edit Daily Water Goal" else "Set Daily Water Goal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Customize your daily hydration target in ml or liters (e.g. 1500 ml, 2000 ml, 2.5 L, 3 L):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Presets Title & Row
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = goalText.trim().equals(preset, ignoreCase = true) ||
                                (parsedGoal != null && parseWaterGoal(preset) == parsedGoal)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) NeonCyanContainer else Color(0x3318233A)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCyan else GlassBorderSubtle,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    goalText = preset
                                    errorMessage = null
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Unit Quick Appenders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Append Unit:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    listOf("ml", "L").forEach { unit ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x2638BDF8))
                                .border(1.dp, Color(0x4D38BDF8), RoundedCornerShape(6.dp))
                                .clickable {
                                    val cleaned = goalText.trim()
                                    if (cleaned.isNotBlank() && !cleaned.endsWith(unit, ignoreCase = true)) {
                                        goalText = "$cleaned $unit"
                                        errorMessage = null
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "+ $unit",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = goalText,
                    onValueChange = {
                        goalText = it
                        errorMessage = null
                    },
                    label = { Text("Desired Daily Goal") },
                    placeholder = { Text("e.g. 2.5 L or 2000 ml", color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderStroke
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("water_goal_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Live Feedback Badge
                if (parsedGoal != null) {
                    val targetLDisplay = if (parsedGoal % 1000 == 0) {
                        "${parsedGoal / 1000}.0"
                    } else {
                        String.format(java.util.Locale.US, "%.1f", parsedGoal / 1000f)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x2606B6D4))
                            .border(1.dp, Color(0x6606B6D4), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "✓ Target: ${String.format(java.util.Locale.US, "%,d", parsedGoal)} ml ($targetLDisplay L / day)",
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = CoralRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (goalText.isNotBlank()) {
                    Text(
                        text = "Enter a valid amount (e.g. 1500 ml, 2000 ml, 2.5 L, 3 L)",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsed = parseWaterGoal(goalText)
                            if (parsed == null) {
                                errorMessage = "Goal must be between 100 ml and 15,000 ml (e.g. 2000 ml, 2.5 L, 3 L)"
                            } else {
                                onSave(parsed)
                            }
                        },
                        enabled = parsedGoal != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            disabledContainerColor = Color(0x331E293B)
                        ),
                        modifier = Modifier.testTag("water_goal_save_button")
                    ) {
                        Text(
                            text = if (currentGoalMl > 0) "Save Goal" else "Set Goal",
                            color = if (parsedGoal != null) Color(0xFF0F172A) else TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomWaterAmountDialog(
    onDismiss: () -> Unit,
    onAdd: (Int) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = GlassDarkSurface,
            borderColor = GlassGlowCyan
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Add Custom Water",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter amount of water consumed (1 ml to 5,000 ml):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("Amount in ml") },
                    placeholder = { Text("e.g. 350") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderStroke
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("water_custom_amount_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = CoralRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsed = amountText.toIntOrNull()
                            if (parsed == null || parsed <= 0 || parsed > 5000) {
                                errorMessage = "Please enter an amount between 1 ml and 5,000 ml"
                            } else {
                                onAdd(parsed)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.testTag("water_custom_add_button")
                    ) {
                        Text("Add Water", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditWaterEntryDialog(
    entry: WaterEntry,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var amountText by remember { mutableStateOf(entry.amountMl.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = GlassDarkSurface,
            borderColor = GlassGlowCyan
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Edit Water Entry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Adjust the logged amount for this entry:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("Amount in ml") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderStroke
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = CoralRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsed = amountText.toIntOrNull()
                            if (parsed == null || parsed <= 0 || parsed > 5000) {
                                errorMessage = "Please enter an amount between 1 and 5,000 ml"
                            } else {
                                onSave(parsed)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Update", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditTimesDialog(
    currentStart: String,
    currentEnd: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var start by remember { mutableStateOf(currentStart) }
    var end by remember { mutableStateOf(currentEnd) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = GlassDarkSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Active Hours",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Format: HH:mm (24-hour time, e.g. 08:00 and 21:00)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it.take(5) },
                    label = { Text("Start Time") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderStroke
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it.take(5) },
                    label = { Text("End Time") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderStroke
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(start.trim(), end.trim()) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
