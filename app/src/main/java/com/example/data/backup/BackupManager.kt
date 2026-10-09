package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.models.*
import com.example.data.remote.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(
    private val database: AppDatabase,
    private val firebaseManager: FirebaseManager
) {
    companion object {
        private const val TAG = "BackupManager"
        const val BACKUP_APP_NAME = "DayForge"
        const val BACKUP_FORMAT = "DayForge_Backup"
        const val CURRENT_VERSION = 1
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Creates a complete, structured JSON backup of the user's data.
     * Guaranteed not to include passwords, private tokens, or other users' data.
     */
    suspend fun createBackupJson(context: Context, currentUid: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()

            // 1. Metadata
            val now = System.currentTimeMillis()
            root.put("app", BACKUP_APP_NAME)
            root.put("format", BACKUP_FORMAT)
            root.put("version", CURRENT_VERSION)
            root.put("exportedAt", now)
            root.put("exportDateFormatted", dateFormat.format(Date(now)))
            root.put("userId", currentUid)

            // 2. User Profile (excluding sensitive credentials)
            val profile = database.userProfileDao().getUserProfileOnce(currentUid)
            if (profile != null) {
                val profileObj = JSONObject().apply {
                    put("uid", currentUid)
                    put("email", profile.email)
                    put("username", profile.username)
                    put("displayName", profile.displayName)
                    put("bio", profile.bio)
                    put("photoUrl", profile.photoUrl)
                    put("disciplineScore", profile.disciplineScore)
                    put("currentStreak", profile.currentStreak)
                    put("bestStreak", profile.bestStreak)
                    put("completionRate", profile.completionRate.toDouble())
                    put("publicProfileEnabled", profile.publicProfileEnabled)
                    put("createdAt", profile.createdAt)
                }
                root.put("userProfile", profileObj)
            }

            // 3. Habits
            val habits = database.habitDao().getHabitsForUserOnce(currentUid)
            val habitsArray = JSONArray()
            for (h in habits) {
                val hObj = JSONObject().apply {
                    put("id", h.id)
                    put("title", h.title)
                    put("description", h.description)
                    put("category", h.category.name)
                    put("iconName", h.iconName)
                    put("colorHex", h.colorHex)
                    put("reminderTime", h.reminderTime ?: JSONObject.NULL)
                    put("repeatDays", h.repeatDays)
                    put("goalValue", h.goalValue)
                    put("goalUnit", h.goalUnit)
                    put("isPaused", h.isPaused)
                    put("orderIndex", h.orderIndex)
                    put("createdAt", h.createdAt)
                }
                habitsArray.put(hObj)
            }
            root.put("habits", habitsArray)

            // 4. Habit Completions
            val completions = database.habitCompletionDao().getAllCompletionsForUserOnce(currentUid)
            val completionsArray = JSONArray()
            for (c in completions) {
                val cObj = JSONObject().apply {
                    put("habitId", c.habitId)
                    put("date", c.date)
                    put("status", c.status.name)
                    put("completedAt", c.completedAt)
                    put("numericProgress", c.numericProgress)
                }
                completionsArray.put(cObj)
            }
            root.put("habitCompletions", completionsArray)

            // 5. Water Records & Water Entries
            val waterRecords = database.waterDao().getAllWaterRecordsOnce(currentUid)
            val waterRecordsArray = JSONArray()
            for (w in waterRecords) {
                val wObj = JSONObject().apply {
                    put("date", w.date)
                    put("currentMl", w.currentMl)
                    put("targetMl", w.targetMl)
                }
                waterRecordsArray.put(wObj)
            }
            root.put("waterRecords", waterRecordsArray)

            val waterEntries = database.waterDao().getAllWaterEntriesOnce(currentUid)
            val waterEntriesArray = JSONArray()
            for (e in waterEntries) {
                val eObj = JSONObject().apply {
                    put("id", e.id)
                    put("date", e.date)
                    put("amountMl", e.amountMl)
                    put("timestamp", e.timestamp)
                }
                waterEntriesArray.put(eObj)
            }
            root.put("waterEntries", waterEntriesArray)

            // 6. Focus Sessions
            val focusSessions = database.focusDao().getAllFocusSessionsOnce(currentUid)
            val focusArray = JSONArray()
            for (f in focusSessions) {
                val fObj = JSONObject().apply {
                    put("id", f.id)
                    put("habitId", f.habitId ?: JSONObject.NULL)
                    put("habitTitle", f.habitTitle)
                    put("durationMinutes", f.durationMinutes)
                    put("completedMinutes", f.completedMinutes)
                    put("timestamp", f.timestamp)
                    put("isCompleted", f.isCompleted)
                }
                focusArray.put(fObj)
            }
            root.put("focusSessions", focusArray)

            // 7. Night Reviews
            val nightReviews = database.nightReviewDao().getAllNightReviewsOnce(currentUid)
            val reviewsArray = JSONArray()
            for (r in nightReviews) {
                val rObj = JSONObject().apply {
                    put("date", r.date)
                    put("completedHabits", r.completedHabits)
                    put("totalHabits", r.totalHabits)
                    put("completionPercentage", r.completionPercentage)
                    put("streak", r.streak)
                    put("waterIntake", r.waterIntake)
                    put("waterGoal", r.waterGoal)
                    put("focusMinutes", r.focusMinutes)
                    put("wentWell", r.wentWell)
                    put("improvement", r.improvement)
                    put("reflection", r.reflection)
                    put("rating", r.rating)
                    put("createdAt", r.createdAt)
                    put("updatedAt", r.updatedAt)
                }
                reviewsArray.put(rObj)
            }
            root.put("nightReviews", reviewsArray)

            // 8. Achievements (unlocked)
            val unlockedAchievements = database.achievementDao().getUnlockedAchievementsOnce()
            val achievementsArray = JSONArray()
            for (a in unlockedAchievements) {
                val aObj = JSONObject().apply {
                    put("id", a.id)
                    put("title", a.title)
                    put("description", a.description)
                    put("icon", a.icon)
                    put("isUnlocked", a.isUnlocked)
                    put("unlockedAt", a.unlockedAt ?: JSONObject.NULL)
                }
                achievementsArray.put(aObj)
            }
            root.put("achievements", achievementsArray)

            // 9. Centralized Smart Notifications
            val notifs = database.notificationDao().getNotificationsOnce(currentUid)
            val notifsArray = JSONArray()
            for (n in notifs) {
                val nObj = JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("message", n.message)
                    put("type", n.type.name)
                    put("targetDestination", n.targetDestination)
                    put("relatedEntityId", n.relatedEntityId ?: JSONObject.NULL)
                    put("timestamp", n.timestamp)
                    put("isRead", n.isRead)
                    put("scheduledDate", n.scheduledDate)
                }
                notifsArray.put(nObj)
            }
            root.put("notifications", notifsArray)

            // 10. Preferences & Appearance
            val prefs = context.getSharedPreferences("dayforge_prefs", Context.MODE_PRIVATE)
            val prefsObj = JSONObject().apply {
                put("launchAnimationEnabled", prefs.getBoolean("launch_animation_enabled", true))
                put("reducedMotionEnabled", prefs.getBoolean("reduced_motion_enabled", false))
            }
            root.put("preferences", prefsObj)

            Result.success(root.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Error generating backup JSON: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Saves raw JSON data to a destination Uri chosen via ActivityResultContracts.CreateDocument.
     */
    suspend fun writeJsonToUri(context: Context, jsonString: String, destinationUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(jsonString)
                    writer.flush()
                }
            } ?: return@withContext Result.failure(Exception("Cannot open destination stream for $destinationUri"))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing backup to URI: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Creates an automatic safety snapshot of the current user data before any restore.
     * Saved to context.filesDir/backups/DayForge_Safety_Snapshot_...json
     */
    suspend fun createSafetySnapshot(context: Context, currentUid: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val jsonResult = createBackupJson(context, currentUid)
            if (jsonResult.isFailure) {
                return@withContext Result.failure(jsonResult.exceptionOrNull() ?: Exception("Failed to generate safety snapshot"))
            }
            val json = jsonResult.getOrThrow()
            val backupDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
            val fileName = "DayForge_Safety_Snapshot_${System.currentTimeMillis()}.json"
            val snapshotFile = File(backupDir, fileName)
            snapshotFile.writeText(json, Charsets.UTF_8)
            Log.i(TAG, "Safety snapshot successfully saved to ${snapshotFile.absolutePath}")
            Result.success(snapshotFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create safety snapshot: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Reads a backup file from a selected Uri and validates its structure, version, and content.
     */
    suspend fun validateBackupFromUri(context: Context, uri: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line).append("\n")
                    }
                }
            } ?: return@withContext Result.failure(Exception("Failed to read from selected file"))

            val rawJson = sb.toString()
            if (rawJson.isBlank()) {
                return@withContext Result.failure(Exception("The selected file is empty."))
            }

            val root = try {
                JSONObject(rawJson)
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("The selected file is not a valid JSON document: ${e.message}"))
            }

            // Verify identity: app or format or habits presence
            val app = root.optString("app", "")
            val format = root.optString("format", "")
            val hasDayForgeIdentity = app.equals(BACKUP_APP_NAME, ignoreCase = true) ||
                    format.equals(BACKUP_FORMAT, ignoreCase = true) ||
                    root.has("habits") || root.has("habitCompletions")

            if (!hasDayForgeIdentity) {
                return@withContext Result.failure(
                    Exception("Unsupported file. This file does not appear to be a DayForge backup file.")
                )
            }

            val version = root.optInt("version", 1)
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
            val originalUserId = root.optString("userId", "unknown")

            val habitsCount = root.optJSONArray("habits")?.length() ?: 0
            val completionsCount = root.optJSONArray("habitCompletions")?.length() ?: 0
            val waterRecordsCount = root.optJSONArray("waterRecords")?.length() ?: 0
            val waterEntriesCount = root.optJSONArray("waterEntries")?.length() ?: 0
            val nightReviewsCount = root.optJSONArray("nightReviews")?.length() ?: 0
            val focusSessionsCount = root.optJSONArray("focusSessions")?.length() ?: 0
            val achievementsCount = root.optJSONArray("achievements")?.length() ?: 0
            val notificationsCount = root.optJSONArray("notifications")?.length() ?: 0
            val hasProfile = root.has("userProfile")
            val hasReminders = root.has("reminderSettings")
            val hasPreferences = root.has("preferences")

            val summary = BackupSummary(
                version = version,
                exportedAt = exportedAt,
                originalUserId = originalUserId,
                habitsCount = habitsCount,
                completionsCount = completionsCount,
                waterRecordsCount = waterRecordsCount,
                waterEntriesCount = waterEntriesCount,
                nightReviewsCount = nightReviewsCount,
                focusSessionsCount = focusSessionsCount,
                achievementsCount = achievementsCount,
                notificationsCount = notificationsCount,
                hasProfile = hasProfile,
                hasReminders = hasReminders,
                hasPreferences = hasPreferences,
                rawJson = rawJson
            )
            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Validation failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Executes the restore process safely.
     * Strictly rewrites all user identifiers to `currentUid` to ensure security and privacy.
     */
    suspend fun restoreBackup(
        context: Context,
        currentUid: String,
        rawJson: String,
        autoSafetySnapshot: Boolean = true
    ): Result<BackupRestoreReport> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Automatic Safety Snapshot
            var safetySnapshotPath: String? = null
            if (autoSafetySnapshot) {
                val snapshotRes = createSafetySnapshot(context, currentUid)
                if (snapshotRes.isSuccess) {
                    safetySnapshotPath = snapshotRes.getOrNull()?.absolutePath
                }
            }

            val root = JSONObject(rawJson)

            // Step 2: Wipe current user records to allow clean restore
            database.habitDao().deleteAllHabitsForUser(currentUid)
            database.habitCompletionDao().deleteAllCompletionsForUser(currentUid)
            database.waterDao().deleteAllWaterForUser(currentUid)
            database.waterDao().deleteAllWaterEntriesForUser(currentUid)
            database.focusDao().deleteAllFocusForUser(currentUid)
            database.nightReviewDao().deleteAllNightReviewsForUser(currentUid)
            database.notificationDao().clearAllForUser(currentUid)

            // Step 3: Restore Habits
            val habitsArray = root.optJSONArray("habits")
            val habitsToInsert = mutableListOf<Habit>()
            if (habitsArray != null) {
                for (i in 0 until habitsArray.length()) {
                    val obj = habitsArray.getJSONObject(i)
                    val catName = obj.optString("category", "HEALTH")
                    val cat = try { HabitCategory.valueOf(catName) } catch (_: Exception) { HabitCategory.HEALTH }
                    val habit = Habit(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = currentUid, // Enforce current user
                        title = obj.optString("title", "Untitled Habit"),
                        description = obj.optString("description", ""),
                        category = cat,
                        iconName = obj.optString("iconName", "check_circle"),
                        colorHex = obj.optLong("colorHex", 0xFF10B981),
                        reminderTime = if (obj.isNull("reminderTime")) null else obj.optString("reminderTime"),
                        repeatDays = obj.optString("repeatDays", "1,2,3,4,5,6,7"),
                        goalValue = obj.optInt("goalValue", 1),
                        goalUnit = obj.optString("goalUnit", "times"),
                        isPaused = obj.optBoolean("isPaused", false),
                        orderIndex = obj.optInt("orderIndex", i),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    habitsToInsert.add(habit)
                }
                if (habitsToInsert.isNotEmpty()) {
                    database.habitDao().insertHabits(habitsToInsert)
                }
            }

            // Step 4: Restore Habit Completions
            val completionsArray = root.optJSONArray("habitCompletions")
            val completionsToInsert = mutableListOf<HabitCompletion>()
            if (completionsArray != null) {
                for (i in 0 until completionsArray.length()) {
                    val obj = completionsArray.getJSONObject(i)
                    val statusName = obj.optString("status", "COMPLETED")
                    val status = try { CompletionStatus.valueOf(statusName) } catch (_: Exception) { CompletionStatus.COMPLETED }
                    val comp = HabitCompletion(
                        habitId = obj.optString("habitId", ""),
                        userId = currentUid, // Enforce current user
                        date = obj.optString("date", ""),
                        status = status,
                        completedAt = obj.optLong("completedAt", System.currentTimeMillis()),
                        numericProgress = obj.optInt("numericProgress", 1)
                    )
                    if (comp.habitId.isNotBlank() && comp.date.isNotBlank()) {
                        completionsToInsert.add(comp)
                    }
                }
                if (completionsToInsert.isNotEmpty()) {
                    database.habitCompletionDao().insertCompletions(completionsToInsert)
                }
            }

            // Step 5: Restore Water Records & Entries
            val waterRecordsArray = root.optJSONArray("waterRecords")
            val waterRecordsToInsert = mutableListOf<WaterRecord>()
            if (waterRecordsArray != null) {
                for (i in 0 until waterRecordsArray.length()) {
                    val obj = waterRecordsArray.getJSONObject(i)
                    val record = WaterRecord(
                        userId = currentUid,
                        date = obj.optString("date", ""),
                        currentMl = obj.optInt("currentMl", 0),
                        targetMl = obj.optInt("targetMl", 2500)
                    )
                    if (record.date.isNotBlank()) {
                        waterRecordsToInsert.add(record)
                    }
                }
                if (waterRecordsToInsert.isNotEmpty()) {
                    database.waterDao().insertOrUpdateWaterRecords(waterRecordsToInsert)
                }
            }

            val waterEntriesArray = root.optJSONArray("waterEntries")
            val waterEntriesToInsert = mutableListOf<WaterEntry>()
            if (waterEntriesArray != null) {
                for (i in 0 until waterEntriesArray.length()) {
                    val obj = waterEntriesArray.getJSONObject(i)
                    val entry = WaterEntry(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = currentUid,
                        date = obj.optString("date", ""),
                        amountMl = obj.optInt("amountMl", 250),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    if (entry.date.isNotBlank()) {
                        waterEntriesToInsert.add(entry)
                    }
                }
                if (waterEntriesToInsert.isNotEmpty()) {
                    database.waterDao().insertWaterEntries(waterEntriesToInsert)
                }
            }

            // Step 6: Restore Focus Sessions
            val focusArray = root.optJSONArray("focusSessions")
            val focusToInsert = mutableListOf<FocusSession>()
            if (focusArray != null) {
                for (i in 0 until focusArray.length()) {
                    val obj = focusArray.getJSONObject(i)
                    val session = FocusSession(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = currentUid,
                        habitId = if (obj.isNull("habitId")) null else obj.optString("habitId"),
                        habitTitle = obj.optString("habitTitle", "Deep Work"),
                        durationMinutes = obj.optInt("durationMinutes", 25),
                        completedMinutes = obj.optInt("completedMinutes", 25),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isCompleted = obj.optBoolean("isCompleted", true)
                    )
                    focusToInsert.add(session)
                }
                if (focusToInsert.isNotEmpty()) {
                    database.focusDao().insertFocusSessions(focusToInsert)
                }
            }

            // Step 7: Restore Night Reviews
            val reviewsArray = root.optJSONArray("nightReviews")
            val reviewsToInsert = mutableListOf<NightReviewEntry>()
            if (reviewsArray != null) {
                for (i in 0 until reviewsArray.length()) {
                    val obj = reviewsArray.getJSONObject(i)
                    val review = NightReviewEntry(
                        date = obj.optString("date", ""),
                        userId = currentUid,
                        completedHabits = obj.optInt("completedHabits", 0),
                        totalHabits = obj.optInt("totalHabits", 0),
                        completionPercentage = obj.optInt("completionPercentage", 0),
                        streak = obj.optInt("streak", 0),
                        waterIntake = obj.optInt("waterIntake", 0),
                        waterGoal = obj.optInt("waterGoal", 2500),
                        focusMinutes = obj.optInt("focusMinutes", 0),
                        wentWell = obj.optString("wentWell", ""),
                        improvement = obj.optString("improvement", ""),
                        reflection = obj.optString("reflection", ""),
                        rating = obj.optInt("rating", 5),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                    if (review.date.isNotBlank()) {
                        reviewsToInsert.add(review)
                    }
                }
                if (reviewsToInsert.isNotEmpty()) {
                    database.nightReviewDao().insertNightReviews(reviewsToInsert)
                }
            }

            // Step 8: Restore Achievements
            val achievementsArray = root.optJSONArray("achievements")
            if (achievementsArray != null) {
                for (i in 0 until achievementsArray.length()) {
                    val obj = achievementsArray.getJSONObject(i)
                    val id = obj.optString("id", "")
                    val isUnlocked = obj.optBoolean("isUnlocked", false)
                    val unlockedAt = if (obj.isNull("unlockedAt")) null else obj.optLong("unlockedAt")
                    if (id.isNotBlank() && isUnlocked) {
                        val ach = database.achievementDao().getAllAchievementsOnceOrNull()?.find { it.id == id }
                        if (ach != null) {
                            database.achievementDao().updateAchievement(ach.copy(isUnlocked = true, unlockedAt = unlockedAt ?: System.currentTimeMillis()))
                        }
                    }
                }
            }

            // Step 9: Restore Smart Notifications
            val notifsArray = root.optJSONArray("notifications")
            val notifsToInsert = mutableListOf<AppNotification>()
            if (notifsArray != null) {
                for (i in 0 until notifsArray.length()) {
                    val obj = notifsArray.getJSONObject(i)
                    val typeName = obj.optString("type", "SYSTEM")
                    val notifType = try { NotificationType.valueOf(typeName) } catch (_: Exception) { NotificationType.SYSTEM }
                    val notif = AppNotification(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = currentUid,
                        title = obj.optString("title", "DayForge Update"),
                        message = obj.optString("message", ""),
                        type = notifType,
                        targetDestination = obj.optString("targetDestination", "home"),
                        relatedEntityId = if (obj.isNull("relatedEntityId")) null else obj.optString("relatedEntityId"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isRead = obj.optBoolean("isRead", false),
                        scheduledDate = obj.optString("scheduledDate", "")
                    )
                    notifsToInsert.add(notif)
                }
                if (notifsToInsert.isNotEmpty()) {
                    database.notificationDao().insertAll(notifsToInsert)
                }
            }

            // Step 10: Restore Preferences
            if (root.has("preferences")) {
                val prefObj = root.getJSONObject("preferences")
                val prefs = context.getSharedPreferences("dayforge_prefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putBoolean("launch_animation_enabled", prefObj.optBoolean("launchAnimationEnabled", true))
                    putBoolean("reduced_motion_enabled", prefObj.optBoolean("reducedMotionEnabled", false))
                    apply()
                }
            }

            // Step 11: Restore User Profile Details (bio, photo, public status)
            var profileRestored = false
            if (root.has("userProfile")) {
                val profileObj = root.getJSONObject("userProfile")
                val existing = database.userProfileDao().getUserProfileOnce(currentUid)
                if (existing != null) {
                    val updated = existing.copy(
                        bio = profileObj.optString("bio", existing.bio),
                        photoUrl = profileObj.optString("photoUrl", existing.photoUrl),
                        publicProfileEnabled = profileObj.optBoolean("publicProfileEnabled", existing.publicProfileEnabled),
                        disciplineScore = profileObj.optInt("disciplineScore", existing.disciplineScore),
                        currentStreak = profileObj.optInt("currentStreak", existing.currentStreak),
                        bestStreak = profileObj.optInt("bestStreak", existing.bestStreak)
                    )
                    database.userProfileDao().insertOrUpdate(updated)
                    profileRestored = true
                }
            }

            // Step 12: Sync with Cloud if authenticated
            if (!currentUid.startsWith("guest_")) {
                try {
                    // Sync habits
                    for (h in habitsToInsert) {
                        firebaseManager.syncHabitToCloud(currentUid, h)
                    }
                    // Sync completions
                    for (c in completionsToInsert) {
                        firebaseManager.syncCompletionToCloud(currentUid, c)
                    }
                    // Sync water
                    for (w in waterRecordsToInsert) {
                        firebaseManager.syncWaterToCloud(currentUid, w)
                    }
                    // Sync focus
                    for (f in focusToInsert) {
                        firebaseManager.syncFocusSessionToCloud(currentUid, f)
                    }
                    // Sync reviews
                    for (r in reviewsToInsert) {
                        firebaseManager.syncNightReviewToCloud(currentUid, r)
                    }
                    // Sync notifications
                    for (n in notifsToInsert) {
                        firebaseManager.saveNotificationToCloud(currentUid, n)
                    }
                    // Sync profile
                    val finalProfile = database.userProfileDao().getUserProfileOnce(currentUid)
                    if (finalProfile != null) {
                        firebaseManager.syncUserProfileToCloud(finalProfile)
                    }
                    Log.i(TAG, "Restored records successfully queued to cloud.")
                } catch (cloudErr: Exception) {
                    Log.w(TAG, "Cloud sync following restore encountered non-fatal error: ${cloudErr.message}")
                }
            }

            val report = BackupRestoreReport(
                habitsRestored = habitsToInsert.size,
                completionsRestored = completionsToInsert.size,
                waterRecordsRestored = waterRecordsToInsert.size,
                focusSessionsRestored = focusToInsert.size,
                nightReviewsRestored = reviewsToInsert.size,
                notificationsRestored = notifsToInsert.size,
                profileRestored = profileRestored,
                safetySnapshotPath = safetySnapshotPath
            )
            Result.success(report)
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed with exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun com.example.data.local.AchievementDao.getAllAchievementsOnceOrNull(): List<Achievement>? {
        return try {
            getUnlockedAchievementsOnce()
        } catch (_: Exception) {
            null
        }
    }
}
