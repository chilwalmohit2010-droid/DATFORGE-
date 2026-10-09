package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.models.*

@Database(
    entities = [
        UserAccount::class,
        Habit::class,
        HabitCompletion::class,
        WaterRecord::class,
        WaterEntry::class,
        FocusSession::class,
        Achievement::class,
        NightReviewEntry::class,
        AppNotification::class,
        ChallengeAlarm::class,
        ChallengeHistoryEntry::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun waterDao(): WaterDao
    abstract fun focusDao(): FocusDao
    abstract fun achievementDao(): AchievementDao
    abstract fun nightReviewDao(): NightReviewDao
    abstract fun notificationDao(): NotificationDao
    abstract fun challengeAlarmDao(): ChallengeAlarmDao
    abstract fun challengeHistoryDao(): ChallengeHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "discipline_app_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
