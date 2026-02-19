package com.example.baiturrahman.ui.components

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.baiturrahman.data.model.PrayerData
import com.example.baiturrahman.ui.theme.EmeraldGreen
import com.example.baiturrahman.ui.theme.JetBrainsMono
import com.example.baiturrahman.ui.theme.LocalAppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CurrentTimeDisplay(prayerData: PrayerData? = null) {
    val c = LocalAppColors.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMobile = screenWidth < 600

    val hoursFormat = SimpleDateFormat("HH", Locale.ROOT)
    val minutesFormat = SimpleDateFormat("mm", Locale.ROOT)
    val secondsFormat = SimpleDateFormat("ss", Locale.ROOT)

    var hours by remember { mutableStateOf(hoursFormat.format(Date())) }
    var minutes by remember { mutableStateOf(minutesFormat.format(Date())) }
    var seconds by remember { mutableStateOf(secondsFormat.format(Date())) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val now = Date()
                hours = hoursFormat.format(now)
                minutes = minutesFormat.format(now)
                seconds = secondsFormat.format(now)
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

    val mainSize = if (isMobile) 48.sp else 72.sp
    val colonSize = if (isMobile) 40.sp else 56.sp
    val secondsSize = if (isMobile) 24.sp else 28.sp

    // Pulsing colon animation
    val colonAlpha = pulsingAlpha(minAlpha = 0.7f, maxAlpha = 1.0f, durationMillis = 3000)

    Row(
        verticalAlignment = Alignment.Bottom
    ) {
        // Hours
        GlowText(
            text = hours,
            style = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize = mainSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-2).sp
            ),
            color = c.textPrimary,
        )
        // Colon
        Text(
            text = ":",
            style = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize = colonSize,
                fontWeight = FontWeight.Light,
            ),
            color = EmeraldGreen.copy(alpha = colonAlpha),
            modifier = Modifier.padding(horizontal = if (isMobile) 2.dp else 4.dp)
        )
        // Minutes
        GlowText(
            text = minutes,
            style = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize = mainSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-2).sp
            ),
            color = c.textPrimary,
        )
        // Seconds (smaller, muted)
        Text(
            text = seconds,
            style = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize = secondsSize,
                fontWeight = FontWeight.Light,
            ),
            color = c.mutedForeground,
            modifier = Modifier.padding(start = if (isMobile) 4.dp else 8.dp, bottom = if (isMobile) 4.dp else 8.dp)
        )
    }
}
