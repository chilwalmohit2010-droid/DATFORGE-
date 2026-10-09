package com.example.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.*
import java.util.Locale

/**
 * Foreground service to maintain accurate background focus session tracking
 * and enforce native app restrictions during active sessions.
 */
class FocusRestrictionService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var isMonitoring = false
    private var lastInterceptionTime = 0L

    companion object {
        private const val TAG = "FocusRestrictionService"
        const val CHANNEL_ID = "dayforge_focus_session_channel"
        const val NOTIFICATION_ID = 4001

        const val ACTION_START = "com.example.action.START_FOCUS_SERVICE"
        const val ACTION_STOP = "com.example.action.STOP_FOCUS_SERVICE"
        const val ACTION_SESSION_COMPLETED = "com.example.action.FOCUS_SESSION_COMPLETED"

        fun startService(context: Context) {
            val intent = Intent(context, FocusRestrictionService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start FocusRestrictionService: ${e.message}")
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FocusRestrictionService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop FocusRestrictionService: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoring()
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                startMonitoring()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        val initialNotif = buildNotification(FocusSessionManager.computeRemainingSeconds(this))
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(
                        NOTIFICATION_ID,
                        initialNotif,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, initialNotif)
                }
            } else {
                startForeground(NOTIFICATION_ID, initialNotif)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Foreground start failed: ${e.message}")
        }

        serviceScope.launch {
            while (isMonitoring) {
                val remainingSeconds = FocusSessionManager.computeRemainingSeconds(applicationContext)

                if (remainingSeconds <= 0) {
                    // Session finished!
                    onSessionCompleted()
                    break
                }

                // Update notification every 2 seconds
                if (remainingSeconds % 2 == 0) {
                    val notif = buildNotification(remainingSeconds)
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, notif)
                }

                // Native App Restriction check
                if (FocusSessionManager.isRestrictionEnabled(applicationContext)) {
                    checkAppRestrictions(remainingSeconds)
                }

                delay(1000)
            }
        }
    }

    private fun checkAppRestrictions(remainingSeconds: Int) {
        val now = System.currentTimeMillis()
        if (now - lastInterceptionTime < 4000) {
            // Debounce to prevent multiple activity launches
            return
        }

        val foregroundApp = FocusSessionManager.getForegroundAppPackage(applicationContext) ?: return
        val restrictedApps = FocusSessionManager.getRestrictedApps(applicationContext)

        if (restrictedApps.contains(foregroundApp) && foregroundApp != packageName) {
            lastInterceptionTime = now
            FocusSessionManager.setRestrictedAppDetected(foregroundApp)

            val interceptIntent = Intent(applicationContext, FocusRestrictionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("restricted_package", foregroundApp)
                putExtra("remaining_seconds", remainingSeconds)
            }
            try {
                startActivity(interceptIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch FocusRestrictionActivity: ${e.message}")
            }
        }
    }

    private fun onSessionCompleted() {
        stopMonitoring()
        FocusSessionManager.clearActiveSession(applicationContext)

        // Broadcast or send intent to main app
        val completionIntent = Intent(ACTION_SESSION_COMPLETED).apply {
            setPackage(packageName)
        }
        sendBroadcast(completionIntent)

        // Show completed notification
        showCompletionNotification()

        stopForeground(true)
        stopSelf()
    }

    private fun stopMonitoring() {
        isMonitoring = false
        serviceScope.coroutineContext.cancelChildren()
    }

    private fun buildNotification(remainingSeconds: Int): Notification {
        val mins = remainingSeconds / 60
        val secs = remainingSeconds % 60
        val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        val habitTitle = FocusSessionManager.getHabitTitle(this)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            action = "OPEN_FOCUS_SCREEN"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Session Active • $timeFormatted")
            .setContentText("Focusing on '$habitTitle' • Stay locked in 🎯")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showCompletionNotification() {
        val habitTitle = FocusSessionManager.getHabitTitle(this)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            action = "OPEN_FOCUS_SCREEN"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Session Completed! 🏆")
            .setContentText("Great discipline! Completed flow block for '$habitTitle'.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(NOTIFICATION_ID + 1, notif)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Session Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live remaining time and active status for DAYFORGE Focus sessions"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
