package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.library.StudyLibraryStorageManager
import com.example.data.model.ChapterEntity
import com.example.data.model.NoteAttachment
import com.example.data.model.NoteEntity
import com.example.data.model.SubjectEntity
import kotlinx.coroutines.launch

val STUDY_CATEGORIES = listOf(
    "Lecture Notes",
    "Formulas & Cheatsheet",
    "Past Papers",
    "Lab Reports",
    "Reference Guides",
    "Summary & Diagrams",
    "Homework & Exercises"
)

/**
 * Interactive Dialog to Rename an Attachment
 */
@Composable
fun RenameAttachmentDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Rename Attachment", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                Text(
                    "Enter a new display name for this study material:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) error = null
                    },
                    label = { Text("File Name *") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_attachment_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "File name cannot be empty"
                        return@Button
                    }
                    onConfirm(name.trim())
                },
                modifier = Modifier.testTag("rename_confirm_button")
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Interactive Action Sheet / Dialog for an attachment
 */
@Composable
fun AttachmentActionDialog(
    attachment: NoteAttachment,
    onDismiss: () -> Unit,
    onOpenInApp: () -> Unit,
    onOpenExternally: () -> Unit,
    onExport: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = attachment.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${attachment.fileSizeFormatted} • ${attachment.storageType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Open in App
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenInApp(); onDismiss() },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveRedEye,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (attachment.isPdf) "Read PDF In-App" else if (attachment.isImage) "View & Zoom Photo" else "Preview Document",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Built-in interactive study viewer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Open with External App
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenExternally(); onDismiss() },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Open with External App",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Download / Export / Share
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExport(); onDismiss() },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Download / Share File",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Rename
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRename(); onDismiss() },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Rename Attachment",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Remove
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRemove(); onDismiss() },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Remove from Note",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Upgraded Digital Study Library Note Editor Dialog
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DigitalLibraryNoteDialog(
    noteToEdit: NoteEntity?,
    subjects: List<SubjectEntity>,
    chapters: List<ChapterEntity>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        content: String,
        subjectId: Long?,
        isPinned: Boolean,
        chapter: String,
        category: String,
        attachments: List<NoteAttachment>
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
    var content by remember { mutableStateOf(noteToEdit?.content ?: "") }
    var selectedSubjectId by remember { mutableStateOf(noteToEdit?.subjectId) }
    var chapter by remember { mutableStateOf(noteToEdit?.chapter ?: "") }
    var category by remember { mutableStateOf(noteToEdit?.category ?: "Lecture Notes") }
    var isPinned by remember { mutableStateOf(noteToEdit?.isPinned ?: false) }

    var isSubjectDropdownExpanded by remember { mutableStateOf(false) }
    var isChapterDropdownExpanded by remember { mutableStateOf(false) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }

    // Persistent attachments list in editor
    val attachments = remember {
        mutableStateListOf<NoteAttachment>().apply {
            addAll(noteToEdit?.getAttachments() ?: emptyList())
        }
    }

    var isImportingFiles by remember { mutableStateOf(false) }
    var attachmentToRename by remember { mutableStateOf<NoteAttachment?>(null) }

    // Android Document Picker for PDFs and general study materials
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isImportingFiles = true
            coroutineScope.launch {
                uris.forEach { uri ->
                    try {
                        val imported = StudyLibraryStorageManager.importFileUri(context, uri)
                        attachments.add(imported)
                    } catch (_: Exception) {}
                }
                isImportingFiles = false
            }
        }
    }

    // Android Visual Media Picker for photos, whiteboard captures, and diagrams
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isImportingFiles = true
            coroutineScope.launch {
                uris.forEach { uri ->
                    try {
                        val imported = StudyLibraryStorageManager.importFileUri(context, uri)
                        attachments.add(imported)
                    } catch (_: Exception) {}
                }
                isImportingFiles = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (noteToEdit == null) "New Study Material & Note" else "Edit Study Material",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Note Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = null
                    },
                    label = { Text("Title *") },
                    placeholder = { Text("e.g. Thermodynamics Chapter 3 & Formulas") },
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_note_title_input")
                )

                // Subject Selector
                ExposedDropdownMenuBox(
                    expanded = isSubjectDropdownExpanded,
                    onExpandedChange = { isSubjectDropdownExpanded = it }
                ) {
                    val sName = subjects.find { it.id == selectedSubjectId }?.name ?: "General (No Subject)"
                    OutlinedTextField(
                        value = sName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSubjectDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                            .testTag("library_note_subject_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = isSubjectDropdownExpanded,
                        onDismissRequest = { isSubjectDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("General (No Subject)") },
                            onClick = {
                                selectedSubjectId = null
                                isSubjectDropdownExpanded = false
                            }
                        )
                        subjects.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = {
                                    selectedSubjectId = s.id
                                    isSubjectDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Chapter Selector / Input
                val availableChapters = chapters.filter { it.subjectId == selectedSubjectId }
                ExposedDropdownMenuBox(
                    expanded = isChapterDropdownExpanded,
                    onExpandedChange = { isChapterDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = chapter,
                        onValueChange = { chapter = it },
                        label = { Text("Chapter / Topic") },
                        placeholder = { Text("e.g. Newton's Laws or select chapter") },
                        trailingIcon = {
                            if (availableChapters.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isChapterDropdownExpanded)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                            .fillMaxWidth()
                            .testTag("library_note_chapter_input")
                    )
                    if (availableChapters.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = isChapterDropdownExpanded,
                            onDismissRequest = { isChapterDropdownExpanded = false }
                        ) {
                            availableChapters.forEach { ch ->
                                DropdownMenuItem(
                                    text = { Text("Ch ${ch.chapterNumber}: ${ch.title}") },
                                    onClick = {
                                        chapter = ch.title
                                        isChapterDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Resource Category Selector
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { isCategoryDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                            .testTag("library_note_category_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false }
                    ) {
                        STUDY_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    isCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Note Content / Text Editor
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note Content & Formulas") },
                    placeholder = { Text("Key definitions, formulas, revision points, exam tips...") },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_note_content_input")
                )

                // Attachments Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Attachments (${attachments.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isImportingFiles) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons to Attach Files
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                documentPickerLauncher.launch(
                                    arrayOf("application/pdf", "application/msword", "text/*", "*/*")
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("attach_pdf_button"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Attach PDF / Doc", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("attach_photo_button"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Attach Photos", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Display attached files
                    if (attachments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            attachments.forEachIndexed { index, att ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
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
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = att.fileName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = att.fileSizeFormatted,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = if (att.storageType == "FIREBASE") "• Cloud ☁️" else "• Device 📱",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (att.storageType == "FIREBASE") Color(0xFF10B981) else Color(0xFF64748B)
                                                )
                                            }
                                        }

                                        // Rename file
                                        IconButton(
                                            onClick = { attachmentToRename = att },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(16.dp))
                                        }

                                        // Remove file
                                        IconButton(
                                            onClick = { attachments.removeAt(index) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Pin toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPinned = !isPinned }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pin this note to the top of your library", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = "Title is required"
                        return@Button
                    }
                    onSave(
                        title.trim(),
                        content.trim(),
                        selectedSubjectId,
                        isPinned,
                        chapter.trim(),
                        category.trim(),
                        attachments.toList()
                    )
                },
                modifier = Modifier.testTag("save_library_note_button")
            ) {
                Text(if (noteToEdit == null) "Create Note" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_library_note_button")
            ) {
                Text("Cancel")
            }
        }
    )

    if (attachmentToRename != null) {
        RenameAttachmentDialog(
            currentName = attachmentToRename!!.fileName,
            onDismiss = { attachmentToRename = null },
            onConfirm = { newName ->
                val index = attachments.indexOfFirst { it.id == attachmentToRename!!.id }
                if (index != -1) {
                    attachments[index] = StudyLibraryStorageManager.renameAttachment(attachments[index], newName)
                }
                attachmentToRename = null
            }
        )
    }
}
