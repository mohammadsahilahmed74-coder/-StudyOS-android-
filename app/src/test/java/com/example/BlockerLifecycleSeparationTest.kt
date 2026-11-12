package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.focus.FocusRepository
import com.example.ui.viewmodel.FocusViewModel
import com.example.util.AppBlockerManager
import com.example.util.WebsiteBlockerManager
import com.example.util.YouTubeShortsBlockerManager
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
class BlockerLifecycleSeparationTest {

    private lateinit var application: Application

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        // Reset all blocker preferences before each test
        application.getSharedPreferences("studyos_app_blocker_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        application.getSharedPreferences("studyos_website_blocker_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        application.getSharedPreferences("studyos_youtube_shorts_blocker_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        application.getSharedPreferences("studyos_focus_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun testScenarioA_FocusOn_AppBlockerWorks() {
        // Configure App Blocker
        AppBlockerManager.saveBlockedPackages(application, setOf("com.instagram.android", "com.zhiliaoapp.musically"))
        AppBlockerManager.setAppBlockerEnabled(application, true)

        val viewModel = FocusViewModel(application)
        // Simulate permission granted
        viewModel.updatePermissionsForTesting(usageGranted = true, overlayGranted = true, accessibilityGranted = true)

        // Initially Focus is OFF
        assertFalse(viewModel.isFocusActive())
        assertFalse(AppBlockerManager.isBlockingActive(application))

        // Start Focus Mode
        viewModel.startSession()
        assertTrue(viewModel.isFocusActive())
        assertTrue(viewModel.uiState.value.isRunning)
        assertTrue("App Blocker must be active during active Focus session", AppBlockerManager.isBlockingActive(application))
    }

    @Test
    fun testScenarioB_FocusOff_AppBlockerStops() {
        AppBlockerManager.saveBlockedPackages(application, setOf("com.instagram.android"))
        AppBlockerManager.setAppBlockerEnabled(application, true)

        val viewModel = FocusViewModel(application)
        viewModel.updatePermissionsForTesting(usageGranted = true, overlayGranted = true, accessibilityGranted = true)

        // Turn Focus ON
        viewModel.startSession()
        assertTrue(AppBlockerManager.isBlockingActive(application))

        // End Focus Mode
        viewModel.resetOrEndSession(recordCurrentAsCompleted = false)
        assertFalse(viewModel.isFocusActive())
        assertFalse("App Blocker must stop when Focus Mode ends", AppBlockerManager.isBlockingActive(application))
    }

    @Test
    fun testScenarioB_StopwatchFocusOff_AppBlockerStops() {
        AppBlockerManager.saveBlockedPackages(application, setOf("com.instagram.android"))
        AppBlockerManager.setAppBlockerEnabled(application, true)

        val viewModel = FocusViewModel(application)
        viewModel.updatePermissionsForTesting(usageGranted = true, overlayGranted = true, accessibilityGranted = true)

        // Start Stopwatch
        viewModel.startStopwatch()
        assertTrue(viewModel.isFocusActive())
        assertTrue(AppBlockerManager.isBlockingActive(application))

        // Pause Stopwatch -> App blocker stops
        viewModel.pauseStopwatch()
        assertFalse(viewModel.isFocusActive())
        assertFalse("App Blocker must stop when Stopwatch is paused", AppBlockerManager.isBlockingActive(application))

        // Resume Stopwatch -> App blocker reactivates
        viewModel.resumeStopwatch()
        assertTrue(viewModel.isFocusActive())
        assertTrue("App Blocker must reactivate when Stopwatch resumes", AppBlockerManager.isBlockingActive(application))

        // Stop Stopwatch -> App blocker stops
        viewModel.stopStopwatch()
        assertFalse(AppBlockerManager.isBlockingActive(application))
    }

    @Test
    fun testScenarioC_FocusOff_WebsiteBlockerContinuesWorking() {
        // Enable Website Blocker and add blocked domains
        WebsiteBlockerManager.addBlockedWebsite(application, "reddit.com")
        WebsiteBlockerManager.setWebsiteBlockingActive(application, true)

        val viewModel = FocusViewModel(application)
        // Focus is OFF
        assertFalse(viewModel.isFocusActive())
        assertTrue("Website Blocker must continue working even when Focus is OFF", WebsiteBlockerManager.isWebsiteBlockingActive(application))

        val sites = WebsiteBlockerManager.getBlockedWebsites(application)
        assertEquals(1, sites.size)
        assertEquals("reddit.com", sites[0].domain)
        assertTrue(sites[0].isCurrentlyBlocked)
    }

    @Test
    fun testScenarioD_FocusOff_ShortsBlockerContinuesWorking() {
        YouTubeShortsBlockerManager.setBlockerEnabled(application, true)

        val viewModel = FocusViewModel(application)
        assertFalse(viewModel.isFocusActive())
        assertTrue("Shorts Blocker must continue working when Focus is OFF", YouTubeShortsBlockerManager.isShortsBlockerActive(application))
    }

    @Test
    fun testScenarioE_FocusOnThenOff_WebsiteAndShortsBlockersRemainEnabled() {
        // Setup all blockers
        AppBlockerManager.saveBlockedPackages(application, setOf("com.twitter.android"))
        AppBlockerManager.setAppBlockerEnabled(application, true)
        WebsiteBlockerManager.addBlockedWebsite(application, "twitter.com")
        WebsiteBlockerManager.setWebsiteBlockingActive(application, true)
        YouTubeShortsBlockerManager.setBlockerEnabled(application, true)

        val viewModel = FocusViewModel(application)
        viewModel.updatePermissionsForTesting(usageGranted = true, overlayGranted = true, accessibilityGranted = true)

        // 1. Focus ON
        viewModel.startSession()
        assertTrue(viewModel.isFocusActive())
        assertTrue(AppBlockerManager.isBlockingActive(application))
        assertTrue(WebsiteBlockerManager.isWebsiteBlockingActive(application))
        assertTrue(YouTubeShortsBlockerManager.isShortsBlockerActive(application))

        // 2. Focus OFF (User ends session)
        viewModel.resetOrEndSession(recordCurrentAsCompleted = true)
        assertFalse(viewModel.isFocusActive())

        // Verification: App blocker stops, but Website & Shorts blockers REMAIN ENABLED!
        assertFalse("App Blocker must stop after session ends", AppBlockerManager.isBlockingActive(application))
        assertTrue("Website Blocker MUST remain active after Focus session ends", WebsiteBlockerManager.isWebsiteBlockingActive(application))
        assertTrue("YouTube Shorts Blocker MUST remain active after Focus session ends", YouTubeShortsBlockerManager.isShortsBlockerActive(application))

        // 3. Start Stopwatch and then Reset Stopwatch
        viewModel.startStopwatch()
        assertTrue(AppBlockerManager.isBlockingActive(application))
        viewModel.resetStopwatch()

        // Verification again:
        assertFalse("App Blocker must stop after Stopwatch reset", AppBlockerManager.isBlockingActive(application))
        assertTrue("Website Blocker MUST remain active after Stopwatch reset", WebsiteBlockerManager.isWebsiteBlockingActive(application))
        assertTrue("YouTube Shorts Blocker MUST remain active after Stopwatch reset", YouTubeShortsBlockerManager.isShortsBlockerActive(application))
    }

    @Test
    fun testScenarioF_ApplicationRestart_IndependentBlockerPreferencesRestored() {
        // Set independent blocker preferences
        AppBlockerManager.saveBlockedPackages(application, setOf("com.netflix.mediaclient"))
        AppBlockerManager.setAppBlockerEnabled(application, true)
        WebsiteBlockerManager.addBlockedWebsite(application, "netflix.com")
        WebsiteBlockerManager.setWebsiteBlockingActive(application, true)
        YouTubeShortsBlockerManager.setBlockerEnabled(application, true)

        // Simulate app restart by instantiating a fresh ViewModel
        val restartedViewModel = FocusViewModel(application)

        // Verify preferences are restored
        val state = restartedViewModel.uiState.value
        assertTrue("App Blocker enabled preference must be restored", state.isAppBlockerEnabled)
        assertTrue("Blocked packages list must be preserved", state.blockedPackages.contains("com.netflix.mediaclient"))
        assertTrue("Website Blocker enabled preference must be restored", state.isWebsiteBlockerEnabled)
        assertEquals(1, state.blockedWebsites.size)
        assertEquals("netflix.com", state.blockedWebsites[0].domain)
        assertTrue("YouTube Shorts status must be enabled", state.shortsBlockerStatus.isEnabled)

        // Verify that App Blocker is NOT active on restart since no focus session was active
        assertFalse("App Blocker service should not be active without running session", AppBlockerManager.isBlockingActive(application))
        // Verify Website Blocker and Shorts Blocker ARE active independently
        assertTrue("Website Blocker remains active across restarts", WebsiteBlockerManager.isWebsiteBlockingActive(application))
        assertTrue("Shorts Blocker remains active across restarts", YouTubeShortsBlockerManager.isShortsBlockerActive(application))
    }
}
