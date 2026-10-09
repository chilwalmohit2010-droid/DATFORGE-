package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.models.FocusSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FocusAndTimerTestSuite {

    private lateinit var db: AppDatabase
    private val testUserId = "user_focus_test_002"

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
    fun testElapsedTimeCalculation_RemainingSecondsAccuracy() {
        val now = 100_000L
        val targetEndTime = now + (25 * 60 * 1000L) // 25 minutes in future (1,500,000 ms)

        // At start: 1500 seconds remaining
        val remainingAtStart = ((targetEndTime - now + 999L) / 1000L).toInt()
        assertEquals(1500, remainingAtStart)

        // After 10 minutes (600,000 ms elapsed): 900 seconds remaining
        val timeAfter10Mins = now + (10 * 60 * 1000L)
        val remainingAfter10Mins = ((targetEndTime - timeAfter10Mins + 999L) / 1000L).toInt()
        assertEquals(900, remainingAfter10Mins)

        // When elapsed time reaches or exceeds target
        val timeAtEnd = targetEndTime
        val diffAtEnd = targetEndTime - timeAtEnd
        assertTrue("Difference must be <= 0 at completion", diffAtEnd <= 0L)
    }

    @Test
    fun testFocusSessionPersistenceAndDuplicatePrevention() = runBlocking {
        val sessionId = UUID.randomUUID().toString()
        val session1 = FocusSession(
            id = sessionId,
            userId = testUserId,
            habitTitle = "Deep Work",
            durationMinutes = 25,
            completedMinutes = 25,
            isCompleted = true,
            timestamp = System.currentTimeMillis()
        )

        // Record session in database
        db.focusDao().insertFocusSession(session1)
        var sessions = db.focusDao().getAllFocusSessions(testUserId).first()
        assertEquals(1, sessions.size)
        assertEquals(25, sessions[0].completedMinutes)

        // Attempting to record duplicate session with same ID replaces rather than creating duplicate row
        val duplicateAttempt = session1.copy(completedMinutes = 25)
        db.focusDao().insertFocusSession(duplicateAttempt)
        sessions = db.focusDao().getAllFocusSessions(testUserId).first()
        assertEquals("Duplicate session ID must not create duplicate entries", 1, sessions.size)
    }
}
