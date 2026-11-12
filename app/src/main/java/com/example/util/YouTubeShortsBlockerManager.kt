package com.example.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.util.Log

data class ShortsBlockerStatus(
    val isEnabled: Boolean = false,
    val isFirstShortAllowed: Boolean = true,
    val isTemporarilyDisabled: Boolean = false,
    val temporaryRemainingMinutes: Int = 0,
    val hasAccessibilityPermission: Boolean = false,
    val totalInterceptedCount: Int = 0
)

object YouTubeShortsBlockerManager {
    private const val TAG = "ShortsBlockerMgr"
    private const val PREFS_NAME = "studyos_youtube_shorts_blocker_prefs"
    private const val KEY_ENABLED = "shorts_blocker_enabled"
    private const val KEY_FIRST_SHORT_ALLOWED = "first_short_allowed_enabled"
    private const val KEY_TEMP_UNBLOCK_UNTIL = "temp_unblock_until_millis"
    private const val KEY_INTERCEPT_COUNT = "total_shorts_intercepted_count"

    private var inShortsFeed = false
    private var shortsViewCount = 0

    fun isBlockerEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, false)
    }

    fun setBlockerEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isFirstShortAllowedEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_FIRST_SHORT_ALLOWED, true) // Default true as requested
    }

    fun setFirstShortAllowedEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FIRST_SHORT_ALLOWED, enabled).apply()
    }

    fun handleShortsInspection(context: Context, isShortsActive: Boolean): Boolean {
        if (!isShortsActive) {
            inShortsFeed = false
            shortsViewCount = 0
            return false
        }

        // Active in Shorts feed
        if (!inShortsFeed) {
            inShortsFeed = true
            shortsViewCount = 1
            // If First Short Allowed is enabled, allow the first short
            if (isFirstShortAllowedEnabled(context)) {
                return false
            } else {
                return true
            }
        }

        // Already in Shorts feed - user scrolled/swiped to second short or beyond
        if (isFirstShortAllowedEnabled(context)) {
            if (shortsViewCount == 1) {
                shortsViewCount++
                return true // Block the second short immediately!
            } else {
                return true // Block subsequent shorts
            }
        } else {
            return true
        }
    }

    fun resetShortsSession() {
        inShortsFeed = false
        shortsViewCount = 0
    }

    fun isTemporarilyDisabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val unblockUntil = prefs.getLong(KEY_TEMP_UNBLOCK_UNTIL, 0L)
        return unblockUntil > 0L && System.currentTimeMillis() < unblockUntil
    }

    fun getTemporaryRemainingMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val unblockUntil = prefs.getLong(KEY_TEMP_UNBLOCK_UNTIL, 0L)
        val diff = unblockUntil - System.currentTimeMillis()
        return if (diff > 0) ((diff / 60_000L).toInt() + 1) else 0
    }

    fun temporarilyDisable(context: Context, durationMinutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val unblockUntil = System.currentTimeMillis() + (durationMinutes * 60_000L)
        prefs.edit().putLong(KEY_TEMP_UNBLOCK_UNTIL, unblockUntil).apply()
    }

    fun cancelTemporaryDisable(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_TEMP_UNBLOCK_UNTIL).apply()
    }

    fun isShortsBlockerActive(context: Context): Boolean {
        if (!isBlockerEnabled(context)) return false
        if (isTemporarilyDisabled(context)) return false
        return true
    }

    fun recordShortIntercepted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_INTERCEPT_COUNT, 0) + 1
        prefs.edit().putInt(KEY_INTERCEPT_COUNT, count).apply()
    }

    fun getInterceptedCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_INTERCEPT_COUNT, 0)
    }

    fun hasAccessibilityPermission(context: Context): Boolean {
        return try {
            val expectedComponentName = "${context.packageName}/${StudyAccessibilityBlockerService::class.java.name}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedComponentName, ignoreCase = true) ||
                    (componentName.contains(context.packageName, ignoreCase = true) &&
                     componentName.contains("StudyAccessibilityBlockerService", ignoreCase = true))
                ) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking accessibility permission: ${e.message}")
            false
        }
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch accessibility settings: ${e.message}")
        }
    }

    fun getStatus(context: Context): ShortsBlockerStatus {
        val enabled = isBlockerEnabled(context)
        val firstAllowed = isFirstShortAllowedEnabled(context)
        val tempDisabled = isTemporarilyDisabled(context)
        val remaining = getTemporaryRemainingMinutes(context)
        val hasPerm = hasAccessibilityPermission(context)
        val count = getInterceptedCount(context)
        return ShortsBlockerStatus(
            isEnabled = enabled,
            isFirstShortAllowed = firstAllowed,
            isTemporarilyDisabled = tempDisabled,
            temporaryRemainingMinutes = remaining,
            hasAccessibilityPermission = hasPerm,
            totalInterceptedCount = count
        )
    }
}
