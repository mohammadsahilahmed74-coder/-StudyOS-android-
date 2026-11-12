package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Reusable AlertDialog component containing text fields for 'Task Title' and 'Description',
 * enabling users to quickly and easily add new study tasks from anywhere in the app.
 */
@Composable
fun AddTaskAlertDialog(
    initialTitle: String = "",
    initialDescription: String = "",
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var titleError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("add_task_alert_dialog"),
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Add Study Task",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = null
                    },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Revise Physics Chapter 3") },
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_task_dialog_title_field")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Details, notes, formula sheets, page numbers...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_task_dialog_desc_field")
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
                    onConfirm(title.trim(), description.trim())
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("add_task_dialog_confirm_button")
            ) {
                Text("Add Task")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("add_task_dialog_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
