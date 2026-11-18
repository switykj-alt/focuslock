package com.example.focuslock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts the blocker after the phone reboots, if the session has not ended. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Session.isActive(context)) {
            context.startForegroundService(Intent(context, BlockerService::class.java))
        }
    }
}
