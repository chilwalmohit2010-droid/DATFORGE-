package com.example.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.models.ChallengeHistoryEntry
import com.example.data.models.ChallengeType
import com.example.data.remote.FirebaseManager
import com.example.data.repository.DisciplineRepository
import com.example.ui.screens.alarm.ChallengeAlarmScreen
import com.example.ui.theme.DisciplineTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChallengeAlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen and display on top of keyguard / lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()

        val alarmId = intent.getStringExtra(ChallengeAlarmManager.EXTRA_ALARM_ID) ?: ""
        val alarmName = intent.getStringExtra(ChallengeAlarmManager.EXTRA_ALARM_NAME) ?: "Wake-Up Warrior"
        val challengeTypeStr = intent.getStringExtra(ChallengeAlarmManager.EXTRA_CHALLENGE_TYPE) ?: "PUSH_UPS"
        val challengeType = try { ChallengeType.valueOf(challengeTypeStr) } catch (e: Exception) { ChallengeType.PUSH_UPS }
        val target = intent.getIntExtra(ChallengeAlarmManager.EXTRA_TARGET, challengeType.defaultTarget)
        val linkedHabitId = intent.getStringExtra(ChallengeAlarmManager.EXTRA_LINKED_HABIT_ID)
        val videoUriStr = intent.getStringExtra(ChallengeAlarmManager.EXTRA_VIDEO_URI)
        val snoozeMinutes = intent.getIntExtra(ChallengeAlarmManager.EXTRA_SNOOZE_MINUTES, 5)

        val database = AppDatabase.getInstance(applicationContext)
        val firebaseManager = FirebaseManager(applicationContext)
        val repository = DisciplineRepository(application, database, firebaseManager)

        setContent {
            DisciplineTheme {
                ChallengeAlarmScreen(
                    alarmId = alarmId,
                    alarmName = alarmName,
                    challengeType = challengeType,
                    target = target,
                    linkedHabitId = linkedHabitId,
                    videoUriStr = videoUriStr,
                    snoozeMinutes = snoozeMinutes,
                    onChallengeFinished = { entry ->
                        saveResultAndClose(repository, entry, isCompleted = true)
                    },
                    onStopSafetyClicked = { entry ->
                        saveResultAndClose(repository, entry, isCompleted = false)
                    },
                    onSnoozeClicked = {
                        snoozeAndClose(alarmId, snoozeMinutes)
                    }
                )
            }
        }
    }

    private fun saveResultAndClose(
        repository: DisciplineRepository,
        entry: ChallengeHistoryEntry,
        isCompleted: Boolean
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val uid = repository.getCurrentUserId() ?: "guest_warrior"
            val finalEntry = entry.copy(userId = uid)

            // Save history locally and in cloud
            repository.saveChallengeResult(finalEntry)

            if (isCompleted && !finalEntry.linkedHabitId.isNullOrBlank()) {
                repository.markHabitCompletedToday(uid, finalEntry.linkedHabitId)
            }

            // Launch Main App
            val mainIntent = Intent(this@ChallengeAlarmActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination", "alarms")
            }
            startActivity(mainIntent)
            finish()
        }
    }

    private fun snoozeAndClose(alarmId: String, snoozeMinutes: Int) {
        val snoozeIntent = Intent(this, ChallengeAlarmReceiver::class.java).apply {
            action = ChallengeAlarmManager.ACTION_SNOOZE_ALARM
            putExtra(ChallengeAlarmManager.EXTRA_ALARM_ID, alarmId)
            putExtra(ChallengeAlarmManager.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        sendBroadcast(snoozeIntent)
        finish()
    }
}
