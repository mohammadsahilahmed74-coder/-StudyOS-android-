package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.SubjectsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.TimetableScreen
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.StudyOSTheme
import com.example.ui.viewmodel.StudyViewModel

data class NavDestination(
    val index: Int,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    private var pendingStudyViewModel: StudyViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check before setContent, ViewModels or Firebase calls
        com.example.util.configureAppCheck(this, intent)
        com.example.util.FocusNotificationManager.createNotificationChannels(this)
        com.example.util.FocusNotificationManager.rescheduleAllReminders(this)

        enableEdgeToEdge()
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val authManager = remember { com.example.ui.auth.AuthManager(context = context) }
            val currentUser by authManager.authStateFlow().collectAsStateWithLifecycle(initialValue = authManager.currentUser)
            val isGuestMode by authManager.isGuestMode.collectAsStateWithLifecycle(initialValue = authManager.isGuestActive)

            val viewModel: StudyViewModel = viewModel()
            pendingStudyViewModel = viewModel

            androidx.compose.runtime.LaunchedEffect(intent) {
                if (intent?.getBooleanExtra("EXTRA_OPEN_FOCUS", false) == true ||
                    intent?.getIntExtra("TARGET_TAB", -1) == 9 ||
                    intent?.getStringExtra("START_DESTINATION") == "FOCUS_STOPWATCH") {
                    viewModel.setTab(9)
                }
            }

            val darkModePref by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val isDark = when (darkModePref) {
                true -> true
                false -> false
                null -> isSystemInDarkTheme()
            }

            StudyOSTheme(darkTheme = isDark) {
                if (currentUser == null && !isGuestMode) {
                    com.example.ui.auth.AuthScreen(
                        authManager = authManager,
                        onAuthenticated = {}
                    )
                } else {
                    StudyOSApp(
                        viewModel = viewModel,
                        isDarkMode = isDark,
                        user = currentUser,
                        authManager = authManager
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("EXTRA_OPEN_FOCUS", false) ||
            intent.getIntExtra("TARGET_TAB", -1) == 9 ||
            intent.getStringExtra("START_DESTINATION") == "FOCUS_STOPWATCH") {
            pendingStudyViewModel?.setTab(9)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyOSApp(
    viewModel: StudyViewModel,
    isDarkMode: Boolean,
    user: com.google.firebase.auth.FirebaseUser? = null,
    authManager: com.example.ui.auth.AuthManager = remember { com.example.ui.auth.AuthManager() }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val darkModePref by viewModel.isDarkMode.collectAsStateWithLifecycle()

    val destinations = listOf(
        NavDestination(0, "Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
        NavDestination(9, "Focus", Icons.Default.Timer, "nav_focus"),
        NavDestination(3, "Schedule", Icons.Default.CalendarMonth, "nav_timetable"),
        NavDestination(11, "Groups", Icons.Default.People, "nav_groups"),
        NavDestination(10, "Blocks", Icons.Default.Lock, "nav_blocks"),
        NavDestination(1, "Tasks", Icons.Default.Checklist, "nav_tasks"),
        NavDestination(4, "Notes", Icons.Default.Description, "nav_notes")
    )

    // Handle back button: return to dashboard if on any other tab
    if (currentTab != 0) {
        BackHandler { viewModel.setTab(0) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            ) {
                                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "StudyOS",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    },
                    actions = {
                        // Theme Toggle
                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = when (darkModePref) {
                                    true -> Icons.Default.DarkMode
                                    false -> Icons.Default.LightMode
                                    null -> Icons.Default.BrightnessAuto
                                },
                                contentDescription = "Toggle Theme"
                            )
                        }

                        // AI Assistant with Search & Maps Grounding
                        IconButton(
                            onClick = { viewModel.setTab(8) },
                            modifier = Modifier.testTag("nav_ai_assistant_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Research & Maps",
                                tint = if (currentTab == 8) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Science / Bioscience Skills Lab Button
                        IconButton(
                            onClick = { viewModel.setTab(7) },
                            modifier = Modifier.testTag("nav_bioscience_skills_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Bioscience Skills Lab",
                                tint = if (currentTab == 7) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Profile Icon Button
                        IconButton(
                            onClick = { viewModel.setTab(6) },
                            modifier = Modifier.testTag("nav_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "User Profile",
                                tint = if (currentTab == 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Logout Icon Button
                        IconButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier.testTag("auth_logout_button")
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        destinations.forEach { dest ->
                            NavigationBarItem(
                                selected = currentTab == dest.index,
                                onClick = { viewModel.setTab(dest.index) },
                                icon = { Icon(dest.icon, contentDescription = dest.title) },
                                label = { Text(dest.title, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (isWideScreen) {
                // Wide Screen / Tablet Layout with NavigationRail
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        destinations.forEach { dest ->
                            NavigationRailItem(
                                selected = currentTab == dest.index,
                                onClick = { viewModel.setTab(dest.index) },
                                icon = { Icon(dest.icon, contentDescription = dest.title) },
                                label = { Text(dest.title) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                    ScreenContent(
                        currentTab = currentTab,
                        viewModel = viewModel,
                        user = user,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                ScreenContent(
                    currentTab = currentTab,
                    viewModel = viewModel,
                    user = user,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }

    if (showLogoutDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { androidx.compose.material3.Text("Sign Out") },
            text = {
                androidx.compose.material3.Text("Are you sure you want to sign out of StudyOS? You can sign back in anytime.")
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showLogoutDialog = false
                        scope.launch {
                            authManager.signOut(context)
                        }
                    },
                    modifier = Modifier.testTag("auth_logout_confirm_button")
                ) {
                    androidx.compose.material3.Text("Sign Out")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showLogoutDialog = false },
                    modifier = Modifier.testTag("auth_logout_cancel_button")
                ) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ScreenContent(
    currentTab: Int,
    viewModel: StudyViewModel,
    user: com.google.firebase.auth.FirebaseUser? = null,
    modifier: Modifier = Modifier
) {
    when (currentTab) {
        0 -> DashboardScreen(
            viewModel = viewModel,
            onNavigateToTasks = { viewModel.setTab(1) },
            onNavigateToSubjects = { viewModel.setTab(2) },
            onNavigateToTimetable = { viewModel.setTab(3) },
            onNavigateToAnalytics = { viewModel.setTab(5) },
            user = user,
            modifier = modifier
        )
        1 -> TasksScreen(viewModel = viewModel, modifier = modifier)
        2 -> SubjectsScreen(viewModel = viewModel, modifier = modifier)
        3 -> TimetableScreen(viewModel = viewModel, modifier = modifier)
        4 -> NotesScreen(viewModel = viewModel, modifier = modifier)
        5 -> AnalyticsScreen(viewModel = viewModel, modifier = modifier)
        6 -> com.example.ui.screens.ProfileScreen(
            user = user,
            onBack = { viewModel.setTab(0) },
            modifier = modifier
        )
        7 -> com.example.ui.screens.BioscienceSkillsScreen(
            viewModel = viewModel,
            onBack = { viewModel.setTab(0) },
            modifier = modifier
        )
        8 -> com.example.ui.ai.AiAssistantScreen(
            onBack = { viewModel.setTab(0) },
            modifier = modifier
        )
        9 -> com.example.ui.screens.FocusScreen(
            modifier = modifier
        )
        10 -> com.example.ui.screens.BlocksScreen(
            modifier = modifier
        )
        11 -> com.example.ui.screens.GroupsScreen(
            modifier = modifier
        )
        else -> DashboardScreen(
            viewModel = viewModel,
            onNavigateToTasks = { viewModel.setTab(1) },
            onNavigateToSubjects = { viewModel.setTab(2) },
            onNavigateToTimetable = { viewModel.setTab(3) },
            onNavigateToAnalytics = { viewModel.setTab(5) },
            modifier = modifier
        )
    }
}
