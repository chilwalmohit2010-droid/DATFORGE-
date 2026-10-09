package com.example.data.local

import androidx.room.*
import com.example.data.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    fun getUserProfile(uid: String): Flow<UserAccount?>

    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    suspend fun getUserProfileOnce(uid: String): UserAccount?

    @Query("SELECT * FROM user_profiles WHERE username = :username COLLATE NOCASE LIMIT 1")
    suspend fun getUserByUsername(username: String): UserAccount?

    @Query("SELECT * FROM user_profiles WHERE publicProfileEnabled = 1 AND (username LIKE '%' || :query || '%' OR displayName LIKE '%' || :query || '%') ORDER BY disciplineScore DESC")
    suspend fun searchPublicProfiles(query: String): List<UserAccount>

    @Query("SELECT * FROM user_profiles WHERE publicProfileEnabled = 1 ORDER BY disciplineScore DESC LIMIT 20")
    suspend fun getTopPublicProfiles(): List<UserAccount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserAccount)

    @Query("DELETE FROM user_profiles WHERE uid = :uid")
    suspend fun deleteUser(uid: String)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY orderIndex ASC, createdAt DESC")
    fun getHabitsForUser(userId: String): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY orderIndex ASC, createdAt DESC")
    suspend fun getHabitsForUserOnce(userId: String): List<Habit>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: String): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHabit(habit: Habit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<Habit>)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: String)

    @Query("DELETE FROM habits WHERE userId = :userId")
    suspend fun deleteAllHabitsForUser(userId: String)
}

@Dao
interface HabitCompletionDao {
    @Query("SELECT * FROM habit_completions WHERE userId = :userId")
    suspend fun getAllCompletionsForUserOnce(userId: String): List<HabitCompletion>

    @Query("SELECT * FROM habit_completions WHERE userId = :userId")
    fun getAllCompletionsForUser(userId: String): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE userId = :userId AND date = :date")
    fun getCompletionsForDate(userId: String, date: String): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE userId = :userId AND date = :date")
    suspend fun getCompletionsForDateOnce(userId: String, date: String): List<HabitCompletion>

    @Query("SELECT * FROM habit_completions WHERE userId = :userId AND habitId = :habitId")
    fun getCompletionsForHabit(userId: String, habitId: String): Flow<List<HabitCompletion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletions(completions: List<HabitCompletion>)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCompletion(habitId: String, date: String)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    suspend fun deleteAllForHabit(habitId: String)

    @Query("DELETE FROM habit_completions WHERE userId = :userId")
    suspend fun deleteAllCompletionsForUser(userId: String)
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_records WHERE userId = :userId AND date = :date LIMIT 1")
    fun getWaterRecord(userId: String, date: String): Flow<WaterRecord?>

