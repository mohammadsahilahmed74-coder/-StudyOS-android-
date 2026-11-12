package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.focus.WeeklyFocusRanking
import com.example.data.focus.WeekKeyUtils
import androidx.compose.material.icons.filled.Search
import com.example.ui.viewmodel.FocusMainTool
import com.example.ui.viewmodel.FocusTimerMode
import com.example.ui.viewmodel.FocusVisualTheme
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.StopwatchLap
import com.example.util.AmbientSound
import com.example.util.AppBlockerManager
import com.example.util.BlockableAppInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FocusScreen(
    focusViewModel: FocusViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by focusViewModel.uiState.collectAsStateWithLifecycle()
    val stats by focusViewModel.stats.collectAsStateWithLifecycle()
    val sessions by focusViewModel.sessions.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSoundMenu by remember { mutableStateOf(false) }
    var showAppBlockerDialog by remember { mutableStateOf(false) }
    var showWebsiteBlockerDialog by remember { mutableStateOf(false) }
    var showShortsBlockerDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showLeaderboardDialog by remember { mutableStateOf(false) }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        focusViewModel.refreshPermissions()
        focusViewModel.refreshNotificationSettings()
    }

    // Re-check permissions on resume (when returning from System Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                focusViewModel.refreshPermissions()
                focusViewModel.refreshInstalledApps()
                focusViewModel.refreshWebsites()
                focusViewModel.refreshNotificationSettings()
                focusViewModel.refreshShortsBlockerStatus()
                focusViewModel.refreshLeaderboard()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Full-screen distraction-free mode
    if (uiState.isFullScreenMode) {
        DistractionFreeFocusView(
            uiState = uiState,
            onPause = { focusViewModel.pauseSession() },
            onResume = { focusViewModel.resumeSession() },
            onEnd = { focusViewModel.resetOrEndSession(recordCurrentAsCompleted = true) },
            onPauseStopwatch = { focusViewModel.pauseStopwatch() },
            onResumeStopwatch = { focusViewModel.resumeStopwatch() },
            onLapStopwatch = { focusViewModel.lapStopwatch() },
            onResetStopwatch = { focusViewModel.resetStopwatch(saveAsSession = true) },
            onToggleMute = { focusViewModel.toggleMute() },
            onExitFullScreen = { focusViewModel.toggleFullScreenMode() }
        )
        return
    }

    val themeBackgroundBrush = getThemeBackgroundBrush(uiState.visualTheme)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeBackgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("focus_screen_root"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error Recovery State Card (displayed when an exception was captured on startup)
            if (uiState.isError || uiState.initializationError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("focus_error_recovery_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEE2E2),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = "Warning",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Focus Mode Recovery State",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                                Text(
                                    text = "A background focus service encountered an error on startup. StudyOS safely caught it so you can continue working.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF7F1D1D)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.initializationError ?: "Service initialization failure captured.",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFB91C1C),
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { focusViewModel.retryInitialization() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("focus_retry_initialization_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Services")
                            }

                            OutlinedButton(
                                onClick = { focusViewModel.resetCorruptedState() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("focus_reset_corrupted_state_button")
                            ) {
                                Text("Reset State", color = Color(0xFF991B1B))
                            }
                        }
                    }
                }
            }

            // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Regain Focus",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Enter a distraction-free flow state",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // App Blocker Icon
                IconButton(
                    onClick = { showAppBlockerDialog = true },
                    modifier = Modifier.testTag("focus_app_blocker_button")
                ) {
                    Icon(
                        imageVector = if (uiState.isAppBlockerEnabled && uiState.blockedPackages.isNotEmpty()) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "App Blocker",
                        tint = if (uiState.isAppBlockerEnabled && uiState.blockedPackages.isNotEmpty())
                            Color(0xFFEF4444)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Website Blocker Icon
                IconButton(
                    onClick = { showWebsiteBlockerDialog = true },
                    modifier = Modifier.testTag("focus_website_blocker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Website Blocker",
                        tint = if (uiState.isWebsiteBlockerEnabled && uiState.blockedWebsites.any { it.isCurrentlyBlocked })
                            Color(0xFF6366F1)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Ambient Sound Menu Button
                Box {
                    IconButton(
                        onClick = { showSoundMenu = true },
                        modifier = Modifier.testTag("focus_ambient_sound_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isMuted) Icons.Default.MusicOff else Icons.Default.MusicNote,
                            contentDescription = "Ambient Sounds",
                            tint = if (uiState.currentAmbientSound != AmbientSound.NONE && !uiState.isMuted)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showSoundMenu,
                        onDismissRequest = { showSoundMenu = false }
                    ) {
                        AmbientSound.values().forEach { sound ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(sound.displayName)
                                        if (uiState.currentAmbientSound == sound) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                },
                                onClick = {
                                    focusViewModel.setAmbientSound(sound)
                                    showSoundMenu = false
                                }
                            )
                        }
                    }
                }

                // Full Screen Distraction-Free Toggle
                IconButton(
                    onClick = { focusViewModel.toggleFullScreenMode() },
                    modifier = Modifier.testTag("focus_fullscreen_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Distraction-Free Mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Study Reminders Notification Icon
                IconButton(
                    onClick = { showReminderDialog = true },
                    modifier = Modifier.testTag("focus_reminder_settings_button")
                ) {
                    Icon(
                        imageVector = if (uiState.reminderSettings.isDailyReminderEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                        contentDescription = "Study Reminders",
                        tint = if (uiState.reminderSettings.isDailyReminderEnabled)
                            Color(0xFFF59E0B)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Global Focus Leaderboard Icon
                IconButton(
                    onClick = { showLeaderboardDialog = true },
                    modifier = Modifier.testTag("focus_leaderboard_top_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Global Focus Leaderboard",
                        tint = Color(0xFFF59E0B)
                    )
                }

                // Focus Visual Environment Theme Switcher
                IconButton(
                    onClick = { showThemeDialog = true },
                    modifier = Modifier.testTag("focus_theme_selector_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Visual Themes",
                        tint = Color(uiState.visualTheme.primaryColorHex)
                    )
                }

                // Duration / Goals Settings
                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier.testTag("focus_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Focus Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Active App Blocker Status Pill
        if (uiState.isAppBlockerEnabled && uiState.blockedPackages.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEF2F2),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAppBlockerDialog = true }
                    .testTag("app_blocker_active_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "App Blocker Active (${uiState.blockedPackages.size} apps blocked during session)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF991B1B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text("Configure", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active Website Blocker Status Pill
        if (uiState.isWebsiteBlockerEnabled && uiState.blockedWebsites.any { it.isCurrentlyBlocked }) {
            val blockedCount = uiState.blockedWebsites.count { it.isCurrentlyBlocked }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEEF2FF),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWebsiteBlockerDialog = true }
                    .testTag("website_blocker_active_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Website Blocker Active ($blockedCount websites restricted)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF3730A3),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text("Manage", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Daily Study Reminder Banner
        if (uiState.reminderSettings.isDailyReminderEnabled) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEF3C7),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showReminderDialog = true }
                    .testTag("focus_reminder_active_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daily Reminder: ${uiState.reminderSettings.formattedPrimaryTime}${if (uiState.reminderSettings.isSecondaryReminderEnabled) " & ${uiState.reminderSettings.formattedSecondaryTime}" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text("Change", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Independent YouTube Shorts Blocker Banner
        if (uiState.shortsBlockerStatus.isEnabled) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEE2E2),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showShortsBlockerDialog = true }
                    .testTag("shorts_blocker_active_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.LockClock, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.shortsBlockerStatus.isTemporarilyDisabled)
                                "Shorts Blocker: Paused (${uiState.shortsBlockerStatus.temporaryRemainingMinutes}m left)"
                            else
                                "YouTube Shorts Blocker Active (24/7 Independent)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF991B1B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text("Configure", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Feature 1: Tool Selector Tabs: TIMER vs STOPWATCH
        TabRow(
            selectedTabIndex = if (uiState.activeTool == FocusMainTool.TIMER) 0 else 1,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        ) {
            Tab(
                selected = uiState.activeTool == FocusMainTool.TIMER,
                onClick = { focusViewModel.setTool(FocusMainTool.TIMER) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Timer", fontWeight = FontWeight.SemiBold)
                    }
                },
                modifier = Modifier.testTag("tab_focus_timer")
            )
            Tab(
                selected = uiState.activeTool == FocusMainTool.STOPWATCH,
                onClick = { focusViewModel.setTool(FocusMainTool.STOPWATCH) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stopwatch", fontWeight = FontWeight.SemiBold)
                    }
                },
                modifier = Modifier.testTag("tab_focus_stopwatch")
            )
        }

        // Main Tool Content
        if (uiState.activeTool == FocusMainTool.TIMER) {
            // Timer Modes (Pomodoro, Deep Work, Short Break, Long Break, Custom)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FocusTimerMode.values().forEach { mode ->
                        val isSelected = uiState.mode == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !uiState.isRunning) { focusViewModel.setMode(mode) }
                                .testTag("focus_mode_${mode.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Circular Focus Timer Visual
            CircularFocusTimer(
                formattedTime = uiState.formattedTime,
                progress = uiState.progress,
                modeTitle = uiState.mode.title,
                isRunning = uiState.isRunning,
                isPaused = uiState.isPaused
            )

            // Timer Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!uiState.isRunning) {
                    Button(
                        onClick = { focusViewModel.startSession() },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .height(54.dp)
                            .width(190.dp)
                            .testTag("focus_start_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Focus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                } else {
                    if (uiState.isPaused) {
                        Button(
                            onClick = { focusViewModel.resumeSession() },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("focus_resume_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume")
                        }
                    } else {
                        Button(
                            onClick = { focusViewModel.pauseSession() },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("focus_pause_button")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause")
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    OutlinedButton(
                        onClick = { focusViewModel.resetOrEndSession(recordCurrentAsCompleted = true) },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("focus_end_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("End & Save")
                    }
                }
            }
        } else {
            // STOPWATCH TOOL VIEW (Start, Pause, Resume, Stop, Save, Lap, Reset, History)
            StopwatchDisplayView(
                formattedTime = uiState.formattedStopwatchTime,
                isRunning = uiState.isStopwatchRunning,
                isPaused = uiState.isStopwatchPaused,
                laps = uiState.stopwatchLaps,
                savedSessions = uiState.savedStopwatchSessions,
                onStart = { focusViewModel.startStopwatch() },
                onPause = { focusViewModel.pauseStopwatch() },
                onResume = { focusViewModel.resumeStopwatch() },
                onStop = { focusViewModel.stopStopwatch() },
                onLap = { focusViewModel.lapStopwatch() },
                onReset = { focusViewModel.resetStopwatch(saveAsSession = false) },
                onRenameSession = { id, title -> focusViewModel.renameStopwatchSession(id, title) },
                onDeleteSession = { id -> focusViewModel.deleteStopwatchSession(id) }
            )
        }

        // Daily & Weekly Goals Progress Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("focus_goals_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Focus Progress & Streak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔥 ${stats.currentStreakDays} Day Streak", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val dailyProgress = if (stats.dailyGoalMinutes > 0) {
                    (stats.todayFocusedMinutes.toFloat() / stats.dailyGoalMinutes).coerceIn(0f, 1f)
                } else 0f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Daily Goal (${stats.todayFocusedMinutes} / ${stats.dailyGoalMinutes} min)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Text("${(dailyProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { dailyProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                val weeklyProgress = if (stats.weeklyGoalMinutes > 0) {
                    (stats.weeklyFocusedMinutes.toFloat() / stats.weeklyGoalMinutes).coerceIn(0f, 1f)
                } else 0f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Weekly Goal (${stats.weeklyFocusedMinutes} / ${stats.weeklyGoalMinutes} min)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Text("${(weeklyProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { weeklyProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = Color(0xFF10B981),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // Achievements Unlocked
        if (stats.unlockedAchievements.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Achievements Unlocked", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(stats.unlockedAchievements) { badge ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⭐ ", fontSize = 12.sp)
                                    Text(badge, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Global Focus Weekly Leaderboard Section
        GlobalFocusLeaderboardCard(
            rankings = uiState.leaderboardRankings,
            userRank = uiState.userWeeklyRank,
            weeklyMinutes = stats.weeklyFocusedMinutes,
            onOpenFull = { showLeaderboardDialog = true }
        )

        // Recent Sessions History
        Card(
            modifier = Modifier.fillMaxWidth().testTag("focus_history_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Focus Session History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (sessions.isEmpty()) {
                    Text(
                        text = "No focus sessions recorded yet. Start your first session above!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
                    sessions.take(6).forEach { session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(session.mode.replace("_", " "), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text(dateFormat.format(Date(session.completedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "+${session.durationMinutes} min",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // APP BLOCKER CONFIGURATION DIALOG
    // -------------------------------------------------------------
    if (showAppBlockerDialog) {
        AppBlockerModalDialog(
            isBlockerEnabled = uiState.isAppBlockerEnabled,
            hasUsagePerm = uiState.hasUsagePermission,
            hasOverlayPerm = uiState.hasOverlayPermission,
            blockedPackages = uiState.blockedPackages,
            installedApps = uiState.installedApps,
            onToggleEnabled = { focusViewModel.setAppBlockerEnabled(it) },
            onToggleApp = { focusViewModel.toggleAppBlocked(it) },
            onRequestUsagePerm = { AppBlockerManager.requestUsageStatsPermission(context) },
            onRequestOverlayPerm = { AppBlockerManager.requestOverlayPermission(context) },
            onDismiss = { showAppBlockerDialog = false }
        )
    }

    // -------------------------------------------------------------
    // WEBSITE BLOCKER CONFIGURATION DIALOG
    // -------------------------------------------------------------
    if (showWebsiteBlockerDialog) {
        WebsiteBlockerModalDialog(
            isBlockerEnabled = uiState.isWebsiteBlockerEnabled,
            hasAccessibilityPerm = uiState.hasAccessibilityPermission,
            blockedWebsites = uiState.blockedWebsites,
            onToggleEnabled = { focusViewModel.setWebsiteBlockerEnabled(it) },
            onAddWebsite = { domain, isTemp, duration ->
                focusViewModel.addBlockedWebsite(domain, isTemp, duration)
            },
            onRemoveWebsite = { domain ->
                focusViewModel.removeBlockedWebsite(domain)
            },
            onRequestAccessibilityPerm = {
                com.example.util.WebsiteBlockerManager.requestAccessibilityPermission(context)
            },
            onOpenAppInfo = {
                com.example.util.WebsiteBlockerManager.openAppInfoSettings(context)
            },
            onDismiss = { showWebsiteBlockerDialog = false }
        )
    }

    // -------------------------------------------------------------
    // STUDY REMINDER & COMPLETION NOTIFICATIONS DIALOG
    // -------------------------------------------------------------
    if (showReminderDialog) {
        FocusReminderModalDialog(
            settings = uiState.reminderSettings,
            hasNotificationPerm = uiState.hasNotificationPermission,
            onRequestPerm = {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    notificationPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onSaveSettings = { newSettings ->
                focusViewModel.updateReminderSettings(newSettings)
            },
            onSendTestReminder = { focusViewModel.sendTestDailyReminder() },
            onSendTestCompletion = { focusViewModel.sendTestCompletionAlert() },
            onDismiss = { showReminderDialog = false }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        var focusMins by remember { mutableIntStateOf(uiState.customFocusMinutes) }
        var breakMins by remember { mutableIntStateOf(uiState.customBreakMinutes) }
        var dailyGoal by remember { mutableIntStateOf(stats.dailyGoalMinutes) }
        var weeklyGoal by remember { mutableIntStateOf(stats.weeklyGoalMinutes) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Customize Focus & Goals", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val focusHours = focusMins / 60
                    val focusRemMins = focusMins % 60
                    val focusDisplay = if (focusHours > 0) "$focusHours h ${if (focusRemMins > 0) "$focusRemMins m" else ""}" else "$focusMins mins"
                    Text("Focus Session Duration: $focusMins minutes ($focusDisplay)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Slider(
                        value = focusMins.toFloat(),
                        onValueChange = { focusMins = it.toInt() },
                        valueRange = 1f..720f,
                        steps = 71
                    )

                    Text("Short Break Duration: $breakMins minutes", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Slider(
                        value = breakMins.toFloat(),
                        onValueChange = { breakMins = it.toInt() },
                        valueRange = 2f..30f,
                        steps = 13
                    )

                    Text("Daily Focus Goal: $dailyGoal minutes", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Slider(
                        value = dailyGoal.toFloat(),
                        onValueChange = { dailyGoal = it.toInt() },
                        valueRange = 30f..360f,
                        steps = 10
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusViewModel.setCustomDurations(focusMins, breakMins)
                        focusViewModel.updateGoals(dailyGoal, weeklyGoal)
                        showSettingsDialog = false
                    }
                ) {
                    Text("Save Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Completion Celebration Dialog
    if (uiState.showCompletionDialog) {
        AlertDialog(
            onDismissRequest = { focusViewModel.dismissCompletionDialog() },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFD1FAE5),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🎉", fontSize = 28.sp)
                    }
                }
            },
            title = {
                Text(
                    text = "Session Completed!",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "+${uiState.lastCompletedMinutes} minutes of focused deep work logged.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF059669)
                    )
                    Text(
                        text = uiState.motivationalMessage,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { focusViewModel.dismissCompletionDialog() },
                    modifier = Modifier.testTag("focus_completion_dismiss_button")
                ) {
                    Text("Awesome, Continue")
                }
            }
        )
    }

    // Global Focus Leaderboard Dialog
    if (showLeaderboardDialog) {
        GlobalLeaderboardModalDialog(
            rankings = uiState.leaderboardRankings,
            userRank = uiState.userWeeklyRank,
            isLoading = uiState.isLeaderboardLoading,
            onRefresh = { focusViewModel.refreshLeaderboard() },
            onDismiss = { showLeaderboardDialog = false }
        )
    }

    // Focus Environment Theme Selector Dialog
    if (showThemeDialog) {
        ThemeSelectorModalDialog(
            currentTheme = uiState.visualTheme,
            onSelectTheme = { focusViewModel.setVisualTheme(it) },
            onDismiss = { showThemeDialog = false }
        )
    }

    // Save Stopwatch Session Review Dialog
    if (uiState.showSaveStopwatchDialog && uiState.pendingSaveSession != null) {
        SaveStopwatchSessionDialog(
            session = uiState.pendingSaveSession!!,
            onSave = { title, tag -> focusViewModel.saveStopwatchSession(title, tag) },
            onDiscard = { focusViewModel.discardStopwatchSession() },
            onDismiss = { focusViewModel.dismissSaveStopwatchDialog() }
        )
    }

    // Independent YouTube Shorts Blocker Configuration Dialog
    if (showShortsBlockerDialog) {
        AlertDialog(
            onDismissRequest = { showShortsBlockerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = null,
                        tint = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("YouTube Shorts Blocker", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                YouTubeShortsBlockerCard(
                    status = uiState.shortsBlockerStatus,
                    onToggleEnabled = { focusViewModel.toggleShortsBlocker(it) },
                    onTemporarilyDisable = { focusViewModel.temporarilyDisableShortsBlocker(it) },
                    onCancelTemporaryDisable = { focusViewModel.cancelShortsBlockerTemporaryDisable() },
                    onRequestAccessibilityPerm = { focusViewModel.openShortsBlockerAccessibilitySettings() }
                )
            },
            confirmButton = {
                Button(
                    onClick = { showShortsBlockerDialog = false },
                    modifier = Modifier.testTag("shorts_blocker_dialog_done")
                ) {
                    Text("Done")
                }
            }
        )
    }
    } // closes Box
} // closes FocusScreen

/**
 * Stopwatch Display with Start -> Pause -> Resume -> Stop -> Save flow and History
 */
@Composable
fun StopwatchDisplayView(
    formattedTime: String,
    isRunning: Boolean,
    isPaused: Boolean,
    laps: List<StopwatchLap>,
    savedSessions: List<com.example.data.focus.SavedStopwatchSession>,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
    onRenameSession: (id: String, newTitle: String) -> Unit,
    onDeleteSession: (id: String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .size(220.dp)
                .testTag("stopwatch_display_circle")
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "STOPWATCH",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        isPaused -> "PAUSED"
                        isRunning -> "TRACKING"
                        else -> "READY"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isPaused -> Color(0xFFF59E0B)
                        isRunning -> Color(0xFF10B981)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action Buttons: Start -> Pause -> Resume -> Stop -> Save Flow
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isRunning) {
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .height(52.dp)
                        .width(170.dp)
                        .testTag("stopwatch_start_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            } else {
                if (isPaused) {
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("stopwatch_resume_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume")
                    }

                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("stopwatch_stop_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop & Save")
                    }

                    OutlinedButton(
                        onClick = onReset,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("stopwatch_reset_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }
                } else {
                    Button(
                        onClick = onPause,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("stopwatch_pause_button")
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause")
                    }

                    Button(
                        onClick = onLap,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("stopwatch_lap_button")
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lap")
                    }

                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("stopwatch_stop_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop & Save")
                    }
                }
            }
        }

        // Laps Display Table
        if (laps.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("stopwatch_laps_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recorded Laps", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    laps.forEach { lap ->
                        val lapSec = lap.lapTimeMillis / 1000
                        val lapMin = lapSec / 60
                        val lapSecRem = lapSec % 60
                        val lapHund = (lap.lapTimeMillis % 1000) / 10

                        val splitSec = lap.splitTimeMillis / 1000
                        val splitMin = splitSec / 60
                        val splitSecRem = splitSec % 60
                        val splitHund = (lap.splitTimeMillis % 1000) / 10

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Lap ${lap.lapIndex}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Text("+%02d:%02d.%02d".format(lapMin, lapSecRem, lapHund), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text("%02d:%02d.%02d".format(splitMin, splitSecRem, splitHund), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Stopwatch History Section
        StopwatchHistorySection(
            sessions = savedSessions,
            onRenameSession = onRenameSession,
            onDeleteSession = onDeleteSession
        )
    }
}

/**
 * App Blocker Configuration Modal Dialog
 */
@Composable
fun AppBlockerModalDialog(
    isBlockerEnabled: Boolean,
    hasUsagePerm: Boolean,
    hasOverlayPerm: Boolean,
    blockedPackages: Set<String>,
    installedApps: List<BlockableAppInfo>,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleApp: (String) -> Unit,
    onRequestUsagePerm: () -> Unit,
    onRequestOverlayPerm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFEF4444))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Focus App Blocker", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Enable Blocker Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable App Blocker", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Automatically blocks selected distracting apps during active sessions. Unselected apps remain accessible.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isBlockerEnabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.testTag("app_blocker_enable_switch")
                    )
                }

                HorizontalDivider()

                // Permission Status Cards
                if (!hasUsagePerm) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Usage Access Permission Needed", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Android requires Usage Access to detect when a blocked app is opened.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF78350F))
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onRequestUsagePerm,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
                                modifier = Modifier.testTag("grant_usage_perm_button")
                            ) {
                                Text("Grant Usage Permission", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Text(
                    text = "Select Distracting Apps to Block (${blockedPackages.size} Selected)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                if (installedApps.isEmpty()) {
                    Text(
                        text = "Loading installed apps list...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    installedApps.forEach { app ->
                        val isBlocked = blockedPackages.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleApp(app.packageName) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isBlocked) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isBlocked) Icons.Default.Block else Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = if (isBlocked) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                            Checkbox(
                                checked = isBlocked,
                                onCheckedChange = { onToggleApp(app.packageName) },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444))
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.testTag("app_blocker_save_button")) {
                Text("Done")
            }
        }
    )
}

/**
 * Website Blocker Configuration Modal Dialog:
 * - Enter URLs / domains (e.g. youtube.com, instagram.com)
 * - Block, Unblock, and Temporary Block options with customizable duration
 * - Accessible UI with persistent list
 */
@Composable
fun WebsiteBlockerModalDialog(
    isBlockerEnabled: Boolean,
    hasAccessibilityPerm: Boolean,
    blockedWebsites: List<com.example.util.BlockedWebsite>,
    onToggleEnabled: (Boolean) -> Unit,
    onAddWebsite: (String, Boolean, Int) -> Unit,
    onRemoveWebsite: (String) -> Unit,
    onRequestAccessibilityPerm: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onDismiss: () -> Unit
) {
    var inputUrl by remember { mutableStateOf("") }
    var isTemporary by remember { mutableStateOf(false) }
    var temporaryDurationMinutes by remember { mutableIntStateOf(30) }
    var inputError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF6366F1))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Website Blocker", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Enable Website Blocker Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Block Distracting Websites", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Works 24/7 independently of Focus Mode to block URL domains in browsers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isBlockerEnabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.testTag("website_blocker_enable_switch")
                    )
                }

                HorizontalDivider()

                // Accessibility Permission Notice & Android 13+ Restricted Settings Guide
                if (!hasAccessibilityPerm) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Accessibility Service & Android 13+ Restricted Settings", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Enables StudyOS to detect browser URLs and block forbidden domains. If Android 13+ shows 'Restricted Setting', tap Step 1 first.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF3730A3))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onOpenAppInfo,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B7280)),
                                    modifier = Modifier.weight(1f).testTag("open_app_info_button")
                                ) {
                                    Text("1. Open App Info", style = MaterialTheme.typography.labelSmall)
                                }
                                Button(
                                    onClick = onRequestAccessibilityPerm,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                    modifier = Modifier.weight(1f).testTag("grant_accessibility_perm_button")
                                ) {
                                    Text("2. Enable Service", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Note: In App Info, tap top-right 3 dots (⋮) -> 'Allow restricted settings', then enable service.", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = Color(0xFF4338CA))
                        }
                    }
                }

                // Add New Website Section
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Add Website or Domain", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = {
                                inputUrl = it
                                inputError = null
                            },
                            label = { Text("e.g. youtube.com, instagram.com") },
                            placeholder = { Text("domain or URL") },
                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            isError = inputError != null,
                            supportingText = inputError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            modifier = Modifier.fillMaxWidth().testTag("website_blocker_input")
                        )

                        // Temporary Block Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Temporary Block Only", style = MaterialTheme.typography.bodySmall)
                            Switch(
                                checked = isTemporary,
                                onCheckedChange = { isTemporary = it },
                                modifier = Modifier.testTag("website_temporary_switch")
                            )
                        }

                        if (isTemporary) {
                            Column {
                                Text("Temporary Duration: $temporaryDurationMinutes minutes", style = MaterialTheme.typography.labelSmall)
                                Slider(
                                    value = temporaryDurationMinutes.toFloat(),
                                    onValueChange = { temporaryDurationMinutes = it.toInt() },
                                    valueRange = 5f..180f,
                                    steps = 34
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val clean = inputUrl.trim()
                                if (clean.isBlank()) {
                                    inputError = "Please enter a valid domain"
                                    return@Button
                                }
                                onAddWebsite(clean, isTemporary, temporaryDurationMinutes)
                                inputUrl = ""
                                isTemporary = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("website_blocker_add_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isTemporary) "Block for $temporaryDurationMinutes Min" else "Block Permanently")
                        }
                    }
                }

                // Blocked Websites List
                Text(
                    text = "Blocked Websites (${blockedWebsites.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                if (blockedWebsites.isEmpty()) {
                    Text(
                        text = "No websites blocked yet. Add distracting domains above (e.g. twitter.com, reddit.com).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    blockedWebsites.forEach { site ->
                        val isBlockedNow = site.isCurrentlyBlocked
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isBlockedNow) Color(0xFFFEE2E2) else Color(0xFFE2E8F0),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isBlockedNow) Icons.Default.Block else Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = if (isBlockedNow) Color(0xFFEF4444) else Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(site.domain, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    if (site.isTemporary) {
                                        Text(
                                            text = if (isBlockedNow) "Temp block: ${site.remainingTemporaryMinutes}m remaining" else "Temporary block expired",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isBlockedNow) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text("Permanent block", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onRemoveWebsite(site.domain) },
                                modifier = Modifier.testTag("remove_website_${site.domain}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Unblock", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.testTag("website_blocker_done_button")) {
                Text("Done")
            }
        }
    )
}

/**
 * Daily Study Reminders & Session Completion Alerts Configuration Dialog
 */
@Composable
fun FocusReminderModalDialog(
    settings: com.example.util.FocusReminderSettings,
    hasNotificationPerm: Boolean,
    onRequestPerm: () -> Unit,
    onSaveSettings: (com.example.util.FocusReminderSettings) -> Unit,
    onSendTestReminder: () -> Unit,
    onSendTestCompletion: () -> Unit,
    onDismiss: () -> Unit
) {
    var isDailyEnabled by remember { mutableStateOf(settings.isDailyReminderEnabled) }
    var hour by remember { mutableIntStateOf(settings.reminderHour) }
    var minute by remember { mutableIntStateOf(settings.reminderMinute) }
    var isSecondaryEnabled by remember { mutableStateOf(settings.isSecondaryReminderEnabled) }
    var secHour by remember { mutableIntStateOf(settings.secondaryHour) }
    var secMinute by remember { mutableIntStateOf(settings.secondaryMinute) }
    var isCompletionAlertEnabled by remember { mutableStateOf(settings.isCompletionAlertEnabled) }
    var minDuration by remember { mutableIntStateOf(settings.minSessionMinutesForAlert) }
    var soundVibrate by remember { mutableStateOf(settings.soundAndVibrateEnabled) }
    var testFeedback by remember { mutableStateOf<String?>(null) }

    fun commitSettings() {
        onSaveSettings(
            com.example.util.FocusReminderSettings(
                isDailyReminderEnabled = isDailyEnabled,
                reminderHour = hour,
                reminderMinute = minute,
                isSecondaryReminderEnabled = isSecondaryEnabled,
                secondaryHour = secHour,
                secondaryMinute = secMinute,
                isCompletionAlertEnabled = isCompletionAlertEnabled,
                minSessionMinutesForAlert = minDuration,
                soundAndVibrateEnabled = soundVibrate
            )
        )
    }

    AlertDialog(
        onDismissRequest = {
            commitSettings()
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFF59E0B))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Study Notifications", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Permission Warning Card if not granted
                if (!hasNotificationPerm) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Notification Permission Required", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("To receive study reminders at your preferred times, please enable notifications.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF78350F))
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onRequestPerm,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
                                modifier = Modifier.testTag("grant_notification_perm_button")
                            ) {
                                Text("Allow Notifications", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Section 1: Daily Focus Reminders
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Daily Study Reminder", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Get alerted when it's time for your daily focus session.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isDailyEnabled,
                                onCheckedChange = { isDailyEnabled = it },
                                modifier = Modifier.testTag("daily_reminder_switch")
                            )
                        }

                        if (isDailyEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text("Preferred Daily Study Time", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                            // Display current formatted time
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = com.example.util.FocusReminderSettings.formatTime(hour, minute),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Steppers for Hour and Minute
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { hour = (hour - 1 + 24) % 24 },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text("-1h")
                                        }
                                        OutlinedButton(
                                            onClick = { hour = (hour + 1) % 24 },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text("+1h")
                                        }
                                        OutlinedButton(
                                            onClick = { minute = (minute + 15) % 60 },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("+15m")
                                        }
                                    }
                                }
                            }

                            // Quick Time Preset Chips
                            Text("Quick Presets:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    Triple("Morning", 9, 0),
                                    Triple("Noon", 13, 0),
                                    Triple("Evening", 18, 0),
                                    Triple("Night", 21, 0)
                                ).forEach { (label, h, m) ->
                                    val isSelected = hour == h && minute == m
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .clickable {
                                                hour = h
                                                minute = m
                                            }
                                            .testTag("preset_$label")
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            // Secondary Evening Reminder Toggle
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Second Daily Study Reminder", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = isSecondaryEnabled,
                                    onCheckedChange = { isSecondaryEnabled = it },
                                    modifier = Modifier.testTag("secondary_reminder_switch")
                                )
                            }

                            if (isSecondaryEnabled) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = com.example.util.FocusReminderSettings.formatTime(secHour, secMinute),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = { secHour = (secHour - 1 + 24) % 24 },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("-1h")
                                            }
                                            OutlinedButton(
                                                onClick = { secHour = (secHour + 1) % 24 },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("+1h")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Session Completion Alert
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Session Completion Alerts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Alerts you with heads-up celebration when deep work is finished.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isCompletionAlertEnabled,
                                onCheckedChange = { isCompletionAlertEnabled = it },
                                modifier = Modifier.testTag("completion_alert_switch")
                            )
                        }

                        if (isCompletionAlertEnabled) {
                            Column {
                                Text("Alert on sessions of at least: $minDuration minutes", style = MaterialTheme.typography.labelSmall)
                                Slider(
                                    value = minDuration.toFloat(),
                                    onValueChange = { minDuration = it.toInt() },
                                    valueRange = 5f..60f,
                                    steps = 10,
                                    modifier = Modifier.testTag("min_duration_slider")
                                )
                            }
                        }
                    }
                }

                // Section 3: Sound & Vibration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sound & Vibration for Alerts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = soundVibrate,
                        onCheckedChange = { soundVibrate = it },
                        modifier = Modifier.testTag("sound_vibrate_switch")
                    )
                }

                // Section 4: Test Notifications
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Verify Notification System", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSendTestReminder()
                                testFeedback = "Study reminder notification sent! Check status bar."
                            },
                            modifier = Modifier.weight(1f).testTag("send_test_reminder_button")
                        ) {
                            Text("Test Reminder", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = {
                                onSendTestCompletion()
                                testFeedback = "Session completed alert sent! Check status bar."
                            },
                            modifier = Modifier.weight(1f).testTag("send_test_completion_button")
                        ) {
                            Text("Test Completion", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    testFeedback?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    commitSettings()
                    onDismiss()
                },
                modifier = Modifier.testTag("save_notification_settings_button")
            ) {
                Text("Save & Apply")
            }
        }
    )
}

