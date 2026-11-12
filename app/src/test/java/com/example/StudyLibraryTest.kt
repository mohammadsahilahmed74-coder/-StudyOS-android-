package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.library.StudyLibraryStorageManager
import com.example.data.model.NoteAttachment
import com.example.data.model.NoteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudyLibraryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testNoteAttachmentSerialization() {
        val attachment = NoteAttachment(
            id = "att_123",
            fileName = "Mechanics_Formula_Sheet.pdf",
            fileType = "pdf",
            mimeType = "application/pdf",
            fileSizeFormatted = "1.2 MB",
            fileSizeBytes = 1258291L,
            localPath = "/data/user/0/com.example/files/study_attachments/att_123.pdf",
            remoteUrl = "https://firebasestorage.googleapis.com/v0/b/...",
            storageType = "FIREBASE",
            createdAtMillis = 1700000000000L
        )

        assertTrue(attachment.isPdf)
        assertFalse(attachment.isImage)
        assertFalse(attachment.isDocument)

        val json = attachment.toJson()
        val parsed = NoteAttachment.fromJson(json)

        assertEquals("att_123", parsed.id)
        assertEquals("Mechanics_Formula_Sheet.pdf", parsed.fileName)
        assertEquals("pdf", parsed.fileType)
        assertEquals("1.2 MB", parsed.fileSizeFormatted)
        assertEquals(1258291L, parsed.fileSizeBytes)
        assertEquals("FIREBASE", parsed.storageType)
    }

    @Test
    fun testMultipleAttachmentsListJson() {
        val list = listOf(
            NoteAttachment(
                id = "1",
                fileName = "Chapter3_Summary.pdf",
                fileType = "pdf",
                fileSizeFormatted = "500 KB"
            ),
            NoteAttachment(
                id = "2",
                fileName = "Whiteboard_Diagram.png",
                fileType = "image",
                fileSizeFormatted = "2.1 MB"
            )
        )

        val jsonString = NoteAttachment.toJsonString(list)
        val deserialized = NoteAttachment.parseList(jsonString)

        assertEquals(2, deserialized.size)
        assertEquals("Chapter3_Summary.pdf", deserialized[0].fileName)
        assertTrue(deserialized[0].isPdf)
        assertEquals("Whiteboard_Diagram.png", deserialized[1].fileName)
        assertTrue(deserialized[1].isImage)
    }

    @Test
    fun testNoteEntityAttachmentHelpers() {
        val attachments = listOf(
            NoteAttachment(id = "pdf_1", fileName = "Exam_Paper_2025.pdf", fileType = "pdf"),
            NoteAttachment(id = "img_1", fileName = "Circuit_Diagram.jpg", fileType = "image")
        )

        val note = NoteEntity(
            id = 10,
            title = "Physics Final Prep",
            content = "Read chapter 4 and solve numericals",
            chapter = "Current Electricity",
            category = "Past Papers"
        ).withAttachments(attachments)

        assertEquals(2, note.attachmentCount)
        assertTrue(note.hasPdfs)
        assertTrue(note.hasImages)
        assertEquals("Current Electricity", note.chapter)
        assertEquals("Past Papers", note.category)

        val extracted = note.getAttachments()
        assertEquals(2, extracted.size)
        assertEquals("Exam_Paper_2025.pdf", extracted[0].fileName)
        assertEquals("Circuit_Diagram.jpg", extracted[1].fileName)
    }

    @Test
    fun testDetermineFileType() {
        assertEquals("pdf", StudyLibraryStorageManager.determineFileType("Physics_Notes.pdf", "application/pdf"))
        assertEquals("image", StudyLibraryStorageManager.determineFileType("diagram.png", "image/png"))
        assertEquals("image", StudyLibraryStorageManager.determineFileType("whiteboard.JPG", "image/jpeg"))
        assertEquals("document", StudyLibraryStorageManager.determineFileType("syllabus.docx", "application/msword"))
        assertEquals("document", StudyLibraryStorageManager.determineFileType("notes.txt", "text/plain"))
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", StudyLibraryStorageManager.formatFileSize(0L))
        assertEquals("512 B", StudyLibraryStorageManager.formatFileSize(512L))
        assertEquals("1.5 KB", StudyLibraryStorageManager.formatFileSize(1536L))
        assertEquals("2.5 MB", StudyLibraryStorageManager.formatFileSize(2621440L))
    }

    @Test
    fun testRenameAttachmentPreservesExtension() {
        val original = NoteAttachment(
            id = "test_1",
            fileName = "scan_001.pdf",
            fileType = "pdf",
            mimeType = "application/pdf"
        )

        val renamed = StudyLibraryStorageManager.renameAttachment(original, "Thermodynamics Derivations")
        assertEquals("Thermodynamics Derivations.pdf", renamed.fileName)
        assertEquals("pdf", renamed.fileType)

        // If user explicitly provided extension
        val renamedWithExt = StudyLibraryStorageManager.renameAttachment(original, "CheatSheet.pdf")
        assertEquals("CheatSheet.pdf", renamedWithExt.fileName)
    }

    @Test
    fun testSearchNotesByAttachmentFileName() {
        val note1 = NoteEntity(
            id = 1,
            title = "Calculus Derivatives",
            content = "Product rule and quotient rule",
            chapter = "Differentiation",
            category = "Lecture Notes"
        ).withAttachments(
            listOf(NoteAttachment(id = "c1", fileName = "Calculus_Derivation_Tables.pdf", fileType = "pdf"))
        )

        val note2 = NoteEntity(
            id = 2,
            title = "Organic Chemistry Reactions",
            content = "Benzene electrophilic substitution mechanisms",
            chapter = "Hydrocarbons",
            category = "Summary & Diagrams"
        ).withAttachments(
            listOf(NoteAttachment(id = "c2", fileName = "Benzene_Reaction_Chart.png", fileType = "image"))
        )

        val allNotes = listOf(note1, note2)

        // Searching by attachment name "Reaction_Chart" should find note2!
        val searchResults = allNotes.filter { note ->
            val query = "Reaction_Chart"
            note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true) ||
                    note.getAttachments().any { it.fileName.contains(query, ignoreCase = true) }
        }

        assertEquals(1, searchResults.size)
        assertEquals(2L, searchResults[0].id)
        assertEquals("Organic Chemistry Reactions", searchResults[0].title)
    }

    @Test
    fun testSamplePdfGeneration() {
        val samplePdf = StudyLibraryStorageManager.createSamplePdfIfMissing(context)
        assertNotNull(samplePdf)
        assertEquals("Physics_Mechanics_Formulas_Summary.pdf", samplePdf!!.fileName)
        assertTrue(samplePdf.isPdf)
        assertTrue(samplePdf.localPath.isNotBlank())
        val file = File(samplePdf.localPath)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }
}
