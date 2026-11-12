package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SubjectEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TimetableSlotEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ExamDialog
import com.example.ui.components.PriorityBadge
import com.example.ui.components.SlotTypeBadge
import com.example.ui.components.SubjectPill
import com.example.ui.components.TaskDialog
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.viewmodel.StudyViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: StudyViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToSubjects: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    user: com.google.firebase.auth.FirebaseUser? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember {
        context.getSharedPreferences("studyos_user_profile_prefs", android.content.Context.MODE_PRIVATE)
    }

    val greetingName = remember(user) {
        if (user != null) {
            user.displayName?.takeIf { it.isNotBlank() }
                ?: sharedPrefs.getString("user_name_${user.uid}", null)
                ?: user.email?.substringBefore("@")
                ?: "Student"
        } else {
            sharedPrefs.getString("user_name_guest", "Guest User") ?: "Guest User"
        }
    }
    val todayTasks by viewModel.todayTasks.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val examCountdowns by viewModel.examCountdowns.collectAsStateWithLifecycle()
    val timetableSlots by viewModel.timetableSlots.collectAsStateWithLifecycle()
    val overallSyllabus by viewModel.overallSyllabusProgress.collectAsStateWithLifecycle()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }

    var showAddExamDialog by remember { mutableStateOf(false) }
    var taskStatusFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, COMPLETED

    var reminderNotificationFeedback by remember { mutableStateOf<String?>(null) }
    var pendingReminderTask by remember { mutableStateOf<TaskEntity?>(null) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        pendingReminderTask?.let { task ->
            if (isGranted) {
                com.example.util.ReminderManager.scheduleTaskReminder(
                    context = context,
                    taskId = task.id,
                    taskTitle = task.title,
                    taskDescription = task.description,
                    triggerAtMillis = if (task.dueDateMillis > System.currentTimeMillis()) task.dueDateMillis else System.currentTimeMillis() + 1000L
                )
                reminderNotificationFeedback = "Reminder scheduled for \"${task.title}\""
            } else {
                reminderNotificationFeedback = "Notification permission denied. Please allow notifications in device settings."
            }
        }
        pendingReminderTask = null
    }

    val onScheduleReminder: (TaskEntity) -> Unit = { task ->
        pendingReminderTask = task
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                com.example.util.ReminderManager.scheduleTaskReminder(
                    context = context,
                    taskId = task.id,
                    taskTitle = task.title,
                    taskDescription = task.description,
                    triggerAtMillis = if (task.dueDateMillis > System.currentTimeMillis()) task.dueDateMillis else System.currentTimeMillis() + 1000L
                )
                reminderNotificationFeedback = "Reminder set for \"${task.title}\""
                pendingReminderTask = null
            } else {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            com.example.util.ReminderManager.scheduleTaskReminder(
                context = context,
                taskId = task.id,
                taskTitle = task.title,
                taskDescription = task.description,
                triggerAtMillis = if (task.dueDateMillis > System.currentTimeMillis()) task.dueDateMillis else System.currentTimeMillis() + 1000L
            )
            reminderNotificationFeedback = "Reminder set for \"${task.title}\""
            pendingReminderTask = null
        }
    }

    val primaryExamCountdown = examCountdowns.firstOrNull { !it.isPast } ?: examCountdowns.firstOrNull()

    // Determine current day slots
    val todayDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).let {
        if (it == Calendar.SUNDAY) 7 else it - 1
    }
    val todaySlots = timetableSlots.filter { it.dayOfWeek == todayDayOfWeek }

    val completedTodayCount = todayTasks.count { it.isCompleted }
    val activeTodayCount = todayTasks.count { !it.isCompleted }
    val totalTodayCount = todayTasks.size
    val todayTasksPercent = if (totalTodayCount > 0) completedTodayCount.toFloat() / totalTodayCount else 0f

    val filteredTodayTasks = todayTasks.filter { task ->
        when (taskStatusFilter) {
            "ACTIVE" -> !task.isCompleted
            "COMPLETED" -> task.isCompleted
            else -> true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_scroll_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Greeting
            item {
                DashboardHeader(
                    completedTasks = completedTodayCount,
                    totalTasks = totalTodayCount,
                    userGreetingName = greetingName
                )
            }

            // Notification Feedback Banner
            item {
                AnimatedVisibility(visible = reminderNotificationFeedback != null) {
                    reminderNotificationFeedback?.let { feedback ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reminder_feedback_banner"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = feedback,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                IconButton(
                                    onClick = { reminderNotificationFeedback = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Segmented Button Row: Task Filter (All, Active, Completed)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_task_filter_segmented_row"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Triple("ALL", "All", totalTodayCount),
                            Triple("ACTIVE", "Active", activeTodayCount),
                            Triple("COMPLETED", "Completed", completedTodayCount)
                        ).forEach { (filterKey, label, count) ->
                            val isSelected = taskStatusFilter == filterKey
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { taskStatusFilter = filterKey }
                                    .testTag("dashboard_filter_${filterKey.lowercase()}"),
                                shape = RoundedCornerShape(9.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        Text(
                                            text = "$count",
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Primary Exam Countdown Card
            item {
                if (primaryExamCountdown != null) {
                    ExamCountdownHeroCard(
                        countdown = primaryExamCountdown,
                        onViewAllExams = onNavigateToAnalytics,
                        onAddNewExam = { showAddExamDialog = true }
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddExamDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = PrimaryIndigo)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("No exam countdown added", style = MaterialTheme.typography.titleMedium)
                                Text("Tap here to set your target exam date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = { showAddExamDialog = true }) {
                                Text("Add Exam")
                            }
                        }
                    }
                }
            }

            // Quick Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stat 1: Tasks Today
                    StatMetricCard(
                        title = "Tasks Today",
                        value = "$completedTodayCount/$totalTodayCount",
                        progress = todayTasksPercent,
                        progressColor = AccentEmerald,
                        subtitle = if (totalTodayCount == 0) "No tasks" else "${(todayTasksPercent * 100).toInt()}% Done",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToTasks
                    )

                    // Stat 2: Syllabus
                    StatMetricCard(
                        title = "Syllabus Covered",
                        value = "${(overallSyllabus * 100).toInt()}%",
                        progress = overallSyllabus,
                        progressColor = PrimaryIndigo,
                        subtitle = "${subjects.size} Subjects",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSubjects
                    )

                    // Stat 3: Study Timetable
                    StatMetricCard(
                        title = "Today's Schedule",
                        value = "${todaySlots.size} slots",
                        progress = if (todaySlots.isNotEmpty()) 1f else 0f,
                        progressColor = AccentAmber,
                        subtitle = "Day $todayDayOfWeek of 7",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToTimetable
                    )
                }
            }

            // Section: Today's Tasks
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Today's Study Tasks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "$completedTodayCount/$totalTodayCount",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    TextButton(
                        onClick = onNavigateToTasks,
                        modifier = Modifier.testTag("dashboard_view_all_tasks_button")
                    ) {
                        Text("View All")
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (filteredTodayTasks.isEmpty()) {
                item {
                    val emptyTitle = when (taskStatusFilter) {
                        "ACTIVE" -> "No active tasks for today"
                        "COMPLETED" -> "No completed tasks yet"
                        else -> "No tasks for today"
                    }
                    val emptySubtitle = when (taskStatusFilter) {
                        "ACTIVE" -> "Great job! All of today's tasks are finished."
                        "COMPLETED" -> "Check off your finished study tasks to see them here."
                        else -> "Plan your study sessions and stay on track!"
                    }
                    EmptyStateView(
                        icon = Icons.Default.CheckCircle,
                        title = emptyTitle,
                        subtitle = emptySubtitle,
                        actionText = if (taskStatusFilter == "ALL") "Add First Task" else null,
                        onAction = if (taskStatusFilter == "ALL") ({ showAddTaskDialog = true }) else null
                    )
                }
            } else {
                items(filteredTodayTasks, key = { "task_${it.id}" }) { task ->
                    val subject = subjects.find { it.id == task.subjectId }
                    DashboardTaskItem(
                        task = task,
                        subject = subject,
                        onToggleComplete = { viewModel.toggleTaskCompleted(task) },
                        onEdit = { taskToEdit = task },
                        onDelete = { taskToDelete = task },
                        onSetReminder = { onScheduleReminder(task) }
                    )
                }
            }

            // Section: Today's Timetable Glance
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Timetable",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = onNavigateToTimetable,
                        modifier = Modifier.testTag("dashboard_view_timetable_button")
                    ) {
                        Text("Full Timetable")
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (todaySlots.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "No timetable slots scheduled for today. Rest or plan extra revisions!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(todaySlots, key = { "slot_${it.id}" }) { slot ->
                    val subject = subjects.find { it.id == slot.subjectId }
                    DashboardTimetableSlotItem(slot = slot, subject = subject)
                }
            }

            // Bottom padding for FAB clearance
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Add Task FAB
        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("dashboard_add_task_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Study Task")
        }
    }

    // Task Dialog
    if (showAddTaskDialog || taskToEdit != null) {
        TaskDialog(
            taskToEdit = taskToEdit,
            subjects = subjects,
            onDismiss = {
                showAddTaskDialog = false
                taskToEdit = null
            },
            onSave = { title, description, subjectId, dueDateMillis, priority, estimatedMinutes ->
                if (taskToEdit == null) {
                    viewModel.insertTask(title, description, subjectId, dueDateMillis, priority, estimatedMinutes)
                } else {
                    viewModel.updateTask(
                        taskToEdit!!.copy(
                            title = title,
                            description = description,
                            subjectId = subjectId,
                            dueDate = dueDateMillis,
                            priority = priority,
                            estimatedMinutes = estimatedMinutes
                        )
                    )
                }
                showAddTaskDialog = false
                taskToEdit = null
            }
        )
    }

    // Add Exam Dialog
    if (showAddExamDialog) {
        ExamDialog(
            examToEdit = null,
            subjects = subjects,
            onDismiss = { showAddExamDialog = false },
            onSave = { title, subjectId, examDateMillis, targetScore, notes ->
                viewModel.insertExam(title, subjectId, examDateMillis, targetScore, notes)
                showAddExamDialog = false
            }
        )
    }

    // Confirm Delete Dialog
    if (taskToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Task?",
            message = "Are you sure you want to delete \"${taskToDelete!!.title}\"?",
            onConfirm = {
                viewModel.deleteTask(taskToDelete!!)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }
}

@Composable
fun DashboardHeader(
    completedTasks: Int,
    totalTasks: Int,
    userGreetingName: String = "Student"
) {
    val dateStr = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    Column {
        Text(
            text = dateStr.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Welcome, $userGreetingName",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (totalTasks == 0) "Ready to set today's study goals?" else "You've completed $completedTasks of $totalTasks study goals today",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ExamCountdownHeroCard(
    countdown: com.example.ui.viewmodel.ExamCountdown,
    onViewAllExams: () -> Unit,
    onAddNewExam: () -> Unit
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E1B4B),
            Color(0xFF312E81),
            Color(0xFF4338CA)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exam_countdown_hero_card"),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "EXAM COUNTDOWN",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentAmber.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Target: ${countdown.exam.targetScore}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = countdown.exam.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Days & Hours Blocks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CountdownDigitBlock(
                        value = "${countdown.daysRemaining}",
                        label = "DAYS LEFT",
                        modifier = Modifier.weight(1f)
                    )
                    CountdownDigitBlock(
                        value = "${countdown.hoursRemaining}",
                        label = "HOURS",
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clickable(onClick = onViewAllExams),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                            Text("All Exams", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CountdownDigitBlock(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 28.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    progress: Float,
    progressColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("stat_card_${title.replace(' ', '_').lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun DashboardTaskItem(
    task: TaskEntity,
    subject: SubjectEntity?,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetReminder: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(checkedColor = AccentEmerald),
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SubjectPill(subject = subject)
                    PriorityBadge(priority = task.priority)
                    Text(
                        text = "• ${task.estimatedMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("task_delete_button_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Set Reminder")
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onSetReminder()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Task") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Task", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardTimetableSlotItem(
    slot: TimetableSlotEntity,
    subject: SubjectEntity?
) {
    val subjectColor = parseHexColor(subject?.colorHex, PrimaryIndigo)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = subjectColor.copy(alpha = 0.15f),
                modifier = Modifier.width(68.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(slot.startTime, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = subjectColor)
                    Text(slot.endTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = subject?.name ?: "Study Session",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    SlotTypeBadge(type = slot.slotType)
                }
                if (slot.topic.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = slot.topic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
