package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PriorityBadge
import com.example.ui.components.SubjectPill
import com.example.ui.components.TaskDialog
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AccentEmerald
import com.example.ui.viewmodel.StudyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TasksScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val searchQuery by viewModel.taskSearchQuery.collectAsStateWithLifecycle()
    val filterStatus by viewModel.taskFilterStatus.collectAsStateWithLifecycle()
    val filterSubjectId by viewModel.taskFilterSubjectId.collectAsStateWithLifecycle()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }
    var showClearCompletedConfirm by remember { mutableStateOf(false) }

    // Filtering logic
    val filteredTasks = tasks.filter { task ->
        val matchesQuery = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true)

        val matchesStatus = when (filterStatus) {
            "PENDING" -> !task.isCompleted
            "COMPLETED" -> task.isCompleted
            else -> true
        }

        val matchesSubject = filterSubjectId == null || task.subjectId == filterSubjectId

        matchesQuery && matchesStatus && matchesSubject
    }

    val pendingCount = tasks.count { !it.isCompleted }
    val completedCount = tasks.count { it.isCompleted }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar / Search
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setTaskSearchQuery(it) },
                    placeholder = { Text("Search tasks...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setTaskSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tasks_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Status Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "All (${tasks.size})",
                        "PENDING" to "Pending ($pendingCount)",
                        "COMPLETED" to "Completed ($completedCount)"
                    ).forEach { (statusKey, label) ->
                        val isSelected = filterStatus == statusKey
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setTaskFilterStatus(statusKey) }
                                .testTag("task_status_filter_$statusKey"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subject filter row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAllSelected = filterSubjectId == null
                    Surface(
                        modifier = Modifier
                            .clickable { viewModel.setTaskFilterSubjectId(null) }
                            .testTag("task_subject_filter_all"),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isAllSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = "All Subjects",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    subjects.forEach { subject ->
                        val isSelected = filterSubjectId == subject.id
                        val subjectColor = parseHexColor(subject.colorHex)
                        Surface(
                            modifier = Modifier
                                .clickable { viewModel.setTaskFilterSubjectId(subject.id) }
                                .testTag("task_subject_filter_${subject.id}"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) subjectColor else subjectColor.copy(alpha = 0.12f),
                            contentColor = if (isSelected) Color.White else subjectColor
                        ) {
                            Text(
                                text = subject.name,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Tasks List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tasks_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (completedCount > 0 && filterStatus != "PENDING") {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showClearCompletedConfirm = true },
                                modifier = Modifier.testTag("clear_completed_tasks_button")
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Completed", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                if (filteredTasks.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.TaskAlt,
                            title = if (searchQuery.isNotBlank()) "No matching tasks" else "No tasks found",
                            subtitle = if (searchQuery.isNotBlank()) "Try a different search query" else "Create study tasks to keep track of your daily study schedule",
                            actionText = "Create Task",
                            onAction = { showAddTaskDialog = true }
                        )
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        val subject = subjects.find { it.id == task.subjectId }
                        TaskListItemCard(
                            task = task,
                            subject = subject,
                            onToggleComplete = { viewModel.toggleTaskCompleted(task) },
                            onEdit = { taskToEdit = task },
                            onDelete = { taskToDelete = task }
                        )
                    }
                }
            }
        }

        // Add Task FAB
        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("tasks_add_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
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

    // Confirm Delete Task
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

    // Confirm Clear Completed
    if (showClearCompletedConfirm) {
        ConfirmDeleteDialog(
            title = "Clear Completed Tasks?",
            message = "This will remove all finished tasks from your study list.",
            onConfirm = {
                viewModel.clearCompletedTasks()
                showClearCompletedConfirm = false
            },
            onDismiss = { showClearCompletedConfirm = false }
        )
    }
}

@Composable
fun TaskListItemCard(
    task: TaskEntity,
    subject: SubjectEntity?,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val formattedDueDate = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(task.dueDateMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
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
                    style = MaterialTheme.typography.titleMedium,
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
                        maxLines = 2,
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
                        text = "Due: $formattedDueDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "• ${task.estimatedMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
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
