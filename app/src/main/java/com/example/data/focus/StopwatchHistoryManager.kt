package com.example.data.focus

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class SavedStopwatchSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Deep Work Session",
    val dateFormatted: String = "",
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val lapsCount: Int = 0,
    val subjectTag: String = "Study",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val formattedDuration: String
        get() {
            val totalSec = durationMillis / 1000
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            return if (h > 0) {
                String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", m, s)
            }
        }

    val formattedTimeRange: String
        get() {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val start = if (startTimeMillis > 0) timeFormat.format(Date(startTimeMillis)) else "--"
            val end = if (endTimeMillis > 0) timeFormat.format(Date(endTimeMillis)) else "--"
            return "$start - $end"
        }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("dateFormatted", dateFormatted)
            put("startTimeMillis", startTimeMillis)
            put("endTimeMillis", endTimeMillis)
            put("durationMillis", durationMillis)
            put("lapsCount", lapsCount)
            put("subjectTag", subjectTag)
            put("createdAtMillis", createdAtMillis)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): SavedStopwatchSession {
            return SavedStopwatchSession(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", "Deep Work Session"),
                dateFormatted = json.optString("dateFormatted", ""),
                startTimeMillis = json.optLong("startTimeMillis", 0L),
                endTimeMillis = json.optLong("endTimeMillis", 0L),
                durationMillis = json.optLong("durationMillis", 0L),
                lapsCount = json.optInt("lapsCount", 0),
                subjectTag = json.optString("subjectTag", "Study"),
                createdAtMillis = json.optLong("createdAtMillis", System.currentTimeMillis())
            )
        }
    }
}

object StopwatchHistoryManager {
    private const val TAG = "StopwatchHistoryMgr"
    private const val PREFS_NAME = "studyos_stopwatch_history_prefs"
    private const val KEY_SESSIONS = "saved_stopwatch_sessions_json"

    fun getSavedSessions(context: Context): List<SavedStopwatchSession> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SESSIONS, "[]") ?: "[]"
        val list = mutableListOf<SavedStopwatchSession>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                list.add(SavedStopwatchSession.fromJson(arr.getJSONObject(i)))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing saved stopwatch sessions: ${e.message}")
        }
        return list.sortedByDescending { it.createdAtMillis }
    }

    fun saveSessionsLocally(context: Context, list: List<SavedStopwatchSession>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_SESSIONS, arr.toString()).apply()
    }

    suspend fun saveSession(context: Context, session: SavedStopwatchSession) {
        val current = getSavedSessions(context).toMutableList()
        current.removeAll { it.id == session.id }
        current.add(0, session)
        saveSessionsLocally(context, current)

        // Sync to Firestore if authenticated
        withContext(Dispatchers.IO) {
            try {
                val user = try { Firebase.auth.currentUser } catch (_: Exception) { null }
                if (user != null) {
                    val db = try { Firebase.firestore } catch (_: Exception) { null }
                    if (db != null) {
                        val sessionData = hashMapOf(
                            "id" to session.id,
                            "title" to session.title,
                            "dateFormatted" to session.dateFormatted,
                            "startTimeMillis" to session.startTimeMillis,
                            "endTimeMillis" to session.endTimeMillis,
                            "durationMillis" to session.durationMillis,
                            "lapsCount" to session.lapsCount,
                            "subjectTag" to session.subjectTag,
                            "createdAtMillis" to session.createdAtMillis
                        )
                        db.collection("users").document(user.uid)
                            .collection("stopwatch_sessions")
                            .document(session.id)
                            .set(sessionData)
                            .await()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Optional Firestore sync for stopwatch session failed: ${e.message}")
            }
        }
    }

    suspend fun renameSession(context: Context, id: String, newTitle: String) {
        val current = getSavedSessions(context).toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = current[index].copy(title = newTitle.trim())
            current[index] = updated
            saveSessionsLocally(context, current)

            withContext(Dispatchers.IO) {
                try {
                    val user = try { Firebase.auth.currentUser } catch (_: Exception) { null }
                    if (user != null) {
                        Firebase.firestore.collection("users").document(user.uid)
                            .collection("stopwatch_sessions")
                            .document(id)
                            .update("title", newTitle.trim())
                            .await()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun deleteSession(context: Context, id: String) {
        val current = getSavedSessions(context).toMutableList()
        current.removeAll { it.id == id }
        saveSessionsLocally(context, current)

        withContext(Dispatchers.IO) {
            try {
                val user = try { Firebase.auth.currentUser } catch (_: Exception) { null }
                if (user != null) {
                    Firebase.firestore.collection("users").document(user.uid)
                        .collection("stopwatch_sessions")
                        .document(id)
                        .delete()
                        .await()
                }
            } catch (_: Exception) {}
        }
    }

    fun clearAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SESSIONS).apply()
    }
}
