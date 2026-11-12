package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChapterEntity
import com.example.data.model.SubjectEntity
import com.example.ui.components.ChapterDialog
import com.example.ui.components.ChapterStatusBadge
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SubjectDialog
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.viewmodel.StudyViewModel
import com.example.ui.viewmodel.SubjectProgress

@Composable
fun SubjectsScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val subjectsProgress by viewModel.subjectsProgress.collectAsStateWithLifecycle()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsStateWithLifecycle()

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }

    var showAddChapterDialog by remember { mutableStateOf(false) }
    var chapterToEdit by remember { mutableStateOf<ChapterEntity?>(null) }
    var chapterToDelete by remember { mutableStateOf<ChapterEntity?>(null) }

    val activeSubject = subjects.find { it.id == selectedSubjectId }

    if (activeSubject != null) {
        // Detailed Chapter Tracker View for Active Subject
        BackHandler { viewModel.setSelectedSubjectId(null) }

        val subjectChapters = chapters.filter { it.subjectId == activeSubject.id }
        val subjectProg = subjectsProgress.find { it.subject.id == activeSubject.id }

        SubjectChapterTrackerView(
            subject = activeSubject,
            progress = subjectProg,
            chapters = subjectChapters,
            onBack = { viewModel.setSelectedSubjectId(null) },
            onAddChapter = { showAddChapterDialog = true },
            onEditChapter = { chapterToEdit = it },
            onDeleteChapter = { chapterToDelete = it },
            onUpdateChapterStatus = { chapter, status -> viewModel.updateChapterStatus(chapter, status) },
            onToggleRevision = { viewModel.toggleChapterRevision(it) },
            modifier = modifier
        )
    } else {
        // All Subjects Overview
        Box(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("subjects_overview_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SubjectsSummaryBanner(subjectsProgress = subjectsProgress)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subjects & Syllabus",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${subjects.size} Subjects",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (subjectsProgress.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.School,
                            title = "No subjects found",
                            subtitle = "Add your school or college subjects to begin tracking syllabus",
                            actionText = "Add Subject",
                            onAction = { showAddSubjectDialog = true }
                        )
                    }
                } else {
                    items(subjectsProgress, key = { it.subject.id }) { item ->
                        SubjectCardItem(
                            progress = item,
                            onClick = { viewModel.setSelectedSubjectId(item.subject.id) },
                            onEdit = { subjectToEdit = item.subject },
                            onDelete = { subjectToDelete = item.subject }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }

            FloatingActionButton(
                onClick = { showAddSubjectDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("add_subject_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        }
    }

    // Subject Add / Edit Dialog
    if (showAddSubjectDialog || subjectToEdit != null) {
        SubjectDialog(
            subjectToEdit = subjectToEdit,
            onDismiss = {
                showAddSubjectDialog = false
                subjectToEdit = null
            },
            onSave = { name, colorHex, iconKey, targetHours ->
                if (subjectToEdit == null) {
                    viewModel.insertSubject(name, colorHex, iconKey, targetHours)
                } else {
                    viewModel.updateSubject(
                        subjectToEdit!!.copy(
                            name = name,
                            colorHex = colorHex,
                            iconKey = iconKey,
                            targetHoursPerWeek = targetHours
                        )
                    )
                }
                showAddSubjectDialog = false
                subjectToEdit = null
            }
        )
    }

    // Chapter Add / Edit Dialog
    if (showAddChapterDialog || chapterToEdit != null) {
        val targetSubjectId = activeSubject?.id ?: 0L
        val maxChapterNum = chapters.filter { it.subjectId == targetSubjectId }.maxOfOrNull { it.chapterNumber } ?: 0
        ChapterDialog(
            chapterToEdit = chapterToEdit,
            subjectId = targetSubjectId,
            nextChapterNumber = maxChapterNum + 1,
            onDismiss = {
                showAddChapterDialog = false
                chapterToEdit = null
            },
            onSave = { chapterNumber, title, notes ->
                if (chapterToEdit == null) {
                    viewModel.insertChapter(targetSubjectId, chapterNumber, title, notes)
                } else {
                    viewModel.updateChapter(
                        chapterToEdit!!.copy(
                            chapterNumber = chapterNumber,
                            title = title,
                            notes = notes
                        )
                    )
                }
                showAddChapterDialog = false
                chapterToEdit = null
            }
        )
    }

    // Confirm Delete Subject
    if (subjectToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Subject?",
            message = "This will delete ${subjectToDelete!!.name} and all associated chapters, tasks, and timetable slots.",
            onConfirm = {
                viewModel.deleteSubject(subjectToDelete!!)
                subjectToDelete = null
            },
            onDismiss = { subjectToDelete = null }
        )
    }

    // Confirm Delete Chapter
    if (chapterToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Chapter?",
            message = "Are you sure you want to delete \"${chapterToDelete!!.title}\"?",
            onConfirm = {
                viewModel.deleteChapter(chapterToDelete!!)
                chapterToDelete = null
            },
            onDismiss = { chapterToDelete = null }
        )
    }
}

@Composable
fun SubjectsSummaryBanner(
    subjectsProgress: List<SubjectProgress>
) {
    val totalChapters = subjectsProgress.sumOf { it.totalChapters }
    val completedChapters = subjectsProgress.sumOf { it.completedChapters }
    val revisedChapters = subjectsProgress.sumOf { it.revisedChapters }
    val completionPercent = if (totalChapters > 0) completedChapters.toFloat() / totalChapters else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Overall Syllabus Mastery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Text(
                        text = "${(completionPercent * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { completionPercent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Completed: $completedChapters / $totalChapters Chapters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Revised: $revisedChapters",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentEmerald,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun SubjectCardItem(
    progress: SubjectProgress,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val subject = progress.subject
    val color = parseHexColor(subject.colorHex, PrimaryIndigo)
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("subject_card_${subject.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Subject options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Subject") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Subject", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${progress.completedChapters} of ${progress.totalChapters} chapters done",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(progress.completionPercent * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress.completionPercent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Target: ${subject.targetHoursPerWeek}h/wk",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp
                        )
                    }
                    if (progress.revisedChapters > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentEmerald.copy(alpha = 0.12f),
                            contentColor = AccentEmerald
                        ) {
                            Text(
                                text = "${progress.revisedChapters} Revised",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Text(
                    text = "Track Chapters →",
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SubjectChapterTrackerView(
    subject: SubjectEntity,
    progress: SubjectProgress?,
    chapters: List<ChapterEntity>,
    onBack: () -> Unit,
    onAddChapter: () -> Unit,
    onEditChapter: (ChapterEntity) -> Unit,
    onDeleteChapter: (ChapterEntity) -> Unit,
    onUpdateChapterStatus: (ChapterEntity, String) -> Unit,
    onToggleRevision: (ChapterEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = parseHexColor(subject.colorHex, PrimaryIndigo)
    var filterStatus by remember { mutableStateOf("ALL") }

    val filteredChapters = chapters.filter {
        when (filterStatus) {
            "NOT_STARTED" -> it.status == "NOT_STARTED"
            "IN_PROGRESS" -> it.status == "IN_PROGRESS"
            "COMPLETED" -> it.status == "COMPLETED"
            else -> true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_to_subjects_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to subjects")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Chapter Tracker & Revision Checklist",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Subject Syllabus Progress Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Syllabus Progress",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        val percent = progress?.completionPercent ?: 0f
                        Text(
                            text = "${(percent * 100).toInt()}% Done",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (progress?.completionPercent ?: 0f).coerceIn(0f, 1f) },
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
                            text = "Completed: ${progress?.completedChapters ?: 0} / ${progress?.totalChapters ?: 0}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "In Progress: ${progress?.inProgressChapters ?: 0}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Revised: ${progress?.revisedChapters ?: 0}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentEmerald
                        )
                    }
                }
            }

            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "ALL" to "All (${chapters.size})",
                    "NOT_STARTED" to "To Do",
                    "IN_PROGRESS" to "Ongoing",
                    "COMPLETED" to "Done"
                ).forEach { (fKey, label) ->
                    val isSelected = filterStatus == fKey
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { filterStatus = fKey },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Chapter list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("chapter_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredChapters.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.MenuBook,
                            title = "No chapters found",
                            subtitle = "Add chapters for ${subject.name} to track your study goals",
                            actionText = "Add Chapter",
                            onAction = onAddChapter
                        )
                    }
                } else {
                    items(filteredChapters, key = { it.id }) { chapter ->
                        ChapterListItemCard(
                            chapter = chapter,
                            subjectColor = color,
                            onUpdateStatus = { newStatus -> onUpdateChapterStatus(chapter, newStatus) },
                            onToggleRevision = { onToggleRevision(chapter) },
                            onEdit = { onEditChapter(chapter) },
                            onDelete = { onDeleteChapter(chapter) }
                        )
                    }
                }
            }
        }

        // Add Chapter FAB
        FloatingActionButton(
            onClick = onAddChapter,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_chapter_fab"),
            containerColor = color,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Chapter")
        }
    }
}

