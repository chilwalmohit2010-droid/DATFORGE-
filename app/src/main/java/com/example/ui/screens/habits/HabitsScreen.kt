package com.example.ui.screens.habits

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CompletionStatus
import com.example.data.models.Habit
import com.example.data.models.HabitCategory
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel

@Composable
fun HabitsScreen(
    viewModel: DisciplineViewModel,
    onOpenAddHabit: () -> Unit,
    onOpenMonthlyTracker: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    val todayCompletions by viewModel.todayCompletions.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf<HabitCategory?>(null) }
    var showOnlyActive by remember { mutableStateOf(false) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val completedHabitIds = remember(todayCompletions) {
        todayCompletions.filter { it.status == CompletionStatus.COMPLETED }.map { it.habitId }.toSet()
    }

    val filteredHabits = remember(allHabits, selectedCategoryFilter, showOnlyActive, searchQuery) {
        allHabits.filter { habit ->
            val matchesCategory = selectedCategoryFilter == null || habit.category == selectedCategoryFilter
            val matchesActive = !showOnlyActive || !habit.isPaused
            val matchesSearch = searchQuery.isBlank() || habit.title.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesActive && matchesSearch
        }
    }

    LiquidGlassBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("habits_screen_column"),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
            // Top Header: Back Arrow, "Habits" centered, circular '+' button (Screen 5)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x261E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onBack?.invoke() }
                            .testTag("habits_back_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Habits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x261E293B))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                .clickable { isSearchActive = !isSearchActive }
                                .testTag("habits_search_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Filled.Close else Icons.Outlined.Search,
                                contentDescription = "Search Habits",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x261E293B))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                .clickable { onOpenAddHabit() }
                                .testTag("habits_add_header_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add Habit",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

                // Search Bar input if active
                if (isSearchActive) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search habits...", color = TextMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedContainerColor = Color(0x80131C2E),
                                unfocusedContainerColor = Color(0x80131C2E),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, null, tint = TextSecondary)
                            }
                        )
                    }
                }

                // Category Filter Chips Row (All, Health, Focus, Learning, Other...)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                val isSelected = selectedCategoryFilter == null
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { selectedCategoryFilter = null },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) NeonGreen else Color(0x4D141C2E),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) NeonGreen else Color(0x26FFFFFF)
                                    )
                                ) {
                                    Text(
                                        text = "All",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            items(HabitCategory.values()) { category ->
                                val isSelected = selectedCategoryFilter == category
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable {
                                            selectedCategoryFilter = if (isSelected) null else category
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) NeonGreen else Color(0x4D141C2E),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) NeonGreen else Color(0x26FFFFFF)
                                    )
                                ) {
                                    Text(
                                        text = category.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Monthly Tracker banner shortcut
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color(0x59FFFFFF), Color(0x1AFFFFFF))
                                ),
                                RoundedCornerShape(20.dp)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onOpenMonthlyTracker() },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x8C141C2E)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0x14FFFFFF), Color.Transparent)
                                    )
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x3322C55E))
                                        .border(1.dp, Color(0x4D22C55E), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.GridView, null, tint = NeonGreenBright, modifier = Modifier.size(22.dp))
                                }
                                Column {
                                    Text(
                                        text = "Monthly Completion Matrix",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Inspect month-by-month consistency records",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary)
                        }
                    }
                }

                // Habits List
                if (filteredHabits.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0x80131C2E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x3322C55E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.AddCircleOutline, null, tint = NeonGreenBright, modifier = Modifier.size(28.dp))
                                }
                                Text("No habits found in this category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Tap the '+' button below to add your next discipline standard.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    items(filteredHabits, key = { it.id }) { habit ->
                        val isCompleted = completedHabitIds.contains(habit.id)
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            HabitItemCard(
                                habit = habit,
                                isCompletedToday = isCompleted,
                                onToggleComplete = { viewModel.toggleHabitCompletion(habit.id) },
                                onEdit = { onEditHabit(habit) },
                                onPauseToggle = { viewModel.toggleHabitPause(habit) },
                                onDelete = { habitToDelete = habit },
                                showStreakOnRight = true
                            )
                        }
                    }
                }

                // Watermark Footer
                item {
                    WatermarkFooter()
                }
            }

            // Floating Action Button (+ Add Habit)
            FloatingActionButton(
                onClick = onOpenAddHabit,
                containerColor = NeonGreen,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 90.dp)
                    .testTag("fab_add_habit")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Habit", modifier = Modifier.size(28.dp))
            }
        }
    }

    // Delete Confirmation Dialog
    if (habitToDelete != null) {
        val target = habitToDelete!!
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("Delete Habit?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${target.title}'? Its completion history will be removed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHabit(target)
                        habitToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

