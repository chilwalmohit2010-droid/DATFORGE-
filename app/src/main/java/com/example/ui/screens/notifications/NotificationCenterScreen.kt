package com.example.ui.screens.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
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
import androidx.core.content.ContextCompat
import com.example.data.models.AppNotification
import com.example.data.models.NotificationType
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.WatermarkFooter
import com.example.ui.components.tactilePress
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    viewModel: DisciplineViewModel,
    onNavigateBack: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenWater: () -> Unit,
    onOpenNightReview: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAlarms: () -> Unit = {},
    onOpenLeaderboard: () -> Unit = {},
    onOpenContentHub: () -> Unit = {}
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()
    val context = LocalContext.current

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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        viewModel.checkAndGenerateSmartReminders()
    }

    val todayDate = remember { LocalDate.now() }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, h:mm a") }

    // Split into Today and Earlier
    val (todayNotifs, earlierNotifs) = remember(notifications) {
        val todayList = mutableListOf<AppNotification>()
        val earlierList = mutableListOf<AppNotification>()

        notifications.forEach { notif ->
            val notifDate = Instant.ofEpochMilli(notif.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            if (notifDate == todayDate) {
                todayList.add(notif)
            } else {
                earlierList.add(notif)
            }
        }
        Pair(todayList, earlierList)
    }

    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x331E293B))
                                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                    .testTag("notification_center_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Notifications",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    if (unreadCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = CoralRed.copy(alpha = 0.18f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "$unreadCount New",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = CoralRed,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Smart discipline alerts & reminders",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Mark all as read button
                            if (unreadCount > 0) {
                                IconButton(
                                    onClick = { viewModel.markAllNotificationsAsRead() },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x261E293B))
                                        .border(1.dp, Color(0x26FFFFFF), CircleShape)
                                        .testTag("notification_center_mark_all_read")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DoneAll,
                                        contentDescription = "Mark all as read",
                                        tint = CyanBright,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Reminder settings shortcut
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x261E293B))
                                    .border(1.dp, Color(0x26FFFFFF), CircleShape)
                                    .testTag("notification_center_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Reminder Settings",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag("notification_center_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Permission Warning Banner if device notifications are disabled
                if (!hasNotificationPermission) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    Brush.horizontalGradient(listOf(AmberWarning.copy(alpha = 0.5f), Color(0x14FFFFFF))),
                                    RoundedCornerShape(16.dp)
                                )
                                .testTag("notification_permission_banner"),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x661A1820)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.NotificationsOff,
                                        contentDescription = null,
                                        tint = AmberWarning,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "System Alerts Disabled",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Enable notifications to receive timely alerts for habits, water, and night review.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AmberWarning.copy(alpha = 0.2f),
                                        contentColor = AmberWarning
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("enable_notifications_button")
                                ) {
                                    Text("Enable", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Empty State
                if (notifications.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0x66101626),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(CyanBright.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = null,
                                        tint = CyanBright,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(
                                    text = "All Caught Up! ✨",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "No pending alerts or notifications. Your habits, hydration, and reflection schedule are running smoothly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                                Button(
                                    onClick = onOpenSettings,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0x331E293B),
                                        contentColor = CyanBright
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanBright.copy(alpha = 0.3f)),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(Icons.Outlined.Alarm, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Configure Reminder Times")
                                }
                            }
                        }
                    }
                } else {
                    // Today Section
                    if (todayNotifs.isNotEmpty()) {
                        item {
                            Text(
                                text = "TODAY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 4.dp)
                            )
                        }

                        items(todayNotifs, key = { it.id }) { notif ->
                            NotificationItemCard(
                                notification = notif,
                                formattedTime = Instant.ofEpochMilli(notif.timestamp)
                                    .atZone(ZoneId.systemDefault())
                                    .format(timeFormatter),
                                onClick = {
                                    viewModel.markNotificationAsRead(notif.id)
                                    when (notif.targetDestination) {
                                        "habits" -> onOpenHabits()
                                        "water" -> onOpenWater()
                                        "night_review" -> onOpenNightReview()
                                        "focus" -> onOpenFocus()
                                        "alarms" -> onOpenAlarms()
                                        "leaderboard" -> onOpenLeaderboard()
                                        "content" -> onOpenContentHub()
                                        else -> if (notif.type == NotificationType.ALARM) onOpenAlarms() else if (notif.type == NotificationType.LEADERBOARD) onOpenLeaderboard() else if (notif.type == NotificationType.CONTENT) onOpenContentHub() else onOpenHabits()
                                    }
                                },
                                onToggleRead = {
                                    viewModel.markNotificationAsRead(notif.id)
                                },
                                onDelete = {
                                    viewModel.deleteNotification(notif.id)
                                }
                            )
                        }
                    }

                    // Earlier Section
                    if (earlierNotifs.isNotEmpty()) {
                        item {
                            Text(
                                text = "EARLIER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
                            )
                        }

                        items(earlierNotifs, key = { it.id }) { notif ->
                            NotificationItemCard(
                                notification = notif,
                                formattedTime = Instant.ofEpochMilli(notif.timestamp)
                                    .atZone(ZoneId.systemDefault())
                                    .format(dateFormatter),
                                onClick = {
                                    viewModel.markNotificationAsRead(notif.id)
                                    when (notif.targetDestination) {
                                        "habits" -> onOpenHabits()
                                        "water" -> onOpenWater()
                                        "night_review" -> onOpenNightReview()
                                        "focus" -> onOpenFocus()
                                        "alarms" -> onOpenAlarms()
                                        "leaderboard" -> onOpenLeaderboard()
                                        "content" -> onOpenContentHub()
                                        else -> if (notif.type == NotificationType.ALARM) onOpenAlarms() else if (notif.type == NotificationType.LEADERBOARD) onOpenLeaderboard() else if (notif.type == NotificationType.CONTENT) onOpenContentHub() else onOpenHabits()
                                    }
                                },
                                onToggleRead = {
                                    viewModel.markNotificationAsRead(notif.id)
                                },
                                onDelete = {
                                    viewModel.deleteNotification(notif.id)
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    WatermarkFooter()
                }
            }
        }
    }
}

