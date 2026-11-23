package com.example.chesoverlay

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class ScreenCaptureService : Service() {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    override fun onCreate() {
        super.onCreate()
        // Переносим уведомление и запуск foreground сюда
        val channelId = "screen_capture_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Захват экрана", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        // Для Android 14 обязателен PendingIntent, иначе будет краш
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Анализ шахмат")
            .setContentText("Приложение смотрит на экран...")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Throwable) {
            logCrash("Ошибка startForeground", e)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
            val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra("data", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra("data")
            }

            if (resultCode == Activity.RESULT_OK && data != null) {
                val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = mgr.getMediaProjection(resultCode, data)
                startCapture()
            } else {
                logCrash("Пустые данные", Exception("resultCode: $resultCode"))
                stopSelf()
            }
        } catch (e: Throwable) {
            logCrash("Ошибка в onStartCommand", e)
            stopSelf()
        }
        return START_STICKY
    }

    private fun startCapture() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                val w = image.width
                val h = image.height
                image.close()
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(applicationContext, "Кадр захвачен: ${w}x${h}", Toast.LENGTH_SHORT).show()
                }
            }
        }, null)
    }

    private fun logCrash(prefix: String, e: Throwable) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        val file = File(filesDir, "crash.txt")
        file.writeText("$prefix:\n$sw")
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        super.onDestroy()
    }
}
