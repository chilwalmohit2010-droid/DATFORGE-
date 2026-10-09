package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.models.Habit
import com.example.ui.components.AchievementCelebrationDialog
import com.example.ui.components.DayForgeLiveLaunchAnimation
import com.example.ui.components.DayForgeMountainLogo
import com.example.ui.components.DisciplineBottomBar
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.GlassToastHost
import com.example.ui.components.NavTab
import com.example.ui.screens.alarm.AlarmManagementScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.auth.ProfileSetupScreen
import com.example.ui.screens.discovery.DiscoverUsersScreen
import com.example.ui.screens.focus.FocusScreen
import com.example.ui.screens.habits.AddEditHabitDialog
import com.example.ui.screens.habits.HabitsScreen
import com.example.ui.screens.habits.MonthlyTrackerScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.leaderboard.LeaderboardScreen
import com.example.ui.screens.notifications.NotificationCenterScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.PublicProfileScreen
import com.example.ui.screens.progress.ProgressScreen
import com.example.ui.screens.reminders.NightReviewDialog
import com.example.ui.screens.reminders.NightReviewScreen
import com.example.ui.screens.reminders.RemindersSettingsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.water.WaterTrackerSheet
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel
import com.example.ui.viewmodel.StartupUiState

enum class AppRootState {
    LOADING,
    AUTH,
    PROFILE_SETUP,
    MAIN_APP
}

sealed class AppDestination {
    data class Tab(val tab: NavTab) : AppDestination()
    data class PublicProfile(val username: String) : AppDestination()
    object DiscoverUsers : AppDestination()
    object NotificationCenter : AppDestination()
    object MonthlyTracker : AppDestination()
    object RemindersSettings : AppDestination()
    object NightReview : AppDestination()
    object Settings : AppDestination()
    object Alarms : AppDestination()
    object Leaderboard : AppDestination()
    object ContentHub : AppDestination()
}

