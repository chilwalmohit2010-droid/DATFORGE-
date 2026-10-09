package com.example.ui.screens.reminders

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.*
import com.example.ui.components.WatermarkFooter
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun NightReviewDialog(
    habits: List<Habit>,
    todayCompletions: List<HabitCompletion>,
    todayWater: WaterRecord?,
    initialEntry: NightReviewEntry? = null,
    onDismiss: () -> Unit,
    onSaveReview: (NightReviewEntry) -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val formattedDate = remember {
        today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
    }
    val dateIso = remember {
        today.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    var score by remember { mutableStateOf(initialEntry?.score ?: 5) }
    var wins by remember { mutableStateOf(initialEntry?.wins ?: "") }
    var struggles by remember { mutableStateOf(initialEntry?.struggles ?: "") }
    var tomorrowPriority by remember { mutableStateOf(initialEntry?.tomorrowPriority ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    val activeHabits = habits.filter { !it.isPaused }
    val completedCount = todayCompletions.count { it.status == CompletionStatus.COMPLETED }
    val totalCount = activeHabits.size

    val waterCurrent = todayWater?.currentMl ?: 0
    val waterTarget = todayWater?.targetMl ?: 2500

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB3030712))
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x59A855F7), Color(0x1F38BDF8), Color(0x10FFFFFF))
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .animateContentSize(),
                color = Color(0xF20F172A),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x33A855F7))
                                    .border(1.dp, Color(0x66A855F7), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bedtime,
                                    contentDescription = null,
                                    tint = NeonPurple,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Night Review & Reflection",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x1AFFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Daily Snapshot Summary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x221E293B))
                            .border(1.dp, Color(0x3364748B), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Habits stat
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.CheckCircle, null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "$completedCount / $totalCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "Habits Done",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp)
                                .background(Color(0x3364748B))
                        )

                        // Water stat
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.WaterDrop, null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "${waterCurrent}ml",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "Goal: ${waterTarget}ml",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Star Rating Section
                    Text(
                        text = "How disciplined was your day?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            IconButton(
                                onClick = { score = i },
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (i <= score) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Star $i",
                                    tint = if (i <= score) AmberRating else TextMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    val scoreLabel = when (score) {
                        1 -> "Rough day • Ready to reset and rebuild tomorrow"
                        2 -> "Challenging • Small wins kept the line moving"
                        3 -> "Solid effort • Steady discipline maintained"
                        4 -> "High output • Strong focus and execution"
                        5 -> "Flawless discipline • Total victory today! 🏆"
                        else -> ""
                    }
                    Text(
                        text = scoreLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberRating,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Reflection 1: Today's Wins
                    Text(
                        text = "1. Today's Wins & Victories",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = wins,
                        onValueChange = { wins = it },
                        placeholder = { Text("e.g. Woke up early, drank 2.5L water, finished coding task...", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = Color(0x3364748B),
                            focusedContainerColor = Color(0x1A1E293B),
                            unfocusedContainerColor = Color(0x101E293B)
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reflection 2: Struggles / Friction
                    Text(
                        text = "2. Struggles or Lessons Learned",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CoralRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = struggles,
                        onValueChange = { struggles = it },
                        placeholder = { Text("e.g. Procrastinated after lunch, need to protect afternoon focus block...", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CoralRed,
                            unfocusedBorderColor = Color(0x3364748B),
                            focusedContainerColor = Color(0x1A1E293B),
                            unfocusedContainerColor = Color(0x101E293B)
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reflection 3: Tomorrow's Top Priority
                    Text(
                        text = "3. Tomorrow's #1 Priority",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tomorrowPriority,
                        onValueChange = { tomorrowPriority = it },
                        placeholder = { Text("e.g. Complete high-priority client module before 11 AM", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = Color(0x3364748B),
                            focusedContainerColor = Color(0x1A1E293B),
                            unfocusedContainerColor = Color(0x101E293B)
                        )
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Save Button
                    Button(
                        onClick = {
                            isSaving = true
                            val entry = NightReviewEntry(
                                date = dateIso,
                                score = score,
                                wins = wins.trim(),
                                struggles = struggles.trim(),
                                tomorrowPriority = tomorrowPriority.trim(),
                                timestamp = System.currentTimeMillis()
                            )
                            onSaveReview(entry)
                            Toast.makeText(context, "Evening reflection saved to cloud! 🌙", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_night_review_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPurple
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Night Review",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    WatermarkFooter()
                }
            }
        }
    }
}
