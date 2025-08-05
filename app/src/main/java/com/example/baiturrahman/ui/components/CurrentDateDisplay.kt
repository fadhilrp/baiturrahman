package com.example.baiturrahman.ui.components

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.baiturrahman.data.model.PrayerData
import com.example.baiturrahman.ui.theme.emeraldGreen
import com.example.baiturrahman.ui.theme.white
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CurrentDateDisplay(prayerData: PrayerData?) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMobile = screenWidth < 600

    // Responsive text sizes
    val dateTextSize = if (isMobile) 18.sp else 25.sp
    val verticalPadding = if (isMobile) 10.dp else 8.dp
    val dividerWidth = if (isMobile) 150.dp else 250.dp

    // Use a simple date format for displaying date
    val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id"))

    // State to hold the current date string
    var dateString by remember { mutableStateOf(dateFormat.format(Date())) }

    // Get the lifecycle owner
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Use DisposableEffect to handle timer creation and cleanup
    DisposableEffect(lifecycleOwner) {
        // Create a handler on the main thread
        val handler = Handler(Looper.getMainLooper())

        // Create a runnable that updates the date every minute
        val runnable = object : Runnable {
            override fun run() {
                dateString = dateFormat.format(Date())
                // Update every minute (60000ms)
                handler.postDelayed(this, 60000)
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

    // Format the Hijri date from prayerData
    val hijriDay = prayerData?.date?.hijri?.day ?: ""
    val hijriMonth = prayerData?.date?.hijri?.month?.en ?: ""
    val hijriYear = prayerData?.date?.hijri?.year ?: ""

    // Replace "Shaʿbān" with "Sha'ban" if needed
    val formattedMonth = when (hijriMonth) {
        "Shaʿbān" -> "Sha'ban"
        else -> hijriMonth
    }

    // Build the Hijri date string (e.g., "23 Sha'ban 1446")
    val hijriDateText = if (hijriDay.isNotEmpty() && hijriMonth.isNotEmpty() && hijriYear.isNotEmpty()) {
        "$hijriDay $formattedMonth $hijriYear"
    } else {
        "- - -" // Fallback if data is not available
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(white)
            .padding(vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(
            text = dateString,
            fontSize = dateTextSize,
            color = emeraldGreen,
        )

        // Add a thin line
        HorizontalDivider(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .width(dividerWidth),
            thickness = 2.dp,
            color = emeraldGreen
        )

        Text(
            text = hijriDateText,
            fontSize = dateTextSize,
            color = emeraldGreen,
        )
    }
}
