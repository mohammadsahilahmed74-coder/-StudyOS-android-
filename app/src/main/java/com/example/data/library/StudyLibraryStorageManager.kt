package com.example.data.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.data.model.NoteAttachment
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.util.UUID

object StudyLibraryStorageManager {

    private const val TAG = "StudyLibraryStorage"
    private const val ATTACHMENTS_DIR = "study_attachments"

    fun getAttachmentsDirectory(context: Context): File {
        val dir = File(context.filesDir, ATTACHMENTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Imports a user-selected file Uri into app's persistent internal storage.
     */
    suspend fun importFileUri(
        context: Context,
        uri: Uri,
        customName: String? = null
    ): NoteAttachment = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        var displayName = customName ?: ""
        var sizeBytes = 0L

        if (displayName.isBlank()) {
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex) ?: ""
                        }
                        if (sizeIndex != -1) {
                            sizeBytes = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query Uri metadata: ${e.message}")
            }
        }

        if (displayName.isBlank()) {
            val lastPathSegment = uri.lastPathSegment ?: "document_${System.currentTimeMillis()}"
            displayName = lastPathSegment.substringAfterLast('/')
        }

        val mimeType = contentResolver.getType(uri) ?: getMimeTypeFromFileName(displayName)
        val fileType = determineFileType(displayName, mimeType)

        val id = UUID.randomUUID().toString()
        val extension = displayName.substringAfterLast('.', "")
        val safeExtension = if (extension.isNotBlank()) ".$extension" else ""
        val sanitizedBase = displayName.substringBeforeLast('.')
            .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            .take(30)
        val targetFileName = "${id}_${sanitizedBase}$safeExtension"

        val targetFile = File(getAttachmentsDirectory(context), targetFileName)

        try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (sizeBytes <= 0L && targetFile.exists()) {
                sizeBytes = targetFile.length()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file from uri: ${e.message}", e)
        }

        val formattedSize = formatFileSize(sizeBytes)

        val initialAttachment = NoteAttachment(
            id = id,
            fileName = displayName,
            fileType = fileType,
            mimeType = mimeType,
            fileSizeFormatted = formattedSize,
            fileSizeBytes = sizeBytes,
            localPath = targetFile.absolutePath,
            remoteUrl = "",
            storageType = "LOCAL",
            createdAtMillis = System.currentTimeMillis()
        )

