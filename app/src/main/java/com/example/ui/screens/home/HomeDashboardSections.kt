package com.example.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 1. Today Overview Card (Design 5 / iOS 27 Liquid Glass)
 * Matches Screen 4 of the reference image:
 * Header "Today's Progress", left circular ring with percentage,
 * right stats: Completed X/Y, Focus Xh Ym, Water X/Y L.
 */
@Composable
fun TodayOverviewCard(
    user: UserAccount?,
    disciplineScore: Int,
    completedCount: Int,
    totalCount: Int,
    completionPercentage: Int,
    todayFocusMinutes: Int = 0,
    currentWaterMl: Int = 0,
    targetWaterMl: Int = 0,
    isReducedMotion: Boolean = false,
    onOpenHabits: () -> Unit = {},
    onOpenFocus: () -> Unit = {},
    onOpenWater: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animatedPercentage by animateIntAsState(
        targetValue = completionPercentage,
        animationSpec = if (isReducedMotion) tween(0) else tween(800, easing = FastOutSlowInEasing),
        label = "todayCompletionPct"
    )

    val focusHours = todayFocusMinutes / 60
    val focusMins = todayFocusMinutes % 60
    val focusFormatted = if (focusHours > 0) "${focusHours}h ${focusMins}m" else "${focusMins}m"

    val currentWaterL = currentWaterMl / 1000f
    val targetWaterL = if (targetWaterMl > 0) targetWaterMl / 1000f else 3.0f
    val waterFormatted = String.format(Locale.getDefault(), "%.1f / %.1f L", currentWaterL, targetWaterL)

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_today_overview_card"),
        backgroundColor = Color(0x99101728),
        borderColor = Color(0x40FFFFFF)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: "Today's Progress"
            Text(
                text = "Today's Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 17.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Glowing Circular Progress Ring with "78%"
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DisciplineScoreRing(
                        score = animatedPercentage,
                        size = 96.dp,
                        strokeWidth = 9.dp,
                        animated = !isReducedMotion
                    )
                }

                // Right: 3 Metric Stats Rows
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Completed X / Y
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenHabits() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x3322C55E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "$completedCount / $totalCount",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 13.5.sp
                            )
                        }
                    }

                    // Row 2: Focus Time
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenFocus() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x33A855F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Focus",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = focusFormatted,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 13.5.sp
                            )
                        }
                    }

                    // Row 3: Water
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenWater() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Water",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = waterFormatted,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4 Quick Metric Glass Cards in a row (Design 5 Screen 4):
 * Habits (7/9), Focus (2h 30m), Water (2.2 L), Streak (12 days)
 */
@Composable
fun FourQuickMetricsRow(
    completedCount: Int,
    totalCount: Int,
    todayFocusMinutes: Int,
    currentWaterMl: Int,
    streakDays: Int,
    onOpenHabits: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenWater: () -> Unit,
    isReducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val focusHours = todayFocusMinutes / 60
    val focusMins = todayFocusMinutes % 60
    val focusFormatted = if (focusHours > 0) "${focusHours}h ${focusMins}m" else "${focusMins}m"
    val waterL = currentWaterMl / 1000f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_four_quick_metrics_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Metric 1: Habits
        QuickMetricPillCard(
            title = "Habits",
            value = "$completedCount/$totalCount",
            icon = Icons.Filled.CheckCircle,
            tint = NeonGreen,
            containerColor = Color(0x2622C55E),
            modifier = Modifier.weight(1f),
            isReducedMotion = isReducedMotion,
            onClick = onOpenHabits
        )

        // Metric 2: Focus
        QuickMetricPillCard(
            title = "Focus",
            value = focusFormatted,
            icon = Icons.Filled.Timer,
            tint = NeonPurple,
            containerColor = Color(0x26A855F7),
            modifier = Modifier.weight(1f),
            isReducedMotion = isReducedMotion,
            onClick = onOpenFocus
        )

        // Metric 3: Water
        QuickMetricPillCard(
            title = "Water",
            value = String.format(Locale.getDefault(), "%.1f L", waterL),
            icon = Icons.Filled.WaterDrop,
            tint = NeonCyan,
            containerColor = Color(0x2606B6D4),
            modifier = Modifier.weight(1f),
            isReducedMotion = isReducedMotion,
            onClick = onOpenWater
        )

        // Metric 4: Streak
        QuickMetricPillCard(
            title = "Streak",
            value = "$streakDays days",
            icon = Icons.Filled.LocalFireDepartment,
            tint = StreakAmber,
            containerColor = Color(0x26F59E0B),
            modifier = Modifier.weight(1f),
            isReducedMotion = isReducedMotion,
            onClick = onOpenHabits
        )
    }
}

