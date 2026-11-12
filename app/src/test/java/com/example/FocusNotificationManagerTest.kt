package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.FocusNotificationManager
import com.example.util.FocusReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FocusNotificationManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("studyos_focus_notifications_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testDefaultSettings() {
        val settings = FocusNotificationManager.getSettings(context)
        assertTrue(settings.isDailyReminderEnabled)
        assertEquals(9, settings.reminderHour)
        assertEquals(0, settings.reminderMinute)
        assertEquals("09:00 AM", settings.formattedPrimaryTime)
        assertFalse(settings.isSecondaryReminderEnabled)
        assertTrue(settings.isCompletionAlertEnabled)
        assertEquals(15, settings.minSessionMinutesForAlert)
    }

    @Test
    fun testSaveAndRetrieveSettings() {
        val updated = FocusReminderSettings(
            isDailyReminderEnabled = true,
            reminderHour = 14,
            reminderMinute = 30,
            isSecondaryReminderEnabled = true,
            secondaryHour = 20,
            secondaryMinute = 15,
            isCompletionAlertEnabled = true,
            minSessionMinutesForAlert = 25,
            soundAndVibrateEnabled = true
        )
        FocusNotificationManager.saveSettings(context, updated)

        val retrieved = FocusNotificationManager.getSettings(context)
        assertEquals(14, retrieved.reminderHour)
        assertEquals(30, retrieved.reminderMinute)
        assertEquals("02:30 PM", retrieved.formattedPrimaryTime)
        assertTrue(retrieved.isSecondaryReminderEnabled)
        assertEquals(20, retrieved.secondaryHour)
        assertEquals(15, retrieved.secondaryMinute)
        assertEquals("08:15 PM", retrieved.formattedSecondaryTime)
        assertEquals(25, retrieved.minSessionMinutesForAlert)
    }

    @Test
    fun testTimeFormatting() {
        assertEquals("12:00 AM", FocusReminderSettings.formatTime(0, 0))
        assertEquals("09:05 AM", FocusReminderSettings.formatTime(9, 5))
        assertEquals("12:30 PM", FocusReminderSettings.formatTime(12, 30))
        assertEquals("07:45 PM", FocusReminderSettings.formatTime(19, 45))
        assertEquals("11:59 PM", FocusReminderSettings.formatTime(23, 59))
    }

    @Test
    fun testNotificationChannelsCreated() {
        FocusNotificationManager.createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val reminderChannel = notificationManager.getNotificationChannel(FocusNotificationManager.CHANNEL_DAILY_REMINDERS)
        assertNotNull(reminderChannel)
        assertEquals("Daily Study Reminders", reminderChannel.name)

        val completionChannel = notificationManager.getNotificationChannel(FocusNotificationManager.CHANNEL_COMPLETION_ALERTS)
        assertNotNull(completionChannel)
        assertEquals("Focus Session Alerts", completionChannel.name)
    }

    @Test
    fun testDeliverStudyReminderNotification() {
        FocusNotificationManager.deliverStudyReminderNotification(context, isSecondary = false)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifications = notificationManager.activeNotifications
        assertTrue(notifications.any { it.id == FocusNotificationManager.NOTIFICATION_ID_PRIMARY_REMINDER })
    }

    @Test
    fun testNotifySessionCompleted() {
        FocusNotificationManager.notifySessionCompleted(context, durationMinutes = 25, modeTitle = "Pomodoro")
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifications = notificationManager.activeNotifications
        assertTrue(notifications.any { it.id == FocusNotificationManager.NOTIFICATION_ID_COMPLETION_ALERT })
    }
}
