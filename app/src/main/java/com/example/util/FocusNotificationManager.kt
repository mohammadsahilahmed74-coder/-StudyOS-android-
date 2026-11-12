package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

data class FocusReminderSettings(
    val isDailyReminderEnabled: Boolean = true,
    val reminderHour: Int = 9,      // 9:00 AM default
    val reminderMinute: Int = 0,
    val isSecondaryReminderEnabled: Boolean = false,
    val secondaryHour: Int = 19,    // 7:00 PM default
    val secondaryMinute: Int = 0,
    val isCompletionAlertEnabled: Boolean = true,
    val minSessionMinutesForAlert: Int = 15,
    val soundAndVibrateEnabled: Boolean = true
) {
    val formattedPrimaryTime: String
        get() = formatTime(reminderHour, reminderMinute)

    val formattedSecondaryTime: String
        get() = formatTime(secondaryHour, secondaryMinute)

    companion object {
        fun formatTime(hour: Int, minute: Int): String {
            val period = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return "%02d:%02d %s".format(displayHour, minute, period)
        }
    }
}

/**
 * BroadcastReceiver responsible for delivering scheduled daily study reminders
 * and restoring scheduled alarms upon device boot.
 */
class FocusNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            FocusNotificationManager.ACTION_TRIGGER_STUDY_REMINDER -> {
                val isSecondary = intent.getBooleanExtra(FocusNotificationManager.EXTRA_IS_SECONDARY, false)
                FocusNotificationManager.deliverStudyReminderNotification(context, isSecondary)
                // Reschedule for next day to ensure recurring daily alarm
                val settings = FocusNotificationManager.getSettings(context)
                if (isSecondary && settings.isSecondaryReminderEnabled) {
                    FocusNotificationManager.scheduleDailyAlarm(
                        context = context,
                        hour = settings.secondaryHour,
                        minute = settings.secondaryMinute,
                        requestCode = FocusNotificationManager.REQUEST_CODE_SECONDARY_REMINDER,
                        isSecondary = true
                    )
                } else if (!isSecondary && settings.isDailyReminderEnabled) {
                    FocusNotificationManager.scheduleDailyAlarm(
                        context = context,
                        hour = settings.reminderHour,
                        minute = settings.reminderMinute,
                        requestCode = FocusNotificationManager.REQUEST_CODE_PRIMARY_REMINDER,
                        isSecondary = false
                    )
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                FocusNotificationManager.rescheduleAllReminders(context)
            }
        }
    }
}

/**
 * Comprehensive Focus Notification Manager:
 * 1. Schedules daily study reminders at users' preferred times using AlarmManager.
 * 2. Alerts users with high-priority heads-up notifications when focus sessions complete.
 * 3. Supports customizable reminder times, secondary reminders, and permission checks.
 */
object FocusNotificationManager {
    const val ACTION_TRIGGER_STUDY_REMINDER = "com.example.action.TRIGGER_FOCUS_REMINDER"
    const val EXTRA_IS_SECONDARY = "extra_is_secondary"
    const val EXTRA_OPEN_FOCUS = "EXTRA_OPEN_FOCUS"

    const val CHANNEL_DAILY_REMINDERS = "focus_daily_study_reminders"
    const val CHANNEL_COMPLETION_ALERTS = "focus_session_completed_alerts"

    const val REQUEST_CODE_PRIMARY_REMINDER = 2001
    const val REQUEST_CODE_SECONDARY_REMINDER = 2002
    const val NOTIFICATION_ID_PRIMARY_REMINDER = 3001
    const val NOTIFICATION_ID_SECONDARY_REMINDER = 3002
    const val NOTIFICATION_ID_COMPLETION_ALERT = 3003
    const val NOTIFICATION_ID_TEST_REMINDER = 3004

    private const val PREFS_NAME = "studyos_focus_notifications_prefs"
    private const val KEY_DAILY_ENABLED = "daily_enabled"
    private const val KEY_HOUR = "reminder_hour"
    private const val KEY_MINUTE = "reminder_minute"
    private const val KEY_SECONDARY_ENABLED = "secondary_enabled"
    private const val KEY_SECONDARY_HOUR = "secondary_hour"
    private const val KEY_SECONDARY_MINUTE = "secondary_minute"
    private const val KEY_COMPLETION_ENABLED = "completion_alert_enabled"
    private const val KEY_MIN_SESSION_MINUTES = "min_session_minutes"
    private const val KEY_SOUND_VIBRATE = "sound_vibrate_enabled"

