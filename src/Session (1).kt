package com.example.focuslock

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.telecom.TelecomManager

/** Stores the running focus session: which apps are allowed and when it ends. */
object Session {
    const val MAX_APPS = 4

    private fun prefs(c: Context) = c.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun endTime(c: Context): Long = prefs(c).getLong("end", 0L)

    fun isActive(c: Context): Boolean = endTime(c) > System.currentTimeMillis()

    fun apps(c: Context): List<String> =
        (prefs(c).getString("apps", "") ?: "").split(",").filter { it.isNotBlank() }

    fun start(c: Context, apps: Collection<String>, durationMs: Long) {
        prefs(c).edit()
            .putLong("end", System.currentTimeMillis() + durationMs)
            .putString("apps", apps.joinToString(","))
            .commit()
    }

    // ---- Remembered choice (kept after a session ends) ----
    private fun saved(c: Context) = c.getSharedPreferences("choice", Context.MODE_PRIVATE)

    fun saveChoice(c: Context, apps: Collection<String>, hours: Int, minutes: Int) {
        saved(c).edit()
            .putString("apps", apps.joinToString(","))
            .putInt("hours", hours)
            .putInt("minutes", minutes)
            .apply()
    }

    fun savedApps(c: Context): List<String> =
        (saved(c).getString("apps", "") ?: "").split(",").filter { it.isNotBlank() }

    fun savedHours(c: Context): Int = saved(c).getInt("hours", 1)
    fun savedMinutes(c: Context): Int = saved(c).getInt("minutes", 0)

    fun clear(c: Context) {
        prefs(c).edit().clear().commit()
    }

    /**
     * Everything that may be on screen during a session: the 4 chosen apps,
     * this app, the system UI, and the phone app (so emergency calls always work).
     */
    fun allowedPackages(c: Context): Set<String> {
        val set = mutableSetOf(
            c.packageName,
            "android",                                  // system dialogs
            "com.android.systemui",                     // status bar, notifications
            "com.android.phone",                        // emergency dialer
            "com.android.emergency",
            "com.android.server.telecom",
            "com.android.permissioncontroller",         // permission pop-ups inside allowed apps
            "com.google.android.permissioncontroller",
        )
        c.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let { set.add(it) }
        // The home screen stays usable so the allowed apps can be opened from it.
        // (Settings, Play Store and the uninstall pop-up are still blocked.)
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        c.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName?.let { if (it != "android") set.add(it) }
        set.addAll(apps(c))
        return set
    }
}

fun hasUsageAccess(c: Context): Boolean {
    val ops = c.getSystemService(AppOpsManager::class.java)
    val mode = if (Build.VERSION.SDK_INT >= 29) {
        ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
    } else {
        @Suppress("DEPRECATION")
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}
