package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class HabitCategory(val displayName: String, val defaultIcon: String) {
    HEALTH("Health", "favorite"),
    FITNESS("Fitness", "fitness_center"),
    PRODUCTIVITY("Productivity", "bolt"),
    MINDSET("Mindset", "psychology"),
    ROUTINE("Routine", "schedule"),
    LEARNING("Learning", "menu_book"),
    FINANCE("Finance", "payments"),
    CUSTOM("Custom", "star")
}

enum class CompletionStatus {
    COMPLETED,
    MISSED,
    SKIPPED
}

@Entity(tableName = "user_profiles")
data class UserAccount(
    @PrimaryKey val uid: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val disciplineScore: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val completionRate: Float = 0f,
    val isProfileComplete: Boolean = false,
    val publicProfileEnabled: Boolean = false,
    val leaderboardEnabled: Boolean = false
)

data class LeaderboardEntry(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val disciplineScore: Int = 0,
    val currentStreak: Int = 0,
    val rank: Int = 0,
    val leaderboardEnabled: Boolean = true,
    val updatedAt: Long = 0L
)

data class PublicUserProfile(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val photoUrl: String = "",
    val publicProfileEnabled: Boolean = false,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val disciplineScore: Int = 0,
    val publicAchievements: List<PublicAchievement> = emptyList(),
    val updatedAt: Long = 0L
)

data class PublicAchievement(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val icon: String = "emoji_events",
    val unlockedAt: Long? = null
)

enum class HabitDifficulty(val displayName: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}

enum class HabitGoalType(val displayName: String) {
    ONCE("Complete Once"),
    DURATION("Duration"),
    QUANTITY("Quantity")
}

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: HabitCategory = HabitCategory.HEALTH,
    val iconName: String = "check_circle",
    val colorHex: Long = 0xFF10B981, // Emerald default
    val reminderTime: String? = null, // e.g., "07:30"
    val repeatDays: String = "1,2,3,4,5,6,7", // CSV of ISO DayOfWeek 1 (Mon) to 7 (Sun)
    val goalValue: Int = 1,
    val goalUnit: String = "times",
    val isPaused: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0,
    val durationMinutes: Int = 0,
    val startDate: String? = null, // YYYY-MM-DD
    val endDate: String? = null,   // YYYY-MM-DD
    val difficulty: HabitDifficulty = HabitDifficulty.MEDIUM,
    val notes: String = "",
    val isMorningRoutine: Boolean = false,
    val goalType: HabitGoalType = HabitGoalType.ONCE,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getRepeatDaysList(): List<Int> {
        return if (repeatDays.isBlank()) listOf(1, 2, 3, 4, 5, 6, 7)
        else repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    fun isScheduledForDayOfWeek(dayOfWeekIso: Int): Boolean {
        return getRepeatDaysList().contains(dayOfWeekIso)
    }

    fun isScheduledForDate(date: LocalDate): Boolean {
        if (isPaused) return false
        if (!startDate.isNullOrBlank()) {
            try {
                val start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE)
                if (date.isBefore(start)) return false
            } catch (_: Exception) {}
        }
        if (!endDate.isNullOrBlank()) {
            try {
                val end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE)
                if (date.isAfter(end)) return false
            } catch (_: Exception) {}
        }
        return isScheduledForDayOfWeek(date.dayOfWeek.value)
    }

    fun isEnded(date: LocalDate = LocalDate.now()): Boolean {
        if (endDate.isNullOrBlank()) return false
        return try {
            val end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE)
            date.isAfter(end)
        } catch (_: Exception) {
            false
        }
    }

    fun isFuture(date: LocalDate = LocalDate.now()): Boolean {
        if (startDate.isNullOrBlank()) return false
        return try {
            val start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE)
            date.isBefore(start)
        } catch (_: Exception) {
            false
        }
    }
}

@Entity(tableName = "habit_completions", primaryKeys = ["habitId", "date"])
data class HabitCompletion(
    val habitId: String = "",
    val userId: String = "",
    val date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), // YYYY-MM-DD
    val status: CompletionStatus = CompletionStatus.COMPLETED,
    val completedAt: Long = System.currentTimeMillis(),
    val numericProgress: Int = 1
)

@Entity(tableName = "water_records", primaryKeys = ["userId", "date"])
data class WaterRecord(
    val userId: String = "",
    val date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val currentMl: Int = 0,
    val targetMl: Int = 0
)

@Entity(tableName = "water_entries")
data class WaterEntry(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val amountMl: Int = 250,
    val timestamp: Long = System.currentTimeMillis()
)

data class WaterReminderSettings(
    val isEnabled: Boolean = false,
    val intervalMinutes: Int = 60, // 30, 60, 90, 120
    val startTime: String = "08:00",
    val endTime: String = "21:00"
)

data class NightReviewReminderSettings(
    val isEnabled: Boolean = false,
    val time: String = "21:30" // 9:30 PM default
)

data class ReminderSettings(
    val waterReminders: WaterReminderSettings = WaterReminderSettings(),
    val nightReview: NightReviewReminderSettings = NightReviewReminderSettings(),
    val habitReminders: Map<String, Boolean> = emptyMap(), // habitId -> isEnabled
    val habitReminderTimes: Map<String, String> = emptyMap() // habitId -> time string e.g. "07:30"
)

