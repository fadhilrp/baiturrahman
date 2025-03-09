package com.example.baiturrahman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baiturrahman.data.model.PrayerTimings
import com.example.baiturrahman.ui.theme.emeraldGreen
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.LocalTime

@Composable
fun NextPrayerCountdown(
    timings: PrayerTimings?,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    // Update current time every second
    LaunchedEffect(Unit) {
        while(true) {
            currentTime = LocalDateTime.now()
            delay(1000)
        }
    }

    // Get current time as LocalTime for comparison
    val currentTimeObj = LocalTime.of(
        currentTime.hour,
        currentTime.minute,
        currentTime.second
    )

    // Create a map of prayer times
    val prayerTimes = mapOf(
        "Imsak" to (timings?.Imsak?.substringBefore(" ") ?: "04:25"),
        "Shubuh" to (timings?.Fajr?.substringBefore(" ") ?: "04:35"),
        "Syuruq" to (timings?.Sunrise?.substringBefore(" ") ?: "05:57"),
        "Dhuha" to "06:22",
        "Dzuhur" to (timings?.Dhuhr?.substringBefore(" ") ?: "12:13"),
        "Ashar" to (timings?.Asr?.substringBefore(" ") ?: "15:34"),
        "Maghrib" to (timings?.Maghrib?.substringBefore(" ") ?: "18:04"),
        "Isya" to (timings?.Isha?.substringBefore(" ") ?: "19:18")
    )

    // Order of prayer times for comparison
    val orderedPrayers = listOf("Imsak", "Shubuh", "Syuruq", "Dhuha", "Dzuhur", "Ashar", "Maghrib", "Isya")

    // Convert prayer times to LocalTime objects
    val prayerTimeObjects = prayerTimes.mapValues { (_, timeStr) ->
        try {
            LocalTime.parse(timeStr)
        } catch (e: Exception) {
            null
        }
    }

    // Find current and next prayer
    val (currentPrayer, nextPrayer) = findCurrentAndNextPrayer(currentTimeObj, prayerTimeObjects, orderedPrayers)

    // Calculate time remaining until next prayer
    val timeRemaining = calculateTimeRemaining(currentTimeObj, prayerTimeObjects[nextPrayer] ?: LocalTime.MIDNIGHT)

    Box(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = 100.dp,
                    topEnd = 100.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            )
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$nextPrayer $timeRemaining",
            color = emeraldGreen,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun findCurrentAndNextPrayer(
    currentTime: LocalTime,
    prayerTimes: Map<String, LocalTime?>,
    orderedPrayers: List<String>
): Pair<String, String> {
    // Find the current prayer
    var currentPrayer = orderedPrayers.last()

    for (i in orderedPrayers.indices.reversed()) {
        val prayer = orderedPrayers[i]
        val prayerTime = prayerTimes[prayer]

        if (prayerTime != null && !currentTime.isBefore(prayerTime)) {
            currentPrayer = prayer
            break
        }
    }

    // Find the next prayer
    val currentIndex = orderedPrayers.indexOf(currentPrayer)
    val nextIndex = (currentIndex + 1) % orderedPrayers.size
    val nextPrayer = orderedPrayers[nextIndex]

    return Pair(currentPrayer, nextPrayer)
}

private fun calculateTimeRemaining(currentTime: LocalTime, nextPrayerTime: LocalTime): String {
    var nextTimeSeconds = nextPrayerTime.toSecondOfDay()
    val currentTimeSeconds = currentTime.toSecondOfDay()

    // If next prayer is tomorrow (e.g., current time is after Isya, next is Imsak)
    if (nextTimeSeconds < currentTimeSeconds) {
        nextTimeSeconds += 24 * 60 * 60 // Add 24 hours in seconds
    }

    val diffSeconds = nextTimeSeconds - currentTimeSeconds

    // Format as -HH:MM:SS
    val hours = diffSeconds / 3600
    val minutes = (diffSeconds % 3600) / 60
    val seconds = diffSeconds % 60

    return String.format("-%02d:%02d:%02d", hours, minutes, seconds)
}

