package com.example.multiplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.multiplayer.ui.home.HomeScreen
import com.example.multiplayer.ui.theme.MultiPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MultiPlayerTheme {
                HomeScreen()
            }
        }
    }
}

