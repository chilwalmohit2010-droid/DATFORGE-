package com.example.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.notifications.WaterReminderManager

class WaterReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WaterReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val startTime = intent.getStringExtra("startTime") ?: "08:00"
        val endTime = intent.getStringExtra("endTime") ?: "21:00"

        Log.d(TAG, "WaterReminderReceiver triggered. Checking active hours: $startTime - $endTime")

        if (WaterReminderManager.isWithinActiveHours(startTime, endTime)) {
            val reminderManager = WaterReminderManager(context)
            reminderManager.sendScheduledNotification()
        } else {
            Log.d(TAG, "Current time is outside active hydration hours ($startTime - $endTime). Notification suppressed.")
        }
    }
}
