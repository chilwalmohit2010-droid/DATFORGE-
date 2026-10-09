package com.example.data.backup

import com.example.data.models.*

data class DayForgeBackupMetadata(
    val app: String = "DayForge",
    val format: String = "DayForge_Backup",
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val exportDateFormatted: String = "",
    val userId: String = ""
)

data class BackupPreferences(
    val launchAnimationEnabled: Boolean = true,
    val reducedMotionEnabled: Boolean = false
)

data class BackupSummary(
    val version: Int,
    val exportedAt: Long,
    val originalUserId: String,
    val habitsCount: Int,
    val completionsCount: Int,
    val waterRecordsCount: Int,
    val waterEntriesCount: Int,
    val nightReviewsCount: Int,
    val focusSessionsCount: Int,
    val achievementsCount: Int,
    val notificationsCount: Int,
    val hasProfile: Boolean,
    val hasReminders: Boolean,
    val hasPreferences: Boolean,
    val rawJson: String
)

data class BackupRestoreReport(
    val habitsRestored: Int,
    val completionsRestored: Int,
    val waterRecordsRestored: Int,
    val focusSessionsRestored: Int,
    val nightReviewsRestored: Int,
    val notificationsRestored: Int,
    val profileRestored: Boolean,
    val safetySnapshotPath: String? = null
)
