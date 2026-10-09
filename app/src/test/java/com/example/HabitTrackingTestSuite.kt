package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.models.CompletionStatus
import com.example.data.models.Habit
import com.example.data.models.HabitCategory
import com.example.data.models.HabitCompletion
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HabitTrackingTestSuite {

    private lateinit var db: AppDatabase
    private val testUserId = "user_habit_test_001"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testHabitScheduledDays_DailyAndCustom() {
        val dailyHabit = Habit(
            id = "h_daily",
            userId = testUserId,
            title = "Daily Journal",
            repeatDays = "1,2,3,4,5,6,7"
        )
        for (day in 1..7) {
            assertTrue(dailyHabit.isScheduledForDayOfWeek(day))
        }

        val weekendHabit = Habit(
            id = "h_weekend",
            userId = testUserId,
            title = "Weekend Trail Run",
            repeatDays = "6,7"
        )
        assertFalse(weekendHabit.isScheduledForDayOfWeek(1)) // Monday
        assertFalse(weekendHabit.isScheduledForDayOfWeek(5)) // Friday
        assertTrue(weekendHabit.isScheduledForDayOfWeek(6))  // Saturday
        assertTrue(weekendHabit.isScheduledForDayOfWeek(7))  // Sunday
    }

    @Test
    fun testPausedHabit_IsNotScheduledForAnyDate() {
        val pausedHabit = Habit(
            id = "h_paused",
            userId = testUserId,
            title = "Paused Routine",
            repeatDays = "1,2,3,4,5,6,7",
            isPaused = true
        )
        val today = LocalDate.of(2026, 10, 8)
        assertFalse("Paused habit must not be scheduled for any date", pausedHabit.isScheduledForDate(today))
    }

    @Test
    fun testHabitStartAndEndDates_BoundaryConditions() {
        val habit = Habit(
            id = "h_dates",
            userId = testUserId,
            title = "Spring Sprint",
            repeatDays = "1,2,3,4,5,6,7",
            startDate = "2026-05-01",
            endDate = "2026-05-31"
        )

        // Before start date
        val beforeStart = LocalDate.of(2026, 4, 30)
        assertFalse(habit.isScheduledForDate(beforeStart))
        assertTrue(habit.isFuture(beforeStart))

        // During date range
        val insideRange = LocalDate.of(2026, 5, 15)
        assertTrue(habit.isScheduledForDate(insideRange))
        assertFalse(habit.isEnded(insideRange))
        assertFalse(habit.isFuture(insideRange))

        // After end date
        val afterEnd = LocalDate.of(2026, 6, 1)
        assertFalse(habit.isScheduledForDate(afterEnd))
        assertTrue(habit.isEnded(afterEnd))
    }

    @Test
    fun testHabitCompletionAndUndo_DatabaseOperations() = runBlocking {
        val habit = Habit(
            id = "h_action",
            userId = testUserId,
            title = "Cold Shower",
            repeatDays = "1,2,3,4,5,6,7"
        )
        db.habitDao().insertOrUpdateHabit(habit)

        val dateStr = "2026-10-08"

        // Initial state: not completed
        var completions = db.habitCompletionDao().getCompletionsForDateOnce(testUserId, dateStr)
        assertTrue(completions.isEmpty())

        // Action: Complete habit
        val completion = HabitCompletion(
            habitId = habit.id,
            userId = testUserId,
            date = dateStr,
            status = CompletionStatus.COMPLETED,
            completedAt = System.currentTimeMillis()
        )
        db.habitCompletionDao().insertCompletion(completion)

        completions = db.habitCompletionDao().getCompletionsForDateOnce(testUserId, dateStr)
        assertEquals(1, completions.size)
        assertEquals(habit.id, completions[0].habitId)
        assertEquals(CompletionStatus.COMPLETED, completions[0].status)

        // Action: Undo completion
        db.habitCompletionDao().deleteCompletion(habit.id, dateStr)
        completions = db.habitCompletionDao().getCompletionsForDateOnce(testUserId, dateStr)
        assertTrue("Completions must be empty after undo deletion", completions.isEmpty())
    }

    @Test
    fun testDuplicateCompletionPrevention_ReplacesWithoutDuplicates() = runBlocking {
        val dateStr = "2026-10-08"
        val completion1 = HabitCompletion(
            habitId = "h_dup",
            userId = testUserId,
            date = dateStr,
            status = CompletionStatus.COMPLETED,
            completedAt = 1000L
        )
        val completion2 = HabitCompletion(
            habitId = "h_dup",
            userId = testUserId,
            date = dateStr,
            status = CompletionStatus.COMPLETED,
            completedAt = 2000L
        )

        // Insert first
        db.habitCompletionDao().insertCompletion(completion1)
        // Insert duplicate for same habit and date
        db.habitCompletionDao().insertCompletion(completion2)

        val completions = db.habitCompletionDao().getCompletionsForDateOnce(testUserId, dateStr)
        assertEquals("Duplicate insertions for same habit/date must replace and not duplicate", 1, completions.size)
        assertEquals(2000L, completions[0].completedAt)
    }
}
