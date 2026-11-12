package com.example.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

data class BlockableAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isBlocked: Boolean = false
)

object AppBlockerManager {
    private const val PREFS_NAME = "studyos_app_blocker_prefs"
    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
    private const val KEY_BLOCKING_ACTIVE = "blocking_active"
    private const val KEY_APP_BLOCKER_ENABLED = "app_blocker_enabled"

    fun isAppBlockerEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_APP_BLOCKER_ENABLED, true)
    }

    fun setAppBlockerEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_APP_BLOCKER_ENABLED, enabled).apply()
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            android.util.Log.w("AppBlockerManager", "Error checking usage stats permission: ${e.message}")
            false
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return try {
            Settings.canDrawOverlays(context)
        } catch (e: Exception) {
            android.util.Log.w("AppBlockerManager", "Error checking overlay permission: ${e.message}")
            false
        }
    }

    fun requestUsageStatsPermission(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("AppBlockerManager", "Error launching usage access settings: ${e.message}", e)
        }
    }

    fun requestOverlayPermission(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("AppBlockerManager", "Error launching overlay settings: ${e.message}", e)
        }
    }

    fun getBlockedPackages(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_BLOCKED_PACKAGES, emptySet()) ?: emptySet()
    }

    fun saveBlockedPackages(context: Context, packages: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_BLOCKED_PACKAGES, packages).apply()
    }

    fun isBlockingActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BLOCKING_ACTIVE, false)
    }

    fun setBlockingActive(context: Context, active: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BLOCKING_ACTIVE, active).apply()
        if (active) {
            FocusBlockerService.startService(context)
        } else {
            FocusBlockerService.stopService(context)
        }
    }

    /**
     * Lists non-system or popular user-installed apps with launcher activities
     */
    fun getInstalledLaunchableApps(context: Context): List<BlockableAppInfo> {
        return try {
            val pm = context.packageManager ?: return emptyList()
            val blocked = getBlockedPackages(context)
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val myPackage = context.packageName

            resolveInfos.mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val pkg = activityInfo.packageName ?: return@mapNotNull null
                if (pkg == myPackage) return@mapNotNull null

                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)
                    BlockableAppInfo(
                        packageName = pkg,
                        appName = appName,
                        icon = icon,
                        isBlocked = blocked.contains(pkg)
                    )
                } catch (_: Exception) {
                    null
                }
            }.distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            android.util.Log.e("AppBlockerManager", "Error querying installed apps: ${e.message}", e)
            emptyList()
        }
    }
}