@Composable
private fun QuickMetricPillCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    isReducedMotion: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(84.dp)
            .tactilePress(isReducedMotion = isReducedMotion) { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = Color(0x8C11182B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x38FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 10.5.sp
            )

            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 11.5.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * Next Reminder Card (Design 5 Screen 4)
 * "Next Reminder" "Workout 7:30 PM" with badge and chevron >
 */
@Composable
fun NextReminderGlassCard(
    reminderTitle: String = "Workout",
    reminderTime: String = "7:30 PM",
    icon: ImageVector = Icons.Filled.FitnessCenter,
    badgeColor: Color = Color(0xFF6366F1),
    onClick: () -> Unit,
    isReducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .tactilePress(isReducedMotion = isReducedMotion) { onClick() }
            .testTag("home_next_reminder_card"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0x8C11182B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x38FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor.copy(alpha = 0.25f))
                        .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "Next Reminder",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "$reminderTitle $reminderTime",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Open Reminders",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 8. Quick Actions Bar
 * Compact 4-pill shortcut area for commonly used app actions.
 */
@Composable
fun QuickActionsRow(
    onAddHabit: () -> Unit,
    onAddWater: () -> Unit,
    onStartFocus: () -> Unit,
    onNightReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_quick_actions_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionButton(
            label = "+ Habit",
            icon = Icons.Filled.Add,
            accentColor = NeonGreen,
            containerColor = Color(0x3322C55E),
            modifier = Modifier.weight(1f),
            testTag = "quick_action_add_habit",
            onClick = onAddHabit
        )

        QuickActionButton(
            label = "+250ml",
            icon = Icons.Filled.WaterDrop,
            accentColor = NeonCyan,
            containerColor = Color(0x3338BDF8),
            modifier = Modifier.weight(1f),
            testTag = "quick_action_add_water",
            onClick = onAddWater
        )

        QuickActionButton(
            label = "Focus",
            icon = Icons.Filled.Timer,
            accentColor = NeonPurple,
            containerColor = Color(0x33A855F7),
            modifier = Modifier.weight(1f),
            testTag = "quick_action_focus",
            onClick = onStartFocus
        )

        QuickActionButton(
            label = "Review",
            icon = Icons.Filled.Bedtime,
            accentColor = NeonAmber,
            containerColor = Color(0x33FBBF24),
            modifier = Modifier.weight(1f),
            testTag = "quick_action_night_review",
            onClick = onNightReview
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .tactilePress { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = Color(0x80131C2E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x38FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * 3 & 4. Water & Focus Dual Cards Row
 * Compact 2-column layout showing real water progress and focus stats.
 */
@Composable
fun WaterAndFocusRow(
    currentWaterMl: Int,
    targetWaterMl: Int,
    onAddWater: (Int) -> Unit,
    onOpenWaterTracker: () -> Unit,
    todayFocusMinutes: Int,
    todayFocusSessionCount: Int,
    isTimerRunning: Boolean,
    timerRemainingSeconds: Int,
    onOpenFocus: () -> Unit,
    isReducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_water_focus_row"),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Compact Water Card
        CompactWaterCard(
            currentMl = currentWaterMl,
            targetMl = targetWaterMl,
            onAddWater = onAddWater,
            onOpenWaterTracker = onOpenWaterTracker,
            isReducedMotion = isReducedMotion,
            modifier = Modifier.weight(1f)
        )

        // Compact Focus Card
        CompactFocusCard(
            todayMinutes = todayFocusMinutes,
            sessionCount = todayFocusSessionCount,
            isTimerRunning = isTimerRunning,
            timerRemainingSeconds = timerRemainingSeconds,
            onOpenFocus = onOpenFocus,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun CompactWaterCard(
    currentMl: Int,
    targetMl: Int,
    onAddWater: (Int) -> Unit,
    onOpenWaterTracker: () -> Unit,
    isReducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hasGoal = targetMl > 0
    val waterPercent = if (hasGoal) minOf(100, ((currentMl.toDouble() / targetMl.toDouble()) * 100.0).roundToInt()) else 0
    val rawProgress = if (hasGoal) (waterPercent / 100f).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = if (isReducedMotion) tween(0) else tween(700),
        label = "waterProgressPct"
    )
    val currentLitres = String.format(java.util.Locale.US, "%.1f", currentMl / 1000f)
    val targetLitres = if (hasGoal) {
        if (targetMl % 1000 == 0) "${targetMl / 1000}.0" else String.format(java.util.Locale.US, "%.1f", targetMl / 1000f)
    } else ""

    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0x59FFFFFF), Color(0x26FFFFFF), Color(0x0DFFFFFF))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpenWaterTracker() }
            .testTag("home_compact_water_card"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0x8C141C2E)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x14FFFFFF), Color.Transparent)
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x3338BDF8))
                            .border(1.dp, Color(0x4D38BDF8), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WaterDrop,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "WATER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp
                    )
                }

                // Quick Add +250ml
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x3306B6D4))
                        .clickable { onAddWater(250) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("home_water_quick_250")
                ) {
                    Text(
                        text = "+250",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "$currentLitres L",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (hasGoal) {
                Text(
                    text = "Goal: $targetLitres L ($waterPercent%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            } else {
                Text(
                    text = "Tap to set goal",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Linear Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x26FFFFFF))
            ) {
                if (hasGoal) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (waterPercent >= 100) NeonGreen else NeonCyan)
                    )
                }
            }
        }
    }
}