    fun getSettings(context: Context): FocusReminderSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return FocusReminderSettings(
            isDailyReminderEnabled = prefs.getBoolean(KEY_DAILY_ENABLED, true),
            reminderHour = prefs.getInt(KEY_HOUR, 9),
            reminderMinute = prefs.getInt(KEY_MINUTE, 0),
            isSecondaryReminderEnabled = prefs.getBoolean(KEY_SECONDARY_ENABLED, false),
            secondaryHour = prefs.getInt(KEY_SECONDARY_HOUR, 19),
            secondaryMinute = prefs.getInt(KEY_SECONDARY_MINUTE, 0),
            isCompletionAlertEnabled = prefs.getBoolean(KEY_COMPLETION_ENABLED, true),
            minSessionMinutesForAlert = prefs.getInt(KEY_MIN_SESSION_MINUTES, 15),
            soundAndVibrateEnabled = prefs.getBoolean(KEY_SOUND_VIBRATE, true)
        )
    }

    fun saveSettings(context: Context, settings: FocusReminderSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_DAILY_ENABLED, settings.isDailyReminderEnabled)
            .putInt(KEY_HOUR, settings.reminderHour)
            .putInt(KEY_MINUTE, settings.reminderMinute)
            .putBoolean(KEY_SECONDARY_ENABLED, settings.isSecondaryReminderEnabled)
            .putInt(KEY_SECONDARY_HOUR, settings.secondaryHour)
            .putInt(KEY_SECONDARY_MINUTE, settings.secondaryMinute)
            .putBoolean(KEY_COMPLETION_ENABLED, settings.isCompletionAlertEnabled)
            .putInt(KEY_MIN_SESSION_MINUTES, settings.minSessionMinutesForAlert)
            .putBoolean(KEY_SOUND_VIBRATE, settings.soundAndVibrateEnabled)
            .apply()

        rescheduleAllReminders(context)
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // Channel 1: Daily Study Reminders
            val reminderChannel = NotificationChannel(
                CHANNEL_DAILY_REMINDERS,
                "Daily Study Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to begin your scheduled daily focus sessions and keep your study streak active."
                enableVibration(true)
                setShowBadge(true)
            }

            // Channel 2: Session Completion Alerts
            val completionChannel = NotificationChannel(
                CHANNEL_COMPLETION_ALERTS,
                "Focus Session Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Celebratory notifications when a focus session is completed."
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(completionChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun rescheduleAllReminders(context: Context) {
        val settings = getSettings(context)

        // Cancel previous alarms
        cancelAlarm(context, REQUEST_CODE_PRIMARY_REMINDER)
        cancelAlarm(context, REQUEST_CODE_SECONDARY_REMINDER)

        if (settings.isDailyReminderEnabled) {
            scheduleDailyAlarm(
                context = context,
                hour = settings.reminderHour,
                minute = settings.reminderMinute,
                requestCode = REQUEST_CODE_PRIMARY_REMINDER,
                isSecondary = false
            )
        }

        if (settings.isSecondaryReminderEnabled) {
            scheduleDailyAlarm(
                context = context,
                hour = settings.secondaryHour,
                minute = settings.secondaryMinute,
                requestCode = REQUEST_CODE_SECONDARY_REMINDER,
                isSecondary = true
            )
        }
    }

    fun scheduleDailyAlarm(
        context: Context,
        hour: Int,
        minute: Int,
        requestCode: Int,
        isSecondary: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, FocusNotificationReceiver::class.java).apply {
            action = ACTION_TRIGGER_STUDY_REMINDER
            putExtra(EXTRA_IS_SECONDARY, isSecondary)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, FocusNotificationReceiver::class.java).apply {
            action = ACTION_TRIGGER_STUDY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun deliverStudyReminderNotification(context: Context, isSecondary: Boolean) {
        createNotificationChannels(context)
        val settings = getSettings(context)
        val formattedTime = if (isSecondary) settings.formattedSecondaryTime else settings.formattedPrimaryTime

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_FOCUS, true)
            putExtra("TARGET_TAB", 9)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            if (isSecondary) NOTIFICATION_ID_SECONDARY_REMINDER else NOTIFICATION_ID_PRIMARY_REMINDER,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isSecondary) "🌙 Evening Study Time!" else "🎯 Time for Your Daily Focus Session!"
        val message = "Your scheduled study time ($formattedTime) has arrived. Step into flow state and make progress on your subjects."

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setSummaryText("StudyOS Daily Reminder")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.mipmap.ic_launcher,
                "Start Session",
                pendingIntent
            )
            .apply {
                if (settings.soundAndVibrateEnabled) {
                    setVibrate(longArrayOf(0, 300, 200, 300))
                }
            }
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(
            if (isSecondary) NOTIFICATION_ID_SECONDARY_REMINDER else NOTIFICATION_ID_PRIMARY_REMINDER,
            notification
        )
    }

    /**
     * Alerts the user when a focus session is completed.
     */
    fun notifySessionCompleted(
        context: Context,
        durationMinutes: Int,
        modeTitle: String
    ) {
        val settings = getSettings(context)
        if (!settings.isCompletionAlertEnabled) return

        createNotificationChannels(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_FOCUS, true)
            putExtra("TARGET_TAB", 9)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_COMPLETION_ALERT,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🎉 Focus Session Finished! (+$durationMinutes min)"
        val message = "Awesome work! You completed your $durationMinutes-minute $modeTitle session. Take a restorative break before your next sprint."

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETION_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setSummaryText("Focus Completed")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.mipmap.ic_launcher,
                "View History",
                pendingIntent
            )
            .apply {
                if (settings.soundAndVibrateEnabled) {
                    setVibrate(longArrayOf(0, 400, 200, 400))
                }
            }
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID_COMPLETION_ALERT, notification)
    }

    /**
     * Sends an immediate test daily study reminder for the user to confirm notifications work.
     */
    fun sendTestDailyReminder(context: Context) {
        createNotificationChannels(context)
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_FOCUS, true)
            putExtra("TARGET_TAB", 9)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_TEST_REMINDER,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🎯 Test Study Reminder Active!")
            .setContentText("Notifications are working! You will be reminded daily at your chosen study time.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Notifications are working perfectly! You will receive daily reminders at your configured study times to help you build a consistent habit.")
                    .setSummaryText("StudyOS Notifications")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID_TEST_REMINDER, notification)
    }

    /**
     * Sends an immediate test completion notification.
     */
    fun sendTestCompletionAlert(context: Context) {
        createNotificationChannels(context)
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_FOCUS, true)
            putExtra("TARGET_TAB", 9)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_COMPLETION_ALERT,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETION_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🎉 Test: Focus Session Completed!")
            .setContentText("Great job completing your test 25-minute Deep Work session! Ready for a break?")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Great job completing your test 25-minute Deep Work session! Take a 5-minute break and return recharged.")
                    .setSummaryText("Focus Completed")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID_COMPLETION_ALERT, notification)
    }
}
