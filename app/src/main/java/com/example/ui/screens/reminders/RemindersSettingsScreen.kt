package com.example.ui.screens.reminders

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.models.*
import com.example.ui.components.CategoryIcon
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.WatermarkFooter
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersSettingsScreen(
    viewModel: DisciplineViewModel,
    onNavigateBack: () -> Unit,
    onOpenNightReview: () -> Unit,
    onOpenAlarms: () -> Unit = {}
) {
    val context = LocalContext.current
    val habits by viewModel.allHabits.collectAsState()
    val reminderSettings by viewModel.reminderSettings.collectAsState()
    val todayWater by viewModel.todayWaterRecord.collectAsState()
    val alarms by viewModel.allAlarms.collectAsState()

    // Permission state
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var pendingActionOnPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPermissionExplanationDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            pendingActionOnPermission?.invoke()
            Toast.makeText(context, "Notifications enabled! 🔔", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                context,
                "Notification permission denied. You can enable it in Device Settings.",
                Toast.LENGTH_LONG
            ).show()
        }
        pendingActionOnPermission = null
    }

    fun executeWithPermissionCheck(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            pendingActionOnPermission = action
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    // Dialog state for editing habit reminder time
    var editingHabitTimeFor by remember { mutableStateOf<Habit?>(null) }
    var editingNightReviewTime by remember { mutableStateOf(false) }
    var editingWaterWindow by remember { mutableStateOf(false) }

    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x330A0F1D))
                        .padding(top = 8.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x1AFFFFFF))
                                .testTag("reminders_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Reminders & Alerts",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Discipline schedules, hydration & reflection",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                // 1. Permission Status Card
                item {
                    PermissionStatusCard(
                        hasPermission = hasNotificationPermission,
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                Toast.makeText(context, "System notifications are supported!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenSettings = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Unable to open settings directly.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // 2. Web Preview & Environment Notice
                item {
                    WebPreviewNoticeCard()
                }

                // 2b. Wake-Up Challenge Alarms Section
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color(0x6622C55E), Color(0x2638BDF8), Color(0x10FFFFFF))
                                ),
                                RoundedCornerShape(20.dp)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x80131C2E)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x3322C55E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Alarm,
                                            contentDescription = null,
                                            tint = NeonGreenBright,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Wake-Up Challenges ⚔️",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${alarms.count { it.isEnabled }} active • ${alarms.size} total alarms",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Button(
                                    onClick = onOpenAlarms,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonGreenBright,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Manage", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            Text(
                                text = "Require squats, push-ups, jumping jacks, or timed challenges before the alarm can be silenced. Full camera rep detection.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // 3. Night Review Section
                item {
                    NightReviewSettingsCard(
                        nightSettings = reminderSettings.nightReview,
                        onToggle = { isEnabled ->
                            if (isEnabled) {
                                executeWithPermissionCheck {
                                    viewModel.updateNightReviewSettings(true, reminderSettings.nightReview.time)
                                }
                            } else {
                                viewModel.updateNightReviewSettings(false, reminderSettings.nightReview.time)
                            }
                        },
                        onEditTime = { editingNightReviewTime = true },
                        onOpenNightReview = onOpenNightReview,
                        onSendTestAlert = {
                            viewModel.sendTestNotification(ReminderType.NIGHT_REVIEW)
                            Toast.makeText(context, "Sent test Night Review alert! 🌙", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // 4. Water Reminders Section
                item {
                    val waterSettings = reminderSettings.waterReminders
                    WaterReminderSettingsCard(
                        settings = waterSettings,
                        targetMl = todayWater?.targetMl ?: 2500,
                        onToggle = { isEnabled ->
                            if (isEnabled) {
                                executeWithPermissionCheck {
                                    val updated = waterSettings.copy(isEnabled = true)
                                    viewModel.updateWaterReminderSettings(updated)
                                }
                            } else {
                                val updated = waterSettings.copy(isEnabled = false)
                                viewModel.updateWaterReminderSettings(updated)
                            }
                        },
                        onSelectInterval = { minutes ->
                            val updated = waterSettings.copy(intervalMinutes = minutes)
                            viewModel.updateWaterReminderSettings(updated)
                        },
                        onEditWindow = { editingWaterWindow = true },
                        onSendTestAlert = {
                            viewModel.sendTestNotification(ReminderType.WATER)
                            Toast.makeText(context, "Sent test Hydration alert! 💧", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // 5. Habits Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Habit Reminders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "${habits.count { !it.reminderTime.isNullOrBlank() && !it.isPaused }} Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent
                        )
                    }
                }

                // Habit items list
                if (habits.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp)),
                            color = Color(0x1A1E293B)
                        ) {
                            Text(
                                text = "No habits created yet. Add a habit on the Habits screen to schedule discipline reminders.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(habits, key = { it.id }) { habit ->
                        val isEnabled = !habit.reminderTime.isNullOrBlank() && !habit.isPaused
                        val reminderTime = habit.reminderTime ?: reminderSettings.habitReminderTimes[habit.id] ?: "07:30"

                        HabitReminderItemCard(
                            habit = habit,
                            isEnabled = isEnabled,
                            reminderTime = reminderTime,
                            onToggle = { enable ->
                                if (enable) {
                                    executeWithPermissionCheck {
                                        viewModel.toggleHabitReminder(habit.id, true, reminderTime)
                                    }
                                } else {
                                    viewModel.toggleHabitReminder(habit.id, false, null)
                                }
                            },
                            onEditTime = { editingHabitTimeFor = habit },
                            onTestAlert = {
                                viewModel.sendTestNotification(ReminderType.HABIT, habit.title)
                                Toast.makeText(context, "Sent test alert for '${habit.title}'! ⚡", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                // Footer branding
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    WatermarkFooter()
                }
            }
        }
    }

    // Habit Time Edit Dialog
    editingHabitTimeFor?.let { targetHabit ->
        val currentTime = targetHabit.reminderTime ?: "07:30"
        TimePickerDialog(
            title = "Reminder Time for '${targetHabit.title}'",
            initialTime = currentTime,
            onDismiss = { editingHabitTimeFor = null },
            onConfirm = { newTime ->
                viewModel.toggleHabitReminder(targetHabit.id, true, newTime)
                editingHabitTimeFor = null
                Toast.makeText(context, "Reminder set for $newTime ⚡", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Night Review Time Edit Dialog
    if (editingNightReviewTime) {
        TimePickerDialog(
            title = "Night Review Reminder Time",
            initialTime = reminderSettings.nightReview.time,
            onDismiss = { editingNightReviewTime = false },
            onConfirm = { newTime ->
                viewModel.updateNightReviewSettings(reminderSettings.nightReview.isEnabled, newTime)
                editingNightReviewTime = false
                Toast.makeText(context, "Night Review set for $newTime 🌙", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Water Window Edit Dialog
    if (editingWaterWindow) {
        val currentWater = reminderSettings.waterReminders
        WaterWindowDialog(
            startTime = currentWater.startTime,
            endTime = currentWater.endTime,
            onDismiss = { editingWaterWindow = false },
            onConfirm = { start, end ->
                val updated = currentWater.copy(startTime = start, endTime = end)
                viewModel.updateWaterReminderSettings(updated)
                editingWaterWindow = false
                Toast.makeText(context, "Active hours set: $start - $end 💧", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun PermissionStatusCard(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                1.dp,
                if (hasPermission) Color(0x3322C55E) else Color(0x33F59E0B),
                RoundedCornerShape(18.dp)
            ),
        color = if (hasPermission) Color(0x1A052E16) else Color(0x1A451A03)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (hasPermission) Color(0x3322C55E) else Color(0x33F59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasPermission) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                        contentDescription = null,
                        tint = if (hasPermission) NeonGreen else AmberRating,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = if (hasPermission) "Notifications Enabled" else "Permission Required",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasPermission) NeonGreen else AmberRating
                    )
                    Text(
                        text = if (hasPermission) "Discipline alerts will fire on schedule" else "Enable to receive habit, water and review alerts",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            if (!hasPermission) {
                OutlinedButton(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberRating),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(AmberRating, AmberRating))),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WebPreviewNoticeCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x2638BDF8), RoundedCornerShape(16.dp)),
        color = Color(0x140284C7)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )

            Column {
                Text(
                    text = "Platform Capability Note",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Exact background alarms and lock-screen wakeups rely on native Android OS execution. In AI Studio Web Preview, your reminder preferences are securely saved to Firebase cloud, and you can verify alerts immediately using the 'Test Alert' buttons.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun NightReviewSettingsCard(
    nightSettings: NightReviewReminderSettings,
    onToggle: (Boolean) -> Unit,
    onEditTime: () -> Unit,
    onOpenNightReview: () -> Unit,
    onSendTestAlert: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0x33A855F7), RoundedCornerShape(20.dp)),
        color = Color(0x1A1E1B4B)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x33A855F7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bedtime,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Night Review Reminder",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Daily reflection at ${nightSettings.time}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = nightSettings.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonPurple,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Color(0x3364748B)
                    )
                )
            }

            AnimatedVisibility(visible = nightSettings.isEnabled) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reminder Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onEditTime() }
                                .border(1.dp, Color(0x4DA855F7), RoundedCornerShape(8.dp)),
                            color = Color(0x26A855F7)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.AccessTime, null, tint = NeonPurple, modifier = Modifier.size(14.dp))
                                Text(
                                    text = nightSettings.time,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonPurple
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenNightReview,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPurple),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeonPurple, NeonPurple)))
                        ) {
                            Text("Open Tonight's Review", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = onSendTestAlert,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(38.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33A855F7))
                        ) {
                            Text("Test Alert", fontSize = 12.sp, color = NeonPurple, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WaterReminderSettingsCard(
    settings: WaterReminderSettings,
    targetMl: Int,
    onToggle: (Boolean) -> Unit,
    onSelectInterval: (Int) -> Unit,
    onEditWindow: () -> Unit,
    onSendTestAlert: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0x3306B6D4), RoundedCornerShape(20.dp)),
        color = Color(0x1A083344)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x3306B6D4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WaterDrop,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Hydration Reminders",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Every ${settings.intervalMinutes} min (${settings.startTime} - ${settings.endTime})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CyanAccent,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Color(0x3364748B)
                    )
                )
            }

            AnimatedVisibility(visible = settings.isEnabled) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Reminder Interval",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val intervals = listOf(30, 60, 90, 120)
                        intervals.forEach { mins ->
                            val isSelected = settings.intervalMinutes == mins
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectInterval(mins) }
                                    .border(
                                        1.dp,
                                        if (isSelected) CyanAccent else Color(0x2664748B),
                                        RoundedCornerShape(10.dp)
                                    ),
                                color = if (isSelected) Color(0x3306B6D4) else Color(0x141E293B)
                            ) {
                                Text(
                                    text = "${mins}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CyanAccent else TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Hours Window",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Reminders pause during sleep",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onEditWindow() }
                                .border(1.dp, Color(0x4D06B6D4), RoundedCornerShape(8.dp)),
                            color = Color(0x2606B6D4)
                        ) {
                            Text(
                                text = "${settings.startTime} - ${settings.endTime}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onSendTestAlert,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x3306B6D4))
                        ) {
                            Text("Test Water Alert", fontSize = 12.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabitReminderItemCard(
    habit: Habit,
    isEnabled: Boolean,
    reminderTime: String,
    onToggle: (Boolean) -> Unit,
    onEditTime: () -> Unit,
    onTestAlert: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isEnabled) Color(0x3322C55E) else Color(0x1FFFFFFF),
                RoundedCornerShape(16.dp)
            ),
        color = Color(0x1F111827)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x2622C55E)),
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryIcon(
                            iconName = habit.iconName,
                            tint = if (isEnabled) NeonGreen else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = habit.category.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            if (isEnabled) {
                                Text("•", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                Text(
                                    text = "Alert at $reminderTime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Color(0x3364748B)
                    )
                )
            }

            AnimatedVisibility(visible = isEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEditTime() }
                            .border(1.dp, Color(0x4D22C55E), RoundedCornerShape(8.dp)),
                        color = Color(0x1F22C55E)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Edit, null, tint = NeonGreen, modifier = Modifier.size(12.dp))
                            Text(
                                text = "Edit Time: $reminderTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onTestAlert,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeonGreen, NeonGreen)))
                    ) {
                        Text("Test Alert", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    title: String,
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var timeInput by remember { mutableStateOf(initialTime) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF20F172A),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter time in 24-hour format (HH:mm):",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = timeInput,
                    onValueChange = {
                        timeInput = it.take(5)
                        errorMsg = null
                    },
                    label = { Text("Time (e.g. 07:30 or 21:00)") },
                    singleLine = true,
                    isError = errorMsg != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color(0x3364748B)
                    )
                )

                errorMsg?.let {
                    Text(text = it, color = CoralRed, style = MaterialTheme.typography.labelSmall)
                }

                // Quick presets
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf("06:30", "07:30", "12:00", "18:00", "21:30")
                    presets.forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { timeInput = preset }
                                .border(1.dp, Color(0x3364748B), RoundedCornerShape(8.dp)),
                            color = Color(0x1F1E293B)
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val regex = Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
                    if (regex.matches(timeInput.trim())) {
                        onConfirm(timeInput.trim())
                    } else {
                        errorMsg = "Please enter valid 24h time like 07:30 or 21:00"
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text("Set Time", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun WaterWindowDialog(
    startTime: String,
    endTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var start by remember { mutableStateOf(startTime) }
    var end by remember { mutableStateOf(endTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF20F172A),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Hydration Active Hours",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Reminders will only be sent between start and end times:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it.take(5) },
                    label = { Text("Start Time (e.g. 08:00)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it.take(5) },
                    label = { Text("End Time (e.g. 21:00)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(start.trim(), end.trim())
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Text("Save Window", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
