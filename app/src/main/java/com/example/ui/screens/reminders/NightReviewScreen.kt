package com.example.ui.screens.reminders

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CompletionStatus
import com.example.data.models.NightReviewEntry
import com.example.ui.components.WatermarkFooter
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NightReviewScreen(
    viewModel: DisciplineViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val todayDateIso = remember { today.format(DateTimeFormatter.ISO_LOCAL_DATE) }
    val formattedDisplayDate = remember {
        today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
    }
    val todayDayOfWeek = remember { today.dayOfWeek.value }

    // Observe data from ViewModel
    val habits by viewModel.allHabits.collectAsState()
    val todayCompletions by viewModel.todayCompletions.collectAsState()
    val todayWater by viewModel.todayWaterRecord.collectAsState()
    val userWaterGoal by viewModel.userWaterGoal.collectAsState()
    val todayFocusMinutes by viewModel.todayFocusMinutes.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val savedTodayReview by viewModel.todayNightReview.collectAsState()
    val allReviews by viewModel.allNightReviews.collectAsState()

    // Automatic calculation of statistics from actual user data
    val scheduledHabits = remember(habits, todayDayOfWeek) {
        val filtered = habits.filter { !it.isPaused && it.isScheduledForDayOfWeek(todayDayOfWeek) }
        if (filtered.isNotEmpty()) filtered else habits.filter { !it.isPaused }
    }
    val totalHabitsCount = scheduledHabits.size
    val completedHabitsCount = remember(todayCompletions, scheduledHabits) {
        todayCompletions.count { comp ->
            comp.status == CompletionStatus.COMPLETED && scheduledHabits.any { it.id == comp.habitId }
        }
    }
    val completionPercentage = remember(completedHabitsCount, totalHabitsCount) {
        if (totalHabitsCount > 0) ((completedHabitsCount * 100) / totalHabitsCount).coerceIn(0, 100) else 0
    }
    val currentStreak = currentUser?.currentStreak ?: 0
    val waterIntake = todayWater?.currentMl ?: 0
    val waterGoal = todayWater?.targetMl?.takeIf { it > 0 } ?: (userWaterGoal ?: 2500)

    // Form inputs state
    var isEditing by remember { mutableStateOf(false) }
    var inputRating by remember { mutableStateOf(5) }
    var inputWentWell by remember { mutableStateOf("") }
    var inputImprovement by remember { mutableStateOf("") }
    var inputReflection by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Populate inputs when saved review exists or changes
    LaunchedEffect(savedTodayReview) {
        val review = savedTodayReview
        if (review != null) {
            inputRating = review.rating
            inputWentWell = review.wentWell.ifBlank { review.wins }
            inputImprovement = review.improvement.ifBlank { review.struggles }
            inputReflection = review.reflection.ifBlank { review.tomorrowPriority }
            isEditing = false
        } else {
            // New review form
            inputRating = 5
            inputWentWell = ""
            inputImprovement = ""
            inputReflection = ""
            isEditing = true
        }
    }

    // Determine past reviews list (exclude today)
    val pastReviews = remember(allReviews, todayDateIso) {
        allReviews.filter { it.date != todayDateIso }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Night Review",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Evening closure & mindful reflection",
                            fontSize = 12.sp,
                            color = GlassTextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .testTag("night_review_back_button")
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33A855F7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = "Evening",
                            tint = NeonPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xF20B0F17)
                )
            )
        },
        containerColor = Color(0xFF070A0F)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
        ) {
            // 1. Evening Calm Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(Color(0x59A855F7), Color(0x3338BDF8), Color(0x10FFFFFF))
                            ),
                            RoundedCornerShape(20.dp)
                        ),
                    color = Color(0xCC0F172A),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🌙",
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = formattedDisplayDate,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x33A855F7),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "Daily Closure",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFD8B4FE),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Reflect honestly on today's discipline, celebrate every single win, and enter tomorrow with clear purpose.",
                            fontSize = 13.sp,
                            color = GlassTextMuted,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 2. Performance Snapshot (Live Auto-Calculated Statistics)
            item {
                Text(
                    text = "TODAY'S PERFORMANCE SNAPSHOT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = GlassTextMuted,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Habit Completion Tile
                        StatGlassCard(
                            modifier = Modifier.weight(1f),
                            title = "Habits",
                            value = "$completionPercentage%",
                            subtitle = "$completedHabitsCount of $totalHabitsCount completed",
                            icon = Icons.Default.CheckCircle,
                            iconTint = NeonEmerald,
                            progress = if (totalHabitsCount > 0) completedHabitsCount.toFloat() / totalHabitsCount else 0f
                        )

                        // Streak Tile
                        StatGlassCard(
                            modifier = Modifier.weight(1f),
                            title = "Streak",
                            value = "$currentStreak Days",
                            subtitle = if (currentStreak > 0) "Consistency blazing" else "Build momentum",
                            icon = Icons.Default.LocalFireDepartment,
                            iconTint = AmberRating,
                            progress = null
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Hydration Tile
                        val waterRatio = if (waterGoal > 0) (waterIntake.toFloat() / waterGoal).coerceIn(0f, 1f) else 0f
                        StatGlassCard(
                            modifier = Modifier.weight(1f),
                            title = "Hydration",
                            value = "${waterIntake} ml",
                            subtitle = "Goal: ${waterGoal} ml (${(waterRatio * 100).toInt()}%)",
                            icon = Icons.Default.WaterDrop,
                            iconTint = ElectricBlue,
                            progress = waterRatio
                        )

                        // Focus Time Tile
                        StatGlassCard(
                            modifier = Modifier.weight(1f),
                            title = "Deep Focus",
                            value = "${todayFocusMinutes}m",
                            subtitle = "Mindful focus time",
                            icon = Icons.Default.Timer,
                            iconTint = NeonPurple,
                            progress = null
                        )
                    }
                }
            }

            // 3. Today's Review Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY'S REVIEW",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = GlassTextMuted,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    if (savedTodayReview != null && !isEditing) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x2210B981)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Completed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NeonEmerald
                                )
                            }
                        }
                    }
                }
            }

            // Display Mode or Edit Mode
            if (savedTodayReview != null && !isEditing) {
                // SAVED REVIEW VIEW MODE
                item {
                    val review = savedTodayReview!!
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color(0x4010B981), Color(0x1A38BDF8), Color(0x10FFFFFF))
                                ),
                                RoundedCornerShape(20.dp)
                            ),
                        color = Color(0xCC0F172A),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Rating Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Day Rating",
                                        fontSize = 13.sp,
                                        color = GlassTextMuted
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        repeat(5) { index ->
                                            Icon(
                                                imageVector = if (index < review.rating) Icons.Filled.Star else Icons.Outlined.Star,
                                                contentDescription = null,
                                                tint = if (index < review.rating) AmberRating else Color(0x44FFFFFF),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = getRatingDescriptor(review.rating),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AmberRating
                                        )
                                    }
                                }
                            }

                            Divider(color = Color(0x1AFFFFFF))

                            // Field 1: What went well
                            ReviewResponseField(
                                title = "What went well today?",
                                text = review.wentWell.ifBlank { review.wins }.ifBlank { "No wins recorded" },
                                accentColor = NeonEmerald,
                                icon = Icons.Default.ThumbUp
                            )

                            // Field 2: What could be improved
                            ReviewResponseField(
                                title = "What could I improve tomorrow?",
                                text = review.improvement.ifBlank { review.struggles }.ifBlank { "No improvements recorded" },
                                accentColor = Color(0xFFF59E0B),
                                icon = Icons.Default.TrendingUp
                            )

                            // Field 3: Today's reflection
                            val reflectionText = review.reflection.ifBlank { review.tomorrowPriority }
                            if (reflectionText.isNotBlank()) {
                                ReviewResponseField(
                                    title = "Today's reflection",
                                    text = reflectionText,
                                    accentColor = NeonPurple,
                                    icon = Icons.Default.Lightbulb
                                )
                            }

                            // Snapshot Note
                            Text(
                                text = "🔒 Saved snapshot of day's stats: ${review.completedHabits}/${review.totalHabits} habits (${review.completionPercentage}%), ${review.waterIntake}ml water, ${review.focusMinutes}m focus, ${review.streak}d streak.",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 16.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )

                            // Action buttons: Edit & Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { isEditing = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("edit_night_review_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33A855F7))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFC084FC)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Edit Review",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showDeleteConfirmDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("delete_night_review_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFFF87171)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33EF4444))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFF87171)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Delete Review",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // INPUT / EDIT FORM MODE
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color(0x59A855F7), Color(0x3338BDF8), Color(0x10FFFFFF))
                                ),
                                RoundedCornerShape(20.dp)
                            ),
                        color = Color(0xCC0F172A),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Rating Input (1 to 5 stars)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "How would you rate today?",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    (1..5).forEach { star ->
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (inputRating >= star) Color(0x26F59E0B) else Color(0x0DFFFFFF)
                                                )
                                                .clickable { inputRating = star }
                                                .testTag("star_rating_$star"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (inputRating >= star) Icons.Filled.Star else Icons.Outlined.Star,
                                                contentDescription = "$star Stars",
                                                tint = if (inputRating >= star) AmberRating else Color(0x4DFFFFFF),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = getRatingDescriptor(inputRating),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AmberRating,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }

                            Divider(color = Color(0x1AFFFFFF))

                            // Field 1: What went well today?
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "What went well today?",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = " *",
                                        color = NeonEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = inputWentWell,
                                    onValueChange = { inputWentWell = it },
                                    placeholder = {
                                        Text(
                                            "e.g., Completed morning run, finished deep work session, stayed hydrated...",
                                            fontSize = 13.sp,
                                            color = Color(0x66FFFFFF)
                                        )
                                    },
                                    minLines = 3,
                                    maxLines = 5,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_what_went_well"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = NeonEmerald,
                                        unfocusedBorderColor = Color(0x22FFFFFF),
                                        focusedContainerColor = Color(0x33000000),
                                        unfocusedContainerColor = Color(0x22000000)
                                    )
                                )
                            }

                            // Field 2: What could I improve tomorrow?
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "What could I improve tomorrow?",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = " *",
                                        color = Color(0xFFF59E0B),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = inputImprovement,
                                    onValueChange = { inputImprovement = it },
                                    placeholder = {
                                        Text(
                                            "e.g., Start work 30 mins earlier, avoid late phone screen time, reduce snacks...",
                                            fontSize = 13.sp,
                                            color = Color(0x66FFFFFF)
                                        )
                                    },
                                    minLines = 3,
                                    maxLines = 5,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_what_to_improve"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFF59E0B),
                                        unfocusedBorderColor = Color(0x22FFFFFF),
                                        focusedContainerColor = Color(0x33000000),
                                        unfocusedContainerColor = Color(0x22000000)
                                    )
                                )
                            }

                            // Field 3: Today's reflection (optional)
                            Column {
                                Text(
                                    text = "Today's reflection (optional)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = inputReflection,
                                    onValueChange = { inputReflection = it },
                                    placeholder = {
                                        Text(
                                            "Any gratitude, lessons, or thoughts to clear your mind...",
                                            fontSize = 13.sp,
                                            color = Color(0x66FFFFFF)
                                        )
                                    },
                                    minLines = 2,
                                    maxLines = 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_reflection"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = NeonPurple,
                                        unfocusedBorderColor = Color(0x22FFFFFF),
                                        focusedContainerColor = Color(0x33000000),
                                        unfocusedContainerColor = Color(0x22000000)
                                    )
                                )
                            }

                            // Action Buttons: Save Review & (if editing) Cancel
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (savedTodayReview != null) {
                                    OutlinedButton(
                                        onClick = { isEditing = false },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .testTag("cancel_edit_night_review_button"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                                    ) {
                                        Text(
                                            text = "Cancel",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (inputWentWell.isBlank() && inputImprovement.isBlank()) {
                                            Toast.makeText(
                                                context,
                                                "Please write at least what went well or what to improve.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@Button
                                        }

                                        isSaving = true
                                        val nowMs = System.currentTimeMillis()
                                        val entryToSave = NightReviewEntry(
                                            date = todayDateIso,
                                            userId = currentUser?.uid ?: "",
                                            completedHabits = completedHabitsCount,
                                            totalHabits = totalHabitsCount,
                                            completionPercentage = completionPercentage,
                                            streak = currentStreak,
                                            waterIntake = waterIntake,
                                            waterGoal = waterGoal,
                                            focusMinutes = todayFocusMinutes,
                                            wentWell = inputWentWell.trim(),
                                            improvement = inputImprovement.trim(),
                                            reflection = inputReflection.trim(),
                                            rating = inputRating,
                                            createdAt = savedTodayReview?.createdAt ?: nowMs,
                                            updatedAt = nowMs
                                        )

                                        viewModel.saveNightReview(entryToSave) {
                                            isSaving = false
                                            isEditing = false
                                            Toast.makeText(
                                                context,
                                                "Night review saved successfully! 🌙",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(if (savedTodayReview != null) 1.5f else 1f)
                                        .height(50.dp)
                                        .testTag("save_night_review_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    enabled = !isSaving
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF10B981), Color(0xFF059669))
                                                ),
                                                RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSaving) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (savedTodayReview != null) "Save Changes" else "Save Review",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Previous Reviews Section
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PREVIOUS REVIEWS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = GlassTextMuted,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    if (pastReviews.isNotEmpty()) {
                        Text(
                            text = "${pastReviews.size} logged",
                            fontSize = 12.sp,
                            color = GlassTextMuted
                        )
                    }
                }
            }

            if (pastReviews.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(18.dp)),
                        color = Color(0x990F172A),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = Color(0x44FFFFFF),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Previous Reviews Yet",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Daily evening reflections build your personal discipline archive and mindful clarity.",
                                fontSize = 12.sp,
                                color = GlassTextMuted,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                items(pastReviews, key = { it.date }) { pastReview ->
                    PastReviewGlassCard(review = pastReview)
                }
            }

            // Watermark footer
            item {
                Spacer(modifier = Modifier.height(8.dp))
                WatermarkFooter()
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete Today's Review?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete your review for today ($formattedDisplayDate)? You can write a new one anytime.",
                    fontSize = 14.sp,
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteNightReview(todayDateIso) {
                            Toast.makeText(context, "Review deleted.", Toast.LENGTH_SHORT).show()
                            isEditing = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("confirm_delete_night_review")
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun StatGlassCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    progress: Float? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp)),
        color = Color(0xB30F172A),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlassTextMuted
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = GlassTextMuted,
                maxLines = 1
            )

            if (progress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = iconTint,
                    trackColor = Color(0x22FFFFFF)
                )
            }
        }
    }
}

