package com.example.baiturrahman.ui.components

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.baiturrahman.data.model.PrayerData
import com.example.baiturrahman.ui.theme.emeraldGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CurrentTimeDisplay(prayerData: PrayerData?) {
    // Use a simple date format for displaying time
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // State to hold the current time string
    var timeString by remember { mutableStateOf(timeFormat.format(Date())) }

    // Get the lifecycle owner
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Use DisposableEffect to handle timer creation and cleanup
    DisposableEffect(lifecycleOwner) {
        // Create a handler on the main thread
        val handler = Handler(Looper.getMainLooper())

        // Create a runnable that updates the time every second
        val runnable = object : Runnable {
            override fun run() {
                timeString = timeFormat.format(Date())
                handler.postDelayed(this, 1000)
            }
        }

        // Start the timer when the component is first composed
        handler.post(runnable)

        // Create a lifecycle observer to handle lifecycle events
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Resume the timer when the app is resumed
                    handler.post(runnable)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // Stop the timer when the app is paused
                    handler.removeCallbacks(runnable)
                }
                else -> { /* no-op */ }
            }
        }

        // Register the observer
        lifecycleOwner.lifecycle.addObserver(observer)

        // Clean up when the component is disposed
        onDispose {
            handler.removeCallbacks(runnable)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(emeraldGreen)
            .padding(vertical = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeString,
            fontSize = 60.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

