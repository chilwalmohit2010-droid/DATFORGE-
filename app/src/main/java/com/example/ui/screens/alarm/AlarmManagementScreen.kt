package com.example.ui.screens.alarm

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.alarm.ChallengeAlarmActivity
import com.example.alarm.ChallengeAlarmManager
import com.example.data.models.ChallengeAlarm
import com.example.data.models.ChallengeHistoryEntry
import com.example.data.models.ChallengeType
import com.example.data.models.Habit
import com.example.ui.components.CategoryIcon
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.WatermarkFooter
import com.example.ui.components.tactilePress
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmManagementScreen(
    viewModel: DisciplineViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alarms by viewModel.allAlarms.collectAsState()
    val habits by viewModel.allHabits.collectAsState()
    val history by viewModel.challengeHistory.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<ChallengeAlarm?>(null) }
    var alarmToDelete by remember { mutableStateOf<ChallengeAlarm?>(null) }

    // Permissions check
    val alarmManagerHelper = remember { ChallengeAlarmManager(context) }
    var canScheduleExact by remember { mutableStateOf(alarmManagerHelper.canScheduleExactAlarms()) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x330A0F1D))
                    .statusBarsPadding()
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
                            .testTag("alarm_management_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Wake-Up Challenges ⚔️",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Native alarms requiring movement to dismiss",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    // Quick Add Alarm button in header
                    IconButton(
                        onClick = {
                            alarmToEdit = null
                            showAddEditDialog = true
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x3322C55E))
                            .border(1.dp, Color(0x6622C55E), CircleShape)
                            .testTag("alarm_management_add_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Alarm",
                            tint = NeonGreenBright
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    alarmToEdit = null
                    showAddEditDialog = true
                },
                containerColor = NeonGreenBright,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.testTag("alarm_management_fab")
            ) {
                Icon(Icons.Filled.AlarmAdd, contentDescription = "Add Alarm", modifier = Modifier.size(26.dp))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            // Permission & Compatibility Card if needed
            if (!canScheduleExact || !hasNotificationPermission) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0x33F59E0B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x80F59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.WarningAmber, null, tint = StreakAmber, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "System Alarm Readiness",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "To ensure wake-up challenges ring reliably on time when the device is locked or asleep, please grant Exact Alarm and Notification capabilities.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Open Device Settings > Alarms & Reminders", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Exact Alarms", fontSize = 12.sp)
                                    }
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                    Button(
                                        onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                        colors = ButtonDefaults.buttonColors(containerColor = StreakAmber, contentColor = Color.Black),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Allow Notifications", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Web Preview Banner Note (Requirement 15)
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x331E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Android, null, tint = FocusCyan, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Native Android Alarm Engine",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Alarms use native AlarmManager, WakeLocks, Full-Screen Intent, and local pose analysis. Live testing requires the DAYFORGE Android app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Alarms Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SCHEDULED ALARMS (${alarms.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    if (alarms.isNotEmpty()) {
                        Text(
                            text = "+ New Alarm",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreenBright,
                            modifier = Modifier
                                .clickable {
                                    alarmToEdit = null
                                    showAddEditDialog = true
                                }
                                .padding(4.dp)
                                .testTag("alarm_new_text_button")
                        )
                    }
                }
            }

            // Alarms List or Empty State
            if (alarms.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x80131C2E)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x3322C55E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Alarm,
                                    contentDescription = null,
                                    tint = NeonGreenBright,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "No Wake-Up Alarms Set",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Forge morning discipline by creating a challenge alarm that requires squats, push-ups, or jumping jacks to stop!",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp
                            )
                            Button(
                                onClick = {
                                    alarmToEdit = null
                                    showAddEditDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreenBright,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("alarm_empty_add_button")
                            ) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Wake-Up Challenge", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmItemCard(
                        alarm = alarm,
                        habits = habits,
                        onToggle = { isEnabled ->
                            viewModel.toggleAlarm(alarm.id, isEnabled)
                        },
                        onEdit = {
                            alarmToEdit = alarm
                            showAddEditDialog = true
                        },
                        onDelete = {
                            alarmToDelete = alarm
                        },
                        onTestChallenge = {
                            // Launch Challenge Alarm Activity immediately
                            val testIntent = Intent(context, ChallengeAlarmActivity::class.java).apply {
                                putExtra(ChallengeAlarmManager.EXTRA_ALARM_ID, alarm.id)
                                putExtra(ChallengeAlarmManager.EXTRA_ALARM_NAME, alarm.name)
                                putExtra(ChallengeAlarmManager.EXTRA_CHALLENGE_TYPE, alarm.challengeType.name)
                                putExtra(ChallengeAlarmManager.EXTRA_TARGET, alarm.target)
                                putExtra(ChallengeAlarmManager.EXTRA_LINKED_HABIT_ID, alarm.linkedHabitId)
                                putExtra(ChallengeAlarmManager.EXTRA_SOUND_URI, alarm.soundUri)
                                putExtra(ChallengeAlarmManager.EXTRA_VIDEO_URI, alarm.videoUri)
                                putExtra(ChallengeAlarmManager.EXTRA_SNOOZE_MINUTES, alarm.snoozeMinutes)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            context.startActivity(testIntent)
                        }
                    )
                }
            }

            // Recent Challenge History Section
            if (history.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "RECENT CHALLENGE LOGS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                }

                items(history.take(5), key = { it.id }) { entry ->
                    ChallengeHistoryItem(entry = entry)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                WatermarkFooter()
            }
        }
    }

    // Add / Edit Alarm Dialog
    if (showAddEditDialog) {
        AddEditAlarmDialog(
            existingAlarm = alarmToEdit,
            habits = habits,
            onDismiss = { showAddEditDialog = false },
            onSave = { updatedAlarm ->
                viewModel.saveAlarm(updatedAlarm) {
                    showAddEditDialog = false
                    Toast.makeText(context, "Alarm '${updatedAlarm.name}' saved ⏰", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (alarmToDelete != null) {
        val target = alarmToDelete!!
        AlertDialog(
            onDismissRequest = { alarmToDelete = null },
            title = {
                Text("Delete Wake-Up Alarm?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to delete '${target.name}' set for ${target.time}? Its native schedule will be cancelled.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAlarm(target)
                        alarmToDelete = null
                        Toast.makeText(context, "Alarm deleted", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete", color = CrimsonRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { alarmToDelete = null }) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            containerColor = Color(0xFF141C2E),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun AlarmItemCard(
    alarm: ChallengeAlarm,
    habits: List<Habit>,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTestChallenge: () -> Unit
) {
    val formattedTime = remember(alarm.time) {
        try {
            val parts = alarm.time.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val localTime = LocalTime.of(h, m)
            localTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
        } catch (e: Exception) {
            alarm.time
        }
    }

    val repeatText = remember(alarm.repeatDays) {
        val days = alarm.getRepeatDaysList()
        when {
            days.isEmpty() -> "One-time"
            days.size == 7 -> "Daily"
            days == listOf(1, 2, 3, 4, 5) -> "Weekdays"
            days == listOf(6, 7) -> "Weekends"
            else -> {
                val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                days.mapNotNull { names.getOrNull(it - 1) }.joinToString(", ")
            }
        }
    }

    val linkedHabit = remember(alarm.linkedHabitId, habits) {
        habits.firstOrNull { it.id == alarm.linkedHabitId }
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alarm_item_${alarm.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Time & Toggle Switch Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = if (alarm.isEnabled) NeonGreenBright else TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = alarm.name.ifBlank { "Morning Warrior Alarm" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (alarm.isEnabled) TextPrimary else TextSecondary
                    )
                }

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = NeonGreenBright,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Color(0x33FFFFFF)
                    ),
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                )
            }

            // Challenge Badge & Repeat Days Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Challenge Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x2622C55E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4D22C55E))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CategoryIcon(iconName = alarm.challengeType.iconName, tint = NeonGreenBright, modifier = Modifier.size(13.dp))
                        Text(
                            text = "${alarm.challengeType.displayName} • ${alarm.target} ${alarm.challengeType.unit}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreenBright,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Repeat days pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x2638BDF8),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4D38BDF8))
                ) {
                    Text(
                        text = repeatText,
                        style = MaterialTheme.typography.labelSmall,
                        color = FocusCyan,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Additional details: Sound, Video, Linked Habit
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (linkedHabit != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Link, null, tint = FocusCyan, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Linked to: ${linkedHabit.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.MusicNote, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Text(
                            text = alarm.soundTitle.ifBlank { "Default Sound" },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (alarm.videoUri.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Videocam, null, tint = NeonPurple, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Video set",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonPurple,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (alarm.vibration) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Vibration, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Vibrate",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)

            // Card Bottom Actions: Test Challenge, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Test Challenge preview button
                OutlinedButton(
                    onClick = onTestChallenge,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreenBright),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6622C55E)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("alarm_test_button_${alarm.id}")
                ) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Challenge", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp).testTag("alarm_edit_${alarm.id}")
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp).testTag("alarm_delete_${alarm.id}")
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = CrimsonRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeHistoryItem(entry: ChallengeHistoryEntry) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x660F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1FFFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (entry.status == "COMPLETED") Color(0x3322C55E) else Color(0x33EF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (entry.status == "COMPLETED") Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (entry.status == "COMPLETED") NeonGreenBright else CrimsonRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "${entry.challengeType.displayName} (${entry.completedAmount}/${entry.target} ${entry.challengeType.unit})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${entry.alarmName.ifBlank { "Challenge Alarm" }} • ${entry.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (entry.status == "COMPLETED") Color(0x3322C55E) else Color(0x33EF4444)
            ) {
                Text(
                    text = entry.status,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.status == "COMPLETED") NeonGreenBright else CrimsonRed,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Add / Edit Challenge Alarm Modal Dialog (Requirement 1, 10, 11)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmDialog(
    existingAlarm: ChallengeAlarm?,
    habits: List<Habit>,
    onDismiss: () -> Unit,
    onSave: (ChallengeAlarm) -> Unit
) {
    val context = LocalContext.current

    var alarmName by remember { mutableStateOf(existingAlarm?.name ?: "Wake-Up Warrior") }
    var selectedHour by remember { mutableStateOf(existingAlarm?.time?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 6) }
    var selectedMinute by remember { mutableStateOf(existingAlarm?.time?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 30) }
    var selectedDays by remember {
        mutableStateOf(
            existingAlarm?.getRepeatDaysList()?.toSet() ?: setOf(1, 2, 3, 4, 5, 6, 7)
        )
    }
    var challengeType by remember { mutableStateOf(existingAlarm?.challengeType ?: ChallengeType.PUSH_UPS) }
    var target by remember { mutableStateOf(existingAlarm?.target ?: 15) }
    var linkedHabitId by remember { mutableStateOf(existingAlarm?.linkedHabitId) }

    var soundUri by remember { mutableStateOf(existingAlarm?.soundUri ?: "") }
    var soundTitle by remember { mutableStateOf(existingAlarm?.soundTitle ?: "DayForge Default Sound") }
    var videoUri by remember { mutableStateOf(existingAlarm?.videoUri ?: "") }
    var vibration by remember { mutableStateOf(existingAlarm?.vibration ?: true) }
    var snoozeMinutes by remember { mutableStateOf(existingAlarm?.snoozeMinutes ?: 5) }

    // Audio Preview MediaPlayer
    var isPlayingPreview by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.release()
            previewPlayer = null
        }
    }

    fun stopAudioPreview() {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
            isPlayingPreview = false
        } catch (e: Exception) {}
    }

    fun playAudioPreview(uriStr: String) {
        stopAudioPreview()
        try {
            val uri = if (uriStr.isNotBlank()) Uri.parse(uriStr) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            previewPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
                start()
                setOnCompletionListener {
                    isPlayingPreview = false
                }
            }
            isPlayingPreview = true
        } catch (e: Exception) {
            Toast.makeText(context, "Could not play sound preview: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Audio Sound Picker Launcher (System Ringtone / Audio Picker)
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                soundUri = uri.toString()
                val ringtone = RingtoneManager.getRingtone(context, uri)
                soundTitle = ringtone.getTitle(context) ?: "Custom Sound"
            }
        }
    }

    // Custom Video Picker Launcher (Android Photo/Video Picker)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            videoUri = uri.toString()
            Toast.makeText(context, "Custom video selected! 🎬", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = {
            stopAudioPreview()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB3030712))
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x6622C55E), Color(0x2638BDF8), Color(0x10FFFFFF))
                        ),
                        RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFA0E1424)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Modal Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (existingAlarm == null) "Set Challenge Alarm ⏰" else "Edit Challenge Alarm ⏰",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Define challenge target & wake-up time",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        IconButton(
                            onClick = {
                                stopAudioPreview()
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0x1AFFFFFF))
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Scrollable Config Form
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Alarm Name Field
                        OutlinedTextField(
                            value = alarmName,
                            onValueChange = { alarmName = it },
                            label = { Text("Alarm Name", color = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("alarm_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreenBright,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // 2. Alarm Time Selector
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x40141C2E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "ALARM TIME",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Hour Stepper
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(onClick = { selectedHour = (selectedHour + 1) % 24 }) {
                                            Icon(Icons.Filled.KeyboardArrowUp, null, tint = TextPrimary)
                                        }
                                        Text(
                                            text = String.format("%02d", selectedHour),
                                            style = MaterialTheme.typography.displaySmall,
                                            fontWeight = FontWeight.Black,
                                            color = NeonGreenBright
                                        )
                                        IconButton(onClick = { selectedHour = if (selectedHour - 1 < 0) 23 else selectedHour - 1 }) {
                                            Icon(Icons.Filled.KeyboardArrowDown, null, tint = TextPrimary)
                                        }
                                    }

                                    Text(
                                        text = ":",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    // Minute Stepper
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(onClick = { selectedMinute = (selectedMinute + 5) % 60 }) {
                                            Icon(Icons.Filled.KeyboardArrowUp, null, tint = TextPrimary)
                                        }
                                        Text(
                                            text = String.format("%02d", selectedMinute),
                                            style = MaterialTheme.typography.displaySmall,
                                            fontWeight = FontWeight.Black,
                                            color = NeonGreenBright
                                        )
                                        IconButton(onClick = { selectedMinute = if (selectedMinute - 5 < 0) 55 else selectedMinute - 5 }) {
                                            Icon(Icons.Filled.KeyboardArrowDown, null, tint = TextPrimary)
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Repeat Days Selector
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x40141C2E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "REPEAT DAYS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.sp
                                )

                                // Preset Chips (Daily, Weekdays, Weekends, Once)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val isDaily = selectedDays.size == 7
                                    val isWeekdays = selectedDays == setOf(1, 2, 3, 4, 5)
                                    val isOnce = selectedDays.isEmpty()

                                    FilterChip(
                                        selected = isDaily,
                                        onClick = { selectedDays = setOf(1, 2, 3, 4, 5, 6, 7) },
                                        label = { Text("Daily", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = isWeekdays,
                                        onClick = { selectedDays = setOf(1, 2, 3, 4, 5) },
                                        label = { Text("Weekdays", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = isOnce,
                                        onClick = { selectedDays = emptySet() },
                                        label = { Text("Once", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Individual Day Bubbles (Mon - Sun)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                                    for (dayIdx in 1..7) {
                                        val isSelected = selectedDays.contains(dayIdx)
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) NeonGreenBright else Color(0x331E293B))
                                                .border(1.dp, if (isSelected) NeonGreenBright else Color(0x33FFFFFF), CircleShape)
                                                .clickable {
                                                    selectedDays = if (isSelected) {
                                                        selectedDays - dayIdx
                                                    } else {
                                                        selectedDays + dayIdx
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayLabels[dayIdx - 1],
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.Black else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Challenge Type & Custom Target
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x40141C2E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "WAKE-UP CHALLENGE TYPE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.sp
                                )

                                ChallengeType.values().forEach { type ->
                                    val isSelected = challengeType == type
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) Color(0x3322C55E) else Color(0x1AFFFFFF),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) NeonGreenBright else Color(0x26FFFFFF)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                challengeType = type
                                                if (type == ChallengeType.TIMED_CHALLENGE) {
                                                    target = 60
                                                } else {
                                                    target = type.defaultTarget
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                CategoryIcon(
                                                    iconName = type.iconName,
                                                    tint = if (isSelected) NeonGreenBright else TextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = type.displayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) NeonGreenBright else TextPrimary
                                                )
                                            }
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    challengeType = type
                                                    if (type == ChallengeType.TIMED_CHALLENGE) {
                                                        target = 60
                                                    } else {
                                                        target = type.defaultTarget
                                                    }
                                                },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = NeonGreenBright,
                                                    unselectedColor = TextSecondary
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Target Stepper
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Target (${challengeType.unit}):",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val step = if (challengeType == ChallengeType.TIMED_CHALLENGE) 15 else 5
                                                target = (target - step).coerceAtLeast(step)
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x33FFFFFF))
                                        ) {
                                            Icon(Icons.Filled.Remove, null, tint = TextPrimary)
                                        }

                                        Text(
                                            text = "$target",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            color = NeonGreenBright
                                        )

                                        IconButton(
                                            onClick = {
                                                val step = if (challengeType == ChallengeType.TIMED_CHALLENGE) 15 else 5
                                                target = (target + step).coerceAtMost(300)
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x33FFFFFF))
                                        ) {
                                            Icon(Icons.Filled.Add, null, tint = TextPrimary)
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Linked Habit (Optional)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x40141C2E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "LINK TO EXISTING HABIT (OPTIONAL)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.sp
                                )

                                FilterChip(
                                    selected = linkedHabitId == null,
                                    onClick = { linkedHabitId = null },
                                    label = { Text("None") }
                                )

                                habits.forEach { habit ->
                                    val isSelected = linkedHabitId == habit.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { linkedHabitId = habit.id },
                                        label = { Text(habit.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                    )
                                }
                            }
                        }

                        // 6. Custom Alarm Sound (Requirement 10)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x40141C2E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "ALARM SOUND",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = soundTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (soundUri.isBlank()) "Default Ringtone" else "Custom Audio URI",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Preview Button
                                    IconButton(
                                        onClick = {
                                            if (isPlayingPreview) {
                                                stopAudioPreview()
                                            } else {
                                                playAudioPreview(soundUri)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x3322C55E))
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingPreview) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                            contentDescription = "Preview Sound",
                                            tint = NeonGreenBright,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                                if (soundUri.isNotBlank()) {
                                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(soundUri))
                                                }
                                            }
                                            ringtonePickerLauncher.launch(intent)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Choose Sound", fontSize = 12.sp)
                                    }

                                    if (soundUri.isNotBlank()) {
                                        TextButton(
                                            onClick = {
                                                soundUri = ""
                                                soundTitle = "DayForge Default Sound"
                                                stopAudioPreview()
                                            }
                                        ) {
                                            Text("Restore Default", fontSize = 11.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }

                        // 7. Optional Custom Alarm Video (Requirement 11)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x40141C2E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "ALARM VIDEO (OPTIONAL)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.sp
                                )

                                Text(
                                    text = if (videoUri.isNotBlank()) "Video selected (plays muted on alarm screen)" else "No background video selected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (videoUri.isNotBlank()) NeonPurple else TextSecondary,
                                    fontSize = 12.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            videoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.VideoLibrary, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (videoUri.isBlank()) "Select Video" else "Change Video", fontSize = 12.sp)
                                    }

                                    if (videoUri.isNotBlank()) {
                                        TextButton(onClick = { videoUri = "" }) {
                                            Text("Remove Video", fontSize = 11.sp, color = CrimsonRed)
                                        }
                                    }
                                }
                            }
                        }

                        // 8. Vibration & Snooze Controls
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x40141C2E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Vibrate during alarm", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    Switch(
                                        checked = vibration,
                                        onCheckedChange = { vibration = it },
                                        colors = SwitchDefaults.colors(checkedTrackColor = NeonGreenBright)
                                    )
                                }

                                HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Snooze interval", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(0, 5, 10, 15).forEach { mins ->
                                            FilterChip(
                                                selected = snoozeMinutes == mins,
                                                onClick = { snoozeMinutes = mins },
                                                label = { Text(if (mins == 0) "Off" else "${mins}m", fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Modal Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                stopAudioPreview()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                stopAudioPreview()
                                val timeFormatted = String.format("%02d:%02d", selectedHour, selectedMinute)
                                val repeatString = selectedDays.sorted().joinToString(",")
                                val id = existingAlarm?.id ?: UUID.randomUUID().toString()

                                val updatedAlarm = ChallengeAlarm(
                                    id = id,
                                    userId = existingAlarm?.userId ?: "",
                                    name = alarmName.ifBlank { "Wake-Up Warrior" },
                                    time = timeFormatted,
                                    repeatDays = repeatString,
                                    isEnabled = true,
                                    challengeType = challengeType,
                                    target = target,
                                    linkedHabitId = linkedHabitId,
                                    soundUri = soundUri,
                                    soundTitle = soundTitle,
                                    videoUri = videoUri,
                                    vibration = vibration,
                                    snoozeMinutes = snoozeMinutes,
                                    createdAt = existingAlarm?.createdAt ?: System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                onSave(updatedAlarm)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreenBright,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("alarm_save_button")
                        ) {
                            Text("Save Alarm", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