@Entity(tableName = "night_reviews", primaryKeys = ["userId", "date"])
data class NightReviewEntry(
    val date: String = "", // YYYY-MM-DD
    val userId: String = "",
    val completedHabits: Int = 0,
    val totalHabits: Int = 0,
    val completionPercentage: Int = 0,
    val streak: Int = 0,
    val waterIntake: Int = 0,
    val waterGoal: Int = 0,
    val focusMinutes: Int = 0,
    val wentWell: String = "",
    val improvement: String = "",
    val reflection: String = "",
    val rating: Int = 5, // 1 to 5 stars
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val score: Int = rating,
    val wins: String = wentWell,
    val struggles: String = improvement,
    val tomorrowPriority: String = reflection,
    val timestamp: Long = createdAt
)

enum class ReminderType {
    HABIT,
    WATER,
    NIGHT_REVIEW
}

enum class NotificationType {
    HABIT,
    WATER,
    NIGHT_REVIEW,
    FOCUS,
    ALARM,
    LEADERBOARD,
    SYSTEM,
    CONTENT
}

enum class ChallengeType(
    val displayName: String,
    val iconName: String,
    val unit: String,
    val defaultTarget: Int
) {
    PUSH_UPS("Push-ups", "fitness_center", "reps", 15),
    SQUATS("Squats", "accessibility_new", "reps", 20),
    JUMPING_JACKS("Jumping Jacks", "directions_run", "reps", 25),
    TIMED_CHALLENGE("Timed Challenge", "timer", "seconds", 60)
}

@Entity(tableName = "challenge_alarms")
data class ChallengeAlarm(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val name: String = "Wake-Up Warrior",
    val time: String = "06:30", // HH:mm
    val repeatDays: String = "1,2,3,4,5,6,7", // CSV of ISO DayOfWeek 1 (Mon) to 7 (Sun), or empty string for one-time
    val isEnabled: Boolean = true,
    val challengeType: ChallengeType = ChallengeType.PUSH_UPS,
    val target: Int = 15,
    val linkedHabitId: String? = null,
    val soundUri: String = "",
    val soundTitle: String = "DayForge Default Sound",
    val videoUri: String = "",
    val vibration: Boolean = true,
    val snoozeMinutes: Int = 5,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getRepeatDaysList(): List<Int> {
        return if (repeatDays.isBlank()) emptyList()
        else repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    fun isOneTime(): Boolean = repeatDays.isBlank()

    fun isScheduledForDayOfWeek(dayOfWeekIso: Int): Boolean {
        val list = getRepeatDaysList()
        return list.isEmpty() || list.contains(dayOfWeekIso)
    }
}

@Entity(tableName = "challenge_history")
data class ChallengeHistoryEntry(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val alarmId: String? = null,
    val alarmName: String = "",
    val challengeType: ChallengeType = ChallengeType.PUSH_UPS,
    val target: Int = 15,
    val completedAmount: Int = 15,
    val status: String = "COMPLETED", // COMPLETED, STOPPED, CANCELLED, SNOOZED
    val linkedHabitId: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
)

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val targetDestination: String = "",
    val relatedEntityId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val scheduledDate: String = ""
)

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val habitId: String? = null,
    val habitTitle: String = "Deep Work",
    val durationMinutes: Int = 25,
    val completedMinutes: Int = 25,
    val timestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = true
)

data class FocusSettings(
    val durationMinutes: Int = 25,
    val linkedHabitId: String? = null,
    val restrictionEnabled: Boolean = false,
    val restrictedApps: List<String> = emptyList(), // package names e.g. ["com.instagram.android"]
    val relatedAppPackage: String? = null,
    val relatedAppName: String? = null,
    val autoOpenRelatedApp: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val description: String = "",
    val icon: String = "emoji_events",
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val requirementType: String = "streak", // streak, total_habits, water_days, focus_sessions, night_reviews
    val requirementValue: Int = 1
)

data class DisciplineScoreBreakdown(
    val score: Int = 0,
    val habitsScore: Int = 0,     // up to 30
    val streakScore: Int = 0,     // up to 20
    val waterScore: Int = 0,      // up to 15
    val focusScore: Int = 0,      // up to 20
    val reviewScore: Int = 0,     // up to 15
    val habitsExplanation: String = "Completion of scheduled routine habits",
    val streakExplanation: String = "Continuous daily consistency without breaking",
    val waterExplanation: String = "Achieving daily target hydration volume",
    val focusExplanation: String = "Completing deep work flow blocks",
    val reviewExplanation: String = "Daily mindfulness & evening reviews"
)

data class JournalEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val time: String = "",
    val date: String = "", // YYYY-MM-DD
    val tags: List<String> = emptyList(),
    val category: String = "Reflection",
    val createdAt: Long = System.currentTimeMillis()
)

enum class ContentCategory(val displayName: String, val iconName: String) {
    ALL("All", "auto_awesome"),
    FOR_YOU("For You", "recommend"),
    FITNESS("Fitness", "fitness_center"),
    HEALTH("Health", "favorite"),
    DISCIPLINE("Discipline", "military_tech"),
    STUDY("Study", "school"),
    PRODUCTIVITY("Productivity", "hourglass_top"),
    MINDSET("Mindset", "psychology"),
    SAVED("Saved", "bookmark")
}

data class DisciplineContent(
    val contentId: String = "",
    val title: String = "",
    val description: String = "",
    val mediaUrl: String = "",
    val thumbnailUrl: String = "",
    val category: String = "DISCIPLINE",
    val creatorName: String = "DayForge Academy",
    val createdAt: Long = System.currentTimeMillis(),
    val duration: String = "2 min",
    val isPublished: Boolean = true,
    val isFeatured: Boolean = false,
    val ctaLabel: String? = null,
    val ctaAction: String? = null
)

