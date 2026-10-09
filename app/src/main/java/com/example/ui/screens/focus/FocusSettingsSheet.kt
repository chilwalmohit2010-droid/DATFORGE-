package com.example.ui.screens.focus

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.models.Habit
import com.example.focus.FocusSessionManager
import com.example.ui.components.tactilePress
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel
import com.example.util.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSettingsSheet(
    viewModel: DisciplineViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val focusSettings by viewModel.focusSettings.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()

    var hasUsageAccess by remember {
        mutableStateOf(PermissionManager.hasUsageAccessPermission(context))
    }

    // Refresh permission when user returns from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = PermissionManager.hasUsageAccessPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showManageRestrictedAppsDialog by remember { mutableStateOf(false) }
    var showSelectRelatedAppDialog by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var showCustomDurationInput by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf(focusSettings.durationMinutes.toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xF20F172A),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0x40FFFFFF))
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Focus Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Durations, habit links & app restrictions",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x331E293B))
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
            }

            // ==========================================
            // 1. FOCUS DURATION (15, 25, 45, 60, 90, Custom)
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "FOCUS DURATION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = TextSecondary
                    )

                    val presetDurations = listOf(15, 25, 45, 60, 90)
                    val currentMins = focusSettings.durationMinutes
                    val isCustom = !presetDurations.contains(currentMins)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetDurations.forEach { mins ->
                            val isSelected = currentMins == mins && !showCustomDurationInput
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) NeonGreen else Color(0x331E293B),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonGreen else Color(0x26FFFFFF)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .tactilePress(scaleDown = 0.92f) {
                                        if (!isTimerRunning) {
                                            showCustomDurationInput = false
                                            viewModel.setFocusPreset(mins)
                                        }
                                    }
                            ) {
                                Text(
                                    text = "${mins}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        // Custom chip
                        val isCustomSelected = isCustom || showCustomDurationInput
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCustomSelected) NeonGreen else Color(0x331E293B),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCustomSelected) NeonGreen else Color(0x26FFFFFF)
                            ),
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(12.dp))
                                .tactilePress(scaleDown = 0.92f) {
                                    if (!isTimerRunning) {
                                        showCustomDurationInput = true
                                    }
                                }
                        ) {
                            Text(
                                text = if (isCustom) "${currentMins}m" else "Custom",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCustomSelected) Color.Black else TextPrimary,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Custom duration text field
                    if (showCustomDurationInput) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customMinutesText,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }.take(3)
                                    customMinutesText = filtered
                                },
                                label = { Text("Duration (1 to 480 mins)", color = TextSecondary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    val mins = customMinutesText.toIntOrNull()?.coerceIn(1, 480) ?: 25
                                    viewModel.setCustomFocusDuration(mins)
                                    showCustomDurationInput = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text("Apply", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. LINKED HABIT (Existing habits, None)
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "LINKED HABIT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = TextSecondary
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "None" option
                        val isNoneSelected = focusSettings.linkedHabitId == null
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isNoneSelected) NeonGreen else Color(0x331E293B),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isNoneSelected) NeonGreen else Color(0x26FFFFFF)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .tactilePress(scaleDown = 0.94f) {
                                        viewModel.setSelectedFocusHabit(null)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = null,
                                        tint = if (isNoneSelected) Color.Black else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "None (Deep Work)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNoneSelected) Color.Black else TextPrimary
                                    )
                                }
                            }
                        }

                        // Habits from user profile
                        items(allHabits, key = { it.id }) { habit ->
                            val isSelected = focusSettings.linkedHabitId == habit.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) NeonGreen else Color(0x331E293B),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonGreen else Color(0x26FFFFFF)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .tactilePress(scaleDown = 0.94f) {
                                        viewModel.setSelectedFocusHabit(habit.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Flag,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.Black else NeonGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = habit.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. APP RESTRICTIONS (OFF / ON & Manage)
            // ==========================================
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x8C141C2E),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x14FFFFFF)))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Switch Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Shield,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "App Restrictions",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "Block distracting apps during active Focus sessions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            Switch(
                                checked = focusSettings.restrictionEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && !hasUsageAccess) {
                                        showPermissionRationaleDialog = true
                                    } else {
                                        viewModel.updateFocusRestrictionEnabled(checked)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = NeonGreen
                                )
                            )
                        }

                        // Permission Warning Banner if access is needed
                        if (!hasUsageAccess) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x1AEF4444),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4DEF4444)),
                                modifier = Modifier.fillMaxWidth()
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
                                        onClick = { showPermissionRationaleDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CoralRed,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(42.dp)
                                    ) {
                                        Text("Grant Focus Access", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Manage Restricted Apps button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Restricted Apps (${focusSettings.restrictedApps.size})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            TextButton(onClick = { showManageRestrictedAppsDialog = true }) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Manage Apps", color = NeonGreen, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Selected Restricted Apps list
                        if (focusSettings.restrictedApps.isEmpty()) {
                            Text(
                                text = "No apps selected. Tap 'Manage Apps' to pick distracting applications.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(focusSettings.restrictedApps, key = { it }) { pkg ->
                                    val shortName = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0x331E293B),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = shortName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextPrimary
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Remove $shortName",
                                                tint = TextSecondary,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { viewModel.removeRestrictedApp(pkg) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4. RELATED APP & AUTO-OPEN (Requirement 6 & 7)
            // ==========================================
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x8C141C2E),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x14FFFFFF)))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Launch,
                                contentDescription = null,
                                tint = ElectricIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Related App",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Associate a tool or reading app with Focus (e.g. Notes, PDF, Charts)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Selected app card
                        val relatedApp = focusSettings.relatedAppName
                        val relatedPkg = focusSettings.relatedAppPackage

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x331E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (!relatedApp.isNullOrBlank()) relatedApp else "None",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!relatedApp.isNullOrBlank()) NeonGreen else TextPrimary
                                    )
                                    Text(
                                        text = if (!relatedPkg.isNullOrBlank()) relatedPkg else "No application linked",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!relatedPkg.isNullOrBlank()) {
                                        IconButton(
                                            onClick = { viewModel.updateRelatedApp(null, null) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = TextSecondary)
                                        }
                                    }

                                    Button(
                                        onClick = { showSelectRelatedAppDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ElectricIndigo,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (relatedPkg.isNullOrBlank()) "Select App" else "Change",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Auto-Open Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Open Related App",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Automatically launch this app when your Focus session starts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            Switch(
                                checked = focusSettings.autoOpenRelatedApp,
                                onCheckedChange = { checked ->
                                    if (checked && focusSettings.relatedAppPackage.isNullOrBlank()) {
                                        showSelectRelatedAppDialog = true
                                    } else {
                                        viewModel.updateAutoOpenRelatedApp(checked)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = NeonGreen
                                )
                            )
                        }
                    }
                }
            }

            // Web preview notice
            item {
                Text(
                    text = "App restriction and auto-open utilize Android Usage Access and Package Manager APIs. In web preview, core timers and stats operate seamlessly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0x66FFFFFF),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }

    // Dialog: Manage Restricted Apps (Multi-select)
    if (showManageRestrictedAppsDialog) {
        InstalledAppPickerDialog(
            title = "Manage Restricted Apps",
            subtitle = "Select apps to restrict during active Focus blocks",
            isMultipleSelection = true,
            initiallySelectedPackages = focusSettings.restrictedApps.toSet(),
            onDismiss = { showManageRestrictedAppsDialog = false },
            onConfirmSelection = { selectedApps ->
                viewModel.updateRestrictedApps(selectedApps.map { it.packageName })
                showManageRestrictedAppsDialog = false
            }
        )
    }

    // Dialog: Select Related App (Single-select)
    if (showSelectRelatedAppDialog) {
        InstalledAppPickerDialog(
            title = "Select Related App",
            subtitle = "Choose an app to open automatically when starting Focus",
            isMultipleSelection = false,
            initiallySelectedPackages = focusSettings.relatedAppPackage?.let { setOf(it) } ?: emptySet(),
            onDismiss = { showSelectRelatedAppDialog = false },
            onConfirmSelection = { selectedApps ->
                val chosen = selectedApps.firstOrNull()
                if (chosen != null) {
                    viewModel.updateRelatedApp(chosen.packageName, chosen.appName)
                }
                showSelectRelatedAppDialog = false
            }
        )
    }

    // Dialog: Focus Usage Access Rationale (Requirement 9 & 10)
    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = NeonGreen)
                    Text("Focus Access Required", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Focus Access helps DAYFORGE detect selected app usage while a Focus session is active so it can protect your deep work time.",
                        color = TextPrimary
                    )
                    Text(
                        text = "DAYFORGE does not collect personal data or browsing history. All checks run locally on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationaleDialog = false
                        PermissionManager.openUsageAccessSettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Continue to Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationaleDialog = false }) {
                    Text("Not Now", color = TextSecondary)
                }
            },
            containerColor = Color(0xF20F172A),
            shape = RoundedCornerShape(20.dp)
        )
    }
}
