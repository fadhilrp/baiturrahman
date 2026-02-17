package com.example.baiturrahman.ui.components

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.baiturrahman.data.model.PrayerData
import com.example.baiturrahman.ui.theme.EmeraldGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CurrentTimeDisplay(prayerData: PrayerData?) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMobile = screenWidth < 600

    val verticalPadding = if (isMobile) 10.dp else 15.dp

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    var timeString by remember { mutableStateOf(timeFormat.format(Date())) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                timeString = timeFormat.format(Date())
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> handler.post(runnable)
                Lifecycle.Event.ON_PAUSE -> handler.removeCallbacks(runnable)
                else -> { }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            handler.removeCallbacks(runnable)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(EmeraldGradient)
            .padding(vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeString,
            style = if (isMobile) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayLarge,
            color = Color.White
        )
    }
}