@Composable
fun CompactFocusCard(
    todayMinutes: Int,
    sessionCount: Int,
    isTimerRunning: Boolean,
    timerRemainingSeconds: Int,
    onOpenFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    if (isTimerRunning) {
                        listOf(Color(0x80A855F7), Color(0x33A855F7), Color(0x1AA855F7))
                    } else {
                        listOf(Color(0x59FFFFFF), Color(0x26FFFFFF), Color(0x0DFFFFFF))
                    }
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpenFocus() }
            .testTag("home_compact_focus_card"),
        shape = RoundedCornerShape(20.dp),
        color = if (isTimerRunning) Color(0x8C1E1430) else Color(0x8C141C2E)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x14FFFFFF), Color.Transparent)
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33A855F7))
                            .border(1.dp, Color(0x4DA855F7), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "FOCUS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp
                    )
                }

                if (isTimerRunning) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x3322C55E))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreenBright,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isTimerRunning) {
                val mins = timerRemainingSeconds / 60
                val secs = timerRemainingSeconds % 60
                Text(
                    text = String.format(java.util.Locale.US, "%02d:%02d", mins, secs),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeonPurple
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Session active ⏳",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonGreenBright,
                    fontSize = 11.sp
                )
            } else {
                Text(
                    text = "${todayMinutes}m",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (sessionCount > 0) "$sessionCount session${if (sessionCount != 1) "s" else ""}" else "Tap to start flow",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x26FFFFFF))
            ) {
                if (todayMinutes > 0 || isTimerRunning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (isTimerRunning) 1f else (todayMinutes / 60f).coerceIn(0.15f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(NeonPurple)
                    )
                }
            }
        }
    }
}

/**
 * 5. Night Review Card
 * Displays real Night Review status, rating, or prompt to complete reflection.
 */
