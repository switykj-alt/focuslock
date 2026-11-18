package com.example.focuslock

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * The screen shown during a session. Any blocked app is replaced by this screen.
 * It shows the time left and the allowed apps.
 */
class HubActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timeText: TextView
    private val tick = object : Runnable {
        override fun run() {
            update()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(0xFF101820.toInt())
        }
        root.addView(TextView(this).apply {
            text = "Focus mode"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        })
        timeText = TextView(this).apply {
            textSize = 48f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(32))
        }
        root.addView(timeText)
        root.addView(TextView(this).apply {
            text = "Allowed apps"
            setTextColor(0xFFB0BEC5.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(12))
        })

        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val pm = packageManager
        for (pkg in Session.apps(this)) {
            val (label, icon) = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)) to pm.getApplicationIcon(pkg)
            } catch (e: PackageManager.NameNotFoundException) { continue }

            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(4), dp(8), dp(4), dp(8))
                isClickable = true
                setOnClickListener { openApp(pkg, label.toString()) }
            }
            item.addView(ImageView(this).apply {
                setImageDrawable(icon)
                layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
            })
            item.addView(TextView(this).apply {
                text = label
                setTextColor(0xFFFFFFFF.toInt())
                gravity = Gravity.CENTER
                maxLines = 2
            })
            grid.addView(item)
        }
        root.addView(grid)

        root.addView(Button(this).apply {
            text = "Emergency call"
            setOnClickListener { startActivity(Intent(Intent.ACTION_DIAL)) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(48) }
        })

        setContentView(root)
    }

    private fun openApp(pkg: String, label: String) {
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch == null) {
            Toast.makeText(this, "Can't open $label", Toast.LENGTH_SHORT).show()
            return
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            startActivity(launch)
        } catch (e: Exception) {
            Toast.makeText(this, "Can't open $label", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!Session.isActive(this)) {
            finish()
            return
        }
        handler.post(tick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    private fun update() {
        val left = Session.endTime(this) - System.currentTimeMillis()
        if (left <= 0) {
            Session.clear(this)
            Toast.makeText(this, "Focus session finished", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        timeText.text = formatDuration(left)
    }

    // The back button does nothing while a session is running.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!Session.isActive(this)) {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
