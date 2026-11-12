package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ui.screens.FocusBlockedActivity

class FocusBlockerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var isMonitoring = false
    private var lastBlockedLaunchTime = 0L

    private val checkRunnable = object : Runnable {
        override fun run() {
            if (!isMonitoring) return
            checkForegroundAppAndEnforce()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                AppBlockerManager.setBlockingActive(this, false)
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                val broadcastIntent = Intent("com.example.STUDYOS_FOCUS_ACTION").apply {
                    putExtra("ACTION_TYPE", "PAUSE")
                }
                sendBroadcast(broadcastIntent)
            }
            ACTION_RESUME -> {
                val broadcastIntent = Intent("com.example.STUDYOS_FOCUS_ACTION").apply {
                    putExtra("ACTION_TYPE", "RESUME")
                }
                sendBroadcast(broadcastIntent)
            }
        }

        try {
            val notification = buildForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (e: Exception) {
                    android.util.Log.w("FocusBlockerService", "startForeground with specialUse type failed: ${e.message}")
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("FocusBlockerService", "Failed to startForeground: ${e.message}", e)
        }

        isMonitoring = true
        handler.removeCallbacks(checkRunnable)
        handler.post(checkRunnable)

        return START_STICKY
    }

    override fun onDestroy() {
        isMonitoring = false
        handler.removeCallbacks(checkRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkForegroundAppAndEnforce() {
        if (!AppBlockerManager.isBlockingActive(this)) {
            stopSelf()
            return
        }

        val blockedPackages = AppBlockerManager.getBlockedPackages(this)
        if (blockedPackages.isEmpty()) return

        val foregroundPackage = getForegroundPackageName() ?: return
        if (foregroundPackage == packageName) return

        if (blockedPackages.contains(foregroundPackage)) {
            val now = System.currentTimeMillis()
            if (now - lastBlockedLaunchTime > 1500L) {
                lastBlockedLaunchTime = now
                val blockIntent = Intent(this, FocusBlockedActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("BLOCKED_PACKAGE", foregroundPackage)
                }
                startActivity(blockIntent)
            }
        }
    }

    private fun getForegroundPackageName(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 4000L

        val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
        val event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                return event.packageName
            }
        }
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "StudyOS Focus Protection & Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Enforces distraction-free app blocking and provides session controls"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FocusBlockerService::class.java).setAction(ACTION_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, FocusBlockerService::class.java).setAction(ACTION_RESUME),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, FocusBlockerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Session Active & Protected")
            .setContentText("Distracting apps are blocked. Stay focused!")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
            .addAction(android.R.drawable.ic_media_play, "Resume", resumeIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE_FOCUS"
        const val ACTION_RESUME = "com.example.ACTION_RESUME_FOCUS"
        const val ACTION_STOP = "com.example.ACTION_STOP_FOCUS"

        private const val CHANNEL_ID = "studyos_focus_blocker_channel"
        private const val NOTIFICATION_ID = 2002

        fun startService(context: Context) {
            try {
                val intent = Intent(context, FocusBlockerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("FocusBlockerService", "Unable to start FocusBlockerService: ${e.message}", e)
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, FocusBlockerService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                android.util.Log.w("FocusBlockerService", "Unable to stop FocusBlockerService: ${e.message}")
            }
        }
    }
}
