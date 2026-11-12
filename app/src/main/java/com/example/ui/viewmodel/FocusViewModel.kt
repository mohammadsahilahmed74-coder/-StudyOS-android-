package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.focus.FocusRepository
import com.example.data.focus.FocusSession
import com.example.data.focus.FocusStats
import com.example.data.focus.PersistedStopwatchState
import com.example.data.focus.PersistedTimerState
import com.example.data.focus.SavedStopwatchSession
import com.example.data.focus.StopwatchHistoryManager
import com.example.data.focus.FocusLeaderboardRepository
import com.example.data.focus.WeeklyFocusRanking
import com.example.util.AmbientSound
import com.example.util.AmbientSoundPlayer
import com.example.util.AppBlockerManager
import com.example.util.BlockableAppInfo
import com.example.util.BlockedWebsite
import com.example.util.FocusBlockerService
import com.example.util.FocusNotificationManager
import com.example.util.FocusReminderSettings
import com.example.util.WebsiteBlockerManager
import com.example.util.YouTubeShortsBlockerManager
import com.example.util.ShortsBlockerStatus
import com.example.widget.StudyOSStopwatchWidgetProvider
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FocusMainTool {
    TIMER,
    STOPWATCH
}

enum class FocusTimerMode(val title: String, val defaultMinutes: Int) {
    POMODORO("Pomodoro", 25),
    DEEP_WORK("Deep Work", 50),
    SHORT_BREAK("Short Break", 5),
    LONG_BREAK("Long Break", 15),
    CUSTOM("Custom", 25)
}

enum class FocusVisualTheme(
    val id: String,
    val title: String,
    val description: String,
    val primaryColorHex: Long = 0xFF6366F1
) {
    DEFAULT("default", "Minimalist Studio", "Obsidian & Deep Space Navy", 0xFF6366F1),
    FOREST("forest", "Emerald Forest", "Misty Pine & Whispering Woodlands", 0xFF10B981),
    OCEAN("ocean", "Abyssal Ocean", "Deep Pacific & Bioluminescent Waters", 0xFF0EA5E9),
    SPACE("space", "Cosmic Starlight", "Infinite Galactic Violet & Nebula Dust", 0xFF8B5CF6)
}

data class StopwatchLap(
    val lapIndex: Int,
    val lapTimeMillis: Long,
    val splitTimeMillis: Long
)

data class FocusUiState(
    val activeTool: FocusMainTool = FocusMainTool.TIMER,

    // Error and Recovery State
    val isError: Boolean = false,
    val initializationError: String? = null,

    // Visual Background Theme
    val visualTheme: FocusVisualTheme = FocusVisualTheme.DEFAULT,

    // Timer Fields
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val mode: FocusTimerMode = FocusTimerMode.POMODORO,
    val totalDurationSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val customFocusMinutes: Int = 25,
    val customBreakMinutes: Int = 5,

    // Stopwatch Fields
    val isStopwatchRunning: Boolean = false,
    val isStopwatchPaused: Boolean = false,
    val stopwatchElapsedMillis: Long = 0L,
    val stopwatchStartTimeMillis: Long = 0L,
    val stopwatchLaps: List<StopwatchLap> = emptyList(),
    val showSaveStopwatchDialog: Boolean = false,
    val pendingSaveSession: SavedStopwatchSession? = null,
    val savedStopwatchSessions: List<SavedStopwatchSession> = emptyList(),

    // Ambient & UI Fields
    val currentAmbientSound: AmbientSound = AmbientSound.NONE,
    val isMuted: Boolean = false,
    val showCompletionDialog: Boolean = false,
    val lastCompletedMinutes: Int = 0,
    val motivationalMessage: String = "",
    val isFullScreenMode: Boolean = false,

    // App Blocker state
    val isAppBlockerEnabled: Boolean = false,
    val blockedPackages: Set<String> = emptySet(),
    val installedApps: List<BlockableAppInfo> = emptyList(),
    val hasUsagePermission: Boolean = false,
    val hasOverlayPermission: Boolean = false,

    // Website Blocker state
    val isWebsiteBlockerEnabled: Boolean = true,
    val blockedWebsites: List<BlockedWebsite> = emptyList(),
    val hasAccessibilityPermission: Boolean = false,

    // Independent YouTube Shorts Blocker state
    val shortsBlockerStatus: ShortsBlockerStatus = ShortsBlockerStatus(),

    // Notification Reminder state
    val reminderSettings: FocusReminderSettings = FocusReminderSettings(),
    val hasNotificationPermission: Boolean = true,

    // Global Focus Leaderboard state
    val leaderboardRankings: List<WeeklyFocusRanking> = emptyList(),
    val userWeeklyRank: Int = 0,
    val isLeaderboardLoading: Boolean = false
) {
    val progress: Float
        get() = if (totalDurationSeconds > 0) {
            (totalDurationSeconds - remainingSeconds).toFloat() / totalDurationSeconds
        } else 0f

    val formattedTime: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return "%02d:%02d".format(mins, secs)
        }

    val formattedStopwatchTime: String
        get() {
            val totalSeconds = stopwatchElapsedMillis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hundredths = (stopwatchElapsedMillis % 1000) / 10
            return "%02d:%02d.%02d".format(minutes, seconds, hundredths)
        }
}

class FocusViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FocusRepository(application)
    private val leaderboardRepository = FocusLeaderboardRepository(application)
    private val soundPlayer = AmbientSoundPlayer(application)
    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var stopwatchJob: Job? = null
    private var leaderboardJob: Job? = null

    // Real-time observed sessions stream
    val sessions: StateFlow<List<FocusSession>> = repository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _stats = MutableStateFlow(FocusStats())
    val stats: StateFlow<FocusStats> = _stats.asStateFlow()

    private val motivationalMessages = listOf(
        "Outstanding focus! Every minute spent deliberately builds your mastery.",
        "Deep work conquered! Your brain is strengthening high-leverage cognitive pathways.",
        "Flow state achieved. Celebrate this win and take a well-deserved mindful breath.",
        "Consistency is your superpower. Another step closer to academic excellence!",
        "Brilliant session! You just outworked your distractions."
    )

    companion object {
        private const val TAG = "FocusViewModel"
    }

    private var sessionsCollectionJob: Job? = null

    init {
        initializeFocusServices()
    }

    fun retryInitialization() {
        Log.d(TAG, "Manual retry of focus services requested by user.")
        _uiState.value = _uiState.value.copy(isError = false, initializationError = null)
        initializeFocusServices()
    }

    fun resetCorruptedState() {
        try {
            Log.d(TAG, "Resetting corrupted Focus state and reinitializing...")
            val prefs = context.getSharedPreferences("studyos_focus_prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            _uiState.value = FocusUiState()
            initializeFocusServices()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing focus prefs: ${e.message}", e)
        }
    }

    fun setVisualTheme(theme: FocusVisualTheme) {
        _uiState.value = _uiState.value.copy(visualTheme = theme)
        try {
            val prefs = context.getSharedPreferences("studyos_focus_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("visual_theme", theme.name).apply()
            Log.d(TAG, "Visual theme set to ${theme.name}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist visual theme: ${e.message}")
        }
    }

    private fun initializeFocusServices() {
        try {
            Log.d(TAG, "Starting FocusViewModel services initialization...")

            // Stage 0: Restore visual theme
            Log.d(TAG, "Stage 0: Restoring Focus Visual Theme preference...")
            val prefs = context.getSharedPreferences("studyos_focus_prefs", Context.MODE_PRIVATE)
            val savedThemeName = prefs.getString("visual_theme", FocusVisualTheme.DEFAULT.name) ?: FocusVisualTheme.DEFAULT.name
            val restoredTheme = try {
                FocusVisualTheme.valueOf(savedThemeName)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown theme '$savedThemeName', falling back to DEFAULT", e)
                FocusVisualTheme.DEFAULT
            }

            // Stage 1: Timer service initialization
            Log.d(TAG, "Stage 1: Restoring persistent Timer service state...")
            val persistedTimer = repository.getActiveTimerState()
            val restoredMode = try {
                FocusTimerMode.valueOf(persistedTimer.mode)
            } catch (e: Exception) {
                Log.w(TAG, "Invalid timer mode '${persistedTimer.mode}', fallback to POMODORO", e)
                FocusTimerMode.POMODORO
            }
            Log.d(TAG, "Timer state restored: running=${persistedTimer.isRunning}, paused=${persistedTimer.isPaused}, remaining=${persistedTimer.remainingSeconds}s")

            // Stage 2: Stopwatch service initialization
            Log.d(TAG, "Stage 2: Restoring persistent Stopwatch service state...")
            val persistedStopwatch = repository.getActiveStopwatchState()
            val restoredLaps = mutableListOf<StopwatchLap>()
            var prevSplit = 0L
            persistedStopwatch.laps.forEachIndexed { idx, split ->
                val lapTime = split - prevSplit
                prevSplit = split
                restoredLaps.add(StopwatchLap(idx + 1, lapTime, split))
            }
            Log.d(TAG, "Stopwatch state restored: running=${persistedStopwatch.isRunning}, elapsed=${persistedStopwatch.elapsedMillis}ms, laps=${restoredLaps.size}")

            // Stage 3: App Blocker service initialization & permissions
            Log.d(TAG, "Stage 3: Initializing App Blocker service and checking permissions...")
            val usagePerm = AppBlockerManager.hasUsageStatsPermission(context)
            val overlayPerm = AppBlockerManager.hasOverlayPermission(context)
            val savedBlocked = AppBlockerManager.getBlockedPackages(context)
            val isAppBlockerPref = AppBlockerManager.isAppBlockerEnabled(context)
            Log.d(TAG, "App Blocker state: usagePerm=$usagePerm, overlayPerm=$overlayPerm, blockedCount=${savedBlocked.size}, enabled=$isAppBlockerPref")

            // Stage 4: Website Blocker service initialization
            Log.d(TAG, "Stage 4: Initializing Website Blocker service and checking accessibility...")
            val accessibilityPerm = WebsiteBlockerManager.isAccessibilityServiceEnabled(context)
            val savedWebsites = WebsiteBlockerManager.getBlockedWebsites(context)
            val isWebsiteBlockerPref = WebsiteBlockerManager.isWebsiteBlockingActive(context)
            Log.d(TAG, "Website Blocker state: accessibilityPerm=$accessibilityPerm, blockedSitesCount=${savedWebsites.size}, active=$isWebsiteBlockerPref")

            // Stage 5: Notification Reminders initialization
            Log.d(TAG, "Stage 5: Initializing Study Reminder notification settings...")
            val reminderSettings = FocusNotificationManager.getSettings(context)
            val notifPerm = FocusNotificationManager.hasNotificationPermission(context)
            Log.d(TAG, "Notifications state: perm=$notifPerm, dailyReminderEnabled=${reminderSettings.isDailyReminderEnabled}")

            // Stage 5b: Stopwatch History & YouTube Shorts Blocker state
            Log.d(TAG, "Stage 5b: Restoring Stopwatch History and Independent Shorts Blocker state...")
            val savedStopwatchList = try {
                StopwatchHistoryManager.getSavedSessions(context)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load saved stopwatch sessions: ${e.message}")
                emptyList()
            }
            val shortsStatus = try {
                YouTubeShortsBlockerManager.getStatus(context)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load shorts blocker status: ${e.message}")
                ShortsBlockerStatus()
            }

            // Update UI State with healthy initial configuration
            _uiState.value = _uiState.value.copy(
                isError = false,
                initializationError = null,
                visualTheme = restoredTheme,
                isRunning = persistedTimer.isRunning,
                isPaused = persistedTimer.isPaused,
                mode = restoredMode,
                totalDurationSeconds = persistedTimer.initialTotalSeconds,
                remainingSeconds = persistedTimer.remainingSeconds,
                customFocusMinutes = persistedTimer.selectedMinutes,
                customBreakMinutes = persistedTimer.breakMinutes,
                isStopwatchRunning = persistedStopwatch.isRunning,
                isStopwatchPaused = persistedStopwatch.isPaused,
                stopwatchElapsedMillis = persistedStopwatch.elapsedMillis,
                stopwatchStartTimeMillis = persistedStopwatch.sessionStartTimeMillis,
                stopwatchLaps = restoredLaps,
                savedStopwatchSessions = savedStopwatchList,
                shortsBlockerStatus = shortsStatus,
                hasUsagePermission = usagePerm,
                hasOverlayPermission = overlayPerm,
                hasAccessibilityPermission = accessibilityPerm,
                blockedPackages = savedBlocked,
                blockedWebsites = savedWebsites,
                isAppBlockerEnabled = isAppBlockerPref,
                isWebsiteBlockerEnabled = isWebsiteBlockerPref,
                reminderSettings = reminderSettings,
                hasNotificationPermission = notifPerm
            )

            // Stage 6: Resume tickers safely if running
            Log.d(TAG, "Stage 6: Checking active running tickers...")
            val isTimerRunning = persistedTimer.isRunning && !persistedTimer.isPaused && persistedTimer.remainingSeconds > 0
            val isStopwatchRunning = persistedStopwatch.isRunning && !persistedStopwatch.isPaused

            if (isTimerRunning) {
                Log.d(TAG, "Resuming timer ticker from previous state...")
                startTicker()
            }
            if (isStopwatchRunning) {
                Log.d(TAG, "Resuming stopwatch ticker from previous state...")
                startStopwatchTicker(persistedStopwatch.lastStartTimeMillis, persistedStopwatch.elapsedMillis)
            }

            if (isTimerRunning || isStopwatchRunning) {
                activateAppBlockerIfConfigured()
                FocusBlockerService.startService(context)
            } else {
                deactivateAppBlocker()
                FocusBlockerService.stopService(context)
            }

            // Sync widget with restored state
            try {
                StudyOSStopwatchWidgetProvider.updateAllWidgets(context)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update widgets on init: ${e.message}")
            }

            // Stage 7: Sessions and Leaderboard sync
            Log.d(TAG, "Stage 7: Launching background streams for stats, sessions, and leaderboard...")
            observeSessionsStream()
            refreshInstalledApps()
            observeLeaderboardStream()

            Log.d(TAG, "FocusViewModel services initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Exception during FocusViewModel services initialization: ${e.message}", e)
            e.printStackTrace()
            _uiState.value = _uiState.value.copy(
                isError = true,
                initializationError = e.localizedMessage ?: "Failed to initialize Focus Mode services (${e.javaClass.simpleName})"
            )
        }
    }

    private fun observeSessionsStream() {
        sessionsCollectionJob?.cancel()
        sessionsCollectionJob = viewModelScope.launch {
            try {
                sessions.collect { sessionList ->
                    try {
                        val computedStats = repository.calculateStats(sessionList)
                        _stats.value = computedStats

                        val currentUser = try {
                            FirebaseApp.getInstance()
                            Firebase.auth.currentUser
                        } catch (_: Exception) { null }

                        val guestName = context.getSharedPreferences("StudyOS_Prefs", Context.MODE_PRIVATE)
                            .getString("user_name_guest", "Guest User") ?: "Guest User"
                        val displayName = currentUser?.displayName ?: guestName

                        leaderboardRepository.syncUserWeeklyRanking(
                            weeklyMinutes = computedStats.weeklyFocusedMinutes,
                            sessionsCount = computedStats.completedSessionsCount,
                            streakDays = computedStats.currentStreakDays,
                            displayName = displayName
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error calculating stats or syncing weekly ranking: ${e.message}")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Error collecting sessions: ${e.message}")
            }
        }
    }

    fun observeLeaderboardStream() {
        leaderboardJob?.cancel()
        leaderboardJob = viewModelScope.launch {
            try {
                val currentUser = try {
                    FirebaseApp.getInstance()
                    Firebase.auth.currentUser
                } catch (_: Exception) { null }
                val uid = currentUser?.uid ?: "local_guest"
                val guestName = context.getSharedPreferences("StudyOS_Prefs", Context.MODE_PRIVATE)
                    .getString("user_name_guest", "Guest User") ?: "Guest User"
                val displayName = currentUser?.displayName ?: guestName
                val currentStats = _stats.value

                _uiState.value = _uiState.value.copy(isLeaderboardLoading = true)
                leaderboardRepository.observeWeeklyLeaderboard(
                    currentUserId = uid,
                    currentUserName = displayName,
                    currentWeeklyMinutes = currentStats.weeklyFocusedMinutes,
                    currentStreak = currentStats.currentStreakDays,
                    currentSessions = currentStats.completedSessionsCount
                ).collect { rankings ->
                    val myRank = rankings.firstOrNull { it.isCurrentUser }?.rank ?: 0
                    _uiState.value = _uiState.value.copy(
                        leaderboardRankings = rankings,
                        userWeeklyRank = myRank,
                        isLeaderboardLoading = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Error in observeLeaderboardStream: ${e.message}")
                _uiState.value = _uiState.value.copy(isLeaderboardLoading = false)
            }
        }
    }

    fun refreshLeaderboard() {
        observeLeaderboardStream()
    }

    fun setTool(tool: FocusMainTool) {
        _uiState.value = _uiState.value.copy(activeTool = tool)
    }

    fun refreshPermissions() {
        val usagePerm = AppBlockerManager.hasUsageStatsPermission(context)
        val overlayPerm = AppBlockerManager.hasOverlayPermission(context)
        val accessibilityPerm = WebsiteBlockerManager.isAccessibilityServiceEnabled(context)
        val notifPerm = FocusNotificationManager.hasNotificationPermission(context)
        _uiState.value = _uiState.value.copy(
            hasUsagePermission = usagePerm,
            hasOverlayPermission = overlayPerm,
            hasAccessibilityPermission = accessibilityPerm,
            hasNotificationPermission = notifPerm
        )
    }

    fun updatePermissionsForTesting(usageGranted: Boolean = true, overlayGranted: Boolean = true, accessibilityGranted: Boolean = true) {
        _uiState.value = _uiState.value.copy(
            hasUsagePermission = usageGranted,
            hasOverlayPermission = overlayGranted,
            hasAccessibilityPermission = accessibilityGranted
        )
        if (isFocusActive()) {
            activateAppBlockerIfConfigured()
        }
    }

    fun refreshNotificationSettings() {
        val settings = FocusNotificationManager.getSettings(context)
        val notifPerm = FocusNotificationManager.hasNotificationPermission(context)
        _uiState.value = _uiState.value.copy(
            reminderSettings = settings,
            hasNotificationPermission = notifPerm
        )
    }

    fun updateReminderSettings(settings: FocusReminderSettings) {
        FocusNotificationManager.saveSettings(context, settings)
        refreshNotificationSettings()
    }

    fun sendTestDailyReminder() {
        FocusNotificationManager.sendTestDailyReminder(context)
    }

    fun sendTestCompletionAlert() {
        FocusNotificationManager.sendTestCompletionAlert(context)
    }

    fun refreshWebsites() {
        val list = WebsiteBlockerManager.getBlockedWebsites(context)
        val isEnabled = WebsiteBlockerManager.isWebsiteBlockingActive(context)
        _uiState.value = _uiState.value.copy(
            blockedWebsites = list,
            isWebsiteBlockerEnabled = isEnabled
        )
    }

    fun addBlockedWebsite(url: String, isTemporary: Boolean = false, durationMinutes: Int = 30) {
        val success = WebsiteBlockerManager.addBlockedWebsite(context, url, isTemporary, durationMinutes)
        if (success) {
            refreshWebsites()
        }
    }

    fun removeBlockedWebsite(domain: String) {
        WebsiteBlockerManager.removeBlockedWebsite(context, domain)
        refreshWebsites()
    }

    fun setWebsiteBlockerEnabled(enabled: Boolean) {
        WebsiteBlockerManager.setWebsiteBlockingActive(context, enabled)
        _uiState.value = _uiState.value.copy(isWebsiteBlockerEnabled = enabled)
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            try {
                val apps = AppBlockerManager.getInstalledLaunchableApps(context)
                _uiState.value = _uiState.value.copy(installedApps = apps)
            } catch (_: Exception) {}
        }
    }

    fun toggleAppBlocked(packageName: String) {
        val currentBlocked = _uiState.value.blockedPackages.toMutableSet()
        if (currentBlocked.contains(packageName)) {
            currentBlocked.remove(packageName)
        } else {
            currentBlocked.add(packageName)
        }
        AppBlockerManager.saveBlockedPackages(context, currentBlocked)
        _uiState.value = _uiState.value.copy(
            blockedPackages = currentBlocked
        )
        refreshInstalledApps()
        if (isFocusActive()) {
            activateAppBlockerIfConfigured()
        }
    }

    fun setAppBlockerEnabled(enabled: Boolean) {
        AppBlockerManager.setAppBlockerEnabled(context, enabled)
        _uiState.value = _uiState.value.copy(isAppBlockerEnabled = enabled)
        if (!enabled) {
            deactivateAppBlocker()
        } else if (isFocusActive()) {
            activateAppBlockerIfConfigured()
        }
    }

    // -------------------------------------------------------------
    // TIMER LOGIC
    // -------------------------------------------------------------
    fun setMode(mode: FocusTimerMode) {
        if (_uiState.value.isRunning) return
        val minutes = when (mode) {
            FocusTimerMode.POMODORO -> _uiState.value.customFocusMinutes
            FocusTimerMode.DEEP_WORK -> 50
            FocusTimerMode.SHORT_BREAK -> _uiState.value.customBreakMinutes
            FocusTimerMode.LONG_BREAK -> 15
            FocusTimerMode.CUSTOM -> _uiState.value.customFocusMinutes
        }
        val totalSecs = minutes * 60
        _uiState.value = _uiState.value.copy(
            mode = mode,
            totalDurationSeconds = totalSecs,
            remainingSeconds = totalSecs
        )
        persistTimerState()
    }

    fun setCustomDurations(focusMins: Int, breakMins: Int) {
        val safeFocus = focusMins.coerceIn(1, 720) // up to 12 hours (720 minutes)
        val safeBreak = breakMins.coerceIn(1, 120) // up to 2 hours break
        _uiState.value = _uiState.value.copy(
            customFocusMinutes = safeFocus,
            customBreakMinutes = safeBreak
        )
        if (!_uiState.value.isRunning) {
            if (_uiState.value.mode == FocusTimerMode.POMODORO || _uiState.value.mode == FocusTimerMode.CUSTOM || _uiState.value.mode == FocusTimerMode.DEEP_WORK) {
                _uiState.value = _uiState.value.copy(
                    totalDurationSeconds = safeFocus * 60,
                    remainingSeconds = safeFocus * 60
                )
            } else if (_uiState.value.mode == FocusTimerMode.SHORT_BREAK || _uiState.value.mode == FocusTimerMode.LONG_BREAK) {
                _uiState.value = _uiState.value.copy(
                    totalDurationSeconds = safeBreak * 60,
                    remainingSeconds = safeBreak * 60
                )
            }
        }
        persistTimerState()
    }

    fun startSession() {
        if (_uiState.value.isRunning && !_uiState.value.isPaused) return
        _uiState.value = _uiState.value.copy(isRunning = true, isPaused = false)
        startTicker()
        persistTimerState()
        activateBlockingIfConfigured()
        FocusBlockerService.startService(context)
    }

    fun pauseSession() {
        if (!_uiState.value.isRunning) return
        _uiState.value = _uiState.value.copy(isPaused = true)
        timerJob?.cancel()
        timerJob = null
        persistTimerState()
        if (!_uiState.value.isStopwatchRunning || _uiState.value.isStopwatchPaused) {
            deactivateAppBlocker()
            FocusBlockerService.stopService(context)
        }
    }

    fun resumeSession() {
        if (!_uiState.value.isRunning || !_uiState.value.isPaused) return
        _uiState.value = _uiState.value.copy(isPaused = false)
        startTicker()
        persistTimerState()
        activateBlockingIfConfigured()
        FocusBlockerService.startService(context)
    }

    fun resetOrEndSession(recordCurrentAsCompleted: Boolean = false) {
        timerJob?.cancel()
        timerJob = null

        val current = _uiState.value
        val minutesWorked = ((current.totalDurationSeconds - current.remainingSeconds) / 60).coerceAtLeast(0)

        if (recordCurrentAsCompleted && minutesWorked >= 1) {
            viewModelScope.launch {
                repository.recordFocusSession(
                    durationMinutes = minutesWorked,
                    mode = current.mode.name
                )
            }
            if (minutesWorked >= current.reminderSettings.minSessionMinutesForAlert) {
                FocusNotificationManager.notifySessionCompleted(
                    context = context,
                    durationMinutes = minutesWorked,
                    modeTitle = current.mode.title
                )
            }
        }

        val defaultSecs = when (current.mode) {
            FocusTimerMode.POMODORO, FocusTimerMode.CUSTOM -> current.customFocusMinutes * 60
            FocusTimerMode.DEEP_WORK -> 50 * 60
            FocusTimerMode.SHORT_BREAK -> current.customBreakMinutes * 60
            FocusTimerMode.LONG_BREAK -> 15 * 60
        }

        _uiState.value = current.copy(
            isRunning = false,
            isPaused = false,
            remainingSeconds = defaultSecs,
            totalDurationSeconds = defaultSecs,
            isFullScreenMode = false
        )
        repository.clearActiveTimerState()
        soundPlayer.stopSound()

        // If stopwatch isn't actively running, stop App Blocker & Service
        if (!_uiState.value.isStopwatchRunning || _uiState.value.isStopwatchPaused) {
            deactivateAppBlocker()
            FocusBlockerService.stopService(context)
        }
    }

    private fun startTicker() {
        timerJob?.cancel()
        val targetEndTime = System.currentTimeMillis() + (_uiState.value.remainingSeconds * 1000L)

        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                delay(1000L)
                val newRemaining = ((targetEndTime - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
                _uiState.value = _uiState.value.copy(remainingSeconds = newRemaining)
                persistTimerState()
                if (newRemaining <= 0) {
                    onSessionCompleted()
                    break
                }
            }
        }
    }

    private fun onSessionCompleted() {
        timerJob?.cancel()
        timerJob = null
        val durationMins = _uiState.value.totalDurationSeconds / 60
        val modeTitle = _uiState.value.mode.title

        soundPlayer.stopSound()

        viewModelScope.launch {
            repository.recordFocusSession(
                durationMinutes = durationMins,
                mode = _uiState.value.mode.name
            )
        }

        // Notify user with high-priority celebratory notification
        FocusNotificationManager.notifySessionCompleted(
            context = context,
            durationMinutes = durationMins,
            modeTitle = modeTitle
        )

        repository.clearActiveTimerState()

        _uiState.value = _uiState.value.copy(
            isRunning = false,
            isPaused = false,
            showCompletionDialog = true,
            lastCompletedMinutes = durationMins,
            motivationalMessage = motivationalMessages.random(),
            remainingSeconds = _uiState.value.totalDurationSeconds,
            isFullScreenMode = false
        )

        if (!_uiState.value.isStopwatchRunning || _uiState.value.isStopwatchPaused) {
            deactivateAppBlocker()
            FocusBlockerService.stopService(context)
        }
    }

    // -------------------------------------------------------------
    // STOPWATCH LOGIC (Start, Pause, Resume, Stop, Save, Lap, Reset)
    // -------------------------------------------------------------
    fun startStopwatch() {
        if (_uiState.value.isStopwatchRunning && !_uiState.value.isStopwatchPaused) return
        val currentElapsed = _uiState.value.stopwatchElapsedMillis
        val now = System.currentTimeMillis()
        val sessionStart = if (_uiState.value.stopwatchStartTimeMillis > 0L) {
            _uiState.value.stopwatchStartTimeMillis
        } else {
            now
        }

        _uiState.value = _uiState.value.copy(
            isStopwatchRunning = true,
            isStopwatchPaused = false,
            stopwatchStartTimeMillis = sessionStart
        )
        startStopwatchTicker(now, currentElapsed)
        persistStopwatchState(now, currentElapsed, sessionStart)
        activateBlockingIfConfigured()
        FocusBlockerService.startService(context)
        StudyOSStopwatchWidgetProvider.updateAllWidgets(context)
    }

    fun pauseStopwatch() {
        if (!_uiState.value.isStopwatchRunning) return
        stopwatchJob?.cancel()
        stopwatchJob = null
        _uiState.value = _uiState.value.copy(isStopwatchPaused = true)
        persistStopwatchState(0L, _uiState.value.stopwatchElapsedMillis, _uiState.value.stopwatchStartTimeMillis)
        StudyOSStopwatchWidgetProvider.updateAllWidgets(context)
        if (!_uiState.value.isRunning || _uiState.value.isPaused) {
            deactivateAppBlocker()
            FocusBlockerService.stopService(context)
        }
    }

    fun resumeStopwatch() {
        if (!_uiState.value.isStopwatchRunning || !_uiState.value.isStopwatchPaused) return
        val currentElapsed = _uiState.value.stopwatchElapsedMillis
        val now = System.currentTimeMillis()
        val sessionStart = if (_uiState.value.stopwatchStartTimeMillis > 0L) {
            _uiState.value.stopwatchStartTimeMillis
        } else {
            now - currentElapsed
        }

        _uiState.value = _uiState.value.copy(
            isStopwatchPaused = false,
            stopwatchStartTimeMillis = sessionStart
        )
        startStopwatchTicker(now, currentElapsed)
        persistStopwatchState(now, currentElapsed, sessionStart)
        activateBlockingIfConfigured()
        FocusBlockerService.startService(context)
        StudyOSStopwatchWidgetProvider.updateAllWidgets(context)
    }

    fun lapStopwatch() {
        if (!_uiState.value.isStopwatchRunning) return
        val currentSplit = _uiState.value.stopwatchElapsedMillis
        val currentLaps = _uiState.value.stopwatchLaps
        val prevSplit = currentLaps.firstOrNull()?.splitTimeMillis ?: 0L
        val lapTime = currentSplit - prevSplit

        val newLap = StopwatchLap(
            lapIndex = currentLaps.size + 1,
            lapTimeMillis = lapTime,
            splitTimeMillis = currentSplit
        )

        val updatedLaps = listOf(newLap) + currentLaps
        _uiState.value = _uiState.value.copy(stopwatchLaps = updatedLaps)
        persistStopwatchState(
            lastStartTime = if (_uiState.value.isStopwatchPaused) 0L else System.currentTimeMillis(),
            elapsed = currentSplit,
            sessionStartTime = _uiState.value.stopwatchStartTimeMillis
        )
    }

    /**
     * Stop the stopwatch and present the Save Session dialog with full session metadata.
     */
    fun stopStopwatch() {
        if (!_uiState.value.isStopwatchRunning && _uiState.value.stopwatchElapsedMillis == 0L) return
        
        stopwatchJob?.cancel()
        stopwatchJob = null

        val currentElapsed = _uiState.value.stopwatchElapsedMillis
        val now = System.currentTimeMillis()
        val startMillis = if (_uiState.value.stopwatchStartTimeMillis > 0L) {
            _uiState.value.stopwatchStartTimeMillis
        } else {
            (now - currentElapsed).coerceAtLeast(0L)
        }
        val dateFormatted = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(startMillis))

        val candidate = SavedStopwatchSession(
            title = "Deep Work Session",
            dateFormatted = dateFormatted,
            startTimeMillis = startMillis,
            endTimeMillis = now,
            durationMillis = currentElapsed,
            lapsCount = _uiState.value.stopwatchLaps.size,
            subjectTag = "Study"
        )

        _uiState.value = _uiState.value.copy(
            isStopwatchPaused = true,
            pendingSaveSession = candidate,
            showSaveStopwatchDialog = true
        )

        persistStopwatchState(0L, currentElapsed, startMillis)
        StudyOSStopwatchWidgetProvider.updateAllWidgets(context)
        if (!_uiState.value.isRunning || _uiState.value.isPaused) {
            deactivateAppBlocker()
        }
    }

    fun saveStopwatchSession(customTitle: String, subjectTag: String) {
        val candidate = _uiState.value.pendingSaveSession ?: return
        val finalTitle = if (customTitle.isNotBlank()) customTitle.trim() else "Deep Work Session"
        val finalSession = candidate.copy(
            title = finalTitle,
            subjectTag = subjectTag
        )

        // Immediate synchronous local update for zero-latency UI responsiveness
        val currentSessions = _uiState.value.savedStopwatchSessions.toMutableList()
        currentSessions.removeAll { it.id == finalSession.id }
        currentSessions.add(0, finalSession)
        StopwatchHistoryManager.saveSessionsLocally(context, currentSessions)

        // Reset stopwatch after saving
        resetStopwatch(saveAsSession = false)
        _uiState.value = _uiState.value.copy(
            showSaveStopwatchDialog = false,
            pendingSaveSession = null,
            savedStopwatchSessions = currentSessions
        )

        viewModelScope.launch {
            try {
                StopwatchHistoryManager.saveSession(context, finalSession)
                val durationMins = (finalSession.durationMillis / 60000L).toInt().coerceAtLeast(1)
                repository.recordFocusSession(
                    durationMinutes = durationMins,
                    mode = "STOPWATCH_FOCUS",
                    tag = subjectTag
                )
                if (durationMins >= _uiState.value.reminderSettings.minSessionMinutesForAlert) {
                    FocusNotificationManager.notifySessionCompleted(
                        context = context,
                        durationMinutes = durationMins,
                        modeTitle = "Stopwatch: $finalTitle"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving stopwatch session: ${e.message}", e)
            }
        }
    }

    fun dismissSaveStopwatchDialog() {
        _uiState.value = _uiState.value.copy(showSaveStopwatchDialog = false)
    }

    fun discardStopwatchSession() {
        _uiState.value = _uiState.value.copy(
            showSaveStopwatchDialog = false,
            pendingSaveSession = null
        )
        resetStopwatch(saveAsSession = false)
    }

    fun renameStopwatchSession(id: String, newTitle: String) {
        val currentList = _uiState.value.savedStopwatchSessions.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(title = newTitle.trim())
            StopwatchHistoryManager.saveSessionsLocally(context, currentList)
            _uiState.value = _uiState.value.copy(savedStopwatchSessions = currentList)
        }

        viewModelScope.launch {
            try {
                StopwatchHistoryManager.renameSession(context, id, newTitle)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to rename stopwatch session: ${e.message}", e)
            }
        }
    }

    fun deleteStopwatchSession(id: String) {
        val currentList = _uiState.value.savedStopwatchSessions.toMutableList()
        currentList.removeAll { it.id == id }
        StopwatchHistoryManager.saveSessionsLocally(context, currentList)
        _uiState.value = _uiState.value.copy(savedStopwatchSessions = currentList)

        viewModelScope.launch {
            try {
                StopwatchHistoryManager.deleteSession(context, id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete stopwatch session: ${e.message}", e)
            }
        }
    }

    fun resetStopwatch(saveAsSession: Boolean = false) {
        stopwatchJob?.cancel()
        stopwatchJob = null

        val currentElapsed = _uiState.value.stopwatchElapsedMillis
        val minutes = (currentElapsed / 60000).toInt()

        if (saveAsSession && minutes >= 1) {
            viewModelScope.launch {
                repository.recordFocusSession(
                    durationMinutes = minutes,
                    mode = "STOPWATCH_FOCUS"
                )
            }
            if (minutes >= _uiState.value.reminderSettings.minSessionMinutesForAlert) {
                FocusNotificationManager.notifySessionCompleted(
                    context = context,
                    durationMinutes = minutes,
                    modeTitle = "Stopwatch Deep Work"
                )
            }
        }

        _uiState.value = _uiState.value.copy(
            isStopwatchRunning = false,
            isStopwatchPaused = false,
            stopwatchElapsedMillis = 0L,
            stopwatchStartTimeMillis = 0L,
            stopwatchLaps = emptyList(),
            pendingSaveSession = null,
            showSaveStopwatchDialog = false
        )
        repository.clearActiveStopwatchState()
        StudyOSStopwatchWidgetProvider.updateAllWidgets(context)

        if (!_uiState.value.isRunning || _uiState.value.isPaused) {
            deactivateAppBlocker()
            FocusBlockerService.stopService(context)
        }
    }

    private fun startStopwatchTicker(startTime: Long, baseElapsed: Long) {
        stopwatchJob?.cancel()
        stopwatchJob = viewModelScope.launch {
            while (true) {
                delay(30L)
                val newElapsed = baseElapsed + (System.currentTimeMillis() - startTime)
                _uiState.value = _uiState.value.copy(stopwatchElapsedMillis = newElapsed)
                persistStopwatchState(startTime, newElapsed, _uiState.value.stopwatchStartTimeMillis)
            }
        }
    }

    private fun persistStopwatchState(lastStartTime: Long, elapsed: Long, sessionStartTime: Long = 0L) {
        val splits = _uiState.value.stopwatchLaps.map { it.splitTimeMillis }
        repository.saveActiveStopwatchState(
            PersistedStopwatchState(
                isRunning = _uiState.value.isStopwatchRunning,
                isPaused = _uiState.value.isStopwatchPaused,
                elapsedMillis = elapsed,
                lastStartTimeMillis = lastStartTime,
                laps = splits,
                sessionStartTimeMillis = sessionStartTime
            )
        )
    }

    // -------------------------------------------------------------
    // INDEPENDENT YOUTUBE SHORTS BLOCKER CONTROLS
    // -------------------------------------------------------------
    fun refreshShortsBlockerStatus() {
        val status = YouTubeShortsBlockerManager.getStatus(context)
        _uiState.value = _uiState.value.copy(shortsBlockerStatus = status)
    }

    fun toggleShortsBlocker(enabled: Boolean) {
        YouTubeShortsBlockerManager.setBlockerEnabled(context, enabled)
        refreshShortsBlockerStatus()
    }

    fun temporarilyDisableShortsBlocker(minutes: Int) {
        YouTubeShortsBlockerManager.temporarilyDisable(context, minutes)
        refreshShortsBlockerStatus()
    }

    fun cancelShortsBlockerTemporaryDisable() {
        YouTubeShortsBlockerManager.cancelTemporaryDisable(context)
        refreshShortsBlockerStatus()
    }

    fun openShortsBlockerAccessibilitySettings() {
        YouTubeShortsBlockerManager.openAccessibilitySettings(context)
    }

    // -------------------------------------------------------------
    // AUTOMATIC APP BLOCKING CONTROLS (Tied to Focus & Stopwatch)
    // -------------------------------------------------------------
    fun isFocusActive(): Boolean {
        val current = _uiState.value
        return (current.isRunning && !current.isPaused) || (current.isStopwatchRunning && !current.isStopwatchPaused)
    }

    private fun activateAppBlockerIfConfigured() {
        try {
            if (isFocusActive() &&
                _uiState.value.isAppBlockerEnabled &&
                _uiState.value.blockedPackages.isNotEmpty() &&
                _uiState.value.hasUsagePermission
            ) {
                AppBlockerManager.setBlockingActive(context, true)
            } else {
                AppBlockerManager.setBlockingActive(context, false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error activating app blocker: ${e.message}", e)
        }
    }

    private fun deactivateAppBlocker() {
        try {
            AppBlockerManager.setBlockingActive(context, false)
        } catch (e: Exception) {
            Log.w(TAG, "Error deactivating app blocker: ${e.message}")
        }
    }

    private fun activateBlockingIfConfigured() {
        activateAppBlockerIfConfigured()
    }

    private fun deactivateBlocking() {
        deactivateAppBlocker()
    }

    // -------------------------------------------------------------
    // AMBIENT SOUNDS & UI MODALS
    // -------------------------------------------------------------
    fun dismissCompletionDialog() {
        _uiState.value = _uiState.value.copy(showCompletionDialog = false)
    }

    fun setAmbientSound(sound: AmbientSound) {
        _uiState.value = _uiState.value.copy(currentAmbientSound = sound)
        soundPlayer.playSound(sound)
    }

    fun toggleMute() {
        soundPlayer.toggleMute()
        _uiState.value = _uiState.value.copy(isMuted = soundPlayer.isMuted)
    }

    fun toggleFullScreenMode() {
        _uiState.value = _uiState.value.copy(isFullScreenMode = !_uiState.value.isFullScreenMode)
    }

    fun updateGoals(dailyMinutes: Int, weeklyMinutes: Int) {
        repository.saveDailyGoal(dailyMinutes)
        repository.saveWeeklyGoal(weeklyMinutes)
        _stats.value = _stats.value.copy(
            dailyGoalMinutes = dailyMinutes,
            weeklyGoalMinutes = weeklyMinutes
        )
    }

    private fun persistTimerState() {
        val current = _uiState.value
        val targetMillis = if (current.isRunning && !current.isPaused) {
            System.currentTimeMillis() + (current.remainingSeconds * 1000L)
        } else 0L

        repository.saveActiveTimerState(
            PersistedTimerState(
                isRunning = current.isRunning,
                isPaused = current.isPaused,
                mode = current.mode.name,
                initialTotalSeconds = current.totalDurationSeconds,
                remainingSeconds = current.remainingSeconds,
                targetEndTimeMillis = targetMillis,
                selectedMinutes = current.customFocusMinutes,
                breakMinutes = current.customBreakMinutes
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        soundPlayer.stopSound()
        timerJob?.cancel()
        stopwatchJob?.cancel()
        deactivateAppBlocker()
    }
}
