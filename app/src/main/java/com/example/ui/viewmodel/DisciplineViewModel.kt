package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.*
import com.example.data.local.AppDatabase
import com.example.data.models.*
import com.example.data.remote.FirebaseManager
import com.example.data.repository.DisciplineRepository
import com.example.focus.FocusRestrictionService
import com.example.focus.FocusSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

data class MonthlyHabitRow(
    val habit: Habit,
    val dayStatuses: Map<Int, CompletionStatus?> // Day of month (1..31) -> Status
)

data class HeatmapDay(
    val date: LocalDate,
    val completionRatio: Float, // 0.0 to 1.0
    val completedCount: Int,
    val totalCount: Int
)

data class HabitProgressStat(
    val habit: Habit,
    val completedDaysCount: Int,
    val scheduledDaysCount: Int,
    val completionPercentage: Int,
    val currentStreak: Int
)

sealed interface StartupUiState {
    object Loading : StartupUiState
    object Ready : StartupUiState
    data class Error(val message: String, val details: String? = null) : StartupUiState
}

sealed interface PublicProfileUiState {
    object Idle : PublicProfileUiState
    object Loading : PublicProfileUiState
    data class Success(val profile: com.example.data.models.PublicUserProfile) : PublicProfileUiState
    data class Private(val username: String, val displayName: String, val photoUrl: String) : PublicProfileUiState
    data class NotFound(val username: String) : PublicProfileUiState
    data class Error(val message: String) : PublicProfileUiState
}

class DisciplineViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val firebaseManager = FirebaseManager(application)
    private val repository = DisciplineRepository(application, database, firebaseManager)

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // --- User & Auth State ---
    private val _startupState = MutableStateFlow<StartupUiState>(StartupUiState.Loading)
    val startupState: StateFlow<StartupUiState> = _startupState.asStateFlow()

    val isAuthInitializing = MutableStateFlow(true)
    val startupError = MutableStateFlow<String?>(null)

    private var startupJob: Job? = null
    private var failsafeJob: Job? = null

    val activeUid = repository.activeUidFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentUser = activeUid.flatMapLatest { uid ->
        if (uid != null) repository.getUserProfile(uid) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var authError = MutableStateFlow<String?>(null)
        private set
    var authLoading = MutableStateFlow(false)
        private set

    init {
        startAppInitialization()
    }

    fun retryStartup() {
        startAppInitialization()
    }

    fun forceShowLogin() {
        viewModelScope.launch {
            failsafeJob?.cancel()
            startupJob?.cancel()
            repository.setActiveUid(null)
            _startupState.value = StartupUiState.Ready
            isAuthInitializing.value = false
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            failsafeJob?.cancel()
            startupJob?.cancel()
            authLoading.value = true
            startupError.value = null
            val res = repository.continueAsGuest()
            if (res.isSuccess) {
                authError.value = null
                _startupState.value = StartupUiState.Ready
            } else {
                authError.value = "Failed to start guest session."
            }
            authLoading.value = false
            isAuthInitializing.value = false
        }
    }

    fun startAppInitialization() {
        startupJob?.cancel()
        failsafeJob?.cancel()

        _startupState.value = StartupUiState.Loading
        isAuthInitializing.value = true
        startupError.value = null

        // 10-Second Watchdog Failsafe: Ensures UI never hangs indefinitely on loading
        failsafeJob = viewModelScope.launch(Dispatchers.Default) {
            delay(10_000L)
            if (_startupState.value is StartupUiState.Loading) {
                val err = "Startup timed out after 10 seconds. Network or cloud services are unreachable."
                android.util.Log.e("StartupSequence", err)
                _startupState.value = StartupUiState.Error("Something went wrong while loading.", err)
                startupError.value = "Something went wrong while loading."
                isAuthInitializing.value = false
            }
        }

        startupJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("StartupSequence", "1. Initializing Firebase safely in background...")
                val initResult = FirebaseManager.initialize(getApplication())
                if (initResult.isFailure) {
                    val err = initResult.exceptionOrNull()?.localizedMessage ?: "Failed to initialize Firebase"
                    android.util.Log.e("StartupSequence", "Firebase initialization failed: $err")
                    _startupState.value = StartupUiState.Error("Something went wrong while loading.", err)
                    startupError.value = "Something went wrong while loading."
                    failsafeJob?.cancel()
                    isAuthInitializing.value = false
                    return@launch
                }

                android.util.Log.d("StartupSequence", "2. Resolving Firebase Auth state...")
                val currentFbUser = withTimeoutOrNull(3000L) {
                    firebaseManager.getCurrentFirebaseUser() ?: run {
                        firebaseManager.authStateFlow().firstOrNull()
                    }
                }

                val storedUid = repository.getStoredActiveUidOnce()
                val isGuest = storedUid != null && storedUid.startsWith("guest_")

                if (currentFbUser != null) {
                    val uid = currentFbUser.uid
                    android.util.Log.d("StartupSequence", "3. Logged in user: $uid. Restoring data...")
                    repository.setActiveUid(uid)

                    // Optional data restoration with safety timeout (max 4s)
                    withTimeoutOrNull(4000L) {
                        repository.restoreCloudUserData(uid)
                    }

                    repository.ensureLocalProfileExists(uid, currentFbUser.email, currentFbUser.displayName)
                } else if (isGuest && storedUid != null) {
                    android.util.Log.d("StartupSequence", "3. Active guest session: $storedUid")
                    repository.setActiveUid(storedUid)
                    repository.ensureGuestProfileExists(storedUid)
                } else {
                    android.util.Log.d("StartupSequence", "3. User logged out, navigating to login.")
                    repository.setActiveUid(null)
                }

                failsafeJob?.cancel()
                _startupState.value = StartupUiState.Ready
                isAuthInitializing.value = false
                android.util.Log.i("StartupSequence", "Startup sequence completed successfully.")
                checkAndGenerateSmartReminders()
                refreshContentHub()

            } catch (e: Exception) {
                failsafeJob?.cancel()
                val err = e.localizedMessage ?: e.message ?: "Startup execution error"
                android.util.Log.e("StartupSequence", "Startup caught exception: $err", e)
                try {
                    val currentUid = firebaseManager.getCurrentFirebaseUser()?.uid
                    if (currentUid != null) {
                        repository.setActiveUid(currentUid)
                        repository.ensureLocalProfileExists(currentUid)
                        _startupState.value = StartupUiState.Ready
                    } else {
                        _startupState.value = StartupUiState.Error("Something went wrong while loading.", err)
                        startupError.value = "Something went wrong while loading."
                    }
                } catch (fallbackEx: Exception) {
                    _startupState.value = StartupUiState.Error("Something went wrong while loading.", err)
                    startupError.value = "Something went wrong while loading."
                }
                isAuthInitializing.value = false
            }
        }
    }

    // --- Date selection ---
    val todayDateStr = MutableStateFlow(LocalDate.now().format(isoFormatter))
    val selectedDateStr = MutableStateFlow(LocalDate.now().format(isoFormatter))

    fun refreshTodayDate() {
        val nowIso = LocalDate.now().format(isoFormatter)
        if (todayDateStr.value != nowIso) {
            todayDateStr.value = nowIso
        }
    }

    // --- Habits ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allHabits: StateFlow<List<Habit>> = activeUid.flatMapLatest { uid ->
        if (uid != null) repository.getHabits(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayCompletions: StateFlow<List<HabitCompletion>> = activeUid.flatMapLatest { uid ->
        if (uid != null) {
            todayDateStr.flatMapLatest { date ->
                repository.getCompletionsForDate(uid, date)
            }
        } else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allCompletions: StateFlow<List<HabitCompletion>> = activeUid.flatMapLatest { uid ->
        if (uid != null) repository.getAllCompletions(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Water ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userWaterGoal: StateFlow<Int?> = activeUid.flatMapLatest { uid ->
        if (uid != null) repository.getUserWaterGoalFlow(uid) else flowOf(3000)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3000)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayWaterRecord: StateFlow<WaterRecord?> = activeUid.flatMapLatest { uid ->
        if (uid != null) {
            todayDateStr.flatMapLatest { date ->
                repository.getWaterRecord(uid, date)
            }
        } else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayWaterEntries: StateFlow<List<WaterEntry>> = activeUid.flatMapLatest { uid ->
        if (uid != null) {
            todayDateStr.flatMapLatest { date ->
                repository.getWaterEntriesForDate(uid, date)
            }
        } else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val waterHistory: StateFlow<List<WaterRecord>> = activeUid.flatMapLatest { uid ->
        if (uid != null) repository.getAllWaterRecords(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waterReminderSettings: StateFlow<WaterReminderSettings> = repository.waterReminderSettings

    // --- Focus Sessions (needed by Content Hub recommendations and Focus features) ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allFocusSessions: StateFlow<List<FocusSession>> = activeUid.flatMapLatest { uid ->
        if (uid != null) repository.getAllFocusSessions(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val recentFocusSessions: StateFlow<List<FocusSession>> = activeUid.flatMapLatest { uid ->
        if (uid != null) repository.getRecentFocusSessions(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Content Hub State
    val contentItems = repository.contentItems
    val savedContentIds = repository.savedContentIds
    val viewedContentIds = repository.viewedContentIds

    private val _selectedContentCategory = MutableStateFlow(ContentCategory.ALL)
    val selectedContentCategory: StateFlow<ContentCategory> = _selectedContentCategory.asStateFlow()

    private val _isContentLoading = MutableStateFlow(false)
    val isContentLoading: StateFlow<Boolean> = _isContentLoading.asStateFlow()

    private val _contentLoadError = MutableStateFlow<String?>(null)
    val contentLoadError: StateFlow<String?> = _contentLoadError.asStateFlow()

    private val _contentSaveMessage = MutableStateFlow<String?>(null)
    val contentSaveMessage: StateFlow<String?> = _contentSaveMessage.asStateFlow()

    fun clearContentSaveMessage() {
        _contentSaveMessage.value = null
    }

    /**
     * Smart Content Personalization based on user activity:
     * - Active habits categories
     * - Focus sessions frequency (Productivity / Study)
     * - Saved content categories
     * - View history (prioritize unviewed / fresh content)
     */
    val forYouRecommendations: StateFlow<List<DisciplineContent>> = combine(
        contentItems,
        allHabits,
        recentFocusSessions,
        savedContentIds,
        viewedContentIds
    ) { items, habits, focusSessions, savedIds, viewedIds ->
        if (items.isEmpty()) return@combine emptyList()

        val activeCategories = mutableMapOf<String, Int>()

        // 1. Habits category weight
        habits.forEach { habit ->
            val catKey = habit.category.name.uppercase()
            activeCategories[catKey] = (activeCategories[catKey] ?: 0) + 3
            if (catKey == "FITNESS") activeCategories["FITNESS"] = (activeCategories["FITNESS"] ?: 0) + 2
            if (catKey == "PRODUCTIVITY" || catKey == "STUDY") {
                activeCategories["PRODUCTIVITY"] = (activeCategories["PRODUCTIVITY"] ?: 0) + 2
                activeCategories["STUDY"] = (activeCategories["STUDY"] ?: 0) + 2
            }
            if (catKey == "HEALTH") activeCategories["HEALTH"] = (activeCategories["HEALTH"] ?: 0) + 2
            if (catKey == "DISCIPLINE") activeCategories["DISCIPLINE"] = (activeCategories["DISCIPLINE"] ?: 0) + 2
            if (catKey == "MINDFULNESS") activeCategories["MINDSET"] = (activeCategories["MINDSET"] ?: 0) + 2
        }

        // 2. Focus activity weight
        if (focusSessions.isNotEmpty()) {
            activeCategories["PRODUCTIVITY"] = (activeCategories["PRODUCTIVITY"] ?: 0) + 4
            activeCategories["STUDY"] = (activeCategories["STUDY"] ?: 0) + 3
        }

        // 3. Saved content category weight
        items.filter { savedIds.contains(it.contentId) }.forEach { savedItem ->
            val cat = savedItem.category.uppercase()
            activeCategories[cat] = (activeCategories[cat] ?: 0) + 3
        }

        // Sort items by relevance score, giving boost to unviewed and featured items
        items.sortedWith { a, b ->
            val scoreA = calculateRecommendationScore(a, activeCategories, viewedIds)
            val scoreB = calculateRecommendationScore(b, activeCategories, viewedIds)
            scoreB.compareTo(scoreA)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun calculateRecommendationScore(
        item: DisciplineContent,
        categoryWeights: Map<String, Int>,
        viewedIds: Set<String>
    ): Double {
        var score = (categoryWeights[item.category.uppercase()] ?: 1).toDouble()
        if (item.isFeatured) score += 3.0
        // Unviewed items are preferred to avoid repetition
        if (!viewedIds.contains(item.contentId)) {
            score += 4.0
        } else {
            score -= 1.5
        }
        return score
    }

    /**
     * Trending / Popular Section
     */
    val trendingContent: StateFlow<List<DisciplineContent>> = contentItems.map { list ->
        list.sortedWith(
            compareByDescending<DisciplineContent> { it.isFeatured }
                .thenByDescending { it.createdAt }
        ).take(6)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * User's Saved Bookmarks
     */
    val savedContentList: StateFlow<List<DisciplineContent>> = combine(
        contentItems,
        savedContentIds
    ) { items, savedIds ->
        items.filter { savedIds.contains(it.contentId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectContentCategory(category: ContentCategory) {
        _selectedContentCategory.value = category
    }

    fun refreshContentHub() {
        val uid = activeUid.value
        viewModelScope.launch {
            _isContentLoading.value = true
            _contentLoadError.value = null
            try {
                repository.refreshContentHub(uid)
            } catch (e: Exception) {
                _contentLoadError.value = "Couldn't load content."
            } finally {
                _isContentLoading.value = false
            }
        }
    }

    fun toggleSaveContent(contentId: String) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            try {
                val wasSaved = repository.toggleSaveContent(uid, contentId)
                _contentSaveMessage.value = if (wasSaved) "Saved to Bookmarks 🔖" else "Removed from Saved"
            } catch (e: Exception) {
                _contentSaveMessage.value = "Couldn't save this content."
            }
        }
    }

    fun markContentAsViewed(contentId: String) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.markContentAsViewed(uid, contentId)
        }
    }

    val reminderSettings: StateFlow<ReminderSettings> = repository.reminderSettings

    // --- Challenge Alarms (Step 19) ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allAlarms: StateFlow<List<ChallengeAlarm>> = activeUid.flatMapLatest { uid ->
        if (uid != null) repository.getAlarmsFlow(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val challengeHistory: StateFlow<List<ChallengeHistoryEntry>> = activeUid.flatMapLatest { uid ->
        if (uid != null) repository.getChallengeHistoryFlow(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveAlarm(alarm: ChallengeAlarm, onComplete: (() -> Unit)? = null) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            val finalAlarm = if (alarm.userId.isBlank()) alarm.copy(userId = uid) else alarm
            repository.saveChallengeAlarm(finalAlarm)
            onComplete?.invoke()
        }
    }

    fun toggleAlarm(alarmId: String, isEnabled: Boolean) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.toggleChallengeAlarm(uid, alarmId, isEnabled)
        }
    }

    fun deleteAlarm(alarm: ChallengeAlarm) {
        viewModelScope.launch {
            repository.deleteChallengeAlarm(alarm)
        }
    }

    fun saveChallengeResult(entry: ChallengeHistoryEntry) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            val finalEntry = if (entry.userId.isBlank()) entry.copy(userId = uid) else entry
            repository.saveChallengeResult(finalEntry)
        }
    }

    // --- Focus Sessions Metrics & Timers ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayFocusMinutes: StateFlow<Int> = activeUid.flatMapLatest { uid ->
        if (uid != null) {
            todayDateStr.flatMapLatest { dateStr ->
                try {
                    val parsed = LocalDate.parse(dateStr, isoFormatter)
                    val startOfDay = parsed.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val endOfDay = parsed.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    repository.getFocusMinutesForDateRange(uid, startOfDay, endOfDay)
                } catch (e: Exception) {
                    flowOf(0)
                }
            }
        } else flowOf(0)
    }.map { it ?: 0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayFocusSessions: StateFlow<List<FocusSession>> = activeUid.flatMapLatest { uid ->
        if (uid != null) {
            todayDateStr.flatMapLatest { dateStr ->
                try {
                    val parsed = LocalDate.parse(dateStr, isoFormatter)
                    val startOfDay = parsed.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val endOfDay = parsed.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    repository.getFocusSessionsForDateRange(uid, startOfDay, endOfDay)
                } catch (e: Exception) {
                    flowOf(emptyList())
                }
            }
        } else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val totalFocusMinutes: StateFlow<Int> = activeUid.flatMapLatest { uid ->
        if (uid != null) repository.getTotalFocusMinutes(uid) else flowOf(0)
    }.map { it ?: 0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- Night Reviews ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayNightReview: StateFlow<NightReviewEntry?> = activeUid.flatMapLatest { uid ->
        if (uid != null) {
            todayDateStr.flatMapLatest { date ->
                repository.getNightReviewForDate(uid, date)
            }
        } else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allNightReviews: StateFlow<List<NightReviewEntry>> = activeUid.flatMapLatest { uid ->
        if (uid != null) repository.getAllNightReviews(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Timer state
    var isTimerRunning = MutableStateFlow(false)
        private set
    var timerRemainingSeconds = MutableStateFlow(25 * 60)
        private set
    var timerTotalSeconds = MutableStateFlow(25 * 60)
        private set
    var selectedFocusHabitId = MutableStateFlow<String?>(null)
        private set
    val focusSettings: StateFlow<FocusSettings> = repository.focusSettings
    val journalEntries: StateFlow<List<JournalEntry>> = repository.journalEntries
    val focusEventMessage = MutableSharedFlow<String>()
    private var timerJob: Job? = null

    fun addJournalEntry(title: String, content: String, category: String = "Reflection", tags: List<String> = emptyList()) {
        val uid = activeUid.value ?: return
        val now = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
        val todayStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        val entry = JournalEntry(
            userId = uid,
            title = title,
            content = content,
            time = now,
            date = todayStr,
            tags = tags,
            category = category,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.addJournalEntry(uid, entry)
        }
    }

    fun deleteJournalEntry(entryId: String) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.deleteJournalEntry(uid, entryId)
        }
    }

    // --- Achievements & Discipline Score ---
    val achievements: StateFlow<List<Achievement>> = repository.getAchievements().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Achievement unlock celebratory popup flow (guaranteed to trigger only once per unlock)
    private val _newlyUnlockedAchievement = MutableStateFlow<Achievement?>(null)
    val newlyUnlockedAchievement: StateFlow<Achievement?> = _newlyUnlockedAchievement.asStateFlow()

    private val seenUnlockedAchievementIds = mutableSetOf<String>()
    private var hasInitializedAchievementTracking = false

    init {
        viewModelScope.launch {
            achievements.collect { list ->
                if (!hasInitializedAchievementTracking) {
                    // Seed existing unlocked achievements on initial load so we don't spam celebrations
                    seenUnlockedAchievementIds.addAll(list.filter { it.isUnlocked }.map { it.id })
                    hasInitializedAchievementTracking = true
                } else {
                    val freshUnlock = list.firstOrNull { it.isUnlocked && !seenUnlockedAchievementIds.contains(it.id) }
                    if (freshUnlock != null) {
                        seenUnlockedAchievementIds.add(freshUnlock.id)
                        _newlyUnlockedAchievement.value = freshUnlock
                    }
                }
            }
        }
    }

    fun dismissUnlockedAchievementCelebration() {
        _newlyUnlockedAchievement.value = null
    }

    // Universal subtle glass toast feedback system
    private val _globalToastMessage = MutableStateFlow<String?>(null)
    val globalToastMessage: StateFlow<String?> = _globalToastMessage.asStateFlow()

    private val _isGlobalToastError = MutableStateFlow(false)
    val isGlobalToastError: StateFlow<Boolean> = _isGlobalToastError.asStateFlow()

    fun showToast(message: String, isError: Boolean = false) {
        _isGlobalToastError.value = isError
        _globalToastMessage.value = message
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000L)
            if (_globalToastMessage.value == message) {
                _globalToastMessage.value = null
            }
        }
    }

    fun dismissToast() {
        _globalToastMessage.value = null
    }

    val disciplineScoreBreakdown: StateFlow<DisciplineScoreBreakdown> = repository.disciplineScoreBreakdown

    // --- Monthly Tracker State ---
    val selectedYearMonth = MutableStateFlow(YearMonth.now())

    // --- Auth Actions ---
    fun signUp(email: String, pass: String, onSuccess: () -> Unit) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || pass.length < 6) {
            authError.value = "Please provide a valid email and minimum 6-character password."
            return
        }
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            val result = repository.signUp(cleanEmail, pass)
            authLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                authError.value = result.exceptionOrNull()?.message ?: "Signup failed. Please try again."
            }
        }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || pass.isBlank()) {
            authError.value = "Please enter your email and password."
            return
        }
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            val result = repository.login(cleanEmail, pass)
            authLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                authError.value = result.exceptionOrNull()?.message ?: "Login failed. Check your credentials."
            }
        }
    }

    fun continueAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            val result = repository.continueAsGuest()
            authLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                authError.value = "Failed to start guest session."
            }
        }
    }

    fun sendPasswordReset(email: String, onSent: () -> Unit) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) {
            authError.value = "Please enter your email to receive a reset link."
            return
        }
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            val result = repository.sendPasswordReset(email.trim())
            authLoading.value = false
            if (result.isSuccess) {
                onSent()
            } else {
                authError.value = result.exceptionOrNull()?.message ?: "Failed to send reset link."
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    // --- Public Profile & Discovery State ---
    val searchQuery = MutableStateFlow("")
    val searchResults = MutableStateFlow<List<PublicUserProfile>>(emptyList())
    val isSearching = MutableStateFlow(false)
    val searchError = MutableStateFlow<String?>(null)
    val publicProfileErrorMessage = MutableStateFlow<String?>(null)

    val publicProfileUiState = MutableStateFlow<PublicProfileUiState>(PublicProfileUiState.Idle)

    private var searchJob: Job? = null
    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
        searchPublicUsers(query)
    }

    fun searchPublicUsers(query: String) {
        searchJob?.cancel()
        val normalized = query.trim().trimStart('@').trim().lowercase()
        searchJob = viewModelScope.launch {
            isSearching.value = true
            searchError.value = null
            try {
                val results = repository.searchPublicUsers(normalized)
                searchResults.value = results
            } catch (e: Exception) {
                Log.w("DisciplineViewModel", "Search public users error: ${e.message}")
                searchError.value = e.message ?: "Failed to query public users"
                searchResults.value = emptyList()
            } finally {
                isSearching.value = false
            }
        }
    }

    fun clearPublicProfileError() {
        publicProfileErrorMessage.value = null
    }

    fun loadPublicProfile(username: String) {
        viewModelScope.launch {
            publicProfileUiState.value = PublicProfileUiState.Loading
            try {
                val profile = repository.fetchPublicProfile(username)
                if (profile == null) {
                    publicProfileUiState.value = PublicProfileUiState.NotFound(username)
                } else if (!profile.publicProfileEnabled) {
                    publicProfileUiState.value = PublicProfileUiState.Private(
                        username = profile.username,
                        displayName = profile.displayName,
                        photoUrl = profile.photoUrl
                    )
                } else {
                    publicProfileUiState.value = PublicProfileUiState.Success(profile)
                }
            } catch (e: Exception) {
                publicProfileUiState.value = PublicProfileUiState.Error(e.message ?: "Failed to load public profile")
            }
        }
    }

    fun togglePublicProfile(enabled: Boolean, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val uid = activeUid.value
        if (uid == null) {
            val err = "No active user session"
            publicProfileErrorMessage.value = err
            onResult(false, err)
            return
        }
        viewModelScope.launch {
            val result = repository.togglePublicProfile(uid, enabled)
            if (result.isSuccess) {
                publicProfileErrorMessage.value = null
                onResult(true, null)
            } else {
                val errMsg = result.exceptionOrNull()?.message ?: "Failed to update profile visibility"
                publicProfileErrorMessage.value = errMsg
                onResult(false, errMsg)
            }
        }
    }

    fun checkUsernameAvailability(username: String, onResult: (Boolean) -> Unit) {
        val uid = activeUid.value ?: ""
        viewModelScope.launch {
            val available = repository.checkUsernameAvailability(username, uid)
            onResult(available)
        }
    }

    // --- Global Leaderboard State & Actions ---
    private val _leaderboardEntries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = _leaderboardEntries.asStateFlow()

    private val _isLeaderboardLoading = MutableStateFlow(false)
    val isLeaderboardLoading: StateFlow<Boolean> = _isLeaderboardLoading.asStateFlow()

    private val _userLeaderboardRank = MutableStateFlow<Int?>(null)
    val userLeaderboardRank: StateFlow<Int?> = _userLeaderboardRank.asStateFlow()

    fun refreshLeaderboard() {
        val uid = activeUid.value
        viewModelScope.launch {
            _isLeaderboardLoading.value = true
            try {
                val list = repository.getGlobalLeaderboard(100)
                _leaderboardEntries.value = list

                if (uid != null) {
                    val userEntry = list.find { it.uid == uid }
                    val currentRank = userEntry?.rank
                    _userLeaderboardRank.value = currentRank

                    // Check and unlock leaderboard achievements if participating
                    if (currentRank != null && currentRank > 0) {
                        repository.evaluateLeaderboardRankAchievements(uid, currentRank)
                    }
                }
            } catch (e: Exception) {
                Log.w("DisciplineViewModel", "Leaderboard load failed: ${e.message}")
            } finally {
                _isLeaderboardLoading.value = false
            }
        }
    }

    fun toggleLeaderboardParticipation(enabled: Boolean, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val uid = activeUid.value
        if (uid == null) {
            onResult(false, "No active user session")
            return
        }
        viewModelScope.launch {
            val result = repository.toggleLeaderboardParticipation(uid, enabled)
            if (result.isSuccess) {
                refreshLeaderboard()
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to update leaderboard settings")
            }
        }
    }

    // --- Launch Animation Setting ---
    private val appPrefs by lazy {
        getApplication<Application>().getSharedPreferences("dayforge_prefs", Context.MODE_PRIVATE)
    }

    val isLaunchAnimationEnabled = MutableStateFlow(
        getApplication<Application>()
            .getSharedPreferences("dayforge_prefs", Context.MODE_PRIVATE)
            .getBoolean("launch_animation_enabled", true)
    )

    fun toggleLaunchAnimation(enabled: Boolean) {
        isLaunchAnimationEnabled.value = enabled
        appPrefs.edit().putBoolean("launch_animation_enabled", enabled).apply()
    }

    // --- Reduced Motion Setting ---
    val isReducedMotionEnabled = MutableStateFlow(
        getApplication<Application>()
            .getSharedPreferences("dayforge_prefs", Context.MODE_PRIVATE)
            .getBoolean("reduced_motion_enabled", false)
    )

    fun toggleReducedMotion(enabled: Boolean) {
        isReducedMotionEnabled.value = enabled
        appPrefs.edit().putBoolean("reduced_motion_enabled", enabled).apply()
    }

    // --- Centralized Smart Notification Center State & Actions ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val notifications = activeUid.flatMapLatest { uid ->
        if (uid != null) repository.getNotificationsFlow(uid)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val unreadNotificationsCount = activeUid.flatMapLatest { uid ->
        if (uid != null) repository.getUnreadNotificationsCountFlow(uid)
        else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markNotificationAsRead(id: String) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.markNotificationAsRead(id, uid, isRead = true)
        }
    }

    fun markAllNotificationsAsRead() {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(uid)
        }
    }

    fun deleteNotification(id: String) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.deleteNotification(id, uid)
        }
    }

    fun checkAndGenerateSmartReminders() {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.generateSmartRemindersIfDue(uid)
        }
    }

    // --- Profile Setup & Edit ---
    fun saveProfile(
        username: String,
        displayName: String,
        bio: String,
        photoUrl: String,
        publicProfileEnabled: Boolean? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            authLoading.value = true
            val result = repository.saveUserProfile(uid, username, displayName, bio, photoUrl, publicProfileEnabled)
            authLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Failed to save profile")
            }
        }
    }

    // --- Habit Actions ---
    fun saveHabit(
        id: String?,
        title: String,
        description: String,
        category: HabitCategory,
        iconName: String,
        colorHex: Long,
        reminderTime: String?,
        repeatDays: String,
        goalValue: Int,
        goalUnit: String,
        durationMinutes: Int = 0,
        startDate: String? = null,
        endDate: String? = null,
        difficulty: HabitDifficulty = HabitDifficulty.MEDIUM,
        notes: String = "",
        isMorningRoutine: Boolean = false,
        goalType: HabitGoalType = HabitGoalType.ONCE,
        isPaused: Boolean = false
    ) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            val habit = Habit(
                id = id ?: "",
                userId = uid,
                title = title.trim(),
                description = description.trim(),
                category = category,
                iconName = iconName,
                colorHex = colorHex,
                reminderTime = reminderTime?.trim()?.ifBlank { null },
                repeatDays = repeatDays,
                goalValue = goalValue,
                goalUnit = goalUnit.trim().ifBlank { "times" },
                durationMinutes = durationMinutes,
                startDate = startDate?.trim()?.ifBlank { null },
                endDate = endDate?.trim()?.ifBlank { null },
                difficulty = difficulty,
                notes = notes.trim(),
                isMorningRoutine = isMorningRoutine,
                goalType = goalType,
                isPaused = isPaused,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveHabit(habit)
            repository.recalculateUserStats(uid)
            showToast(if (id.isNullOrBlank()) "Habit created ✨" else "Habit updated ✨")
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            val uid = activeUid.value ?: return@launch
            repository.recalculateUserStats(uid)
            showToast("Habit deleted")
        }
    }

    fun toggleHabitPause(habit: Habit) {
        viewModelScope.launch {
            val willPause = !habit.isPaused
            repository.toggleHabitPause(habit)
            showToast(if (willPause) "Habit paused ⏸️" else "Habit resumed ▶️")
        }
    }

    fun toggleHabitCompletion(habitId: String, date: String = todayDateStr.value) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            val wasCompleted = todayCompletions.value.any { it.habitId == habitId }
            repository.toggleHabitCompletion(uid, habitId, date)
            if (wasCompleted) {
                showToast("Habit unmarked")
            } else {
                showToast("Habit completed! Streak forged 🔥")
            }
        }
    }

    fun setHabitStatusForDate(habitId: String, date: String, status: CompletionStatus?) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            if (status == null) {
                repository.toggleHabitCompletion(uid, habitId, date, null)
            } else {
                repository.toggleHabitCompletion(uid, habitId, date, status)
            }
        }
    }

    // --- Water Actions ---
    fun addWater(amountMl: Int) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.addWater(uid, todayDateStr.value, amountMl)
            showToast("Added +${amountMl} ml water 💧")
        }
    }

    fun editWaterEntry(entryId: String, date: String = todayDateStr.value, newAmountMl: Int) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.editWaterEntry(uid, entryId, date, newAmountMl)
        }
    }

    fun deleteWaterEntry(entryId: String, date: String = todayDateStr.value) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.deleteWaterEntry(uid, entryId, date)
        }
    }

    fun setWaterGoal(goalMl: Int, date: String = todayDateStr.value) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.setWaterTarget(uid, date, goalMl)
        }
    }

    fun updateWaterReminderSettings(settings: WaterReminderSettings) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.saveWaterReminderSettings(uid, settings)
        }
    }

    fun updateReminderSettings(settings: ReminderSettings) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.saveReminderSettings(uid, settings)
        }
    }

    fun toggleHabitReminder(habitId: String, isEnabled: Boolean, time: String? = null) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.toggleHabitReminder(uid, habitId, isEnabled, time)
        }
    }

    fun updateNightReviewSettings(isEnabled: Boolean, time: String) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.updateNightReviewReminder(uid, isEnabled, time)
        }
    }

    fun saveNightReview(entry: NightReviewEntry, onComplete: () -> Unit = {}) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.saveNightReview(uid, entry.copy(userId = uid, updatedAt = System.currentTimeMillis()))
            showToast("Night review recorded 🌙")
            onComplete()
        }
    }

    suspend fun getNightReview(date: String): NightReviewEntry? {
        val uid = activeUid.value ?: return null
        return repository.fetchNightReview(uid, date)
    }

    fun sendTestWaterNotification() {
        repository.triggerTestWaterReminder()
    }

    fun sendTestNotification(type: ReminderType, customTitle: String = "Morning Focus") {
        repository.triggerTestNotification(type, customTitle)
    }

    private var targetEndTimeMillis: Long? = null

    // --- Focus Timer Actions (Step 20 Upgraded) ---
    fun setFocusPreset(minutes: Int, habitId: String? = selectedFocusHabitId.value) {
        timerJob?.cancel()
        isTimerRunning.value = false
        targetEndTimeMillis = null
        val durationMins = minutes.coerceAtLeast(1)
        val seconds = durationMins * 60
        timerTotalSeconds.value = seconds
        timerRemainingSeconds.value = seconds
        selectedFocusHabitId.value = habitId

        // Also persist duration in focusSettings
        val current = focusSettings.value
        if (current.durationMinutes != durationMins || current.linkedHabitId != habitId) {
            updateFocusDuration(durationMins)
            if (habitId != current.linkedHabitId) {
                updateFocusLinkedHabit(habitId)
            }
        }
    }

    fun setCustomFocusDuration(minutes: Int) {
        setFocusPreset(minutes, selectedFocusHabitId.value)
    }

    fun setSelectedFocusHabit(habitId: String?) {
        selectedFocusHabitId.value = habitId
        updateFocusLinkedHabit(habitId)
    }

    fun updateFocusDuration(minutes: Int) {
        val uid = activeUid.value ?: return
        val updated = focusSettings.value.copy(
            durationMinutes = minutes.coerceAtLeast(1),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveFocusSettings(uid, updated)
        }
    }

    fun updateFocusLinkedHabit(habitId: String?) {
        val uid = activeUid.value ?: return
        val updated = focusSettings.value.copy(
            linkedHabitId = habitId,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveFocusSettings(uid, updated)
        }
    }

    fun updateFocusRestrictionEnabled(enabled: Boolean) {
        val uid = activeUid.value ?: return
        val updated = focusSettings.value.copy(
            restrictionEnabled = enabled,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveFocusSettings(uid, updated)
        }
    }

    fun updateRestrictedApps(apps: List<String>) {
        val uid = activeUid.value ?: return
        val updated = focusSettings.value.copy(
            restrictedApps = apps.distinct(),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveFocusSettings(uid, updated)
        }
    }

    fun addRestrictedApp(packageName: String) {
        val currentList = focusSettings.value.restrictedApps.toMutableList()
        if (!currentList.contains(packageName)) {
            currentList.add(packageName)
            updateRestrictedApps(currentList)
        }
    }

    fun removeRestrictedApp(packageName: String) {
        val currentList = focusSettings.value.restrictedApps.toMutableList()
        if (currentList.remove(packageName)) {
            updateRestrictedApps(currentList)
        }
    }

    fun updateRelatedApp(packageName: String?, appName: String?) {
        val uid = activeUid.value ?: return
        val updated = focusSettings.value.copy(
            relatedAppPackage = packageName,
            relatedAppName = appName,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveFocusSettings(uid, updated)
        }
    }

    fun updateAutoOpenRelatedApp(enabled: Boolean) {
        val uid = activeUid.value ?: return
        val updated = focusSettings.value.copy(
            autoOpenRelatedApp = enabled,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveFocusSettings(uid, updated)
        }
    }

    fun startFocusTimer(context: Context? = null) {
        if (isTimerRunning.value) return
        timerJob?.cancel()
        if (timerRemainingSeconds.value <= 0) {
            timerRemainingSeconds.value = timerTotalSeconds.value
        }
        val target = System.currentTimeMillis() + (timerRemainingSeconds.value * 1000L)
        targetEndTimeMillis = target
        isTimerRunning.value = true

        val habitId = selectedFocusHabitId.value ?: focusSettings.value.linkedHabitId
        val habitTitle = if (habitId != null) {
            allHabits.value.firstOrNull { it.id == habitId }?.title ?: "Deep Work"
        } else {
            "General Flow"
        }

        // Save active session locally & launch native features if context available
        if (context != null) {
            val settings = focusSettings.value
            FocusSessionManager.saveActiveSession(
                context = context,
                sessionId = UUID.randomUUID().toString(),
                targetEndTimeMillis = target,
                totalSeconds = timerTotalSeconds.value,
                habitId = habitId,
                habitTitle = habitTitle,
                restrictionEnabled = settings.restrictionEnabled,
                restrictedApps = settings.restrictedApps
            )

            // Auto-open related app if requested (Requirement 6)
            if (settings.autoOpenRelatedApp && !settings.relatedAppPackage.isNullOrBlank()) {
                val launched = FocusSessionManager.launchRelatedApp(context, settings.relatedAppPackage)
                if (!launched) {
                    viewModelScope.launch {
                        focusEventMessage.emit("Related app isn't available on this device.")
                    }
                }
            }

            // Start native restriction service if enabled
            if (settings.restrictionEnabled) {
                FocusRestrictionService.startService(context)
            }
        }

        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isTimerRunning.value) {
                val now = System.currentTimeMillis()
                val targetTime = targetEndTimeMillis ?: break
                val diff = targetTime - now
                if (diff <= 0L) {
                    timerRemainingSeconds.value = 0
                    isTimerRunning.value = false
                    targetEndTimeMillis = null
                    completeFocusSession(context)
                    break
                } else {
                    val secsLeft = ((diff + 999L) / 1000L).toInt()
                    if (timerRemainingSeconds.value != secsLeft) {
                        timerRemainingSeconds.value = secsLeft
                        if (context != null) {
                            FocusSessionManager.updateRemainingTime(context, secsLeft)
                        }
                    }
                }
                delay(200)
            }
        }
    }

    fun pauseFocusTimer(context: Context? = null) {
        timerJob?.cancel()
        isTimerRunning.value = false
        val target = targetEndTimeMillis
        if (target != null) {
            val now = System.currentTimeMillis()
            val secsLeft = (((target - now) + 999L) / 1000L).coerceAtLeast(0L).toInt()
            timerRemainingSeconds.value = secsLeft
            if (context != null) {
                FocusSessionManager.updateRemainingTime(context, secsLeft)
            }
        }
        targetEndTimeMillis = null
        if (context != null) {
            FocusRestrictionService.stopService(context)
        }
    }

    fun resetFocusTimer(context: Context? = null) {
        timerJob?.cancel()
        isTimerRunning.value = false
        targetEndTimeMillis = null
        timerRemainingSeconds.value = timerTotalSeconds.value
        if (context != null) {
            FocusSessionManager.clearActiveSession(context)
            FocusRestrictionService.stopService(context)
        }
    }

    fun syncFocusTimerWithSystem(context: Context) {
        if (!isTimerRunning.value && FocusSessionManager.isSessionRunning(context)) {
            val remaining = FocusSessionManager.computeRemainingSeconds(context)
            if (remaining <= 0) {
                completeFocusSession(context)
            } else {
                timerRemainingSeconds.value = remaining
                timerTotalSeconds.value = FocusSessionManager.getTotalSeconds(context)
                selectedFocusHabitId.value = FocusSessionManager.getHabitId(context)
                targetEndTimeMillis = FocusSessionManager.getTargetEndTimeMillis(context)
                isTimerRunning.value = true
                startFocusTimer(context)
            }
        } else if (isTimerRunning.value) {
            val target = targetEndTimeMillis ?: return
            val diff = target - System.currentTimeMillis()
            if (diff <= 0L) {
                timerRemainingSeconds.value = 0
                isTimerRunning.value = false
                targetEndTimeMillis = null
                completeFocusSession(context)
            } else {
                timerRemainingSeconds.value = ((diff + 999L) / 1000L).toInt()
            }
        }
    }

    private fun completeFocusSession(context: Context? = null) {
        if (context != null) {
            FocusSessionManager.clearActiveSession(context)
            FocusRestrictionService.stopService(context)
        }
        val uid = activeUid.value ?: return
        val totalMins = (timerTotalSeconds.value / 60).coerceAtLeast(1)
        val habitId = selectedFocusHabitId.value ?: focusSettings.value.linkedHabitId
        val habitTitle = if (habitId != null) {
            allHabits.value.firstOrNull { it.id == habitId }?.title ?: "Deep Work"
        } else {
            "General Flow"
        }
        viewModelScope.launch(Dispatchers.IO) {
            val sessionId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val session = FocusSession(
                id = sessionId,
                userId = uid,
                habitId = habitId,
                habitTitle = habitTitle,
                durationMinutes = totalMins,
                completedMinutes = totalMins,
                timestamp = now,
                isCompleted = true
            )
            repository.logFocusSession(session)
            if (habitId != null) {
                repository.toggleHabitCompletion(uid, habitId, todayDateStr.value, CompletionStatus.COMPLETED)
            }
            val focusNotif = AppNotification(
                id = UUID.randomUUID().toString(),
                userId = uid,
                title = "Focus Session Completed 🎯",
                message = "Great discipline! You completed a $totalMins-minute flow block for '$habitTitle'. Momentum is building!",
                type = NotificationType.FOCUS,
                targetDestination = "focus",
                relatedEntityId = sessionId,
                timestamp = now,
                isRead = false,
                scheduledDate = todayDateStr.value
            )
            repository.addNotification(focusNotif)
        }
    }

    // --- Monthly Navigation ---
    fun previousMonth() {
        selectedYearMonth.value = selectedYearMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        selectedYearMonth.value = selectedYearMonth.value.plusMonths(1)
    }

    fun setMonth(yearMonth: YearMonth) {
        selectedYearMonth.value = yearMonth
    }

    // --- Night Review Actions ---
    fun deleteNightReview(date: String, onComplete: () -> Unit = {}) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            repository.deleteNightReview(uid, date)
            onComplete()
        }
    }

    fun fetchNightReviewForDate(date: String, onResult: (NightReviewEntry?) -> Unit) {
        val uid = activeUid.value ?: return
        viewModelScope.launch {
            val review = repository.fetchNightReview(uid, date)
            onResult(review)
        }
    }

    // ==========================================
    // --- Step 17: Backup & Data Management ---
    // ==========================================
    val isExportingBackup = MutableStateFlow(false)
    val isImportingBackup = MutableStateFlow(false)
    val pendingBackupSummary = MutableStateFlow<BackupSummary?>(null)
    val backupOperationMessage = MutableStateFlow<String?>(null)
    val backupErrorMessage = MutableStateFlow<String?>(null)

    fun prepareExportJson(onReady: (String) -> Unit, onError: (String) -> Unit) {
        val uid = activeUid.value ?: run {
            onError("You must have an active session to export backup.")
            return
        }
        viewModelScope.launch {
            isExportingBackup.value = true
            backupErrorMessage.value = null
            backupOperationMessage.value = "Packaging DayForge backup..."
            val result = repository.createBackupJson(uid)
            isExportingBackup.value = false
            if (result.isSuccess) {
                val json = result.getOrThrow()
                backupOperationMessage.value = null
                onReady(json)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to export backup."
                backupErrorMessage.value = err
                backupOperationMessage.value = null
                onError(err)
            }
        }
    }

    fun saveExportToUri(json: String, uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isExportingBackup.value = true
            backupErrorMessage.value = null
            backupOperationMessage.value = "Saving backup file to device..."
            val result = repository.writeBackupToUri(json, uri)
            isExportingBackup.value = false
            if (result.isSuccess) {
                backupOperationMessage.value = "Backup exported successfully! 📁"
                onSuccess()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to save file."
                backupErrorMessage.value = err
                onError(err)
            }
        }
    }

    fun validateSelectedBackupFile(uri: Uri, onValidated: (BackupSummary) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isImportingBackup.value = true
            backupErrorMessage.value = null
            backupOperationMessage.value = "Validating DayForge backup file..."
            val result = repository.validateBackupFromUri(uri)
            isImportingBackup.value = false
            backupOperationMessage.value = null
            if (result.isSuccess) {
                val summary = result.getOrThrow()
                pendingBackupSummary.value = summary
                onValidated(summary)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Invalid or corrupted backup file."
                backupErrorMessage.value = err
                onError(err)
            }
        }
    }

    fun clearPendingBackup() {
        pendingBackupSummary.value = null
        backupErrorMessage.value = null
        backupOperationMessage.value = null
    }

    fun executeRestoreBackup(
        summary: BackupSummary,
        autoSafetySnapshot: Boolean = true,
        onSuccess: (BackupRestoreReport) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = activeUid.value ?: run {
            onError("You must be logged in to restore data.")
            return
        }
        viewModelScope.launch {
            isImportingBackup.value = true
            backupErrorMessage.value = null
            backupOperationMessage.value = "Restoring DayForge records & applying safety snapshot..."
            val result = repository.restoreBackup(uid, summary.rawJson, autoSafetySnapshot)
            isImportingBackup.value = false
            backupOperationMessage.value = null
            if (result.isSuccess) {
                val report = result.getOrThrow()
                pendingBackupSummary.value = null
                backupOperationMessage.value = "Restoration complete! ${report.habitsRestored} habits & ${report.completionsRestored} check-ins restored."
                onSuccess(report)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to restore backup."
                backupErrorMessage.value = err
                onError(err)
            }
        }
    }
}
