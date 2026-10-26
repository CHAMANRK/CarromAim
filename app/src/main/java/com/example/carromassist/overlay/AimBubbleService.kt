package com.example.carromassist.overlay

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/** One-line live status pill plus a separate draggable aim toggle. */
class AimBubbleService : Service() {
    private lateinit var wm: WindowManager
    private var statusPill: TextView? = null
    private var bubble: TextView? = null
    private var aimOn = false
    private var startX = 0
    private var startY = 0
    private var touchX = 0f
    private var touchY = 0f
    private var statusText = "JARVIS · Overlay ready"

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            statusText = intent?.getStringExtra(EXTRA_STATUS) ?: return
            statusPill?.text = statusText
        }
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE

        val pill = TextView(this).apply {
            text = statusText
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER_VERTICAL
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(14, 8, 14, 8)
            background = pillBackground(Color.rgb(24, 32, 38), Color.rgb(121, 224, 195))
        }
        val pillParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            width = (resources.displayMetrics.widthPixels * 0.92f).toInt()
            x = 0; y = (resources.displayMetrics.density * 42).toInt()
        }
        wm.addView(pill, pillParams)
        statusPill = pill

        val view = TextView(this).apply {
            text = "🎯 AIM OFF"
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(14, 10, 14, 10)
            background = pillBackground(Color.rgb(30, 75, 65), Color.rgb(121, 224, 195))
            contentDescription = "Toggle Jarvis aim state"
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 18; y = (resources.displayMetrics.density * 112).toInt() }
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { startX = params.x; startY = params.y; touchX = event.rawX; touchY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> { params.x = startX + (event.rawX - touchX).toInt(); params.y = startY + (event.rawY - touchY).toInt(); wm.updateViewLayout(view, params); true }
                MotionEvent.ACTION_UP -> {
                    val moved = kotlin.math.abs(event.rawX - touchX) > 10 || kotlin.math.abs(event.rawY - touchY) > 10
                    if (!moved) {
                        aimOn = !aimOn
                        view.text = if (aimOn) "🎯 AIM ON" else "🎯 AIM OFF"
                        view.background = pillBackground(if (aimOn) Color.rgb(130, 75, 20) else Color.rgb(30, 75, 65), if (aimOn) Color.rgb(255, 194, 77) else Color.rgb(121, 224, 195))
                        if (aimOn) setStatus("AIM ON · Detection not connected") else setStatus("AIM OFF · Overlay ready")
                    }
                    true
                }
                else -> true
            }
        }
        wm.addView(view, params)
        bubble = view
        val filter = IntentFilter(ACTION_STATUS)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(statusReceiver, filter)
        }
    }

    private fun setStatus(message: String) {
        statusText = message
        statusPill?.text = message
    }

    private fun pillBackground(fill: Int, stroke: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 28f
        setColor(fill)
        setStroke(2, stroke)
    }

    override fun onDestroy() {
        try { unregisterReceiver(statusReceiver) } catch (_: Exception) { }
        statusPill?.let { runCatching { wm.removeView(it) } }; statusPill = null
        bubble?.let { runCatching { wm.removeView(it) } }; bubble = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STATUS = "com.example.carromassist.OVERLAY_STATUS"
        const val EXTRA_STATUS = "status_text"
    }
}
