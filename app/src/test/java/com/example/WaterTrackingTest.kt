package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.models.WaterEntry
import com.example.data.models.WaterRecord
import com.example.data.models.WaterReminderSettings
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
class WaterTrackingTest {

    private lateinit var db: AppDatabase
    private val userId = "test_user_water_123"
    private val todayDate = "2026-09-03"

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
    fun testProtocol_Step1and2_Add250And500_VerifyTotal750() = runBlocking {
        // Step 1: Add +250 ml
        val entry1 = WaterEntry(
            id = UUID.randomUUID().toString(),
            userId = userId,
            date = todayDate,
            amountMl = 250,
            timestamp = System.currentTimeMillis()
        )
        db.waterDao().insertWaterEntry(entry1)

        var entries = db.waterDao().getWaterEntriesForDateOnce(userId, todayDate)
        assertEquals(1, entries.size)
        assertEquals(250, entries.sumOf { it.amountMl })

        // Step 2: Add +500 ml
        val entry2 = WaterEntry(
            id = UUID.randomUUID().toString(),
            userId = userId,
            date = todayDate,
            amountMl = 500,
            timestamp = System.currentTimeMillis()
        )
        db.waterDao().insertWaterEntry(entry2)

        entries = db.waterDao().getWaterEntriesForDateOnce(userId, todayDate)
        assertEquals(2, entries.size)
        val totalIntake = entries.sumOf { it.amountMl }
        assertEquals(750, totalIntake)

        // Save daily aggregated record
        val record = WaterRecord(
            userId = userId,
            date = todayDate,
            currentMl = totalIntake,
            targetMl = 2500
        )
        db.waterDao().insertOrUpdateWater(record)

        val retrievedRecord = db.waterDao().getWaterRecordOnce(userId, todayDate)
        assertNotNull(retrievedRecord)
        assertEquals(750, retrievedRecord!!.currentMl)
        assertEquals(2500, retrievedRecord.targetMl)
    }

    @Test
    fun testProtocol_Step3and4_RefreshApp_VerifyTotalRemainsCorrect() = runBlocking {
        // Populate entries (250ml + 500ml)
        val entry1 = WaterEntry(UUID.randomUUID().toString(), userId, todayDate, 250, System.currentTimeMillis())
        val entry2 = WaterEntry(UUID.randomUUID().toString(), userId, todayDate, 500, System.currentTimeMillis())
        db.waterDao().insertWaterEntry(entry1)
        db.waterDao().insertWaterEntry(entry2)

        val record = WaterRecord(userId, todayDate, 750, 2500)
        db.waterDao().insertOrUpdateWater(record)

        // Simulate app refresh (querying DB anew)
        val freshRecord = db.waterDao().getWaterRecordOnce(userId, todayDate)
        val freshEntries = db.waterDao().getWaterEntriesForDateOnce(userId, todayDate)

        assertNotNull(freshRecord)
        assertEquals(750, freshRecord!!.currentMl)
        assertEquals(2, freshEntries.size)
        assertEquals(750, freshEntries.sumOf { it.amountMl })
    }

    @Test
    fun testProtocol_Step5and6_LogoutAndLogin_VerifyDataRestoration() = runBlocking {
        // Simulate remote cloud data after user had logged 250ml + 500ml
        val cloudRecord = WaterRecord(userId, todayDate, 750, 2500)
        val cloudEntries = listOf(
            WaterEntry("e1", userId, todayDate, 250, 1000L),
            WaterEntry("e2", userId, todayDate, 500, 2000L)
        )

        // Simulate logout (clear local cache)
        db.waterDao().deleteAllWaterForUser(userId)
        db.waterDao().deleteAllWaterEntriesForUser(userId)

        assertEquals(0, db.waterDao().getWaterEntriesForDateOnce(userId, todayDate).size)
        assertNull(db.waterDao().getWaterRecordOnce(userId, todayDate))

        // Simulate login restoration from cloud
        db.waterDao().insertOrUpdateWater(cloudRecord)
        db.waterDao().insertWaterEntries(cloudEntries)

        // Verify data is present and intact
        val restoredRecord = db.waterDao().getWaterRecordOnce(userId, todayDate)
        val restoredEntries = db.waterDao().getWaterEntriesForDateOnce(userId, todayDate)

        assertNotNull(restoredRecord)
        assertEquals(750, restoredRecord!!.currentMl)
        assertEquals(2, restoredEntries.size)
        // Entries are ordered by timestamp DESC (e2 at 2000L comes first, e1 at 1000L comes second)
        assertEquals(500, restoredEntries[0].amountMl)
        assertEquals(250, restoredEntries[1].amountMl)
        assertEquals(750, restoredEntries.sumOf { it.amountMl })
    }

    @Test
    fun testWaterEntryEditAndDelete() = runBlocking {
        val entry = WaterEntry("edit_me", userId, todayDate, 250, System.currentTimeMillis())
        db.waterDao().insertWaterEntry(entry)

        // Edit
        val updated = entry.copy(amountMl = 350)
        db.waterDao().updateWaterEntry(updated)

        var entries = db.waterDao().getWaterEntriesForDateOnce(userId, todayDate)
        assertEquals(1, entries.size)
        assertEquals(350, entries[0].amountMl)

        // Delete
        db.waterDao().deleteWaterEntryById("edit_me")
        entries = db.waterDao().getWaterEntriesForDateOnce(userId, todayDate)
        assertTrue(entries.isEmpty())
    }

    @Test
    fun testWaterReminderSettings() {
        val settings = WaterReminderSettings(
            isEnabled = true,
            intervalMinutes = 60,
            startTime = "08:00",
            endTime = "21:00"
        )
        assertTrue(settings.isEnabled)
        assertEquals(60, settings.intervalMinutes)
        assertEquals("08:00", settings.startTime)
        assertEquals("21:00", settings.endTime)
    }
}
