package com.example.baiturrahman.ui.components

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.baiturrahman.data.model.PrayerData
import com.example.baiturrahman.ui.theme.DarkSurface
import com.example.baiturrahman.ui.theme.EmeraldGreen
import com.example.baiturrahman.ui.theme.EmeraldLight
import com.example.baiturrahman.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CurrentDateDisplay(prayerData: PrayerData?) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMobile = screenWidth < 600

    val verticalPadding = if (isMobile) 10.dp else 8.dp
    val dividerWidth = if (isMobile) 150.dp else 250.dp

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
        "$hijriDay $formattedMonth $hijriYear"
    } else {
        "- - -"
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        cornerRadius = 8.dp,
        backgroundColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dateString,
                style = if (isMobile) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                color = EmeraldLight,
            )

            HorizontalDivider(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .width(dividerWidth),
                thickness = 1.dp,
                color = EmeraldGreen.copy(alpha = 0.5f)
            )

            Text(
                text = hijriDateText,
                style = if (isMobile) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                color = TextSecondary,
            )
        }
    }
}
