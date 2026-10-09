package com.example.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.models.ChallengeAlarm
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ChallengeAlarmManager(private val context: Context) {

    companion object {
        const val TAG = "ChallengeAlarmManager"
        const val CHANNEL_ID = "dayforge_challenge_alarms_channel"
        const val CHANNEL_NAME = "Challenge Wake-Up Alarms"

        const val ACTION_CHALLENGE_ALARM = "com.example.action.CHALLENGE_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.action.SNOOZE_ALARM"
        const val ACTION_DISMISS_ALARM = "com.example.action.DISMISS_ALARM"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_NAME = "extra_alarm_name"
        const val EXTRA_CHALLENGE_TYPE = "extra_challenge_type"
        const val EXTRA_TARGET = "extra_target"
        const val EXTRA_LINKED_HABIT_ID = "extra_linked_habit_id"
        const val EXTRA_SOUND_URI = "extra_sound_uri"
        const val EXTRA_SOUND_TITLE = "extra_sound_title"
        const val EXTRA_VIDEO_URI = "extra_video_uri"
        const val EXTRA_VIBRATION = "extra_vibration"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical native wake-up challenge alarms"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Checks if the app can schedule exact alarms on Android 12+ (API 31+).
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    /**
     * Schedules a challenge alarm based on configured time & repeat days.
     */
    fun scheduleAlarm(alarm: ChallengeAlarm): Long {
        if (!alarm.isEnabled) {
            cancelAlarm(alarm.id)
            return 0L
        }

        val triggerTimeMs = calculateNextTriggerMillis(alarm)
        if (triggerTimeMs <= System.currentTimeMillis()) {
            Log.w(TAG, "Trigger time in the past for alarm ${alarm.id}")
            return 0L
        }

        // Always cancel existing schedule first to prevent duplicate firing
        cancelAlarm(alarm.id)

        val intent = Intent(context, ChallengeAlarmReceiver::class.java).apply {
            action = ACTION_CHALLENGE_ALARM
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_NAME, alarm.name)
            putExtra(EXTRA_CHALLENGE_TYPE, alarm.challengeType.name)
            putExtra(EXTRA_TARGET, alarm.target)
            putExtra(EXTRA_LINKED_HABIT_ID, alarm.linkedHabitId)
            putExtra(EXTRA_SOUND_URI, alarm.soundUri)
            putExtra(EXTRA_SOUND_TITLE, alarm.soundTitle)
            putExtra(EXTRA_VIDEO_URI, alarm.videoUri)
            putExtra(EXTRA_VIBRATION, alarm.vibration)
            putExtra(EXTRA_SNOOZE_MINUTES, alarm.snoozeMinutes)
        }

        val requestCode = alarm.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, ChallengeAlarmActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                }
                Log.d(TAG, "Successfully scheduled alarm ${alarm.id} (${alarm.name}) for $triggerTimeMs")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm: ${e.message}. Falling back to inexact.")
            alarmManager?.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm: ${e.message}", e)
        }

        return triggerTimeMs
    }

    /**
     * Cancels an existing scheduled alarm.
     */
    fun cancelAlarm(alarmId: String) {
        val intent = Intent(context, ChallengeAlarmReceiver::class.java).apply {
            action = ACTION_CHALLENGE_ALARM
        }
        val requestCode = alarmId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Cancelled alarm $alarmId")
    }

    /**
     * Schedules a snooze for the current alarm.
     */
    fun scheduleSnooze(alarm: ChallengeAlarm, minutes: Int = 5) {
        val snoozeTimeMs = System.currentTimeMillis() + minutes * 60 * 1000L

        val intent = Intent(context, ChallengeAlarmReceiver::class.java).apply {
            action = ACTION_CHALLENGE_ALARM
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_NAME, alarm.name)
            putExtra(EXTRA_CHALLENGE_TYPE, alarm.challengeType.name)
            putExtra(EXTRA_TARGET, alarm.target)
            putExtra(EXTRA_LINKED_HABIT_ID, alarm.linkedHabitId)
            putExtra(EXTRA_SOUND_URI, alarm.soundUri)
            putExtra(EXTRA_SOUND_TITLE, alarm.soundTitle)
            putExtra(EXTRA_VIDEO_URI, alarm.videoUri)
            putExtra(EXTRA_VIBRATION, alarm.vibration)
            putExtra(EXTRA_SNOOZE_MINUTES, alarm.snoozeMinutes)
        }

        val requestCode = ("snooze_${alarm.id}").hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTimeMs, pendingIntent)
            } else {
                alarmManager?.setExact(AlarmManager.RTC_WAKEUP, snoozeTimeMs, pendingIntent)
            }
            Log.d(TAG, "Scheduled snooze for ${alarm.id} in $minutes minutes ($snoozeTimeMs)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule snooze: ${e.message}")
        }
    }

    /**
     * Calculates the next exact epoch millis for the alarm.
     */
    fun calculateNextTriggerMillis(alarm: ChallengeAlarm): Long {
        val parts = alarm.time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 6
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 30

        val now = LocalDateTime.now()
        val todayAlarm = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        val repeatDays = alarm.getRepeatDaysList()

        if (repeatDays.isEmpty()) {
            // One-time alarm
            return if (todayAlarm.isAfter(now)) {
                todayAlarm.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } else {
                todayAlarm.plusDays(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }

        // Repeating on selected days of week
        for (dayOffset in 0..7) {
            val candidate = todayAlarm.plusDays(dayOffset.toLong())
            val candidateDayOfWeek = candidate.dayOfWeek.value // 1 (Mon) to 7 (Sun)
            if (repeatDays.contains(candidateDayOfWeek)) {
                if (dayOffset == 0 && candidate.isBefore(now)) {
                    continue // today's time already passed
                }
                return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }

        // Fallback to tomorrow same time
        return todayAlarm.plusDays(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
