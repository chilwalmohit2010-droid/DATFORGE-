package com.example

import com.example.data.models.CompletionStatus
import com.example.data.models.FocusSession
import com.example.data.models.Habit
import com.example.data.models.HabitCategory
import com.example.data.models.HabitCompletion
import com.example.data.models.NightReviewEntry
import com.example.data.models.UserAccount
import com.example.data.models.WaterRecord
import com.example.ui.screens.progress.AnalyticsCalculator
import com.example.ui.screens.progress.AnalyticsPeriod
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AnalyticsCalculatorTest {

    private val today = LocalDate.of(2026, 9, 10)
    private val iso = DateTimeFormatter.ISO_LOCAL_DATE

    @Test
    fun testPeriodDatesCount() {
        val days7 = AnalyticsCalculator.getPeriodDates(AnalyticsPeriod.SEVEN_DAYS, today, null)
        assertEquals(7, days7.size)
        assertEquals(today.minusDays(6), days7.first())
        assertEquals(today, days7.last())

        val days30 = AnalyticsCalculator.getPeriodDates(AnalyticsPeriod.THIRTY_DAYS, today, null)
        assertEquals(30, days30.size)

        val days90 = AnalyticsCalculator.getPeriodDates(AnalyticsPeriod.NINETY_DAYS, today, null)
        assertEquals(90, days90.size)
    }

    @Test
    fun testComputeDailyMetricsAndOverview() {
        val habit1 = Habit(id = "h1", title = "Meditation", repeatDays = "1,2,3,4,5,6,7", category = HabitCategory.MINDSET)
        val habit2 = Habit(id = "h2", title = "Workout", repeatDays = "1,2,3,4,5,6,7", category = HabitCategory.FITNESS)
        val habits = listOf(habit1, habit2)

        val completionDate = today.format(iso)
        val completions = listOf(
            HabitCompletion(habitId = "h1", date = completionDate, status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = "h2", date = completionDate, status = CompletionStatus.COMPLETED)
        )

        val waterRecords = listOf(
            WaterRecord(date = completionDate, currentMl = 2500, targetMl = 2000)
        )

        val focusSessions = listOf(
            FocusSession(id = "f1", completedMinutes = 45, isCompleted = true, timestamp = System.currentTimeMillis())
        )

        val nightReviews = listOf(
            NightReviewEntry(userId = "u1", date = completionDate, rating = 5)
        )

        val dates = listOf(today)
        val metrics = AnalyticsCalculator.computeDailyMetrics(
            dates = dates,
            habits = habits,
            completions = completions,
            waterRecords = waterRecords,
            defaultWaterGoalMl = 2000,
            focusSessions = focusSessions,
            nightReviews = nightReviews
        )

        assertEquals(1, metrics.size)
        val point = metrics.first()
        assertEquals(2, point.habitsCompleted)
        assertEquals(2, point.habitsScheduled)
        assertEquals(1.0f, point.habitCompletionRate, 0.01f)
        assertEquals(2500, point.waterIntakeMl)
        assertTrue(point.isWaterGoalMet)
        assertTrue(point.nightReviewDone)
        assertTrue(point.compositeConsistencyRate > 0.8f)

        val user = UserAccount(uid = "u1", currentStreak = 5, bestStreak = 10)
        val overview = AnalyticsCalculator.computePeriodOverview(user, metrics, 85)

        assertEquals(5, overview.currentStreak)
        assertEquals(10, overview.bestStreak)
        assertEquals(2, overview.totalHabitCompletions)
        assertEquals(100, overview.habitConsistencyPercentage)
        assertEquals(100, overview.waterGoalSuccessRate)
        assertEquals(85, overview.disciplineScore)
    }

    @Test
    fun testHabitBreakdownsAndStreaks() {
        val habit = Habit(id = "h1", title = "Reading", repeatDays = "1,2,3,4,5,6,7", category = HabitCategory.LEARNING)
        val completions = listOf(
            HabitCompletion(habitId = "h1", date = today.format(iso), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = "h1", date = today.minusDays(1).format(iso), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = "h1", date = today.minusDays(2).format(iso), status = CompletionStatus.COMPLETED)
        )

        val periodDates = AnalyticsCalculator.getPeriodDates(AnalyticsPeriod.SEVEN_DAYS, today, null)
        val breakdowns = AnalyticsCalculator.computeHabitBreakdowns(listOf(habit), completions, periodDates, today)

        assertEquals(1, breakdowns.size)
        val b = breakdowns.first()
        assertEquals("Reading", b.title)
        assertEquals(3, b.currentStreak)
        assertTrue(b.bestStreak >= 3)
        assertEquals(3, b.periodCompletions)
        assertEquals(3, b.allTimeCompletions)
    }

    @Test
    fun testHeatmapDaysGenerated() {
        val habit = Habit(id = "h1", title = "Yoga", repeatDays = "1,2,3,4,5,6,7")
        val completions = listOf(
            HabitCompletion(habitId = "h1", date = today.format(iso), status = CompletionStatus.COMPLETED)
        )
        val heatmapDays = AnalyticsCalculator.computeHeatmapDays(today, listOf(habit), completions)
        assertEquals(91, heatmapDays.size)
        assertTrue(heatmapDays.last().isToday)
        assertEquals(1, heatmapDays.last().completionCount)
    }
}
