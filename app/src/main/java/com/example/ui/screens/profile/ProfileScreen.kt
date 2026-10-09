package com.example.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.models.Achievement
import com.example.data.models.UserAccount
import com.example.ui.components.CommonTopBar
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.WatermarkFooter
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel
import com.example.util.ProfileImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: DisciplineViewModel,
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null,
    onOpenWaterTracker: () -> Unit = {},
    onOpenReminders: () -> Unit = {},
    onOpenNightReview: () -> Unit = {},
    onOpenDiscoverUsers: () -> Unit = {},
    onOpenPublicProfile: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAlarms: () -> Unit = {},
    onOpenLeaderboard: () -> Unit = {},
    onOpenContentHub: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val disciplineBreakdown by viewModel.disciplineScoreBreakdown.collectAsState()
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showPhase2Dialog by remember { mutableStateOf(false) }
    var showScoreBreakdownDialog by remember { mutableStateOf(false) }
    var achievementFilter by remember { mutableStateOf("All") }

    LiquidGlassBackground {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("profile_screen_column"),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Top Header: Screen 10 Match: [←]  Profile  [⚙]
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x261E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onBack?.invoke() }
                            .testTag("profile_back_button"),
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
                        text = "Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x261E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onOpenSettings() }
                            .testTag("profile_settings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Advanced Settings",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // Hero Profile Glass Card
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
                            RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0x8C141C2E)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x1AFFFFFF), Color.Transparent)
                                )
                            )
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile DP with glow ring
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, Brush.sweepGradient(listOf(NeonGreenBright, CyanAccent, NeonGreenBright)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentUser?.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(currentUser?.photoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profile DP",
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

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = currentUser?.displayName?.ifBlank { "Discipline Athlete" } ?: "Discipline Athlete",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        Text(
                            text = "@${currentUser?.username?.ifBlank { "warrior" } ?: "warrior"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreenBright
                        )

                        if (!currentUser?.bio.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "\"${currentUser?.bio}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Public Stats Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x660B101D))
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(16.dp))
                                .padding(vertical = 14.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showScoreBreakdownDialog = true }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${currentUser?.disciplineScore ?: 0}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonGreenBright
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text("Discipline", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = "View score breakdown",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }

                            Divider(
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(1.dp),
                                color = Color(0x33FFFFFF)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${currentUser?.currentStreak ?: 0}d",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Text("Current Streak", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }

                            Divider(
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(1.dp),
                                color = Color(0x33FFFFFF)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val ratePct = ((currentUser?.completionRate ?: 0.8f) * 100).toInt()
                                Text(
                                    text = "$ratePct%",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyanAccent
                                )
                                Text("Completion", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // Guest Session Banner
            if (currentUser?.uid?.startsWith("guest_") == true) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .border(1.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x2600F5FF)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Guest Session (Offline)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "All data is stored on this device. Sign up to enable cloud backup & leaderboard sync.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Button(
                                onClick = onLogout,
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Sign Up", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Privacy Guarantee Notice Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))
                            ),
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
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
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = NeonGreenBright, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Your private habit titles, notes, and water records are strictly private and never publicly exposed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Achievements Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Discipline Achievements",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            val unlockedCount = achievements.count { it.isUnlocked }
                            Text(
                                text = "$unlockedCount of ${achievements.size} milestones unlocked",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        // Filter Chips
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("All", "Unlocked", "Locked").forEach { filter ->
                                val isSelected = achievementFilter == filter
                                Surface(
                                    modifier = Modifier.clickable { achievementFilter = filter },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) NeonGreenBright.copy(alpha = 0.2f) else Color(0x331E293B),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) NeonGreenBright.copy(alpha = 0.6f) else Color(0x26FFFFFF)
                                    )
                                ) {
                                    Text(
                                        text = filter,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) NeonGreenBright else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val filteredAchievements = remember(achievements, achievementFilter) {
                        when (achievementFilter) {
                            "Unlocked" -> achievements.filter { it.isUnlocked }
                            "Locked" -> achievements.filter { !it.isUnlocked }
                            else -> achievements
                        }
                    }

                    if (filteredAchievements.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x4D141C2E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF))
                        ) {
                            Text(
                                text = if (achievementFilter == "Unlocked") "No achievements unlocked yet. Check in habits, log water, and focus to unlock milestones!" else "All achievements unlocked! Incredible discipline.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    } else {
                        filteredAchievements.chunked(2).forEach { rowList ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowList.forEach { achievement ->
                                    val isUnlocked = achievement.isUnlocked
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                1.dp,
                                                if (isUnlocked) Brush.verticalGradient(listOf(Color(0x8022C55E), Color(0x3322C55E)))
                                                else Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x0DFFFFFF))),
                                                RoundedCornerShape(16.dp)
                                            ),
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isUnlocked) Color(0x8C141C2E) else Color(0x660B101D)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isUnlocked) Brush.verticalGradient(listOf(Color(0x2622C55E), Color.Transparent))
                                                    else Brush.verticalGradient(listOf(Color(0x0DFFFFFF), Color.Transparent))
                                                )
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isUnlocked) Color(0x3322C55E) else Color(0x331E293B)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val icon = when (achievement.icon) {
                                                    "flag" -> Icons.Filled.Flag
                                                    "local_fire_department" -> Icons.Filled.LocalFireDepartment
                                                    "bolt" -> Icons.Filled.Bolt
                                                    "military_tech" -> Icons.Filled.MilitaryTech
                                                    "construction" -> Icons.Filled.Construction
                                                    "all_inclusive" -> Icons.Filled.AllInclusive
                                                    "water_drop" -> Icons.Filled.WaterDrop
                                                    "psychology" -> Icons.Filled.Psychology
                                                    "nightlight_round" -> Icons.Filled.NightlightRound
                                                    else -> Icons.Filled.EmojiEvents
                                                }
                                                Icon(
                                                    imageVector = if (isUnlocked) icon else Icons.Outlined.Lock,
                                                    contentDescription = null,
                                                    tint = if (isUnlocked) AmberWarning else TextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = achievement.title,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isUnlocked) TextPrimary else TextSecondary,
                                                        maxLines = 1
                                                    )
                                                }
                                                Text(
                                                    text = if (isUnlocked) achievement.description else "Target: ${achievement.description}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary,
                                                    fontSize = 10.sp,
                                                    maxLines = 2
                                                )
                                            }
                                        }
                                    }
                                }
                                if (rowList.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Hydration & Water Settings Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenWaterTracker() }
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .testTag("profile_water_settings_button"),
                    shape = RoundedCornerShape(18.dp),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x3306B6D4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.WaterDrop, null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Hydration & Water Reminders", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Daily goal, history & smart interval alerts", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary)
                    }
                }
            }

            // Reminders & Notifications Settings Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenReminders() }
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))
                            ),
                            RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x3322C55E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.NotificationsActive, null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Reminders & Alerts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Configure habit alarms, water timer & night review", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary)
                    }
                }
            }

            // Wake-Up Challenge Alarms Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenAlarms() }
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x6622C55E), Color(0x2638BDF8), Color(0x10FFFFFF))
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .testTag("profile_wake_up_alarms_button"),
                    shape = RoundedCornerShape(18.dp),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x3322C55E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Alarm, null, tint = NeonGreenBright, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Wake-Up Challenges ⚔️", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Push-ups, squats & movement alarms with pose detection", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary)
                    }
                }
            }

            // Global Leaderboard & Competition Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenLeaderboard() }
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x66FBBF24), Color(0x2638BDF8), Color(0x10FFFFFF))
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .testTag("profile_leaderboard_button"),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0x8C141C2E)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x1AFBBF24), Color.Transparent)
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x33FBBF24)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.EmojiEvents, null, tint = NeonAmber, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Global Leaderboard 🏆", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Compete in rankings with your Discipline Score", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary)
                    }
                }
            }

            // Night Review & Reflection Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenNightReview() }
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x59A855F7), Color(0x2638BDF8), Color(0x10FFFFFF))
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .testTag("profile_night_review_button"),
                    shape = RoundedCornerShape(18.dp),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x33A855F7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Nightlight, null, tint = Color(0xFFC084FC), modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Night Review & Reflection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Evening closure, day rating, and reflection history", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary)
                    }
                }
            }

            // Advanced Settings & Control Center Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenSettings() }
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x5938BDF8), Color(0x1A38BDF8))
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .testTag("profile_advanced_settings_button"),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0x8C141C2E)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x1A38BDF8), Color.Transparent)
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x3338BDF8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Settings, null, tint = CyanBright, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Advanced Settings & Preferences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Account, privacy guarantee, reduced motion & data", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary)
                    }
                }
            }

            // Public Profile Section
            item {
                Text(
                    text = "PUBLIC PROFILE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Public Profile Visibility Setting Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))
                            ),
                            RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (currentUser?.publicProfileEnabled == true)
                                            NeonGreenBright.copy(alpha = 0.2f)
                                        else Color(0x26FFFFFF)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentUser?.publicProfileEnabled == true) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = null,
                                    tint = if (currentUser?.publicProfileEnabled == true) NeonGreenBright else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Public Profile",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (currentUser?.publicProfileEnabled == true) NeonGreenBright.copy(alpha = 0.15f) else Color(0x26FFFFFF)
                                    ) {
                                        Text(
                                            text = if (currentUser?.publicProfileEnabled == true) "ON" else "OFF",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentUser?.publicProfileEnabled == true) NeonGreenBright else TextMuted,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                val isGuest = currentUser?.uid?.startsWith("guest_") == true
                                Text(
                                    text = if (isGuest)
                                        "Guest Mode (Local Only). Sign in to enable public discovery."
                                    else if (currentUser?.publicProfileEnabled == true)
                                        "Discoverable by @${currentUser?.username}. Streaks & score visible."
                                    else
                                        "Private. Hidden from public athlete search.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        val context = LocalContext.current
                        val isGuestMode = currentUser?.uid?.startsWith("guest_") == true
                        Switch(
                            checked = if (isGuestMode) false else (currentUser?.publicProfileEnabled ?: false),
                            onCheckedChange = { enabled ->
                                if (isGuestMode) {
                                    android.widget.Toast.makeText(context, "Public discovery requires an authenticated DayForge account. Sign in to enable.", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    viewModel.togglePublicProfile(enabled) { success, error ->
                                        if (!success && error != null) {
                                            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF0D131F),
                                checkedTrackColor = NeonGreenBright,
                                uncheckedTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier.testTag("public_profile_switch")
                        )
                    }
                }
            }

            // Settings -> Launch Animation Switch
            item {
                val isLaunchAnimEnabled by viewModel.isLaunchAnimationEnabled.collectAsState()
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))
                            ),
                            RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isLaunchAnimEnabled)
                                            CyanBright.copy(alpha = 0.2f)
                                        else Color(0x26FFFFFF)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Animation,
                                    contentDescription = null,
                                    tint = if (isLaunchAnimEnabled) CyanBright else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Launch Animation",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isLaunchAnimEnabled) CyanBright.copy(alpha = 0.15f) else Color(0x26FFFFFF)
                                    ) {
                                        Text(
                                            text = if (isLaunchAnimEnabled) "ON" else "OFF",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLaunchAnimEnabled) CyanBright else TextMuted,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isLaunchAnimEnabled)
                                        "4-second DayForge live construction animation on cold launch."
                                    else
                                        "Disabled. Opens directly to the Home screen on cold launch.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isLaunchAnimEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.toggleLaunchAnimation(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF0D131F),
                                checkedTrackColor = CyanBright,
                                uncheckedTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier.testTag("launch_animation_switch")
                        )
                    }
                }
            }

            // Preview Public Profile Button (When username exists)
            if (!currentUser?.username.isNullOrBlank()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                onOpenPublicProfile(currentUser?.username ?: "")
                            }
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(NeonGreenBright.copy(alpha = 0.3f), Color(0x14FFFFFF))
                                ),
                                RoundedCornerShape(18.dp)
                            ),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0x8C141C2E)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CyanBright.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountBox,
                                        contentDescription = null,
                                        tint = CyanBright,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "View My Public Profile",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Preview what other athletes see when looking up @${currentUser?.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary)
                        }
                    }
                }
            }

            // Find Users / Search Users Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenDiscoverUsers() }
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .testTag("profile_find_users_button"),
                    shape = RoundedCornerShape(18.dp),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricIndigo.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.PersonSearch, null, tint = CyanBright, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "Find Users",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Search by @username or username",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary)
                    }
                }
            }

            // Discover / Content Hub Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenContentHub() }
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x6600F0FF), Color(0x2638BDF8), Color(0x10FFFFFF))
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .testTag("profile_content_hub_button"),
                    shape = RoundedCornerShape(18.dp),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x3300F0FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AutoAwesome, null, tint = Color(0xFF00F0FF), modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "DAYFORGE Discover",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Fitness, Health & Discipline feeds",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary)
                    }
                }
            }

            // Phase 2 Native Features Info
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { showPhase2Dialog = true }
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))
                            ),
                            RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x3306B6D4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Sensors, null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Phase 2 Hardware Modules", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Camera pushup AI, exact challenge alarm, focus lock", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary)
                    }
                }
            }

            // Logout Button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    OutlinedButton(
                        onClick = { showLogoutConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("logout_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonRed)
                    ) {
                        Icon(Icons.Filled.Logout, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out of Discipline", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Watermark Footer
            item {
                WatermarkFooter()
            }
        }
    }

    // Edit Profile Modal Dialog
    if (showEditProfileDialog) {
        var editUsername by remember { mutableStateOf(currentUser?.username ?: "") }
        var editDisplayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
        var editBio by remember { mutableStateOf(currentUser?.bio ?: "") }
        var editPhotoUrl by remember { mutableStateOf(currentUser?.photoUrl ?: "") }
        var editPublicProfileEnabled by remember { mutableStateOf(currentUser?.publicProfileEnabled ?: false) }
        var editError by remember { mutableStateOf<String?>(null) }
        var isSaving by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                val uid = currentUser?.uid ?: "current"
                val processed = ProfileImageUtils.processAndSaveProfileImage(context, uri, uid)
                if (processed != null) {
                    editPhotoUrl = processed
                    Toast.makeText(context, "Photo selected! Save to apply.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to process photo from gallery.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val presetAvatars = listOf(
            "", // Default generated vector/asset
            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150"
        )

        Dialog(onDismissRequest = { if (!isSaving) showEditProfileDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        1.dp,
                        Brush.verticalGradient(listOf(Color(0x80FFFFFF), Color(0x26FFFFFF))),
                        RoundedCornerShape(24.dp)
                    ),
                color = DeepNavyBg,
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x26FFFFFF), Color.Transparent)
                            )
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Edit Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = TextPrimary)

                    if (editError != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CrimsonRed.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.ErrorOutline, null, tint = CrimsonRed, modifier = Modifier.size(16.dp))
                                Text(editError ?: "", color = CrimsonRed, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Avatar Selection & Preview
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .border(
                                    2.5.dp,
                                    Brush.sweepGradient(listOf(NeonGreenBright, CyanAccent, NeonGreenBright)),
                                    CircleShape
                                )
                                .testTag("edit_avatar_preview"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (editPhotoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(editPhotoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Avatar Preview",
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Choose from Gallery / Change / Remove Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreenBright.copy(alpha = 0.2f),
                                    contentColor = NeonGreenBright
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreenBright.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("edit_choose_photo_button")
                            ) {
                                Icon(Icons.Outlined.PhotoCamera, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (editPhotoUrl.isNotBlank()) "Change Photo" else "Choose from Gallery",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            if (editPhotoUrl.isNotBlank()) {
                                TextButton(
                                    onClick = { editPhotoUrl = "" },
                                    modifier = Modifier.testTag("edit_remove_photo_button")
                                ) {
                                    Icon(Icons.Outlined.Delete, null, tint = CoralRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove", color = CoralRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Or Select Preset Avatar", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            presetAvatars.forEach { url ->
                                val isSelected = (editPhotoUrl == url)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) NeonGreenBright else Color(0x33FFFFFF),
                                            shape = CircleShape
                                        )
                                        .clickable { editPhotoUrl = url },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (url.isBlank()) {
                                        Image(
                                            painter = painterResource(id = R.drawable.avatar_discipline_default),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current).data(url).build(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editDisplayName,
                        onValueChange = { editDisplayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_display_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreenBright,
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        )
                    )

                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' } },
                        label = { Text("Username (@handle)") },
                        leadingIcon = { Text("@", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = NeonGreenBright) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_username_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreenBright,
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        )
                    )

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("edit_bio_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreenBright,
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        )
                    )

                    // Public Profile Visibility Switch
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x33FFFFFF),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (editPublicProfileEnabled) NeonGreenBright.copy(alpha = 0.5f) else Color(0x26FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (editPublicProfileEnabled) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                        contentDescription = null,
                                        tint = if (editPublicProfileEnabled) NeonGreenBright else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Public Profile",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = if (editPublicProfileEnabled)
                                        "ON • Discoverable by @$editUsername. Only streaks & score visible."
                                    else
                                        "OFF • Private. Hidden from athlete discovery.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (editPublicProfileEnabled) NeonGreenBright else TextSecondary
                                )
                            }
                            Switch(
                                checked = editPublicProfileEnabled,
                                onCheckedChange = { editPublicProfileEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF0D131F),
                                    checkedTrackColor = NeonGreenBright,
                                    uncheckedTrackColor = Color(0x33FFFFFF)
                                ),
                                modifier = Modifier.testTag("edit_public_profile_switch")
                            )
                        }
                    }

                    // Teenager Safe Privacy Reminder Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x1FFFFFFF),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = CyanBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Habit names, completion history, water logs, and night notes are strictly confidential.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showEditProfileDialog = false },
                            enabled = !isSaving
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (editUsername.length < 3) {
                                    editError = "Username must be at least 3 characters."
                                    return@Button
                                }
                                if (editDisplayName.isBlank()) {
                                    editError = "Please enter a display name."
                                    return@Button
                                }
                                editError = null
                                isSaving = true
                                viewModel.saveProfile(
                                    username = editUsername,
                                    displayName = editDisplayName,
                                    bio = editBio,
                                    photoUrl = editPhotoUrl,
                                    publicProfileEnabled = editPublicProfileEnabled,
                                    onSuccess = {
                                        isSaving = false
                                        showEditProfileDialog = false
                                    },
                                    onError = { err ->
                                        isSaving = false
                                        editError = err
                                    }
                                )
                            },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreenBright, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_profile_button")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Save Profile", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Phase 2 Native Architecture Info Dialog
    if (showPhase2Dialog) {
        AlertDialog(
            onDismissRequest = { showPhase2Dialog = false },
            title = { Text("Phase 2 Hardware Integration", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Phase 1 Core Application & Cloud Foundation is complete. Phase 2 extends this with deep Android sensor integrations:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("• CameraX real-time Push-Up AI counter\n• Exact AlarmManager wake-up challenges\n• UsageStatsManager / Accessibility App Lock\n• Native Android Push Notifications & Haptics", style = MaterialTheme.typography.bodySmall, color = CyanAccent)
                }
            },
            confirmButton = {
                Button(onClick = { showPhase2Dialog = false }, colors = ButtonDefaults.buttonColors(containerColor = NeonGreenBright, contentColor = Color.Black)) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DeepNavyBg
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Sign Out?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Are you sure you want to sign out of your account?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DeepNavyBg
        )
    }

    // Discipline Score Breakdown Dialog
    if (showScoreBreakdownDialog) {
        AlertDialog(
            onDismissRequest = { showScoreBreakdownDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = NeonGreenBright)
                    Text("Discipline Score Breakdown", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${disciplineBreakdown.score} / 100",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = NeonGreenBright
                        )
                        val tier = when {
                            disciplineBreakdown.score >= 85 -> "Elite" to NeonGreenBright
                            disciplineBreakdown.score >= 70 -> "Master" to CyanAccent
                            disciplineBreakdown.score >= 50 -> "Consistent" to AmberWarning
                            disciplineBreakdown.score >= 25 -> "Building" to Color(0xFF60A5FA)
                            else -> "Starter" to TextSecondary
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = tier.second.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, tier.second.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = tier.first,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = tier.second,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Score is calculated transparently from real activity. Consistency matters more than extreme spikes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Divider(color = Color(0x26FFFFFF))

                    // 1. Habit Completion
                    BreakdownDetailRow(
                        title = "Habits (30 pts max)",
                        points = disciplineBreakdown.habitsScore,
                        maxPoints = 30,
                        explanation = disciplineBreakdown.habitsExplanation,
                        color = NeonGreenBright
                    )

                    // 2. Streak
                    BreakdownDetailRow(
                        title = "Streak Consistency (20 pts max)",
                        points = disciplineBreakdown.streakScore,
                        maxPoints = 20,
                        explanation = disciplineBreakdown.streakExplanation,
                        color = AmberWarning
                    )

                    // 3. Water
                    BreakdownDetailRow(
                        title = "Hydration Goal (15 pts max)",
                        points = disciplineBreakdown.waterScore,
                        maxPoints = 15,
                        explanation = disciplineBreakdown.waterExplanation,
                        color = CyanAccent
                    )

                    // 4. Focus
                    BreakdownDetailRow(
                        title = "Focus Flow (20 pts max)",
                        points = disciplineBreakdown.focusScore,
                        maxPoints = 20,
                        explanation = disciplineBreakdown.focusExplanation,
                        color = Color(0xFFA855F7)
                    )

                    // 5. Review
                    BreakdownDetailRow(
                        title = "Night Reviews (15 pts max)",
                        points = disciplineBreakdown.reviewScore,
                        maxPoints = 15,
                        explanation = disciplineBreakdown.reviewExplanation,
                        color = Color(0xFF6366F1)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showScoreBreakdownDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreenBright, contentColor = Color.Black)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DeepNavyBg
        )
    }
}

@Composable
private fun BreakdownDetailRow(
    title: String,
    points: Int,
    maxPoints: Int,
    explanation: String,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("$points / $maxPoints", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(explanation, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
    }
}