@Composable
private fun NotificationItemCard(
    notification: AppNotification,
    formattedTime: String,
    onClick: () -> Unit,
    onToggleRead: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, accentColor) = when (notification.type) {
        NotificationType.HABIT -> Pair(Icons.AutoMirrored.Filled.DirectionsRun, NeonGreenBright)
        NotificationType.WATER -> Pair(Icons.Filled.WaterDrop, CyanBright)
        NotificationType.NIGHT_REVIEW -> Pair(Icons.Filled.NightlightRound, NeonPurple)
        NotificationType.FOCUS -> Pair(Icons.Filled.Timer, GoldYellow)
        NotificationType.ALARM -> Pair(Icons.Filled.Alarm, CrimsonRed)
        NotificationType.LEADERBOARD -> Pair(Icons.Filled.EmojiEvents, GoldYellow)
        NotificationType.SYSTEM -> Pair(Icons.Filled.Shield, ElectricIndigo)
        NotificationType.CONTENT -> Pair(Icons.Filled.AutoAwesome, NeonCyan)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (!notification.isRead) Color(0x99141C2E) else Color(0x590E1422),
        border = androidx.compose.foundation.BorderStroke(
            width = if (!notification.isRead) 1.2.dp else 1.dp,
            brush = if (!notification.isRead)
                Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.5f), Color(0x14FFFFFF)))
            else
                Brush.verticalGradient(listOf(Color(0x26FFFFFF), Color(0x0AFFFFFF)))
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .tactilePress { onClick() }
            .clickable { onClick() }
            .testTag("notification_card_${notification.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = if (!notification.isRead) 0.22f else 0.12f))
                    .border(
                        1.dp,
                        accentColor.copy(alpha = if (!notification.isRead) 0.45f else 0.2f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Message Body
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(CoralRed)
                        )
                    }

                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (!notification.isRead) TextPrimary else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!notification.isRead) TextPrimary.copy(alpha = 0.85f) else TextMuted,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )

                    Text(
                        text = "Tap to open",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }

            // Quick Dismiss Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss",
                    tint = TextMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
