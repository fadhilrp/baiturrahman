package com.example.baiturrahman.ui.components

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun PrayerTimesGrid(
    timings: PrayerTimings?,
    isMobile: Boolean = false
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    var isIqomahTime by remember { mutableStateOf(false) }
    var currentIqomahPrayer by remember { mutableStateOf("") }
    var iqomahEndTime by remember { mutableStateOf<LocalTime?>(null) }
    var shouldPlayPrayerAlarm by remember { mutableStateOf(false) }
    var shouldPlayIqomahAlarm by remember { mutableStateOf(false) }

    // Track the current prayer for highlighting
    var currentPrayerName by remember { mutableStateOf("") }

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

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Get current time as LocalTime for comparison
    val currentTimeObj = LocalTime.of(
        currentTime.hour,
        currentTime.minute,
        currentTime.second
    )

    // Map of prayer names to their times
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
        } ?: "XX:XX"),
        "Dzuhur" to (timings?.Dhuhr?.substringBefore(" ") ?: "XX:XX"),
        "Ashar" to (timings?.Asr?.substringBefore(" ") ?: "XX:XX"),
        "Maghrib" to (timings?.Maghrib?.substringBefore(" ") ?: "XX:XX"),
        "Isya" to (timings?.Isha?.substringBefore(" ") ?: "XX:XX")
    )

    // Convert prayer times to LocalTime objects for comparison
    val prayerTimeObjects = prayerTimes.mapValues { (_, timeStr) ->
        try {
            LocalTime.parse(timeStr)
        } catch (e: Exception) {
            null
        }
    }

    // Order of prayer times for comparison
    val orderedPrayers = listOf("Imsak", "Shubuh", "Syuruq", "Dhuha", "Dzuhur", "Ashar", "Maghrib", "Isya")

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
                    currentIqomahPrayer = prayerName
                    currentPrayerName = prayerName
                    // Set iqomah end time to 10 minutes after prayer time
                    iqomahEndTime = prayerTime.plusMinutes(10)
                    shouldPlayPrayerAlarm = true
                    break
                }
            }
        }
    }

    // Determine current prayer even when not in iqomah time
    LaunchedEffect(currentTimeObj, isIqomahTime) {
        if (!isIqomahTime) {
            // Find the current prayer
            var foundCurrent = false

            // First check if current time is after the last prayer of the day
            val lastPrayer = orderedPrayers.last()
            val lastPrayerTime = prayerTimeObjects[lastPrayer]

            if (lastPrayerTime != null && !currentTimeObj.isBefore(lastPrayerTime)) {
                currentPrayerName = lastPrayer
                foundCurrent = true
            }

            // If not found yet, check all prayers in reverse order
            if (!foundCurrent) {
                for (i in orderedPrayers.indices.reversed()) {
                    val prayer = orderedPrayers[i]
                    val prayerTime = prayerTimeObjects[prayer]

                    if (prayerTime != null && !currentTimeObj.isBefore(prayerTime)) {
                        currentPrayerName = prayer
                        foundCurrent = true
                        break
                    }
                }
            }

            // If still not found, default to the first prayer
            if (!foundCurrent) {
                currentPrayerName = orderedPrayers.first()
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

    if (isMobile) {
        // Mobile layout - 2 rows of 4 prayers each
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // First row: Imsak, Shubuh, Syuruq, Dhuha
            Row(
                modifier = Modifier
                    .fillMaxWidth()
//                    .height(IntrinsicSize.Min)
                    .height(84.dp)
            ) {
                listOf("Imsak", "Shubuh", "Syuruq", "Dhuha").forEach { name ->
                    PrayerTimeCell(
                        name = name,
                        time = prayerTimes[name] ?: "XX:XX",
                        isCurrentPrayer = name == currentPrayerName,
                        isIqomahTime = isIqomahTime && name == currentIqomahPrayer,
                        isMobile = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Second row: Dzuhur, Ashar, Maghrib, Isya
            Row(
                modifier = Modifier
                    .fillMaxWidth()
//                    .height(IntrinsicSize.Min)
                    .height(84.dp)
            ) {
                listOf("Dzuhur", "Ashar", "Maghrib", "Isya").forEach { name ->
                    PrayerTimeCell(
                        name = name,
                        time = prayerTimes[name] ?: "XX:XX",
                        isCurrentPrayer = name == currentPrayerName,
                        isIqomahTime = isIqomahTime && name == currentIqomahPrayer,
                        isMobile = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    } else {
        // TV/Tablet layout - single row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            prayerTimes.forEach { (name, time) ->
                PrayerTimeCell(
                    name = name,
                    time = time,
                    isCurrentPrayer = name == currentPrayerName,
                    isIqomahTime = isIqomahTime && name == currentIqomahPrayer,
                    isMobile = false,
                    modifier = Modifier.weight(1f)
                )
            }
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

@Composable
private fun PrayerTimeCell(
    name: String,
    time: String,
    isCurrentPrayer: Boolean,
    isIqomahTime: Boolean,
    isMobile: Boolean,
    modifier: Modifier = Modifier
) {
    // Responsive text sizes
    val nameTextSize = if (isMobile) 16.sp else 20.sp
    val timeTextSize = if (isMobile) 25.sp else 32.sp
    val cellPadding = if (isMobile) 6.dp else 16.dp

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(if (isIqomahTime) Color.Yellow else emeraldGreen)
            .border(0.5.dp, Color.White)
            .padding(vertical = cellPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = name,
            color = if (isCurrentPrayer) {
                if (isIqomahTime) Color.Black else Color.Yellow
            } else {
                if (isIqomahTime && name == "Imsak") Color.Gray else Color.White
            },
            fontWeight = FontWeight.Bold,
            fontSize = nameTextSize
        )
        Spacer(modifier = Modifier.height(if (isMobile) 1.dp else 4.dp))
        Text(
            text = time,
            color = if (isCurrentPrayer) {
                if (isIqomahTime) Color.Black else Color.Yellow
            } else {
                if (isIqomahTime && name == "Imsak") Color.Gray else Color.White
            },
            fontSize = timeTextSize,
            fontWeight = FontWeight.Bold
        )
    }
}