/**
 * Beautiful Circular Animated Timer inspired by Regain.
 */
@Composable
fun CircularFocusTimer(
    formattedTime: String,
    progress: Float,
    modeTitle: String,
    isRunning: Boolean,
    isPaused: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "timerProgress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(240.dp)
            .testTag("circular_focus_timer")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeftOffset = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Animated Foreground Arc
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = modeTitle.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    isPaused -> "PAUSED"
                    isRunning -> "FOCUSING"
                    else -> "READY"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isPaused -> Color(0xFFF59E0B)
                    isRunning -> Color(0xFF10B981)
                    else -> MaterialTheme.colorScheme.primary
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Distraction-Free Full-Screen Focus Mode.
 */
@Composable
fun DistractionFreeFocusView(
    uiState: com.example.ui.viewmodel.FocusUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
    onPauseStopwatch: () -> Unit,
    onResumeStopwatch: () -> Unit,
    onLapStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
    onToggleMute: () -> Unit,
    onExitFullScreen: () -> Unit
) {
    val themeBrush = getThemeBackgroundBrush(uiState.visualTheme)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeBrush)
            .padding(24.dp)
            .testTag("distraction_free_focus_view"),
        contentAlignment = Alignment.Center
    ) {
        // Exit Fullscreen Button in top right
        IconButton(
            onClick = onExitFullScreen,
            modifier = Modifier.align(Alignment.TopEnd).testTag("exit_fullscreen_button")
        ) {
            Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color.White.copy(alpha = 0.8f))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Text(
                text = if (uiState.activeTool == FocusMainTool.TIMER) "DEEP FOCUS TIMER" else "DEEP WORK STOPWATCH",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )

            if (uiState.activeTool == FocusMainTool.TIMER) {
                CircularFocusTimer(
                    formattedTime = uiState.formattedTime,
                    progress = uiState.progress,
                    modeTitle = uiState.mode.title,
                    isRunning = uiState.isRunning,
                    isPaused = uiState.isPaused
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (uiState.isPaused) {
                        Button(
                            onClick = onResume,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume")
                        }
                    } else {
                        Button(
                            onClick = onPause,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause")
                        }
                    }

                    OutlinedButton(
                        onClick = onEnd,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("End Session")
                    }
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.size(240.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.formattedStopwatchTime,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (uiState.isStopwatchPaused) {
                        Button(
                            onClick = onResumeStopwatch,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume")
                        }
                    } else {
                        Button(
                            onClick = onPauseStopwatch,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause")
                        }
                        Button(
                            onClick = onLapStopwatch,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lap")
                        }
                    }

                    OutlinedButton(
                        onClick = onResetStopwatch,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }
                }
            }
        }
    }
}

