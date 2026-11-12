package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.WebsiteBlockerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WebsiteBlockerManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("studyos_website_blocker_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testDomainSanitization() {
        assertEquals("youtube.com", WebsiteBlockerManager.sanitizeDomain("https://www.youtube.com/watch?v=123"))
        assertEquals("instagram.com", WebsiteBlockerManager.sanitizeDomain("http://instagram.com/feed"))
        assertEquals("reddit.com", WebsiteBlockerManager.sanitizeDomain("www.reddit.com/r/all"))
        assertEquals("twitter.com", WebsiteBlockerManager.sanitizeDomain("   twitter.com/home   "))
        assertEquals("twitch.tv", WebsiteBlockerManager.sanitizeDomain("https://twitch.tv:443/directory"))
    }

    @Test
    fun testAddAndRemoveBlockedWebsite() {
        // Add permanent website
        val added = WebsiteBlockerManager.addBlockedWebsite(context, "youtube.com", isTemporary = false)
        assertTrue(added)

        var list = WebsiteBlockerManager.getBlockedWebsites(context)
        assertEquals(1, list.size)
        assertEquals("youtube.com", list[0].domain)
        assertFalse(list[0].isTemporary)
        assertTrue(list[0].isCurrentlyBlocked)

        // Add temporary website
        WebsiteBlockerManager.addBlockedWebsite(context, "https://instagram.com", isTemporary = true, durationMinutes = 45)
        list = WebsiteBlockerManager.getBlockedWebsites(context)
        assertEquals(2, list.size)

        val insta = list.first { it.domain == "instagram.com" }
        assertTrue(insta.isTemporary)
        assertTrue(insta.isCurrentlyBlocked)
        assertTrue(insta.remainingTemporaryMinutes in 44..45)

        // Remove website
        WebsiteBlockerManager.removeBlockedWebsite(context, "youtube.com")
        list = WebsiteBlockerManager.getBlockedWebsites(context)
        assertEquals(1, list.size)
        assertEquals("instagram.com", list[0].domain)
    }

    @Test
    fun testWebsiteBlockingActiveToggle() {
        assertFalse(WebsiteBlockerManager.isWebsiteBlockingActive(context))

        WebsiteBlockerManager.setWebsiteBlockingActive(context, true)
        assertTrue(WebsiteBlockerManager.isWebsiteBlockingActive(context))

        WebsiteBlockerManager.setWebsiteBlockingActive(context, false)
        assertFalse(WebsiteBlockerManager.isWebsiteBlockingActive(context))
    }
}
