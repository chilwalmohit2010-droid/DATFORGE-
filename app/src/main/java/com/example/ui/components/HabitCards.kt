package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CompletionStatus
import com.example.data.models.Habit
import com.example.data.models.HabitCompletion
import com.example.data.models.HabitDifficulty
import com.example.data.models.HabitGoalType
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun HabitItemCard(
    habit: Habit,
    isCompletedToday: Boolean,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onPauseToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showStreakOnRight: Boolean = false,
    streakCount: Int = 0
) {
    var showMenu by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile iOS Card spring scale
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "habitCardScale"
    )

    // Animated Card Background & Border transitions
    val cardBgColor by animateColorAsState(
        targetValue = if (isCompletedToday) Color(0x8A122426) else Color(0x80131C2E),
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "cardBgColor"
    )

    // Category badge color calculation based on icon or category name
    val (badgeBg, badgeTint) = when (habit.iconName.lowercase()) {
        "fitness_center", "fitness" -> Pair(Color(0x33A855F7), NeonPurple)
        "water_drop", "water" -> Pair(Color(0x3338BDF8), NeonCyan)
        "menu_book", "learning" -> Pair(Color(0x3322C55E), NeonGreenBright)
        "bolt", "productivity" -> Pair(Color(0x33A855F7), NeonPurple)
        "bedtime", "sleep" -> Pair(Color(0x33FBBF24), NeonAmber)
        else -> Pair(Color(0x3322C55E), Color(habit.colorHex))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = if (isCompletedToday) {
                        listOf(Color(0x8022C55E), Color(0x3322C55E), Color(0x1A22C55E))
                    } else {
                        listOf(Color(0x59FFFFFF), Color(0x26FFFFFF), Color(0x0DFFFFFF))
                    }
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onToggleComplete() }
            .testTag("habit_card_${habit.id}"),
        shape = RoundedCornerShape(20.dp),
        color = cardBgColor,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x14FFFFFF),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon in glowing squircle container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(badgeBg)
                    .border(1.dp, badgeTint.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    iconName = habit.iconName,
                    tint = if (habit.isPaused) TextMuted else badgeTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Habit Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (habit.isPaused) TextMuted
                        else if (isCompletedToday) TextSecondary
                        else TextPrimary,
                        textDecoration = if (isCompletedToday) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (habit.isPaused) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x3364748B)
                        ) {
                            Text(
                                text = "PAUSED",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Schedule, Difficulty, Duration/Goal & Reminder tags
                val scheduleText = when (habit.repeatDays.trim()) {
                    "1,2,3,4,5,6,7", "" -> "Daily"
                    "1,2,3,4,5" -> "Mon-Fri"
                    "6,7" -> "Sat-Sun"
                    else -> "${habit.getRepeatDaysList().size}d/wk"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = habit.category.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )

                    Text(
                        text = scheduleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )

                    // Goal / Duration Badge if applicable
                    if (habit.durationMinutes > 0) {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = Color(0x2638BDF8)
                        ) {
                            Text(
                                text = "${habit.durationMinutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (habit.goalValue > 1 || (habit.goalUnit != "times" && habit.goalUnit.isNotBlank())) {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = Color(0x2638BDF8)
                        ) {
                            Text(
                                text = "${habit.goalValue} ${habit.goalUnit}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Difficulty indicator
                    val (diffColor, diffBg) = when (habit.difficulty) {
                        HabitDifficulty.EASY -> Pair(NeonGreenBright, Color(0x2622C55E))
                        HabitDifficulty.MEDIUM -> Pair(NeonAmber, Color(0x26FBBF24))
                        HabitDifficulty.HARD -> Pair(CoralRed, Color(0x26EF4444))
                    }
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = diffBg
                    ) {
                        Text(
                            text = habit.difficulty.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = diffColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Notes Indicator Icon
                    if (habit.notes.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Outlined.StickyNote2,
                            contentDescription = "Notes attached",
                            tint = CyanAccent,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    // Clear Reminder Status Pill on Habit Card
                    val hasReminder = !habit.reminderTime.isNullOrBlank() && !habit.isPaused
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (hasReminder) Color(0x2606B6D4) else Color(0x1464748B))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Icon(
                            imageVector = if (hasReminder) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsOff,
                            contentDescription = if (hasReminder) "Reminder active at ${habit.reminderTime}" else "Reminder off",
                            tint = if (hasReminder) CyanAccent else TextMuted,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = if (hasReminder) habit.reminderTime!! else "Off",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasReminder) CyanAccent else TextMuted,
                            fontSize = 9.5.sp,
                            fontWeight = if (hasReminder) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (showStreakOnRight) {
                // Streak Counter layout with subtle spring value
                val animatedStreak by androidx.compose.animation.core.animateIntAsState(
                    targetValue = streakCount,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    ),
                    label = "streakCounterAnim"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "$animatedStreak",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            } else {
                // Square Glass Checkbox with tactile spring animation
                val checkScale by animateFloatAsState(
                    targetValue = if (isCompletedToday) 1f else 0.3f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    ),
                    label = "checkScale"
                )
                val checkAlpha by animateFloatAsState(
                    targetValue = if (isCompletedToday) 1f else 0f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
                    label = "checkAlpha"
                )
                val checkboxBgColor by animateColorAsState(
                    targetValue = if (isCompletedToday) NeonGreen else Color(0x1AFFFFFF),
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    ),
                    label = "checkboxBg"
                )
                val checkboxBorderColor by animateColorAsState(
                    targetValue = if (isCompletedToday) NeonGreenBright else Color(0x59FFFFFF),
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    ),
                    label = "checkboxBorder"
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(checkboxBgColor)
                        .border(
                            width = 1.5.dp,
                            color = checkboxBorderColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onToggleComplete() }
                        .testTag("toggle_habit_${habit.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (checkAlpha > 0.05f) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Completed",
                            tint = Color.Black,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer {
                                    scaleX = checkScale
                                    scaleY = checkScale
                                    alpha = checkAlpha
                                }
                        )
                    }
                }
            }

            // Options Overflow Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Habit") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (habit.isPaused) "Resume Habit" else "Pause Habit") },
                        onClick = {
                            showMenu = false
                            onPauseToggle()
                        },
                        leadingIcon = {
                            Icon(
                                if (habit.isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                                null
                            )
                        }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Delete Habit", color = CoralRed) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = CoralRed) }
                    )
                }
            }
        }
    }
}

