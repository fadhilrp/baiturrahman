package com.example.baiturrahman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baiturrahman.data.model.PrayerTimings
import com.example.baiturrahman.ui.theme.emeraldGreen
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun PrayerTimesGrid(timings: PrayerTimings?) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while(true) {
            currentTime = LocalDateTime.now()
            delay(1000)
        }
    }

    val currentTimeFormatted = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    val currentTimeObj = LocalTime.parse(currentTimeFormatted)

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

    val currentPrayer = determineCurrentPrayer(currentTimeObj, prayerTimes)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        prayerTimes.forEach { (name, time) ->
            PrayerTimeCell(
                name = name,
                time = time,
                isCurrentPrayer = name == currentPrayer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PrayerTimeCell(
    name: String,
    time: String,
    isCurrentPrayer: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(emeraldGreen)
            .border(0.5.dp, Color.White)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = name,
            color = if (isCurrentPrayer) Color.Yellow else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = time,
            color = if (isCurrentPrayer) Color.Yellow else Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun determineCurrentPrayer(currentTime: LocalTime, prayerTimes: Map<String, String>): String {
    val prayerTimeObjects = prayerTimes.mapValues { (_, timeStr) ->
        try {
            LocalTime.parse(timeStr)
        } catch (e: Exception) {
            null
        }
    }

    // Order of prayer times for comparison
    val orderedPrayers = listOf("Imsak", "Shubuh", "Syuruq", "Dhuha", "Dzuhur", "Ashar", "Maghrib", "Isya")

    // Check if current time is before first prayer or after last prayer
    val firstPrayer = orderedPrayers.first()
    val lastPrayer = orderedPrayers.last()

    prayerTimeObjects[firstPrayer]?.let { firstTime ->
        if (currentTime.isBefore(firstTime)) {
            return lastPrayer
        }
    }

    // Find the current prayer time
    for (i in orderedPrayers.indices.reversed()) {
        val prayer = orderedPrayers[i]
        prayerTimeObjects[prayer]?.let { prayerTime ->
            if (!currentTime.isBefore(prayerTime)) {
                return prayer
            }
        }
    }

    return lastPrayer // Default to last prayer if no other matches
}

