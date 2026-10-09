package com.example.data.local

import androidx.room.*
import com.example.data.models.AppNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsFlow(userId: String): Flow<List<AppNotification>>

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getNotificationsOnce(userId: String): List<AppNotification>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun getUnreadCountFlow(userId: String): Flow<Int>

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun getNotificationById(id: String): AppNotification?

    @Query("SELECT * FROM notifications WHERE userId = :userId AND scheduledDate = :scheduledDate AND type = :type LIMIT 1")
    suspend fun getNotificationByDateAndType(userId: String, scheduledDate: String, type: String): AppNotification?

    @Query("SELECT * FROM notifications WHERE userId = :userId AND scheduledDate = :scheduledDate AND relatedEntityId = :entityId LIMIT 1")
    suspend fun getNotificationByDateAndEntity(userId: String, scheduledDate: String, entityId: String): AppNotification?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(notification: AppNotification)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<AppNotification>)

    @Query("UPDATE notifications SET isRead = :isRead WHERE id = :id")
    suspend fun updateReadStatus(id: String, isRead: Boolean)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)
}
