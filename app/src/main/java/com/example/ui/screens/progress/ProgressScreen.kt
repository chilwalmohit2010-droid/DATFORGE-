package com.example.ui.screens.progress

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.outlined.Lock
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
import com.example.ui.components.CommonTopBar
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.WatermarkFooter
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel
import java.time.LocalDate

@Composable
fun ProgressScreen(
    viewModel: DisciplineViewModel,
    onBack: (() -> Unit)? = null,
    onOpenLeaderboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    val allCompletions by viewModel.allCompletions.collectAsState()
    val waterHistory by viewModel.waterHistory.collectAsState()
    val userWaterGoal by viewModel.userWaterGoal.collectAsState()
    val allFocusSessions by viewModel.allFocusSessions.collectAsState()
    val allNightReviews by viewModel.allNightReviews.collectAsState()
    val disciplineBreakdown by viewModel.disciplineScoreBreakdown.collectAsState()
    val achievements by viewModel.achievements.collectAsState()

    var selectedPeriod by remember { mutableStateOf(AnalyticsPeriod.SEVEN_DAYS) }
    var showBreakdownDialog by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val defaultWaterGoal = userWaterGoal ?: 2500

    // Earliest date from user creation or first completion
    val earliestDate = remember(currentUser, allCompletions) {
        val firstCompletionDate = allCompletions.mapNotNull {
            try { LocalDate.parse(it.date) } catch (e: Exception) { null }
        }.minOrNull()
        firstCompletionDate ?: today.minusDays(30)
    }

    // Dates for selected period
    val periodDates = remember(selectedPeriod, today, earliestDate) {
        AnalyticsCalculator.getPeriodDates(selectedPeriod, today, earliestDate)
    }

    // Daily metrics across the selected period
    val dailyMetrics = remember(periodDates, allHabits, allCompletions, waterHistory, defaultWaterGoal, allFocusSessions, allNightReviews) {
        AnalyticsCalculator.computeDailyMetrics(
            dates = periodDates,
            habits = allHabits,
            completions = allCompletions,
            waterRecords = waterHistory,
            defaultWaterGoalMl = defaultWaterGoal,
            focusSessions = allFocusSessions,
            nightReviews = allNightReviews
        )
    }

    // Period overview stats (streaks, total, avg, consistency, water, focus, reviews, discipline)
    val periodOverviewStats = remember(currentUser, dailyMetrics, disciplineBreakdown.score) {
        AnalyticsCalculator.computePeriodOverview(currentUser, dailyMetrics, disciplineBreakdown.score)
    }

    // 90-day activity heatmap data
    val heatmapDays = remember(today, allHabits, allCompletions) {
        AnalyticsCalculator.computeHeatmapDays(today, allHabits, allCompletions)
    }

    // Habit breakdown stats
    val habitBreakdowns = remember(allHabits, allCompletions, periodDates, today) {
        AnalyticsCalculator.computeHabitBreakdowns(allHabits, allCompletions, periodDates, today)
    }

    // Behavioral insights
    val insights = remember(allHabits, allCompletions, dailyMetrics, habitBreakdowns, periodOverviewStats, today) {
        AnalyticsCalculator.computeInsights(
            habits = allHabits,
            completions = allCompletions,
            dailyMetrics = dailyMetrics,
            habitBreakdowns = habitBreakdowns,
            overview = periodOverviewStats,
            today = today
        )
    }

    LiquidGlassBackground {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("progress_screen_column"),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Top Bar: Back arrow and centered "Progress" (Screen 8)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x261E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onBack?.invoke() }
                            .testTag("progress_back_button"),
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
                        text = "Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x261E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onOpenLeaderboard() }
                            .testTag("progress_leaderboard_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = "Global Leaderboard",
                            tint = NeonAmber,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // Period Selector (Today, Week, Month, Year) - Screen 8
            item {
                PeriodSelectorBar(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { selectedPeriod = it },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            // Reference Style 1: Weekly Performance Bar Chart (Screen 8: Habit Completion & Focus Time)
            item {
                WeeklyPerformanceCard(
                    dailyMetrics = dailyMetrics,
                    overallConsistencyPercent = periodOverviewStats.habitConsistencyPercentage,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Reference Style 2: Bottom 3 Stats Row (Streak, Total Habits, Best Day) - Screen 8
            item {
                KeyMetrics2x2Grid(
                    totalHabits = allHabits.count { !it.isPaused },
                    completionRate = periodOverviewStats.habitConsistencyPercentage,
                    bestStreak = periodOverviewStats.bestStreak,
                    waterAvgL = if (defaultWaterGoal > 0) defaultWaterGoal / 1000f else 2.5f,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // 1. Dynamic Discipline Score Hero Card
            item {
                DisciplineScoreHeroCard(
                    breakdown = disciplineBreakdown,
                    onOpenBreakdown = { showBreakdownDialog = true }
                )
            }

            // 3. Real Performance Overview Grid (Streaks, Completions, Average, Consistency, Water, Focus, Reviews)
            item {
                OverviewMetricsGrid(
                    stats = periodOverviewStats,
                    period = selectedPeriod,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // 4. Data Trends & Charts (Habits, Water, Focus, Consistency)
            item {
                AnalyticsChartsCard(
                    dailyMetrics = dailyMetrics,
                    period = selectedPeriod,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // 5. 90-Day GitHub-style Activity Heatmap
            item {
                HeatmapCard(
                    heatmapDays = heatmapDays,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // 6. Data-Based Behavioral Insights
            item {
                InsightsCard(
                    insights = insights,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // 7. Habit Breakdown Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Habit Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Streaks & consistency for active habits (${selectedPeriod.label})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x26FFFFFF)
                    ) {
                        Text(
                            text = "${habitBreakdowns.size} Active",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (habitBreakdowns.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x4D0B101D),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1FFFFFFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "No active habits found. Create a habit to start tracking streaks and analytics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(habitBreakdowns, key = { it.habitId }) { habitStats ->
                    HabitBreakdownItemCard(
                        habitStats = habitStats,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }

            // 8. Achievements Showcase
            item {
                AchievementsShowcaseCard(
                    achievements = achievements,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }

            // Watermark Footer
            item {
                WatermarkFooter()
            }
        }
    }

    // Transparent Discipline Score Breakdown Dialog
    if (showBreakdownDialog) {
        DisciplineScoreDialog(
            breakdown = disciplineBreakdown,
            onDismiss = { showBreakdownDialog = false }
        )
    }
}

@Composable
private fun WeeklyPerformanceCard(
    dailyMetrics: List<DailyMetricPoint>,
    overallConsistencyPercent: Int,
    modifier: Modifier = Modifier
) {
    val last7 = remember(dailyMetrics) {
        dailyMetrics.takeLast(7)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_performance_card"),
        shape = RoundedCornerShape(22.dp),
        color = Color(0x8C141C2E),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.verticalGradient(listOf(Color(0x59FFFFFF), Color(0x1AFFFFFF)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0x14FFFFFF), Color.Transparent)))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x3322C55E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6622C55E))
                ) {
                    Text(
                        text = "$overallConsistencyPercent%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreenBright,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 7 Days Vertical Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

                for (index in 0 until 7) {
                    val metric = if (index < last7.size) last7[index] else null
                    val completionRate = if (metric != null && metric.habitsScheduled > 0) {
                        (metric.habitsCompleted.toFloat() / metric.habitsScheduled.toFloat()).coerceIn(0f, 1f)
                    } else if (metric != null && metric.habitsCompleted > 0) {
                        1f
                    } else 0f

                    val isToday = (index == last7.size - 1)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Vertical Bar Container
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(95.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x26FFFFFF)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (completionRate > 0.05f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(completionRate)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(NeonGreenBright, NeonGreen)
                                            )
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = dayLabels.getOrElse(index) { "•" },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) NeonGreenBright else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyMetrics2x2Grid(
    totalHabits: Int,
    completionRate: Int,
    bestStreak: Int,
    waterAvgL: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmallMetricCard(
                modifier = Modifier.weight(1f),
                title = "TOTAL HABITS",
                value = "$totalHabits",
                icon = Icons.Filled.ListAlt,
                iconColor = NeonPurple
            )
            SmallMetricCard(
                modifier = Modifier.weight(1f),
                title = "COMPLETION RATE",
                value = "$completionRate%",
                icon = Icons.Filled.CheckCircle,
                iconColor = NeonGreenBright
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmallMetricCard(
                modifier = Modifier.weight(1f),
                title = "BEST STREAK",
                value = "$bestStreak Days",
                icon = Icons.Filled.Bolt,
                iconColor = NeonAmber
            )
            SmallMetricCard(
                modifier = Modifier.weight(1f),
                title = "WATER AVG",
                value = String.format(java.util.Locale.US, "%.1f L", waterAvgL),
                icon = Icons.Filled.WaterDrop,
                iconColor = NeonCyan
            )
        }
    }
}

@Composable
private fun SmallMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color(0x8C141C2E),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.verticalGradient(listOf(Color(0x4DFFFFFF), Color(0x14FFFFFF)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0x14FFFFFF), Color.Transparent)))
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                fontSize = 19.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun DisciplineScoreHeroCard(
    breakdown: com.example.data.models.DisciplineScoreBreakdown,
    onOpenBreakdown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("discipline_score_hero_card"),
        shape = RoundedCornerShape(24.dp),
        color = Color(0x8C141C2E),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(Color(0x6622C55E), Color(0x1A22C55E), Color(0x0DFFFFFF))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x1F22C55E), Color(0x0A06B6D4), Color.Transparent)
                    )
                )
                .padding(20.dp)
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x3322C55E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = NeonGreenBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "DISCIPLINE SCORE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Real activity rating • 0–100",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                val tier = when {
                    breakdown.score >= 85 -> "Elite" to NeonGreenBright
                    breakdown.score >= 70 -> "Master" to CyanAccent
                    breakdown.score >= 50 -> "Consistent" to AmberWarning
                    breakdown.score >= 25 -> "Building" to Color(0xFF60A5FA)
                    else -> "Starter" to TextSecondary
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = tier.second.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, tier.second.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onOpenBreakdown() }
                ) {
                    Text(
                        text = "${tier.first} • View Info",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tier.second,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(86.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(86.dp),
                        color = Color(0x26FFFFFF),
                        strokeWidth = 8.dp,
                        trackColor = Color.Transparent
                    )
                    val animatedScoreProgress by animateFloatAsState(
                        targetValue = (breakdown.score / 100f).coerceIn(0f, 1f),
                        label = "score_ring"
                    )
                    CircularProgressIndicator(
                        progress = { animatedScoreProgress },
                        modifier = Modifier.size(86.dp),
                        color = NeonGreenBright,
                        strokeWidth = 8.dp,
                        trackColor = Color.Transparent,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${breakdown.score}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "/ 100",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            breakdown.score >= 85 -> "Peak discipline achieved across daily habits, hydration, deep flow sessions, and night reflections."
                            breakdown.score >= 70 -> "High consistency established. Sustain your active streak to unlock Elite standing."
                            breakdown.score >= 50 -> "Solid routine in progress. Keep checking in daily to increase your score."
                            breakdown.score >= 25 -> "Early momentum building. Regular check-ins and flow sessions accelerate your progress."
                            else -> "Complete habits, meet your water goal, and log focus sessions to earn points."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to view calculation details →",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonGreenBright,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onOpenBreakdown() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementsShowcaseCard(
    achievements: List<com.example.data.models.Achievement>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("progress_achievements_card"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0x8C141C2E),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.verticalGradient(listOf(Color(0x59FFFFFF), Color(0x1AFFFFFF)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0x14FFFFFF), Color.Transparent)))
                .padding(18.dp)
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
                            .background(Color(0x33F59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.EmojiEvents, null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "ACHIEVEMENTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }
                val unlockedCount = achievements.count { it.isUnlocked }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (unlockedCount > 0) Color(0x3322C55E) else Color(0x26FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (unlockedCount > 0) Color(0x6622C55E) else Color(0x33FFFFFF)
                    )
                ) {
                    Text(
                        text = "$unlockedCount / ${achievements.size} Unlocked",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (unlockedCount > 0) NeonGreenBright else TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                achievements.take(4).forEach { achievement ->
                    val isUnlocked = achievement.isUnlocked
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isUnlocked) Brush.verticalGradient(listOf(Color(0x8022C55E), Color(0x3322C55E)))
                                else Brush.verticalGradient(listOf(Color(0x26FFFFFF), Color(0x0DFFFFFF))),
                                RoundedCornerShape(14.dp)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isUnlocked) Color(0x40102A1E) else Color(0x4D0B101D)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isUnlocked) Color(0x3322C55E) else Color(0x26FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = when (achievement.icon) {
                                    "flag" -> Icons.Filled.Flag
                                    "local_fire_department" -> Icons.Filled.LocalFireDepartment
                                    "bolt" -> Icons.Filled.Bolt
                                    "military_tech" -> Icons.Filled.MilitaryTech
                                    "construction" -> Icons.Filled.Construction
                                    "all_inclusive" -> Icons.Filled.AllInclusive
                                    "water_drop" -> Icons.Filled.WaterDrop
                                    "psychology" -> Icons.Filled.Psychology
                                    "nightlight_round" -> Icons.Filled.NightlightRound
                                    else -> Icons.Filled.EmojiEvents
                                }
                                Icon(
                                    imageVector = if (isUnlocked) icon else Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = if (isUnlocked) AmberWarning else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = achievement.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnlocked) TextPrimary else TextSecondary
                                    )
                                    if (isUnlocked) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0x3322C55E)
                                        ) {
                                            Text(
                                                text = "UNLOCKED",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = NeonGreenBright,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (isUnlocked) achievement.description else "Requirement: ${achievement.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisciplineScoreDialog(
    breakdown: com.example.data.models.DisciplineScoreBreakdown,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It", color = NeonGreenBright, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x3322C55E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = NeonGreenBright, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        text = "Discipline Score (${breakdown.score}/100)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Transparent weighted formula",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ScoreBreakdownRow(
                    title = "Habit Completion",
                    points = breakdown.habitsScore,
                    maxPoints = 30,
                    explanation = breakdown.habitsExplanation,
                    icon = Icons.Filled.CheckCircle,
                    color = NeonGreenBright
                )
                ScoreBreakdownRow(
                    title = "Streak Consistency",
                    points = breakdown.streakScore,
                    maxPoints = 20,
                    explanation = breakdown.streakExplanation,
                    icon = Icons.Filled.Bolt,
                    color = AmberWarning
                )
                ScoreBreakdownRow(
                    title = "Water-Goal Completion",
                    points = breakdown.waterScore,
                    maxPoints = 15,
                    explanation = breakdown.waterExplanation,
                    icon = Icons.Filled.WaterDrop,
                    color = CyanAccent
                )
                ScoreBreakdownRow(
                    title = "Completed Focus Sessions",
                    points = breakdown.focusScore,
                    maxPoints = 20,
                    explanation = breakdown.focusExplanation,
                    icon = Icons.Filled.Psychology,
                    color = Color(0xFFA855F7)
                )
                ScoreBreakdownRow(
                    title = "Night Review Consistency",
                    points = breakdown.reviewScore,
                    maxPoints = 15,
                    explanation = breakdown.reviewExplanation,
                    icon = Icons.Filled.NightlightRound,
                    color = Color(0xFF6366F1)
                )
            }
        },
        containerColor = Color(0xFF141C2E),
        textContentColor = TextPrimary,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun ScoreBreakdownRow(
    title: String,
    points: Int,
    maxPoints: Int,
    explanation: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0x660B101D),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1FFFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
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
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "$points / $maxPoints pts",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (points > 0) color else TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val progress = (points.toFloat() / maxPoints.toFloat()).coerceIn(0f, 1f)
            val animatedProgress by animateFloatAsState(targetValue = progress, label = "diag_prog_$title")
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = Color(0x26FFFFFF)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = explanation,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 10.5.sp
            )
        }
    }
}