@Composable
fun ChapterListItemCard(
    chapter: ChapterEntity,
    subjectColor: Color,
    onUpdateStatus: (String) -> Unit,
    onToggleRevision: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chapter_card_${chapter.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chapter Number Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = subjectColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Ch ${chapter.chapterNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = subjectColor,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (chapter.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = chapter.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Chapter menu")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Chapter") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Chapter", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Selector and Revision Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Switcher
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "NOT_STARTED" to "To Do",
                        "IN_PROGRESS" to "Ongoing",
                        "COMPLETED" to "Done"
                    ).forEach { (sKey, label) ->
                        val isSelected = chapter.status.equals(sKey, ignoreCase = true)
                        val chipColor = when (sKey) {
                            "COMPLETED" -> AccentEmerald
                            "IN_PROGRESS" -> PrimaryIndigo
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Surface(
                            modifier = Modifier
                                .clickable { onUpdateStatus(sKey) }
                                .testTag("status_toggle_${chapter.id}_$sKey"),
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) chipColor else chipColor.copy(alpha = 0.1f),
                            contentColor = if (isSelected) Color.White else chipColor
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Revision Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onToggleRevision)
                ) {
                    Checkbox(
                        checked = chapter.isRevisionDone,
                        onCheckedChange = { onToggleRevision() },
                        colors = CheckboxDefaults.colors(checkedColor = AccentEmerald),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Revised",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (chapter.isRevisionDone) FontWeight.Bold else FontWeight.Normal,
                        color = if (chapter.isRevisionDone) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
