package com.example.chesoverlay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL 
            setPadding(50, 50, 50, 50)
        }
        
        val tv = TextView(this).apply { text = "Chess Overlay готов" }
        
        val btn = Button(this).apply {
            text = "Включить плавающую кнопку"
            setOnClickListener {
                // Проверяем, дал ли пользователь разрешение на оверлей
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    // Если нет - отправляем в настройки
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                } else {
                    // Если да - запускаем службу
                    startService(Intent(this@MainActivity, OverlayService::class.java))
                }
            }
        }
        
        layout.addView(tv)
        layout.addView(btn)
        setContentView(layout)
    }
}
