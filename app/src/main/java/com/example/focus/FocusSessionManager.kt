package com.example.focus

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import com.example.data.models.FocusSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null
)

/**
 * Manages active Focus session persistence, accurate timestamp tracking,
 * foreground restricted app detection, and launching the related app.
 */
object FocusSessionManager {
    private const val TAG = "FocusSessionManager"
    private const val PREFS_NAME = "dayforge_focus_session_prefs"
    private const val KEY_IS_RUNNING = "is_running"
    private const val KEY_TARGET_END_MILLIS = "target_end_millis"
    private const val KEY_TOTAL_SECONDS = "total_seconds"
    private const val KEY_REMAINING_SECONDS = "remaining_seconds"
    private const val KEY_HABIT_ID = "habit_id"
    private const val KEY_HABIT_TITLE = "habit_title"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_RESTRICTION_ENABLED = "restriction_enabled"
    private const val KEY_RESTRICTED_APPS = "restricted_apps"

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _currentRestrictedAppDetected = MutableStateFlow<String?>(null)
    val currentRestrictedAppDetected: StateFlow<String?> = _currentRestrictedAppDetected.asStateFlow()

    // Cache of installed apps to avoid repeated package manager scans
    private var cachedInstalledApps: List<InstalledAppInfo>? = null

    /**
     * Save active focus session state to SharedPreferences for timestamp survival
     * across background, screen lock, and process interruptions.
     */
    fun saveActiveSession(
        context: Context,
        sessionId: String,
        targetEndTimeMillis: Long,
        totalSeconds: Int,
        habitId: String?,
        habitTitle: String,
        restrictionEnabled: Boolean,
        restrictedApps: List<String>
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_RUNNING, true)
            .putLong(KEY_TARGET_END_MILLIS, targetEndTimeMillis)
            .putInt(KEY_TOTAL_SECONDS, totalSeconds)
            .putString(KEY_HABIT_ID, habitId ?: "")
            .putString(KEY_HABIT_TITLE, habitTitle)
            .putString(KEY_SESSION_ID, sessionId)
            .putBoolean(KEY_RESTRICTION_ENABLED, restrictionEnabled)
            .putStringSet(KEY_RESTRICTED_APPS, restrictedApps.toSet())
            .apply()
        _isSessionActive.value = true
    }

    fun updateRemainingTime(context: Context, remainingSeconds: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_REMAINING_SECONDS, remainingSeconds).apply()
    }

    fun clearActiveSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        _isSessionActive.value = false
        _currentRestrictedAppDetected.value = null
    }

    fun isSessionRunning(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_RUNNING, false)
    }

    fun getTargetEndTimeMillis(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_TARGET_END_MILLIS, 0L)
    }

    fun getTotalSeconds(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_TOTAL_SECONDS, 25 * 60)
    }

    fun getHabitTitle(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_HABIT_TITLE, "Deep Work") ?: "Deep Work"
    }

    fun getHabitId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val h = prefs.getString(KEY_HABIT_ID, null)
        return if (h.isNullOrBlank()) null else h
    }

    fun getRestrictedApps(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_RESTRICTED_APPS, emptySet()) ?: emptySet()
    }

    fun isRestrictionEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_RESTRICTION_ENABLED, false)
    }

    /**
     * Compute remaining seconds strictly from current system timestamp vs target end timestamp.
     */
    fun computeRemainingSeconds(context: Context): Int {
        val target = getTargetEndTimeMillis(context)
        if (target <= 0L) return 0
        val now = System.currentTimeMillis()
        val diff = target - now
        return if (diff <= 0L) 0 else ((diff + 999L) / 1000L).toInt()
    }

    /**
     * Inspect which app is currently in the foreground using UsageStatsManager.
     */
    fun getForegroundAppPackage(context: Context): String? {
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 10000 // look back 10 seconds
            val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
            val event = UsageEvents.Event()
            var lastForegroundApp: String? = null

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val type = event.eventType
                if (type == UsageEvents.Event.ACTIVITY_RESUMED ||
                    type == UsageEvents.Event.MOVE_TO_FOREGROUND
                ) {
                    lastForegroundApp = event.packageName
                }
            }

            if (lastForegroundApp != null) {
                return lastForegroundApp
            }

            // Fallback: check queryUsageStats
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
            stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (e: Exception) {
            Log.w(TAG, "Error checking foreground app: ${e.message}")
            null
        }
    }

    /**
     * Query all launchable applications on the device for the restriction and related app selectors.
     */
    fun getInstalledApps(context: Context, forceRefresh: Boolean = false): List<InstalledAppInfo> {
        if (!forceRefresh && cachedInstalledApps != null) {
            return cachedInstalledApps!!
        }

        return try {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedActivities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launcherIntent, 0)
            }

            val myPackage = context.packageName
            val apps = mutableListOf<InstalledAppInfo>()

            for (resolveInfo in resolvedActivities) {
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg == myPackage) continue // exclude DAYFORGE itself

                val name = resolveInfo.loadLabel(pm).toString()
                val icon = try {
                    resolveInfo.loadIcon(pm)
                } catch (e: Exception) {
                    null
                }
                apps.add(InstalledAppInfo(packageName = pkg, appName = name, icon = icon))
            }

            val result = apps.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
            cachedInstalledApps = result
            result
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching installed apps: ${e.message}")
            emptyList()
        }
    }

    /**
     * Safely attempt to open a related installed app.
     * Returns true if successfully launched, false if not installed or cannot be launched.
     */
    fun launchRelatedApp(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch related app ($packageName): ${e.message}")
            false
        }
    }

    fun setRestrictedAppDetected(packageName: String?) {
        _currentRestrictedAppDetected.value = packageName
    }
}
