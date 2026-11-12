package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetable_slots")
data class TimetableSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int, // 1 = Monday .. 7 = Sunday
    val startTime: String, // e.g. "08:30"
    val endTime: String,   // e.g. "10:00"
    val subjectId: Long? = null,
    val topic: String = "",
    val slotType: String = "SELF_STUDY", // LECTURE, SELF_STUDY, REVISION, PRACTICE
    val roomOrLink: String = ""
)
