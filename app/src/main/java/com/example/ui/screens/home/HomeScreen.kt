package com.example.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CompletionStatus
import com.example.data.models.Habit
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: DisciplineViewModel,
    onNavigateTab: (NavTab) -> Unit,
    onOpenAddHabit: () -> Unit,
    onOpenMonthlyTracker: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onOpenWaterTracker: () -> Unit = {},
    onOpenReminders: () -> Unit = {},
    onOpenNightReview: () -> Unit = {},
    onOpenNotifications: () -> Unit = onOpenReminders,
    onOpenAlarms: () -> Unit = {},
    onOpenDiscover: () -> Unit = {}
) {
    // Real-time state flows
    val currentUser by viewModel.currentUser.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    val allAlarms by viewModel.allAlarms.collectAsState()
    val todayCompletions by viewModel.todayCompletions.collectAsState()
    val allCompletions by viewModel.allCompletions.collectAsState()
    val waterRecord by viewModel.todayWaterRecord.collectAsState()
    val userWaterGoal by viewModel.userWaterGoal.collectAsState()
    val todayFocusMinutes by viewModel.todayFocusMinutes.collectAsState()
    val todayFocusSessions by viewModel.todayFocusSessions.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val timerRemainingSeconds by viewModel.timerRemainingSeconds.collectAsState()
    val todayNightReview by viewModel.todayNightReview.collectAsState()
    val disciplineScoreBreakdown by viewModel.disciplineScoreBreakdown.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val unreadNotifsCount by viewModel.unreadNotificationsCount.collectAsState()
    val isReducedMotion by viewModel.isReducedMotionEnabled.collectAsState()
    val contentItems by viewModel.contentItems.collectAsState()

    val today = remember { LocalDate.now() }
    val todayDayOfWeek = today.dayOfWeek.value
    val todayFormattedDate = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    }

    // Dynamic Time-Based Greeting
    val greetingTitle = remember(currentUser) {
        val hour = LocalTime.now().hour
        val timeGreeting = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
        val name = currentUser?.displayName?.trim()?.ifBlank { null }
            ?: currentUser?.username?.trim()?.ifBlank { null }
            ?: "Champion"
        "$timeGreeting, $name 👋"
    }

    // Effective Water Target
    val effectiveWaterTarget = remember(waterRecord, userWaterGoal) {
        if ((waterRecord?.targetMl ?: 0) > 0) {
            waterRecord!!.targetMl
        } else if ((userWaterGoal ?: 0) > 0) {
            userWaterGoal!!
        } else {
            3000
        }
    }

    // Filter today's active habits
    val todayScheduledHabits = remember(allHabits, todayDayOfWeek) {
        allHabits.filter { !it.isPaused && it.isScheduledForDayOfWeek(todayDayOfWeek) }
    }

    val completedHabitIds = remember(todayCompletions) {
        todayCompletions
            .filter { it.status == CompletionStatus.COMPLETED }
            .map { it.habitId }
            .toSet()
    }

    val completedCount = todayScheduledHabits.count { completedHabitIds.contains(it.id) }
    val totalCount = todayScheduledHabits.size
    val completionPercentage = if (totalCount > 0) {
        ((completedCount.toFloat() / totalCount.toFloat()) * 100).toInt()
    } else 0

    // Real past 7-day completion rate calculated from user's history
    val past7DayRate = remember(allCompletions, allHabits, today) {
        var scheduledTotal = 0
        var completedTotal = 0
        val completionLookup = allCompletions
            .filter { it.status == CompletionStatus.COMPLETED }
            .map { "${it.habitId}_${it.date}" }
            .toSet()

        for (i in 0..6) {
            val d = today.minusDays(i.toLong())
            val isoD = d.dayOfWeek.value
            val scheduledForDay = allHabits.filter { !it.isPaused && it.isScheduledForDayOfWeek(isoD) }
            scheduledTotal += scheduledForDay.size
            val dStr = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
            completedTotal += scheduledForDay.count { habit ->
                completionLookup.contains("${habit.id}_$dStr")
            }
        }
        if (scheduledTotal > 0) ((completedTotal.toFloat() / scheduledTotal.toFloat()) * 100).toInt() else 0
    }

    val waterProgressPercent = remember(waterRecord, effectiveWaterTarget) {
        val current = waterRecord?.currentMl ?: 0
        if (effectiveWaterTarget > 0) {
            ((current.toFloat() / effectiveWaterTarget.toFloat()) * 100).toInt().coerceAtMost(100)
        } else 0
    }

    val activeDisciplineScore = currentUser?.disciplineScore ?: disciplineScoreBreakdown.score

    LiquidGlassBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_column"),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1 & 7. Top Header: Menu icon, Greeting, Subtitle, Bell with badge, and User Avatar (Screen 4)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Menu Icon on Left (Screen 4)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x261E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .tactilePress(scaleDown = 0.90f) { onNavigateTab(NavTab.PROFILE) }
                            .testTag("home_menu_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Title & Subtitle (Screen 4)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greetingTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.5.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "Discipline today, Freedom tomorrow.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Smart Notification Bell Button with animated badge (Screen 4)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x261E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .tactilePress(scaleDown = 0.90f) { onOpenNotifications() }
                            .testTag("home_notification_bell_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications & Reminders",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )

                        if (unreadNotifsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 4.dp, end = 4.dp)
                                    .size(if (unreadNotifsCount > 9) 16.dp else 10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                                    .border(1.5.dp, Color(0xFF0F172A), CircleShape)
                                    .testTag("home_notification_red_badge"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (unreadNotifsCount in 1..99 && unreadNotifsCount > 9) {
                                    Text(
                                        text = "$unreadNotifsCount",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 8.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Circular User Avatar (Screen 4)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color(0xFF00F0FF), CircleShape)
                            .clickable { onNavigateTab(NavTab.PROFILE) }
                            .testTag("home_user_avatar_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        val photo = currentUser?.photoUrl
                        if (!photo.isNullOrBlank()) {
                            coil.compose.AsyncImage(
                                model = photo,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.avatar_discipline_default),
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // 1. TODAY OVERVIEW CARD (Design 5 Screen 4)
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    TodayOverviewCard(
                        user = currentUser,
                        disciplineScore = activeDisciplineScore,
                        completedCount = completedCount,
                        totalCount = totalCount,
                        completionPercentage = completionPercentage,
                        todayFocusMinutes = todayFocusMinutes,
                        currentWaterMl = waterRecord?.currentMl ?: 0,
                        targetWaterMl = effectiveWaterTarget,
                        isReducedMotion = isReducedMotion,
                        onOpenHabits = { onNavigateTab(NavTab.HABITS) },
                        onOpenFocus = { onNavigateTab(NavTab.FOCUS) },
                        onOpenWater = onOpenWaterTracker
                    )
                }
            }

            // 1b. FOUR QUICK METRICS ROW (Design 5 Screen 4)
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    FourQuickMetricsRow(
                        completedCount = completedCount,
                        totalCount = totalCount,
                        todayFocusMinutes = todayFocusMinutes,
                        currentWaterMl = waterRecord?.currentMl ?: 0,
                        streakDays = currentUser?.currentStreak ?: 0,
                        onOpenHabits = { onNavigateTab(NavTab.HABITS) },
                        onOpenFocus = { onNavigateTab(NavTab.FOCUS) },
                        onOpenWater = onOpenWaterTracker,
                        isReducedMotion = isReducedMotion
                    )
                }
            }

            // 1c. NEXT REMINDER CARD (Design 5 Screen 4)
            item {
                val nextActiveAlarm = allAlarms.firstOrNull { it.isEnabled }
                val reminderTitle = nextActiveAlarm?.name ?: "Workout"
                val reminderTime = nextActiveAlarm?.time ?: "7:30 PM"
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    NextReminderGlassCard(
                        reminderTitle = reminderTitle,
                        reminderTime = reminderTime,
                        onClick = onOpenAlarms,
                        isReducedMotion = isReducedMotion
                    )
                }
            }

            // 1d. WAKE-UP CHALLENGE ALARM CARD
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    WakeUpAlarmDashboardCard(
                        alarms = allAlarms,
                        onToggleAlarm = { alarmId, isEnabled ->
                            viewModel.toggleAlarm(alarmId, isEnabled)
                        },
                        onManageAlarms = onOpenAlarms
                    )
                }
            }

            // 8. QUICK ACTIONS ROW
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    QuickActionsRow(
                        onAddHabit = onOpenAddHabit,
                        onAddWater = { viewModel.addWater(250) },
                        onStartFocus = { onNavigateTab(NavTab.FOCUS) },
                        onNightReview = onOpenNightReview
                    )
                }
            }

            // 3 & 4. WATER & FOCUS DUAL CARDS
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    WaterAndFocusRow(
                        currentWaterMl = waterRecord?.currentMl ?: 0,
                        targetWaterMl = effectiveWaterTarget,
                        onAddWater = { amount -> viewModel.addWater(amount) },
                        onOpenWaterTracker = onOpenWaterTracker,
                        todayFocusMinutes = todayFocusMinutes,
                        todayFocusSessionCount = todayFocusSessions.size,
                        isTimerRunning = isTimerRunning,
                        timerRemainingSeconds = timerRemainingSeconds,
                        onOpenFocus = { onNavigateTab(NavTab.FOCUS) },
                        isReducedMotion = isReducedMotion
                    )
                }
            }

            // 5. NIGHT REVIEW CARD
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    NightReviewCard(
                        todayReview = todayNightReview,
                        onOpenNightReview = onOpenNightReview
                    )
                }
            }

            // 5b. DAYFORGE DAILY / FOR YOU CONTENT CARD
            item {
                val featured = remember(contentItems) {
                    contentItems.firstOrNull { it.isFeatured } ?: contentItems.firstOrNull()
                }
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    DayForgeDailyHomeCard(
                        featuredContent = featured,
                        onOpenDiscover = onOpenDiscover,
                        isReducedMotion = isReducedMotion
                    )
                }
            }

            // 6. ACHIEVEMENTS + DISCIPLINE BREAKDOWN
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    DisciplineAchievementsCard(
                        breakdown = disciplineScoreBreakdown,
                        achievements = achievements,
                        onOpenProgress = { onNavigateTab(NavTab.PROGRESS) }
                    )
                }
            }

            // 9. PROGRESS SNAPSHOT
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    ProgressSnapshotCard(
                        sevenDayCompletionRate = past7DayRate,
                        waterProgressPercent = waterProgressPercent,
                        todayFocusMinutes = todayFocusMinutes,
                        currentStreak = currentUser?.currentStreak ?: 0,
                        onOpenProgress = { onNavigateTab(NavTab.PROGRESS) }
                    )
                }
            }

            // 2. TODAY'S HABITS SECTION HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Today's Habits",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        if (totalCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (completedCount == totalCount) Color(0x3322C55E) else Color(0x331E293B)
                            ) {
                                Text(
                                    text = "$completedCount/$totalCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (completedCount == totalCount) NeonGreenBright else TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonGreenBright,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onNavigateTab(NavTab.HABITS) }
                            .testTag("home_see_all_habits_link")
                    )
                }
            }

            // 2. TODAY'S HABITS LIST
            if (todayScheduledHabits.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x80131C2E)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x3322C55E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = NeonGreenBright,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "No habits scheduled for today",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Add a new habit or adjust your repeat schedule to forge discipline.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 12.sp
                            )
                            Button(
                                onClick = onOpenAddHabit,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("home_empty_create_habit_button")
                            ) {
                                Text("+ Create Habit", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(todayScheduledHabits, key = { it.id }) { habit ->
                    val isCompleted = completedHabitIds.contains(habit.id)
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        HabitItemCard(
                            habit = habit,
                            isCompletedToday = isCompleted,
                            onToggleComplete = { viewModel.toggleHabitCompletion(habit.id) },
                            onEdit = { onEditHabit(habit) },
                            onPauseToggle = { viewModel.toggleHabitPause(habit) },
                            onDelete = { viewModel.deleteHabit(habit) }
                        )
                    }
                }
            }

            // Watermark Footer
            item {
                Spacer(modifier = Modifier.height(10.dp))
                WatermarkFooter()
            }
        }
    }
}
