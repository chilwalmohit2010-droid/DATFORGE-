package com.example.ui.screens.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.components.tactilePress
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel
import com.example.util.ProfileImageUtils

/**
 * Screen 3: Profile Setup — EXACT Match to Reference Image (Design 5: Black Glass)
 *
 * Visual Structure:
 * - Top Bar: Back arrow button `←`, "Profile Setup" centered title
 * - Center Avatar: Large circular picture with glowing cyan border & camera icon badge `📷`
 * - Black Glass Card:
 *   - "Display Name" field
 *   - "@ Username" field
 *   - "Bio" field with "34/120" counter
 * - Theme/Accent Selection: 6 vibrant glowing color circles
 * - Action: Large glowing blue/cyan pill "Continue" button
 */
@Composable
fun ProfileSetupScreen(
    viewModel: DisciplineViewModel,
    onCompleted: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val authLoading by viewModel.authLoading.collectAsState()
    val context = LocalContext.current

    var username by remember(currentUser) {
        mutableStateOf(currentUser?.username?.ifBlank { "mohit" } ?: "mohit")
    }
    var displayName by remember(currentUser) {
        mutableStateOf(currentUser?.displayName?.ifBlank { "Mohit" } ?: "Mohit")
    }
    var bio by remember(currentUser) {
        mutableStateOf(currentUser?.bio?.ifBlank { "Discipline today, Freedom tomorrow." } ?: "Discipline today, Freedom tomorrow.")
    }
    var photoUrl by remember(currentUser) { mutableStateOf(currentUser?.photoUrl ?: "") }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val accentColors = listOf(
        Color(0xFF00F0FF), // Electric Cyan
        Color(0xFF2563EB), // Royal Blue
        Color(0xFF22C55E), // Emerald Green
        Color(0xFFF97316), // Sunset Orange
        Color(0xFFA855F7), // Neon Purple
        Color(0xFFEC4899)  // Magenta Pink
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val uid = currentUser?.uid ?: "user_${System.currentTimeMillis()}"
            val processed = ProfileImageUtils.processAndSaveProfileImage(context, uri, uid)
            if (processed != null) {
                photoUrl = processed
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF04060C))
    ) {
        // 1. Cinematic Mountain Background
        Image(
            painter = painterResource(id = R.drawable.img_cinematic_mountain_bg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.32f },
            contentScale = ContentScale.Crop
        )

        // 2. Atmospheric Dark Vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x9904060C),
                            Color(0x59080E1C),
                            Color(0xF2030509)
                        )
                    )
                )
        )

        // 3. Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // TOP BAR: Back button and "Profile Setup" (Screen 3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                IconButton(
                    onClick = onCompleted,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x261E293B))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .testTag("profile_setup_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Profile Setup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 17.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // CENTER: Large Circular Profile Picture + Camera Badge (Screen 3)
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(126.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Cyan Aura
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0x4000F0FF), Color.Transparent)
                            )
                        )
                )

                // Avatar Container
                Box(
                    modifier = Modifier
                        .size(114.dp)
                        .clip(CircleShape)
                        .border(2.dp, Brush.sweepGradient(listOf(Color(0xFF00F0FF), Color(0xFF2563EB), Color(0xFF00F0FF))), CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.avatar_discipline_default),
                            contentDescription = "Default avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Camera Icon Pill Badge (Screen 3 bottom right)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .border(1.5.dp, Color(0xFF00F0FF), CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Change photo",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Error banner if any
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x33EF4444),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66EF4444))
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // BLACK GLASS FORM CARD (Screen 3)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x4D00F0FF), Color(0x2038BDF8), Color(0x14FFFFFF))
                        ),
                        RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = Color(0x990E1626)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Field 1: Display Name
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Display Name",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp
                        )
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_setup_name_input"),
                            placeholder = { Text("Your Name", color = Color(0xFF64748B)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0x2E1E293B),
                                unfocusedContainerColor = Color(0x1F1E293B),
                                focusedBorderColor = Color(0xFF00F0FF),
                                unfocusedBorderColor = Color(0x26FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    // Field 2: @Username
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "@ Username",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp
                        )
                        OutlinedTextField(
                            value = if (username.startsWith("@")) username else "@$username",
                            onValueChange = {
                                val clean = it.removePrefix("@").trim().lowercase()
                                username = clean
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_setup_username_input"),
                            placeholder = { Text("@handle", color = Color(0xFF64748B)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0x2E1E293B),
                                unfocusedContainerColor = Color(0x1F1E293B),
                                focusedBorderColor = Color(0xFF00F0FF),
                                unfocusedBorderColor = Color(0x26FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    // Field 3: Bio with 34/120 character counter
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Bio",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp
                        )
                        OutlinedTextField(
                            value = bio,
                            onValueChange = {
                                if (it.length <= 120) bio = it
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(94.dp)
                                .testTag("profile_setup_bio_input"),
                            placeholder = { Text("What fuels your discipline?", color = Color(0xFF64748B)) },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0x2E1E293B),
                                unfocusedContainerColor = Color(0x1F1E293B),
                                focusedBorderColor = Color(0xFF00F0FF),
                                unfocusedBorderColor = Color(0x26FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "${bio.length}/120",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // THEME ACCENT COLOR SELECTION CIRCLES (Screen 3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                accentColors.forEachIndexed { index, color ->
                    val isSelected = index == selectedColorIndex
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color.White else Color(0x33FFFFFF),
                                shape = CircleShape
                            )
                            .clickable { selectedColorIndex = index },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // LARGE "Continue" BUTTON (Screen 3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF0284C7),
                                Color(0xFF2563EB),
                                Color(0xFF00F0FF)
                            )
                        )
                    )
                    .tactilePress(scaleDown = 0.965f) {
                        val cleanUsername = username.removePrefix("@").trim()
                        if (cleanUsername.isBlank()) {
                            errorMessage = "Please enter a valid username."
                            return@tactilePress
                        }
                        errorMessage = null
                        viewModel.saveProfile(
                            username = cleanUsername,
                            displayName = displayName.trim().ifBlank { "Athlete" },
                            bio = bio.trim(),
                            photoUrl = photoUrl,
                            publicProfileEnabled = true,
                            onSuccess = { onCompleted() },
                            onError = { err -> errorMessage = err }
                        )
                    }
                    .testTag("profile_setup_continue_btn"),
                contentAlignment = Alignment.Center
            ) {
                if (authLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