/**
 * Returns a rich ambient gradient brush matching the selected Focus Visual Theme.
 */
fun getThemeBackgroundBrush(theme: FocusVisualTheme): Brush {
    return when (theme) {
        FocusVisualTheme.DEFAULT -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A), // Slate 900
                Color(0xFF1E293B), // Slate 800
                Color(0xFF0A0F1D)  // Deep obsidian navy
            )
        )
        FocusVisualTheme.FOREST -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF062817), // Deep pine canopy
                Color(0xFF0B3B24), // Emerald moss
                Color(0xFF021B0E)  // Dark woodland earth
            )
        )
        FocusVisualTheme.OCEAN -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF031E38), // Oceanic trench blue
                Color(0xFF0A3A6B), // Deep marine cyan
                Color(0xFF011024)  // Midnight abyssal water
            )
        )
        FocusVisualTheme.SPACE -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0E0728), // Deep cosmic void
                Color(0xFF261250), // Nebula amethyst purple
                Color(0xFF080318)  // Starry twilight space
            )
        )
    }
}

/**
 * Modal dialog enabling students to choose their ambient focus environment background theme.
 */
@Composable
fun ThemeSelectorModalDialog(
    currentTheme: FocusVisualTheme,
    onSelectTheme: (FocusVisualTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Focus Environment Themes", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Switch between ambient visual backgrounds to enhance deep work immersion and focus flow.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FocusVisualTheme.values().forEach { theme ->
                    val isSelected = currentTheme == theme
                    val previewBrush = getThemeBackgroundBrush(theme)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectTheme(theme)
                                onDismiss()
                            }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(theme.primaryColorHex) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .testTag("theme_option_${theme.id}"),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(previewBrush)
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = theme.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(theme.primaryColorHex)
                                            ) {
                                                Text(
                                                    text = "Active",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = theme.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(theme.primaryColorHex),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("theme_selector_done_button")
            ) {
                Text("Done")
            }
        }
    )
}

