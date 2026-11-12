package com.example.data.focus

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FocusSession(
    val id: String = "",
    val userId: String = "",
    val durationMinutes: Int = 25,
    val mode: String = "POMODORO", // "POMODORO", "DEEP_WORK", "SHORT_BREAK", "LONG_BREAK"
    val completedAt: Long = System.currentTimeMillis(),
    val tag: String = "Study"
)

data class FocusStats(
    val totalFocusedMinutes: Int = 0,
    val todayFocusedMinutes: Int = 0,
    val weeklyFocusedMinutes: Int = 0,
    val completedSessionsCount: Int = 0,
    val currentStreakDays: Int = 1,
    val dailyGoalMinutes: Int = 120,
    val weeklyGoalMinutes: Int = 600,
    val unlockedAchievements: List<String> = emptyList()
)

data class PersistedStopwatchState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedMillis: Long = 0L,
    val lastStartTimeMillis: Long = 0L,
    val laps: List<Long> = emptyList(),
    val sessionStartTimeMillis: Long = 0L
)

data class PersistedTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val mode: String = "POMODORO", // "POMODORO", "DEEP_WORK", "BREAK", "CUSTOM_TIMER"
    val initialTotalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val targetEndTimeMillis: Long = 0L,
    val selectedMinutes: Int = 25,
    val breakMinutes: Int = 5
)

/**
 * FocusRepository manages persistent focus sessions, daily/weekly stats,
 * streaks, achievements, and active session recovery across app restarts.
 */
class FocusRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("studyos_focus_prefs", Context.MODE_PRIVATE)
    private val firestore: FirebaseFirestore? by lazy {
        try {
            val app = com.google.firebase.FirebaseApp.getInstance()
            val dbId = context.getString(com.example.R.string.firestore_database_id)
            FirebaseFirestore.getInstance(app, dbId)
        } catch (_: Exception) {
            try {
                FirebaseFirestore.getInstance()
            } catch (_: Exception) {
                null
            }
        }
    }
    private val auth by lazy {
        try {
            com.google.firebase.FirebaseApp.getInstance()
            Firebase.auth
        } catch (_: Exception) {
            null
        }
    }

    private fun getCurrentUserId(): String = try {
        auth?.currentUser?.uid ?: "local_guest"
    } catch (_: Exception) {
        "local_guest"
    }

    // Local Persistence of Active Timer State (for instant refresh restore)
    fun saveActiveTimerState(state: PersistedTimerState) {
        val uid = getCurrentUserId()
        prefs.edit()
            .putBoolean("timer_running_$uid", state.isRunning)
            .putBoolean("timer_paused_$uid", state.isPaused)
            .putString("timer_mode_$uid", state.mode)
            .putInt("timer_initial_seconds_$uid", state.initialTotalSeconds)
            .putInt("timer_remaining_seconds_$uid", state.remainingSeconds)
            .putLong("timer_target_end_millis_$uid", state.targetEndTimeMillis)
            .putInt("timer_selected_minutes_$uid", state.selectedMinutes)
            .putInt("timer_break_minutes_$uid", state.breakMinutes)
            .apply()
    }

    fun getActiveTimerState(): PersistedTimerState {
        val uid = getCurrentUserId()
        val isRunning = prefs.getBoolean("timer_running_$uid", false)
        val isPaused = prefs.getBoolean("timer_paused_$uid", false)
        val mode = prefs.getString("timer_mode_$uid", "POMODORO") ?: "POMODORO"
        val initialSeconds = prefs.getInt("timer_initial_seconds_$uid", 25 * 60)
        val targetEndMillis = prefs.getLong("timer_target_end_millis_$uid", 0L)
        val selectedMinutes = prefs.getInt("timer_selected_minutes_$uid", 25)
        val breakMinutes = prefs.getInt("timer_break_minutes_$uid", 5)

        // Calculate accurate remaining seconds if was running
        val remainingSeconds = if (isRunning && !isPaused && targetEndMillis > System.currentTimeMillis()) {
            ((targetEndMillis - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
        } else {
            prefs.getInt("timer_remaining_seconds_$uid", 25 * 60)
        }

        return PersistedTimerState(
            isRunning = isRunning && remainingSeconds > 0,
            isPaused = isPaused,
            mode = mode,
            initialTotalSeconds = initialSeconds,
            remainingSeconds = remainingSeconds,
            targetEndTimeMillis = targetEndMillis,
            selectedMinutes = selectedMinutes,
            breakMinutes = breakMinutes
        )
    }

    fun clearActiveTimerState() {
        val uid = getCurrentUserId()
        prefs.edit()
            .remove("timer_running_$uid")
            .remove("timer_paused_$uid")
            .remove("timer_mode_$uid")
            .remove("timer_initial_seconds_$uid")
            .remove("timer_remaining_seconds_$uid")
            .remove("timer_target_end_millis_$uid")
            .apply()
    }

    // Stopwatch Persistence
    fun saveActiveStopwatchState(state: PersistedStopwatchState) {
        val uid = getCurrentUserId()
        val lapsString = state.laps.joinToString(",")
        prefs.edit()
            .putBoolean("stopwatch_running_$uid", state.isRunning)
            .putBoolean("stopwatch_paused_$uid", state.isPaused)
            .putLong("stopwatch_elapsed_$uid", state.elapsedMillis)
            .putLong("stopwatch_start_time_$uid", state.lastStartTimeMillis)
            .putLong("stopwatch_session_start_time_$uid", state.sessionStartTimeMillis)
            .putString("stopwatch_laps_$uid", lapsString)
            .apply()
    }

    fun getActiveStopwatchState(): PersistedStopwatchState {
        val uid = getCurrentUserId()
        val isRunning = prefs.getBoolean("stopwatch_running_$uid", false)
        val isPaused = prefs.getBoolean("stopwatch_paused_$uid", false)
        val baseElapsed = prefs.getLong("stopwatch_elapsed_$uid", 0L)
        val startTime = prefs.getLong("stopwatch_start_time_$uid", 0L)
        val sessionStartTime = prefs.getLong("stopwatch_session_start_time_$uid", 0L)
        val lapsRaw = prefs.getString("stopwatch_laps_$uid", "") ?: ""
        val laps = if (lapsRaw.isNotBlank()) {
            lapsRaw.split(",").mapNotNull { it.toLongOrNull() }
        } else emptyList()

        val currentElapsed = if (isRunning && !isPaused && startTime > 0L) {
            baseElapsed + (System.currentTimeMillis() - startTime)
        } else baseElapsed

        return PersistedStopwatchState(
            isRunning = isRunning,
            isPaused = isPaused,
            elapsedMillis = currentElapsed,
            lastStartTimeMillis = startTime,
            laps = laps,
            sessionStartTimeMillis = sessionStartTime
        )
    }

    fun clearActiveStopwatchState() {
        val uid = getCurrentUserId()
        prefs.edit()
            .remove("stopwatch_running_$uid")
            .remove("stopwatch_paused_$uid")
            .remove("stopwatch_elapsed_$uid")
            .remove("stopwatch_start_time_$uid")
            .remove("stopwatch_session_start_time_$uid")
            .remove("stopwatch_laps_$uid")
            .apply()
    }

    // Goals in SharedPreferences
    fun saveDailyGoal(minutes: Int) {
        val uid = getCurrentUserId()
        prefs.edit().putInt("daily_goal_$uid", minutes).apply()
    }

    fun getDailyGoal(): Int {
        val uid = getCurrentUserId()
        return prefs.getInt("daily_goal_$uid", 120)
    }

    fun saveWeeklyGoal(minutes: Int) {
        val uid = getCurrentUserId()
        prefs.edit().putInt("weekly_goal_$uid", minutes).apply()
    }

    fun getWeeklyGoal(): Int {
        val uid = getCurrentUserId()
        return prefs.getInt("weekly_goal_$uid", 600)
    }

    // Local Sessions Cache
    private fun getLocalSessions(uid: String): List<FocusSession> {
        val rawJson = prefs.getString("local_sessions_$uid", "[]") ?: "[]"
        val list = mutableListOf<FocusSession>()
        try {
            val arr = JSONArray(rawJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    FocusSession(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        userId = uid,
                        durationMinutes = obj.optInt("durationMinutes", 25),
                        mode = obj.optString("mode", "POMODORO"),
                        completedAt = obj.optLong("completedAt", System.currentTimeMillis()),
                        tag = obj.optString("tag", "Study")
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.completedAt }
    }

    private fun saveLocalSession(session: FocusSession) {
        val uid = session.userId
        val existing = getLocalSessions(uid).toMutableList()
        existing.add(0, session)
        val arr = JSONArray()
        existing.take(50).forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("userId", item.userId)
            obj.put("durationMinutes", item.durationMinutes)
            obj.put("mode", item.mode)
            obj.put("completedAt", item.completedAt)
            obj.put("tag", item.tag)
            arr.put(obj)
        }
        prefs.edit().putString("local_sessions_$uid", arr.toString()).apply()
    }

    // Record session persistently (both local storage & Firestore if authenticated)
    suspend fun recordFocusSession(durationMinutes: Int, mode: String, tag: String = "Study"): FocusSession {
        val uid = getCurrentUserId()
        val session = FocusSession(
            id = "focus_${System.currentTimeMillis()}",
            userId = uid,
            durationMinutes = durationMinutes,
            mode = mode,
            completedAt = System.currentTimeMillis(),
            tag = tag
        )

        // Always save locally first for guaranteed zero-latency persistence
        saveLocalSession(session)

        // Save to Firestore if user is authenticated
        val currentUser = try { auth?.currentUser } catch (_: Exception) { null }
        val db = firestore
        if (currentUser != null && db != null) {
            try {
                val sessionMap = hashMapOf(
                    "userId" to uid,
                    "durationMinutes" to durationMinutes,
                    "mode" to mode,
                    "tag" to tag,
                    "completedAt" to session.completedAt
                )
                db.collection("users").document(uid)
                    .collection("focus_sessions")
                    .document(session.id)
                    .set(sessionMap)
                    .await()
            } catch (e: Exception) {
                android.util.Log.w("FocusRepository", "Error syncing session to Firestore: ${e.message}")
            }
        }

        return session
    }

    fun observeSessions(): Flow<List<FocusSession>> = callbackFlow {
        val uid = getCurrentUserId()
        // Emit local cache immediately
        trySend(getLocalSessions(uid))

        val currentUser = try { auth?.currentUser } catch (_: Exception) { null }
        val db = firestore
        if (currentUser != null && db != null) {
            var listener: ListenerRegistration? = null
            try {
                listener = db.collection("users").document(uid)
                    .collection("focus_sessions")
                    .orderBy("completedAt", Query.Direction.DESCENDING)
                    .limit(50)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.w("FocusRepository", "Firestore observeSessions listener error: ${error.message}")
                            trySend(getLocalSessions(uid))
                            return@addSnapshotListener
                        }
                        if (snapshot != null && !snapshot.isEmpty) {
                            val remoteList = snapshot.documents.mapNotNull { doc ->
                                try {
                                    FocusSession(
                                        id = doc.id,
                                        userId = uid,
                                        durationMinutes = (doc.getLong("durationMinutes") ?: 25L).toInt(),
                                        mode = doc.getString("mode") ?: "POMODORO",
                                        completedAt = doc.getLong("completedAt") ?: System.currentTimeMillis(),
                                        tag = doc.getString("tag") ?: "Study"
                                    )
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            trySend(remoteList)
                        } else {
                            trySend(getLocalSessions(uid))
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("FocusRepository", "Failed to start snapshot listener: ${e.message}")
                trySend(getLocalSessions(uid))
            }

            awaitClose {
                try {
                    listener?.remove()
                } catch (_: Exception) {}
            }
        } else {
            awaitClose { }
        }
    }

    fun calculateStats(sessions: List<FocusSession>): FocusStats {
        val now = System.currentTimeMillis()
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dayFormat.format(Date(now))

        val oneWeekAgo = now - (7L * 24 * 60 * 60 * 1000)

        var totalMinutes = 0
        var todayMinutes = 0
        var weeklyMinutes = 0

        val distinctDays = mutableSetOf<String>()

        sessions.forEach { s ->
            totalMinutes += s.durationMinutes
            val sDay = dayFormat.format(Date(s.completedAt))
            distinctDays.add(sDay)
            if (sDay == todayStr) {
                todayMinutes += s.durationMinutes
            }
            if (s.completedAt >= oneWeekAgo) {
                weeklyMinutes += s.durationMinutes
            }
        }

        // Calculate consecutive daily streak
        var streak = 0
        var checkDay = now
        while (true) {
            val dStr = dayFormat.format(Date(checkDay))
            if (distinctDays.contains(dStr)) {
                streak++
                checkDay -= (24L * 60 * 60 * 1000)
            } else if (dStr == todayStr) {
                // If haven't completed today yet, check yesterday
                checkDay -= (24L * 60 * 60 * 1000)
            } else {
                break
            }
        }

        // Achievements logic
        val achievements = mutableListOf<String>()
        if (sessions.isNotEmpty()) achievements.add("First Focus Session")
        if (totalMinutes >= 60) achievements.add("1 Hour of Pure Focus")
        if (totalMinutes >= 300) achievements.add("Deep Work Novice (5h)")
        if (totalMinutes >= 1200) achievements.add("Master of Flow (20h)")
        if (streak >= 3) achievements.add("3-Day Streak")
        if (streak >= 7) achievements.add("Weekly Habit Champion (7-Day Streak)")
        if (todayMinutes >= getDailyGoal()) achievements.add("Daily Goal Crusher")

        return FocusStats(
            totalFocusedMinutes = totalMinutes,
            todayFocusedMinutes = todayMinutes,
            weeklyFocusedMinutes = weeklyMinutes,
            completedSessionsCount = sessions.size,
            currentStreakDays = streak.coerceAtLeast(if (sessions.isNotEmpty()) 1 else 0),
            dailyGoalMinutes = getDailyGoal(),
            weeklyGoalMinutes = getWeeklyGoal(),
            unlockedAchievements = achievements
        )
    }
}
