package com.example.ui.screens.permissions

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.components.tactilePress
import com.example.ui.theme.*
import com.example.util.PermissionManager

/**
 * Reusable DAYFORGE Permission Onboarding Section (Step 20).
 * Displays contextual status, explanations, and direct settings actions
 * for all 6 system permissions.
 */
@Composable
fun PermissionCenterSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Live permission states
    var hasNotifications by remember { mutableStateOf(PermissionManager.hasNotificationPermission(context)) }
    var hasExactAlarm by remember { mutableStateOf(PermissionManager.hasExactAlarmPermission(context)) }
    var hasFullScreenAlarm by remember { mutableStateOf(PermissionManager.hasFullScreenIntentPermission(context)) }
    var hasCamera by remember { mutableStateOf(PermissionManager.hasCameraPermission(context)) }
    var hasUsageAccess by remember { mutableStateOf(PermissionManager.hasUsageAccessPermission(context)) }
    var isBatteryOptimized by remember { mutableStateOf(!PermissionManager.isIgnoringBatteryOptimizations(context)) }

    // Rationale dialog state
    var selectedPermissionForRationale by remember { mutableStateOf<PermissionManager.PermissionItem?>(null) }

    fun refreshAllPermissions() {
        hasNotifications = PermissionManager.hasNotificationPermission(context)
        hasExactAlarm = PermissionManager.hasExactAlarmPermission(context)
        hasFullScreenAlarm = PermissionManager.hasFullScreenIntentPermission(context)
        hasCamera = PermissionManager.hasCameraPermission(context)
        hasUsageAccess = PermissionManager.hasUsageAccessPermission(context)
        isBatteryOptimized = !PermissionManager.isIgnoringBatteryOptimizations(context)
    }

    // Refresh automatically when user returns from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshAllPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Direct permission launchers for runtime permissions
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifications = granted
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCamera = granted
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        Text(
            text = "DEVICE PERMISSIONS & SYSTEM ACCESS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = TextSecondary,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
        )

        // Glass Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))),
                    RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            color = Color(0x8C141C2E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0x14FFFFFF), Color.Transparent)))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Notifications
                PermissionStatusRow(
                    title = "Notifications",
                    subtitle = "Alerts for habit schedules, water intervals & Focus updates",
                    icon = Icons.Filled.NotificationsActive,
                    isGranted = hasNotifications,
                    grantedLabel = "Allowed",
                    deniedLabel = "Not allowed",
                    actionButtonText = "Allow Notifications",
                    onActionClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            selectedPermissionForRationale = PermissionManager.PermissionItem.NOTIFICATIONS
                        }
                    }
                )

                HorizontalDivider(color = Color(0x1AFFFFFF))

                // 2. Exact Alarm
                PermissionStatusRow(
                    title = "Exact Alarm",
                    subtitle = "Allows wake-up challenges & reminders to trigger at the exact second",
                    icon = Icons.Filled.Alarm,
                    isGranted = hasExactAlarm,
                    grantedLabel = "Available",
                    deniedLabel = "Permission Required",
                    actionButtonText = "Allow Alarm Access",
                    onActionClick = {
                        selectedPermissionForRationale = PermissionManager.PermissionItem.EXACT_ALARM
                    }
                )

                HorizontalDivider(color = Color(0x1AFFFFFF))

                // 3. Full-Screen Alarm
                PermissionStatusRow(
                    title = "Full-Screen Alarm",
                    subtitle = "Displays interactive morning challenges over the lock screen",
                    icon = Icons.Filled.Fullscreen,
                    isGranted = hasFullScreenAlarm,
                    grantedLabel = "Allowed",
                    deniedLabel = "Permission Required",
                    actionButtonText = "Allow Full-Screen Alarms",
                    onActionClick = {
                        selectedPermissionForRationale = PermissionManager.PermissionItem.FULL_SCREEN_ALARM
                    }
                )

                HorizontalDivider(color = Color(0x1AFFFFFF))

                // 4. Camera
                PermissionStatusRow(
                    title = "Camera",
                    subtitle = "Used solely for local repetition pose detection (push-ups/squats)",
                    icon = Icons.Filled.Videocam,
                    isGranted = hasCamera,
                    grantedLabel = "Allowed",
                    deniedLabel = "Not allowed",
                    actionButtonText = "Allow Camera",
                    onActionClick = {
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                )

                HorizontalDivider(color = Color(0x1AFFFFFF))

                // 5. Focus / Usage Access
                PermissionStatusRow(
                    title = "Focus / Usage Access",
                    subtitle = "Detects distracting app usage during active Focus sessions",
                    icon = Icons.Filled.Shield,
                    isGranted = hasUsageAccess,
                    grantedLabel = "Allowed",
                    deniedLabel = "Not allowed",
                    actionButtonText = "Grant Focus Access",
                    onActionClick = {
                        selectedPermissionForRationale = PermissionManager.PermissionItem.FOCUS_USAGE_ACCESS
                    }
                )

                HorizontalDivider(color = Color(0x1AFFFFFF))

                // 6. Battery Optimization
                PermissionStatusRow(
                    title = "Battery Optimization",
                    subtitle = "Prevents OS from killing background focus timers or alarms",
                    icon = Icons.Filled.BatteryChargingFull,
                    isGranted = !isBatteryOptimized,
                    grantedLabel = "Unrestricted",
                    deniedLabel = "Optimized",
                    actionButtonText = "Allow Background Activity",
                    onActionClick = {
                        selectedPermissionForRationale = PermissionManager.PermissionItem.BATTERY_OPTIMIZATION
                    }
                )
            }
        }
    }

    // Contextual Smart Explanation Dialog (Requirement 9 & 10)
    selectedPermissionForRationale?.let { perm ->
        AlertDialog(
            onDismissRequest = { selectedPermissionForRationale = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = NeonGreen
                    )
                    Text(
                        text = perm.title,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = perm.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "DAYFORGE is private by design. No tracking data or personal information is ever transmitted off your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedPermissionForRationale = null
                        when (perm) {
                            PermissionManager.PermissionItem.NOTIFICATIONS ->
                                PermissionManager.openNotificationSettings(context)
                            PermissionManager.PermissionItem.EXACT_ALARM ->
                                PermissionManager.openExactAlarmSettings(context)
                            PermissionManager.PermissionItem.FULL_SCREEN_ALARM ->
                                PermissionManager.openFullScreenIntentSettings(context)
                            PermissionManager.PermissionItem.CAMERA ->
                                cameraLauncher.launch(Manifest.permission.CAMERA)
                            PermissionManager.PermissionItem.FOCUS_USAGE_ACCESS ->
                                PermissionManager.openUsageAccessSettings(context)
                            PermissionManager.PermissionItem.BATTERY_OPTIMIZATION ->
                                PermissionManager.openBatteryOptimizationSettings(context)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPermissionForRationale = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xF20F172A),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isGranted: Boolean,
    grantedLabel: String,
    deniedLabel: String,
    actionButtonText: String,
    onActionClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isGranted) Color(0x3322C55E) else Color(0x33EF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) NeonGreenBright else CoralRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Status Chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isGranted) Color(0x3322C55E) else Color(0x33EF4444),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isGranted) Color(0x6622C55E) else Color(0x66EF4444)
                )
            ) {
                Text(
                    text = if (isGranted) grantedLabel else deniedLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) NeonGreen else CoralRed,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 11.sp
                )
            }
        }

        // Action button if permission is missing
        if (!isGranted) {
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x3322C55E),
                    contentColor = NeonGreen
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .tactilePress(scaleDown = 0.96f)
            ) {
                Text(
                    text = actionButtonText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
