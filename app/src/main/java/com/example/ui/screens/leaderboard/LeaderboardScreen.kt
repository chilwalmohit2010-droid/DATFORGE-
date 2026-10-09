package com.example.ui.screens.leaderboard

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.models.LeaderboardEntry
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.StreakFlameBadge
import com.example.ui.components.tactilePress
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel

@Composable
fun LeaderboardScreen(
    viewModel: DisciplineViewModel,
    onBack: () -> Unit,
    onOpenPublicProfile: (String) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val entries by viewModel.leaderboardEntries.collectAsState()
    val isLoading by viewModel.isLeaderboardLoading.collectAsState()
    val userRank by viewModel.userLeaderboardRank.collectAsState()

    // Trigger initial fetch when screen opens
    LaunchedEffect(Unit) {
        viewModel.refreshLeaderboard()
    }

    val isUserParticipating = currentUser?.leaderboardEnabled == true
    val top1 = entries.getOrNull(0)
    val top2 = entries.getOrNull(1)
    val top3 = entries.getOrNull(2)

    LiquidGlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header: [<-]  Leaderboard  [Refresh]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x261E293B))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable { onBack() }
                        .testTag("leaderboard_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Leaderboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Compete with discipline, not comparison.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x261E293B))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable { viewModel.refreshLeaderboard() }
                        .testTag("leaderboard_refresh_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = CyanAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh Leaderboard",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("leaderboard_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // Privacy / Opt-out Banner if Leaderboard is OFF
                if (!isUserParticipating) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .border(
                                    1.dp,
                                    Brush.verticalGradient(listOf(Color(0x66FBBF24), Color(0x1AFBBF24))),
                                    RoundedCornerShape(20.dp)
                                ),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0x8C141C2E)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(listOf(Color(0x1AFBBF24), Color.Transparent))
                                    )
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x26FBBF24)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.VisibilityOff,
                                        contentDescription = null,
                                        tint = NeonAmber,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "You're currently hidden from the Global Leaderboard.",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Only your selected public leaderboard information (name, username, Discipline Score, streak) will be visible. Private habits, notes, and records are never exposed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { viewModel.toggleLeaderboardParticipation(true) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonGreen,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(44.dp)
                                        .testTag("enable_leaderboard_button")
                                ) {
                                    Text(
                                        "Enable Leaderboard",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Current User's Own Standing Card (Screen 6 Spec)
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .border(
                                    1.dp,
                                    Brush.verticalGradient(listOf(Color(0x8038BDF8), Color(0x2638BDF8))),
                                    RoundedCornerShape(20.dp)
                                ),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0x990F172A)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(listOf(Color(0x1F38BDF8), Color.Transparent))
                                    )
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0x2600F5FF))
                                            .border(1.5.dp, CyanAccent.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "RANK",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CyanAccent
                                            )
                                            Text(
                                                text = if (userRank != null && userRank!! > 0) "#${userRank}" else "#—",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = currentUser?.displayName?.ifBlank { "You" } ?: "You",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "@${currentUser?.username?.ifBlank { "you" } ?: "you"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CyanAccent,
                                            fontSize = 11.5.sp
                                        )
                                        Text(
                                            text = "${currentUser?.currentStreak ?: 0} days streak",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${currentUser?.disciplineScore ?: 0}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        color = NeonGreenBright
                                    )
                                    Text(
                                        text = "Discipline Score",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Top 3 Podium Section
                if (entries.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "TOP PODIUM",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMuted,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // 2nd Place (Left)
                                top2?.let { entry ->
                                    PodiumAthleteItem(
                                        entry = entry,
                                        rank = 2,
                                        rankColor = Color(0xFFC0C0C0),
                                        podiumHeight = 110.dp,
                                        onClick = { onOpenPublicProfile(entry.username) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } ?: Spacer(modifier = Modifier.weight(1f))

                                // 1st Place (Center - Elevated)
                                top1?.let { entry ->
                                    PodiumAthleteItem(
                                        entry = entry,
                                        rank = 1,
                                        rankColor = Color(0xFFFBBF24),
                                        podiumHeight = 135.dp,
                                        onClick = { onOpenPublicProfile(entry.username) },
                                        modifier = Modifier.weight(1.15f)
                                    )
                                }

                                // 3rd Place (Right)
                                top3?.let { entry ->
                                    PodiumAthleteItem(
                                        entry = entry,
                                        rank = 3,
                                        rankColor = Color(0xFFCD7F32),
                                        podiumHeight = 95.dp,
                                        onClick = { onOpenPublicProfile(entry.username) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } ?: Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Global Rankings Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Global Rankings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "${entries.size} Athletes",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Empty State
                if (entries.isEmpty() && !isLoading) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0x66141C2E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33FBBF24)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.EmojiEvents,
                                        contentDescription = null,
                                        tint = NeonAmber,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    text = "Leaderboard is waiting for disciplined people.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Be the first to step onto the global stage and showcase your daily consistency.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                if (!isUserParticipating) {
                                    Button(
                                        onClick = { viewModel.toggleLeaderboardParticipation(true) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonGreen,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Enable Leaderboard", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Ranking Items List
                    itemsIndexed(entries, key = { _, item -> item.uid }) { index, entry ->
                        val isCurrentUser = entry.uid == currentUser?.uid
                        val rank = index + 1

                        LeaderboardRowItem(
                            entry = entry,
                            rank = rank,
                            isCurrentUser = isCurrentUser,
                            onClick = { onOpenPublicProfile(entry.username) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumAthleteItem(
    entry: LeaderboardEntry,
    rank: Int,
    rankColor: Color,
    podiumHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar + Crown
        Box(
            modifier = Modifier.size(if (rank == 1) 64.dp else 52.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(2.dp, rankColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (entry.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(entry.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = entry.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.avatar_discipline_default),
                        contentDescription = "Default Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Crown or Medal badge
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 6.dp),
                shape = CircleShape,
                color = rankColor
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = entry.displayName.ifBlank { entry.username },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "@${entry.username}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Podium Block
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(podiumHeight),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = Color(0x66141C2E),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.verticalGradient(listOf(rankColor.copy(alpha = 0.6f), Color(0x1AFFFFFF)))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${entry.disciplineScore}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = NeonGreenBright
                )
                Text(
                    text = "Score",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 9.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                StreakFlameBadge(streakDays = entry.currentStreak)
            }
        }
    }
}

@Composable
private fun LeaderboardRowItem(
    entry: LeaderboardEntry,
    rank: Int,
    isCurrentUser: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val borderColor = if (isCurrentUser) Brush.verticalGradient(listOf(CyanAccent, NeonGreen))
    else Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x0DFFFFFF)))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("leaderboard_row_${entry.uid}"),
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrentUser) Color(0x9910243E) else Color(0x66141C2E)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rank Number
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = when (rank) {
                        1 -> NeonAmber
                        2 -> Color(0xFFC0C0C0)
                        3 -> Color(0xFFCD7F32)
                        else -> if (isCurrentUser) CyanAccent else TextSecondary
                    },
                    modifier = Modifier.width(32.dp)
                )

                // Avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0x40FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (entry.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(entry.photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = entry.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.avatar_discipline_default),
                            contentDescription = "Default Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = entry.displayName.ifBlank { entry.username },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCurrentUser) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CyanAccent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "YOU",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanAccent,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "@${entry.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Score & Streak
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StreakFlameBadge(streakDays = entry.currentStreak)

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${entry.disciplineScore}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = NeonGreenBright
                    )
                    Text(
                        text = "pts",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
