package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SubjectEntity
import com.example.data.model.TimetableSlotEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SlotTypeBadge
import com.example.ui.components.TimetableSlotDialog
import com.example.ui.components.parseHexColor
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.viewmodel.StudyViewModel

val DayNamesFull = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
val DayNamesShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun TimetableScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val timetableSlots by viewModel.timetableSlots.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val selectedDayOfWeek by viewModel.selectedDayOfWeek.collectAsStateWithLifecycle()

    var viewMode by remember { mutableIntStateOf(0) } // 0 = Daily View, 1 = Weekly View
    var showAddSlotDialog by remember { mutableStateOf(false) }
    var slotToEdit by remember { mutableStateOf<TimetableSlotEntity?>(null) }
    var slotToDelete by remember { mutableStateOf<TimetableSlotEntity?>(null) }

    val currentDaySlots = timetableSlots
        .filter { it.dayOfWeek == selectedDayOfWeek }
        .sortedBy { it.startTime }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // View Mode Selector (Daily vs Weekly)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Study Timetable",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (viewMode == 0) "Daily routine for ${DayNamesFull[selectedDayOfWeek - 1]}" else "Complete 7-day weekly study schedule",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        Surface(
                            modifier = Modifier
                                .clickable { viewMode = 0 }
                                .testTag("timetable_view_daily"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (viewMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (viewMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Daily", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .clickable { viewMode = 1 }
                                .testTag("timetable_view_weekly"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (viewMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (viewMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ViewWeek, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Weekly", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (viewMode == 0) {
                // Day Selector Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DayNamesShort.forEachIndexed { index, dayName ->
                        val dayNum = index + 1
                        val isSelected = selectedDayOfWeek == dayNum
                        val count = timetableSlots.count { it.dayOfWeek == dayNum }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setSelectedDayOfWeek(dayNum) }
                                .testTag("day_selector_$dayNum"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$count slots",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Daily slots list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("daily_slots_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentDaySlots.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Default.CalendarMonth,
                                title = "No study slots scheduled for ${DayNamesFull[selectedDayOfWeek - 1]}",
                                subtitle = "Add study sessions, school lectures, or revision blocks",
                                actionText = "Add Study Slot",
                                onAction = { showAddSlotDialog = true }
                            )
                        }
                    } else {
                        items(currentDaySlots, key = { it.id }) { slot ->
                            val subject = subjects.find { it.id == slot.subjectId }
                            TimetableSlotCard(
                                slot = slot,
                                subject = subject,
                                onEdit = { slotToEdit = slot },
                                onDelete = { slotToDelete = slot }
                            )
                        }
                    }
                }
            } else {
                // Weekly Overview View: Grouped by Day
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("weekly_slots_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(listOf(1, 2, 3, 4, 5, 6, 7)) { dayNum ->
                        val daySlots = timetableSlots
                            .filter { it.dayOfWeek == dayNum }
                            .sortedBy { it.startTime }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = DayNamesFull[dayNum - 1],
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "${daySlots.size} Sessions",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (daySlots.isEmpty()) {
                                    Text(
                                        text = "No study slots scheduled",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    daySlots.forEach { slot ->
                                        val subject = subjects.find { it.id == slot.subjectId }
                                        TimetableSlotCard(
                                            slot = slot,
                                            subject = subject,
                                            onEdit = { slotToEdit = slot },
                                            onDelete = { slotToDelete = slot }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Slot FAB
        FloatingActionButton(
            onClick = { showAddSlotDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_slot_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Timetable Slot")
        }
    }

    // Timetable Slot Dialog
    if (showAddSlotDialog || slotToEdit != null) {
        TimetableSlotDialog(
            slotToEdit = slotToEdit,
            defaultDayOfWeek = selectedDayOfWeek,
            subjects = subjects,
            onDismiss = {
                showAddSlotDialog = false
                slotToEdit = null
            },
            onSave = { dayOfWeek, startTime, endTime, subjectId, topic, slotType, room ->
                if (slotToEdit == null) {
                    viewModel.insertSlot(dayOfWeek, startTime, endTime, subjectId, topic, slotType, room)
                } else {
                    viewModel.updateSlot(
                        slotToEdit!!.copy(
                            dayOfWeek = dayOfWeek,
                            startTime = startTime,
                            endTime = endTime,
                            subjectId = subjectId,
                            topic = topic,
                            slotType = slotType,
                            roomOrLink = room
                        )
                    )
                }
                showAddSlotDialog = false
                slotToEdit = null
            }
        )
    }

    // Confirm Delete Dialog
    if (slotToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Slot?",
            message = "Are you sure you want to delete this study slot (${slotToDelete!!.startTime} - ${slotToDelete!!.endTime})?",
            onConfirm = {
                viewModel.deleteSlot(slotToDelete!!)
                slotToDelete = null
            },
            onDismiss = { slotToDelete = null }
        )
    }
}

@Composable
fun TimetableSlotCard(
    slot: TimetableSlotEntity,
    subject: SubjectEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val subjectColor = parseHexColor(subject?.colorHex, PrimaryIndigo)
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("slot_card_${slot.id}"),
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
            // Time Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = subjectColor.copy(alpha = 0.15f),
                modifier = Modifier.width(74.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = slot.startTime,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = subjectColor
                    )
                    Text(
                        text = "to ${slot.endTime}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = subject?.name ?: "Self Study Session",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    SlotTypeBadge(type = slot.slotType)
                }

                if (slot.topic.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = slot.topic,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (slot.roomOrLink.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📍 ${slot.roomOrLink}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Slot options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Slot") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Slot", color = MaterialTheme.colorScheme.error) },
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
