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
import com.example.data.models.WaterReminderSettings
import com.example.data.receiver.WaterReminderReceiver
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class WaterReminderManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "water_reminders_channel"
        const val CHANNEL_NAME = "Water Hydration Reminders"
        const val NOTIFICATION_ID = 1001
        const val TEST_NOTIFICATION_ID = 1002
        private const val REQUEST_CODE = 2001
        private const val TAG = "WaterReminderManager"

        fun isWithinActiveHours(startTimeStr: String, endTimeStr: String): Boolean {
            return try {
                val now = LocalTime.now()
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                val start = LocalTime.parse(startTimeStr, formatter)
                val end = LocalTime.parse(endTimeStr, formatter)

                if (start.isBefore(end)) {
                    !now.isBefore(start) && !now.isAfter(end)
                } else {
                    // Over midnight span
                    !now.isBefore(start) || !now.isAfter(end)
                }
            } catch (e: Exception) {
                true
            }
        }
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle alerts to stay hydrated throughout your active hours."
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
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

    fun scheduleReminders(settings: WaterReminderSettings) {
        if (!settings.isEnabled) {
            cancelReminders()
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
            REQUEST_CODE,
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
            Log.d(TAG, "Scheduled water reminders every ${settings.intervalMinutes} min between ${settings.startTime} and ${settings.endTime}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule repeating alarm: ${e.message}")
        }
    }

    fun cancelReminders() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WaterReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled water reminders")
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling alarm: ${e.message}")
        }
    }

    fun sendTestNotification() {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Cannot send notification: POST_NOTIFICATIONS permission not granted")
            return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Hydration Reminder 💧")
            .setContentText("Drink a glass of water to fuel your focus and energy!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Drink a glass of water now. Staying hydrated keeps your cognitive clarity, discipline, and endurance at peak levels!")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(TEST_NOTIFICATION_ID, notification)
    }

    fun sendScheduledNotification() {
        if (!hasNotificationPermission()) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Time to Hydrate! 💧")
            .setContentText("Take a moment to drink 250ml of water.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }
}
