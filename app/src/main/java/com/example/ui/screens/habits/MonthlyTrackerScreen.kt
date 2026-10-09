package com.example.ui.screens.habits

import androidx.compose.animation.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CompletionStatus
import com.example.data.models.Habit
import com.example.ui.components.CategoryIcon
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.StreakFlameBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun MonthlyTrackerScreen(
    viewModel: DisciplineViewModel,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    val allCompletions by viewModel.allCompletions.collectAsState()
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsState()

    val daysInMonth = selectedYearMonth.lengthOfMonth() // Correctly calculates 28, 29, 30, or 31
    val monthTitle = selectedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    val today = LocalDate.now()
    val isCurrentMonth = (today.year == selectedYearMonth.year && today.month == selectedYearMonth.month)

    // Calculate completions map for quick lookup: "habitId_YYYY-MM-DD" -> CompletionStatus
    val completionsMap = remember(allCompletions) {
        allCompletions.associate { "${it.habitId}_${it.date}" to it.status }
    }

    // Calculate monthly statistics using isScheduledForDate
    val monthlyStats = remember(allHabits, completionsMap, selectedYearMonth, today) {
        var totalScheduled = 0
        var totalCompleted = 0

        for (day in 1..daysInMonth) {
            val date = selectedYearMonth.atDay(day)
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

            // Only count scheduled up to today if in current month
            val isDateElapsedOrToday = !date.isAfter(today)

            allHabits.filterNot { it.isPaused }.forEach { habit ->
                if (habit.isScheduledForDate(date)) {
                    if (isDateElapsedOrToday) {
                        totalScheduled++
                    }
                    val status = completionsMap["${habit.id}_$dateStr"]
                    if (status == CompletionStatus.COMPLETED) {
                        totalCompleted++
                    }
                }
            }
        }
        val rate = if (totalScheduled > 0) ((totalCompleted.toFloat() / totalScheduled.toFloat()) * 100).toInt() else 0
        Pair(rate, totalCompleted)
    }

    LiquidGlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x261E293B))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable { onBack() }
                        .testTag("monthly_tracker_back_button"),
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
                    text = "Monthly Matrix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                StreakFlameBadge(streakDays = currentUser?.currentStreak ?: 0)
            }

            // Month Selector Bar (< Month Year >)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color(0x66141C2E),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x14FFFFFF)))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.previousMonth() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "$daysInMonth Days • ${monthlyStats.first}% Success Rate (${monthlyStats.second} Done)",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonGreenBright,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextMonth() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = "Next Month",
                            tint = TextPrimary
                        )
                    }
                }
            }

            // Legend indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = NeonGreenBright, label = "Completed")
                LegendItem(color = CoralRed, label = "Missed")
                LegendItem(color = NeonAmber, label = "Skipped")
                LegendItem(color = Color(0x33FFFFFF), label = "Off / Future")
            }

            // Main Interactive Grid Table
            val horizontalScrollState = rememberScrollState()

            if (allHabits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No habits created yet. Add habits to view monthly calendar matrix.",
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 60.dp)
                ) {
                    // Header Row with Days (1..N)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x400F172A))
                                .padding(vertical = 8.dp)
                        ) {
                            // Fixed left habit title header
                            Text(
                                text = "HABIT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMuted,
                                modifier = Modifier
                                    .width(130.dp)
                                    .padding(start = 16.dp)
                            )

                            // Horizontal scrollable day headers
                            Row(
                                modifier = Modifier.horizontalScroll(horizontalScrollState),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (day in 1..daysInMonth) {
                                    val isToday = isCurrentMonth && today.dayOfMonth == day
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isToday) Color(0x3322C55E) else Color.Transparent)
                                            .border(
                                                1.dp,
                                                if (isToday) NeonGreenBright else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$day",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isToday) FontWeight.Black else FontWeight.SemiBold,
                                            color = if (isToday) NeonGreenBright else TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }
                        HorizontalDivider(color = Color(0x26FFFFFF))
                    }

                    // Habit Rows
                    items(allHabits, key = { it.id }) { habit ->
                        val habitColor = Color(habit.colorHex)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Sticky Habit Label
                            Row(
                                modifier = Modifier
                                    .width(130.dp)
                                    .padding(start = 16.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (habit.isPaused) TextMuted else habitColor)
                                )
                                Text(
                                    text = habit.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (habit.isPaused) TextMuted else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Horizontal Days Cells
                            Row(
                                modifier = Modifier.horizontalScroll(horizontalScrollState),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (day in 1..daysInMonth) {
                                    val cellDate = selectedYearMonth.atDay(day)
                                    val cellDateStr = cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                    val isScheduled = habit.isScheduledForDate(cellDate)
                                    val isFuture = cellDate.isAfter(today)
                                    val status = completionsMap["${habit.id}_$cellDateStr"]
                                    val isToday = isCurrentMonth && today.dayOfMonth == day

                                    val cellBgColor = when {
                                        status == CompletionStatus.COMPLETED -> NeonGreenBright
                                        status == CompletionStatus.MISSED -> CoralRed.copy(alpha = 0.85f)
                                        status == CompletionStatus.SKIPPED -> NeonAmber.copy(alpha = 0.85f)
                                        !isScheduled -> Color(0x0DFFFFFF)
                                        isFuture -> Color(0x1A1E293B)
                                        else -> Color(0x2E1E293B) // Unrecorded past scheduled date
                                    }

                                    val cellBorderColor = when {
                                        isToday -> CyanAccent
                                        status == CompletionStatus.COMPLETED -> NeonGreen
                                        isFuture -> Color(0x1F38BDF8)
                                        else -> Color(0x26FFFFFF)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(cellBgColor)
                                            .border(
                                                width = if (isToday) 1.5.dp else 0.5.dp,
                                                color = cellBorderColor,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                // Cycle status: Completed -> Missed -> Skipped -> null
                                                val nextStatus = when (status) {
                                                    null -> CompletionStatus.COMPLETED
                                                    CompletionStatus.COMPLETED -> CompletionStatus.MISSED
                                                    CompletionStatus.MISSED -> CompletionStatus.SKIPPED
                                                    CompletionStatus.SKIPPED -> null
                                                }
                                                viewModel.setHabitStatusForDate(habit.id, cellDateStr, nextStatus)
                                            }
                                            .testTag("cell_${habit.id}_$day"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            status == CompletionStatus.COMPLETED -> {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = "Done",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            status == CompletionStatus.MISSED -> {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Missed",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            status == CompletionStatus.SKIPPED -> {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black)
                                                )
                                            }
                                            !isScheduled -> {
                                                Text(
                                                    "-",
                                                    color = Color(0x33FFFFFF),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            isFuture -> {
                                                Box(
                                                    modifier = Modifier
                                                        .size(3.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0x4038BDF8))
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }
                        HorizontalDivider(color = Color(0x14FFFFFF))
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}
