package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val chapterNumber: Int,
    val title: String,
    val status: String = "NOT_STARTED", // NOT_STARTED, IN_PROGRESS, COMPLETED
    val isRevisionDone: Boolean = false,
    val notes: String = "",
    val completedAtMillis: Long? = null
)
