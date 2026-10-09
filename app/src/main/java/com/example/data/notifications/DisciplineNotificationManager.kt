package com.example.data.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.models.Habit
import com.example.data.models.NightReviewReminderSettings
import com.example.data.models.ReminderType
import com.example.data.models.WaterReminderSettings
import com.example.data.receiver.HabitReminderReceiver
import com.example.data.receiver.NightReviewReceiver
import com.example.data.receiver.WaterReminderReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DisciplineNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_WATER = "water_reminders_channel"
        const val CHANNEL_HABITS = "habit_reminders_channel"
        const val CHANNEL_NIGHT_REVIEW = "night_review_channel"

        const val ID_WATER_SCHEDULED = 1001
        const val ID_WATER_TEST = 1002
        const val ID_NIGHT_REVIEW = 2001
        const val ID_NIGHT_REVIEW_TEST = 2002
        const val ID_HABIT_BASE = 3000

        private const val REQUEST_CODE_WATER = 1101
        private const val REQUEST_CODE_NIGHT_REVIEW = 1102
        private const val REQUEST_CODE_HABIT_BASE = 5000

        private const val TAG = "DisciplineNotifManager"

        fun parseTimeToMillis(timeStr: String): Long {
            return try {
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                val targetTime = LocalTime.parse(timeStr.trim(), formatter)
                val now = LocalDateTime.now()
                var targetDateTime = now.withHour(targetTime.hour).withMinute(targetTime.minute).withSecond(0).withNano(0)
                if (targetDateTime.isBefore(now)) {
                    targetDateTime = targetDateTime.plusDays(1)
                }
                targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis() + 60 * 60 * 1000L
            }
        }
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // 1. Water Channel
            val waterChannel = NotificationChannel(
                CHANNEL_WATER,
                "Hydration Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle alerts to stay hydrated throughout your active hours."
                enableVibration(true)
            }

            // 2. Habits Channel
            val habitsChannel = NotificationChannel(
                CHANNEL_HABITS,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled discipline alerts for daily habits."
                enableVibration(true)
            }

            // 3. Night Review Channel
            val nightChannel = NotificationChannel(
                CHANNEL_NIGHT_REVIEW,
                "Night Review & Reflection",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Evening reflection prompt to evaluate your day and plan tomorrow."
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(waterChannel)
            notificationManager.createNotificationChannel(habitsChannel)
            notificationManager.createNotificationChannel(nightChannel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // --- 1. HABIT REMINDERS ---

    fun scheduleHabitReminder(habit: Habit) {
        val reminderTime = habit.reminderTime?.trim()
        if (reminderTime.isNullOrBlank() || habit.isPaused) {
            cancelHabitReminder(habit.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = REQUEST_CODE_HABIT_BASE + (habit.id.hashCode() and 0x7FFF)

        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("habitId", habit.id)
            putExtra("habitTitle", habit.title)
            putExtra("reminderTime", reminderTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = parseTimeToMillis(reminderTime)
        val repeatInterval = AlarmManager.INTERVAL_DAY

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                repeatInterval,
                pendingIntent
            )
            Log.d(TAG, "Scheduled habit reminder for '${habit.title}' at $reminderTime (req $requestCode)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule habit reminder: ${e.message}")
        }
    }

    fun cancelHabitReminder(habitId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = REQUEST_CODE_HABIT_BASE + (habitId.hashCode() and 0x7FFF)
        val intent = Intent(context, HabitReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled habit reminder for $habitId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel habit reminder: ${e.message}")
        }
    }

    fun sendHabitNotification(habitId: String, habitTitle: String, isTest: Boolean = false) {
        if (!hasNotificationPermission()) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "habits")
            putExtra("habitId", habitId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode() and 0x7FFF,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifId = if (isTest) ID_HABIT_BASE else ID_HABIT_BASE + (habitId.hashCode() and 0x7FFF)
        val titleText = if (isTest) "Habit Alert Test ⚡" else "Habit Reminder ⚡"
        val bodyText = if (isTest) "Time for: $habitTitle! Tap to check in and protect your streak." else "Time to focus on: $habitTitle. Keep your streak alive today!"

        val notification = NotificationCompat.Builder(context, CHANNEL_HABITS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(notifId, notification)
    }

    // --- 2. WATER REMINDERS ---

    fun scheduleWaterReminders(settings: WaterReminderSettings) {
        if (!settings.isEnabled) {
            cancelWaterReminders()
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            putExtra("startTime", settings.startTime)
            putExtra("endTime", settings.endTime)
            putExtra("intervalMinutes", settings.intervalMinutes)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WATER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalMillis = (settings.intervalMinutes.coerceAtLeast(15)) * 60 * 1000L
        val triggerAtMillis = System.currentTimeMillis() + intervalMillis

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                intervalMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled water reminders every ${settings.intervalMinutes}m (${settings.startTime} - ${settings.endTime})")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule water alarm: ${e.message}")
        }
    }

    fun cancelWaterReminders() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WaterReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WATER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled water reminders")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel water alarm: ${e.message}")
        }
    }

    fun sendWaterNotification(isTest: Boolean = false) {
        if (!hasNotificationPermission()) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "water")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_WATER,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifId = if (isTest) ID_WATER_TEST else ID_WATER_SCHEDULED
        val titleText = if (isTest) "Hydration Alert Test 💧" else "Time to Hydrate! 💧"
        val bodyText = if (isTest) "Your hydration reminders are active! Drink a fresh glass of water now." else "Drink 250ml of water to fuel physical endurance, mental clarity, and focus."

        val notification = NotificationCompat.Builder(context, CHANNEL_WATER)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(notifId, notification)
    }

    // --- 3. NIGHT REVIEW REMINDERS ---

    fun scheduleNightReview(settings: NightReviewReminderSettings) {
        if (!settings.isEnabled) {
            cancelNightReview()
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NightReviewReceiver::class.java).apply {
            putExtra("reviewTime", settings.time)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_NIGHT_REVIEW,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = parseTimeToMillis(settings.time)
        val repeatInterval = AlarmManager.INTERVAL_DAY

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                repeatInterval,
                pendingIntent
            )
            Log.d(TAG, "Scheduled night review at ${settings.time}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule night review alarm: ${e.message}")
        }
    }

    fun cancelNightReview() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NightReviewReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_NIGHT_REVIEW,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled night review alarm")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel night review alarm: ${e.message}")
        }
    }

    fun sendNightReviewNotification(isTest: Boolean = false) {
        if (!hasNotificationPermission()) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "night_review")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_NIGHT_REVIEW,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifId = if (isTest) ID_NIGHT_REVIEW_TEST else ID_NIGHT_REVIEW
        val titleText = if (isTest) "Night Review Alert Test 🌙" else "Night Review & Reflection 🌙"
        val bodyText = if (isTest) "Evening reflection alert test. Tap to review your day's achievements!" else "Take 2 minutes to review today's discipline, celebrate wins, and set tomorrow's top priority."

        val notification = NotificationCompat.Builder(context, CHANNEL_NIGHT_REVIEW)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(notifId, notification)
    }

    // --- Unified Test Alert Dispatcher ---
    fun sendTestNotification(type: ReminderType, customHabitTitle: String = "Morning Meditation") {
        when (type) {
            ReminderType.WATER -> sendWaterNotification(isTest = true)
            ReminderType.HABIT -> sendHabitNotification("test_habit", customHabitTitle, isTest = true)
            ReminderType.NIGHT_REVIEW -> sendNightReviewNotification(isTest = true)
        }
    }
}
