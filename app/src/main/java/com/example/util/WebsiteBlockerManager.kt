package com.example.util

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.ui.screens.FocusBlockedActivity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class BlockedWebsite(
    val domain: String,
    val isTemporary: Boolean = false,
    val unblockAtMillis: Long = 0L, // 0 means permanent until manually removed
    val temporaryDurationMinutes: Int = 0
) {
    val isCurrentlyBlocked: Boolean
        get() = if (isTemporary && unblockAtMillis > 0L) {
            System.currentTimeMillis() < unblockAtMillis
        } else {
            true
        }

    val remainingTemporaryMinutes: Int
        get() = if (isTemporary && unblockAtMillis > 0L) {
            ((unblockAtMillis - System.currentTimeMillis()) / 60000L).toInt().coerceAtLeast(1)
        } else 0
}

object WebsiteBlockerManager {
    private const val PREFS_NAME = "studyos_website_blocker_prefs"
    private const val KEY_BLOCKED_WEBSITES = "blocked_websites_json"
    private const val KEY_WEBSITE_BLOCKING_ACTIVE = "website_blocking_active"

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
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
                    componentName.contains(context.packageName, ignoreCase = true) && componentName.contains("StudyAccessibilityBlockerService", ignoreCase = true)) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            android.util.Log.w("WebsiteBlockerManager", "Error checking accessibility service: ${e.message}")
            false
        }
    }

    fun requestAccessibilityPermission(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("WebsiteBlockerManager", "Error launching accessibility settings: ${e.message}", e)
        }
    }

    fun openAppInfoSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("WebsiteBlockerManager", "Error launching app info: ${e.message}", e)
        }
    }

    fun isWebsiteBlockingActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_WEBSITE_BLOCKING_ACTIVE, false)
    }

    fun setWebsiteBlockingActive(context: Context, active: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_WEBSITE_BLOCKING_ACTIVE, active).apply()
    }

    fun isBlockerEnabled(context: Context): Boolean = isWebsiteBlockingActive(context)

    fun setBlockerEnabled(context: Context, enabled: Boolean) = setWebsiteBlockingActive(context, enabled)

    fun getBlockedWebsites(context: Context): List<BlockedWebsite> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_BLOCKED_WEBSITES, "[]") ?: "[]"
        val list = mutableListOf<BlockedWebsite>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val domain = obj.getString("domain")
                val isTemp = obj.optBoolean("isTemporary", false)
                val unblockAt = obj.optLong("unblockAtMillis", 0L)
                val tempMins = obj.optInt("temporaryDurationMinutes", 0)
                list.add(BlockedWebsite(domain, isTemp, unblockAt, tempMins))
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveBlockedWebsites(context: Context, list: List<BlockedWebsite>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("domain", item.domain)
            obj.put("isTemporary", item.isTemporary)
            obj.put("unblockAtMillis", item.unblockAtMillis)
            obj.put("temporaryDurationMinutes", item.temporaryDurationMinutes)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_BLOCKED_WEBSITES, arr.toString()).apply()
    }

    fun addBlockedWebsite(
        context: Context,
        rawUrl: String,
        isTemporary: Boolean = false,
        durationMinutes: Int = 30
    ): Boolean {
        val sanitized = sanitizeDomain(rawUrl)
        if (sanitized.isBlank()) return false

        val existing = getBlockedWebsites(context).toMutableList()
        existing.removeAll { it.domain.equals(sanitized, ignoreCase = true) }

        val unblockAt = if (isTemporary && durationMinutes > 0) {
            System.currentTimeMillis() + (durationMinutes * 60_000L)
        } else {
            0L
        }

        existing.add(
            BlockedWebsite(
                domain = sanitized,
                isTemporary = isTemporary,
                unblockAtMillis = unblockAt,
                temporaryDurationMinutes = durationMinutes
            )
        )
        saveBlockedWebsites(context, existing)
        return true
    }

    fun removeBlockedWebsite(context: Context, domain: String) {
        val sanitized = sanitizeDomain(domain)
        val existing = getBlockedWebsites(context).toMutableList()
        existing.removeAll { it.domain.equals(sanitized, ignoreCase = true) }
        saveBlockedWebsites(context, existing)
    }

    fun sanitizeDomain(url: String): String {
        var clean = url.trim().lowercase(Locale.getDefault())
        if (clean.startsWith("http://")) clean = clean.removePrefix("http://")
        if (clean.startsWith("https://")) clean = clean.removePrefix("https://")
        if (clean.startsWith("www.")) clean = clean.removePrefix("www.")
        if (clean.contains("/")) clean = clean.substringBefore("/")
        if (clean.contains("?")) clean = clean.substringBefore("?")
        if (clean.contains(":")) clean = clean.substringBefore(":")
        return clean.trim()
    }
}

/**
 * High-performance Accessibility Service that inspects browser address bars
 * (Chrome, Firefox, Edge, Opera, Samsung Internet, Brave, etc.) and blocks forbidden URLs.
 */
class StudyAccessibilityBlockerService : AccessibilityService() {

    private var lastInterceptMillis = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val isShortsBlockerActive = YouTubeShortsBlockerManager.isShortsBlockerActive(this)
        val isWebsiteBlockerActive = WebsiteBlockerManager.isWebsiteBlockingActive(this)

