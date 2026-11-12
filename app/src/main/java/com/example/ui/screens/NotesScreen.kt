package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.library.StudyLibraryStorageManager
import com.example.data.model.NoteAttachment
import com.example.data.model.NoteEntity
import com.example.data.model.SubjectEntity
import com.example.ui.components.AttachmentActionDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DigitalLibraryNoteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.RenameAttachmentDialog
import com.example.ui.components.STUDY_CATEGORIES
import com.example.ui.components.SubjectPill
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AccentAmber
import com.example.ui.viewmodel.StudyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()

    val searchQuery by viewModel.noteSearchQuery.collectAsStateWithLifecycle()
    val filterSubjectId by viewModel.noteFilterSubjectId.collectAsStateWithLifecycle()
    val filterCategory by viewModel.noteFilterCategory.collectAsStateWithLifecycle()
    val filterFileType by viewModel.noteFilterFileType.collectAsStateWithLifecycle()
    val libraryViewMode by viewModel.libraryViewMode.collectAsStateWithLifecycle()

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<NoteEntity?>(null) }
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }

    // Active in-app viewers and management dialogs
    var activePdfViewerAttachment by remember { mutableStateOf<NoteAttachment?>(null) }
    var activeImageViewerAttachment by remember { mutableStateOf<NoteAttachment?>(null) }
    var selectedAttachmentForAction by remember { mutableStateOf<Pair<NoteEntity, NoteAttachment>?>(null) }
    var attachmentToRename by remember { mutableStateOf<Pair<NoteEntity, NoteAttachment>?>(null) }

    val currentUser = remember {
        try { Firebase.auth.currentUser } catch (_: Exception) { null }
    }

    // Filter notes with multi-dimensional matching: title, content, chapter, category, AND attached file names!
    val filteredNotes = notes.filter { note ->
        val query = searchQuery.trim()
        val attachments = note.getAttachments()

        val matchesQuery = query.isBlank() ||
                note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true) ||
                note.chapter.contains(query, ignoreCase = true) ||
                note.category.contains(query, ignoreCase = true) ||
                attachments.any { it.fileName.contains(query, ignoreCase = true) }

        val matchesSubject = filterSubjectId == null || note.subjectId == filterSubjectId
        val matchesCategory = filterCategory == null || note.category.equals(filterCategory, ignoreCase = true)

        val matchesFileType = when (filterFileType) {
            "PDF" -> attachments.any { it.isPdf }
            "IMAGE" -> attachments.any { it.isImage }
            "DOC" -> attachments.any { it.isDocument }
            else -> true
        }

        matchesQuery && matchesSubject && matchesCategory && matchesFileType
    }

    // Flatten all attachments across filtered notes for the "Files & Materials" library view
    val allLibraryFiles = filteredNotes.flatMap { note ->
        note.getAttachments().map { att -> Pair(note, att) }
    }

    // Total library statistics
    val totalAttachments = notes.sumOf { it.attachmentCount }
    val totalPdfs = notes.sumOf { it.getAttachments().count { att -> att.isPdf } }
    val totalImages = notes.sumOf { it.getAttachments().count { att -> att.isImage } }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header & Search
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Digital Study Library",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "v2.0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "PDFs, photo notes, formulas & chapter materials",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Cloud / Local Storage Indicator Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentUser != null) Color(0xFF065F46) else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = if (currentUser != null) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentUser != null) "Cloud Sync" else "Device Storage",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (currentUser != null) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar with attachment-aware query
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setNoteSearchQuery(it) },
                    placeholder = { Text("Search notes, chapters, formulas, or file names...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setNoteSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Library Stats Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryStatBadge(
                        label = "Notes",
                        value = "${notes.size}",
                        icon = Icons.Default.Description,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LibraryStatBadge(
                        label = "Files",
                        value = "$totalAttachments",
                        icon = Icons.Default.AttachFile,
                        color = Color(0xFF8B5CF6)
                    )
                    LibraryStatBadge(
                        label = "PDFs",
                        value = "$totalPdfs",
                        icon = Icons.Default.PictureAsPdf,
                        color = Color(0xFFEF4444)
                    )
                    LibraryStatBadge(
                        label = "Images",
                        value = "$totalImages",
                        icon = Icons.Default.Image,
                        color = Color(0xFF38BDF8)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // View Mode Switcher: Study Notes vs Library Files
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(2.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .clickable { viewModel.setLibraryViewMode("NOTES") }
                                .testTag("view_mode_notes"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (libraryViewMode == "NOTES") MaterialTheme.colorScheme.surface else Color.Transparent,
                            tonalElevation = if (libraryViewMode == "NOTES") 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewAgenda,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (libraryViewMode == "NOTES") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Notes View",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (libraryViewMode == "NOTES") FontWeight.Bold else FontWeight.Normal,
                                    color = if (libraryViewMode == "NOTES") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .clickable { viewModel.setLibraryViewMode("FILES") }
                                .testTag("view_mode_files"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (libraryViewMode == "FILES") MaterialTheme.colorScheme.surface else Color.Transparent,
                            tonalElevation = if (libraryViewMode == "FILES") 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (libraryViewMode == "FILES") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Files Library (${allLibraryFiles.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (libraryViewMode == "FILES") FontWeight.Bold else FontWeight.Normal,
                                    color = if (libraryViewMode == "FILES") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // File Type Filter Dropdown / Quick Toggle
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("ALL" to "All", "PDF" to "PDFs", "IMAGE" to "Images", "DOC" to "Docs").forEach { (typeKey, typeLabel) ->
                            val isSelected = filterFileType == typeKey
                            Surface(
                                modifier = Modifier
                                    .clickable { viewModel.setNoteFilterFileType(typeKey) }
                                    .testTag("file_type_filter_$typeKey"),
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                            ) {
                                Text(
                                    text = typeLabel,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subject Filter Chips Row
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
                            .clickable { viewModel.setNoteFilterSubjectId(null) }
                            .testTag("note_subject_filter_all"),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isAllSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = "All Subjects",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    subjects.forEach { subject ->
                        val isSelected = filterSubjectId == subject.id
                        val count = notes.count { it.subjectId == subject.id }
                        val subjectColor = parseHexColor(subject.colorHex)
                        Surface(
                            modifier = Modifier
                                .clickable { viewModel.setNoteFilterSubjectId(subject.id) }
                                .testTag("note_subject_filter_${subject.id}"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) subjectColor else subjectColor.copy(alpha = 0.12f),
                            contentColor = if (isSelected) Color.White else subjectColor
                        ) {
                            Text(
                                text = "${subject.name} ($count)",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isCatAll = filterCategory == null
                    Surface(
                        modifier = Modifier.clickable { viewModel.setNoteFilterCategory(null) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCatAll) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "All Categories",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCatAll) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    STUDY_CATEGORIES.forEach { cat ->
                        val isCatSelected = filterCategory == cat
                        Surface(
                            modifier = Modifier.clickable {
                                viewModel.setNoteFilterCategory(if (isCatSelected) null else cat)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCatSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = cat,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCatSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Content Area: Notes List OR Files Library Grid
            if (libraryViewMode == "NOTES") {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("notes_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredNotes.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Default.Description,
                                title = if (searchQuery.isNotBlank()) "No notes or files found" else "Your Study Library is empty",
                                subtitle = if (searchQuery.isNotBlank()) "Try searching another formula, topic, or file name" else "Create your first revision note and attach lecture PDFs, diagrams, or formula cheat sheets",
                                actionText = "Create Note & Attach Material",
                                onAction = { showAddNoteDialog = true }
                            )
                        }
                    } else {
                        items(filteredNotes, key = { it.id }) { note ->
                            val subject = subjects.find { it.id == note.subjectId }
                            DigitalLibraryNoteCard(
                                note = note,
                                subject = subject,
                                searchQuery = searchQuery,
                                onTogglePin = { viewModel.toggleNotePin(note) },
                                onEdit = { noteToEdit = note },
                                onDelete = { noteToDelete = note },
                                onOpenPdf = { att -> activePdfViewerAttachment = att },
                                onOpenImage = { att -> activeImageViewerAttachment = att },
                                onAttachmentAction = { att -> selectedAttachmentForAction = Pair(note, att) }
                            )
                        }
                    }
                }
            } else {
                // Files & Materials Grid View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("library_files_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (allLibraryFiles.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Default.Folder,
                                title = "No study files found",
                                subtitle = "Attach PDFs, diagrams, and study guides to your notes to build your digital library",
                                actionText = "Add Note & Upload Files",
                                onAction = { showAddNoteDialog = true }
                            )
                        }
                    } else {
                        items(allLibraryFiles, key = { "${it.first.id}_${it.second.id}" }) { (note, att) ->
                            val subject = subjects.find { it.id == note.subjectId }
                            LibraryFileCard(
                                note = note,
                                attachment = att,
                                subject = subject,
                                onOpenPdf = { activePdfViewerAttachment = att },
                                onOpenImage = { activeImageViewerAttachment = att },
                                onAttachmentAction = { selectedAttachmentForAction = Pair(note, att) }
                            )
                        }
                    }
                }
            }
        }

        // Add Note & Material Floating Action Button
        FloatingActionButton(
            onClick = { showAddNoteDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_note_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Study Material")
        }
    }

    // Note Editor Dialog
    if (showAddNoteDialog || noteToEdit != null) {
        DigitalLibraryNoteDialog(
            noteToEdit = noteToEdit,
            subjects = subjects,
            chapters = chapters,
            onDismiss = {
                showAddNoteDialog = false
                noteToEdit = null
            },
            onSave = { title, content, subjectId, isPinned, chapter, category, attachments ->
                if (noteToEdit == null) {
                    viewModel.insertNote(
                        title = title,
                        content = content,
                        subjectId = subjectId,
                        isPinned = isPinned,
                        chapter = chapter,
                        category = category,
                        attachments = attachments
                    )
                } else {
                    viewModel.updateNote(
                        noteToEdit!!.copy(
                            title = title,
                            content = content,
                            subjectId = subjectId,
                            isPinned = isPinned,
                            chapter = chapter,
                            category = category,
                            attachmentsJson = NoteAttachment.toJsonString(attachments)
                        )
                    )
                }
                showAddNoteDialog = false
                noteToEdit = null
            }
        )
    }

    // Confirm Delete Dialog
    if (noteToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Study Note?",
            message = "Are you sure you want to delete \"${noteToDelete!!.title}\" and its ${noteToDelete!!.attachmentCount} attached study files?",
            onConfirm = {
                viewModel.deleteNote(noteToDelete!!)
                noteToDelete = null
            },
            onDismiss = { noteToDelete = null }
        )
    }

    // Attachment Action Dialog (Open in app, open externally, share, rename, delete)
    if (selectedAttachmentForAction != null) {
        val (parentNote, att) = selectedAttachmentForAction!!
        AttachmentActionDialog(
            attachment = att,
            onDismiss = { selectedAttachmentForAction = null },
            onOpenInApp = {
                if (att.isPdf) {
                    activePdfViewerAttachment = att
                } else if (att.isImage) {
                    activeImageViewerAttachment = att
                } else {
                    StudyLibraryStorageManager.openAttachmentExternally(context, att)
                }
            },
            onOpenExternally = {
                StudyLibraryStorageManager.openAttachmentExternally(context, att)
            },
            onExport = {
                StudyLibraryStorageManager.exportAttachment(context, att)
            },
            onRename = {
                attachmentToRename = selectedAttachmentForAction
            },
            onRemove = {
                viewModel.removeAttachmentFromNote(parentNote, att.id)
                selectedAttachmentForAction = null
            }
        )
    }

    // Rename Attachment Dialog
    if (attachmentToRename != null) {
        val (parentNote, att) = attachmentToRename!!
        RenameAttachmentDialog(
            currentName = att.fileName,
            onDismiss = { attachmentToRename = null },
            onConfirm = { newName ->
                viewModel.renameAttachmentInNote(parentNote, att.id, newName)
                attachmentToRename = null
                selectedAttachmentForAction = null
            }
        )
    }

    // In-App PDF Viewer Modal
    if (activePdfViewerAttachment != null) {
        PdfViewerModal(
            attachment = activePdfViewerAttachment!!,
            onClose = { activePdfViewerAttachment = null }
        )
    }

    // In-App Image Viewer Modal
    if (activeImageViewerAttachment != null) {
        ImageViewerModal(
            attachment = activeImageViewerAttachment!!,
            onClose = { activeImageViewerAttachment = null }
        )
    }
}

@Composable
fun LibraryStatBadge(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
    }
}

/**
 * Enhanced Note Card with Chapter Tag, Category Pill, and Attachment Strip
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DigitalLibraryNoteCard(
    note: NoteEntity,
    subject: SubjectEntity?,
    searchQuery: String,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenPdf: (NoteAttachment) -> Unit,
    onOpenImage: (NoteAttachment) -> Unit,
    onAttachmentAction: (NoteAttachment) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(note.updatedAtMillis))
    val attachments = note.getAttachments()

    // Highlight if search query matches an attachment specifically
    val matchingAttachment = if (searchQuery.isNotBlank()) {
        attachments.find { it.fileName.contains(searchQuery, ignoreCase = true) }
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isPinned)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (note.isPinned) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category & Chapter Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Category pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = note.category.ifBlank { "Lecture Notes" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Chapter badge if set
                    if (note.chapter.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "📖 ${note.chapter}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Pin & Overflow Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Unpin" else "Pin",
                            tint = if (note.isPinned) AccentAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit & Attach Files") },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (note.isPinned) "Unpin Note" else "Pin to Top") },
                                onClick = {
                                    menuExpanded = false
                                    onTogglePin()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Note", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Note Title
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Matching File Search Alert
            if (matchingAttachment != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Matched attachment: ${matchingAttachment.fileName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Attached Materials Strip
            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${attachments.size} Attached ${if (attachments.size == 1) "File" else "Files"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Horizontal / Flow list of attachment pills
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        attachments.forEach { att ->
                            Surface(
                                modifier = Modifier
                                    .clickable {
                                        if (att.isPdf) onOpenPdf(att)
                                        else if (att.isImage) onOpenImage(att)
                                        else onAttachmentAction(att)
                                    }
                                    .testTag("attachment_pill_${att.id}"),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when {
                                            att.isPdf -> Icons.Default.PictureAsPdf
                                            att.isImage -> Icons.Default.Image
                                            else -> Icons.Default.Description
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            att.isPdf -> Color(0xFFEF4444)
                                            att.isImage -> Color(0xFF38BDF8)
                                            else -> Color(0xFF10B981)
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = att.fileName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 140.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "(${att.fileSizeFormatted})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer with Subject pill and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubjectPill(subject = subject)
                Text(
                    text = "Updated: $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Dedicated Card for the "Files & Materials Library" View
 */
@Composable
fun LibraryFileCard(
    note: NoteEntity,
    attachment: NoteAttachment,
    subject: SubjectEntity?,
    onOpenPdf: () -> Unit,
    onOpenImage: () -> Unit,
    onAttachmentAction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (attachment.isPdf) onOpenPdf()
                else if (attachment.isImage) onOpenImage()
                else onAttachmentAction()
            }
            .testTag("library_file_card_${attachment.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File Type Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        when {
                            attachment.isPdf -> Color(0xFFEF4444).copy(alpha = 0.15f)
                            attachment.isImage -> Color(0xFF38BDF8).copy(alpha = 0.15f)
                            else -> Color(0xFF10B981).copy(alpha = 0.15f)
                        },
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        attachment.isPdf -> Icons.Default.PictureAsPdf
                        attachment.isImage -> Icons.Default.Image
                        else -> Icons.Default.Description
                    },
                    contentDescription = null,
                    tint = when {
                        attachment.isPdf -> Color(0xFFEF4444)
                        attachment.isImage -> Color(0xFF38BDF8)
                        else -> Color(0xFF10B981)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = attachment.fileSizeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = "•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                }

                if (note.chapter.isNotBlank() || subject != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = listOfNotNull(subject?.name, note.chapter.takeIf { it.isNotBlank() }).joinToString(" > "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            IconButton(
                onClick = onAttachmentAction,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "File actions", modifier = Modifier.size(18.dp))
            }
        }
    }
}
