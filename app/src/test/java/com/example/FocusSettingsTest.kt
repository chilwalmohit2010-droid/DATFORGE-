package com.example

import com.example.data.models.FocusSettings
import com.example.util.PermissionManager
import org.junit.Assert.*
import org.junit.Test

class FocusSettingsTest {

    @Test
    fun testFocusSettingsDefaults() {
        val settings = FocusSettings()
        assertEquals(25, settings.durationMinutes)
        assertNull(settings.linkedHabitId)
        assertFalse(settings.restrictionEnabled)
        assertTrue(settings.restrictedApps.isEmpty())
        assertNull(settings.relatedAppPackage)
        assertNull(settings.relatedAppName)
        assertFalse(settings.autoOpenRelatedApp)
    }

    @Test
    fun testFocusSettingsCustomValues() {
        val settings = FocusSettings(
            durationMinutes = 45,
            linkedHabitId = "habit_123",
            restrictionEnabled = true,
            restrictedApps = listOf("com.instagram.android", "com.zhiliaoapp.musically"),
            relatedAppPackage = "com.google.android.apps.docs",
            relatedAppName = "Google Docs",
            autoOpenRelatedApp = true
        )

        assertEquals(45, settings.durationMinutes)
        assertEquals("habit_123", settings.linkedHabitId)
        assertTrue(settings.restrictionEnabled)
        assertEquals(2, settings.restrictedApps.size)
        assertTrue(settings.restrictedApps.contains("com.instagram.android"))
        assertEquals("com.google.android.apps.docs", settings.relatedAppPackage)
        assertEquals("Google Docs", settings.relatedAppName)
        assertTrue(settings.autoOpenRelatedApp)
    }

    @Test
    fun testTimestampBasedRemainingSeconds() {
        val now = 1000000L
        val targetEndTime = now + (25 * 60 * 1000L) // 25 mins later

        // At start
        val diffStart = targetEndTime - now
        val remainingStart = ((diffStart + 999L) / 1000L).toInt()
        assertEquals(25 * 60, remainingStart)

        // 10 minutes in
        val nowPlus10Mins = now + (10 * 60 * 1000L)
        val diff10Mins = targetEndTime - nowPlus10Mins
        val remaining10Mins = ((diff10Mins + 999L) / 1000L).toInt()
        assertEquals(15 * 60, remaining10Mins)

        // Expired
        val nowExpired = targetEndTime + 5000L
        val diffExpired = targetEndTime - nowExpired
        val remainingExpired = if (diffExpired <= 0L) 0 else ((diffExpired + 999L) / 1000L).toInt()
        assertEquals(0, remainingExpired)
    }

    @Test
    fun testPermissionItemsHaveExplanations() {
        for (item in PermissionManager.PermissionItem.entries) {
            assertTrue(item.title.isNotBlank())
            assertTrue(item.subtitle.isNotBlank())
            assertTrue(item.explanation.isNotBlank())
            assertTrue(item.grantButtonText.isNotBlank())
        }
    }
}
