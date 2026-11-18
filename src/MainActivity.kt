package com.example.focuslock

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Setup screen: permissions, pick up to 4 apps, set the timer, start. */
class MainActivity : Activity() {

    private val chosen = linkedSetOf<String>()
    private var hours = 1
    private var minutes = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Session.isActive(this)) {
            openHub()
            return
        }
        buildUi()
    }

    private fun openHub() {
        startActivity(Intent(this, HubActivity::class.java))
        finish()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
        }

        root.addView(TextView(this).apply {
            text = "FocusLock"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Pick up to ${Session.MAX_APPS} apps and a time. Until the timer ends, " +
                "every other app (including Settings) is blocked. Phone calls stay available."
            setPadding(0, dp(6), 0, dp(16))
        })

        // --- Permissions ---
        val usageOk = hasUsageAccess(this)
        val overlayOk = Settings.canDrawOverlays(this)
        if (!usageOk) root.addView(button("Step 1: Allow usage access") {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
        if (!overlayOk) root.addView(button("Step 2: Allow display over other apps") {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        })
        if (usageOk && overlayOk) root.addView(TextView(this).apply {
            text = "✓ Permissions granted"
            setPadding(0, 0, 0, dp(8))
        })

        // --- Timer ---
        root.addView(header("Timer"))
        val timerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        timerRow.addView(NumberPicker(this).apply {
            minValue = 0; maxValue = 12; value = hours
            setOnValueChangedListener { _, _, v -> hours = v }
        })
        timerRow.addView(TextView(this).apply { text = " h    " })
        timerRow.addView(NumberPicker(this).apply {
            minValue = 0; maxValue = 59; value = minutes
            setOnValueChangedListener { _, _, v -> minutes = v }
        })
        timerRow.addView(TextView(this).apply { text = " min" })
        root.addView(timerRow)

        // --- App list ---
        val countLabel = header("")
        fun updateCount() { countLabel.text = "Allowed apps (${chosen.size}/${Session.MAX_APPS})" }
        updateCount()
        root.addView(countLabel)

        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, 0)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        for (info in apps) {
            val pkg = info.activityInfo.packageName
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(4), 0, dp(4))
            }
            row.addView(ImageView(this).apply {
                setImageDrawable(info.loadIcon(pm))
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginEnd = dp(12) }
            })
            row.addView(CheckBox(this).apply {
                text = info.loadLabel(pm)
                isChecked = pkg in chosen
                setOnCheckedChangeListener { box, checked ->
                    if (checked) {
                        if (chosen.size >= Session.MAX_APPS && pkg !in chosen) {
                            box.isChecked = false
                            toast("You can choose only ${Session.MAX_APPS} apps")
                        } else chosen.add(pkg)
                    } else chosen.remove(pkg)
                    updateCount()
                }
            })
            root.addView(row)
        }

        root.addView(button("Start focus session") { confirmStart() }.apply {
            textSize = 18f
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun confirmStart() {
        if (!hasUsageAccess(this) || !Settings.canDrawOverlays(this)) {
            toast("Please give both permissions first"); return
        }
        if (chosen.isEmpty()) { toast("Choose at least one app"); return }
        val totalMin = hours * 60 + minutes
        if (totalMin == 0) { toast("Set a time"); return }

        val pm = packageManager
        val names = chosen.joinToString("\n") { pkg ->
            "• " + try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0))
            } catch (e: PackageManager.NameNotFoundException) { pkg }
        }
        AlertDialog.Builder(this)
            .setTitle("Start focus session?")
            .setMessage(
                "For the next ${hours} h ${minutes} min, only these apps will open:\n\n$names\n\n" +
                    "Everything else, including Settings, will be blocked. " +
                    "It cannot be stopped early."
            )
            .setPositiveButton("Start") { _, _ ->
                Session.start(this, chosen, totalMin * 60_000L)
                startForegroundService(Intent(this, BlockerService::class.java))
                openHub()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun header(t: String) = TextView(this).apply {
        text = t
        textSize = 18f
        setTypeface(null, Typeface.BOLD)
        setPadding(0, dp(16), 0, dp(8))
    }

    private fun button(t: String, onClick: () -> Unit) = Button(this).apply {
        text = t
        setOnClickListener { onClick() }
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()
}
