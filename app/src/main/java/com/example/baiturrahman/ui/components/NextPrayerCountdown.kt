package com.example.baiturrahman.ui.components

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baiturrahman.R
import com.example.baiturrahman.data.model.PrayerTimings
import com.example.baiturrahman.ui.theme.emeraldGreen
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

@Composable
fun NextPrayerCountdown(
    timings: PrayerTimings?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    var isIqomahTime by remember { mutableStateOf(false) }
    var currentPrayerName by remember { mutableStateOf("") }
    var iqomahEndTime by remember { mutableStateOf<LocalTime?>(null) }
    var shouldPlayPrayerAlarm by remember { mutableStateOf(false) }
    var shouldPlayIqomahAlarm by remember { mutableStateOf(false) }

    // MediaPlayer instances for alarms
    val prayerAlarmPlayer = remember { MediaPlayer.create(context, R.raw.prayer_alarm) }
    val iqomahAlarmPlayer = remember { MediaPlayer.create(context, R.raw.iqomah_alarm) }

    // Clean up MediaPlayer when component is disposed
    DisposableEffect(Unit) {
        onDispose {
            prayerAlarmPlayer.release()
            iqomahAlarmPlayer.release()
        }
    }

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

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val prayerTimes = mapOf(
        "Imsak" to (timings?.Imsak?.substringBefore(" ") ?: "XX:XX"),
        "Shubuh" to (timings?.Fajr?.substringBefore(" ") ?: "XX:XX"),
        "Syuruq" to (timings?.Sunrise?.substringBefore(" ") ?: "XX:XX"),
        "Dhuha" to (timings?.Sunrise?.substringBefore(" ")?.let {
            try {
                LocalTime.parse(it, timeFormatter).plusMinutes(15).format(timeFormatter)
            } catch (e: Exception) {
                "XX:XX"
            }
        }  ?: "XX:XX"),
        "Dzuhur" to (timings?.Dhuhr?.substringBefore(" ") ?: "XX:XX"),
        "Ashar" to (timings?.Asr?.substringBefore(" ") ?: "XX:XX"),
        "Maghrib" to (timings?.Maghrib?.substringBefore(" ") ?: "XX:XX"),
        "Isya" to (timings?.Isha?.substringBefore(" ") ?: "XX:XX")
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

    // Check if we're in iqomah period or if we just hit a prayer time
    LaunchedEffect(currentTimeObj) {
        // If we have an iqomah end time set, check if we're still in iqomah period
        if (iqomahEndTime != null) {
            if (currentTimeObj.isAfter(iqomahEndTime) || currentTimeObj == iqomahEndTime) {
                // Iqomah period is over
                isIqomahTime = false
                iqomahEndTime = null
                shouldPlayIqomahAlarm = true
            }
        } else {
            // Check if we just hit a prayer time
            for ((prayerName, prayerTime) in prayerTimeObjects) {
                if (prayerTime != null &&
                    // Skip Syuruq and Dhuha as they don't have iqomah
                    prayerName != "Syuruq" && prayerName != "Dhuha" && prayerName != "Imsak" &&
                    // Check if we're exactly at the prayer time or just passed it (within 1 second)
                    (currentTimeObj == prayerTime ||
                            (currentTimeObj.isAfter(prayerTime) &&
                                    ChronoUnit.SECONDS.between(prayerTime, currentTimeObj) < 2))) {

                    // We just hit a prayer time, start iqomah countdown
                    isIqomahTime = true
                    currentPrayerName = prayerName
                    // Set iqomah end time to 10 minutes after prayer time
                    iqomahEndTime = prayerTime.plusMinutes(10)
                    shouldPlayPrayerAlarm = true
                    break
                }
            }
        }
    }

    // Play prayer alarm when needed
    LaunchedEffect(shouldPlayPrayerAlarm) {
        if (shouldPlayPrayerAlarm) {
            playAlarmSound(context, prayerAlarmPlayer)
            shouldPlayPrayerAlarm = false
        }
    }

    // Play iqomah alarm when needed
    LaunchedEffect(shouldPlayIqomahAlarm) {
        if (shouldPlayIqomahAlarm) {
            playAlarmSound(context, iqomahAlarmPlayer)
            shouldPlayIqomahAlarm = false
        }
    }

    // Find current and next prayer if not in iqomah time
    val (currentPrayer, nextPrayer) = if (!isIqomahTime) {
        findCurrentAndNextPrayer(currentTimeObj, prayerTimeObjects, orderedPrayers)
    } else {
        Pair(currentPrayerName, currentPrayerName)
    }

    // Calculate time remaining
    val displayText = if (isIqomahTime) {
        // Calculate time remaining until iqomah ends
        val timeRemaining = calculateTimeRemaining(currentTimeObj, iqomahEndTime ?: LocalTime.MIDNIGHT)
        "Iqomah $currentPrayerName $timeRemaining"
    } else {
        // Calculate time remaining until next prayer
        val timeRemaining = calculateTimeRemaining(currentTimeObj, prayerTimeObjects[nextPrayer] ?: LocalTime.MIDNIGHT)
        "$nextPrayer $timeRemaining"
    }

    // UI for countdown
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
            .background(if (isIqomahTime) Color.Yellow.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displayText,
                color = if (isIqomahTime) Color.Black else emeraldGreen,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )

        }
    }
}

private fun playAlarmSound(context: Context, mediaPlayer: MediaPlayer) {
    try {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.prepare()
        }
        mediaPlayer.start()
    } catch (e: Exception) {
        e.printStackTrace()
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

private fun calculateTimeRemaining(currentTime: LocalTime, targetTime: LocalTime): String {
    var targetTimeSeconds = targetTime.toSecondOfDay()
    val currentTimeSeconds = currentTime.toSecondOfDay()

    // If target time is tomorrow (e.g., current time is after Isya, next is Imsak)
    if (targetTimeSeconds < currentTimeSeconds) {
        targetTimeSeconds += 24 * 60 * 60 // Add 24 hours in seconds
    }

    val diffSeconds = targetTimeSeconds - currentTimeSeconds

    // Format as -HH:MM:SS
    val hours = diffSeconds / 3600
    val minutes = (diffSeconds % 3600) / 60
    val seconds = diffSeconds % 60

    return String.format("-%02d:%02d:%02d", hours, minutes, seconds)
}

