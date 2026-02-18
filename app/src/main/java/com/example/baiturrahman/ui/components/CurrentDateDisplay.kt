package com.example.baiturrahman.ui.components

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.baiturrahman.data.model.PrayerData
import com.example.baiturrahman.ui.theme.EmeraldGreen
import com.example.baiturrahman.ui.theme.TextPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CurrentDateDisplay(prayerData: PrayerData?) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMobile = screenWidth < 600

    val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id"))
    var dateString by remember { mutableStateOf(dateFormat.format(Date())) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                dateString = dateFormat.format(Date())
                handler.postDelayed(this, 60000)
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

    val hijriDay = prayerData?.date?.hijri?.day ?: ""
    val hijriMonth = prayerData?.date?.hijri?.month?.en ?: ""
    val hijriYear = prayerData?.date?.hijri?.year ?: ""

    val formattedMonth = when (hijriMonth) {
        "Shaʿbān" -> "Sha'ban"
        else -> hijriMonth
    }

    val hijriDateText = if (hijriDay.isNotEmpty() && hijriMonth.isNotEmpty() && hijriYear.isNotEmpty()) {
        "$hijriDay $formattedMonth $hijriYear H"
    } else {
        "- - -"
    }

    val textSize = if (isMobile) 14.sp else 18.sp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dateString,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = textSize,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            ),
            color = TextPrimary.copy(alpha = 0.9f),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = hijriDateText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = textSize,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            ),
            color = EmeraldGreen,
        )
    }
}
