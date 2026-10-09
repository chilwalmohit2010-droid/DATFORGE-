package com.example.ui.screens.progress

import java.time.LocalDate

enum class AnalyticsPeriod(val label: String, val daysCount: Int?) {
    SEVEN_DAYS("7 Days", 7),
    THIRTY_DAYS("30 Days", 30),
    NINETY_DAYS("90 Days", 90),
    ALL_TIME("All Time", null)
}

enum class AnalyticsChartType(val label: String) {
    HABITS("Daily Habits"),
    WATER("Water Goal"),
    FOCUS("Focus Flow"),
    CONSISTENCY("Consistency")
}

data class DailyMetricPoint(
    val date: LocalDate,
    val dayLabel: String,
    val habitsCompleted: Int,
    val habitsScheduled: Int,
    val habitCompletionRate: Float,
    val waterIntakeMl: Int,
    val waterGoalMl: Int,
    val isWaterGoalMet: Boolean,
    val focusMinutes: Int,
    val nightReviewDone: Boolean,
    val compositeConsistencyRate: Float
)

data class PeriodOverviewStats(
    val currentStreak: Int,
    val bestStreak: Int,
    val totalHabitCompletions: Int,
    val averageDailyCompletion: Float,
    val habitConsistencyPercentage: Int,
    val waterGoalSuccessRate: Int,
    val totalFocusMinutes: Int,
    val nightReviewsCompleted: Int,
    val disciplineScore: Int,
    val scoreChangeText: String
)

data class HabitBreakdownStats(
    val habitId: String,
    val title: String,
    val categoryName: String,
    val colorHex: Long,
    val completionRatePercentage: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val periodCompletions: Int,
    val allTimeCompletions: Int,
    val trendText: String,
    val isTrendingUp: Boolean?
)

data class AnalyticsInsight(
    val iconName: String,
    val title: String,
    val description: String,
    val highlight: String? = null
)

data class HeatmapDay(
    val date: LocalDate,
    val completionCount: Int,
    val ratio: Float,
    val isToday: Boolean
)
