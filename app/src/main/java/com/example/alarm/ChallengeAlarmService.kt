package com.example.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R

class ChallengeAlarmService : Service() {

    companion object {
        const val TAG = "ChallengeAlarmService"
        const val NOTIFICATION_ID = 9001

        const val ACTION_START_ALARM = "com.example.action.START_ALARM_SERVICE"
        const val ACTION_STOP_ALARM = "com.example.action.STOP_ALARM_SERVICE"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == ACTION_STOP_ALARM) {
            stopAlarmPlayback()
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent.getStringExtra(ChallengeAlarmManager.EXTRA_ALARM_ID) ?: ""
        val alarmName = intent.getStringExtra(ChallengeAlarmManager.EXTRA_ALARM_NAME) ?: "Wake-Up Warrior"
        val challengeType = intent.getStringExtra(ChallengeAlarmManager.EXTRA_CHALLENGE_TYPE) ?: "PUSH_UPS"
        val target = intent.getIntExtra(ChallengeAlarmManager.EXTRA_TARGET, 15)
        val soundUriStr = intent.getStringExtra(ChallengeAlarmManager.EXTRA_SOUND_URI)
        val videoUriStr = intent.getStringExtra(ChallengeAlarmManager.EXTRA_VIDEO_URI)
        val vibration = intent.getBooleanExtra(ChallengeAlarmManager.EXTRA_VIBRATION, true)
        val snoozeMinutes = intent.getIntExtra(ChallengeAlarmManager.EXTRA_SNOOZE_MINUTES, 5)

        // 1. Build and post Foreground Notification
        val notification = buildForegroundNotification(alarmId, alarmName, challengeType, target, videoUriStr, snoozeMinutes)
        startForeground(NOTIFICATION_ID, notification)

        // 2. Start Audio Playback
        startSoundPlayback(soundUriStr)

        // 3. Start Vibration
        if (vibration) {
            startVibration()
        }

        return START_STICKY
    }

    private fun startSoundPlayback(customUriStr: String?) {
        try {
            mediaPlayer?.release()
            mediaPlayer = null

            var uri: Uri? = null
            if (!customUriStr.isNullOrBlank()) {
                try {
                    uri = Uri.parse(customUriStr)
                } catch (e: Exception) {
                    Log.w(TAG, "Custom URI parse error, falling back: ${e.message}")
                }
            }

            if (uri == null) {
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                try {
                    setDataSource(applicationContext, uri!!)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load custom sound URI, using default ringtone: ${e.message}")
                    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    setDataSource(applicationContext, defaultUri)
                }
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm audio: ${e.message}", e)
        }
    }

    private fun startVibration() {
        try {
            val pattern = longArrayOf(0, 600, 300, 600, 300, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun buildForegroundNotification(
        alarmId: String,
        alarmName: String,
        challengeType: String,
        target: Int,
        videoUriStr: String?,
        snoozeMinutes: Int
    ): Notification {
        // Fullscreen challenge activity intent
        val challengeIntent = Intent(this, ChallengeAlarmActivity::class.java).apply {
            putExtra(ChallengeAlarmManager.EXTRA_ALARM_ID, alarmId)
            putExtra(ChallengeAlarmManager.EXTRA_ALARM_NAME, alarmName)
            putExtra(ChallengeAlarmManager.EXTRA_CHALLENGE_TYPE, challengeType)
            putExtra(ChallengeAlarmManager.EXTRA_TARGET, target)
            putExtra(ChallengeAlarmManager.EXTRA_VIDEO_URI, videoUriStr)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.hashCode(),
            challengeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss intent (safety stop)
        val dismissIntent = Intent(this, ChallengeAlarmReceiver::class.java).apply {
            action = ChallengeAlarmManager.ACTION_DISMISS_ALARM
            putExtra(ChallengeAlarmManager.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            (alarmId + "_dismiss").hashCode(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedType = when (challengeType) {
            "PUSH_UPS" -> "Push-ups"
            "SQUATS" -> "Squats"
            "JUMPING_JACKS" -> "Jumping Jacks"
            else -> "Timed Challenge"
        }

        val builder = NotificationCompat.Builder(this, ChallengeAlarmManager.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚔️ DayForge: $alarmName")
            .setContentText("Complete $target $formattedType to wake up and conquer the day!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Start Challenge", fullScreenPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Stop", dismissPendingIntent)

        if (snoozeMinutes > 0) {
            val snoozeIntent = Intent(this, ChallengeAlarmReceiver::class.java).apply {
                action = ChallengeAlarmManager.ACTION_SNOOZE_ALARM
                putExtra(ChallengeAlarmManager.EXTRA_ALARM_ID, alarmId)
                putExtra(ChallengeAlarmManager.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                this,
                (alarmId + "_snooze").hashCode(),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.mipmap.ic_launcher, "Snooze ($snoozeMinutes m)", snoozePendingIntent)
        }

        return builder.build()
    }

    private fun stopAlarmPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping media player: ${e.message}")
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping vibrator: ${e.message}")
        }
    }

    override fun onDestroy() {
        stopAlarmPlayback()
        super.onDestroy()
    }
}
