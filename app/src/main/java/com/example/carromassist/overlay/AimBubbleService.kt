package com.example.carromassist.overlay

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView

/** Floating control only. It toggles the aim-assist UI state; live game detection is not wired yet. */
class AimBubbleService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var aimOn = false
    private var startX = 0
    private var startY = 0
    private var touchX = 0f
    private var touchY = 0f

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = TextView(this).apply {
            text = "🎯\nAIM OFF"
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(14, 12, 14, 12)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 48f
                setColor(Color.rgb(30, 75, 65))
                setStroke(3, Color.rgb(121, 224, 195))
            }
        }
        val type = if (android.os.Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 280 }
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { startX = params.x; startY = params.y; touchX = event.rawX; touchY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> { params.x = startX + (event.rawX - touchX).toInt(); params.y = startY + (event.rawY - touchY).toInt(); wm.updateViewLayout(view, params); true }
                MotionEvent.ACTION_UP -> {
                    val moved = kotlin.math.abs(event.rawX - touchX) > 10 || kotlin.math.abs(event.rawY - touchY) > 10
                    if (!moved) { aimOn = !aimOn; view.text = if (aimOn) "🎯\nAIM ON" else "🎯\nAIM OFF"; view.background = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = 48f; setColor(if (aimOn) Color.rgb(130, 75, 20) else Color.rgb(30, 75, 65)); setStroke(3, if (aimOn) Color.rgb(255, 194, 77) else Color.rgb(121, 224, 195)) } }
                    true
                }
                else -> true
            }
        }
        wm.addView(view, params)
        bubble = view
    }

    override fun onDestroy() { bubble?.let { runCatching { wm.removeView(it) } }; bubble = null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
