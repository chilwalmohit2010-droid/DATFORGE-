package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * Reusable DAYFORGE Permission System.
 * Manages checking, rationale, and contextual settings dispatch
 * for all system capabilities (Step 20).
 */
object PermissionManager {

    enum class PermissionItem(
        val title: String,
        val subtitle: String,
        val explanation: String,
        val grantButtonText: String
    ) {
        NOTIFICATIONS(
            title = "Notifications",
            subtitle = "Habit alerts, water reminders & Focus updates",
            explanation = "Notifications allow DAYFORGE to show your scheduled reminders, morning wake-up alarms, and deep work Focus updates in your status bar.",
            grantButtonText = "Allow Notifications"
        ),
        EXACT_ALARM(
            title = "Exact Alarm",
            subtitle = "Precise wake-up & interval timing",
            explanation = "Exact alarm scheduling ensures your morning Wake-Up Challenge and hourly water reminders trigger at the exact second, even when the device is asleep.",
            grantButtonText = "Allow Alarm Access"
        ),
        FULL_SCREEN_ALARM(
            title = "Full-Screen Alarm",
            subtitle = "Wake screen & interactive challenges",
            explanation = "Full-screen alarm access allows DAYFORGE to display your interactive morning challenges over the lock screen so you never sleep through an alarm.",
            grantButtonText = "Allow Full-Screen Alarms"
        ),
        CAMERA(
            title = "Camera",
            subtitle = "Repetition pose detection",
            explanation = "Camera access is used only for Wake-Up Challenge repetition detection (push-ups, squats, jumping jacks). All processing happens 100% locally on your device.",
            grantButtonText = "Allow Camera"
        ),
        FOCUS_USAGE_ACCESS(
            title = "Focus / Usage Access",
            subtitle = "Distraction app blocking during Focus",
            explanation = "Focus Access helps DAYFORGE detect selected app usage while a Focus session is active so it can protect your deep work time from distractions.",
            grantButtonText = "Grant Focus Access"
        ),
        BATTERY_OPTIMIZATION(
            title = "Battery Optimization",
            subtitle = "Unrestricted background execution",
            explanation = "Allowing DAYFORGE to run without aggressive OS battery killing ensures background focus timers, water reminders, and morning wake-up alarms are never delayed.",
            grantButtonText = "Allow Background Activity"
        )
    }

    // --- Permission Checks ---

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    fun hasFullScreenIntentPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 34) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                notificationManager?.canUseFullScreenIntent() ?: true
            } catch (e: Throwable) {
                true
            }
        } else {
            true
        }
    }

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasUsageAccessPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } catch (e: Exception) {
            false
        }
    }

    // --- Intent Navigation to Settings ---

    fun openNotificationSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                openAppSettingsIntent(context)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openAppSettings(context)
        }
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                openAppSettings(context)
            }
        } else {
            openAppSettings(context)
        }
    }

    fun openFullScreenIntentSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                val intent = Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT").apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                openAppSettings(context)
            }
        } else {
            openAppSettings(context)
        }
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
            } catch (ex: Exception) {
                Toast.makeText(context, "Please open Settings > Usage Access manually", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
            } catch (ex: Exception) {
                openAppSettings(context)
            }
        }
    }

    fun openAppSettings(context: Context) {
        try {
            val intent = openAppSettingsIntent(context)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open system settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
