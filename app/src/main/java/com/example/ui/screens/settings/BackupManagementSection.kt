package com.example.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.backup.BackupRestoreReport
import com.example.data.backup.BackupSummary
import com.example.ui.components.tactilePress
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisciplineViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun BackupManagementSection(
    viewModel: DisciplineViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isReducedMotion by viewModel.isReducedMotionEnabled.collectAsState()
    val isExporting by viewModel.isExportingBackup.collectAsState()
    val isImporting by viewModel.isImportingBackup.collectAsState()
    val pendingSummary by viewModel.pendingBackupSummary.collectAsState()
    val operationMessage by viewModel.backupOperationMessage.collectAsState()
    val errorMessage by viewModel.backupErrorMessage.collectAsState()

    val habits by viewModel.allHabits.collectAsState()
    val completions by viewModel.allCompletions.collectAsState()
    val waterRecords by viewModel.waterHistory.collectAsState()
    val focusSessions by viewModel.allFocusSessions.collectAsState()

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var autoCreateSafetySnapshot by remember { mutableStateOf(true) }
    var restoreSuccessReport by remember { mutableStateOf<BackupRestoreReport?>(null) }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var showArchitectureDetails by remember { mutableStateOf(false) }

    // Launcher to create document (Export JSON to user chosen folder/downloads)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { destinationUri: Uri? ->
        val json = pendingExportJson
        if (destinationUri != null && json != null) {
            viewModel.saveExportToUri(
                json = json,
                uri = destinationUri,
                onSuccess = {
                    Toast.makeText(context, "DayForge backup exported successfully! 📁", Toast.LENGTH_LONG).show()
                    pendingExportJson = null
                },
                onError = { err ->
                    Toast.makeText(context, "Export error: $err", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // Launcher to select document (Import JSON from device)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { sourceUri: Uri? ->
        if (sourceUri != null) {
            viewModel.validateSelectedBackupFile(
                uri = sourceUri,
                onValidated = { summary ->
                    showRestoreConfirmDialog = true
                },
                onError = { err ->
                    Toast.makeText(context, "Validation Error: $err", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Text(
            text = "BACKUP & DATA MANAGEMENT",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = TextSecondary,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
        )

        // Loading / Status Banner if operating
        AnimatedVisibility(
            visible = (isExporting || isImporting || operationMessage != null),
            enter = if (isReducedMotion) EnterTransition.None else fadeIn() + expandVertically(),
            exit = if (isReducedMotion) ExitTransition.None else fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CyanBright.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = Color(0x3306B6D4)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = CyanBright
                    )
                    Text(
                        text = operationMessage ?: "Processing backup data...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Error message banner if any
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = if (isReducedMotion) EnterTransition.None else fadeIn() + expandVertically(),
            exit = if (isReducedMotion) ExitTransition.None else fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CoralRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = CoralRed.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.WarningAmber, null, tint = CoralRed, modifier = Modifier.size(22.dp))
                    Text(
                        text = errorMessage ?: "Backup error encountered.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralRed,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearPendingBackup() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = CoralRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ==========================================
        // 1. EXPORT BACKUP GLASS CARD
        // ==========================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))),
                    RoundedCornerShape(20.dp)
                )
                .testTag("backup_export_card"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0x8C141C2E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0x14FFFFFF), Color.Transparent)))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyanBright.copy(alpha = 0.18f))
                            .border(1.dp, CyanBright.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            tint = CyanBright,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Complete Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Create a safe, structured JSON archive of all your DayForge data",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Included scope list
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x330B111E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Included in Export:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanBright
                        )
                        Text(
                            text = "• Habits & Check-in History (${habits.size} habits, ${completions.size} check-ins)\n" +
                                    "• Hydration Logs & Goals (${waterRecords.size} daily logs)\n" +
                                    "• Focus Sessions (${focusSessions.size} logged sessions)\n" +
                                    "• Evening Night Reviews & Discipline Score\n" +
                                    "• Reminder Settings & Smart Notifications\n" +
                                    "• Appearance & Motion Preferences",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Action Buttons Row: Download (.json) + Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.prepareExportJson(
                                onReady = { json ->
                                    pendingExportJson = json
                                    val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                    val fileName = "DayForge_Backup_$dateStr.json"
                                    createDocumentLauncher.launch(fileName)
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanBright,
                            contentColor = Color(0xFF0B111E)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .tactilePress()
                            .testTag("backup_export_button"),
                        enabled = !isExporting
                    ) {
                        Icon(Icons.Outlined.FileDownload, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isExporting) "Exporting..." else "Save to File", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.prepareExportJson(
                                onReady = { json ->
                                    try {
                                        val tempFile = File(context.cacheDir, "DayForge_Backup_${System.currentTimeMillis()}.json")
                                        tempFile.writeText(json, Charsets.UTF_8)
                                        val contentUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            tempFile
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, contentUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share DayForge Backup"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Direct share: saved to documents recommended.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40FFFFFF)),
                        modifier = Modifier.tactilePress(),
                        enabled = !isExporting
                    ) {
                        Icon(Icons.Outlined.Share, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // ==========================================
        // 2. IMPORT / RESTORE BACKUP GLASS CARD
        // ==========================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))),
                    RoundedCornerShape(20.dp)
                )
                .testTag("backup_import_card"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0x8C141C2E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0x14FFFFFF), Color.Transparent)))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NeonGreenBright.copy(alpha = 0.18f))
                            .border(1.dp, NeonGreenBright.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudUpload,
                            contentDescription = null,
                            tint = NeonGreenBright,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Import & Restore Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Restore DayForge records from a validated JSON backup",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x330B111E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldYellow.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.Shield, null, tint = GoldYellow, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Safety Guarantee: Backups are strictly verified before changing data. A local safety snapshot is automatically created before any restore operation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreenBright.copy(alpha = 0.2f),
                        contentColor = NeonGreenBright
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreenBright.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tactilePress()
                        .testTag("backup_import_button"),
                    enabled = !isImporting
                ) {
                    Icon(Icons.Outlined.FileUpload, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isImporting) "Validating..." else "Choose Backup File (.json)", fontWeight = FontWeight.Bold)
                }
            }
        }

        // ==========================================
        // 3. SECURITY & PRIVACY SPECIFICATION CARD
        // ==========================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color(0x26FFFFFF), Color(0x0DFFFFFF))),
                    RoundedCornerShape(20.dp)
                )
                .clickable { showArchitectureDetails = !showArchitectureDetails },
            shape = RoundedCornerShape(20.dp),
            color = Color(0x66141C2E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.Security, null, tint = CyanBright, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Backup Security & Integrity",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Icon(
                        imageVector = if (showArchitectureDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }

                if (showArchitectureDetails) {
                    HorizontalDivider(color = Color(0x1AFFFFFF))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = NeonGreenBright, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Zero Credential Storage: Passwords, tokens, and Firebase internal secrets are strictly excluded from backups.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = NeonGreenBright, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Cross-Account Isolation: When restoring, all records are re-scoped to your currently authenticated user ID.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = NeonGreenBright, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Bi-directional Sync: Restored records are written to local Room database and mirrored to Firebase Realtime Database.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // RESTORE CONFIRMATION & PREVIEW DIALOG
    // ==========================================
    val summary = pendingSummary
    if (showRestoreConfirmDialog && summary != null) {
        val exportTimeStr = try {
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(summary.exportedAt))
        } catch (_: Exception) {
            "Previous export"
        }

        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                viewModel.clearPendingBackup()
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.Restore, null, tint = CyanBright, modifier = Modifier.size(24.dp))
                    Text("Confirm Data Restoration", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "A valid DayForge backup ($exportTimeStr) was successfully read. Please review the items to be restored:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // Categories Grid / Cards
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x330B111E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Habits Included:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("${summary.habitsCount} habits", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Check-in Records:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("${summary.completionsCount} records", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Hydration Logs:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("${summary.waterRecordsCount} days", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Focus Sessions:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("${summary.focusSessionsCount} sessions", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Night Reviews:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("${summary.nightReviewsCount} reviews", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("System Notifications:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("${summary.notificationsCount} alerts", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }

                    // Warning notice
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CoralRed.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ Warning: Restoration will replace existing habits and logs with the backup contents. Account credentials will remain unchanged.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CoralRed,
                            modifier = Modifier.padding(10.dp),
                            lineHeight = 16.sp
                        )
                    }

                    // Auto Safety Snapshot Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = autoCreateSafetySnapshot,
                            onCheckedChange = { autoCreateSafetySnapshot = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CyanBright,
                                checkmarkColor = Color(0xFF0B111E)
                            )
                        )
                        Text(
                            text = "Save automatic safety snapshot of current data before restore",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        viewModel.executeRestoreBackup(
                            summary = summary,
                            autoSafetySnapshot = autoCreateSafetySnapshot,
                            onSuccess = { report ->
                                restoreSuccessReport = report
                            },
                            onError = { err ->
                                Toast.makeText(context, "Restore error: $err", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Color(0xFF0B111E)),
                    modifier = Modifier.testTag("backup_confirm_restore_button")
                ) {
                    Text("Confirm & Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirmDialog = false
                    viewModel.clearPendingBackup()
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF141C2E),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ==========================================
    // RESTORE SUCCESS REPORT DIALOG
    // ==========================================
    val report = restoreSuccessReport
    if (report != null) {
        AlertDialog(
            onDismissRequest = { restoreSuccessReport = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = NeonGreenBright, modifier = Modifier.size(24.dp))
                    Text("Restoration Complete! 🎉", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Your DayForge records were successfully restored and synchronized with your active account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x330B111E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreenBright.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("• Habits Restored: ${report.habitsRestored}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                            Text("• Check-ins Restored: ${report.completionsRestored}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                            Text("• Hydration Records Restored: ${report.waterRecordsRestored}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                            Text("• Focus Sessions Restored: ${report.focusSessionsRestored}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                            Text("• Night Reviews Restored: ${report.nightReviewsRestored}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                            if (report.safetySnapshotPath != null) {
                                Text("• Safety Snapshot: Saved to device", color = NeonGreenBright, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { restoreSuccessReport = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreenBright, contentColor = Color(0xFF0B111E)),
                    modifier = Modifier.testTag("backup_restore_done_button")
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF141C2E),
            shape = RoundedCornerShape(20.dp)
        )
    }
}
