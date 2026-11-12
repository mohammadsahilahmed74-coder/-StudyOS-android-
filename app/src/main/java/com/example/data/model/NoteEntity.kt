package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val subjectId: Long? = null,
    val isPinned: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val chapter: String = "",
    val category: String = "Lecture Notes",
    val attachmentsJson: String = "[]"
) {
    fun getAttachments(): List<NoteAttachment> {
        return NoteAttachment.parseList(attachmentsJson)
    }

    fun withAttachments(attachments: List<NoteAttachment>): NoteEntity {
        return copy(
            attachmentsJson = NoteAttachment.toJsonString(attachments),
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    val attachmentCount: Int
        get() = getAttachments().size

    val hasPdfs: Boolean
        get() = getAttachments().any { it.isPdf }

    val hasImages: Boolean
        get() = getAttachments().any { it.isImage }
}
