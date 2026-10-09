package com.example.ui.screens.content

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.models.ContentCategory
import com.example.data.models.DisciplineContent
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentDiscoverScreen(
    viewModel: DisciplineViewModel,
    onNavigateBack: () -> Unit,
    onOpenHabits: () -> Unit = {},
    onOpenWater: () -> Unit = {},
    onOpenFocus: () -> Unit = {},
    onOpenJournal: () -> Unit = {},
    onOpenAlarms: () -> Unit = {},
    isReducedMotion: Boolean = false
) {
    val context = LocalContext.current
    val contentList by viewModel.contentItems.collectAsState()
    val forYouList by viewModel.forYouRecommendations.collectAsState()
    val trendingList by viewModel.trendingContent.collectAsState()
    val savedList by viewModel.savedContentList.collectAsState()
    val savedIds by viewModel.savedContentIds.collectAsState()
    val selectedCategory by viewModel.selectedContentCategory.collectAsState()
    val isLoading by viewModel.isContentLoading.collectAsState()
    val errorMsg by viewModel.contentLoadError.collectAsState()
    val saveMessage by viewModel.contentSaveMessage.collectAsState()

    // Focused Viewer state for rich immersion
    var viewingItem by remember { mutableStateOf<DisciplineContent?>(null) }

    // Display toast upon save/bookmark action
    LaunchedEffect(saveMessage) {
        saveMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearContentSaveMessage()
        }
    }

    BackHandler {
        if (viewingItem != null) {
            viewingItem = null
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshContentHub()
    }

    // Filter items based on selected category chip
    val filteredItems = remember(contentList, forYouList, savedList, selectedCategory) {
        when (selectedCategory) {
            ContentCategory.ALL -> contentList
            ContentCategory.FOR_YOU -> forYouList
            ContentCategory.SAVED -> savedList
            ContentCategory.MINDSET -> contentList.filter {
                it.category.equals("MINDSET", ignoreCase = true) || it.category.equals("MOTIVATION", ignoreCase = true)
            }
            else -> contentList.filter { it.category.equals(selectedCategory.name, ignoreCase = true) }
        }
    }

    LiquidGlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("content_hub_screen")
        ) {
            // Header: Screen title + Back Button + Refresh Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x261E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onNavigateBack() }
                            .testTag("content_hub_back_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "DISCOVER",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.2.sp,
                                fontSize = 18.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeonGreenBright.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, NeonGreenBright.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "DAYFORGE HUB",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreenBright,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Build better habits. One day at a time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Refresh Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x261E293B))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable { viewModel.refreshContentHub() }
                        .testTag("content_hub_refresh_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh Content",
                        tint = CyanBright,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Categories horizontal glass chips row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ContentCategory.values()) { category ->
                    val isSelected = category == selectedCategory
                    val chipBg = if (isSelected) NeonGreenBright.copy(alpha = 0.22f) else Color(0x40141C2E)
                    val chipBorder = if (isSelected) NeonGreenBright else Color(0x26FFFFFF)

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.selectContentCategory(category) }
                            .testTag("content_category_chip_${category.name.lowercase()}"),
                        shape = RoundedCornerShape(20.dp),
                        color = chipBg,
                        border = BorderStroke(1.dp, chipBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (category == ContentCategory.FOR_YOU) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isSelected) NeonGreenBright else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else if (category == ContentCategory.SAVED) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = null,
                                    tint = if (isSelected) GoldYellow else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Content Body
            when {
                isLoading && contentList.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = NeonGreenBright,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Loading DAYFORGE Discipline Feed...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                errorMsg != null && contentList.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0x80141C2E),
                            border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.CloudOff,
                                    contentDescription = null,
                                    tint = CrimsonRed,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "Couldn't load content.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Please check your network connection and try again.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = { viewModel.refreshContentHub() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonGreenBright,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("content_retry_button")
                                ) {
                                    Text("Try Again", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // If specific category is selected, or if user is on SAVED or FOR_YOU
                selectedCategory == ContentCategory.SAVED -> {
                    if (savedList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0x66141C2E),
                                border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.BookmarkBorder,
                                        contentDescription = null,
                                        tint = GoldYellow,
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Text(
                                        text = "Nothing saved yet.",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Save useful content and find it here later.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        VerticalFeedPager(
                            items = savedList,
                            savedIds = savedIds,
                            onToggleSave = { id -> viewModel.toggleSaveContent(id) },
                            onMarkViewed = { id -> viewModel.markContentAsViewed(id) },
                            onOpenViewer = { item -> viewingItem = item },
                            onShare = { item -> shareContent(context, item) },
                            onOpenHabits = onOpenHabits,
                            onOpenWater = onOpenWater,
                            onOpenFocus = onOpenFocus,
                            onOpenJournal = onOpenJournal,
                            onOpenAlarms = onOpenAlarms
                        )
                    }
                }

                selectedCategory == ContentCategory.FOR_YOU -> {
                    if (forYouList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0x66141C2E),
                                border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Text(
                                        text = "Build your DAYFORGE routine and we'll personalize your feed.",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "As you complete habits, track water, and run focus blocks, relevant insights appear here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        VerticalFeedPager(
                            items = forYouList,
                            savedIds = savedIds,
                            onToggleSave = { id -> viewModel.toggleSaveContent(id) },
                            onMarkViewed = { id -> viewModel.markContentAsViewed(id) },
                            onOpenViewer = { item -> viewingItem = item },
                            onShare = { item -> shareContent(context, item) },
                            onOpenHabits = onOpenHabits,
                            onOpenWater = onOpenWater,
                            onOpenFocus = onOpenFocus,
                            onOpenJournal = onOpenJournal,
                            onOpenAlarms = onOpenAlarms
                        )
                    }
                }

                // If ALL is selected: rich sectional Discover experience (Sections: FOR YOU, TRENDING, DISCIPLINE, FITNESS, STUDY, MINDSET)
                selectedCategory == ContentCategory.ALL -> {
                    DiscoverSectionalFeed(
                        allContent = contentList,
                        forYouList = forYouList,
                        trendingList = trendingList,
                        savedList = savedList,
                        savedIds = savedIds,
                        onSelectCategory = { cat -> viewModel.selectContentCategory(cat) },
                        onToggleSave = { id -> viewModel.toggleSaveContent(id) },
                        onOpenViewer = { item -> viewingItem = item },
                        onShare = { item -> shareContent(context, item) },
                        onOpenHabits = onOpenHabits,
                        onOpenWater = onOpenWater,
                        onOpenFocus = onOpenFocus,
                        onOpenJournal = onOpenJournal,
                        onOpenAlarms = onOpenAlarms
                    )
                }

                // Individual Category Filter Selected (e.g. FITNESS, STUDY, etc.)
                filteredItems.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0x66141C2E),
                            border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.HourglassEmpty,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(42.dp)
                                )
                                Text(
                                    text = "More coming soon.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "DAYFORGE content is being prepared. Stay disciplined and consistent.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                else -> {
                    VerticalFeedPager(
                        items = filteredItems,
                        savedIds = savedIds,
                        onToggleSave = { id -> viewModel.toggleSaveContent(id) },
                        onMarkViewed = { id -> viewModel.markContentAsViewed(id) },
                        onOpenViewer = { item -> viewingItem = item },
                        onShare = { item -> shareContent(context, item) },
                        onOpenHabits = onOpenHabits,
                        onOpenWater = onOpenWater,
                        onOpenFocus = onOpenFocus,
                        onOpenJournal = onOpenJournal,
                        onOpenAlarms = onOpenAlarms
                    )
                }
            }
        }

        // Focused Content Viewer Dialog
        viewingItem?.let { item ->
            ContentViewerDialog(
                content = item,
                isSaved = savedIds.contains(item.contentId),
                onDismiss = { viewingItem = null },
                onToggleSave = { viewModel.toggleSaveContent(item.contentId) },
                onShare = { shareContent(context, item) },
                onExecuteCta = { action ->
                    viewingItem = null
                    when (action?.lowercase()) {
                        "habits" -> onOpenHabits()
                        "water" -> onOpenWater()
                        "focus" -> onOpenFocus()
                        "journal" -> onOpenJournal()
                        "alarms" -> onOpenAlarms()
                        else -> onOpenHabits()
                    }
                }
            )
        }
    }
}