    @Query("SELECT * FROM water_records WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getWaterRecordOnce(userId: String, date: String): WaterRecord?

    @Query("SELECT * FROM water_records WHERE userId = :userId ORDER BY date DESC")
    fun getAllWaterRecords(userId: String): Flow<List<WaterRecord>>

    @Query("SELECT * FROM water_records WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllWaterRecordsOnce(userId: String): List<WaterRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWater(record: WaterRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWaterRecords(records: List<WaterRecord>)

    @Query("DELETE FROM water_records WHERE userId = :userId")
    suspend fun deleteAllWaterForUser(userId: String)

    // Water Entries
    @Query("SELECT * FROM water_entries WHERE userId = :userId AND date = :date ORDER BY timestamp DESC")
    fun getWaterEntriesForDate(userId: String, date: String): Flow<List<WaterEntry>>

    @Query("SELECT * FROM water_entries WHERE userId = :userId AND date = :date ORDER BY timestamp DESC")
    suspend fun getWaterEntriesForDateOnce(userId: String, date: String): List<WaterEntry>

    @Query("SELECT * FROM water_entries WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllWaterEntries(userId: String): Flow<List<WaterEntry>>

    @Query("SELECT * FROM water_entries WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllWaterEntriesOnce(userId: String): List<WaterEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterEntry(entry: WaterEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterEntries(entries: List<WaterEntry>)

    @Update
    suspend fun updateWaterEntry(entry: WaterEntry)

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteWaterEntryById(id: String)

    @Query("DELETE FROM water_entries WHERE userId = :userId")
    suspend fun deleteAllWaterEntriesForUser(userId: String)
}

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions WHERE userId = :userId AND isCompleted = 1 ORDER BY timestamp DESC LIMIT 50")
    fun getRecentFocusSessions(userId: String): Flow<List<FocusSession>>

    @Query("SELECT SUM(completedMinutes) FROM focus_sessions WHERE userId = :userId AND isCompleted = 1")
    fun getTotalFocusMinutes(userId: String): Flow<Int?>

    @Query("SELECT SUM(completedMinutes) FROM focus_sessions WHERE userId = :userId AND isCompleted = 1 AND timestamp >= :startTimestamp AND timestamp < :endTimestamp")
    fun getFocusMinutesForDateRange(userId: String, startTimestamp: Long, endTimestamp: Long): Flow<Int?>

    @Query("SELECT * FROM focus_sessions WHERE userId = :userId AND isCompleted = 1 AND timestamp >= :startTimestamp AND timestamp < :endTimestamp ORDER BY timestamp DESC")
    fun getFocusSessionsForDateRange(userId: String, startTimestamp: Long, endTimestamp: Long): Flow<List<FocusSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSessions(sessions: List<FocusSession>)

    @Query("SELECT * FROM focus_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllFocusSessions(userId: String): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllFocusSessionsOnce(userId: String): List<FocusSession>

    @Query("SELECT * FROM focus_sessions WHERE userId = :userId AND isCompleted = 1 ORDER BY timestamp DESC")
    suspend fun getAllCompletedFocusSessionsOnce(userId: String): List<FocusSession>

    @Query("DELETE FROM focus_sessions WHERE userId = :userId")
    suspend fun deleteAllFocusForUser(userId: String)
}

@Dao
interface NightReviewDao {
    @Query("SELECT * FROM night_reviews WHERE userId = :userId AND date = :date")
    fun getNightReviewForDate(userId: String, date: String): Flow<NightReviewEntry?>

    @Query("SELECT * FROM night_reviews WHERE userId = :userId AND date = :date")
    suspend fun getNightReviewForDateOnce(userId: String, date: String): NightReviewEntry?

    @Query("SELECT * FROM night_reviews WHERE userId = :userId ORDER BY date DESC")
    fun getAllNightReviews(userId: String): Flow<List<NightReviewEntry>>

    @Query("SELECT * FROM night_reviews WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllNightReviewsOnce(userId: String): List<NightReviewEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNightReview(entry: NightReviewEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNightReviews(entries: List<NightReviewEntry>)

    @Query("DELETE FROM night_reviews WHERE userId = :userId AND date = :date")
    suspend fun deleteNightReview(userId: String, date: String)

    @Query("DELETE FROM night_reviews WHERE userId = :userId")
    suspend fun deleteAllNightReviewsForUser(userId: String)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1")
    suspend fun getUnlockedAchievementsOnce(): List<Achievement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    @Update
    suspend fun updateAchievement(achievement: Achievement)

    @Query("DELETE FROM achievements")
    suspend fun clearAllAchievements()
}

@Dao
interface ChallengeAlarmDao {
    @Query("SELECT * FROM challenge_alarms WHERE userId = :userId ORDER BY time ASC")
    fun getAlarmsForUser(userId: String): Flow<List<ChallengeAlarm>>

    @Query("SELECT * FROM challenge_alarms WHERE userId = :userId ORDER BY time ASC")
    suspend fun getAlarmsForUserOnce(userId: String): List<ChallengeAlarm>

    @Query("SELECT * FROM challenge_alarms WHERE id = :id LIMIT 1")
    suspend fun getAlarmById(id: String): ChallengeAlarm?

    @Query("SELECT * FROM challenge_alarms WHERE isEnabled = 1")
    suspend fun getAllEnabledAlarms(): List<ChallengeAlarm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAlarm(alarm: ChallengeAlarm)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarms(alarms: List<ChallengeAlarm>)

    @Delete
    suspend fun deleteAlarm(alarm: ChallengeAlarm)

    @Query("DELETE FROM challenge_alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: String)

    @Query("DELETE FROM challenge_alarms WHERE userId = :userId")
    suspend fun deleteAllAlarmsForUser(userId: String)
}

@Dao
interface ChallengeHistoryDao {
    @Query("SELECT * FROM challenge_history WHERE userId = :userId ORDER BY completedAt DESC")
    fun getHistoryForUser(userId: String): Flow<List<ChallengeHistoryEntry>>

    @Query("SELECT * FROM challenge_history WHERE userId = :userId AND date = :date")
    fun getHistoryForDate(userId: String, date: String): Flow<List<ChallengeHistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: ChallengeHistoryEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<ChallengeHistoryEntry>)

    @Query("DELETE FROM challenge_history WHERE userId = :userId")
    suspend fun deleteAllHistoryForUser(userId: String)
}

