package com.example.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.models.PublicAchievement
import com.example.data.models.PublicUserProfile
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.WatermarkFooter
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel
import com.example.ui.viewmodel.PublicProfileUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    username: String,
    viewModel: DisciplineViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.publicProfileUiState.collectAsState()

    LaunchedEffect(username) {
        viewModel.loadPublicProfile(username)
    }

    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "@$username",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("public_profile_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate Back",
                                tint = TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (val state = uiState) {
                    is PublicProfileUiState.Loading, is PublicProfileUiState.Idle -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = NeonGreenBright,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Loading athlete profile...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    is PublicProfileUiState.NotFound -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0x99101626),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Brush.verticalGradient(
                                        listOf(Color(0x33FFFFFF), Color(0x0DFFFFFF))
                                    )
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(CrimsonRed.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PersonOff,
                                            contentDescription = null,
                                            tint = CrimsonRed,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Text(
                                        text = "Athlete Not Found",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    Text(
                                        text = "No public athlete with the handle @${state.username} was found. Please check the spelling and try again.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )

                                    Button(
                                        onClick = onNavigateBack,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonGreenBright,
                                            contentColor = Color(0xFF0D131F)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Go Back", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    is PublicProfileUiState.Private -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color(0x99101626),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Brush.verticalGradient(
                                            listOf(GoldYellow.copy(alpha = 0.4f), Color(0x1AFFFFFF))
                                        )
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Avatar with lock overlay
                                        Box(
                                            modifier = Modifier.size(80.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(CircleShape)
                                                    .border(2.dp, GoldYellow.copy(alpha = 0.6f), CircleShape)
                                            ) {
                                                if (state.photoUrl.isNotBlank()) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(state.photoUrl)
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = "Avatar",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.avatar_discipline_default),
                                                        contentDescription = "Avatar",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }

                                            // Lock badge
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF141C2E),
                                                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldYellow),
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .align(Alignment.BottomEnd)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Lock,
                                                    contentDescription = "Private Profile",
                                                    tint = GoldYellow,
                                                    modifier = Modifier
                                                        .padding(5.dp)
                                                        .size(16.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = state.displayName.ifBlank { state.username },
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = GoldYellow.copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldYellow.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "🔒 PRIVATE PROFILE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldYellow,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }

                                        Text(
                                            text = "@${state.username} has chosen to keep their discipline records private. Activity, streaks, and score are protected.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center
                                        )

                                        // Safety Assurance Box
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0x40FFFFFF),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Security,
                                                    contentDescription = null,
                                                    tint = CyanBright,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "DayForge safeguards personal habits, water intake, notes, and schedules.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextMuted
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = onNavigateBack,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0x33FFFFFF),
                                                contentColor = TextPrimary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Go Back", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                WatermarkFooter()
                            }
                        }
                    }

                    is PublicProfileUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = CrimsonRed,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = { viewModel.loadPublicProfile(username) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreenBright)
                                ) {
                                    Text("Retry", color = Color(0xFF0D131F), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is PublicProfileUiState.Success -> {
                        PublicProfileContent(profile = state.profile)
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicProfileContent(profile: PublicUserProfile) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)
    ) {
        // Hero Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0x99101626),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(NeonGreenBright.copy(alpha = 0.5f), Color(0x1AFFFFFF))
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(NeonGreenBright.copy(alpha = 0.10f), Color.Transparent),
                                radius = 400f
                            )
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar with glowing ring
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                brush = Brush.sweepGradient(listOf(NeonGreenBright, CyanBright, NeonGreenBright)),
                                shape = CircleShape
                            )
                    ) {
                        if (profile.photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(profile.photoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.avatar_discipline_default),
                                contentDescription = "Profile Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Display Name
                    Text(
                        text = profile.displayName.ifBlank { profile.username },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    // @username handle chip
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = NeonGreenBright.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreenBright.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "@${profile.username}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonGreenBright,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    // Short Bio (if present)
                    if (profile.bio.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x33FFFFFF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "“${profile.bio}”",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Stats Showcase Row (Score, Current Streak, Best Streak)
        item {
            Text(
                text = "DISCIPLINE METRICS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Discipline Score Card
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Score",
                    value = "${profile.disciplineScore}",
                    subtitle = when {
                        profile.disciplineScore >= 90 -> "Elite"
                        profile.disciplineScore >= 75 -> "Master"
                        profile.disciplineScore >= 50 -> "Committed"
                        else -> "Rising"
                    },
                    accentColor = NeonGreenBright,
                    icon = Icons.Outlined.Shield
                )

                // Current Streak Card
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Streak",
                    value = "${profile.streak}d",
                    subtitle = "Active Daily",
                    accentColor = GoldYellow,
                    icon = Icons.Filled.LocalFireDepartment
                )

                // Best Streak Card
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Best",
                    value = "${profile.bestStreak}d",
                    subtitle = "All-Time",
                    accentColor = CyanBright,
                    icon = Icons.Outlined.EmojiEvents
                )
            }
        }

        // Public Achievements Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PUBLIC ACHIEVEMENTS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${profile.publicAchievements.size} Unlocked",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonGreenBright,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (profile.publicAchievements.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x66101626),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MilitaryTech,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No public achievements unlocked yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(profile.publicAchievements) { ach ->
                PublicAchievementCard(achievement = ach)
            }
        }

        // Teenager Safe Shield Notice
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0x66101626),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanBright.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyanBright.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PrivacyTip,
                            contentDescription = null,
                            tint = CyanBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "DayForge Privacy Protected",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Personal habit names, completion history, water intake, focus sessions, and night notes are strictly concealed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        item {
            WatermarkFooter()
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0x99101626),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PublicAchievementCard(achievement: PublicAchievement) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x8C141C2E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1FFFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(GoldYellow.copy(alpha = 0.15f))
                    .border(1.dp, GoldYellow.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = GoldYellow,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (achievement.description.isNotBlank()) {
                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = NeonGreenBright.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = NeonGreenBright,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Unlocked",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonGreenBright,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
