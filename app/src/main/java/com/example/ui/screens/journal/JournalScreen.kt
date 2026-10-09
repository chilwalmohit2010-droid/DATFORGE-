package com.example.ui.screens.journal

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.JournalEntry
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.WatermarkFooter
import com.example.ui.components.tactilePress
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: DisciplineViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val journalEntries by viewModel.journalEntries.collectAsState()
    val isReducedMotion by viewModel.isReducedMotionEnabled.collectAsState()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showAddEntrySheet by remember { mutableStateOf(false) }

    val categories = listOf("All", "Trade", "Study", "Mindset", "Workout", "Health", "Reflection")

    // Filter entries based on search and category
    val filteredEntries = remember(journalEntries, selectedCategory, searchQuery) {
        journalEntries.filter { entry ->
            val matchesCategory = selectedCategory == "All" ||
                    entry.category.equals(selectedCategory, ignoreCase = true) ||
                    entry.tags.any { it.equals(selectedCategory, ignoreCase = true) }
            val matchesSearch = searchQuery.isBlank() ||
                    entry.title.contains(searchQuery, ignoreCase = true) ||
                    entry.content.contains(searchQuery, ignoreCase = true) ||
                    entry.tags.any { it.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesSearch
        }
    }

    val currentMonthName = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    }

    LiquidGlassBackground {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("journal_screen_column"),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar matching reference image (back/menu, title "Journal", search, +)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (onBack != null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x331E293B))
                                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                    .tactilePress { onBack() }
                                    .testTag("journal_back_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Text(
                            text = "Journal",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            fontSize = 26.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x331E293B))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                .tactilePress { isSearchExpanded = !isSearchExpanded }
                                .testTag("journal_search_toggle_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Filled.Close else Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Add Entry + Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(NeonCyan, Color(0xFF2563EB)))
                                )
                                .border(1.dp, Color(0x66FFFFFF), CircleShape)
                                .tactilePress { showAddEntrySheet = true }
                                .testTag("journal_add_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add Journal Entry",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Search Bar (Animated Expand)
            item {
                AnimatedVisibility(
                    visible = isSearchExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        GlassTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Search notes, tags, trades...",
                            leadingIcon = Icons.Outlined.Search,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = TextSecondary)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Horizontal Date Strip (Reference Image Screen 9)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    // Month navigation row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentMonthName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 17.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { selectedDate = selectedDate.minusMonths(1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Month", tint = TextSecondary)
                            }
                            IconButton(
                                onClick = { selectedDate = selectedDate.plusMonths(1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Next Month", tint = TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal date row showing surrounding days
                    val today = remember { LocalDate.now() }
                    val startOfWeek = selectedDate.minusDays(3)
                    val daysList = remember(selectedDate) {
                        (0..6).map { startOfWeek.plusDays(it.toLong()) }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysList.forEach { date ->
                            val isSelected = date == selectedDate
                            val isCurrentDay = date == today
                            val dayOfWeekName = date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())).take(3)
                            val dayNumber = date.dayOfMonth.toString()

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .tactilePress { selectedDate = date }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Text(
                                    text = dayOfWeekName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) NeonCyan else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (isSelected) {
                                                Modifier.background(Brush.linearGradient(listOf(NeonCyan, Color(0xFF2563EB))))
                                            } else if (isCurrentDay) {
                                                Modifier.background(Color(0x3338BDF8))
                                            } else {
                                                Modifier.background(Color(0x1F1E293B))
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.White else if (isCurrentDay) NeonCyan else Color(0x26FFFFFF),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNumber,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Categories Filter Strip
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0x3338BDF8) else Color(0x660B1220),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NeonCyan else Color(0x26FFFFFF)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .tactilePress { selectedCategory = category }
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) NeonCyan else TextSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Journal Entries List
            if (filteredEntries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EditNote,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No Journal Entries Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tap + to capture your reflections, trades, and study logs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredEntries, key = { it.id }) { entry ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        JournalCardItem(
                            entry = entry,
                            onDelete = { viewModel.deleteJournalEntry(entry.id) },
                            isReducedMotion = isReducedMotion
                        )
                    }
                }
            }

            item {
                WatermarkFooter()
            }
        }
    }

    // Modern iOS-style Bottom Sheet for Adding a Journal Entry
    if (showAddEntrySheet) {
        AddJournalEntrySheet(
            onDismiss = { showAddEntrySheet = false },
            onSave = { title, content, category, tags ->
                viewModel.addJournalEntry(title, content, category, tags)
                showAddEntrySheet = false
            }
        )
    }
}

