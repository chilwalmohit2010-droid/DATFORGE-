package com.example.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.notifications.DisciplineNotificationManager

class HabitReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "HabitReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("habitId") ?: return
        val habitTitle = intent.getStringExtra("habitTitle") ?: "Your Habit"
        val reminderTime = intent.getStringExtra("reminderTime") ?: ""

        Log.d(TAG, "HabitReminderReceiver triggered for habit: $habitTitle ($habitId) at $reminderTime")

        val notificationManager = DisciplineNotificationManager(context)
        notificationManager.sendHabitNotification(habitId, habitTitle)
    }
}