@Composable
fun WaterTrackerCard(
    currentMl: Int,
    targetMl: Int,
    onAddWater: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onOpenWaterTracker: () -> Unit = {}
) {
    val hasGoal = targetMl > 0
    val waterPercent = if (hasGoal) {
        minOf(100, ((currentMl.toDouble() / targetMl.toDouble()) * 100.0).roundToInt().coerceAtLeast(0))
    } else {
        0
    }
    val progress = if (hasGoal) (waterPercent / 100f).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "waterProgress"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x59FFFFFF),
                        Color(0x26FFFFFF),
                        Color(0x0DFFFFFF)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clip(RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = Color(0x8C141C2E),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x14FFFFFF),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x3338BDF8))
                            .border(1.dp, Color(0x4D38BDF8), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WaterDrop,
                            contentDescription = "Water",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "WATER INTAKE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format("%.1f L", currentMl / 1000f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Quick add buttons (+250ml & +500ml)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x26FFFFFF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x38FFFFFF)),
                        modifier = Modifier
                            .clickable { onAddWater(250) }
                            .testTag("add_water_250")
                    ) {
                        Text(
                            text = "+250ml",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x3322C55E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6622C55E)),
                        modifier = Modifier
                            .clickable { onAddWater(500) }
                            .testTag("add_water_500")
                    ) {
                        Text(
                            text = "+500ml",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreenBright,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar and percentage
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = NeonCyan,
                trackColor = Color(0x26FFFFFF)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasGoal) {
                    val targetL = if (targetMl % 1000 == 0) "${targetMl / 1000}.0" else String.format(java.util.Locale.US, "%.1f", targetMl / 1000f)
                    Text(
                        text = "$waterPercent% of $targetL L",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x3306B6D4))
                            .border(1.dp, Color(0x6606B6D4), RoundedCornerShape(6.dp))
                            .clickable { onOpenWaterTracker() }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
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
                            color = NeonCyan,
                            fontSize = 11.sp
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.WaterDrop,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

