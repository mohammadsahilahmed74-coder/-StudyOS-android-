package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExamEntity
import com.example.data.model.SubjectEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ExamDialog
import com.example.ui.components.SubjectPill
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.viewmodel.ExamCountdown
import com.example.ui.viewmodel.StudyViewModel
import com.example.ui.viewmodel.SubjectProgress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val examCountdowns by viewModel.examCountdowns.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val subjectsProgress by viewModel.subjectsProgress.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val timetableSlots by viewModel.timetableSlots.collectAsStateWithLifecycle()
    val overallSyllabus by viewModel.overallSyllabusProgress.collectAsStateWithLifecycle()

    var showAddExamDialog by remember { mutableStateOf(false) }
    var examToEdit by remember { mutableStateOf<ExamEntity?>(null) }
    var examToDelete by remember { mutableStateOf<ExamEntity?>(null) }

    val completedTasks = tasks.count { it.isCompleted }
    val totalTasks = tasks.size
    val highPriorityPending = tasks.count { !it.isCompleted && it.priority == "HIGH" }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("analytics_scroll_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Exam Countdowns & Stats",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Track target exam dates and syllabus mastery",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showAddExamDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_exam_header_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Exam")
                    }
                }
            }

            // Exam Countdowns List
            if (examCountdowns.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.School,
                        title = "No exam countdowns",
                        subtitle = "Add target tests or final board exams to track days remaining",
                        actionText = "Add Exam",
                        onAction = { showAddExamDialog = true }
                    )
                }
            } else {
                items(examCountdowns, key = { "exam_${it.exam.id}" }) { item ->
                    val subject = subjects.find { it.id == item.exam.subjectId }
                    DetailedExamCard(
                        countdown = item,
                        subject = subject,
                        onEdit = { examToEdit = item.exam },
                        onDelete = { examToDelete = item.exam }
                    )
                }
            }

            // High Level Productivity Stats
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Study Productivity Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProductivityCard(
                        title = "Tasks Completed",
                        value = "$completedTasks / $totalTasks",
                        subtitle = if (totalTasks > 0) "${((completedTasks.toFloat() / totalTasks) * 100).toInt()}% Done" else "0%",
                        color = AccentEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    ProductivityCard(
                        title = "Syllabus Mastery",
                        value = "${(overallSyllabus * 100).toInt()}%",
                        subtitle = "${subjects.size} Core Subjects",
                        color = PrimaryIndigo,
                        modifier = Modifier.weight(1f)
                    )
                    ProductivityCard(
                        title = "Weekly Timetable",
                        value = "${timetableSlots.size} slots",
                        subtitle = "Scheduled weekly",
                        color = AccentAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (highPriorityPending > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentRose.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AccentRose,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("!", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "$highPriorityPending High Priority Tasks Pending",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentRose
                                )
                                Text(
                                    text = "Make sure to review critical numericals and revisions first.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Subject Syllabus Completion Breakdown
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Subject-Wise Syllabus Completion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(subjectsProgress, key = { "subject_prog_${it.subject.id}" }) { prog ->
                SubjectSyllabusProgressRow(progress = prog)
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Add Exam FAB
        FloatingActionButton(
            onClick = { showAddExamDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_exam_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Exam")
        }
    }

    // Add / Edit Exam Dialog
    if (showAddExamDialog || examToEdit != null) {
        ExamDialog(
            examToEdit = examToEdit,
            subjects = subjects,
            onDismiss = {
                showAddExamDialog = false
                examToEdit = null
            },
            onSave = { title, subjectId, examDateMillis, targetScore, notes ->
                if (examToEdit == null) {
                    viewModel.insertExam(title, subjectId, examDateMillis, targetScore, notes)
                } else {
                    viewModel.updateExam(
                        examToEdit!!.copy(
                            title = title,
                            subjectId = subjectId,
                            examDateMillis = examDateMillis,
                            targetScore = targetScore,
                            notes = notes
                        )
                    )
                }
                showAddExamDialog = false
                examToEdit = null
            }
        )
    }

    // Confirm Delete Exam
    if (examToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Exam?",
            message = "Are you sure you want to remove \"${examToDelete!!.title}\"?",
            onConfirm = {
                viewModel.deleteExam(examToDelete!!)
                examToDelete = null
            },
            onDismiss = { examToDelete = null }
        )
    }
}

@Composable
fun DetailedExamCard(
    countdown: ExamCountdown,
    subject: SubjectEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val examDateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date(countdown.exam.examDateMillis))

    val gradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF1E1B4B),
            Color(0xFF312E81)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exam_card_${countdown.exam.id}"),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SubjectPill(subject = subject)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentAmber.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Target: ${countdown.exam.targetScore}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Exam options", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Exam") },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Exam", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = countdown.exam.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "📅 $examDateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )

                if (countdown.exam.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📝 ${countdown.exam.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (countdown.isPast) "Passed" else "${countdown.daysRemaining}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "DAYS REMAINING",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${countdown.hoursRemaining}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "HOURS",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductivityCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
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
fun SubjectSyllabusProgressRow(progress: SubjectProgress) {
    val subject = progress.subject
    val color = parseHexColor(subject.colorHex, PrimaryIndigo)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${(progress.completionPercent * 100).toInt()}% Done",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.completionPercent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${progress.completedChapters} / ${progress.totalChapters} Chapters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Text(
                    text = "${progress.revisedChapters} Revised",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentEmerald,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
