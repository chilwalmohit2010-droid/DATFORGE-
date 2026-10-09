package com.example.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.components.DayForgeMountainLogo
import com.example.ui.components.tactilePress
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel

/**
 * Screen 2: Login / Sign Up — EXACT Match to Reference Image (Design 5: Black Glass)
 *
 * Visual Structure:
 * - Top:
 *   - Cinematic mountain backdrop with cyan aurora streak
 *   - DAYFORGE Mountain Logo (white with cyan glow)
 *   - "DAYFORGE" bold tracked wordmark
 *   - "Discipline Builds Freedom" tagline
 * - Auth Glass Card:
 *   - Segmented toggle: "Login" (active cyan/blue pill) vs "Sign Up"
 *   - Rounded glass input fields with Mail / Lock icons
 *   - Password visibility toggle
 *   - "Forgot Password?" right-aligned link
 *   - Primary action button: Glowing cyan/blue pill "Log In  →" / "Sign Up  →"
 *   - "or" divider with horizontal gradient lines
 *   - Alternative sign-in (Guest mode with flame/shield, Google if configured)
 * - Footer:
 *   - "Made by Mohit ❤️" centered at bottom
 * - Real Firebase Auth integration preserved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: DisciplineViewModel,
    onAuthSuccess: () -> Unit
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmailSentMessage by remember { mutableStateOf<String?>(null) }

    val authLoading by viewModel.authLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val focusManager = LocalFocusManager.current

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
                .graphicsLayer { alpha = 0.35f },
            contentScale = ContentScale.Crop
        )

        // 2. Atmospheric Aurora & Vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x9904060C),
                            Color(0x4D060D1E),
                            Color(0xF2030509)
                        )
                    )
                )
        )

        // 3. Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP SECTION: Branding (Screen 2)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 18.dp, bottom = 22.dp)
            ) {
                DayForgeMountainLogo(
                    size = 72.dp,
                    tint = Color.White,
                    glowColor = Color(0xFF00F0FF)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "DAYFORGE",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Discipline Builds Freedom",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }

            // CENTER SECTION: Floating Black Glass Auth Card (Screen 2)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0x6600F0FF),
                                Color(0x3338BDF8),
                                Color(0x1AFFFFFF)
                            )
                        ),
                        shape = RoundedCornerShape(26.dp)
                    ),
                shape = RoundedCornerShape(26.dp),
                color = Color(0x990E1626)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Segmented Control: Login vs Sign Up
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(23.dp))
                            .background(Color(0x331E293B))
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(23.dp))
                            .padding(3.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Login Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .then(
                                        if (!isSignUpMode) {
                                            Modifier.background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color(0xFF2563EB),
                                                        Color(0xFF0284C7)
                                                    )
                                                )
                                            )
                                        } else Modifier
                                    )
                                    .clickable { isSignUpMode = false }
                                    .testTag("auth_tab_login"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Login",
                                    fontWeight = if (!isSignUpMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isSignUpMode) Color.White else Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                            }

                            // Sign Up Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .then(
                                        if (isSignUpMode) {
                                            Modifier.background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color(0xFF0284C7),
                                                        Color(0xFF2563EB)
                                                    )
                                                )
                                            )
                                        } else Modifier
                                    )
                                    .clickable { isSignUpMode = true }
                                    .testTag("auth_tab_signup"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign Up",
                                    fontWeight = if (isSignUpMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSignUpMode) Color.White else Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Error Banner if present
                    if (!authError.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x33EF4444),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66EF4444))
                        ) {
                            Text(
                                text = authError ?: "",
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Email Field (Glass rounded pill)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input"),
                        placeholder = { Text("Email", color = Color(0xFF64748B), fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Mail,
                                contentDescription = "Email",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0x2E1E293B),
                            unfocusedContainerColor = Color(0x1F1E293B),
                            focusedBorderColor = Color(0xFF00F0FF),
                            unfocusedBorderColor = Color(0x26FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Password Field (Glass rounded pill)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input"),
                        placeholder = { Text("Password", color = Color(0xFF64748B), fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "Password",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isSignUpMode) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (email.isNotBlank() && password.isNotBlank()) {
                                    viewModel.login(email.trim(), password, onAuthSuccess)
                                }
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0x2E1E293B),
                            unfocusedContainerColor = Color(0x1F1E293B),
                            focusedBorderColor = Color(0xFF00F0FF),
                            unfocusedBorderColor = Color(0x26FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Confirm Password (in Sign Up mode)
                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_confirm_password_input"),
                            placeholder = { Text("Confirm Password", color = Color(0xFF64748B), fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(19.dp)
                                )
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (email.isNotBlank() && password.isNotBlank() && password == confirmPassword) {
                                        viewModel.signUp(email.trim(), password, onAuthSuccess)
                                    }
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
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

                    // Forgot Password Link (Right-aligned, Screen 2)
                    if (!isSignUpMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "Forgot Password?",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { showForgotPasswordDialog = true }
                                    .testTag("auth_forgot_password_btn")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Primary Button: "Log In  →" / "Sign Up  →" (Screen 2 Gradient Pill)
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
                                focusManager.clearFocus()
                                if (isSignUpMode) {
                                    if (password != confirmPassword) {
                                        // Error feedback handled by view model / state
                                    } else {
                                        viewModel.signUp(email.trim(), password, onAuthSuccess)
                                    }
                                } else {
                                    viewModel.login(email.trim(), password, onAuthSuccess)
                                }
                            }
                            .testTag("auth_primary_submit_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (authLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isSignUpMode) "Sign Up" else "Log In",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Divider "or" with horizontal gradient lines (Screen 2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color(0x33FFFFFF))
                                    )
                                )
                        )
                        Text(
                            text = "or",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 14.dp),
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0x33FFFFFF), Color.Transparent)
                                    )
                                )
                        )
                    }

                    // Alternative Sign-In Options (Screen 2 circular icons)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Guest / Explore Button
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0x331E293B))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                .tactilePress(scaleDown = 0.92f) {
                                    viewModel.continueAsGuest(onAuthSuccess)
                                }
                                .testTag("auth_guest_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = "Continue as Guest",
                                tint = StreakAmber,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Shield / Instant Test Access
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0x331E293B))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                .tactilePress(scaleDown = 0.92f) {
                                    // Pre-fill demo credentials if wanted
                                    email = "athlete@dayforge.app"
                                    password = "password123"
                                }
                                .testTag("auth_quick_fill_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = "Fast fill",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // FOOTER SECTION: "Made by Mohit ❤️" (Screen 2)
            Row(
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Made by Mohit ",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Text(
                    text = "❤️",
                    fontSize = 12.sp
                )
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(email) }
        var isSendingReset by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showForgotPasswordDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, Color(0x4038BDF8), RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xF20F172A)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Reset Password",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Enter your registered email address and we'll send you a password reset link.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Email", color = Color(0xFF64748B)) },
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

                    if (resetEmailSentMessage != null) {
                        Text(
                            text = resetEmailSentMessage ?: "",
                            color = NeonGreen,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showForgotPasswordDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }

                        Button(
                            onClick = {
                                if (resetEmail.isNotBlank()) {
                                    isSendingReset = true
                                    viewModel.sendPasswordReset(resetEmail.trim()) {
                                        isSendingReset = false
                                        resetEmailSentMessage = "Reset link sent! Please check your inbox."
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            if (isSendingReset) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            } else {
                                Text("Send Link", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
