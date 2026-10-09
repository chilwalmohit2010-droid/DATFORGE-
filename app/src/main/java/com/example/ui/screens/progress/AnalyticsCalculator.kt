package com.example.ui.screens.progress

import com.example.data.models.CompletionStatus
import com.example.data.models.FocusSession
import com.example.data.models.Habit
import com.example.data.models.HabitCompletion
import com.example.data.models.NightReviewEntry
import com.example.data.models.UserAccount
import com.example.data.models.WaterRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

object AnalyticsCalculator {

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getPeriodDates(
        period: AnalyticsPeriod,
        today: LocalDate,
        earliestDate: LocalDate?
    ): List<LocalDate> {
        val days = when (period) {
            AnalyticsPeriod.SEVEN_DAYS -> 7
            AnalyticsPeriod.THIRTY_DAYS -> 30
            AnalyticsPeriod.NINETY_DAYS -> 90
            AnalyticsPeriod.ALL_TIME -> {
                if (earliestDate != null && earliestDate.isBefore(today)) {
                    val diff = ChronoUnit.DAYS.between(earliestDate, today).toInt() + 1
                    diff.coerceIn(7, 365)
                } else {
                    30
                }
            }
        }
        return (days - 1 downTo 0).map { today.minusDays(it.toLong()) }
    }

    fun computeDailyMetrics(
        dates: List<LocalDate>,
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        waterRecords: List<WaterRecord>,
        defaultWaterGoalMl: Int,
        focusSessions: List<FocusSession>,
        nightReviews: List<NightReviewEntry>
    ): List<DailyMetricPoint> {
        val completionsMap = completions.associate { "${it.habitId}_${it.date}" to it.status }
        val waterMap = waterRecords.associateBy { it.date }
        val nightReviewDates = nightReviews.map { it.date }.toSet()

        val focusMap = mutableMapOf<String, Int>()
        val zone = ZoneId.systemDefault()
        focusSessions.filter { it.isCompleted }.forEach { session ->
            val sessionDate = Instant.ofEpochMilli(session.timestamp)
                .atZone(zone)
                .toLocalDate()
                .format(isoFormatter)
            focusMap[sessionDate] = (focusMap[sessionDate] ?: 0) + session.completedMinutes
        }

        return dates.map { date ->
            val dateStr = date.format(isoFormatter)
            val dayOfWeek = date.dayOfWeek.value
            val scheduledHabits = habits.filterNot { it.isPaused }
                .filter { it.isScheduledForDayOfWeek(dayOfWeek) }
            val completedCount = scheduledHabits.count {
                completionsMap["${it.id}_$dateStr"] == CompletionStatus.COMPLETED
            }
            val scheduledCount = scheduledHabits.size
            val habitRatio = if (scheduledCount > 0) {
                (completedCount.toFloat() / scheduledCount.toFloat()).coerceIn(0f, 1f)
            } else if (completedCount > 0) {
                1f
            } else {
                0f
            }

            val waterRecord = waterMap[dateStr]
            val waterIntake = waterRecord?.currentMl ?: 0
            val waterGoal = if ((waterRecord?.targetMl ?: 0) > 0) waterRecord!!.targetMl else defaultWaterGoalMl
            val isWaterGoalMet = (waterGoal > 0 && waterIntake >= waterGoal)

            val focusMins = focusMap[dateStr] ?: 0
            val isNightReviewDone = nightReviewDates.contains(dateStr)

            // Calculate composite consistency (0f - 1f)
            val habitFactor = habitRatio * 0.55f
            val waterFactor = if (isWaterGoalMet) 0.15f else (waterIntake.toFloat() / waterGoal.coerceAtLeast(1000).toFloat()).coerceIn(0f, 0.15f)
            val focusFactor = (focusMins.toFloat() / 25f).coerceIn(0f, 1f) * 0.15f
            val reviewFactor = if (isNightReviewDone) 0.15f else 0f
            val compositeRate = (habitFactor + waterFactor + focusFactor + reviewFactor).coerceIn(0f, 1f)

            val dayLabel = when {
                dates.size <= 7 -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                else -> "${date.monthValue}/${date.dayOfMonth}"
            }

            DailyMetricPoint(
                date = date,
                dayLabel = dayLabel,
                habitsCompleted = completedCount,
                habitsScheduled = scheduledCount,
                habitCompletionRate = habitRatio,
                waterIntakeMl = waterIntake,
                waterGoalMl = waterGoal,
                isWaterGoalMet = isWaterGoalMet,
                focusMinutes = focusMins,
                nightReviewDone = isNightReviewDone,
                compositeConsistencyRate = compositeRate
            )
        }
    }

