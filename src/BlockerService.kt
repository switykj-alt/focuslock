package com.example.focuslock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper

/**
 * Runs in the background during a session. About twice a second it checks which
 * app is on screen. If that app is not allowed, it brings the focus screen back.
 */
class BlockerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var allowed: Set<String> = emptySet()
    private var lastForeground: String? = null
    private var lastRedirect = 0L

    private val loop = object : Runnable {
        override fun run() {
            check()
            if (running) handler.postDelayed(this, 400)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must call startForeground() quickly after startForegroundService().
        startForeground(1, buildNotification())
        if (!Session.isActive(this)) {
            stopNow()
            return START_NOT_STICKY
        }
        allowed = Session.allowedPackages(this)
        if (!running) {
            running = true
            handler.post(loop)
        }
        return START_STICKY // restart automatically if the system kills it
    }

    private fun check() {
        if (!Session.isActive(this)) {
            Session.clear(this)
            stopNow()
            return
        }
        val fg = foregroundPackage() ?: return
        val now = System.currentTimeMillis()
        if (fg !in allowed && now - lastRedirect > 700) {
            lastRedirect = now
            startActivity(
                Intent(this, HubActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            )
        }
    }

    /** The package of the app that most recently came to the screen. */
    private fun foregroundPackage(): String? {
        val usm = getSystemService(UsageStatsManager::class.java) ?: return lastForeground
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 30_000, now)
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            @Suppress("DEPRECATION")
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) { // = ACTIVITY_RESUMED
                lastForeground = e.packageName
            }
        }
        return lastForeground
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("focus", "Focus session", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, HubActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, "focus")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Focus session running")
            .setContentText("Other apps are blocked")
            .setWhen(Session.endTime(this))
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setContentIntent(open)
            .build()
    }

    private fun stopNow() {
        running = false
        handler.removeCallbacks(loop)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(loop)
        super.onDestroy()
    }
}
