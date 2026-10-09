package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.DisciplineApp

class MainActivity : ComponentActivity() {
    private var destination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        destination = intent?.getStringExtra("destination")
            ?: if (intent?.action == "OPEN_FOCUS_SCREEN") "focus" else null
        enableEdgeToEdge()
        setContent {
            DisciplineApp(initialDestination = destination)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        destination = intent.getStringExtra("destination")
            ?: if (intent.action == "OPEN_FOCUS_SCREEN") "focus" else null
    }
}

