package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChapterEntity
import com.example.data.model.ExamEntity
import com.example.data.model.NoteEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TimetableSlotEntity
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Utility for parsing color hex safely
fun parseHexColor(hex: String?, defaultColor: Color = PrimaryIndigo): Color {
    if (hex.isNullOrBlank()) return defaultColor
    return try {
        val cleanHex = if (hex.startsWith("#")) hex.substring(1) else hex
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        defaultColor
    }
}

val SubjectPresetColors = listOf(
    "#2563EB", // Blue
    "#059669", // Emerald
    "#7C3AED", // Violet
    "#D97706", // Amber
    "#DB2777", // Rose
    "#0D9488", // Teal
    "#DC2626", // Red
    "#4F46E5"  // Indigo
)

@Composable
fun SubjectPill(
    subject: SubjectEntity?,
    modifier: Modifier = Modifier
) {
    val color = parseHexColor(subject?.colorHex, MaterialTheme.colorScheme.primary)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = subject?.name ?: "General",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val (color, label) = when (priority.uppercase()) {
        "HIGH" -> PriorityHigh to "High"
        "LOW" -> PriorityLow to "Low"
        else -> PriorityMedium to "Medium"
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun ChapterStatusBadge(status: String) {
    val (color, label) = when (status.uppercase()) {
        "COMPLETED" -> AccentEmerald to "Done"
        "IN_PROGRESS" -> PrimaryIndigo to "In Progress"
        else -> MaterialTheme.colorScheme.onSurfaceVariant to "Not Started"
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp
        )
    }
}

@Composable
fun SlotTypeBadge(type: String) {
    val (color, label) = when (type.uppercase()) {
        "LECTURE" -> PrimaryIndigo to "Lecture"
        "SELF_STUDY" -> AccentEmerald to "Self Study"
        "REVISION" -> AccentAmber to "Revision"
        "PRACTICE" -> AccentRose to "Practice"
        else -> MaterialTheme.colorScheme.primary to type
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("empty_state_action_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(actionText)
                }
            }
        }
    }
}