@Composable
private fun ReviewResponseField(
    title: String,
    text: String,
    accentColor: Color,
    icon: ImageVector
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0x33000000),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                color = Color(0xFFE2E8F0),
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun PastReviewGlassCard(review: NightReviewEntry) {
    var expanded by remember { mutableStateOf(false) }
    val formattedDate = remember(review.date) {
        try {
            val parsed = LocalDate.parse(review.date, DateTimeFormatter.ISO_LOCAL_DATE)
            parsed.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
        } catch (e: Exception) {
            review.date
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded }
            .animateContentSize(),
        color = Color(0xB30F172A),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formattedDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < review.rating) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                tint = if (index < review.rating) AmberRating else Color(0x33FFFFFF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = getRatingDescriptor(review.rating),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = AmberRating
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = GlassTextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Stat pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MiniStatPill(
                    text = "${review.completionPercentage}% habits",
                    icon = Icons.Default.Check,
                    color = NeonEmerald
                )
                if (review.waterIntake > 0) {
                    MiniStatPill(
                        text = "${review.waterIntake}ml",
                        icon = Icons.Default.WaterDrop,
                        color = ElectricBlue
                    )
                }
                if (review.focusMinutes > 0) {
                    MiniStatPill(
                        text = "${review.focusMinutes}m focus",
                        icon = Icons.Default.Timer,
                        color = NeonPurple
                    )
                }
                if (review.streak > 0) {
                    MiniStatPill(
                        text = "${review.streak}d streak",
                        icon = Icons.Default.LocalFireDepartment,
                        color = AmberRating
                    )
                }
            }

            // Expanded details
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val winsText = review.wentWell.ifBlank { review.wins }
                    if (winsText.isNotBlank()) {
                        Text(
                            text = "Went well: $winsText",
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 16.sp
                        )
                    }
                    val improveText = review.improvement.ifBlank { review.struggles }
                    if (improveText.isNotBlank()) {
                        Text(
                            text = "To improve: $improveText",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        )
                    }
                    val reflectionText = review.reflection.ifBlank { review.tomorrowPriority }
                    if (reflectionText.isNotBlank()) {
                        Text(
                            text = "Reflection: $reflectionText",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStatPill(
    text: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

private fun getRatingDescriptor(rating: Int): String {
    return when (rating) {
        1 -> "Tough Day"
        2 -> "Room to Grow"
        3 -> "Solid Effort"
        4 -> "Great Focus"
        5 -> "Outstanding Day!"
        else -> "Solid Day"
    }
}
