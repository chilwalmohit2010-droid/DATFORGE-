package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChallengeBootReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "ChallengeBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            Log.d(TAG, "Device rebooted or timezone changed ($action). Rescheduling enabled alarms...")

            val db = AppDatabase.getInstance(context)
            val manager = ChallengeAlarmManager(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val enabledAlarms = db.challengeAlarmDao().getAllEnabledAlarms()
                    for (alarm in enabledAlarms) {
                        manager.scheduleAlarm(alarm)
                    }
                    Log.d(TAG, "Rescheduled ${enabledAlarms.size} alarms successfully.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule alarms on boot: ${e.message}", e)
                }
            }
        }
    }
}
