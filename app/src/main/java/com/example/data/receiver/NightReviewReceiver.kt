package com.example.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.notifications.DisciplineNotificationManager

class NightReviewReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NightReviewReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reviewTime = intent.getStringExtra("reviewTime") ?: "21:30"
        Log.d(TAG, "NightReviewReceiver triggered at $reviewTime")

        val notificationManager = DisciplineNotificationManager(context)
        notificationManager.sendNightReviewNotification()
    }
}