@Composable
fun DisciplineApp(
    viewModel: DisciplineViewModel = viewModel(),
    initialDestination: String? = null
) {
    val startupState by viewModel.startupState.collectAsState()
    val activeUid by viewModel.activeUid.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val newlyUnlockedAchievement by viewModel.newlyUnlockedAchievement.collectAsState()
    val globalToastMessage by viewModel.globalToastMessage.collectAsState()
    val isGlobalToastError by viewModel.isGlobalToastError.collectAsState()

    var currentTab by remember { mutableStateOf(NavTab.HOME) }
    var showMonthlyTracker by remember { mutableStateOf(false) }
    var showRemindersSettings by remember { mutableStateOf(false) }
    var showNightReviewScreen by remember { mutableStateOf(false) }
    var showNightReviewDialog by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }
    var showAddEditHabitDialog by remember { mutableStateOf(false) }
    var showWaterTracker by remember { mutableStateOf(false) }
    var showDiscoverUsers by remember { mutableStateOf(false) }
    var showNotificationCenter by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAlarms by remember { mutableStateOf(false) }
    var showLeaderboardScreen by remember { mutableStateOf(false) }
    var showContentHub by remember { mutableStateOf(false) }
    var showQuickActionSheet by remember { mutableStateOf(false) }
    var viewingPublicUsername by remember { mutableStateOf<String?>(null) }

    // Launch Animation & Reduced Motion settings
    val isLaunchAnimEnabled by viewModel.isLaunchAnimationEnabled.collectAsState()
    val isReducedMotion by viewModel.isReducedMotionEnabled.collectAsState()
    var isLaunchAnimationPlaying by rememberSaveable { mutableStateOf(isLaunchAnimEnabled) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTodayDate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(initialDestination) {
        when (initialDestination) {
            "notifications" -> showNotificationCenter = true
            "settings" -> showSettings = true
            "night_review" -> showNightReviewScreen = true
            "water" -> showWaterTracker = true
            "habits" -> currentTab = NavTab.HABITS
            "reminders" -> showRemindersSettings = true
            "alarms" -> showAlarms = true
            "leaderboard" -> currentTab = NavTab.LEADERBOARD
            "content" -> showContentHub = true
            "discover" -> showContentHub = true
        }
    }

    val rootState = when {
        startupState is StartupUiState.Loading || startupState is StartupUiState.Error -> AppRootState.LOADING
        activeUid == null -> AppRootState.AUTH
        currentUser == null || currentUser?.username.isNullOrBlank() || currentUser?.isProfileComplete == false -> AppRootState.PROFILE_SETUP
        else -> AppRootState.MAIN_APP
    }

    DisciplineTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isLaunchAnimationPlaying) {
                // 0. Premium 4-Second Live Construction Launch Animation (Cold Launch Only)
                DayForgeLiveLaunchAnimation(
                    onAnimationComplete = {
                        isLaunchAnimationPlaying = false
                    },
                    isReducedMotion = isReducedMotion
                )
            } else {
                AnimatedContent(
                    targetState = rootState,
                    transitionSpec = {
                        if (isReducedMotion) {
                            fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                        } else {
                            (fadeIn(tween(350)) + scaleIn(initialScale = 0.98f)) togetherWith fadeOut(tween(250))
                        }
                    },
                    label = "rootAppTransition"
                ) { state ->
                    when (state) {
                        AppRootState.LOADING -> {
                            LiquidGlassLoadingScreen(
                                state = startupState,
                                onRetry = { viewModel.retryStartup() },
                                onContinueAsGuest = { viewModel.continueAsGuest() },
                                onGoToSignIn = { viewModel.forceShowLogin() }
                            )
                        }

                        AppRootState.AUTH -> {
                            AuthScreen(
                                viewModel = viewModel,
                                onAuthSuccess = { /* state handled by Flow */ }
                            )
                        }

                        AppRootState.PROFILE_SETUP -> {
                            ProfileSetupScreen(
                                viewModel = viewModel,
                                onCompleted = { /* state handled by Flow */ }
                            )
                        }

                        AppRootState.MAIN_APP -> {
                            // Compute current screen destination
                            val currentDestination: AppDestination = when {
                                viewingPublicUsername != null -> AppDestination.PublicProfile(viewingPublicUsername!!)
                                showDiscoverUsers -> AppDestination.DiscoverUsers
                                showSettings -> AppDestination.Settings
                                showLeaderboardScreen -> AppDestination.Leaderboard
                                showNotificationCenter -> AppDestination.NotificationCenter
                                showMonthlyTracker -> AppDestination.MonthlyTracker
                                showRemindersSettings -> AppDestination.RemindersSettings
                                showNightReviewScreen -> AppDestination.NightReview
                                showAlarms -> AppDestination.Alarms
                                showContentHub -> AppDestination.ContentHub
                                else -> AppDestination.Tab(currentTab)
                            }

                            Scaffold(
                                bottomBar = {
                                    val isMainTab = currentDestination is AppDestination.Tab
                                    AnimatedVisibility(
                                        visible = isMainTab,
                                        enter = if (isReducedMotion) fadeIn(tween(150)) else slideInVertically(
                                            initialOffsetY = { it },
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) + fadeIn(tween(180)),
                                        exit = if (isReducedMotion) fadeOut(tween(150)) else slideOutVertically(
                                            targetOffsetY = { it },
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) + fadeOut(tween(150))
                                    ) {
                                        DisciplineBottomBar(
                                            currentTab = currentTab,
                                            onTabSelected = { selected ->
                                                currentTab = selected
                                            },
                                            onPlusClick = {
                                                showQuickActionSheet = true
                                            },
                                            modifier = Modifier.testTag("discipline_bottom_bar"),
                                            isReducedMotion = isReducedMotion
                                        )
                                    }
                                }
                            ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    bottom = if (currentDestination is AppDestination.Tab) innerPadding.calculateBottomPadding() else 0.dp
                                )
                        ) {
                            BackHandler(enabled = showQuickActionSheet) {
                                showQuickActionSheet = false
                            }
                            BackHandler(enabled = showWaterTracker) {
                                showWaterTracker = false
                            }
                            BackHandler(enabled = showAddEditHabitDialog) {
                                showAddEditHabitDialog = false
                            }
                            BackHandler(enabled = showNightReviewDialog) {
                                showNightReviewDialog = false
                            }
                            BackHandler(enabled = showNightReviewScreen) {
                                showNightReviewScreen = false
                            }
                            BackHandler(enabled = showMonthlyTracker) {
                                showMonthlyTracker = false
                            }
                            BackHandler(enabled = showRemindersSettings) {
                                showRemindersSettings = false
                            }
                            BackHandler(enabled = showDiscoverUsers) {
                                showDiscoverUsers = false
                            }
                            BackHandler(enabled = viewingPublicUsername != null) {
                                viewingPublicUsername = null
                            }
                            BackHandler(enabled = showSettings) {
                                showSettings = false
                            }
                            BackHandler(enabled = showNotificationCenter) {
                                showNotificationCenter = false
                            }
                            BackHandler(enabled = showAlarms) {
                                showAlarms = false
                            }
                            BackHandler(enabled = showLeaderboardScreen) {
                                showLeaderboardScreen = false
                            }
                            BackHandler(enabled = showContentHub) {
                                showContentHub = false
                            }
                            BackHandler(enabled = currentTab != NavTab.HOME && currentDestination is AppDestination.Tab) {
                                currentTab = NavTab.HOME
                            }

                            AnimatedContent(
                                targetState = currentDestination,
                                transitionSpec = {
                                    if (isReducedMotion) {
                                        fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                                    } else {
                                        val isInitialSubscreen = initialState !is AppDestination.Tab
                                        val isTargetSubscreen = targetState !is AppDestination.Tab

                                        when {
                                            // Pushing to a detail subscreen
                                            !isInitialSubscreen && isTargetSubscreen -> {
                                                (slideInHorizontally(
                                                    initialOffsetX = { (it * 0.12f).toInt() },
                                                    animationSpec = spring(
                                                        dampingRatio = 0.86f,
                                                        stiffness = 400f
                                                    )
                                                ) + scaleIn(initialScale = 0.965f, animationSpec = spring(dampingRatio = 0.86f, stiffness = 400f)) + fadeIn(tween(200))) togetherWith (
                                                    scaleOut(targetScale = 0.965f, animationSpec = spring(dampingRatio = 0.86f, stiffness = 400f)) +
                                                    fadeOut(tween(180))
                                                )
                                            }

                                            // Popping back from detail subscreen to main tabs
                                            isInitialSubscreen && !isTargetSubscreen -> {
                                                (scaleIn(initialScale = 0.965f, animationSpec = spring(dampingRatio = 0.86f, stiffness = 400f)) + fadeIn(tween(200))) togetherWith (
                                                    slideOutHorizontally(
                                                        targetOffsetX = { (it * 0.12f).toInt() },
                                                        animationSpec = spring(
                                                            dampingRatio = 0.86f,
                                                            stiffness = 400f
                                                        )
                                                    ) + scaleOut(targetScale = 0.965f, animationSpec = spring(dampingRatio = 0.86f, stiffness = 400f)) + fadeOut(tween(180))
                                                )
                                            }

                                            // Switching between tabs (iOS 27 spring scale + fade + subtle translation)
                                            initialState is AppDestination.Tab && targetState is AppDestination.Tab -> {
                                                val initialOrdinal = (initialState as AppDestination.Tab).tab.ordinal
                                                val targetOrdinal = (targetState as AppDestination.Tab).tab.ordinal
                                                val direction = if (targetOrdinal > initialOrdinal) 1 else -1

                                                (fadeIn(tween(220, easing = LinearOutSlowInEasing)) +
                                                 scaleIn(initialScale = 0.97f, animationSpec = spring(dampingRatio = 0.86f, stiffness = 420f)) +
                                                 slideInHorizontally(initialOffsetX = { (it * 0.08f * direction).toInt() }, animationSpec = spring(dampingRatio = 0.86f, stiffness = 420f))) togetherWith (
                                                    fadeOut(tween(180, easing = FastOutLinearInEasing)) +
                                                    scaleOut(targetScale = 0.97f, animationSpec = spring(dampingRatio = 0.86f, stiffness = 420f)) +
                                                    slideOutHorizontally(targetOffsetX = { (-it * 0.08f * direction).toInt() }, animationSpec = spring(dampingRatio = 0.86f, stiffness = 420f))
                                                )
                                            }

                                            // Default fallback
                                            else -> {
                                                (scaleIn(initialScale = 0.98f) + fadeIn(tween(180))) togetherWith (scaleOut(targetScale = 0.98f) + fadeOut(tween(160)))
                                            }
                                        }
                                    }
                                },
                                label = "appNavigationTransition"
                            ) { destination ->
                                when (destination) {
                                    is AppDestination.Settings -> {
                                        SettingsScreen(
                                            viewModel = viewModel,
                                            onNavigateBack = { showSettings = false },
                                            onOpenReminders = { showRemindersSettings = true },
                                            onOpenPublicProfile = { uname -> viewingPublicUsername = uname },
                                            onLogout = {
                                                showSettings = false
                                                currentTab = NavTab.HOME
                                            }
                                        )
                                    }
                                    is AppDestination.PublicProfile -> {
                                        PublicProfileScreen(
                                            username = destination.username,
                                            viewModel = viewModel,
                                            onNavigateBack = { viewingPublicUsername = null }
                                        )
                                    }
                                    is AppDestination.DiscoverUsers -> {
                                        DiscoverUsersScreen(
                                            viewModel = viewModel,
                                            onNavigateBack = { showDiscoverUsers = false },
                                            onSelectUser = { username ->
                                                viewingPublicUsername = username
                                            }
                                        )
                                    }
                                    is AppDestination.NotificationCenter -> {
                                        NotificationCenterScreen(
                                            viewModel = viewModel,
                                            onNavigateBack = { showNotificationCenter = false },
                                            onOpenHabits = {
                                                showNotificationCenter = false
                                                currentTab = NavTab.HABITS
                                            },
                                            onOpenWater = {
                                                showNotificationCenter = false
                                                showWaterTracker = true
                                            },
                                            onOpenNightReview = {
                                                showNotificationCenter = false
                                                showNightReviewScreen = true
                                            },
                                            onOpenFocus = {
                                                showNotificationCenter = false
                                                currentTab = NavTab.FOCUS
                                            },
                                            onOpenSettings = {
                                                showNotificationCenter = false
                                                showSettings = true
                                            },
                                            onOpenAlarms = {
                                                showNotificationCenter = false
                                                showAlarms = true
                                            },
                                            onOpenLeaderboard = {
                                                showNotificationCenter = false
                                                currentTab = NavTab.LEADERBOARD
                                            },
                                            onOpenContentHub = {
                                                showNotificationCenter = false
                                                showContentHub = true
                                            }
                                        )
                                    }
                                    is AppDestination.ContentHub -> {
                                        com.example.ui.screens.content.ContentDiscoverScreen(
                                            viewModel = viewModel,
                                            onNavigateBack = { showContentHub = false },
                                            onOpenHabits = {
                                                showContentHub = false
                                                currentTab = NavTab.HABITS
                                            },
                                            onOpenWater = {
                                                showContentHub = false
                                                showWaterTracker = true
                                            },
                                            onOpenFocus = {
                                                showContentHub = false
                                                currentTab = NavTab.FOCUS
                                            },
                                            onOpenJournal = {
                                                showContentHub = false
                                                currentTab = NavTab.JOURNAL
                                            },
                                            onOpenAlarms = {
                                                showContentHub = false
                                                showAlarms = true
                                            },
                                            isReducedMotion = isReducedMotion
                                        )
                                    }
                                    is AppDestination.Leaderboard -> {
                                        LeaderboardScreen(
                                            viewModel = viewModel,
                                            onBack = { showLeaderboardScreen = false },
                                            onOpenPublicProfile = { uname -> viewingPublicUsername = uname }
                                        )
                                    }
                                    is AppDestination.Alarms -> {
                                        AlarmManagementScreen(
                                            viewModel = viewModel,
                                            onNavigateBack = { showAlarms = false }
                                        )
                                    }
                                    is AppDestination.MonthlyTracker -> {
                                        MonthlyTrackerScreen(
                                            viewModel = viewModel,
                                            onBack = { showMonthlyTracker = false }
                                        )
                                    }
                                    is AppDestination.RemindersSettings -> {
                                        RemindersSettingsScreen(
                                            viewModel = viewModel,
                                            onNavigateBack = { showRemindersSettings = false },
                                            onOpenNightReview = {
                                                showNightReviewScreen = true
                                            },
                                            onOpenAlarms = {
                                                showAlarms = true
                                            }
                                        )
                                    }
                                    is AppDestination.NightReview -> {
                                        NightReviewScreen(
                                            viewModel = viewModel,
                                            onNavigateBack = { showNightReviewScreen = false }
                                        )
                                    }
                                    is AppDestination.Tab -> {
                                        when (destination.tab) {
                                            NavTab.HOME -> {
                                                HomeScreen(
                                                    viewModel = viewModel,
                                                    onNavigateTab = { tab -> currentTab = tab },
                                                    onOpenAddHabit = {
                                                        habitToEdit = null
                                                        showAddEditHabitDialog = true
                                                    },
                                                    onOpenMonthlyTracker = { showMonthlyTracker = true },
                                                    onEditHabit = { habit ->
                                                        habitToEdit = habit
                                                        showAddEditHabitDialog = true
                                                    },
                                                    onOpenWaterTracker = { showWaterTracker = true },
                                                    onOpenReminders = { showRemindersSettings = true },
                                                    onOpenNightReview = { showNightReviewScreen = true },
                                                    onOpenNotifications = { showNotificationCenter = true },
                                                    onOpenAlarms = { showAlarms = true },
                                                    onOpenDiscover = { showContentHub = true }
                                                )
                                            }

                                            NavTab.HABITS -> {
                                                HabitsScreen(
                                                    viewModel = viewModel,
                                                    onOpenAddHabit = {
                                                        habitToEdit = null
                                                        showAddEditHabitDialog = true
                                                    },
                                                    onOpenMonthlyTracker = { showMonthlyTracker = true },
                                                    onEditHabit = { habit ->
                                                        habitToEdit = habit
                                                        showAddEditHabitDialog = true
                                                    },
                                                    onBack = { currentTab = NavTab.HOME }
                                                )
                                            }

                                            NavTab.FOCUS -> {
                                                FocusScreen(
                                                    viewModel = viewModel,
                                                    onBack = { currentTab = NavTab.HOME }
                                                )
                                            }

                                            NavTab.PROGRESS -> {
                                                ProgressScreen(
                                                    viewModel = viewModel,
                                                    onBack = { currentTab = NavTab.HOME },
                                                    onOpenLeaderboard = { currentTab = NavTab.LEADERBOARD }
                                                )
                                            }

                                            NavTab.LEADERBOARD -> {
                                                LeaderboardScreen(
                                                    viewModel = viewModel,
                                                    onBack = { currentTab = NavTab.HOME },
                                                    onOpenPublicProfile = { uname -> viewingPublicUsername = uname }
                                                )
                                            }

                                            NavTab.JOURNAL -> {
                                                com.example.ui.screens.journal.JournalScreen(
                                                    viewModel = viewModel,
                                                    onBack = { currentTab = NavTab.HOME }
                                                )
                                            }

                                            NavTab.PROFILE -> {
                                                ProfileScreen(
                                                    viewModel = viewModel,
                                                    onLogout = {
                                                        currentTab = NavTab.HOME
                                                    },
                                                    onBack = { currentTab = NavTab.HOME },
                                                    onOpenWaterTracker = { showWaterTracker = true },
                                                    onOpenReminders = { showRemindersSettings = true },
                                                    onOpenNightReview = { showNightReviewScreen = true },
                                                    onOpenDiscoverUsers = { showDiscoverUsers = true },
                                                    onOpenPublicProfile = { uname -> viewingPublicUsername = uname },
                                                    onOpenSettings = { showSettings = true },
                                                    onOpenAlarms = { showAlarms = true },
                                                    onOpenLeaderboard = { currentTab = NavTab.LEADERBOARD },
                                                    onOpenContentHub = { showContentHub = true }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Night Review Dialog overlay
                    if (showNightReviewDialog) {
                        val habits by viewModel.allHabits.collectAsState()
                        val todayCompletions by viewModel.todayCompletions.collectAsState()
                        val todayWater by viewModel.todayWaterRecord.collectAsState()
                        NightReviewDialog(
                            habits = habits,
                            todayCompletions = todayCompletions,
                            todayWater = todayWater,
                            onDismiss = { showNightReviewDialog = false },
                            onSaveReview = { entry ->
                                viewModel.saveNightReview(entry)
                            }
                        )
                    }

                    // Water Tracker Sheet overlay with smooth upward slide & spring
                    AnimatedVisibility(
                        visible = showWaterTracker,
                        enter = if (isReducedMotion) fadeIn(tween(150)) else slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(tween(200)),
                        exit = if (isReducedMotion) fadeOut(tween(150)) else slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeOut(tween(160))
                    ) {
                        WaterTrackerSheet(
                            viewModel = viewModel,
                            onDismiss = { showWaterTracker = false }
                        )
                    }

                    // Add/Edit Habit Dialog overlay
                    if (showAddEditHabitDialog) {
                        AddEditHabitDialog(
                            habitToEdit = habitToEdit,
                            onDismiss = { showAddEditHabitDialog = false },
                            onSave = { id, title, desc, cat, icon, col, rem, rep, gVal, gUnit, dur, sDate, eDate, diff, notes, morning, gType, paused ->
                                viewModel.saveHabit(
                                    id = id,
                                    title = title,
                                    description = desc,
                                    category = cat,
                                    iconName = icon,
                                    colorHex = col,
                                    reminderTime = rem,
                                    repeatDays = rep,
                                    goalValue = gVal,
                                    goalUnit = gUnit,
                                    durationMinutes = dur,
                                    startDate = sDate,
                                    endDate = eDate,
                                    difficulty = diff,
                                    notes = notes,
                                    isMorningRoutine = morning,
                                    goalType = gType,
                                    isPaused = paused
                                )
                            }
                        )
                    }

                    // Central + Quick Action Sheet overlay (Design 5)
                    if (showQuickActionSheet) {
                        QuickActionBottomSheet(
                            onDismiss = { showQuickActionSheet = false },
                            onNewHabit = {
                                habitToEdit = null
                                showAddEditHabitDialog = true
                            },
                            onStartFocus = {
                                currentTab = NavTab.FOCUS
                            },
                            onLogWater = {
                                showWaterTracker = true
                            },
                            onNewJournal = {
                                currentTab = NavTab.JOURNAL
                            },
                            onAlarms = {
                                showAlarms = true
                            },
                            onNightReview = {
                                showNightReviewScreen = true
                            },
                            onDiscover = {
                                showContentHub = true
                            }
                        )
                    }

                    // Achievement Celebration Dialog Overlay
                    newlyUnlockedAchievement?.let { ach ->
                        AchievementCelebrationDialog(
                            achievementTitle = ach.title,
                            achievementDescription = ach.description,
                            onDismiss = { viewModel.dismissUnlockedAchievementCelebration() },
                            isReducedMotion = isReducedMotion
                        )
                    }

                    // Global Liquid Glass Toast Floating Banner
                    GlassToastHost(
                        message = globalToastMessage,
                        isError = isGlobalToastError,
                        isReducedMotion = isReducedMotion,
                        onDismiss = { viewModel.dismissToast() }
                    )
                }
            }
        }
    }
}
}
}

/**
 * Modern iOS 27 Liquid Glass Quick Action Sheet triggered by the central + button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionBottomSheet(
    onDismiss: () -> Unit,
    onNewHabit: () -> Unit,
    onStartFocus: () -> Unit,
    onLogWater: () -> Unit,
    onNewJournal: () -> Unit,
    onAlarms: () -> Unit,
    onNightReview: () -> Unit = {},
    onDiscover: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xF20B1120),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color(0x40FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                fontSize = 20.sp
            )
            Text(
                text = "What would you like to forge right now?",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            QuickActionSheetRow(
                title = "New Habit",
                subtitle = "Schedule a daily routine or discipline goal",
                icon = Icons.Filled.Add,
                iconColor = NeonGreen,
                iconBg = Color(0x3322C55E),
                onClick = { onDismiss(); onNewHabit() }
            )

            QuickActionSheetRow(
                title = "Deep Focus Session",
                subtitle = "Start a 25/50m locked-in countdown",
                icon = Icons.Filled.Timer,
                iconColor = NeonPurple,
                iconBg = Color(0x33A855F7),
                onClick = { onDismiss(); onStartFocus() }
            )

            QuickActionSheetRow(
                title = "Log Water Intake",
                subtitle = "Track hydration and daily target",
                icon = Icons.Filled.WaterDrop,
                iconColor = NeonCyan,
                iconBg = Color(0x3306B6D4),
                onClick = { onDismiss(); onLogWater() }
            )

            QuickActionSheetRow(
                title = "New Journal Reflection",
                subtitle = "Capture thoughts, trades, or study notes",
                icon = Icons.Filled.MenuBook,
                iconColor = Color(0xFF60A5FA),
                iconBg = Color(0x333B82F6),
                onClick = { onDismiss(); onNewJournal() }
            )

            QuickActionSheetRow(
                title = "Night Review",
                subtitle = "Daily rating, win analysis, & priority planning",
                icon = Icons.Filled.NightsStay,
                iconColor = Color(0xFFC084FC),
                iconBg = Color(0x338B5CF6),
                onClick = { onDismiss(); onNightReview() }
            )

            QuickActionSheetRow(
                title = "Wake-Up Challenge & Alarms",
                subtitle = "Native alarm with photo / motion verification",
                icon = Icons.Filled.Alarm,
                iconColor = StreakAmber,
                iconBg = Color(0x33F59E0B),
                onClick = { onDismiss(); onAlarms() }
            )

            QuickActionSheetRow(
                title = "DAYFORGE Discover",
                subtitle = "Explore fitness, health & discipline feed",
                icon = Icons.Filled.AutoAwesome,
                iconColor = Color(0xFF00F0FF),
                iconBg = Color(0x3300F0FF),
                onClick = { onDismiss(); onDiscover() }
            )
        }
    }
}

@Composable
private fun QuickActionSheetRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color(0x8C141C2E)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.5.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


@Composable
fun LiquidGlassLoadingScreen(
    state: StartupUiState,
    onRetry: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onGoToSignIn: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "startup_anim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "startup_rotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "startup_pulse"
    )

    LiquidGlassBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .scale(if (state is StartupUiState.Loading) pulseScale else 1f),
                    contentAlignment = Alignment.Center
                ) {
                    DayForgeMountainLogo(
                        modifier = Modifier.size(76.dp),
                        tint = Color.White,
                        glowColor = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "DAYFORGE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Discipline Builds Freedom",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                when (state) {
                    is StartupUiState.Loading -> {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            modifier = Modifier
                                .size(32.dp)
                                .rotate(rotation)
                                .testTag("startup_spinner"),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Synchronizing warrior profile...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    is StartupUiState.Error -> {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0x661E293B),
                            borderColor = Color(0x40EF4444)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Unable to connect.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF87171),
                                    textAlign = TextAlign.Center
                                )
                                if (!state.details.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = state.details,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("startup_retry_button")
                        ) {
                            Text("Retry", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onContinueAsGuest,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("startup_guest_button")
                        ) {
                            Text("Continue as Guest", fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        TextButton(
                            onClick = onGoToSignIn,
                            modifier = Modifier.testTag("startup_login_button")
                        ) {
                            Text(
                                "Go to Sign In",
                                color = NeonCyan,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    StartupUiState.Ready -> {}
                }
            }
        }
    }
}