        if (!isShortsBlockerActive && !isWebsiteBlockerActive) return

        val source = event.source ?: return
        val packageName = event.packageName?.toString() ?: ""

        // 1. Independent YouTube Shorts Blocker with First Short Allowed in the YouTube native app
        if (packageName == "com.google.android.youtube") {
            val isShortsActive = isShortsActiveInYouTubeApp(source)
            val shouldBlock = isShortsBlockerActive && YouTubeShortsBlockerManager.handleShortsInspection(this, isShortsActive)
            if (shouldBlock) {
                val now = System.currentTimeMillis()
                if (now - lastInterceptMillis > 1200L) {
                    lastInterceptMillis = now
                    YouTubeShortsBlockerManager.recordShortIntercepted(this)
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    val blockIntent = Intent(this, FocusBlockedActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("BLOCKED_DOMAIN", "Shorts blocked! Stay focused.")
                        putExtra("IS_SHORTS", true)
                        putExtra("IS_WEBSITE", false)
                    }
                    startActivity(blockIntent)
                }
                return
            } else if (!isShortsActive) {
                YouTubeShortsBlockerManager.resetShortsSession()
            }
        }

        // 2. Check for YouTube Shorts and general blocked websites in Browsers
        val url = findUrlInNode(source)

        if (isShortsBlockerActive && url != null) {
            val isShortsUrl = url.contains("youtube.com/shorts", ignoreCase = true) ||
                    url.contains("m.youtube.com/shorts", ignoreCase = true) ||
                    url.contains("youtu.be/shorts", ignoreCase = true) ||
                    (url.contains("youtube.com") && url.contains("/shorts"))
            if (isShortsUrl) {
                val now = System.currentTimeMillis()
                if (now - lastInterceptMillis > 1200L) {
                    lastInterceptMillis = now
                    YouTubeShortsBlockerManager.recordShortIntercepted(this)
                    val blockIntent = Intent(this, FocusBlockedActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("BLOCKED_DOMAIN", "youtube.com/shorts")
                        putExtra("IS_SHORTS", true)
                        putExtra("IS_WEBSITE", true)
                    }
                    startActivity(blockIntent)
                }
                return
            }
        }

        // 3. Independent Website Blocker (active 24/7 independently of Focus Mode)
        if (isWebsiteBlockerActive && url != null) {
            val blockedWebsites = WebsiteBlockerManager.getBlockedWebsites(this)
                .filter { it.isCurrentlyBlocked }
            val matchingBlocked = blockedWebsites.firstOrNull { blocked ->
                url.contains(blocked.domain, ignoreCase = true)
            }
            if (matchingBlocked != null) {
                val now = System.currentTimeMillis()
                if (now - lastInterceptMillis > 1500L) {
                    lastInterceptMillis = now
                    val blockIntent = Intent(this, FocusBlockedActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("BLOCKED_DOMAIN", matchingBlocked.domain)
                        putExtra("IS_WEBSITE", true)
                    }
                    startActivity(blockIntent)
                }
            }
        }
    }

    private fun isShortsActiveInYouTubeApp(node: AccessibilityNodeInfo): Boolean {
        // Direct View IDs used by YouTube app for Shorts reels
        val shortsViewIds = listOf(
            "com.google.android.youtube:id/reel_watch_fragment",
            "com.google.android.youtube:id/reel_recycler",
            "com.google.android.youtube:id/reel_player_page_view",
            "com.google.android.youtube:id/reel_player_view",
            "com.google.android.youtube:id/shorts_container",
            "com.google.android.youtube:id/pivot_shorts"
        )
        for (id in shortsViewIds) {
            val found = node.findAccessibilityNodeInfosByViewId(id)
            if (!found.isNullOrEmpty()) return true
        }

        return inspectNodeForShorts(node, 0)
    }

    private fun inspectNodeForShorts(node: AccessibilityNodeInfo, depth: Int): Boolean {
        if (depth > 5) return false
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        if (viewId.contains("reel") || viewId.contains("shorts_container")) {
            return true
        }
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (desc.startsWith("shorts") && desc.contains("selected")) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (inspectNodeForShorts(child, depth + 1)) return true
        }
        return false
    }

    private fun findUrlInNode(node: AccessibilityNodeInfo): String? {
        val browserAddressBarIds = listOf(
            "com.android.chrome:id/url_bar",
            "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
            "com.microsoft.emmx:id/url_bar",
            "com.sec.android.app.sbrowser:id/location_bar_edit_text",
            "com.brave.browser:id/url_bar",
            "com.opera.browser:id/url_field"
        )

        for (id in browserAddressBarIds) {
            val nodes = node.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                val text = nodes[0].text?.toString()
                if (!text.isNullOrBlank()) return text
            }
        }

        // Check if current node is an address input field
        if (node.isEditable && (node.className?.toString()?.contains("EditText", ignoreCase = true) == true)) {
            val text = node.text?.toString()
            if (text != null && (text.contains(".com") || text.contains(".org") || text.contains(".net") || text.contains(".io") || text.contains("http"))) {
                return text
            }
        }

        // Recursive tree inspection for browser URL bar
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findUrlInNode(child)
            if (found != null) return found
        }
        return null
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
    }
}