@Composable
fun NightReviewCard(
    todayReview: NightReviewEntry?,
    onOpenNightReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = todayReview != null

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpenNightReview() }
            .border(
                1.dp,
                Brush.horizontalGradient(
                    if (isCompleted) {
                        listOf(Color(0x5922C55E), Color(0x33A855F7))
                    } else {
                        listOf(Color(0x59A855F7), Color(0x2638BDF8))
                    }
                ),
                RoundedCornerShape(20.dp)
            )
            .testTag("home_night_review_card"),
        color = Color(0x9917122B)
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
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isCompleted) Color(0x3322C55E) else Color(0x33A855F7))
                        .border(
                            1.dp,
                            if (isCompleted) Color(0x4D22C55E) else Color(0x4DA855F7),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Bedtime,
                        contentDescription = null,
                        tint = if (isCompleted) NeonGreenBright else NeonPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isCompleted) "Night Review Complete ✨" else "Night Review & Reflection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        if (isCompleted) {
                            Text(
                                text = "★ ${todayReview.rating}/5",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonAmber,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (isCompleted) {
                            if (todayReview.wins.isNotBlank()) {
                                "Win: ${todayReview.wins.take(48)}${if (todayReview.wins.length > 48) "..." else ""}"
                            } else {
                                "Discipline scored & daily reflection saved."
                            }
                        } else {
                            "Score discipline, record daily wins & plan tomorrow"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Open",
                tint = if (isCompleted) NeonGreenBright else NeonPurple,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 6. Achievements + Discipline Breakdown Card
 * Displays real Discipline Score, score breakdown dimensions, and latest unlocked achievement.
 */
@Composable
fun DisciplineAchievementsCard(
    breakdown: DisciplineScoreBreakdown,
    achievements: List<Achievement>,
    onOpenProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unlockedList = remember(achievements) {
        achievements.filter { it.isUnlocked }.sortedByDescending { it.unlockedAt ?: 0L }
    }
    val latestUnlocked = unlockedList.firstOrNull()
    val nextMilestone = remember(achievements) {
        achievements.firstOrNull { !it.isUnlocked }
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_discipline_achievements_card"),
        onClick = onOpenProgress
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x3322C55E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = NeonGreenBright,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "DISCIPLINE & ACHIEVEMENTS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp,
                    fontSize = 11.sp
                )
            }

            Text(
                text = "View Breakdown →",
                style = MaterialTheme.typography.labelSmall,
                color = NeonGreenBright,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mini Discipline Breakdown Row (Habits, Streak, Water, Focus, Review)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BreakdownMetricItem(label = "Habits", score = breakdown.habitsScore, max = 30, color = NeonGreen)
            BreakdownMetricItem(label = "Streak", score = breakdown.streakScore, max = 20, color = NeonPurple)
            BreakdownMetricItem(label = "Water", score = breakdown.waterScore, max = 15, color = NeonCyan)
            BreakdownMetricItem(label = "Focus", score = breakdown.focusScore, max = 20, color = NeonPurple)
            BreakdownMetricItem(label = "Review", score = breakdown.reviewScore, max = 15, color = NeonAmber)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Highlight recent unlocked achievement or next goal
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0x331E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (latestUnlocked != null) Color(0x33FBBF24) else Color(0x26FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (latestUnlocked != null) Icons.Filled.MilitaryTech else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = if (latestUnlocked != null) NeonAmber else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = latestUnlocked?.title ?: (nextMilestone?.title ?: "Forge Your First Badge"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (latestUnlocked != null) "Unlocked achievement" else "Upcoming milestone",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (latestUnlocked != null) NeonAmber else TextMuted,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x26FFFFFF)
                ) {
                    Text(
                        text = "${unlockedList.size}/${achievements.size} Badges",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakdownMetricItem(
    label: String,
    score: Int,
    max: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$score/$max",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontSize = 10.sp
        )
    }
}

/**
 * 9. Progress Snapshot Card
 * Compact summary of recent completion trends, water, focus, and streak.
 */
@Composable
fun ProgressSnapshotCard(
    sevenDayCompletionRate: Int,
    waterProgressPercent: Int,
    todayFocusMinutes: Int,
    currentStreak: Int,
    onOpenProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_progress_snapshot_card"),
        onClick = onOpenProgress
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x3338BDF8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.TrendingUp,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "PROGRESS SNAPSHOT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp,
                    fontSize = 11.sp
                )
            }

            Text(
                text = "Full Analytics →",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SnapshotStatColumn(
                value = "$sevenDayCompletionRate%",
                label = "7-Day Rate",
                accentColor = NeonGreenBright
            )
            SnapshotStatColumn(
                value = "$waterProgressPercent%",
                label = "Water Goal",
                accentColor = NeonCyan
            )
            SnapshotStatColumn(
                value = "${todayFocusMinutes}m",
                label = "Focus Today",
                accentColor = NeonPurple
            )
            SnapshotStatColumn(
                value = "${currentStreak}d",
                label = "Streak",
                accentColor = NeonAmber
            )
        }
    }
}

