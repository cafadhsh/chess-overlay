package com.example.chesoverlay

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : AppCompatActivity() {
    
    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            Toast.makeText(this, "Захват экрана отменен", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestNotifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startCaptureIntent()
        } else {
            Toast.makeText(this, "Без разрешения на уведомления захват не запустится", Toast.LENGTH_LONG).show()
        }
    }

    private fun startCaptureIntent() {
        try {
            val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            captureLauncher.launch(mgr.createScreenCaptureIntent())
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка запуска: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val file = File(filesDir, "crash.txt")
            file.writeText(sw.toString())
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }

        // Оборачиваем всё в ScrollView, чтобы можно было листать
        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL 
            setPadding(50, 50, 50, 50)
        }
        
        val crashFile = File(filesDir, "crash.txt")
        if (crashFile.exists()) {
            val errorText = crashFile.readText()
            val tvError = TextView(this).apply {
                text = "ОШИБКА (можно скопировать):\n\n$errorText"
                textSize = 12f
                setTextColor(Color.RED)
                // Это включает выделение и копирование текста
                setTextIsSelectable(true)
                setPadding(0, 0, 0, 50)
            }
            layout.addView(tvError)
            
            val btnClear = Button(this).apply {
                text = "Очистить ошибку"
                setOnClickListener {
                    crashFile.delete()
                    recreate()
                }
            }
            layout.addView(btnClear)
        }
        
        val tv = TextView(this).apply { text = "Chess Overlay готов" }
        
        val btnOverlay = Button(this).apply {
            text = "1. Включить кнопку оверлея"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                } else {
                    startService(Intent(this@MainActivity, OverlayService::class.java))
                }
            }
        }

        val btnCapture = Button(this).apply {
            text = "2. Начать захват экрана"
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        startCaptureIntent()
                    } else {
                        requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    startCaptureIntent()
                }
            }
        }
        
        layout.addView(tv)
        layout.addView(btnOverlay)
        layout.addView(btnCapture)
        
        scrollView.addView(layout)
        setContentView(scrollView)
    }
}
