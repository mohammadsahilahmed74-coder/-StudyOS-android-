package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.focus.FocusRepository
import com.example.data.focus.PersistedStopwatchState
import com.example.data.focus.SavedStopwatchSession
import com.example.data.focus.StopwatchHistoryManager
import com.example.ui.viewmodel.FocusViewModel
import com.example.util.YouTubeShortsBlockerManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StopwatchAndShortsBlockerTest {

    private lateinit var application: Application

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        application.getSharedPreferences("studyos_focus_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        application.getSharedPreferences("studyos_stopwatch_history_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        application.getSharedPreferences("studyos_youtube_shorts_blocker_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun testSavedStopwatchSessionFormattingAndJson() {
        val now = System.currentTimeMillis()
        val session = SavedStopwatchSession(
            id = UUID.randomUUID().toString(),
            title = "Algorithmic Complexity Sprint",
            dateFormatted = "Wed, Oct 24, 2026",
            startTimeMillis = now - 3600_000L,
            endTimeMillis = now,
            durationMillis = 3600_000L + 125_000L, // 1h 2m 5s
            lapsCount = 3,
            subjectTag = "Coding"
        )

        // Duration formatting check: 1h 2m 5s -> 01:02:05
        assertEquals("01:02:05", session.formattedDuration)

        // JSON serialization / deserialization roundtrip
        val json = session.toJson()
        val parsed = SavedStopwatchSession.fromJson(json)

        assertEquals(session.id, parsed.id)
        assertEquals("Algorithmic Complexity Sprint", parsed.title)
        assertEquals("Wed, Oct 24, 2026", parsed.dateFormatted)
        assertEquals(session.startTimeMillis, parsed.startTimeMillis)
        assertEquals(session.endTimeMillis, parsed.endTimeMillis)
        assertEquals(session.durationMillis, parsed.durationMillis)
        assertEquals(3, parsed.lapsCount)
        assertEquals("Coding", parsed.subjectTag)
    }

    @Test
    fun testStopwatchHistoryManagerCrud() = runBlocking {
        // Initial list is empty
        val initialList = StopwatchHistoryManager.getSavedSessions(application)
        assertTrue(initialList.isEmpty())

        val now = System.currentTimeMillis()
        val session1 = SavedStopwatchSession(
            id = "sess_1",
            title = "Math Calculus",
            dateFormatted = "Today",
            startTimeMillis = now - 1800_000L,
            endTimeMillis = now,
            durationMillis = 1800_000L,
            subjectTag = "Mathematics"
        )
        val session2 = SavedStopwatchSession(
            id = "sess_2",
            title = "Biology Review",
            dateFormatted = "Today",
            startTimeMillis = now - 900_000L,
            endTimeMillis = now,
            durationMillis = 900_000L,
            subjectTag = "Science"
        )

        // Save sessions
        StopwatchHistoryManager.saveSession(application, session1)
        StopwatchHistoryManager.saveSession(application, session2)

        val savedList = StopwatchHistoryManager.getSavedSessions(application)
        assertEquals(2, savedList.size)

        // Rename session 1
        StopwatchHistoryManager.renameSession(application, "sess_1", "Advanced Calculus Problem Set")
        val updatedList = StopwatchHistoryManager.getSavedSessions(application)
        val renamed = updatedList.first { it.id == "sess_1" }
        assertEquals("Advanced Calculus Problem Set", renamed.title)

        // Delete session 2
        StopwatchHistoryManager.deleteSession(application, "sess_2")
        val afterDeleteList = StopwatchHistoryManager.getSavedSessions(application)
        assertEquals(1, afterDeleteList.size)
        assertEquals("sess_1", afterDeleteList.first().id)
    }

    @Test
    fun testYouTubeShortsBlockerManagerControls() {
        // Initially disabled
        assertFalse(YouTubeShortsBlockerManager.isBlockerEnabled(application))
        assertFalse(YouTubeShortsBlockerManager.isShortsBlockerActive(application))

        // Enable
        YouTubeShortsBlockerManager.setBlockerEnabled(application, true)
        assertTrue(YouTubeShortsBlockerManager.isBlockerEnabled(application))
        assertTrue(YouTubeShortsBlockerManager.isShortsBlockerActive(application))

        // Temporary disable for 15 minutes
        YouTubeShortsBlockerManager.temporarilyDisable(application, 15)
        assertTrue(YouTubeShortsBlockerManager.isTemporarilyDisabled(application))
        assertFalse(YouTubeShortsBlockerManager.isShortsBlockerActive(application))
        assertTrue(YouTubeShortsBlockerManager.getTemporaryRemainingMinutes(application) in 14..16)

        // Cancel temporary disable
        YouTubeShortsBlockerManager.cancelTemporaryDisable(application)
        assertFalse(YouTubeShortsBlockerManager.isTemporarilyDisabled(application))
        assertTrue(YouTubeShortsBlockerManager.isShortsBlockerActive(application))

        // Distraction counter
        assertEquals(0, YouTubeShortsBlockerManager.getInterceptedCount(application))
        YouTubeShortsBlockerManager.recordShortIntercepted(application)
        YouTubeShortsBlockerManager.recordShortIntercepted(application)
        assertEquals(2, YouTubeShortsBlockerManager.getInterceptedCount(application))

        // Status object
        val status = YouTubeShortsBlockerManager.getStatus(application)
        assertTrue(status.isEnabled)
        assertFalse(status.isTemporarilyDisabled)
        assertEquals(2, status.totalInterceptedCount)
    }

    @Test
    fun testFocusRepositoryStopwatchPersistence() {
        val repository = FocusRepository(application)
        val now = System.currentTimeMillis()

        val state = PersistedStopwatchState(
            isRunning = true,
            isPaused = true,
            elapsedMillis = 45000L,
            lastStartTimeMillis = 0L,
            laps = listOf(15000L, 30000L),
            sessionStartTimeMillis = now - 50000L
        )

        repository.saveActiveStopwatchState(state)

        val restored = repository.getActiveStopwatchState()
        assertTrue(restored.isRunning)
        assertTrue(restored.isPaused)
        assertEquals(45000L, restored.elapsedMillis)
        assertEquals(2, restored.laps.size)
        assertEquals(now - 50000L, restored.sessionStartTimeMillis)

        repository.clearActiveStopwatchState()
        val cleared = repository.getActiveStopwatchState()
        assertFalse(cleared.isRunning)
        assertEquals(0L, cleared.elapsedMillis)
    }

    @Test
    fun testFocusViewModelStopwatchStartPauseResumeStopSaveFlow() {
        val viewModel = FocusViewModel(application)

        // Start Stopwatch
        viewModel.startStopwatch()
        var state = viewModel.uiState.value
        assertTrue(state.isStopwatchRunning)
        assertFalse(state.isStopwatchPaused)
        assertTrue(state.stopwatchStartTimeMillis > 0L)

        // Pause Stopwatch
        viewModel.pauseStopwatch()
        state = viewModel.uiState.value
        assertTrue(state.isStopwatchRunning)
        assertTrue(state.isStopwatchPaused)

        // Resume Stopwatch
        viewModel.resumeStopwatch()
        state = viewModel.uiState.value
        assertTrue(state.isStopwatchRunning)
        assertFalse(state.isStopwatchPaused)

        // Stop Stopwatch -> Triggers Save Session Dialog
        viewModel.stopStopwatch()
        state = viewModel.uiState.value
        assertTrue(state.showSaveStopwatchDialog)
        assertNotNull(state.pendingSaveSession)

        // Save session
        viewModel.saveStopwatchSession("Operating Systems Study", "Computer Science")
        state = viewModel.uiState.value
        assertFalse(state.showSaveStopwatchDialog)
        assertFalse(state.isStopwatchRunning)
        assertEquals(0L, state.stopwatchElapsedMillis)
        assertEquals(1, state.savedStopwatchSessions.size)
        assertEquals("Operating Systems Study", state.savedStopwatchSessions.first().title)
        assertEquals("Computer Science", state.savedStopwatchSessions.first().subjectTag)
    }
}
