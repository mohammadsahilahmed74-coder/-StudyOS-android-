package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.focus.FocusRepository
import java.util.Locale

class StudyOSStopwatchWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        val repository = FocusRepository(context)
        val state = repository.getActiveStopwatchState()
        val now = System.currentTimeMillis()

        when (action) {
            ACTION_STOPWATCH_START -> {
                val currentElapsed = state.elapsedMillis
                val newState = state.copy(
                    isRunning = true,
                    isPaused = false,
                    lastStartTimeMillis = now,
                    elapsedMillis = currentElapsed,
                    sessionStartTimeMillis = if (state.sessionStartTimeMillis > 0L) state.sessionStartTimeMillis else now
                )
                repository.saveActiveStopwatchState(newState)
                updateAllWidgets(context)
            }
            ACTION_STOPWATCH_PAUSE -> {
                if (state.isRunning && !state.isPaused) {
                    val additional = if (state.lastStartTimeMillis > 0L) (now - state.lastStartTimeMillis) else 0L
                    val totalElapsed = state.elapsedMillis + additional
                    val newState = state.copy(
                        isRunning = true,
                        isPaused = true,
                        lastStartTimeMillis = 0L,
                        elapsedMillis = totalElapsed
                    )
                    repository.saveActiveStopwatchState(newState)
                    updateAllWidgets(context)
                }
            }
            ACTION_STOPWATCH_RESUME -> {
                if (state.isRunning && state.isPaused) {
                    val newState = state.copy(
                        isRunning = true,
                        isPaused = false,
                        lastStartTimeMillis = now
                    )
                    repository.saveActiveStopwatchState(newState)
                    updateAllWidgets(context)
                }
            }
            ACTION_STOPWATCH_RESET -> {
                repository.clearActiveStopwatchState()
                updateAllWidgets(context)
            }
            ACTION_STOPWATCH_OPEN_APP -> {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("START_DESTINATION", "FOCUS_STOPWATCH")
                }
                context.startActivity(launchIntent)
            }
        }
    }

    companion object {
        const val ACTION_STOPWATCH_START = "com.example.action.STOPWATCH_START"
        const val ACTION_STOPWATCH_PAUSE = "com.example.action.STOPWATCH_PAUSE"
        const val ACTION_STOPWATCH_RESUME = "com.example.action.STOPWATCH_RESUME"
        const val ACTION_STOPWATCH_RESET = "com.example.action.STOPWATCH_RESET"
        const val ACTION_STOPWATCH_OPEN_APP = "com.example.action.STOPWATCH_OPEN_APP"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, StudyOSStopwatchWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateWidget(context, appWidgetManager, widgetId)
            }
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_studyos_stopwatch)
            val repository = FocusRepository(context)
            val state = repository.getActiveStopwatchState()

            val isRunning = state.isRunning
            val isPaused = state.isPaused
            val elapsed = state.elapsedMillis

            // Open app click on root and time container
            val openAppIntent = Intent(context, StudyOSStopwatchWidgetProvider::class.java).apply {
                action = ACTION_STOPWATCH_OPEN_APP
            }
            val openAppPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_time_container, openAppPendingIntent)

            // Button PendingIntents
            val startIntent = Intent(context, StudyOSStopwatchWidgetProvider::class.java).apply {
                action = ACTION_STOPWATCH_START
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_start,
                PendingIntent.getBroadcast(context, 1, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val pauseIntent = Intent(context, StudyOSStopwatchWidgetProvider::class.java).apply {
                action = ACTION_STOPWATCH_PAUSE
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_pause,
                PendingIntent.getBroadcast(context, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val resumeIntent = Intent(context, StudyOSStopwatchWidgetProvider::class.java).apply {
                action = ACTION_STOPWATCH_RESUME
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_resume,
                PendingIntent.getBroadcast(context, 3, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val resetIntent = Intent(context, StudyOSStopwatchWidgetProvider::class.java).apply {
                action = ACTION_STOPWATCH_RESET
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_reset,
                PendingIntent.getBroadcast(context, 4, resetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            if (isRunning && !isPaused) {
                // RUNNING state
                views.setTextViewText(R.id.widget_status_text, "RUNNING ⚡")
                views.setTextColor(R.id.widget_status_text, 0xFF10B981.toInt())

                views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
                views.setViewVisibility(R.id.widget_static_time, View.GONE)
                views.setChronometer(
                    R.id.widget_chronometer,
                    SystemClock.elapsedRealtime() - elapsed,
                    null,
                    true
                )

                views.setViewVisibility(R.id.widget_btn_start, View.GONE)
                views.setViewVisibility(R.id.widget_btn_pause, View.VISIBLE)
                views.setViewVisibility(R.id.widget_btn_resume, View.GONE)
                views.setViewVisibility(R.id.widget_btn_reset, View.GONE)

            } else if (isRunning && isPaused) {
                // PAUSED state
                views.setTextViewText(R.id.widget_status_text, "PAUSED ⏸")
                views.setTextColor(R.id.widget_status_text, 0xFFF59E0B.toInt())

                views.setViewVisibility(R.id.widget_chronometer, View.GONE)
                views.setChronometer(R.id.widget_chronometer, SystemClock.elapsedRealtime(), null, false)

                views.setViewVisibility(R.id.widget_static_time, View.VISIBLE)
                views.setTextViewText(R.id.widget_static_time, formatElapsed(elapsed))

                views.setViewVisibility(R.id.widget_btn_start, View.GONE)
                views.setViewVisibility(R.id.widget_btn_pause, View.GONE)
                views.setViewVisibility(R.id.widget_btn_resume, View.VISIBLE)
                views.setViewVisibility(R.id.widget_btn_reset, View.VISIBLE)

            } else {
                // READY / STOPPED state
                views.setTextViewText(R.id.widget_status_text, "READY")
                views.setTextColor(R.id.widget_status_text, 0xFF94A3B8.toInt())

                views.setViewVisibility(R.id.widget_chronometer, View.GONE)
                views.setChronometer(R.id.widget_chronometer, SystemClock.elapsedRealtime(), null, false)

                views.setViewVisibility(R.id.widget_static_time, View.VISIBLE)
                views.setTextViewText(R.id.widget_static_time, "00:00:00")

                views.setViewVisibility(R.id.widget_btn_start, View.VISIBLE)
                views.setViewVisibility(R.id.widget_btn_pause, View.GONE)
                views.setViewVisibility(R.id.widget_btn_resume, View.GONE)
                views.setViewVisibility(R.id.widget_btn_reset, View.GONE)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun formatElapsed(elapsedMillis: Long): String {
            val totalSec = elapsedMillis / 1000
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        }
    }
}
