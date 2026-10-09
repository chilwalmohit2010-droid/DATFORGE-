package com.example.ui.screens.settings

import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.WatermarkFooter
import com.example.ui.components.tactilePress
import com.example.ui.screens.permissions.PermissionCenterSection
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel
import com.example.util.ProfileImageUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DisciplineViewModel,
    onNavigateBack: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenPublicProfile: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isLaunchAnimEnabled by viewModel.isLaunchAnimationEnabled.collectAsState()
    val isReducedMotion by viewModel.isReducedMotionEnabled.collectAsState()
    val reminderSettings by viewModel.reminderSettings.collectAsState()

    var editDisplayName by remember(currentUser) { mutableStateOf(currentUser?.displayName ?: "") }
    var editUsername by remember(currentUser) { mutableStateOf(currentUser?.username ?: "") }
    var editBio by remember(currentUser) { mutableStateOf(currentUser?.bio ?: "") }
    var editPhotoUrl by remember(currentUser) { mutableStateOf(currentUser?.photoUrl ?: "") }

    var isSavingProfile by remember { mutableStateOf(false) }
    var profileSaveSuccessMessage by remember { mutableStateOf<String?>(null) }
    var profileErrorMessage by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // Native Gallery Image Picker via ActivityResultContracts.PickVisualMedia
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val uid = currentUser?.uid ?: "current"
            val processed = ProfileImageUtils.processAndSaveProfileImage(context, uri, uid)
            if (processed != null) {
                editPhotoUrl = processed
                Toast.makeText(context, "Photo selected! Tap 'Save Profile' to lock in changes.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to process photo from gallery.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0x331E293B))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                .testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }

                        Column {
                            Text(
                                text = "Advanced Settings",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Account, privacy, appearance & preferences",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag("advanced_settings_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ==========================================
                // 1. ACCOUNT & PROFILE SECTION
                // ==========================================
                item {
                    Text(
                        text = "ACCOUNT & IDENTITY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                    )
                }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color(0x59FFFFFF), Color(0x14FFFFFF))
                                ),
                                RoundedCornerShape(22.dp)
                            ),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0x8C141C2E)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0x14FFFFFF), Color.Transparent)
                                    )
                                )
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Avatar Preview with Choose Photo & Remove Photo controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .border(
                                            2.5.dp,
                                            Brush.sweepGradient(listOf(NeonGreenBright, CyanAccent, NeonGreenBright)),
                                            CircleShape
                                        )
                                        .testTag("settings_avatar_preview"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (editPhotoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(editPhotoUrl)
                                                .crossfade(!isReducedMotion)
                                                .build(),
                                            contentDescription = "Profile Photo Preview",
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

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("settings_choose_photo_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PhotoCamera,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (editPhotoUrl.isNotBlank()) "Change Photo" else "Choose from Gallery",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    if (editPhotoUrl.isNotBlank()) {
                                        TextButton(
                                            onClick = {
                                                editPhotoUrl = ""
                                                Toast.makeText(context, "Photo removed. Tap 'Save Profile' to apply.", Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("settings_remove_photo_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = null,
                                                tint = CoralRed,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Remove Photo",
                                                color = CoralRed,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            // Feedback Message
                            AnimatedVisibility(visible = profileSaveSuccessMessage != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = NeonGreenBright.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreenBright.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, null, tint = NeonGreenBright, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = profileSaveSuccessMessage ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeonGreenBright
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(visible = profileErrorMessage != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CrimsonRed.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.ErrorOutline, null, tint = CrimsonRed, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = profileErrorMessage ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CrimsonRed
                                        )
                                    }
                                }
                            }

                            // Display Name
                            OutlinedTextField(
                                value = editDisplayName,
                                onValueChange = {
                                    editDisplayName = it
                                    profileSaveSuccessMessage = null
                                    profileErrorMessage = null
                                },
                                label = { Text("Display Name") },
                                leadingIcon = { Icon(Icons.Outlined.Badge, null, tint = TextSecondary) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_display_name_input"),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonGreenBright,
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                )
                            )

                            // Username handle
                            OutlinedTextField(
                                value = editUsername,
                                onValueChange = {
                                    editUsername = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' }
                                    profileSaveSuccessMessage = null
                                    profileErrorMessage = null
                                },
                                label = { Text("Username (@handle)") },
                                leadingIcon = {
                                    Text(
                                        "@",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreenBright,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_username_input"),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonGreenBright,
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                )
                            )

                            // Bio
                            OutlinedTextField(
                                value = editBio,
                                onValueChange = {
                                    editBio = it
                                    profileSaveSuccessMessage = null
                                    profileErrorMessage = null
                                },
                                label = { Text("Discipline Motto / Bio") },
                                leadingIcon = { Icon(Icons.Outlined.Notes, null, tint = TextSecondary) },
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_bio_input"),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonGreenBright,
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                )
                            )

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (!currentUser?.username.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = { onOpenPublicProfile(currentUser?.username ?: "") },
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanBright.copy(alpha = 0.5f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Outlined.Visibility, null, tint = CyanBright, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Preview Profile", color = CyanBright, fontSize = 12.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (editUsername.length < 3) {
                                            profileErrorMessage = "Username must be at least 3 characters."
                                            return@Button
                                        }
                                        if (editDisplayName.isBlank()) {
                                            profileErrorMessage = "Please enter a display name."
                                            return@Button
                                        }

                                        isSavingProfile = true
                                        profileErrorMessage = null
                                        profileSaveSuccessMessage = null

                                        viewModel.saveProfile(
                                            username = editUsername.trim(),
                                            displayName = editDisplayName.trim(),
                                            bio = editBio.trim(),
                                            photoUrl = editPhotoUrl.trim(),
                                            publicProfileEnabled = currentUser?.publicProfileEnabled ?: true,
                                            onSuccess = {
                                                isSavingProfile = false
                                                profileSaveSuccessMessage = "Profile updated successfully! ✨"
                                            },
                                            onError = { err ->
                                                isSavingProfile = false
                                                profileErrorMessage = err
                                            }
                                        )
                                    },
                                    enabled = !isSavingProfile,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonGreenBright,
                                        contentColor = Color(0xFF0D131F)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_save_profile_button")
                                ) {
                                    if (isSavingProfile) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color(0xFF0D131F),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save Profile", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 2. PRIVACY & VISIBILITY SECTION
                // ==========================================
                item {
                    Text(
                        text = "PRIVACY & VISIBILITY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                    )
                }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))
                                ),
                                RoundedCornerShape(20.dp)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x8C141C2E)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0x14FFFFFF), Color.Transparent)
                                    )
                                )
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Public Profile Switch Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                            imageVector = if (currentUser?.publicProfileEnabled == true)
                                                Icons.Outlined.Visibility
                                            else Icons.Outlined.VisibilityOff,
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
                                                text = "Public Athlete Profile",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (currentUser?.publicProfileEnabled == true)
                                                    NeonGreenBright.copy(alpha = 0.15f)
                                                else Color(0x26FFFFFF)
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
                                                "Guest Mode (Local Only). Sign in with a DayForge account to enable discovery."
                                            else if (currentUser?.publicProfileEnabled == true)
                                                "Discoverable by @${currentUser?.username}. Streaks, score & badges visible."
                                            else
                                                "Private. Completely hidden from public athlete search.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                val isGuestUser = currentUser?.uid?.startsWith("guest_") == true
                                Switch(
                                    checked = if (isGuestUser) false else (currentUser?.publicProfileEnabled ?: false),
                                    onCheckedChange = { enabled ->
                                        if (isGuestUser) {
                                            Toast.makeText(context, "Public discovery requires an authenticated DayForge account. Sign in to enable.", Toast.LENGTH_LONG).show()
                                        } else {
                                            viewModel.togglePublicProfile(enabled) { success, error ->
                                                if (!success && error != null) {
                                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF0D131F),
                                        checkedTrackColor = NeonGreenBright,
                                        uncheckedTrackColor = Color(0x33FFFFFF)
                                    ),
                                    modifier = Modifier.testTag("settings_public_profile_switch")
                                )
                            }

                            HorizontalDivider(color = Color(0x1FFFFFFF))

                            // Global Leaderboard Participation Toggle (Step 24)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (currentUser?.leaderboardEnabled == true)
                                                    NeonAmber.copy(alpha = 0.2f)
                                                else Color(0x26FFFFFF)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.EmojiEvents,
                                            contentDescription = null,
                                            tint = if (currentUser?.leaderboardEnabled == true) NeonAmber else TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Show me on Leaderboard",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (currentUser?.leaderboardEnabled == true)
                                                    NeonAmber.copy(alpha = 0.15f)
                                                else Color(0x26FFFFFF)
                                            ) {
                                                Text(
                                                    text = if (currentUser?.leaderboardEnabled == true) "ON" else "OFF",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (currentUser?.leaderboardEnabled == true) NeonAmber else TextMuted,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        val isGuest = currentUser?.uid?.startsWith("guest_") == true
                                        Text(
                                            text = if (isGuest)
                                                "Guest Mode. Sign in with a DayForge account to participate in global rankings."
                                            else if (currentUser?.leaderboardEnabled == true)
                                                "Ranked on Global Leaderboard using your Discipline Score & active streak."
                                            else
                                                "Hidden. You will not appear on the global leaderboard rankings.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                val isGuestUserLeaderboard = currentUser?.uid?.startsWith("guest_") == true
                                Switch(
                                    checked = if (isGuestUserLeaderboard) false else (currentUser?.leaderboardEnabled ?: false),
                                    onCheckedChange = { enabled ->
                                        if (isGuestUserLeaderboard) {
                                            Toast.makeText(context, "Leaderboard participation requires an authenticated DayForge account.", Toast.LENGTH_LONG).show()
                                        } else {
                                            viewModel.toggleLeaderboardParticipation(enabled) { success, error ->
                                                if (!success && error != null) {
                                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF0D131F),
                                        checkedTrackColor = NeonAmber,
                                        uncheckedTrackColor = Color(0x33FFFFFF)
                                    ),
                                    modifier = Modifier.testTag("settings_leaderboard_switch")
                                )
                            }

                            // Strict Privacy Guarantee Notice
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x330E1422),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanBright.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Shield,
                                        contentDescription = null,
                                        tint = CyanBright,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "DayForge Privacy Protection Guarantee",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Your private habit names, check-in history, daily water records, focus sessions, Night Reviews, account email, password, and private settings are strictly confidential and NEVER publicly exposed.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 3. APPEARANCE & MOTION SECTION
                // ==========================================
                item {
                    Text(
                        text = "APPEARANCE & MOTION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                    )
                }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))
                                ),
                                RoundedCornerShape(20.dp)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x8C141C2E)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0x14FFFFFF), Color.Transparent)
                                    )
                                )
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Reduced Motion Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                                if (isReducedMotion)
                                                    GoldYellow.copy(alpha = 0.2f)
                                                else Color(0x26FFFFFF)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.MotionPhotosOff,
                                            contentDescription = null,
                                            tint = if (isReducedMotion) GoldYellow else TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Reduced Motion",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isReducedMotion) GoldYellow.copy(alpha = 0.15f) else Color(0x26FFFFFF)
                                            ) {
                                                Text(
                                                    text = if (isReducedMotion) "ON" else "OFF",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isReducedMotion) GoldYellow else TextMuted,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (isReducedMotion)
                                                "Transitions minimized for instantaneous navigation and accessibility."
                                            else
                                                "Smooth cinematic animations and fluid spring gestures enabled.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Switch(
                                    checked = isReducedMotion,
                                    onCheckedChange = { enabled ->
                                        viewModel.toggleReducedMotion(enabled)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF0D131F),
                                        checkedTrackColor = GoldYellow,
                                        uncheckedTrackColor = Color(0x33FFFFFF)
                                    ),
                                    modifier = Modifier.testTag("settings_reduced_motion_switch")
                                )
                            }

                            HorizontalDivider(color = Color(0x1AFFFFFF))

                            // Cold Launch Animation Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                                "4-second DayForge construction animation on cold launch."
                                            else
                                                "Disabled. Opens directly to the Home dashboard.",
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
                                    modifier = Modifier.testTag("settings_launch_animation_switch")
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 4. REMINDERS & NOTIFICATION PREFERENCES
                // ==========================================
                item {
                    Text(
                        text = "REMINDERS & NOTIFICATION SCHEDULE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                    )
                }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onOpenReminders() }
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))
                                ),
                                RoundedCornerShape(20.dp)
                            )
                            .testTag("settings_reminders_link_card"),
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
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x3322C55E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        tint = NeonGreenBright,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Reminder Alarms & Alert Times",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Water interval alerts: ${if (reminderSettings.waterReminders.isEnabled) "ON" else "OFF"} • Night Review: ${if (reminderSettings.nightReview.isEnabled) "ON" else "OFF"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary)
                        }
                    }
                }

                // ==========================================
                // 4b. DEVICE PERMISSIONS & SYSTEM ACCESS (Step 20)
                // ==========================================
                item {
                    PermissionCenterSection()
                }

                // ==========================================
                // 5. BACKUP & DATA MANAGEMENT SECTION
                // ==========================================
                item {
                    BackupManagementSection(viewModel = viewModel)
                }

                // ==========================================
                // 6. ACCOUNT & SESSION SECTION
                // ==========================================
                item {
                    Text(
                        text = "ACCOUNT & SESSION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                    )
                }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))
                                ),
                                RoundedCornerShape(20.dp)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x8C141C2E)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0x14FFFFFF), Color.Transparent)
                                    )
                                )
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Account Email Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Outlined.Email, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                                    Text("Account Email", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                                Text(
                                    text = currentUser?.email?.ifBlank { "Guest Athlete" } ?: "Guest Athlete",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }

                            // Session Type Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Outlined.CloudSync, null, tint = CyanBright, modifier = Modifier.size(18.dp))
                                    Text("Storage Mode", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (currentUser?.uid?.startsWith("guest_") == true) Color(0x26FFFFFF) else NeonGreenBright.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (currentUser?.uid?.startsWith("guest_") == true) "Local Guest" else "Firebase Realtime DB",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentUser?.uid?.startsWith("guest_") == true) TextSecondary else NeonGreenBright,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0x1AFFFFFF))

                            // Safe Local Cache Clear Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "Clear Temporary Cache",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Safely frees memory without deleting any habits, logs, or user account data.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            context.cacheDir.deleteRecursively()
                                            Toast.makeText(context, "Temporary cache cleared safely! ✨", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cache already optimized.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                                    modifier = Modifier.testTag("settings_clear_cache_button")
                                ) {
                                    Icon(Icons.Outlined.CleaningServices, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clear", color = TextPrimary, fontSize = 12.sp)
                                }
                            }

                            HorizontalDivider(color = Color(0x1AFFFFFF))

                            // Logout Button
                            Button(
                                onClick = { showLogoutConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CoralRed.copy(alpha = 0.15f),
                                    contentColor = CoralRed
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_logout_button")
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Logout, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Log Out from Account", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    WatermarkFooter()
                }
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text("Confirm Logout", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to sign out? Your cloud data remains safely saved in your DayForge profile.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF141C2E),
            shape = RoundedCornerShape(20.dp)
        )
    }
}
