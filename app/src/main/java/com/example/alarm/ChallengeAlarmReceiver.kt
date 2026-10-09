package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChallengeAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "ChallengeAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Received action: $action")

        val alarmId = intent.getStringExtra(ChallengeAlarmManager.EXTRA_ALARM_ID) ?: ""

        when (action) {
            ChallengeAlarmManager.ACTION_CHALLENGE_ALARM -> {
                // 1. Start Challenge Alarm Service (Foreground audio + notification)
                val serviceIntent = Intent(context, ChallengeAlarmService::class.java).apply {
                    putExtras(intent)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // 2. Launch full-screen Challenge Activity
                val activityIntent = Intent(context, ChallengeAlarmActivity::class.java).apply {
                    putExtras(intent)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                try {
                    context.startActivity(activityIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot start activity directly (background restrictions): ${e.message}. FullScreenNotification will trigger.")
                }
            }

            ChallengeAlarmManager.ACTION_SNOOZE_ALARM -> {
                // 1. Stop Alarm Service
                val stopServiceIntent = Intent(context, ChallengeAlarmService::class.java).apply {
                    this.action = ChallengeAlarmService.ACTION_STOP_ALARM
                }
                context.startService(stopServiceIntent)

                // 2. Schedule Snooze
                val snoozeMins = intent.getIntExtra(ChallengeAlarmManager.EXTRA_SNOOZE_MINUTES, 5)
                val db = AppDatabase.getInstance(context)
                val manager = ChallengeAlarmManager(context)

                CoroutineScope(Dispatchers.IO).launch {
                    val alarm = db.challengeAlarmDao().getAlarmById(alarmId)
                    if (alarm != null) {
                        manager.scheduleSnooze(alarm, snoozeMins)
                    }
                }
            }

            ChallengeAlarmManager.ACTION_DISMISS_ALARM -> {
                // Stop service
                val stopServiceIntent = Intent(context, ChallengeAlarmService::class.java).apply {
                    this.action = ChallengeAlarmService.ACTION_STOP_ALARM
                }
                context.startService(stopServiceIntent)
            }
        }
    }
}
