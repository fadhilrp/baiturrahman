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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baiturrahman.R
import com.example.baiturrahman.data.model.PrayerTimings
import com.example.baiturrahman.ui.theme.emeraldGreen
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun PrayerTimesGrid(timings: PrayerTimings?) {

    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    var isIqomahTime by remember { mutableStateOf(false) }
    var currentIqomahPrayer by remember { mutableStateOf("") }
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

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val currentTimeFormatted = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    val currentTimeObj = LocalTime.parse(currentTimeFormatted)

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

    // Check if we're in iqomah period
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
            for ((prayerName, timeStr) in prayerTimes) {
                try {
                    val prayerTime = LocalTime.parse(timeStr)
                    // Skip Syuruq and Dhuha as they don't have iqomah
                    if (prayerName != "Syuruq" && prayerName != "Dhuha" && prayerName != "Imsak" &&
                        // Check if we're exactly at the prayer time or just passed it (within 1 second)
                        (currentTimeObj == prayerTime ||
                                (currentTimeObj.isAfter(prayerTime) &&
                                        ChronoUnit.SECONDS.between(prayerTime, currentTimeObj) < 2))) {

                        // We just hit a prayer time, start iqomah countdown
                        isIqomahTime = true
                        currentIqomahPrayer = prayerName
                        // Set iqomah end time to 10 minutes after prayer time
                        iqomahEndTime = prayerTime.plusMinutes(10)
                        shouldPlayPrayerAlarm = true
                        break
                    }
                } catch (e: Exception) {
                    // Skip invalid time formats
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

    val currentPrayer = if (isIqomahTime) {
        currentIqomahPrayer
    } else {
        determineCurrentPrayer(currentTimeObj, prayerTimes)
    }

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
                isIqomahTime = isIqomahTime && name == currentIqomahPrayer,
                modifier = Modifier.weight(1f)
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

@Composable
private fun PrayerTimeCell(
    name: String,
    time: String,
    isCurrentPrayer: Boolean,
    isIqomahTime: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(if (isIqomahTime) Color.Yellow else emeraldGreen)
            .border(0.5.dp, Color.White)
            .padding(vertical = 16.dp),
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
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = time,
            color = if (isCurrentPrayer) {
                if (isIqomahTime) Color.Black else Color.Yellow
            } else {
                if (isIqomahTime && name == "Imsak") Color.Gray else Color.White
            },
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        
        if (isIqomahTime) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "IQOMAH",
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
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