/**
 * 9. Wake-Up Challenge Alarm Dashboard Card
 * Displays next scheduled wake-up challenge alarm with direct toggle and management shortcut.
 */
@Composable
fun WakeUpAlarmDashboardCard(
    alarms: List<ChallengeAlarm>,
    onToggleAlarm: (String, Boolean) -> Unit,
    onManageAlarms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nextAlarm = remember(alarms) {
        alarms.firstOrNull { it.isEnabled } ?: alarms.firstOrNull()
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onManageAlarms() }
            .testTag("home_wakeup_alarm_card")
    ) {
        if (nextAlarm != null) {
            val formattedTime = remember(nextAlarm.time) {
                try {
                    val parts = nextAlarm.time.split(":")
                    val h = parts[0].toInt()
                    val m = parts[1].toInt()
                    java.time.LocalTime.of(h, m).format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))
                } catch (e: Exception) {
                    nextAlarm.time
                }
            }

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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0x3322C55E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Alarm,
                                contentDescription = null,
                                tint = NeonGreenBright,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Wake-Up Challenge ⚔️",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = nextAlarm.name.ifBlank { "Morning Warrior" },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = nextAlarm.isEnabled,
                        onCheckedChange = { isChecked ->
                            onToggleAlarm(nextAlarm.id, isChecked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonGreenBright,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = if (nextAlarm.isEnabled) NeonGreenBright else TextMuted
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                CategoryIcon(iconName = nextAlarm.challengeType.iconName, tint = NeonGreenBright, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "${nextAlarm.challengeType.displayName} • ${nextAlarm.target} ${nextAlarm.challengeType.unit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonGreenBright,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Sleek Alarm button in Black Glass aesthetic
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onManageAlarms() }
                                .testTag("home_edit_alarm_button"),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x3322C55E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6622C55E))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Alarm,
                                    contentDescription = null,
                                    tint = NeonGreenBright,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Alarm",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonGreenBright,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x2622C55E))
                            .border(1.dp, Color(0x3322C55E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AlarmAdd,
                            contentDescription = null,
                            tint = NeonGreenBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Wake-Up Challenge ⚔️",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Wake up with reps & discipline",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Black Glass Styled 'Alarm' Pill Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onManageAlarms() }
                        .testTag("home_wakeup_alarm_button"),
                    shape = RoundedCornerShape(16.dp),
                    color = NeonGreenBright,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Alarm,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Alarm",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SnapshotStatColumn(
    value: String,
    label: String,
    accentColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontSize = 10.sp
        )
    }
}

@Composable
fun DayForgeDailyHomeCard(
    featuredContent: com.example.data.models.DisciplineContent?,
    onOpenDiscover: () -> Unit,
    modifier: Modifier = Modifier,
    isReducedMotion: Boolean = false
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color(0x5900F0FF), Color(0x2638BDF8), Color(0x0DFFFFFF))
                ),
                RoundedCornerShape(22.dp)
            )
            .clickable { onOpenDiscover() }
            .testTag("home_dayforge_daily_card"),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xCC0E1526)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: "FOR YOU" / "DAYFORGE DAILY" + "Open Discover →"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0x3300F0FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "DAYFORGE DAILY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF00F0FF),
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Open Discover",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreenBright,
                        fontSize = 12.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = NeonGreenBright,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val title = featuredContent?.title ?: "The 5-Second Rule for Instant Action"
            val category = featuredContent?.category ?: "DISCIPLINE"
            val desc = featuredContent?.description ?: "Action precedes motivation. Move before your brain makes an excuse."
            val duration = featuredContent?.duration ?: "1 min read"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val thumbUrl = featuredContent?.thumbnailUrl?.ifBlank { featuredContent.mediaUrl }
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    color = Color(0x331E293B),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (!thumbUrl.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = thumbUrl,
                            contentDescription = title,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Filled.Bolt, null, tint = Color(0xFF00F0FF), modifier = Modifier.size(24.dp))
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x3300F0FF)
                        ) {
                            Text(
                                text = category.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00F0FF),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "• $duration",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