        // Try syncing to Firebase Storage if Google authenticated
        uploadToFirebaseIfAuthenticated(context, initialAttachment)
    }

    /**
     * Uploads the file to Firebase Storage if the user is signed in with a Google account.
     * If unauthenticated, offline, or if Storage is not configured, retains local copy gracefully.
     */
    suspend fun uploadToFirebaseIfAuthenticated(
        context: Context,
        attachment: NoteAttachment
    ): NoteAttachment = withContext(Dispatchers.IO) {
        try {
            val user = try { Firebase.auth.currentUser } catch (_: Exception) { null }
            if (user == null) {
                return@withContext attachment.copy(storageType = "LOCAL")
            }

            val localFile = File(attachment.localPath)
            if (!localFile.exists()) {
                return@withContext attachment
            }

            val storage = try { Firebase.storage } catch (_: Exception) { null }
            if (storage == null) {
                return@withContext attachment.copy(storageType = "LOCAL")
            }

            val storageRef = storage.reference
                .child("users")
                .child(user.uid)
                .child("study_materials")
                .child("${attachment.id}_${localFile.name}")

            val uploadTask = storageRef.putFile(Uri.fromFile(localFile)).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            Log.i(TAG, "Successfully uploaded attachment to Firebase Storage: $downloadUrl")
            attachment.copy(
                remoteUrl = downloadUrl,
                storageType = "FIREBASE"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Storage upload skipped or failed (${e.message}), retaining local storage.")
            attachment.copy(storageType = "LOCAL")
        }
    }

    /**
     * Renames an attachment display name.
     */
    fun renameAttachment(attachment: NoteAttachment, newName: String): NoteAttachment {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return attachment

        // Preserve original extension if user omitted it
        val origExt = attachment.fileName.substringAfterLast('.', "")
        val finalName = if (!trimmed.contains('.') && origExt.isNotBlank()) {
            "$trimmed.$origExt"
        } else {
            trimmed
        }

        return attachment.copy(
            fileName = finalName,
            fileType = determineFileType(finalName, attachment.mimeType)
        )
    }

    /**
     * Deletes the attachment from local storage and remote Firebase Storage if present.
     */
    suspend fun deleteAttachment(context: Context, attachment: NoteAttachment) = withContext(Dispatchers.IO) {
        try {
            if (attachment.localPath.isNotBlank()) {
                val file = File(attachment.localPath)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting local attachment: ${e.message}")
        }

        if (attachment.remoteUrl.isNotBlank()) {
            try {
                val storage = try { Firebase.storage } catch (_: Exception) { null }
                storage?.getReferenceFromUrl(attachment.remoteUrl)?.delete()?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting from Firebase Storage: ${e.message}")
            }
        }
    }

    /**
     * Exports/Shares the file via Android Share Sheet using FileProvider.
     */
    fun exportAttachment(context: Context, attachment: NoteAttachment) {
        val file = File(attachment.localPath)
        if (!file.exists()) {
            Log.e(TAG, "Cannot export file because local path does not exist: ${attachment.localPath}")
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = attachment.mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, attachment.fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Save or Share ${attachment.fileName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share attachment: ${e.message}", e)
        }
    }

    /**
     * Launches an external viewer intent using FileProvider if the student wants external app handling.
     */
    fun openAttachmentExternally(context: Context, attachment: NoteAttachment) {
        val file = File(attachment.localPath)
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: ${attachment.localPath}")
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, attachment.mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not open attachment externally: ${e.message}")
            // Fallback to sharing
            exportAttachment(context, attachment)
        }
    }

    fun determineFileType(fileName: String, mimeType: String?): String {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.endsWith(".pdf") || mimeType == "application/pdf" -> "pdf"
            lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                    lowerName.endsWith(".png") || lowerName.endsWith(".webp") ||
                    mimeType?.startsWith("image/") == true -> "image"
            lowerName.endsWith(".doc") || lowerName.endsWith(".docx") ||
                    lowerName.endsWith(".txt") || lowerName.endsWith(".ppt") ||
                    lowerName.endsWith(".pptx") || lowerName.endsWith(".xls") ||
                    lowerName.endsWith(".xlsx") || mimeType?.startsWith("text/") == true -> "document"
            else -> "other"
        }
    }

    private fun getMimeTypeFromFileName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension.isBlank()) return "*/*"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return "${DecimalFormat("#,##0.#").format(value)} ${units[index]}"
    }

    /**
     * Creates a sample bundled study document if needed for quick first-launch verification.
     */
    fun createSamplePdfIfMissing(context: Context): NoteAttachment? {
        return try {
            val dir = getAttachmentsDirectory(context)
            val sampleFile = File(dir, "Physics_Mechanics_Formulas_Summary.pdf")
            if (!sampleFile.exists()) {
                var generated = false
                try {
                    val pdfDoc = android.graphics.pdf.PdfDocument()
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
                    val page = pdfDoc.startPage(pageInfo)
                    val canvas = page.canvas

                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.rgb(18, 24, 38)
                        textSize = 24f
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                    canvas.drawText("StudyOS — Physics Mechanics Summary", 40f, 60f, paint)

                    paint.apply {
                        color = android.graphics.Color.rgb(90, 105, 120)
                        textSize = 13f
                        isFakeBoldText = false
                    }
                    canvas.drawText("Comprehensive revision notes and formula sheets for semester exams", 40f, 85f, paint)

                    // Divider line
                    val linePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.rgb(200, 210, 225)
                        strokeWidth = 2f
                    }
                    canvas.drawLine(40f, 105f, 555f, 105f, linePaint)

                    // Content lines
                    val bodyPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.rgb(30, 41, 59)
                        textSize = 14f
                        isAntiAlias = true
                    }

                    val formulas = listOf(
                        "1. Kinematic Equations (Constant Acceleration):",
                        "   • v = u + at",
                        "   • s = ut + 0.5 * a * t²",
                        "   • v² = u² + 2as",
                        "   • s_n = u + 0.5 * a * (2n - 1)",
                        "",
                        "2. Newton's Laws & Dynamics:",
                        "   • Net Force: F = dp/dt = m · a",
                        "   • Friction Force: f_s ≤ μ_s · N  |  f_k = μ_k · N",
                        "   • Momentum Conservation: Σ p_initial = Σ p_final",
                        "   • Impulse: J = ∫ F dt = Δp",
                        "",
                        "3. Work, Power & Energy Theorem:",
                        "   • Work: W = ∫ F · dr = F · d · cos(θ)",
                        "   • Kinetic Energy: KE = 0.5 · m · v²",
                        "   • Work-Energy Theorem: W_net = ΔKE",
                        "   • Instantaneous Power: P = dW/dt = F · v",
                        "",
                        "4. Circular & Rotational Motion:",
                        "   • Centripetal Acceleration: a_c = v² / r = ω² · r",
                        "   • Torque: τ = r × F = I · α",
                        "   • Angular Momentum: L = I · ω",
                        "   • Rotational KE: KE_rot = 0.5 · I · ω²"
                    )

                    var y = 140f
                    for (line in formulas) {
                        canvas.drawText(line, 40f, y, bodyPaint)
                        y += 26f
                    }

                    pdfDoc.finishPage(page)
                    FileOutputStream(sampleFile).use { out ->
                        pdfDoc.writeTo(out)
                    }
                    pdfDoc.close()
                    generated = true
                } catch (_: Throwable) {
                    generated = false
                }

                if (!generated || !sampleFile.exists() || sampleFile.length() == 0L) {
                    val rawPdf = "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n3 0 obj<</Type/Page/MediaBox[0 0 595 842]/Parent 2 0 R/Resources<<>>>>endobj\nxref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000056 00000 n \n0000000111 00000 n \ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n190\n%%EOF\n"
                    sampleFile.writeText(rawPdf)
                }
            }

            NoteAttachment(
                id = "sample_pdf_mechanics",
                fileName = "Physics_Mechanics_Formulas_Summary.pdf",
                fileType = "pdf",
                mimeType = "application/pdf",
                fileSizeFormatted = formatFileSize(sampleFile.length()),
                fileSizeBytes = sampleFile.length(),
                localPath = sampleFile.absolutePath,
                remoteUrl = "",
                storageType = "LOCAL",
                createdAtMillis = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not generate sample PDF: ${e.message}")
            null
        }
    }
}
