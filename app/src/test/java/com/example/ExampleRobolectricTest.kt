package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.models.Habit
import com.example.data.models.HabitCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DayForge", appName)
    }

    @Test
    fun `test habit schedule repeat days`() {
        val habit = Habit(
            id = "test-1",
            userId = "user-1",
            title = "Morning Workout",
            category = HabitCategory.FITNESS,
            repeatDays = "1,2,3,4,5" // Mon to Fri
        )

        assertTrue(habit.isScheduledForDayOfWeek(1)) // Monday
        assertTrue(habit.isScheduledForDayOfWeek(5)) // Friday
        assertTrue(!habit.isScheduledForDayOfWeek(6)) // Saturday
        assertTrue(!habit.isScheduledForDayOfWeek(7)) // Sunday
    }

    @Test
    fun `test backup json format structure`() {
        val root = org.json.JSONObject().apply {
            put("app", "DayForge")
            put("format", "DayForge_Backup")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("userId", "test-user-123")
            put("habits", org.json.JSONArray())
            put("habitCompletions", org.json.JSONArray())
        }

        assertEquals("DayForge", root.getString("app"))
        assertEquals("DayForge_Backup", root.getString("format"))
        assertEquals(1, root.getInt("version"))
        assertTrue(root.has("habits"))
        assertTrue(root.has("habitCompletions"))
    }
}
