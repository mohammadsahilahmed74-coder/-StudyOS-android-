package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subjectId: Long? = null,
    val examDateMillis: Long,
    val targetScore: String = "95%",
    val notes: String = ""
)
