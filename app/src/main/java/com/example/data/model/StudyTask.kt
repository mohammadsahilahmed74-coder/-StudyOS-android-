package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * StudyTask Room entity class representing individual study tasks for the task manager.
 */
@Entity(tableName = "tasks")
data class StudyTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val description: String = "",

    @ColumnInfo(name = "dueDateMillis")
    val dueDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "isCompleted")
    val isCompleted: Boolean = false,

    val subjectId: Long? = null,

    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW

    val estimatedMinutes: Int = 45,

    val completedAtMillis: Long? = null
) {
    /**
     * Secondary constructor supporting dueDateMillis parameter name.
     */
    @Ignore
    constructor(
        id: Long = 0,
        title: String,
        description: String = "",
        subjectId: Long? = null,
        dueDateMillis: Long = System.currentTimeMillis(),
        priority: String = "MEDIUM",
        isCompleted: Boolean = false,
        estimatedMinutes: Int = 45,
        completedAtMillis: Long? = null
    ) : this(
        id = id,
        title = title,
        description = description,
        dueDate = dueDateMillis,
        isCompleted = isCompleted,
        subjectId = subjectId,
        priority = priority,
        estimatedMinutes = estimatedMinutes,
        completedAtMillis = completedAtMillis
    )

    val dueDateMillis: Long
        get() = dueDate

    val completionStatus: Boolean
        get() = isCompleted
}

/**
 * Typealias to maintain compatibility across all layers.
 */
typealias TaskEntity = StudyTask
