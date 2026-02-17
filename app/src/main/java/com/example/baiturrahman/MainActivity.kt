package com.example.baiturrahman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.baiturrahman.ui.screens.MosqueDashboard
import com.example.baiturrahman.ui.theme.BaiturrahmanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaiturrahmanTheme {
                MosqueDashboard()
            }
        }
    }
}