/**
 * Sectional Feed used for "ALL" tab:
 * - FOR YOU
 * - TRENDING
 * - DISCIPLINE
 * - FITNESS
 * - STUDY
 * - MINDSET
 * - SAVED
 */
@Composable
fun DiscoverSectionalFeed(
    allContent: List<DisciplineContent>,
    forYouList: List<DisciplineContent>,
    trendingList: List<DisciplineContent>,
    savedList: List<DisciplineContent>,
    savedIds: Set<String>,
    onSelectCategory: (ContentCategory) -> Unit,
    onToggleSave: (String) -> Unit,
    onOpenViewer: (DisciplineContent) -> Unit,
    onShare: (DisciplineContent) -> Unit,
    onOpenHabits: () -> Unit,
    onOpenWater: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenAlarms: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("discover_sectional_feed"),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. FOR YOU Section
        if (forYouList.isNotEmpty()) {
            item {
                DiscoverSectionHeader(
                    title = "FOR YOU",
                    subtitle = "Personalized from your active habits & focus blocks",
                    onSeeAll = { onSelectCategory(ContentCategory.FOR_YOU) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(forYouList.take(5), key = { "foryou_${it.contentId}" }) { item ->
                        CompactContentCard(
                            content = item,
                            isSaved = savedIds.contains(item.contentId),
                            onCardClick = { onOpenViewer(item) },
                            onToggleSave = { onToggleSave(item.contentId) }
                        )
                    }
                }
            }
        }

        // 2. TRENDING Section
        if (trendingList.isNotEmpty()) {
            item {
                DiscoverSectionHeader(
                    title = "TRENDING",
                    subtitle = "Top discipline insights & core principles",
                    onSeeAll = null
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trendingList, key = { "trend_${it.contentId}" }) { item ->
                        CompactContentCard(
                            content = item,
                            isSaved = savedIds.contains(item.contentId),
                            onCardClick = { onOpenViewer(item) },
                            onToggleSave = { onToggleSave(item.contentId) }
                        )
                    }
                }
            }
        }

        // 3. DISCIPLINE Section
        val disciplineItems = allContent.filter { it.category.equals("DISCIPLINE", ignoreCase = true) }
        if (disciplineItems.isNotEmpty()) {
            item {
                DiscoverSectionHeader(
                    title = "DISCIPLINE",
                    subtitle = "Consistency, iron routines & self-control",
                    onSeeAll = { onSelectCategory(ContentCategory.DISCIPLINE) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(disciplineItems, key = { "disc_${it.contentId}" }) { item ->
                        CompactContentCard(
                            content = item,
                            isSaved = savedIds.contains(item.contentId),
                            onCardClick = { onOpenViewer(item) },
                            onToggleSave = { onToggleSave(item.contentId) }
                        )
                    }
                }
            }
        }

        // 4. FITNESS Section
        val fitnessItems = allContent.filter { it.category.equals("FITNESS", ignoreCase = true) }
        if (fitnessItems.isNotEmpty()) {
            item {
                DiscoverSectionHeader(
                    title = "FITNESS",
                    subtitle = "Healthy movement, mobility, and strength",
                    onSeeAll = { onSelectCategory(ContentCategory.FITNESS) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(fitnessItems, key = { "fit_${it.contentId}" }) { item ->
                        CompactContentCard(
                            content = item,
                            isSaved = savedIds.contains(item.contentId),
                            onCardClick = { onOpenViewer(item) },
                            onToggleSave = { onToggleSave(item.contentId) }
                        )
                    }
                }
            }
        }

        // 5. STUDY & PRODUCTIVITY Section
        val studyItems = allContent.filter {
            it.category.equals("STUDY", ignoreCase = true) || it.category.equals("PRODUCTIVITY", ignoreCase = true)
        }
        if (studyItems.isNotEmpty()) {
            item {
                DiscoverSectionHeader(
                    title = "STUDY & PRODUCTIVITY",
                    subtitle = "Deep work flow blocks, focus & accelerated learning",
                    onSeeAll = { onSelectCategory(ContentCategory.STUDY) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(studyItems, key = { "study_${it.contentId}" }) { item ->
                        CompactContentCard(
                            content = item,
                            isSaved = savedIds.contains(item.contentId),
                            onCardClick = { onOpenViewer(item) },
                            onToggleSave = { onToggleSave(item.contentId) }
                        )
                    }
                }
            }
        }

        // 6. MINDSET Section
        val mindsetItems = allContent.filter {
            it.category.equals("MINDSET", ignoreCase = true) || it.category.equals("MOTIVATION", ignoreCase = true)
        }
        if (mindsetItems.isNotEmpty()) {
            item {
                DiscoverSectionHeader(
                    title = "MINDSET",
                    subtitle = "Stoic mental models & growth psychology",
                    onSeeAll = { onSelectCategory(ContentCategory.MINDSET) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mindsetItems, key = { "mind_${it.contentId}" }) { item ->
                        CompactContentCard(
                            content = item,
                            isSaved = savedIds.contains(item.contentId),
                            onCardClick = { onOpenViewer(item) },
                            onToggleSave = { onToggleSave(item.contentId) }
                        )
                    }
                }
            }
        }

        // 7. SAVED Section Preview
        if (savedList.isNotEmpty()) {
            item {
                DiscoverSectionHeader(
                    title = "SAVED",
                    subtitle = "Your private bookmarked content",
                    onSeeAll = { onSelectCategory(ContentCategory.SAVED) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(savedList, key = { "saved_${it.contentId}" }) { item ->
                        CompactContentCard(
                            content = item,
                            isSaved = true,
                            onCardClick = { onOpenViewer(item) },
                            onToggleSave = { onToggleSave(item.contentId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoverSectionHeader(
    title: String,
    subtitle: String,
    onSeeAll: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onSeeAll != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSeeAll() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreenBright,
                    fontSize = 12.sp
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = NeonGreenBright,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Compact horizontal cards for sectional view
 */
@Composable
fun CompactContentCard(
    content: DisciplineContent,
    isSaved: Boolean,
    onCardClick: () -> Unit,
    onToggleSave: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(220.dp)
            .height(210.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color(0x5900F0FF), Color(0x1AFFFFFF))
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable { onCardClick() }
            .testTag("compact_content_card_${content.contentId}"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xCC0E1526)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val mediaModel = content.thumbnailUrl.ifBlank { content.mediaUrl }
            if (mediaModel.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(mediaModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = content.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x66000000), Color(0xCC050814), Color(0xF5050814))
                        )
                    )
            )

            // Top row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xCC0B1120)
                ) {
                    Text(
                        text = content.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .clickable { onToggleSave() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (isSaved) GoldYellow else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Bottom text
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${content.creatorName} • ${content.duration}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Vertical Pager for modern vertical reel-style cards (single-category or filtered feed)
 */
@Composable
fun VerticalFeedPager(
    items: List<DisciplineContent>,
    savedIds: Set<String>,
    onToggleSave: (String) -> Unit,
    onMarkViewed: (String) -> Unit,
    onOpenViewer: (DisciplineContent) -> Unit,
    onShare: (DisciplineContent) -> Unit,
    onOpenHabits: () -> Unit,
    onOpenWater: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenAlarms: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("content_vertical_pager")
    ) { page ->
        val content = items[page]
        val isSaved = savedIds.contains(content.contentId)

        LaunchedEffect(content.contentId) {
            onMarkViewed(content.contentId)
        }

        ContentReelCard(
            content = content,
            isSaved = isSaved,
            onToggleSave = { onToggleSave(content.contentId) },
            onShare = { onShare(content) },
            onExecuteCta = { action ->
                when (action?.lowercase()) {
                    "habits" -> onOpenHabits()
                    "water" -> onOpenWater()
                    "focus" -> onOpenFocus()
                    "journal" -> onOpenJournal()
                    "alarms" -> onOpenAlarms()
                    else -> onOpenHabits()
                }
            }
        )
    }
}

/**
 * Interactive Full-Screen Focused Content Viewer
 */
@Composable
fun ContentViewerDialog(
    content: DisciplineContent,
    isSaved: Boolean,
    onDismiss: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onExecuteCta: (String?) -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val mediaModel = content.mediaUrl.ifBlank { content.thumbnailUrl }
                if (mediaModel.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(mediaModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = content.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x88000000), Color(0x40000000), Color(0xEE050814))
                            )
                        )
                )

                // Top Controls: Close [X], Category, Mute/Play
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onDismiss() }
                            .testTag("viewer_close_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xCC0B1120),
                        border = BorderStroke(1.dp, NeonCyan)
                    ) {
                        Text(
                            text = content.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x66000000))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                .clickable { isMuted = !isMuted },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                contentDescription = "Mute",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x66000000))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                .clickable { isPlaying = !isPlaying },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Floating Action Controls on Right (Save, Share)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x88000000))
                            .border(1.dp, if (isSaved) GoldYellow else Color(0x33FFFFFF), CircleShape)
                            .clickable { onToggleSave() }
                            .testTag("viewer_save_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (isSaved) GoldYellow else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x88000000))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onShare() }
                            .testTag("viewer_share_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Bottom Content Details & CTA
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = content.creatorName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Text(
                        text = content.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = content.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 20.sp
                    )
                    if (!content.ctaLabel.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { onExecuteCta(content.ctaAction) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreenBright,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "${content.ctaLabel} →",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContentReelCard(
    content: DisciplineContent,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onExecuteCta: (String?) -> Unit
) {
    var isLiked by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.2.dp,
                Brush.verticalGradient(
                    listOf(Color(0x6600F0FF), Color(0x2638BDF8), Color(0x1AFFFFFF))
                ),
                RoundedCornerShape(24.dp)
            )
            .testTag("content_card_${content.contentId}"),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xF00A0F1D)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val mediaModel = content.mediaUrl.ifBlank { content.thumbnailUrl }
            if (mediaModel.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(mediaModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = content.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xAA050814),
                                Color(0x66050814),
                                Color(0xDD050814),
                                Color(0xF803060E)
                            )
                        )
                    )
            )

            // Top Badges Row (Category + Featured + Duration)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xCC0B1120),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = content.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonCyan,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (content.isFeatured) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldYellow.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, GoldYellow.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.Bolt, null, tint = GoldYellow, modifier = Modifier.size(12.dp))
                                Text(
                                    text = "FEATURED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldYellow,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x80000000),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.Schedule, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                        Text(
                            text = content.duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Right Action Column (Like, Save/Bookmark, Share)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Like Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0x99141C2E))
                        .border(1.dp, if (isLiked) CrimsonRed else Color(0x33FFFFFF), CircleShape)
                        .clickable { isLiked = !isLiked }
                        .testTag("content_like_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) CrimsonRed else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Bookmark / Save Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0x99141C2E))
                        .border(1.dp, if (isSaved) GoldYellow else Color(0x33FFFFFF), CircleShape)
                        .clickable { onToggleSave() }
                        .testTag("content_bookmark_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isSaved) GoldYellow else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Share Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0x99141C2E))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable { onShare() }
                        .testTag("content_share_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Bottom Content Information Section
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0x3300F0FF))
                            .border(1.dp, NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Verified, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = content.creatorName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 19.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = content.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                if (!content.ctaLabel.isNullOrBlank() && !content.ctaAction.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { onExecuteCta(content.ctaAction) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreenBright,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("content_cta_button")
                    ) {
                        Text(
                            text = "${content.ctaLabel} →",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

private fun shareContent(context: android.content.Context, content: DisciplineContent) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "${content.title}\n\n\"${content.description}\"\n\n— DAYFORGE: Discipline Builds Freedom"
            )
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share DAYFORGE Discipline")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open share menu", Toast.LENGTH_SHORT).show()
    }
}