    fun computePeriodOverview(
        currentUser: UserAccount?,
        dailyMetrics: List<DailyMetricPoint>,
        disciplineScore: Int
    ): PeriodOverviewStats {
        val totalCompletions = dailyMetrics.sumOf { it.habitsCompleted }
        val daysCount = dailyMetrics.size.coerceAtLeast(1)
        val avgDaily = totalCompletions.toFloat() / daysCount.toFloat()

        val totalScheduled = dailyMetrics.sumOf { it.habitsScheduled }
        val habitConsistencyPct = if (totalScheduled > 0) {
            ((totalCompletions.toFloat() / totalScheduled.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        } else if (totalCompletions > 0) {
            100
        } else {
            0
        }

        val waterMetDays = dailyMetrics.count { it.isWaterGoalMet }
        val waterGoalSuccessRate = ((waterMetDays.toFloat() / daysCount.toFloat()) * 100f).roundToInt().coerceIn(0, 100)

        val totalFocusMins = dailyMetrics.sumOf { it.focusMinutes }
        val nightReviewsDone = dailyMetrics.count { it.nightReviewDone }

        // Score change compared to early half of period
        val midPoint = daysCount / 2
        val firstHalfAvg = if (midPoint > 0) {
            dailyMetrics.take(midPoint).map { it.compositeConsistencyRate }.average().toFloat()
        } else 0f
        val secondHalfAvg = if (midPoint > 0) {
            dailyMetrics.drop(midPoint).map { it.compositeConsistencyRate }.average().toFloat()
        } else 0f

        val deltaPct = ((secondHalfAvg - firstHalfAvg) * 100).roundToInt()
        val scoreChangeText = when {
            deltaPct > 0 -> "+$deltaPct% trend"
            deltaPct < 0 -> "$deltaPct% trend"
            else -> "Stable"
        }

        return PeriodOverviewStats(
            currentStreak = currentUser?.currentStreak ?: 0,
            bestStreak = currentUser?.bestStreak ?: 0,
            totalHabitCompletions = totalCompletions,
            averageDailyCompletion = avgDaily,
            habitConsistencyPercentage = habitConsistencyPct,
            waterGoalSuccessRate = waterGoalSuccessRate,
            totalFocusMinutes = totalFocusMins,
            nightReviewsCompleted = nightReviewsDone,
            disciplineScore = disciplineScore,
            scoreChangeText = scoreChangeText
        )
    }

    fun computeHeatmapDays(
        today: LocalDate,
        habits: List<Habit>,
        completions: List<HabitCompletion>
    ): List<HeatmapDay> {
        // Last 91 days (13 full weeks x 7 days)
        val completionsMap = completions.associate { "${it.habitId}_${it.date}" to it.status }
        val activeHabits = habits.filterNot { it.isPaused }

        val totalDays = 91
        return (totalDays - 1 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val dateStr = date.format(isoFormatter)
            val dayOfWeek = date.dayOfWeek.value
            val scheduled = activeHabits.filter { it.isScheduledForDayOfWeek(dayOfWeek) }
            val completed = scheduled.count {
                completionsMap["${it.id}_$dateStr"] == CompletionStatus.COMPLETED
            }
            val ratio = if (scheduled.isNotEmpty()) {
                completed.toFloat() / scheduled.size.toFloat()
            } else if (completed > 0) 1f else 0f

            HeatmapDay(
                date = date,
                completionCount = completed,
                ratio = ratio,
                isToday = (date == today)
            )
        }
    }

    fun computeHabitBreakdowns(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        periodDates: List<LocalDate>,
        today: LocalDate
    ): List<HabitBreakdownStats> {
        val periodDateStrings = periodDates.map { it.format(isoFormatter) }.toSet()
        val completionsByHabit = completions
            .filter { it.status == CompletionStatus.COMPLETED }
            .groupBy { it.habitId }

        return habits.map { habit ->
            val habitCompletions = completionsByHabit[habit.id] ?: emptyList()
            val completedDatesSet = habitCompletions.map { it.date }.toSet()

            // Period completions
            val periodCompletionsCount = habitCompletions.count { periodDateStrings.contains(it.date) }
            val scheduledInPeriod = periodDates.count { habit.isScheduledForDayOfWeek(it.dayOfWeek.value) }

            val completionRate = if (scheduledInPeriod > 0) {
                ((periodCompletionsCount.toFloat() / scheduledInPeriod.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
            } else if (periodCompletionsCount > 0) {
                100
            } else {
                0
            }

            // Calculate current streak for this habit
            var currentStreak = 0
            var checkDate = today
            // If today is scheduled and not completed yet, allow checking yesterday to preserve streak
            val isScheduledToday = habit.isScheduledForDayOfWeek(today.dayOfWeek.value)
            val isCompletedToday = completedDatesSet.contains(today.format(isoFormatter))
            if (isScheduledToday && !isCompletedToday) {
                checkDate = today.minusDays(1)
            }

            for (i in 0..365) {
                val d = checkDate.minusDays(i.toLong())
                if (habit.isScheduledForDayOfWeek(d.dayOfWeek.value)) {
                    if (completedDatesSet.contains(d.format(isoFormatter))) {
                        currentStreak++
                    } else {
                        break
                    }
                }
            }

            // Calculate best streak across all history
            var bestStreak = 0
            var runningStreak = 0
            // Traverse from 365 days ago up to today
            for (i in 365 downTo 0) {
                val d = today.minusDays(i.toLong())
                if (habit.isScheduledForDayOfWeek(d.dayOfWeek.value)) {
                    if (completedDatesSet.contains(d.format(isoFormatter))) {
                        runningStreak++
                        if (runningStreak > bestStreak) bestStreak = runningStreak
                    } else {
                        runningStreak = 0
                    }
                }
            }
            if (currentStreak > bestStreak) bestStreak = currentStreak

            // Trend: compare recent half of period vs older half of period
            val halfSize = periodDates.size / 2
            val oldHalf = periodDates.take(halfSize)
            val recentHalf = periodDates.drop(halfSize)

            val oldCompleted = oldHalf.count { habit.isScheduledForDayOfWeek(it.dayOfWeek.value) && completedDatesSet.contains(it.format(isoFormatter)) }
            val oldScheduled = oldHalf.count { habit.isScheduledForDayOfWeek(it.dayOfWeek.value) }.coerceAtLeast(1)
            val oldRate = (oldCompleted.toFloat() / oldScheduled.toFloat()) * 100f

            val recentCompleted = recentHalf.count { habit.isScheduledForDayOfWeek(it.dayOfWeek.value) && completedDatesSet.contains(it.format(isoFormatter)) }
            val recentScheduled = recentHalf.count { habit.isScheduledForDayOfWeek(it.dayOfWeek.value) }.coerceAtLeast(1)
            val recentRate = (recentCompleted.toFloat() / recentScheduled.toFloat()) * 100f

            val diff = (recentRate - oldRate).roundToInt()
            val (trendText, isTrendingUp) = when {
                periodCompletionsCount == 0 -> "Not Started" to null
                diff >= 10 -> "Trending Up (+$diff%)" to true
                diff <= -10 -> "Needs Focus ($diff%)" to false
                else -> "Consistent" to true
            }

            HabitBreakdownStats(
                habitId = habit.id,
                title = habit.title,
                categoryName = habit.category.displayName,
                colorHex = habit.colorHex,
                completionRatePercentage = completionRate,
                currentStreak = currentStreak,
                bestStreak = bestStreak,
                periodCompletions = periodCompletionsCount,
                allTimeCompletions = habitCompletions.size,
                trendText = trendText,
                isTrendingUp = isTrendingUp
            )
        }
    }

    fun computeInsights(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        dailyMetrics: List<DailyMetricPoint>,
        habitBreakdowns: List<HabitBreakdownStats>,
        overview: PeriodOverviewStats,
        today: LocalDate
    ): List<AnalyticsInsight> {
        val insights = mutableListOf<AnalyticsInsight>()
        val totalCompletions = overview.totalHabitCompletions

        if (totalCompletions == 0) {
            insights.add(
                AnalyticsInsight(
                    iconName = "flag",
                    title = "Kickstart Your Consistency",
                    description = "Complete habits and focus sessions to unlock personalized behavioral insights.",
                    highlight = "Ready to start"
                )
            )
            return insights
        }

        // 1. Week over Week comparison
        val past7Days = (6 downTo 0).map { today.minusDays(it.toLong()).format(isoFormatter) }.toSet()
        val prev7Days = (13 downTo 7).map { today.minusDays(it.toLong()).format(isoFormatter) }.toSet()

        val completedThisWeek = completions.count { it.status == CompletionStatus.COMPLETED && past7Days.contains(it.date) }
        val completedLastWeek = completions.count { it.status == CompletionStatus.COMPLETED && prev7Days.contains(it.date) }

        if (completedLastWeek > 0) {
            val delta = completedThisWeek - completedLastWeek
            val pct = ((delta.toFloat() / completedLastWeek.toFloat()) * 100f).roundToInt()
            if (pct > 0) {
                insights.add(
                    AnalyticsInsight(
                        iconName = "trending_up",
                        title = "Velocity Increased",
                        description = "You completed $pct% more habits this week compared to last week ($completedThisWeek vs $completedLastWeek).",
                        highlight = "+$pct% boost"
                    )
                )
            } else if (pct == 0) {
                insights.add(
                    AnalyticsInsight(
                        iconName = "bolt",
                        title = "Steady Weekly Output",
                        description = "You maintained steady execution with $completedThisWeek habit check-ins matching last week.",
                        highlight = "Stable pace"
                    )
                )
            } else {
                insights.add(
                    AnalyticsInsight(
                        iconName = "refresh",
                        title = "Momentum Reset",
                        description = "Completed $completedThisWeek habits this week compared to $completedLastWeek last week. A single good day will restart momentum.",
                        highlight = "Opportunity"
                    )
                )
            }
        } else if (completedThisWeek > 0) {
            insights.add(
                AnalyticsInsight(
                    iconName = "trending_up",
                    title = "Strong Weekly Start",
                    description = "Completed $completedThisWeek habits over the last 7 days. Consistency is forming.",
                    highlight = "$completedThisWeek completed"
                )
            )
        }

        // 2. Strongest Habit
        val strongest = habitBreakdowns.filter { it.periodCompletions > 0 }
            .maxByOrNull { it.completionRatePercentage }

        if (strongest != null) {
            insights.add(
                AnalyticsInsight(
                    iconName = "star",
                    title = "Anchor Habit",
                    description = "'${strongest.title}' is your most reliable routine with a ${strongest.completionRatePercentage}% completion rate.",
                    highlight = "${strongest.completionRatePercentage}% success"
                )
            )
        }

        // 3. Most consistent day of the week
        val metricsByDayOfWeek = dailyMetrics.groupBy { it.date.dayOfWeek }
        val bestDayEntry = metricsByDayOfWeek.mapValues { (_, metrics) ->
            val totalScheduled = metrics.sumOf { it.habitsScheduled }
            val totalCompleted = metrics.sumOf { it.habitsCompleted }
            if (totalScheduled > 0) (totalCompleted.toFloat() / totalScheduled.toFloat()) else 0f
        }.filter { it.value > 0f }.maxByOrNull { it.value }

        if (bestDayEntry != null) {
            val dayName = bestDayEntry.key.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val dayRate = (bestDayEntry.value * 100).roundToInt()
            insights.add(
                AnalyticsInsight(
                    iconName = "calendar_today",
                    title = "Peak Day of Week",
                    description = "Your highest execution rate occurs on ${dayName}s with an average of $dayRate% completion.",
                    highlight = "$dayName ($dayRate%)"
                )
            )
        }

        // 4. Focus or Water highlight
        if (overview.totalFocusMinutes > 0) {
            val hours = overview.totalFocusMinutes / 60
            val mins = overview.totalFocusMinutes % 60
            val formattedTime = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
            insights.add(
                AnalyticsInsight(
                    iconName = "psychology",
                    title = "Deep Flow Investment",
                    description = "You dedicated $formattedTime of uninterrupted deep focus work in this period.",
                    highlight = formattedTime
                )
            )
        } else if (overview.waterGoalSuccessRate >= 50) {
            insights.add(
                AnalyticsInsight(
                    iconName = "water_drop",
                    title = "Hydration Discipline",
                    description = "Met your target water intake on ${overview.waterGoalSuccessRate}% of days in this period.",
                    highlight = "${overview.waterGoalSuccessRate}% target met"
                )
            )
        }

        return insights
    }
}
