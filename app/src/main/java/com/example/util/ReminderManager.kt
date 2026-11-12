package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

/**
 * BroadcastReceiver responsible for delivering scheduled task reminders.
 */
class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Study Session Reminder"
        val taskDesc = intent.getStringExtra(EXTRA_TASK_DESC) ?: "You have an upcoming study task scheduled."

        ReminderManager.showNotification(
            context = context,
            notificationId = taskId.toInt(),
            title = taskTitle,
            message = taskDesc
        )
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESC = "extra_task_desc"
    }
}

/**
 * Helper object to configure notification channels, post immediate notifications,
 * and schedule future study task reminders.
 */
object ReminderManager {
    const val CHANNEL_ID = "studyos_reminders_channel"
    private const val CHANNEL_NAME = "StudyOS Task Reminders"
    private const val CHANNEL_DESC = "Alerts for upcoming study sessions, deadlines, and task reminders."

    /**
     * Initializes notification channels on Android O+.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules a future reminder alarm for a study task.
     */
    fun scheduleTaskReminder(
        context: Context,
        taskId: Long,
        taskTitle: String,
        taskDescription: String,
        triggerAtMillis: Long
    ) {
        createNotificationChannel(context)

        // Only schedule if in future
        if (triggerAtMillis <= System.currentTimeMillis()) {
            // Trigger right away
            showNotification(context, taskId.toInt(), "Upcoming: $taskTitle", taskDescription.ifBlank { "Study task deadline reminder" })
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            putExtra(StudyReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(StudyReminderReceiver.EXTRA_TASK_TITLE, "Upcoming Study Task: $taskTitle")
            putExtra(StudyReminderReceiver.EXTRA_TASK_DESC, taskDescription.ifBlank { "Time to work on your study task!" })
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /**
     * Displays a reminder notification immediately.
     */
    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ) {
        createNotificationChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notificationId, notification)
    }

    /**
     * Cancels an existing reminder for a task.
     */
    fun cancelTaskReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, StudyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
