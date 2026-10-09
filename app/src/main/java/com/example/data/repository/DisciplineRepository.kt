package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.backup.*
import com.example.data.local.AppDatabase
import com.example.data.models.*
import com.example.data.notifications.DisciplineNotificationManager
import com.example.data.notifications.WaterReminderManager
import com.example.data.remote.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.roundToInt

private val Context.dataStore by preferencesDataStore(name = "discipline_prefs")

class DisciplineRepository(
    private val context: Context,
    private val database: AppDatabase,
    val firebaseManager: FirebaseManager
) {
    companion object {
        private const val TAG = "DisciplineRepository"
    }

    val backupManager = BackupManager(database, firebaseManager)

    private val scope = CoroutineScope(Dispatchers.IO)
    private val KEY_CURRENT_UID = stringPreferencesKey("active_user_uid")
    private var activeUserDataListener: com.google.firebase.database.ValueEventListener? = null
    private var activeListenerUid: String? = null

    val reminderManager = WaterReminderManager(context)
    val notificationManager = DisciplineNotificationManager(context)
    val challengeAlarmManager = com.example.alarm.ChallengeAlarmManager(context)
    private val _waterReminderSettings = MutableStateFlow(WaterReminderSettings())
    val waterReminderSettings: StateFlow<WaterReminderSettings> = _waterReminderSettings.asStateFlow()
    private val _reminderSettings = MutableStateFlow(ReminderSettings())
    val reminderSettings: StateFlow<ReminderSettings> = _reminderSettings.asStateFlow()
    private val _focusSettings = MutableStateFlow(FocusSettings())
    val focusSettings: StateFlow<FocusSettings> = _focusSettings.asStateFlow()
    private val _journalEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val journalEntries: StateFlow<List<JournalEntry>> = _journalEntries.asStateFlow()

    private val _disciplineScoreBreakdown = MutableStateFlow(DisciplineScoreBreakdown())
    val disciplineScoreBreakdown: StateFlow<DisciplineScoreBreakdown> = _disciplineScoreBreakdown.asStateFlow()

    // Auth state flow directly synchronized with Firebase Auth + local datastore
    val authStateFlow: Flow<String?> = firebaseManager.authStateFlow().map { firebaseUser ->
        firebaseUser?.uid
    }

    val activeUidFlow: Flow<String?> = combine(
        context.dataStore.data.map { it[KEY_CURRENT_UID] },
        authStateFlow
    ) { storedUid, firebaseUid ->
        if (firebaseUid != null) {
            firebaseUid
        } else if (storedUid?.startsWith("guest_") == true) {
            storedUid
        } else {
            null
        }
    }.distinctUntilChanged()

    init {
        // Observe dynamic auth changes (e.g. user logs out during session)
        scope.launch {
            authStateFlow.collect { uid ->
                if (uid != null) {
                    val currentStored = getStoredActiveUidOnce()
                    if (currentStored != uid) {
                        setActiveUid(uid)
                    }
                    setupRealtimeSyncListener(uid)
                } else {
                    cleanupRealtimeSyncListener()
                    val stored = getStoredActiveUidOnce()
                    if (stored != null && !stored.startsWith("guest_")) {
                        setActiveUid(null)
                    }
                }
            }
        }
    }

    private fun setupRealtimeSyncListener(uid: String) {
        if (uid.startsWith("guest_")) return
        if (activeListenerUid == uid && activeUserDataListener != null) return
        cleanupRealtimeSyncListener()
        activeListenerUid = uid
        activeUserDataListener = firebaseManager.attachUserDataListener(uid) {
            scope.launch {
                // Background cloud sync on remote data changes
                try {
                    val profile = firebaseManager.fetchUserProfile(uid)
                    if (profile != null) {
                        database.userProfileDao().insertOrUpdate(profile)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Realtime listener sync error: ${e.message}")
                }
            }
        }
    }

    private fun cleanupRealtimeSyncListener() {
        val uid = activeListenerUid
        val listener = activeUserDataListener
        if (uid != null && listener != null) {
            firebaseManager.removeUserDataListener(uid, listener)
        }
        activeUserDataListener = null
        activeListenerUid = null
    }

    suspend fun setActiveUid(uid: String?) {
        context.dataStore.edit { prefs ->
            if (uid != null) {
                prefs[KEY_CURRENT_UID] = uid
            } else {
                prefs.remove(KEY_CURRENT_UID)
            }
        }
    }

    // --- Authentication ---
    suspend fun continueAsGuest(): Result<String> {
        val guestUid = "guest_" + UUID.randomUUID().toString().replace("-", "").take(8)
        setActiveUid(guestUid)
        val guestUser = UserAccount(
            uid = guestUid,
            email = "guest@discipline.app",
            username = "guest_${(1000..9999).random()}",
            displayName = "Guest Warrior",
            bio = "Relentless daily discipline & consistency.",
            photoUrl = "",
            disciplineScore = 0,
            isProfileComplete = true
        )
        database.userProfileDao().insertOrUpdate(guestUser)
        seedDefaultAchievements()
        seedInitialHabits(guestUid)
        return Result.success(guestUid)
    }

    suspend fun handleSuccessfulAuth(uid: String) {
        if (uid.startsWith("guest_")) return
        setActiveUid(uid)
        val fbUser = firebaseManager.getCurrentFirebaseUser()

        // 1. Fetch user profile from Realtime Database
        val cloudProfile = firebaseManager.fetchUserProfile(uid)
        if (cloudProfile != null) {
            // Existing user! Restore exact profile and data without resetting progress
            database.userProfileDao().insertOrUpdate(cloudProfile)
            restoreCloudUserData(uid)
        } else {
            // Check local Room cache
            val localProfile = database.userProfileDao().getUserProfileOnce(uid)
            if (localProfile != null && localProfile.isProfileComplete && localProfile.username.isNotBlank()) {
                database.userProfileDao().insertOrUpdate(localProfile)
                firebaseManager.syncUserProfileToCloud(localProfile)
                restoreCloudUserData(uid)
            } else {
                // Brand new user -> initial profile with blank username to route to Profile Setup
                val newUser = UserAccount(
                    uid = uid,
                    email = fbUser?.email ?: "",
                    displayName = fbUser?.displayName ?: (fbUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Warrior"),
                    username = "",
                    bio = "Relentless daily discipline & consistency.",
                    photoUrl = fbUser?.photoUrl?.toString() ?: "",
                    isProfileComplete = false
                )
                database.userProfileDao().insertOrUpdate(newUser)
                firebaseManager.syncUserProfileToCloud(newUser)
                seedDefaultAchievements()
                seedInitialHabits(uid)
            }
        }
        setupRealtimeSyncListener(uid)
    }

    suspend fun signUp(email: String, pass: String): Result<String> {
        val result = firebaseManager.signUpWithEmail(email, pass)
        if (result.isSuccess) {
            val uid = result.getOrThrow()
            handleSuccessfulAuth(uid)
        }
        return result
    }

    suspend fun login(email: String, pass: String): Result<String> {
        val result = firebaseManager.loginWithEmail(email, pass)
        if (result.isSuccess) {
            val uid = result.getOrThrow()
            handleSuccessfulAuth(uid)
        }
        return result
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return firebaseManager.sendPasswordReset(email)
    }

    suspend fun logout() {
        cleanupRealtimeSyncListener()
        firebaseManager.logout()
        setActiveUid(null)
    }

    suspend fun getStoredActiveUidOnce(): String? {
        return try {
            context.dataStore.data.firstOrNull()?.get(KEY_CURRENT_UID)
        } catch (e: Exception) {
            Log.w(TAG, "getStoredActiveUidOnce error: ${e.message}")
            null
        }
    }

    suspend fun getCurrentUserId(): String? = getStoredActiveUidOnce() ?: firebaseManager.getCurrentFirebaseUser()?.uid

    suspend fun ensureLocalProfileExists(uid: String, email: String? = null, displayName: String? = null) {
        withContext(Dispatchers.IO) {
            try {
                val existing = database.userProfileDao().getUserProfileOnce(uid)
                if (existing == null) {
                    val fbUser = firebaseManager.getCurrentFirebaseUser()
                    val userEmail = email ?: fbUser?.email ?: ""
                    val userDisplayName = displayName ?: fbUser?.displayName ?: (userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }.ifBlank { "User" })
                    val defaultUser = UserAccount(
                        uid = uid,
                        email = userEmail,
                        displayName = userDisplayName,
                        username = "",
                        isProfileComplete = false
                    )
                    database.userProfileDao().insertOrUpdate(defaultUser)
                    Log.d(TAG, "Created fallback local profile for uid: $uid")
                }
            } catch (e: Exception) {
                Log.w(TAG, "ensureLocalProfileExists error: ${e.message}")
            }
        }
    }

    suspend fun ensureGuestProfileExists(guestUid: String) {
        withContext(Dispatchers.IO) {
            try {
                val existing = database.userProfileDao().getUserProfileOnce(guestUid)
                if (existing == null) {
                    val guestUser = UserAccount(
                        uid = guestUid,
                        email = "guest@discipline.local",
                        displayName = "Guest Achiever",
                        username = "guest_${guestUid.takeLast(4)}",
                        isProfileComplete = true,
                        disciplineScore = 0
                    )
                    database.userProfileDao().insertOrUpdate(guestUser)
                    seedDefaultAchievements()
                    seedInitialHabits(guestUid)
                    Log.d(TAG, "Created guest profile for uid: $guestUid")
                }
            } catch (e: Exception) {
                Log.w(TAG, "ensureGuestProfileExists error: ${e.message}")
            }
        }
    }

    // --- Restore Cloud User Data from Realtime Database ---
    suspend fun restoreCloudUserData(uid: String) = withContext(Dispatchers.IO) {
        if (uid.startsWith("guest_")) return@withContext
        try {
            // 1. Restore Profile (isolated with safe fallback)
            try {
                val cloudProfile = firebaseManager.fetchUserProfile(uid)
                if (cloudProfile != null) {
                    database.userProfileDao().insertOrUpdate(cloudProfile)
                } else {
                    val local = database.userProfileDao().getUserProfileOnce(uid)
                    if (local == null) {
                        val fbUser = firebaseManager.getCurrentFirebaseUser()
                        val user = UserAccount(
                            uid = uid,
                            email = fbUser?.email ?: "",
                            displayName = fbUser?.displayName ?: (fbUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User"),
                            username = "",
                            isProfileComplete = false
                        )
                        database.userProfileDao().insertOrUpdate(user)
                        scope.launch {
                            try {
                                firebaseManager.syncUserProfileToCloud(user)
                            } catch (e: Exception) {
                                Log.w(TAG, "Background sync profile: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Profile restore error: ${e.message}")
                ensureLocalProfileExists(uid)
            }

            // 2. Restore Habits (isolated)
            try {
                val cloudHabits = firebaseManager.fetchUserHabits(uid)
                if (cloudHabits.isNotEmpty()) {
                    database.habitDao().deleteAllHabitsForUser(uid)
                    database.habitDao().insertHabits(cloudHabits)
                } else {
                    // If cloud is empty and local is also empty, seed starter habits
                    val localHabits = database.habitDao().getHabitsForUserOnce(uid)
                    if (localHabits.isEmpty()) {
                        seedInitialHabits(uid)
                    } else {
                        localHabits.forEach { firebaseManager.syncHabitToCloud(uid, it) }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Habits restore error: ${e.message}")
            }

            // 3. Restore Completions (isolated)
            try {
                val cloudCompletions = firebaseManager.fetchUserCompletions(uid)
                if (cloudCompletions.isNotEmpty()) {
                    database.habitCompletionDao().deleteAllCompletionsForUser(uid)
                    cloudCompletions.forEach { comp ->
                        database.habitCompletionDao().insertCompletion(comp)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Completions restore error: ${e.message}")
            }

            // 4. Restore Water Records & Entries (isolated)
            try {
                val (cloudWater, cloudEntries) = firebaseManager.fetchUserWaterData(uid)
                if (cloudWater.isNotEmpty()) {
                    database.waterDao().deleteAllWaterForUser(uid)
                    database.waterDao().insertOrUpdateWaterRecords(cloudWater)
                }
                if (cloudEntries.isNotEmpty()) {
                    database.waterDao().deleteAllWaterEntriesForUser(uid)
                    database.waterDao().insertWaterEntries(cloudEntries)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Water data restore error: ${e.message}")
            }

            // 4b. Restore Water Goal Setting (from /users/{uid}/settings/waterGoal) (isolated)
            try {
                val cloudGoal = firebaseManager.fetchWaterGoalSetting(uid)
                val effectiveGoal = if (cloudGoal != null && cloudGoal > 0) cloudGoal else (getUserWaterGoalOnce(uid) ?: 3000)
                val goalKey = androidx.datastore.preferences.core.intPreferencesKey("user_water_goal_$uid")
                context.dataStore.edit { prefs ->
                    if (prefs[goalKey] == null || prefs[goalKey]!! <= 0) {
                        prefs[goalKey] = effectiveGoal
                    }
                }
                // If today's water record has no goal set (0), apply this user setting to today's record
                val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val todayRecord = database.waterDao().getWaterRecordOnce(uid, todayStr)
                if (todayRecord != null && todayRecord.targetMl <= 0) {
                    val updated = todayRecord.copy(targetMl = effectiveGoal)
                    database.waterDao().insertOrUpdateWater(updated)
                    scope.launch {
                        try {
                            firebaseManager.syncWaterToCloud(uid, updated)
                        } catch (e: Exception) {
                            Log.w(TAG, "Sync water to cloud: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Water goal restore error: ${e.message}")
            }

            // 4c. Restore Complete Reminder Settings (/users/{uid}/settings/reminders/) (isolated)
            try {
                val cloudReminderSettings = firebaseManager.fetchReminderSettings(uid)
                if (cloudReminderSettings != null) {
                    _reminderSettings.value = cloudReminderSettings
                    _waterReminderSettings.value = cloudReminderSettings.waterReminders
                    if (cloudReminderSettings.waterReminders.isEnabled) {
                        notificationManager.scheduleWaterReminders(cloudReminderSettings.waterReminders)
                        reminderManager.scheduleReminders(cloudReminderSettings.waterReminders)
                    }
                    if (cloudReminderSettings.nightReview.isEnabled) {
                        notificationManager.scheduleNightReview(cloudReminderSettings.nightReview)
                    }
                } else {
                    val legacyWater = firebaseManager.fetchWaterReminderSettings(uid)
                    if (legacyWater != null) {
                        _waterReminderSettings.value = legacyWater
                        _reminderSettings.value = ReminderSettings(waterReminders = legacyWater)
                        if (legacyWater.isEnabled) {
                            reminderManager.scheduleReminders(legacyWater)
                            notificationManager.scheduleWaterReminders(legacyWater)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Reminder settings restore error: ${e.message}")
            }

            // 5. Restore Focus Sessions (isolated)
            try {
                val cloudFocus = firebaseManager.fetchUserFocusSessions(uid)
                if (cloudFocus.isNotEmpty()) {
                    database.focusDao().deleteAllFocusForUser(uid)
                    cloudFocus.forEach { s ->
                        database.focusDao().insertFocusSession(s)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Focus sessions restore error: ${e.message}")
            }

            // 5b. Restore Focus Settings (Step 20)
            try {
                val cloudFocusSettings = firebaseManager.fetchFocusSettings(uid)
                if (cloudFocusSettings != null) {
                    _focusSettings.value = cloudFocusSettings
                }
            } catch (e: Exception) {
                Log.w(TAG, "Focus settings restore error: ${e.message}")
            }

            // 5c. Restore Journal Entries
            try {
                val cloudJournal = firebaseManager.fetchUserJournalEntries(uid)
                if (cloudJournal.isNotEmpty()) {
                    _journalEntries.value = cloudJournal
                } else {
                    _journalEntries.value = getDefaultJournalEntries(uid)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Journal restore error: ${e.message}")
                if (_journalEntries.value.isEmpty()) {
                    _journalEntries.value = getDefaultJournalEntries(uid)
                }
            }

            // 6. Restore Achievements (isolated)
            try {
                val cloudAchievements = firebaseManager.fetchUserAchievements(uid)
                if (cloudAchievements.isNotEmpty()) {
                    database.achievementDao().clearAllAchievements()
                    database.achievementDao().insertAchievements(cloudAchievements)
                }
                seedDefaultAchievements()
            } catch (e: Exception) {
                Log.w(TAG, "Achievements restore error: ${e.message}")
            }

            // 7. Restore Night Reviews (/users/{uid}/nightReviews/) (isolated)
            try {
                val cloudReviews = firebaseManager.fetchAllNightReviews(uid)
                if (cloudReviews.isNotEmpty()) {
                    database.nightReviewDao().deleteAllNightReviewsForUser(uid)
                    database.nightReviewDao().insertNightReviews(cloudReviews)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Night reviews restore error: ${e.message}")
            }

            // 8. Restore Discipline Score breakdown (/users/{uid}/disciplineScore/) (isolated)
            try {
                val cloudScore = firebaseManager.fetchDisciplineScoreFromCloud(uid)
                if (cloudScore != null) {
                    val score = (cloudScore["score"] as? Number)?.toInt() ?: 0
                    val habits = (cloudScore["habitsScore"] as? Number)?.toInt() ?: 0
                    val streak = (cloudScore["streakScore"] as? Number)?.toInt() ?: 0
                    val water = (cloudScore["waterScore"] as? Number)?.toInt() ?: 0
                    val focus = (cloudScore["focusScore"] as? Number)?.toInt() ?: 0
                    val review = (cloudScore["reviewScore"] as? Number)?.toInt() ?: 0
                    _disciplineScoreBreakdown.value = DisciplineScoreBreakdown(
                        score = score,
                        habitsScore = habits,
                        streakScore = streak,
                        waterScore = water,
                        focusScore = focus,
                        reviewScore = review
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Discipline score restore error: ${e.message}")
            }

            // 9. Restore Notifications (/users/{uid}/notifications/) (isolated)
            try {
                syncCloudNotifications(uid)
            } catch (e: Exception) {
                Log.w(TAG, "Notifications restore error: ${e.message}")
            }

            // 10. Restore Challenge Alarms (/users/{uid}/alarms/)
            try {
                val cloudAlarms = firebaseManager.fetchAlarmsFromCloud(uid)
                if (cloudAlarms.isNotEmpty()) {
                    database.challengeAlarmDao().deleteAllAlarmsForUser(uid)
                    database.challengeAlarmDao().insertAlarms(cloudAlarms)
                    for (alarm in cloudAlarms) {
                        if (alarm.isEnabled) {
                            challengeAlarmManager.scheduleAlarm(alarm)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Alarms restore error: ${e.message}")
            }

            // 11. Restore Challenge History (/users/{uid}/challengeHistory/)
            try {
                val cloudHistory = firebaseManager.fetchChallengeHistoryFromCloud(uid)
                if (cloudHistory.isNotEmpty()) {
                    database.challengeHistoryDao().deleteAllHistoryForUser(uid)
                    database.challengeHistoryDao().insertEntries(cloudHistory)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Challenge history restore error: ${e.message}")
            }

            // Recalculate stats with the latest restored data and sync streaks/progress
            try {
                recalculateUserStats(uid)
            } catch (e: Exception) {
                Log.w(TAG, "Recalculate stats error: ${e.message}")
            }
            Log.d(TAG, "Successfully restored user data from Realtime Database for uid: $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring cloud user data: ${e.message}", e)
        }
    }

    // --- User Profile ---
    fun getUserProfile(uid: String): Flow<UserAccount?> {
        return database.userProfileDao().getUserProfile(uid)
    }

    suspend fun checkUsernameAvailability(username: String, currentUid: String): Boolean {
        val clean = username.trim().lowercase()
        if (clean.length < 3 || clean.length > 20) return false
        val validChars = clean.all { it.isLetterOrDigit() || it == '_' }
        if (!validChars) return false

        // Check local DB first
        val localMatch = database.userProfileDao().getUserByUsername(clean)
        if (localMatch != null && localMatch.uid != currentUid) {
            return false
        }
        // Check Realtime Database
        return firebaseManager.isUsernameAvailable(clean, currentUid)
    }

    suspend fun saveUserProfile(
        uid: String,
        username: String,
        displayName: String,
        bio: String,
        photoUrl: String,
        publicProfileEnabled: Boolean? = null
    ): Result<Unit> {
        val cleanUsername = username.trim().lowercase()
        if (cleanUsername.length < 3 || cleanUsername.length > 20) {
            return Result.failure(IllegalArgumentException("Username must be between 3 and 20 characters."))
        }
        val validChars = cleanUsername.all { it.isLetterOrDigit() || it == '_' }
        if (!validChars) {
            return Result.failure(IllegalArgumentException("Username can only contain letters, numbers, and underscores."))
        }

        val isAvailable = checkUsernameAvailability(cleanUsername, uid)
        if (!isAvailable) {
            return Result.failure(IllegalArgumentException("Username @$cleanUsername is already taken by another athlete."))
        }

        val existing = database.userProfileDao().getUserProfileOnce(uid)
        val oldUsername = existing?.username?.trim()?.lowercase()
        if (!oldUsername.isNullOrBlank() && oldUsername != cleanUsername) {
            firebaseManager.releaseOldUsername(oldUsername, uid)
        }

        val finalPublicEnabled = if (uid.startsWith("guest_")) {
            false
        } else {
            publicProfileEnabled ?: existing?.publicProfileEnabled ?: false
        }

        val updated = (existing ?: UserAccount(uid = uid)).copy(
            username = cleanUsername,
            displayName = displayName.trim().ifBlank { cleanUsername },
            bio = bio.trim(),
            photoUrl = photoUrl.trim(),
            publicProfileEnabled = finalPublicEnabled,
            isProfileComplete = true
        )
        database.userProfileDao().insertOrUpdate(updated)

        if (!uid.startsWith("guest_")) {
            try {
                firebaseManager.syncUserProfileToCloud(updated)
                if (updated.publicProfileEnabled) {
                    val unlockedAchievements = try { database.achievementDao().getUnlockedAchievementsOnce() } catch (e: Exception) { emptyList() }
                    try {
                        firebaseManager.syncPublicUserProfile(updated, unlockedAchievements)
                    } catch (e: Exception) {
                        Log.w(TAG, "Public directory sync notice: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing profile to cloud: ${e.message}")
            }
        }

        return Result.success(Unit)
    }

    suspend fun togglePublicProfile(uid: String, enabled: Boolean): Result<Unit> {
        if (uid.startsWith("guest_")) {
            return Result.failure(IllegalStateException("Public discovery is not available in Guest mode. Please sign in or create an account to enable your public profile."))
        }
        val existing = database.userProfileDao().getUserProfileOnce(uid)
            ?: return Result.failure(IllegalStateException("User profile not found."))

        val cleanUsername = existing.username.trim().lowercase()
        if (enabled && cleanUsername.isBlank()) {
            return Result.failure(IllegalStateException("Please set a username in your profile before enabling Public Profile."))
        }

        val updated = existing.copy(
            username = cleanUsername,
            publicProfileEnabled = enabled
        )

        try {
            firebaseManager.syncUserProfileToCloud(updated)
            val unlockedAchievements = try { database.achievementDao().getUnlockedAchievementsOnce() } catch (e: Exception) { emptyList() }
            firebaseManager.syncPublicUserProfile(updated, unlockedAchievements)
            database.userProfileDao().insertOrUpdate(updated)
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "togglePublicProfile cloud update notice: ${e.message}")
            database.userProfileDao().insertOrUpdate(updated)
            return Result.success(Unit)
        }
    }

    suspend fun searchPublicUsers(query: String): List<PublicUserProfile> {
        val cleanQuery = query.trim().trimStart('@').trim().lowercase()
        val remoteResults = try {
            firebaseManager.searchPublicUsers(cleanQuery)
        } catch (e: Exception) {
            emptyList()
        }

        val localProfiles = try {
            val localUsers = if (cleanQuery.isBlank()) {
                database.userProfileDao().getTopPublicProfiles()
            } else {
                database.userProfileDao().searchPublicProfiles(cleanQuery)
            }
            localUsers.filter { it.publicProfileEnabled && !it.uid.startsWith("guest_") }
                .map { u ->
                    PublicUserProfile(
                        uid = u.uid,
                        username = u.username,
                        displayName = u.displayName.ifBlank { u.username },
                        bio = u.bio,
                        photoUrl = u.photoUrl,
                        publicProfileEnabled = true,
                        streak = u.currentStreak,
                        bestStreak = u.bestStreak,
                        disciplineScore = u.disciplineScore,
                        updatedAt = System.currentTimeMillis()
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }

        val combinedMap = linkedMapOf<String, PublicUserProfile>()
        for (p in localProfiles) {
            combinedMap[p.uid] = p
        }
        for (p in remoteResults) {
            combinedMap[p.uid] = p
        }

        return combinedMap.values.sortedWith(
            compareByDescending<PublicUserProfile> { it.username.equals(cleanQuery, ignoreCase = true) }
                .thenByDescending { it.disciplineScore }
        )
    }

    suspend fun fetchPublicProfile(username: String): PublicUserProfile? {
        val cleanUsername = username.trim().trimStart('@').trim().lowercase()
        if (cleanUsername.isBlank()) return null

        val remoteProfile = try {
            firebaseManager.fetchPublicProfileByUsername(cleanUsername)
        } catch (e: Exception) {
            null
        }
        if (remoteProfile != null) return remoteProfile

        return try {
            val localUser = database.userProfileDao().getUserByUsername(cleanUsername)
            if (localUser != null && localUser.publicProfileEnabled && !localUser.uid.startsWith("guest_")) {
                val achievements = try {
                    database.achievementDao().getUnlockedAchievementsOnce()
                } catch (e: Exception) {
                    emptyList()
                }
                PublicUserProfile(
                    uid = localUser.uid,
                    username = localUser.username,
                    displayName = localUser.displayName.ifBlank { localUser.username },
                    bio = localUser.bio,
                    photoUrl = localUser.photoUrl,
                    publicProfileEnabled = true,
                    streak = localUser.currentStreak,
                    bestStreak = localUser.bestStreak,
                    disciplineScore = localUser.disciplineScore,
                    publicAchievements = achievements.map { ach ->
                        PublicAchievement(
                            id = ach.id,
                            title = ach.title,
                            description = ach.description,
                            icon = ach.icon,
                            unlockedAt = ach.unlockedAt ?: 0L
                        )
                    },
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // --- Habits (Unlimited) ---
    fun getHabits(userId: String): Flow<List<Habit>> {
        return database.habitDao().getHabitsForUser(userId)
    }

    suspend fun saveHabit(habit: Habit) {
        val finalHabit = if (habit.id.isBlank()) habit.copy(id = UUID.randomUUID().toString()) else habit
        database.habitDao().insertOrUpdateHabit(finalHabit)
        firebaseManager.syncHabitToCloud(finalHabit.userId, finalHabit)

        if (!finalHabit.reminderTime.isNullOrBlank() && !finalHabit.isPaused) {
            notificationManager.scheduleHabitReminder(finalHabit)
        } else {
            notificationManager.cancelHabitReminder(finalHabit.id)
        }
    }

    suspend fun deleteHabit(habit: Habit) {
        database.habitDao().deleteHabit(habit)
        database.habitCompletionDao().deleteAllForHabit(habit.id)
        firebaseManager.deleteHabitFromCloud(habit.userId, habit.id)
        notificationManager.cancelHabitReminder(habit.id)
    }

    suspend fun toggleHabitPause(habit: Habit) {
        val updated = habit.copy(isPaused = !habit.isPaused)
        database.habitDao().insertOrUpdateHabit(updated)
        firebaseManager.syncHabitToCloud(updated.userId, updated)

        if (!updated.reminderTime.isNullOrBlank() && !updated.isPaused) {
            notificationManager.scheduleHabitReminder(updated)
        } else {
            notificationManager.cancelHabitReminder(updated.id)
        }
    }

    // --- Completions & Toggle ---
    fun getCompletionsForDate(userId: String, date: String): Flow<List<HabitCompletion>> {
        return database.habitCompletionDao().getCompletionsForDate(userId, date)
    }

    fun getAllCompletions(userId: String): Flow<List<HabitCompletion>> {
        return database.habitCompletionDao().getAllCompletionsForUser(userId)
    }

    suspend fun toggleHabitCompletion(userId: String, habitId: String, date: String, targetStatus: CompletionStatus? = null) {
        val existing = database.habitCompletionDao().getCompletionsForDateOnce(userId, date)
            .firstOrNull { it.habitId == habitId }

        if (existing != null && targetStatus == null) {
            // Already completed/logged -> remove (uncheck)
            database.habitCompletionDao().deleteCompletion(habitId, date)
            firebaseManager.deleteCompletionFromCloud(userId, habitId, date)
        } else {
            val status = targetStatus ?: CompletionStatus.COMPLETED
            val completion = HabitCompletion(
                habitId = habitId,
                userId = userId,
                date = date,
                status = status,
                completedAt = System.currentTimeMillis()
            )
            database.habitCompletionDao().insertCompletion(completion)
            firebaseManager.syncCompletionToCloud(userId, completion)
        }
        recalculateUserStats(userId)
    }

    // --- Water Tracking & Hydration ---
    fun getWaterRecord(userId: String, date: String): Flow<WaterRecord?> {
        return database.waterDao().getWaterRecord(userId, date)
    }

    fun getAllWaterRecords(userId: String): Flow<List<WaterRecord>> {
        return database.waterDao().getAllWaterRecords(userId)
    }

    fun getWaterEntriesForDate(userId: String, date: String): Flow<List<WaterEntry>> {
        return database.waterDao().getWaterEntriesForDate(userId, date)
    }

    suspend fun addWater(userId: String, date: String, amountMl: Int) {
        if (amountMl == 0 || amountMl > 5000) return
        if (amountMl < 0) {
            val entries = database.waterDao().getWaterEntriesForDateOnce(userId, date)
            val latest = entries.maxByOrNull { it.timestamp }
            if (latest != null) {
                val newAmt = latest.amountMl + amountMl
                if (newAmt <= 0) {
                    deleteWaterEntry(userId, latest.id, date)
                } else {
                    editWaterEntry(userId, latest.id, date, newAmt)
                }
            }
            return
        }
        val entry = WaterEntry(
            id = UUID.randomUUID().toString(),
            userId = userId,
            date = date,
            amountMl = amountMl,
            timestamp = System.currentTimeMillis()
        )
        database.waterDao().insertWaterEntry(entry)
        firebaseManager.syncWaterEntryToCloud(userId, entry)

        recalculateWaterForDate(userId, date)
    }

    suspend fun editWaterEntry(userId: String, entryId: String, date: String, newAmountMl: Int) {
        if (newAmountMl <= 0 || newAmountMl > 5000) return
        val existingEntries = database.waterDao().getWaterEntriesForDateOnce(userId, date)
        val target = existingEntries.firstOrNull { it.id == entryId } ?: return
        val updated = target.copy(amountMl = newAmountMl)
        database.waterDao().updateWaterEntry(updated)
        firebaseManager.syncWaterEntryToCloud(userId, updated)

        recalculateWaterForDate(userId, date)
    }

    suspend fun deleteWaterEntry(userId: String, entryId: String, date: String) {
        database.waterDao().deleteWaterEntryById(entryId)
        firebaseManager.deleteWaterEntryFromCloud(userId, date, entryId)

        recalculateWaterForDate(userId, date)
    }

    fun getUserWaterGoalFlow(userId: String): Flow<Int?> {
        val key = androidx.datastore.preferences.core.intPreferencesKey("user_water_goal_$userId")
        return context.dataStore.data.map { prefs ->
            prefs[key] ?: 3000
        }
    }

    suspend fun getUserWaterGoalOnce(userId: String): Int? {
        val key = androidx.datastore.preferences.core.intPreferencesKey("user_water_goal_$userId")
        val local = context.dataStore.data.first()[key]
        if (local != null && local > 0) return local
        return 3000
    }

    suspend fun recalculateWaterForDate(userId: String, date: String) {
        val currentRecord = database.waterDao().getWaterRecordOnce(userId, date)
        val entries = database.waterDao().getWaterEntriesForDateOnce(userId, date)
        val totalMl = entries.sumOf { it.amountMl }
        // Preserve historical targetMl if set (> 0).
        // Only if not set (<= 0), check if the user has a saved water goal setting (default 3000 ml).
        val savedGoal = getUserWaterGoalOnce(userId) ?: 3000
        val targetMl = if (currentRecord != null && currentRecord.targetMl > 0) {
            currentRecord.targetMl
        } else {
            savedGoal
        }

        val updatedRecord = WaterRecord(
            userId = userId,
            date = date,
            currentMl = totalMl,
            targetMl = targetMl
        )
        database.waterDao().insertOrUpdateWater(updatedRecord)
        firebaseManager.syncWaterToCloud(userId, updatedRecord)
        recalculateUserStats(userId)
    }

    suspend fun setWaterTarget(userId: String, date: String, targetMl: Int) {
        val validTarget = targetMl.coerceIn(100, 15000)
        // 1. Update this specific day's record (e.g. today). Past days remain untouched!
        val current = database.waterDao().getWaterRecordOnce(userId, date)
            ?: WaterRecord(userId = userId, date = date, currentMl = 0, targetMl = validTarget)
        val updated = current.copy(targetMl = validTarget)
        database.waterDao().insertOrUpdateWater(updated)
        firebaseManager.syncWaterToCloud(userId, updated)

        // 2. Persist user's permanent water goal setting locally and in Firebase Realtime Database
        // at /users/{uid}/settings/waterGoal
        val key = androidx.datastore.preferences.core.intPreferencesKey("user_water_goal_$userId")
        context.dataStore.edit { prefs ->
            prefs[key] = validTarget
        }
        firebaseManager.saveWaterGoalSetting(userId, validTarget)
        recalculateUserStats(userId)
    }

    suspend fun saveWaterReminderSettings(userId: String, settings: WaterReminderSettings) {
        _waterReminderSettings.value = settings
        val currentMaster = _reminderSettings.value.copy(waterReminders = settings)
        _reminderSettings.value = currentMaster
        firebaseManager.syncWaterReminderSettings(userId, settings)
        firebaseManager.syncReminderSettings(userId, currentMaster)
        if (settings.isEnabled) {
            reminderManager.scheduleReminders(settings)
            notificationManager.scheduleWaterReminders(settings)
        } else {
            reminderManager.cancelReminders()
            notificationManager.cancelWaterReminders()
        }
    }

    suspend fun saveReminderSettings(userId: String, settings: ReminderSettings) {
        _reminderSettings.value = settings
        _waterReminderSettings.value = settings.waterReminders
        firebaseManager.syncReminderSettings(userId, settings)

        // Water
        if (settings.waterReminders.isEnabled) {
            reminderManager.scheduleReminders(settings.waterReminders)
            notificationManager.scheduleWaterReminders(settings.waterReminders)
        } else {
            reminderManager.cancelReminders()
            notificationManager.cancelWaterReminders()
        }

        // Night Review
        if (settings.nightReview.isEnabled) {
            notificationManager.scheduleNightReview(settings.nightReview)
        } else {
            notificationManager.cancelNightReview()
        }
    }

    suspend fun toggleHabitReminder(userId: String, habitId: String, isEnabled: Boolean, time: String? = null) {
        val habit = database.habitDao().getHabitById(habitId) ?: return
        val chosenTime = if (isEnabled) {
            time ?: habit.reminderTime ?: "07:30"
        } else null
        val updatedHabit = habit.copy(reminderTime = chosenTime)
        saveHabit(updatedHabit)

        val currentMaster = _reminderSettings.value
        val mapReminders = currentMaster.habitReminders.toMutableMap()
        val mapTimes = currentMaster.habitReminderTimes.toMutableMap()
        mapReminders[habitId] = isEnabled
        if (chosenTime != null) {
            mapTimes[habitId] = chosenTime
        }
        val updatedMaster = currentMaster.copy(
            habitReminders = mapReminders,
            habitReminderTimes = mapTimes
        )
        _reminderSettings.value = updatedMaster
        firebaseManager.syncReminderSettings(userId, updatedMaster)
    }

    suspend fun updateNightReviewReminder(userId: String, isEnabled: Boolean, time: String) {
        val currentMaster = _reminderSettings.value
        val updated = currentMaster.copy(
            nightReview = NightReviewReminderSettings(isEnabled = isEnabled, time = time)
        )
        saveReminderSettings(userId, updated)
    }

    // --- Focus Settings (Step 20) ---
    suspend fun saveFocusSettings(userId: String, settings: FocusSettings) {
        _focusSettings.value = settings
        firebaseManager.syncFocusSettings(userId, settings)
    }

    suspend fun reloadFocusSettingsFromCloud(userId: String) {
        val cloud = firebaseManager.fetchFocusSettings(userId)
        if (cloud != null) {
            _focusSettings.value = cloud
        }
    }

    // --- Journal Entries (Design 5 / iOS 27 Liquid Glass) ---
    fun getDefaultJournalEntries(userId: String): List<JournalEntry> {
        val todayStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        return listOf(
            JournalEntry(
                id = "sample_trade_1",
                userId = userId,
                title = "Trade Journal",
                content = "BTC long, good entry, followed plan.",
                time = "2:30 PM",
                date = todayStr,
                tags = listOf("Trade", "Discipline"),
                category = "Trade",
                createdAt = System.currentTimeMillis() - 3600000L
            ),
            JournalEntry(
                id = "sample_study_2",
                userId = userId,
                title = "Study Notes",
                content = "Chemistry - Chemical Bonding",
                time = "11:10 AM",
                date = todayStr,
                tags = listOf("Study", "Revision"),
                category = "Study",
                createdAt = System.currentTimeMillis() - 7200000L
            ),
            JournalEntry(
                id = "sample_reflection_3",
                userId = userId,
                title = "Daily Reflection",
                content = "Felt productive today. Controlled Shorts. Need to improve focus in evening.",
                time = "9:20 PM",
                date = todayStr,
                tags = listOf("Mindset", "Growth"),
                category = "Mindset",
                createdAt = System.currentTimeMillis() - 14400000L
            ),
            JournalEntry(
                id = "sample_workout_4",
                userId = userId,
                title = "Workout Log",
                content = "Push Day - 45 min",
                time = "7:45 PM",
                date = todayStr,
                tags = listOf("Workout", "Health"),
                category = "Workout",
                createdAt = System.currentTimeMillis() - 21600000L
            )
        )
    }

    suspend fun addJournalEntry(userId: String, entry: JournalEntry) {
        val current = _journalEntries.value.toMutableList()
        current.removeAll { it.id == entry.id }
        current.add(0, entry)
        _journalEntries.value = current
        firebaseManager.syncJournalEntryToCloud(userId, entry)
    }

    suspend fun deleteJournalEntry(userId: String, entryId: String) {
        val current = _journalEntries.value.filter { it.id != entryId }
        _journalEntries.value = current
        firebaseManager.deleteJournalEntryFromCloud(userId, entryId)
    }

    fun getNightReviewForDate(userId: String, date: String): Flow<NightReviewEntry?> {
        return database.nightReviewDao().getNightReviewForDate(userId, date)
    }

    fun getAllNightReviews(userId: String): Flow<List<NightReviewEntry>> {
        return database.nightReviewDao().getAllNightReviews(userId)
    }

    suspend fun saveNightReview(userId: String, entry: NightReviewEntry) {
        val reviewWithUser = entry.copy(userId = userId)
        database.nightReviewDao().insertNightReview(reviewWithUser)
        firebaseManager.syncNightReviewToCloud(userId, reviewWithUser)
        recalculateUserStats(userId)
    }

    suspend fun deleteNightReview(userId: String, date: String) {
        database.nightReviewDao().deleteNightReview(userId, date)
        firebaseManager.deleteNightReviewFromCloud(userId, date)
        recalculateUserStats(userId)
    }

    suspend fun fetchNightReview(userId: String, date: String): NightReviewEntry? {
        val local = database.nightReviewDao().getNightReviewForDateOnce(userId, date)
        if (local != null) return local
        val cloud = firebaseManager.fetchNightReview(userId, date)
        if (cloud != null) {
            database.nightReviewDao().insertNightReview(cloud)
        }
        return cloud
    }

    fun getFocusMinutesForDateRange(userId: String, startTimestamp: Long, endTimestamp: Long): Flow<Int?> {
        return database.focusDao().getFocusMinutesForDateRange(userId, startTimestamp, endTimestamp)
    }

    fun getFocusSessionsForDateRange(userId: String, startTimestamp: Long, endTimestamp: Long): Flow<List<FocusSession>> {
        return database.focusDao().getFocusSessionsForDateRange(userId, startTimestamp, endTimestamp)
    }

    fun triggerTestWaterReminder() {
        notificationManager.sendWaterNotification(isTest = true)
        reminderManager.sendTestNotification()
    }

    fun triggerTestNotification(type: ReminderType, customHabitTitle: String = "Morning Focus") {
        notificationManager.sendTestNotification(type, customHabitTitle)
    }

    // --- Focus Sessions ---
    fun getAllFocusSessions(userId: String): Flow<List<FocusSession>> {
        return database.focusDao().getAllFocusSessions(userId)
    }

    fun getRecentFocusSessions(userId: String): Flow<List<FocusSession>> {
        return database.focusDao().getRecentFocusSessions(userId)
    }

    fun getTotalFocusMinutes(userId: String): Flow<Int?> {
        return database.focusDao().getTotalFocusMinutes(userId)
    }

    suspend fun logFocusSession(session: FocusSession) {
        val finalSession = if (session.id.isBlank()) session.copy(id = UUID.randomUUID().toString()) else session
        database.focusDao().insertFocusSession(finalSession)
        firebaseManager.syncFocusSessionToCloud(finalSession.userId, finalSession)
        recalculateUserStats(finalSession.userId)
    }

    // --- Achievements ---
    fun getAchievements(): Flow<List<Achievement>> {
        return database.achievementDao().getAllAchievements()
    }

    private suspend fun seedDefaultAchievements() {
        val existing = try { database.achievementDao().getAllAchievements().first() } catch (e: Exception) { emptyList() }
        val existingMap = existing.associateBy { it.id }

        val defaults = listOf(
            Achievement("ach_first_step", "First Step", "Complete your first habit check-in", "flag", false, null, "total_habits", 1),
            Achievement("ach_streak_7", "7 Day Streak", "Maintain a 7-day habit streak", "local_fire_department", false, null, "streak", 7),
            Achievement("ach_streak_14", "14 Day Streak", "Maintain a 14-day streak", "bolt", false, null, "streak", 14),
            Achievement("ach_streak_30", "30 Day Streak", "Maintain a 30-day streak", "military_tech", false, null, "streak", 30),
            Achievement("ach_habit_builder", "Habit Builder", "Complete 50 habit check-ins", "construction", false, null, "total_habits", 50),
            Achievement("ach_consistent", "Consistent", "Complete 100 habit check-ins over time", "all_inclusive", false, null, "total_habits", 100),
            Achievement("ach_hydrated", "Hydrated", "Reach your water goal on multiple days (5 days)", "water_drop", false, null, "water_days", 5),
            Achievement("ach_focused", "Focused", "Complete multiple Focus sessions (5 sessions)", "psychology", false, null, "focus_sessions", 5),
            Achievement("ach_reflective", "Reflective", "Complete multiple Night Reviews (3 reviews)", "nightlight_round", false, null, "night_reviews", 3),
            Achievement("ach_wake_up_warrior", "Morning Warrior", "Conquer your first wake-up challenge", "alarm_on", false, null, "wake_up_challenges", 1),
            Achievement("ach_morning_mastery", "Iron Dawn", "Conquer 7 morning wake-up challenges", "military_tech", false, null, "wake_up_challenges", 7),
            Achievement("ach_top_100", "Top 100", "Enter the Top 100 on the Global Leaderboard", "leaderboard", false, null, "leaderboard_rank", 100),
            Achievement("ach_top_50", "Top 50 Elite", "Enter the Top 50 on the Global Leaderboard", "military_tech", false, null, "leaderboard_rank", 50),
            Achievement("ach_top_10", "Top 10 Titan", "Enter the Top 10 on the Global Leaderboard", "workspace_premium", false, null, "leaderboard_rank", 10),
            Achievement("ach_rank_1", "Apex Discipline", "Ascend to #1 on the Global Leaderboard", "emoji_events", false, null, "leaderboard_rank", 1)
        )

        val merged = defaults.map { def ->
            val prev = existingMap[def.id]
            if (prev != null && prev.isUnlocked) {
                def.copy(isUnlocked = true, unlockedAt = prev.unlockedAt)
            } else {
                def
            }
        }
        database.achievementDao().insertAchievements(merged)
    }

    private suspend fun seedInitialHabits(userId: String) {
        val existing = database.habitDao().getHabitsForUserOnce(userId)
        if (existing.isEmpty()) {
            val starterHabits = listOf(
                Habit(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    title = "Morning Hydration & Sunlight",
                    description = "Drink 500ml of water and get 10 mins of natural sunlight",
                    category = HabitCategory.HEALTH,
                    iconName = "water_drop",
                    colorHex = 0xFF06B6D4,
                    reminderTime = "07:00",
                    repeatDays = "1,2,3,4,5,6,7",
                    goalValue = 500,
                    goalUnit = "ml",
                    orderIndex = 0
                ),
                Habit(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    title = "Deep Focus Work (50m)",
                    description = "Zero phone, zero distraction single-task block",
                    category = HabitCategory.PRODUCTIVITY,
                    iconName = "bolt",
                    colorHex = 0xFF8B5CF6,
                    reminderTime = "09:00",
                    repeatDays = "1,2,3,4,5",
                    goalValue = 50,
                    goalUnit = "mins",
                    orderIndex = 1
                ),
                Habit(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    title = "Physical Conditioning",
                    description = "Workout, running, or 100 pushups",
                    category = HabitCategory.FITNESS,
                    iconName = "fitness_center",
                    colorHex = 0xFF10B981,
                    reminderTime = "17:30",
                    repeatDays = "1,2,3,4,5,6",
                    goalValue = 1,
                    goalUnit = "session",
                    orderIndex = 2
                ),
                Habit(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    title = "Evening Reflection & Reading",
                    description = "Read 10 pages and review tomorrow's priorities",
                    category = HabitCategory.MINDSET,
                    iconName = "menu_book",
                    colorHex = 0xFFF59E0B,
                    reminderTime = "21:30",
                    repeatDays = "1,2,3,4,5,6,7",
                    goalValue = 10,
                    goalUnit = "pages",
                    orderIndex = 3
                )
            )
            database.habitDao().insertHabits(starterHabits)
            starterHabits.forEach { firebaseManager.syncHabitToCloud(userId, it) }
        }
    }

    // --- Dynamic Discipline Stats Recalculation ---
    suspend fun recalculateUserStats(userId: String) {
        val user = database.userProfileDao().getUserProfileOnce(userId) ?: return
        val habits = database.habitDao().getHabitsForUserOnce(userId).filterNot { it.isPaused }
        val allCompletions = database.habitCompletionDao().getAllCompletionsForUser(userId).first()

        val today = LocalDate.now()
        val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        // 1. Calculate Current Streak
        var currentStreak = 0
        val todayStr = today.format(isoFormatter)
        val todayScheduled = habits.filter { it.isScheduledForDate(today) }
        val todayCompletedCount = allCompletions.count { it.date == todayStr && it.status == CompletionStatus.COMPLETED }

        var checkDate: LocalDate
        if (todayScheduled.isNotEmpty() && todayCompletedCount > 0) {
            currentStreak++
            checkDate = today.minusDays(1)
        } else {
            // Today not yet completed or in progress, evaluate starting from yesterday
            checkDate = today.minusDays(1)
        }

        var streakBroken = false
        var daysBack = 0
        while (!streakBroken && daysBack < 365) {
            val dateStr = checkDate.format(isoFormatter)
            val scheduledHabits = habits.filter { it.isScheduledForDate(checkDate) }

            if (scheduledHabits.isNotEmpty()) {
                val dayCompleted = allCompletions.any { it.date == dateStr && it.status == CompletionStatus.COMPLETED }
                if (dayCompleted) {
                    currentStreak++
                } else {
                    streakBroken = true
                }
            }
            checkDate = checkDate.minusDays(1)
            daysBack++
        }

        // 2. Calculate Best Historical Streak from all completion records
        val completionDates = allCompletions
            .filter { it.status == CompletionStatus.COMPLETED }
            .mapNotNull {
                try { LocalDate.parse(it.date, isoFormatter) } catch (e: Exception) { null }
            }
            .distinct()
            .sorted()

        var maxHistoricalStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null
        for (date in completionDates) {
            if (prevDate == null || date.minusDays(1) == prevDate) {
                tempStreak++
            } else {
                tempStreak = 1
            }
            if (tempStreak > maxHistoricalStreak) {
                maxHistoricalStreak = tempStreak
            }
            prevDate = date
        }

        var bestStreak = maxOf(user.bestStreak, maxOf(currentStreak, maxHistoricalStreak))

        // 3. Calculate 14-day habit consistency rate
        val last14Days = (0..13).map { today.minusDays(it.toLong()) }
        var totalScheduled = 0
        var totalCompleted = 0
        for (d in last14Days) {
            val dateStr = d.format(isoFormatter)
            val dayOfWeek = d.dayOfWeek.value
            val sched = habits.filter { it.isScheduledForDate(d) }
            totalScheduled += sched.size
            val done = allCompletions.count { it.date == dateStr && it.status == CompletionStatus.COMPLETED }
            totalCompleted += done
        }
        val completionRate = if (totalScheduled > 0) {
            (totalCompleted.toFloat() / totalScheduled.toFloat()).coerceIn(0f, 1f)
        } else if (totalCompleted > 0) {
            1.0f
        } else {
            0f
        }

        // --- REAL DYNAMIC DISCIPLINE SCORE (0 to 100) ---
        // 1. Habit Completion Consistency (up to 30 pts)
        val habitsScore = if (totalCompleted > 0 && totalScheduled > 0) {
            (completionRate * 30f).roundToInt().coerceIn(0, 30)
        } else if (totalCompleted > 0) {
            minOf(30, totalCompleted * 5)
        } else {
            0
        }

        // 2. Current Streak Consistency (up to 20 pts)
        val streakScore = when {
            currentStreak >= 7 -> 20
            currentStreak >= 5 -> 16
            currentStreak >= 3 -> 12
            currentStreak >= 2 -> 8
            currentStreak >= 1 -> 4
            else -> 0
        }

        // 3. Water-Goal Completion (up to 15 pts)
        val allWaterRecords = try { database.waterDao().getAllWaterRecordsOnce(userId) } catch (e: Exception) { emptyList() }
        val waterDaysReached = allWaterRecords.count { it.targetMl > 0 && it.currentMl >= it.targetMl }
        val recent7Days = (0..6).map { today.minusDays(it.toLong()).format(isoFormatter) }
        val recentWaterMet = allWaterRecords.count { it.date in recent7Days && it.targetMl > 0 && it.currentMl >= it.targetMl }
        val todayWater = allWaterRecords.firstOrNull { it.date == todayStr }
        val partialTodayWater = if (todayWater != null && todayWater.targetMl > 0 && todayWater.currentMl < todayWater.targetMl) {
            ((todayWater.currentMl.toFloat() / todayWater.targetMl.toFloat()) * 2f).toInt()
        } else 0
        val waterScore = minOf(15, recentWaterMet * 3 + partialTodayWater)

        // 4. Completed Focus Sessions (up to 20 pts)
        val focusSessions = try { database.focusDao().getAllCompletedFocusSessionsOnce(userId) } catch (e: Exception) { emptyList() }
        val completedFocusCount = focusSessions.count { it.isCompleted }
        val focusScore = when {
            completedFocusCount >= 4 -> 20
            completedFocusCount == 3 -> 16
            completedFocusCount == 2 -> 12
            completedFocusCount == 1 -> 6
            else -> 0
        }

        // 5. Night Review Consistency (up to 15 pts)
        val nightReviews = try { database.nightReviewDao().getAllNightReviewsOnce(userId) } catch (e: Exception) { emptyList() }
        val completedReviewsCount = nightReviews.size
        val reviewScore = minOf(15, completedReviewsCount * 3)

        val totalDisciplineScore = (habitsScore + streakScore + waterScore + focusScore + reviewScore).coerceIn(0, 100)

        val breakdown = DisciplineScoreBreakdown(
            score = totalDisciplineScore,
            habitsScore = habitsScore,
            streakScore = streakScore,
            waterScore = waterScore,
            focusScore = focusScore,
            reviewScore = reviewScore,
            habitsExplanation = if (totalCompleted > 0) "$totalCompleted check-ins in 14d window (${(completionRate * 100).toInt()}%)" else "Complete scheduled habits for up to 30 pts",
            streakExplanation = if (currentStreak > 0) "$currentStreak-day active streak" else "Build a consecutive daily streak for up to 20 pts",
            waterExplanation = if (recentWaterMet > 0) "$recentWaterMet/5 water goal days logged recently" else "Hit daily water target for up to 15 pts",
            focusExplanation = if (completedFocusCount > 0) "$completedFocusCount completed flow sessions" else "Complete deep focus sessions for up to 20 pts",
            reviewExplanation = if (completedReviewsCount > 0) "$completedReviewsCount mindful night reviews" else "Complete evening reflections for up to 15 pts"
        )
        _disciplineScoreBreakdown.value = breakdown

        val updatedUser = user.copy(
            disciplineScore = totalDisciplineScore,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            completionRate = completionRate
        )
        database.userProfileDao().insertOrUpdate(updatedUser)
        firebaseManager.syncUserProfileToCloud(updatedUser)
        if (updatedUser.publicProfileEnabled) {
            val unlockedAchievements = try { database.achievementDao().getUnlockedAchievementsOnce() } catch (e: Exception) { emptyList() }
            try {
                firebaseManager.syncPublicUserProfile(updatedUser, unlockedAchievements)
            } catch (e: Exception) {
                Log.w(TAG, "Public profile sync during score recalculation notice: ${e.message}")
            }
        }
        firebaseManager.syncStreaksToCloud(userId, currentStreak, bestStreak)
        firebaseManager.syncProgressToCloud(userId, totalDisciplineScore, completionRate)
        firebaseManager.syncDisciplineScoreToCloud(
            userId = userId,
            score = totalDisciplineScore,
            breakdown = mapOf(
                "habitsScore" to habitsScore,
                "streakScore" to streakScore,
                "waterScore" to waterScore,
                "focusScore" to focusScore,
                "reviewScore" to reviewScore,
                "totalCompletedHabits" to totalCompleted,
                "currentStreak" to currentStreak,
                "waterDaysReached" to waterDaysReached,
                "completedFocusCount" to completedFocusCount,
                "completedReviewsCount" to completedReviewsCount
            )
        )

        // 6. Evaluate and sync achievements
        try {
            val achievements = database.achievementDao().getAllAchievements().first()
            val totalAllTimeHabitCompletions = allCompletions.count { it.status == CompletionStatus.COMPLETED }
            val completedWakeUpChallenges = try {
                database.challengeHistoryDao().getHistoryForUser(userId).first().count { it.status == "COMPLETED" }
            } catch (e: Exception) { 0 }
            var achievementsChanged = false
            val updatedAchievements = achievements.map { ach ->
                val shouldUnlock = when (ach.id) {
                    "ach_first_step" -> totalAllTimeHabitCompletions >= 1
                    "ach_streak_7" -> bestStreak >= 7 || currentStreak >= 7
                    "ach_streak_14" -> bestStreak >= 14 || currentStreak >= 14
                    "ach_streak_30" -> bestStreak >= 30 || currentStreak >= 30
                    "ach_habit_builder" -> totalAllTimeHabitCompletions >= 50
                    "ach_consistent" -> totalAllTimeHabitCompletions >= 100
                    "ach_hydrated" -> waterDaysReached >= 5
                    "ach_focused" -> completedFocusCount >= 5
                    "ach_reflective" -> completedReviewsCount >= 3
                    "ach_wake_up_warrior" -> completedWakeUpChallenges >= 1
                    "ach_morning_mastery" -> completedWakeUpChallenges >= 7
                    else -> when (ach.requirementType) {
                        "total_habits", "total" -> totalAllTimeHabitCompletions >= ach.requirementValue
                        "streak" -> bestStreak >= ach.requirementValue || currentStreak >= ach.requirementValue
                        "water_days", "water" -> waterDaysReached >= ach.requirementValue
                        "focus_sessions", "focus" -> completedFocusCount >= ach.requirementValue
                        "night_reviews" -> completedReviewsCount >= ach.requirementValue
                        "wake_up_challenges" -> completedWakeUpChallenges >= ach.requirementValue
                        else -> ach.isUnlocked
                    }
                }
                if (shouldUnlock && !ach.isUnlocked) {
                    achievementsChanged = true
                    val achNotif = AppNotification(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        title = "Achievement Unlocked: ${ach.title} 🏆",
                        message = ach.description,
                        type = NotificationType.SYSTEM,
                        targetDestination = "habits",
                        relatedEntityId = ach.id,
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                        scheduledDate = "${todayStr}_${ach.id}"
                    )
                    addNotification(achNotif)
                    ach.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())
                } else ach
            }
            if (achievementsChanged) {
                database.achievementDao().insertAchievements(updatedAchievements)
                firebaseManager.syncAchievementsToCloud(userId, updatedAchievements)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Achievement evaluation error: ${e.message}")
        }
    }

    // --- Settings & Preferences ---
    suspend fun saveUserSettings(userId: String, settings: Map<String, Any?>) {
        firebaseManager.syncSettingsToCloud(userId, settings)
    }

    suspend fun getUserSettings(userId: String): Map<String, Any?>? {
        return firebaseManager.fetchUserSettings(userId)
    }

    // --- Centralized Smart Notification Center ---
    fun getNotificationsFlow(userId: String): Flow<List<AppNotification>> {
        return database.notificationDao().getNotificationsFlow(userId)
    }

    fun getUnreadNotificationsCountFlow(userId: String): Flow<Int> {
        return database.notificationDao().getUnreadCountFlow(userId)
    }

    suspend fun markNotificationAsRead(id: String, userId: String, isRead: Boolean = true) {
        database.notificationDao().updateReadStatus(id, isRead)
        firebaseManager.updateNotificationReadStateInCloud(userId, id, isRead)
    }

    suspend fun markAllNotificationsAsRead(userId: String) {
        database.notificationDao().markAllAsRead(userId)
        firebaseManager.markAllNotificationsAsReadInCloud(userId)
    }

    suspend fun deleteNotification(id: String, userId: String) {
        database.notificationDao().deleteById(id)
        firebaseManager.deleteNotificationFromCloud(userId, id)
    }

    suspend fun syncCloudNotifications(userId: String) {
        if (userId.startsWith("guest_")) return
        try {
            val cloudNotifs = firebaseManager.fetchUserNotifications(userId)
            if (cloudNotifs.isNotEmpty()) {
                database.notificationDao().insertAll(cloudNotifs)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notification sync error: ${e.message}")
        }
    }

    suspend fun addNotification(notification: AppNotification) {
        database.notificationDao().insertOrUpdate(notification)
        firebaseManager.saveNotificationToCloud(notification.userId, notification)
    }

    suspend fun generateSmartRemindersIfDue(userId: String) {
        if (userId.isBlank()) return
        try {
            val now = LocalDateTime.now()
            val todayDate = now.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val currentHour = now.hour
            val currentMinute = now.minute
            val currentTimeMinutes: Int = currentHour * 60 + currentMinute
            val dayOfWeek = now.dayOfWeek.value

            val reminderSettings = _reminderSettings.value

            // 0. DayForge System Welcome Notification (if never issued)
            val welcomeKey = "system_welcome"
            val existingWelcome = database.notificationDao().getNotificationByDateAndType(userId, welcomeKey, NotificationType.SYSTEM.name)
            if (existingWelcome == null) {
                val welcomeNotif = AppNotification(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    title = "Welcome to DayForge 🛡️",
                    message = "Your discipline engine is active. Complete your scheduled habits, track hydration, and review your day each night to build unstoppable momentum.",
                    type = NotificationType.SYSTEM,
                    targetDestination = "habits",
                    relatedEntityId = null,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    scheduledDate = welcomeKey
                )
                addNotification(welcomeNotif)
            }

            // 1. Habit Reminders
            val habits = database.habitDao().getHabitsForUserOnce(userId)
            for (habit in habits) {
                if (habit.isPaused) continue
                if (!habit.isScheduledForDayOfWeek(dayOfWeek)) continue

                val isEnabled: Boolean = reminderSettings.habitReminders[habit.id] ?: (!habit.reminderTime.isNullOrBlank())
                val timeStr: String? = reminderSettings.habitReminderTimes[habit.id] ?: habit.reminderTime
                if (!isEnabled || timeStr.isNullOrBlank()) continue

                val parts: List<String> = timeStr.split(":")
                if (parts.size == 2) {
                    val hHour = parts[0].toIntOrNull() ?: continue
                    val hMin = parts[1].toIntOrNull() ?: continue
                    val habitTimeMinutes: Int = hHour * 60 + hMin

                    // If scheduled time has arrived or passed today
                    if (currentTimeMinutes >= habitTimeMinutes) {
                        val existing = database.notificationDao().getNotificationByDateAndEntity(userId, todayDate, habit.id)
                        if (existing == null) {
                            val notif = AppNotification(
                                id = UUID.randomUUID().toString(),
                                userId = userId,
                                title = "Habit: ${habit.title}",
                                message = "Time to complete your scheduled habit '${habit.title}'. Keep the momentum alive!",
                                type = NotificationType.HABIT,
                                targetDestination = "habits",
                                relatedEntityId = habit.id,
                                timestamp = System.currentTimeMillis(),
                                isRead = false,
                                scheduledDate = todayDate
                            )
                            addNotification(notif)
                        }
                    }
                }
            }

            // 2. Water Reminders
            if (reminderSettings.waterReminders.isEnabled) {
                val startParts: List<String> = reminderSettings.waterReminders.startTime.split(":")
                val endParts: List<String> = reminderSettings.waterReminders.endTime.split(":")
                val startHour = startParts.firstOrNull()?.toIntOrNull() ?: 8
                val endHour = endParts.firstOrNull()?.toIntOrNull() ?: 21

                if (currentHour in startHour..endHour) {
                    val todayWater = database.waterDao().getWaterRecordOnce(userId, todayDate)
                    val currentMl = todayWater?.currentMl ?: 0
                    val targetMl = todayWater?.targetMl ?: 2500

                    if (currentMl < targetMl) {
                        val windowKey = "${todayDate}_water_${currentHour / 2}"
                        val existing = database.notificationDao().getNotificationByDateAndType(userId, windowKey, NotificationType.WATER.name)
                        if (existing == null) {
                            val notif = AppNotification(
                                id = UUID.randomUUID().toString(),
                                userId = userId,
                                title = "Hydration Reminder 💧",
                                message = "Drink 250ml of fresh water now to keep energy, clarity, and physical stamina high.",
                                type = NotificationType.WATER,
                                targetDestination = "water",
                                relatedEntityId = null,
                                timestamp = System.currentTimeMillis(),
                                isRead = false,
                                scheduledDate = windowKey
                            )
                            addNotification(notif)
                        }
                    }
                }
            }

            // 3. Night Review Reminder
            if (reminderSettings.nightReview.isEnabled) {
                val rParts: List<String> = reminderSettings.nightReview.time.split(":")
                val rHour = rParts.firstOrNull()?.toIntOrNull() ?: 21
                val rMin = rParts.getOrNull(1)?.toIntOrNull() ?: 30
                val reviewTimeMinutes: Int = rHour * 60 + rMin

                if (currentTimeMinutes >= reviewTimeMinutes) {
                    val existingReview = database.nightReviewDao().getNightReviewForDateOnce(userId, todayDate)
                    if (existingReview == null) {
                        val existingNotif = database.notificationDao().getNotificationByDateAndType(userId, todayDate, NotificationType.NIGHT_REVIEW.name)
                        if (existingNotif == null) {
                            val notif = AppNotification(
                                id = UUID.randomUUID().toString(),
                                userId = userId,
                                title = "Evening Reflection & Review 🌙",
                                message = "Take 2 minutes to evaluate today's discipline, celebrate wins, and lock in tomorrow's priorities.",
                                type = NotificationType.NIGHT_REVIEW,
                                targetDestination = "night_review",
                                relatedEntityId = null,
                                timestamp = System.currentTimeMillis(),
                                isRead = false,
                                scheduledDate = todayDate
                            )
                            addNotification(notif)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Smart reminders generation error: ${e.message}")
        }
    }

    // --- Step 17: Backup & Data Management ---
    suspend fun createBackupJson(currentUid: String): Result<String> {
        return backupManager.createBackupJson(context, currentUid)
    }

    suspend fun writeBackupToUri(jsonString: String, uri: Uri): Result<Unit> {
        return backupManager.writeJsonToUri(context, jsonString, uri)
    }

    suspend fun validateBackupFromUri(uri: Uri): Result<BackupSummary> {
        return backupManager.validateBackupFromUri(context, uri)
    }

    suspend fun restoreBackup(currentUid: String, rawJson: String, autoSafetySnapshot: Boolean = true): Result<BackupRestoreReport> {
        val result = backupManager.restoreBackup(context, currentUid, rawJson, autoSafetySnapshot)
        if (result.isSuccess) {
            // Trigger score recalculation & smart reminders
            recalculateUserStats(currentUid)
            generateSmartRemindersIfDue(currentUid)
        }
        return result
    }

    suspend fun createSafetySnapshot(currentUid: String): Result<File> {
        return backupManager.createSafetySnapshot(context, currentUid)
    }

    // --- Step 19: Native Challenge Alarms ---
    fun getAlarmsFlow(userId: String): Flow<List<ChallengeAlarm>> {
        return database.challengeAlarmDao().getAlarmsForUser(userId)
    }

    suspend fun saveChallengeAlarm(alarm: ChallengeAlarm) {
        val finalAlarm = if (alarm.id.isBlank()) alarm.copy(id = UUID.randomUUID().toString()) else alarm
        database.challengeAlarmDao().insertOrUpdateAlarm(finalAlarm)
        firebaseManager.syncAlarmToCloud(finalAlarm.userId, finalAlarm)

        if (finalAlarm.isEnabled) {
            challengeAlarmManager.scheduleAlarm(finalAlarm)
            addNotification(
                AppNotification(
                    id = UUID.randomUUID().toString(),
                    userId = finalAlarm.userId,
                    title = "Alarm Scheduled ⏰",
                    message = "${finalAlarm.name} set for ${finalAlarm.time} with ${finalAlarm.challengeType.displayName} challenge (${finalAlarm.target} ${finalAlarm.challengeType.unit}).",
                    type = NotificationType.ALARM,
                    targetDestination = "alarms",
                    timestamp = System.currentTimeMillis()
                )
            )
        } else {
            challengeAlarmManager.cancelAlarm(finalAlarm.id)
        }
    }

    suspend fun toggleChallengeAlarm(userId: String, alarmId: String, isEnabled: Boolean) {
        val existing = database.challengeAlarmDao().getAlarmById(alarmId) ?: return
        val updated = existing.copy(isEnabled = isEnabled, updatedAt = System.currentTimeMillis())
        database.challengeAlarmDao().insertOrUpdateAlarm(updated)
        firebaseManager.syncAlarmToCloud(userId, updated)

        if (isEnabled) {
            challengeAlarmManager.scheduleAlarm(updated)
        } else {
            challengeAlarmManager.cancelAlarm(alarmId)
        }
    }

    suspend fun deleteChallengeAlarm(alarm: ChallengeAlarm) {
        database.challengeAlarmDao().deleteAlarm(alarm)
        firebaseManager.deleteAlarmFromCloud(alarm.userId, alarm.id)
        challengeAlarmManager.cancelAlarm(alarm.id)
    }

    // --- Challenge History ---
    fun getChallengeHistoryFlow(userId: String): Flow<List<ChallengeHistoryEntry>> {
        return database.challengeHistoryDao().getHistoryForUser(userId)
    }

    suspend fun saveChallengeResult(entry: ChallengeHistoryEntry) {
        val finalEntry = if (entry.id.isBlank()) entry.copy(id = UUID.randomUUID().toString()) else entry
        database.challengeHistoryDao().insertEntry(finalEntry)
        firebaseManager.syncChallengeHistoryToCloud(finalEntry.userId, finalEntry)

        if (finalEntry.status == "COMPLETED") {
            addNotification(
                AppNotification(
                    id = UUID.randomUUID().toString(),
                    userId = finalEntry.userId,
                    title = "Wake-Up Challenge Conquered! ⚔️",
                    message = "You completed ${finalEntry.target} ${finalEntry.challengeType.unit} of ${finalEntry.challengeType.displayName}! Great morning start.",
                    type = NotificationType.ALARM,
                    targetDestination = "alarms",
                    timestamp = System.currentTimeMillis()
                )
            )
        } else {
            addNotification(
                AppNotification(
                    id = UUID.randomUUID().toString(),
                    userId = finalEntry.userId,
                    title = "Challenge Stopped",
                    message = "Alarm challenge was stopped safely. Rest up and conquer tomorrow!",
                    type = NotificationType.ALARM,
                    targetDestination = "alarms",
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        recalculateUserStats(finalEntry.userId)
    }

    suspend fun markHabitCompletedToday(userId: String, habitId: String) {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val completion = HabitCompletion(
            habitId = habitId,
            userId = userId,
            date = todayStr,
            status = CompletionStatus.COMPLETED,
            completedAt = System.currentTimeMillis()
        )
        database.habitCompletionDao().insertCompletion(completion)
        firebaseManager.syncCompletionToCloud(userId, completion)
        recalculateUserStats(userId)
    }

    // --- Global Leaderboard & Discipline Competition ---
    suspend fun getGlobalLeaderboard(limit: Int = 100): List<LeaderboardEntry> {
        return firebaseManager.fetchGlobalLeaderboard(limit)
    }

    suspend fun toggleLeaderboardParticipation(userId: String, enabled: Boolean): Result<Unit> {
        val user = database.userProfileDao().getUserProfileOnce(userId) ?: return Result.failure(Exception("User profile not found"))
        val updated = user.copy(leaderboardEnabled = enabled)
        database.userProfileDao().insertOrUpdate(updated)
        firebaseManager.toggleLeaderboardSetting(userId, enabled)
        if (enabled) {
            firebaseManager.syncLeaderboardEntry(updated)
        }
        return Result.success(Unit)
    }

    suspend fun evaluateLeaderboardRankAchievements(userId: String, rank: Int) {
        if (rank <= 0) return
        try {
            val achievements = database.achievementDao().getAllAchievements().first()
            var changed = false
            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val updated = achievements.map { ach ->
                val shouldUnlock = when (ach.id) {
                    "ach_top_100" -> rank in 1..100
                    "ach_top_50" -> rank in 1..50
                    "ach_top_10" -> rank in 1..10
                    "ach_rank_1" -> rank == 1
                    else -> ach.isUnlocked
                }
                if (shouldUnlock && !ach.isUnlocked) {
                    changed = true
                    val notif = AppNotification(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        title = "Leaderboard Milestone: ${ach.title} 🏆",
                        message = "You ranked #$rank on the Global Discipline Leaderboard!",
                        type = NotificationType.LEADERBOARD,
                        targetDestination = "leaderboard",
                        relatedEntityId = ach.id,
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                        scheduledDate = "rank_${rank}_${ach.id}"
                    )
                    addNotification(notif)
                    ach.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())
                } else ach
            }
            if (changed) {
                database.achievementDao().insertAchievements(updated)
                firebaseManager.syncAchievementsToCloud(userId, updated)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Evaluate leaderboard rank achievements error: ${e.message}")
        }
    }

    // =========================================================================
    // DAYFORGE CONTENT HUB REPOSITORY
    // =========================================================================

    private val _contentItems = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.data.models.DisciplineContent>>(emptyList())
    val contentItems: kotlinx.coroutines.flow.StateFlow<List<com.example.data.models.DisciplineContent>> = _contentItems.asStateFlow()

    private val _savedContentIds = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())
    val savedContentIds: kotlinx.coroutines.flow.StateFlow<Set<String>> = _savedContentIds.asStateFlow()

    private val _viewedContentIds = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())
    val viewedContentIds: kotlinx.coroutines.flow.StateFlow<Set<String>> = _viewedContentIds.asStateFlow()

    suspend fun refreshContentHub(userId: String?) {
        try {
            // Seed curated initial content to RTDB if completely empty
            firebaseManager.seedInitialContentIfEmpty(getCuratedDayForgeContent())

            val items = firebaseManager.fetchPublishedContent()
            if (items.isNotEmpty()) {
                _contentItems.value = items
            } else {
                _contentItems.value = getCuratedDayForgeContent()
            }

            if (userId != null && userId.isNotBlank()) {
                val saved = firebaseManager.fetchSavedContentIds(userId)
                _savedContentIds.value = saved

                val history = firebaseManager.fetchContentViewHistory(userId)
                _viewedContentIds.value = history
            }
        } catch (e: Exception) {
            Log.w(TAG, "Refresh content hub error: ${e.message}")
            if (_contentItems.value.isEmpty()) {
                _contentItems.value = getCuratedDayForgeContent()
            }
        }
    }

    suspend fun toggleSaveContent(userId: String, contentId: String): Boolean {
        val current = _savedContentIds.value
        val willSave = !current.contains(contentId)
        val updated = if (willSave) current + contentId else current - contentId
        _savedContentIds.value = updated
        firebaseManager.saveBookmarkContent(userId, contentId, willSave)
        return willSave
    }

    suspend fun markContentAsViewed(userId: String, contentId: String) {
        if (_viewedContentIds.value.contains(contentId)) return
        _viewedContentIds.value = _viewedContentIds.value + contentId
        firebaseManager.recordContentView(userId, contentId)
    }

    suspend fun triggerContentNotification(userId: String, content: com.example.data.models.DisciplineContent) {
        try {
            val notif = AppNotification(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "New DAYFORGE Content 🔥",
                message = "${content.title}: ${content.description.take(70)}...",
                type = NotificationType.CONTENT,
                targetDestination = "content",
                relatedEntityId = content.contentId,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                scheduledDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
            addNotification(notif)
        } catch (e: Exception) {
            Log.w(TAG, "Content notification error: ${e.message}")
        }
    }

    fun getCuratedDayForgeContent(): List<com.example.data.models.DisciplineContent> {
        val now = System.currentTimeMillis()
        return listOf(
            com.example.data.models.DisciplineContent(
                contentId = "df_content_1",
                title = "The 5-Second Rule for Instant Action",
                description = "When you feel an instinct to act on a goal, you must count 5-4-3-2-1 and physically move before your brain makes an excuse. Action precedes motivation.",
                mediaUrl = "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=1080",
                thumbnailUrl = "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=600",
                category = "DISCIPLINE",
                creatorName = "Coach Marcus • DayForge",
                createdAt = now - 3600_000L * 2,
                duration = "1 min read",
                isPublished = true,
                isFeatured = true,
                ctaLabel = "Start Focus",
                ctaAction = "focus"
            ),
            com.example.data.models.DisciplineContent(
                contentId = "df_content_2",
                title = "Hydration Architecture & Cellular Energy",
                description = "Drinking 500ml of cold water within 10 minutes of waking triggers metabolic activation and boosts cognitive alertness by up to 24%.",
                mediaUrl = "https://images.unsplash.com/photo-1548839140-29a749e1bc4e?w=1080",
                thumbnailUrl = "https://images.unsplash.com/photo-1548839140-29a749e1bc4e?w=600",
                category = "HEALTH",
                creatorName = "Dr. Elena Vance • Performance Lab",
                createdAt = now - 3600_000L * 5,
                duration = "2 min read",
                isPublished = true,
                isFeatured = true,
                ctaLabel = "Log Hydration",
                ctaAction = "water"
            ),
            com.example.data.models.DisciplineContent(
                contentId = "df_content_3",
                title = "Compound Movement & Joint Longevity",
                description = "Focus on healthy movement patterns: deep bodyweight squats, controlled planks, and spinal mobility. Consistency and proper form build lifelong strength without injury.",
                mediaUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=1080",
                thumbnailUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=600",
                category = "FITNESS",
                creatorName = "Alex Rivera • Strength & Mobility",
                createdAt = now - 3600_000L * 10,
                duration = "3 min read",
                isPublished = true,
                isFeatured = false,
                ctaLabel = "View Habits",
                ctaAction = "habits"
            ),
            com.example.data.models.DisciplineContent(
                contentId = "df_content_4",
                title = "Deep Work Blocks: Elimination of Context Switching",
                description = "Single-tasking for 50 uninterrupted minutes increases deep output 3x compared to fragmented work. Put the device in Lock Mode and enter flow state.",
                mediaUrl = "https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=1080",
                thumbnailUrl = "https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=600",
                category = "PRODUCTIVITY",
                creatorName = "David Chen • Deep Study Project",
                createdAt = now - 3600_000L * 24,
                duration = "2 min read",
                isPublished = true,
                isFeatured = false,
                ctaLabel = "Launch Timer",
                ctaAction = "focus"
            ),
            com.example.data.models.DisciplineContent(
                contentId = "df_content_5",
                title = "Feynman Technique for Accelerated Learning",
                description = "To master any complex subject: study the core concept, teach it simply to a 10-year-old in plain language, identify knowledge gaps, and refine.",
                mediaUrl = "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=1080",
                thumbnailUrl = "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=600",
                category = "STUDY",
                creatorName = "Prof. Ronald Sterling",
                createdAt = now - 3600_000L * 30,
                duration = "2 min read",
                isPublished = true,
                isFeatured = false,
                ctaLabel = "Journal Notes",
                ctaAction = "journal"
            ),
            com.example.data.models.DisciplineContent(
                contentId = "df_content_6",
                title = "Circadian Rhythm & Iron Dawn Routine",
                description = "Wake up at the exact same hour every day. Natural morning sunlight sets your internal clock, driving peak evening melatonin release and deep recovery sleep.",
                mediaUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=1080",
                thumbnailUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=600",
                category = "DISCIPLINE",
                creatorName = "Dr. Maya Lin • Circadian Medicine",
                createdAt = now - 3600_000L * 48,
                duration = "3 min read",
                isPublished = true,
                isFeatured = false,
                ctaLabel = "Wake-Up Challenge",
                ctaAction = "alarms"
            ),
            com.example.data.models.DisciplineContent(
                contentId = "df_content_7",
                title = "Reframing Resistance: The Stoic Mindset",
                description = "The impediment to action advances action. What stands in the way becomes the way. Learn to view discomfort not as a stopping point, but as the exact forge of your character.",
                mediaUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1080",
                thumbnailUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600",
                category = "MINDSET",
                creatorName = "Marcus Vance • Stoic Athlete",
                createdAt = now - 3600_000L * 15,
                duration = "2 min read",
                isPublished = true,
                isFeatured = true,
                ctaLabel = "Journal Reflection",
                ctaAction = "journal"
            )
        )
    }
}

