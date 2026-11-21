package com.example.chesoverlay

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var floatView: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Создаем красный квадрат с текстом
        floatView = TextView(this).apply {
            text = "АНАЛИЗ"
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(30, 30, 30, 30)
        }

        // Настройки окна поверх других приложений
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        windowManager?.addView(floatView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        floatView?.let { windowManager?.removeView(it) }
    }
}
