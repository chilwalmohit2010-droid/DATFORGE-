package com.example

import com.example.data.models.PublicUserProfile
import com.example.data.models.UserAccount
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupAndSecurityTestSuite {

    @Test
    fun testValidDayForgeBackupStructure_ValidationPasses() {
        val root = JSONObject().apply {
            put("app", "DayForge")
            put("format", "DayForge_Backup")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("userId", "test_user_backup")
            put("habits", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "h1")
                    put("title", "Daily Reading")
                })
            })
            put("habitCompletions", JSONArray())
            put("waterRecords", JSONArray())
            put("nightReviews", JSONArray())
        }

        val app = root.optString("app", "")
        val format = root.optString("format", "")
        val isDayForge = app.equals("DayForge", ignoreCase = true) ||
                format.equals("DayForge_Backup", ignoreCase = true) ||
                root.has("habits")

        assertTrue("Valid DayForge backup must be recognized", isDayForge)
        assertEquals(1, root.getInt("version"))
        assertEquals(1, root.getJSONArray("habits").length())
    }

    @Test
    fun testUnsupportedBackupFile_IsRejected() {
        val foreignFile = JSONObject().apply {
            put("app", "SomeOtherRandomApp")
            put("randomKey", "randomValue")
        }

        val app = foreignFile.optString("app", "")
        val format = foreignFile.optString("format", "")
        val isDayForge = app.equals("DayForge", ignoreCase = true) ||
                format.equals("DayForge_Backup", ignoreCase = true) ||
                foreignFile.has("habits")

        assertFalse("Foreign non-DayForge file must be rejected", isDayForge)
    }

    @Test
    fun testDataSafety_PublicProfileHasNoPasswordOrEmailField() {
        val publicProfile = PublicUserProfile(
            uid = "pub_uid",
            username = "discipline_warrior",
            displayName = "Warrior",
            bio = "Forging habits",
            publicProfileEnabled = true,
            streak = 14,
            disciplineScore = 85
        )

        // Reflection check: ensure PublicUserProfile class does not declare email or password fields
        val fieldNames = publicProfile::class.java.declaredFields.map { it.name }
        assertFalse("PublicUserProfile must not contain email field", fieldNames.contains("email"))
        assertFalse("PublicUserProfile must not contain password field", fieldNames.contains("password"))
        assertFalse("PublicUserProfile must not contain privateToken field", fieldNames.contains("privateToken"))
    }
}
