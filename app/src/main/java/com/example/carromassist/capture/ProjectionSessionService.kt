package com.example.carromassist.capture

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicBoolean

/** User-consented, local frame capture. No frame data leaves the device. */
class ProjectionSessionService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: android.hardware.display.VirtualDisplay? = null
    private var worker: HandlerThread? = null
    private var handler: Handler? = null
    private val paused = AtomicBoolean(false)
    private val stopped = AtomicBoolean(false)
    private var lastFrameAt = 0L

    private val callback = object : MediaProjection.Callback() {
        override fun onStop() { stopCapture() }
        override fun onCapturedContentResize(width: Int, height: Int) { if (width > 0 && height > 0) recreateDisplay(width, height) }
    }

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL, "Screen analysis", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopCapture(); stopSelf(); return START_NOT_STICKY }
        if (intent?.action == ACTION_PAUSE) { paused.set(true); return START_NOT_STICKY }
        if (intent?.action == ACTION_RESUME) { paused.set(false); return START_NOT_STICKY }
        startForeground(NOTIFICATION_ID, notification("Screen analysis is active"))
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, ActivityResult.RESULT_CANCELED) ?: ActivityResult.RESULT_CANCELED
        val data: Intent? = getProjectionIntent(intent)
        if (resultCode != ActivityResult.RESULT_OK || data == null) { stopSelf(); return START_NOT_STICKY }
        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            worker = HandlerThread("CarromFrameCapture").also { it.start() }; handler = Handler(worker!!.looper)
            reader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
            reader?.setOnImageAvailableListener({ source ->
                if (paused.get() || stopped.get()) { source.acquireLatestImage()?.close(); return@setOnImageAvailableListener }
                val now = System.nanoTime()
                if (now - lastFrameAt < 66_000_000L) { source.acquireLatestImage()?.close(); return@setOnImageAvailableListener }
                lastFrameAt = now
                // Acquire latest only and close immediately: bounded queue, local processing hook.
                source.acquireLatestImage()?.use { image ->
                    // Pixel conversion/detection is intentionally not advertised as complete in this baseline.
                    image.width; image.height
                }
            }, handler)
            projection?.registerCallback(callback, handler)
            createDisplay(metrics.widthPixels, metrics.heightPixels)
        } catch (_: SecurityException) { stopCapture(); stopSelf() }
          catch (_: RuntimeException) { stopCapture(); stopSelf() }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun getProjectionIntent(intent: Intent?): Intent? = if (Build.VERSION.SDK_INT >= 33) {
        intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
    } else {
        intent?.getParcelableExtra(EXTRA_RESULT_DATA) as? Intent
    }

    private fun createDisplay(width: Int, height: Int) {
        val p = projection ?: return
        val r = reader ?: return
        if (display != null) return
        display = p.createVirtualDisplay("CarromVisionAssist", width, height, resources.displayMetrics.densityDpi,
            android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, r.surface, null, handler)
    }

    private fun recreateDisplay(width: Int, height: Int) {
        if (stopped.get()) return
        try {
            display?.release(); display = null
            reader?.close()
            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            reader?.setOnImageAvailableListener({ source -> if (paused.get() || stopped.get()) source.acquireLatestImage()?.close() else source.acquireLatestImage()?.close() }, handler)
            createDisplay(width, height)
        } catch (_: RuntimeException) { stopCapture() }
    }

    private fun stopCapture() {
        if (!stopped.compareAndSet(false, true)) return
        try { reader?.setOnImageAvailableListener(null, null) } catch (_: RuntimeException) { }
        try { display?.release() } catch (_: RuntimeException) { }
        try { reader?.close() } catch (_: RuntimeException) { }
        try { projection?.unregisterCallback(callback) } catch (_: RuntimeException) { }
        try { projection?.stop() } catch (_: RuntimeException) { }
        display = null; reader = null; projection = null
        worker?.quitSafely(); worker = null; handler = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun notification(message: String) = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Carrom Vision Assist")
        .setContentText(message).setOngoing(true).build()

    override fun onDestroy() { stopCapture(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_RESULT_CODE = "projection_result_code"
        const val EXTRA_RESULT_DATA = "projection_result_data"
        const val ACTION_PAUSE = "com.example.carromassist.PAUSE"
        const val ACTION_RESUME = "com.example.carromassist.RESUME"
        const val ACTION_STOP = "com.example.carromassist.STOP"
        private const val CHANNEL = "capture_session"
        private const val NOTIFICATION_ID = 7421
    }
    private object ActivityResult { const val RESULT_CANCELED = 0; const val RESULT_OK = -1 }
}
