package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Represents a study material file attached to a note in the Digital Study Library.
 */
data class NoteAttachment(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val fileType: String = "other", // "pdf", "image", "document", "other"
    val mimeType: String = "*/*",
    val fileSizeFormatted: String = "",
    val fileSizeBytes: Long = 0L,
    val localPath: String = "",
    val remoteUrl: String = "",
    val storageType: String = "LOCAL", // "LOCAL" or "FIREBASE"
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val isPdf: Boolean
        get() = fileType == "pdf" || fileName.endsWith(".pdf", ignoreCase = true) || mimeType == "application/pdf"

    val isImage: Boolean
        get() = fileType == "image" ||
                fileName.endsWith(".jpg", ignoreCase = true) ||
                fileName.endsWith(".jpeg", ignoreCase = true) ||
                fileName.endsWith(".png", ignoreCase = true) ||
                fileName.endsWith(".webp", ignoreCase = true) ||
                mimeType.startsWith("image/")

    val isDocument: Boolean
        get() = fileType == "document" ||
                fileName.endsWith(".doc", ignoreCase = true) ||
                fileName.endsWith(".docx", ignoreCase = true) ||
                fileName.endsWith(".txt", ignoreCase = true) ||
                fileName.endsWith(".ppt", ignoreCase = true) ||
                fileName.endsWith(".pptx", ignoreCase = true) ||
                fileName.endsWith(".xls", ignoreCase = true) ||
                fileName.endsWith(".xlsx", ignoreCase = true)

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("fileName", fileName)
            put("fileType", fileType)
            put("mimeType", mimeType)
            put("fileSizeFormatted", fileSizeFormatted)
            put("fileSizeBytes", fileSizeBytes)
            put("localPath", localPath)
            put("remoteUrl", remoteUrl)
            put("storageType", storageType)
            put("createdAtMillis", createdAtMillis)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): NoteAttachment {
            return NoteAttachment(
                id = json.optString("id", UUID.randomUUID().toString()),
                fileName = json.optString("fileName", "Document"),
                fileType = json.optString("fileType", "other"),
                mimeType = json.optString("mimeType", "*/*"),
                fileSizeFormatted = json.optString("fileSizeFormatted", ""),
                fileSizeBytes = json.optLong("fileSizeBytes", 0L),
                localPath = json.optString("localPath", ""),
                remoteUrl = json.optString("remoteUrl", ""),
                storageType = json.optString("storageType", "LOCAL"),
                createdAtMillis = json.optLong("createdAtMillis", System.currentTimeMillis())
            )
        }

        fun parseList(jsonString: String?): List<NoteAttachment> {
            if (jsonString.isNullOrBlank() || jsonString == "[]") return emptyList()
            val list = mutableListOf<NoteAttachment>()
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(fromJson(obj))
                }
            } catch (_: Exception) {}
            return list
        }

        fun toJsonString(list: List<NoteAttachment>): String {
            val array = JSONArray()
            list.forEach { array.put(it.toJson()) }
            return array.toString()
        }
    }
}