@Composable
fun JournalCardItem(
    entry: JournalEntry,
    onDelete: () -> Unit,
    isReducedMotion: Boolean = false
) {
    val categoryDetails = remember(entry.category) {
        when (entry.category.lowercase()) {
            "trade" -> CategoryConfig(
                icon = Icons.Filled.TrendingUp,
                color = NeonGreen,
                bg = Color(0x3322C55E)
            )
            "study" -> CategoryConfig(
                icon = Icons.Filled.MenuBook,
                color = Color(0xFF60A5FA),
                bg = Color(0x333B82F6)
            )
            "mindset", "reflection" -> CategoryConfig(
                icon = Icons.Filled.Psychology,
                color = NeonPurple,
                bg = Color(0x33A855F7)
            )
            "workout" -> CategoryConfig(
                icon = Icons.Filled.FitnessCenter,
                color = Color(0xFFF97316),
                bg = Color(0x33EA580C)
            )
            "health" -> CategoryConfig(
                icon = Icons.Filled.Favorite,
                color = Color(0xFFEC4899),
                bg = Color(0x33DB2777)
            )
            else -> CategoryConfig(
                icon = Icons.Filled.Article,
                color = NeonCyan,
                bg = Color(0x3306B6D4)
            )
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        isReducedMotion = isReducedMotion,
        backgroundColor = Color(0x8C141C2E)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Icon Badge, Title, Time, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(categoryDetails.bg)
                            .border(1.dp, categoryDetails.color.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryDetails.icon,
                            contentDescription = null,
                            tint = categoryDetails.color,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                        if (entry.time.isNotBlank()) {
                            Text(
                                text = entry.time,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete entry",
                        tint = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Entry Content Snippet
            if (entry.content.isNotBlank()) {
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )
            }

            // Tag Pills Row (Reference image style: [Trade] [Discipline])
            if (entry.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    entry.tags.forEachIndexed { index, tag ->
                        val tagColor = when (index % 3) {
                            0 -> NeonCyan
                            1 -> NeonPurple
                            else -> NeonGreen
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = tagColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, tagColor.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = tagColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class CategoryConfig(
    val icon: ImageVector,
    val color: Color,
    val bg: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddJournalEntrySheet(
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String, tags: List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Reflection") }
    var tagsInput by remember { mutableStateOf("") }

    val categories = listOf("Trade", "Study", "Mindset", "Workout", "Health", "Reflection")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xF20F172A),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0x40FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "New Journal Entry",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Capture reflections, trades, or study notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            // Category selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = category == cat
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0x3338BDF8) else Color(0x331E293B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) NeonCyan else Color(0x26FFFFFF)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .tactilePress { category = cat }
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) NeonCyan else TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            GlassTextField(
                value = title,
                onValueChange = { title = it },
                label = "Title",
                placeholder = "e.g., Trade Journal, Daily Reflection"
            )

            GlassTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                label = "Tags (comma separated)",
                placeholder = "e.g., Trade, Discipline, Mindset"
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Reflection / Notes", color = TextSecondary) },
                placeholder = { Text("Write your thoughts...", color = TextSecondary.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedContainerColor = Color(0x4D141C2E),
                    unfocusedContainerColor = Color(0x33141C2E)
                )
            )

            GlassPrimaryButton(
                text = "Save Entry",
                onClick = {
                    if (title.isNotBlank()) {
                        val tags = tagsInput.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                        onSave(title, content, category, tags)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