// ----------------- DIALOGS -----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(
    taskToEdit: TaskEntity?,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, subjectId: Long?, dueDateMillis: Long, priority: String, estimatedMinutes: Int) -> Unit
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var selectedSubjectId by remember { mutableStateOf(taskToEdit?.subjectId) }
    var priority by remember { mutableStateOf(taskToEdit?.priority ?: "MEDIUM") }
    var estimatedMinutes by remember { mutableIntStateOf(taskToEdit?.estimatedMinutes ?: 45) }
    var dueDateMillis by remember { mutableLongStateOf(taskToEdit?.dueDateMillis ?: System.currentTimeMillis()) }
    var isSubjectDropdownExpanded by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (taskToEdit == null) "Add Study Task" else "Edit Study Task",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = null
                    },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Solve Physics Chapter 3 numericals") },
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Instructions") },
                    placeholder = { Text("Notes, book pages, questions...") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_description_input")
                )

                // Subject Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = isSubjectDropdownExpanded,
                    onExpandedChange = { isSubjectDropdownExpanded = it }
                ) {
                    val currentSubjectName = subjects.find { it.id == selectedSubjectId }?.name ?: "No Subject (General)"
                    OutlinedTextField(
                        value = currentSubjectName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSubjectDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("task_subject_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = isSubjectDropdownExpanded,
                        onDismissRequest = { isSubjectDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No Subject (General)") },
                            onClick = {
                                selectedSubjectId = null
                                isSubjectDropdownExpanded = false
                            }
                        )
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(parseHexColor(subject.colorHex))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(subject.name)
                                    }
                                },
                                onClick = {
                                    selectedSubjectId = subject.id
                                    isSubjectDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Priority Selection
                Column {
                    Text("Priority Level", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("HIGH" to PriorityHigh, "MEDIUM" to PriorityMedium, "LOW" to PriorityLow).forEach { (pKey, color) ->
                            val isSelected = priority.equals(pKey, ignoreCase = true)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { priority = pKey }
                                    .testTag("priority_button_$pKey"),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) color else color.copy(alpha = 0.12f),
                                contentColor = if (isSelected) Color.White else color,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pKey.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Due Date Quick Options
                Column {
                    Text("Due Date", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val now = System.currentTimeMillis()
                        val oneDay = 86400000L
                        listOf(
                            "Today" to now,
                            "Tomorrow" to now + oneDay,
                            "In 3 Days" to now + (3 * oneDay)
                        ).forEach { (label, timestamp) ->
                            val isChosen = Math.abs(dueDateMillis - timestamp) < 12 * 3600000L
                            OutlinedButton(
                                onClick = { dueDateMillis = timestamp },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isChosen) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Text(label, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }

                // Estimated Study Time (Minutes)
                OutlinedTextField(
                    value = estimatedMinutes.toString(),
                    onValueChange = {
                        estimatedMinutes = it.filter { char -> char.isDigit() }.toIntOrNull() ?: 30
                    },
                    label = { Text("Est. Minutes (e.g. 45)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = "Task title is required"
                        return@Button
                    }
                    onSave(title, description, selectedSubjectId, dueDateMillis, priority, estimatedMinutes)
                },
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text("Save Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_task_button")) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SubjectDialog(
    subjectToEdit: SubjectEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String, iconKey: String, targetHours: Int) -> Unit
) {
    var name by remember { mutableStateOf(subjectToEdit?.name ?: "") }
    var selectedColor by remember { mutableStateOf(subjectToEdit?.colorHex ?: SubjectPresetColors[0]) }
    var targetHours by remember { mutableIntStateOf(subjectToEdit?.targetHoursPerWeek ?: 6) }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (subjectToEdit == null) "Add New Subject" else "Edit Subject", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    label = { Text("Subject Name *") },
                    placeholder = { Text("e.g. Mathematics, Economics") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_name_input")
                )

                // Color picker
                Column {
                    Text("Subject Accent Color", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SubjectPresetColors.forEach { hex ->
                            val color = parseHexColor(hex)
                            val isSelected = selectedColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = hex }
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = targetHours.toString(),
                    onValueChange = {
                        targetHours = it.filter { char -> char.isDigit() }.toIntOrNull() ?: 5
                    },
                    label = { Text("Target Hours per Week") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Subject name cannot be empty"
                        return@Button
                    }
                    onSave(name, selectedColor, "book", targetHours)
                },
                modifier = Modifier.testTag("save_subject_button")
            ) {
                Text("Save Subject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_subject_button")) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChapterDialog(
    chapterToEdit: ChapterEntity?,
    subjectId: Long,
    nextChapterNumber: Int,
    onDismiss: () -> Unit,
    onSave: (chapterNumber: Int, title: String, notes: String) -> Unit
) {
    var chapterNumber by remember { mutableIntStateOf(chapterToEdit?.chapterNumber ?: nextChapterNumber) }
    var title by remember { mutableStateOf(chapterToEdit?.title ?: "") }
    var notes by remember { mutableStateOf(chapterToEdit?.notes ?: "") }
    var titleError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (chapterToEdit == null) "Add Chapter / Unit" else "Edit Chapter", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = chapterNumber.toString(),
                    onValueChange = {
                        chapterNumber = it.filter { char -> char.isDigit() }.toIntOrNull() ?: 1
                    },
                    label = { Text("Chapter Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = null
                    },
                    label = { Text("Chapter Title *") },
                    placeholder = { Text("e.g. Thermodynamics & Heat") },
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chapter_title_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Study Notes / Key Topics") },
                    placeholder = { Text("Formulas, derivations, important questions...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = "Chapter title is required"
                        return@Button
                    }
                    onSave(chapterNumber, title, notes)
                },
                modifier = Modifier.testTag("save_chapter_button")
            ) {
                Text("Save Chapter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_chapter_button")) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableSlotDialog(
    slotToEdit: TimetableSlotEntity?,
    defaultDayOfWeek: Int,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (dayOfWeek: Int, startTime: String, endTime: String, subjectId: Long?, topic: String, slotType: String, room: String) -> Unit
) {
    var dayOfWeek by remember { mutableIntStateOf(slotToEdit?.dayOfWeek ?: defaultDayOfWeek) }
    var startTime by remember { mutableStateOf(slotToEdit?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(slotToEdit?.endTime ?: "10:30") }
    var selectedSubjectId by remember { mutableStateOf(slotToEdit?.subjectId) }
    var topic by remember { mutableStateOf(slotToEdit?.topic ?: "") }
    var slotType by remember { mutableStateOf(slotToEdit?.slotType ?: "SELF_STUDY") }
    var room by remember { mutableStateOf(slotToEdit?.roomOrLink ?: "") }
    var isSubjectDropdownExpanded by remember { mutableStateOf(false) }

    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (slotToEdit == null) "Add Study Timetable Slot" else "Edit Timetable Slot", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Day of week selector
                Column {
                    Text("Day of Week", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        days.forEachIndexed { index, dayName ->
                            val currentDayNum = index + 1
                            val isSelected = dayOfWeek == currentDayNum
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { dayOfWeek = currentDayNum },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(dayName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        placeholder = { Text("09:00") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        placeholder = { Text("10:30") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Subject Selector
                ExposedDropdownMenuBox(
                    expanded = isSubjectDropdownExpanded,
                    onExpandedChange = { isSubjectDropdownExpanded = it }
                ) {
                    val subjectName = subjects.find { it.id == selectedSubjectId }?.name ?: "General Study"
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSubjectDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isSubjectDropdownExpanded,
                        onDismissRequest = { isSubjectDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("General Study") },
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

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic / Activity") },
                    placeholder = { Text("e.g. Friction problems, Organic chemistry") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Slot Type Chips
                Column {
                    Text("Slot Type", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "SELF_STUDY" to "Self Study",
                            "LECTURE" to "Lecture",
                            "REVISION" to "Revision",
                            "PRACTICE" to "Practice"
                        ).forEach { (tKey, tLabel) ->
                            val isSelected = slotType.equals(tKey, ignoreCase = true)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { slotType = tKey },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(tLabel, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Location / Room (Optional)") },
                    placeholder = { Text("e.g. Study Desk, Room 204") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(dayOfWeek, startTime, endTime, selectedSubjectId, topic, slotType, room)
                },
                modifier = Modifier.testTag("save_slot_button")
            ) {
                Text("Save Slot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_slot_button")) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDialog(
    noteToEdit: NoteEntity?,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, subjectId: Long?, isPinned: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
    var content by remember { mutableStateOf(noteToEdit?.content ?: "") }
    var selectedSubjectId by remember { mutableStateOf(noteToEdit?.subjectId) }
    var isPinned by remember { mutableStateOf(noteToEdit?.isPinned ?: false) }
    var isSubjectDropdownExpanded by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (noteToEdit == null) "Create Study Note" else "Edit Note", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = null
                    },
                    label = { Text("Note Title *") },
                    placeholder = { Text("e.g. Kinematics Derivation Summary") },
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )

                // Subject category
                ExposedDropdownMenuBox(
                    expanded = isSubjectDropdownExpanded,
                    onExpandedChange = { isSubjectDropdownExpanded = it }
                ) {
                    val sName = subjects.find { it.id == selectedSubjectId }?.name ?: "General Notes"
                    OutlinedTextField(
                        value = sName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSubjectDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isSubjectDropdownExpanded,
                        onDismissRequest = { isSubjectDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("General Notes") },
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

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note Content") },
                    placeholder = { Text("Write formulas, concepts, reminders, revision bullet points...") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_content_input")
                )

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
                    Text("Pin this note to the top", style = MaterialTheme.typography.bodyMedium)
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
                    onSave(title, content, selectedSubjectId, isPinned)
                },
                modifier = Modifier.testTag("save_note_button")
            ) {
                Text("Save Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_note_button")) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDialog(
    examToEdit: ExamEntity?,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (title: String, subjectId: Long?, examDateMillis: Long, targetScore: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf(examToEdit?.title ?: "") }
    var selectedSubjectId by remember { mutableStateOf(examToEdit?.subjectId) }
    var targetScore by remember { mutableStateOf(examToEdit?.targetScore ?: "95%") }
    var notes by remember { mutableStateOf(examToEdit?.notes ?: "") }
    var daysInFuture by remember {
        val days = if (examToEdit != null) {
            val diff = examToEdit.examDateMillis - System.currentTimeMillis()
            (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(1).toInt()
        } else {
            30
        }
        mutableIntStateOf(days)
    }
    var isSubjectDropdownExpanded by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (examToEdit == null) "Add Exam Countdown" else "Edit Exam", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = null
                    },
                    label = { Text("Exam / Test Title *") },
                    placeholder = { Text("e.g. Physics Final Exam") },
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exam_title_input")
                )

                // Subject
                ExposedDropdownMenuBox(
                    expanded = isSubjectDropdownExpanded,
                    onExpandedChange = { isSubjectDropdownExpanded = it }
                ) {
                    val sName = subjects.find { it.id == selectedSubjectId }?.name ?: "All Subjects / General Exam"
                    OutlinedTextField(
                        value = sName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSubjectDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isSubjectDropdownExpanded,
                        onDismissRequest = { isSubjectDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Subjects / General Exam") },
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

                // Days from now
                OutlinedTextField(
                    value = daysInFuture.toString(),
                    onValueChange = {
                        daysInFuture = it.filter { char -> char.isDigit() }.toIntOrNull() ?: 1
                    },
                    label = { Text("Days until Exam") },
                    supportingText = {
                        val targetDate = Date(System.currentTimeMillis() + (daysInFuture * 86400000L))
                        Text("Exam Date: " + SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(targetDate))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetScore,
                    onValueChange = { targetScore = it },
                    label = { Text("Target Score / Grade") },
                    placeholder = { Text("e.g. 95% or A+") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Exam Notes / Strategy") },
                    placeholder = { Text("Syllabus coverage, revision checklist...") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = "Exam title is required"
                        return@Button
                    }
                    val examDateMillis = System.currentTimeMillis() + (daysInFuture * 86400000L)
                    onSave(title, selectedSubjectId, examDateMillis, targetScore, notes)
                },
                modifier = Modifier.testTag("save_exam_button")
            ) {
                Text("Save Exam")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_exam_button")) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_delete_button")) {
                Text("Cancel")
            }
        }
    )
}
