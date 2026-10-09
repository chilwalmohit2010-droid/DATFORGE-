package com.example.ui.screens.habits

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.Habit
import com.example.data.models.HabitCategory
import com.example.data.models.HabitDifficulty
import com.example.data.models.HabitGoalType
import com.example.ui.components.CategoryIcon
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitDialog(
    habitToEdit: Habit?,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        title: String,
        description: String,
        category: HabitCategory,
        iconName: String,
        colorHex: Long,
        reminderTime: String?,
        repeatDays: String,
        goalValue: Int,
        goalUnit: String,
        durationMinutes: Int,
        startDate: String?,
        endDate: String?,
        difficulty: HabitDifficulty,
        notes: String,
        isMorningRoutine: Boolean,
        goalType: HabitGoalType,
        isPaused: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf(habitToEdit?.title ?: "") }
    var description by remember { mutableStateOf(habitToEdit?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(habitToEdit?.category ?: HabitCategory.HEALTH) }
    var selectedIcon by remember { mutableStateOf(habitToEdit?.iconName ?: "check_circle") }
    var selectedColor by remember { mutableStateOf(habitToEdit?.colorHex ?: CategoryColors[0]) }
    
    // Goal & Duration
    var goalType by remember { mutableStateOf(habitToEdit?.goalType ?: HabitGoalType.ONCE) }
    var durationMinutes by remember { mutableStateOf(habitToEdit?.durationMinutes ?: 0) }
    var durationMinutesStr by remember { 
        mutableStateOf(if ((habitToEdit?.durationMinutes ?: 0) > 0) habitToEdit!!.durationMinutes.toString() else "30") 
    }
    var goalValueStr by remember { mutableStateOf((habitToEdit?.goalValue ?: 1).toString()) }
    var goalUnit by remember { mutableStateOf(habitToEdit?.goalUnit ?: "times") }

    // Repeat days (1 = Monday, 7 = Sunday)
    var selectedDays by remember {
        mutableStateOf(
            habitToEdit?.getRepeatDaysList()?.toSet() ?: setOf(1, 2, 3, 4, 5, 6, 7)
        )
    }

    // Reminder
    var reminderTime by remember { mutableStateOf(habitToEdit?.reminderTime ?: "07:30") }
    var hasReminder by remember { mutableStateOf(!habitToEdit?.reminderTime.isNullOrBlank()) }

    // Difficulty
    var selectedDifficulty by remember { mutableStateOf(habitToEdit?.difficulty ?: HabitDifficulty.MEDIUM) }

    // Dates
    var startDate by remember { mutableStateOf(habitToEdit?.startDate ?: "") }
    var endDate by remember { mutableStateOf(habitToEdit?.endDate ?: "") }

    // Notes
    var notes by remember { mutableStateOf(habitToEdit?.notes ?: "") }

    // Active / Paused state
    var isPaused by remember { mutableStateOf(habitToEdit?.isPaused ?: false) }

    // Morning routine flag
    var isMorningRoutine by remember { mutableStateOf(habitToEdit?.isMorningRoutine ?: false) }

    val availableIcons = listOf(
        "check_circle", "fitness_center", "favorite", "bolt", "psychology",
        "menu_book", "water_drop", "schedule", "payments", "military_tech",
        "bedtime", "sports_gymnastics", "self_improvement", "hiking", "directions_run"
    )

    val dayLabels = listOf(
        1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB3030712))
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x59FFFFFF), Color(0x1F38BDF8), Color(0x10FFFFFF))
                        ),
                        RoundedCornerShape(24.dp)
                    ),
                color = Color(0xF20F172A),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (habitToEdit == null) "Create New Habit" else "Edit Habit",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = "Define your standards & routines",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = Color(0x26FFFFFF)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Habit Title
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Habit Title *") },
                            placeholder = { Text("e.g. Study Chemistry, 100 Pushups, Drink Water") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("habit_title_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedContainerColor = Color(0x40141C2E),
                                unfocusedContainerColor = Color(0x40141C2E),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        // Habit Description
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description / Purpose (Optional)") },
                            placeholder = { Text("Why this matters to your life & discipline") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedContainerColor = Color(0x40141C2E),
                                unfocusedContainerColor = Color(0x40141C2E),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        // Category Selection
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(HabitCategory.values()) { cat ->
                                val isSelected = (selectedCategory == cat)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) NeonGreen.copy(alpha = 0.2f) else Color(0x331E293B),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) NeonGreen else Color(0x26FFFFFF)
                                    ),
                                    modifier = Modifier
                                        .clickable { selectedCategory = cat }
                                        .testTag("category_chip_${cat.name}")
                                ) {
                                    Text(
                                        text = cat.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) NeonGreen else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        // Goal Type (Complete Once, Duration, Quantity)
                        Text(
                            text = "Habit Goal",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x331E293B))
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            HabitGoalType.values().forEach { type ->
                                val isSelected = (goalType == type)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) NeonGreen else Color.Transparent)
                                        .clickable { goalType = type }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Goal Type specific configuration
                        when (goalType) {
                            HabitGoalType.DURATION -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Target Duration (Minutes)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    // Quick Duration presets (15, 30, 45, 60, 90 mins)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(15, 30, 45, 60, 90).forEach { mins ->
                                            val isSelected = durationMinutesStr == mins.toString()
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) CyanAccent.copy(alpha = 0.25f) else Color(0x331E293B))
                                                    .border(1.dp, if (isSelected) CyanAccent else Color(0x26FFFFFF), RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        durationMinutesStr = mins.toString()
                                                    }
                                                    .padding(vertical = 7.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${mins}m",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) CyanAccent else TextPrimary
                                                )
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = durationMinutesStr,
                                        onValueChange = { durationMinutesStr = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Custom Duration (Minutes)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                            HabitGoalType.QUANTITY -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = goalValueStr,
                                        onValueChange = { goalValueStr = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Target Quantity") },
                                        placeholder = { Text("e.g. 3, 10, 100") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = goalUnit,
                                        onValueChange = { goalUnit = it },
                                        label = { Text("Unit") },
                                        placeholder = { Text("L, pages, reps") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                            HabitGoalType.ONCE -> {
                                // Simple once daily check-in
                            }
                        }

                        // Difficulty Selection (Easy, Medium, Hard)
                        Text(
                            text = "Difficulty Level",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HabitDifficulty.values().forEach { diff ->
                                val isSelected = (selectedDifficulty == diff)
                                val (diffColor, diffBg) = when (diff) {
                                    HabitDifficulty.EASY -> Pair(NeonGreenBright, Color(0x2622C55E))
                                    HabitDifficulty.MEDIUM -> Pair(NeonAmber, Color(0x26FBBF24))
                                    HabitDifficulty.HARD -> Pair(CoralRed, Color(0x26EF4444))
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) diffBg else Color(0x331E293B))
                                        .border(
                                            1.dp,
                                            if (isSelected) diffColor else Color(0x26FFFFFF),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedDifficulty = diff }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = diff.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) diffColor else TextSecondary
                                    )
                                }
                            }
                        }

                        // Color & Icon picker
                        Text(
                            text = "Color Accent",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryColors.forEach { colorVal ->
                                val isSelected = (selectedColor == colorVal)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorVal))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 0.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = colorVal },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Icon Picker
                        Text(
                            text = "Habit Icon",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableIcons) { iconName ->
                                val isSelected = (selectedIcon == iconName)
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Color(selectedColor).copy(alpha = 0.25f)
                                            else Color(0x331E293B)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(selectedColor) else Color(0x26FFFFFF),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedIcon = iconName },
                                    contentAlignment = Alignment.Center
                                ) {
                                    CategoryIcon(
                                        iconName = iconName,
                                        tint = if (isSelected) Color(selectedColor) else TextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Repeat Days Schedule
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Repeat Days",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            // Quick presets (Daily, Weekdays, Weekends)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Daily",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedDays.size == 7) NeonGreen else TextMuted,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedDays.size == 7) Color(0x3322C55E) else Color.Transparent)
                                        .clickable { selectedDays = setOf(1, 2, 3, 4, 5, 6, 7) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "Weekdays",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedDays == setOf(1, 2, 3, 4, 5)) NeonGreen else TextMuted,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedDays == setOf(1, 2, 3, 4, 5)) Color(0x3322C55E) else Color.Transparent)
                                        .clickable { selectedDays = setOf(1, 2, 3, 4, 5) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "Weekends",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedDays == setOf(6, 7)) NeonGreen else TextMuted,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedDays == setOf(6, 7)) Color(0x3322C55E) else Color.Transparent)
                                        .clickable { selectedDays = setOf(6, 7) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            dayLabels.forEach { (dayIso, label) ->
                                val isSelected = selectedDays.contains(dayIso)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) NeonGreen
                                            else Color(0x331E293B)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) NeonGreen else Color(0x26FFFFFF),
                                            CircleShape
                                        )
                                        .clickable {
                                            selectedDays = if (isSelected) {
                                                if (selectedDays.size > 1) selectedDays - dayIso else selectedDays
                                            } else {
                                                selectedDays + dayIso
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else TextPrimary
                                    )
                                }
                            }
                        }

                        // Reminder Time Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x331E293B))
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Daily Reminder",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (hasReminder) "Alert scheduled for $reminderTime" else "No notification",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasReminder) CyanAccent else TextMuted
                                )
                            }
                            Switch(
                                checked = hasReminder,
                                onCheckedChange = { hasReminder = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = NeonGreen
                                )
                            )
                        }

                        if (hasReminder) {
                            OutlinedTextField(
                                value = reminderTime,
                                onValueChange = { reminderTime = it },
                                label = { Text("Reminder Time (HH:mm)") },
                                placeholder = { Text("07:30 or 20:00") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Optional Dates (Start Date & End Date)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = startDate,
                                onValueChange = { startDate = it },
                                label = { Text("Start Date") },
                                placeholder = { Text("YYYY-MM-DD") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = endDate,
                                onValueChange = { endDate = it },
                                label = { Text("End Date (Opt)") },
                                placeholder = { Text("YYYY-MM-DD") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Private Notes
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Private Habit Notes") },
                            placeholder = { Text("e.g. Revise chemistry bonding for 30 minutes, keep water bottle filled") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedContainerColor = Color(0x40141C2E),
                                unfocusedContainerColor = Color(0x40141C2E),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        // Morning Routine & Paused status controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x331E293B))
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Morning Routine",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Pin to Morning Standards",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            Switch(
                                checked = isMorningRoutine,
                                onCheckedChange = { isMorningRoutine = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = NeonGreen
                                )
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x331E293B))
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Pause Habit",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isPaused) "Habit paused (won't affect streak/score)" else "Active habit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isPaused) NeonAmber else TextMuted
                                )
                            }
                            Switch(
                                checked = isPaused,
                                onCheckedChange = { isPaused = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = NeonAmber
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Save Action Button
                    Button(
                        onClick = {
                            if (title.isBlank()) return@Button
                            val repeatCsv = selectedDays.sorted().joinToString(",")
                            val goalVal = goalValueStr.toIntOrNull() ?: 1
                            val durVal = durationMinutesStr.toIntOrNull() ?: 0

                            onSave(
                                habitToEdit?.id,
                                title.trim(),
                                description.trim(),
                                selectedCategory,
                                selectedIcon,
                                selectedColor,
                                if (hasReminder) reminderTime.trim() else null,
                                repeatCsv,
                                goalVal,
                                goalUnit.trim().ifBlank { "times" },
                                if (goalType == HabitGoalType.DURATION) durVal else 0,
                                startDate.trim().ifBlank { null },
                                endDate.trim().ifBlank { null },
                                selectedDifficulty,
                                notes.trim(),
                                isMorningRoutine,
                                goalType,
                                isPaused
                            )
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_habit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = Color.Black
                        ),
                        enabled = title.isNotBlank()
                    ) {
                        Text(
                            text = if (habitToEdit == null) "Create Habit" else "Save Changes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
