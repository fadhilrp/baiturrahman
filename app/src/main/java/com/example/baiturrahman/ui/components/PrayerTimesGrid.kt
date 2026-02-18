package com.example.baiturrahman.ui.components

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.baiturrahman.R
import com.example.baiturrahman.data.model.PrayerTimings
import com.example.baiturrahman.ui.theme.DarkSurface
import com.example.baiturrahman.ui.theme.EmeraldGreen
import com.example.baiturrahman.ui.theme.EmeraldMuted
import com.example.baiturrahman.ui.theme.GlassBorder
import com.example.baiturrahman.ui.theme.GoldAccent
import com.example.baiturrahman.ui.theme.GoldMuted
import com.example.baiturrahman.ui.theme.TextPrimary
import com.example.baiturrahman.ui.theme.TextSecondary
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

    var currentPrayerName by remember { mutableStateOf("") }

    val prayerAlarmPlayer = remember { MediaPlayer.create(context, R.raw.prayer_alarm) }
    val iqomahAlarmPlayer = remember { MediaPlayer.create(context, R.raw.prayer_alarm) }

    DisposableEffect(Unit) {
        onDispose {
            prayerAlarmPlayer.release()
            iqomahAlarmPlayer.release()
        }
    }

    LaunchedEffect(Unit) {
        while(true) {
            currentTime = LocalDateTime.now()
            delay(1000)
        }
    }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val currentTimeObj = LocalTime.of(
        currentTime.hour,
        currentTime.minute,
        currentTime.second
    )

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

    val prayerTimeObjects = prayerTimes.mapValues { (_, timeStr) ->
        try {
            LocalTime.parse(timeStr)
        } catch (e: Exception) {
            null
        }
    }

    val orderedPrayers = listOf("Imsak", "Shubuh", "Syuruq", "Dhuha", "Dzuhur", "Ashar", "Maghrib", "Isya")

    LaunchedEffect(currentTimeObj) {
        if (iqomahEndTime != null) {
            if (currentTimeObj.isAfter(iqomahEndTime) || currentTimeObj == iqomahEndTime) {
                isIqomahTime = false
                iqomahEndTime = null
                shouldPlayIqomahAlarm = true
            }
        } else {
            for ((prayerName, prayerTime) in prayerTimeObjects) {
                if (prayerTime != null &&
                    prayerName != "Syuruq" && prayerName != "Dhuha" && prayerName != "Imsak" &&
                    (currentTimeObj == prayerTime ||
                            (currentTimeObj.isAfter(prayerTime) &&
                                    ChronoUnit.SECONDS.between(prayerTime, currentTimeObj) < 2))) {

                    isIqomahTime = true
                    currentIqomahPrayer = prayerName
                    currentPrayerName = prayerName
                    iqomahEndTime = prayerTime.plusMinutes(10)
                    shouldPlayPrayerAlarm = true
                    break
                }
            }
        }
    }

    LaunchedEffect(currentTimeObj, isIqomahTime) {
        if (!isIqomahTime) {
            var foundCurrent = false

            val lastPrayer = orderedPrayers.last()
            val lastPrayerTime = prayerTimeObjects[lastPrayer]

            if (lastPrayerTime != null && !currentTimeObj.isBefore(lastPrayerTime)) {
                currentPrayerName = lastPrayer
                foundCurrent = true
            }

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

            if (!foundCurrent) {
                currentPrayerName = orderedPrayers.first()
            }
        }
    }

    LaunchedEffect(shouldPlayPrayerAlarm) {
        if (shouldPlayPrayerAlarm) {
            playAlarmSound(context, prayerAlarmPlayer)
            shouldPlayPrayerAlarm = false
        }
    }

    LaunchedEffect(shouldPlayIqomahAlarm) {
        if (shouldPlayIqomahAlarm) {
            playAlarmSound(context, iqomahAlarmPlayer)
            shouldPlayIqomahAlarm = false
        }
    }

    if (isMobile) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
    val cellPadding = if (isMobile) 6.dp else 16.dp

    // Animated colors for smooth transitions
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isIqomahTime -> GoldMuted
            isCurrentPrayer -> EmeraldMuted
            else -> DarkSurface
        },
        animationSpec = tween(durationMillis = STANDARD_DURATION),
        label = "cell_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isIqomahTime -> GoldAccent
            isCurrentPrayer -> EmeraldGreen
            else -> GlassBorder
        },
        animationSpec = tween(durationMillis = STANDARD_DURATION),
        label = "cell_border"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isIqomahTime -> GoldAccent
            isCurrentPrayer -> EmeraldGreen
            else -> TextPrimary
        },
        animationSpec = tween(durationMillis = STANDARD_DURATION),
        label = "cell_text"
    )

    val nameColor by animateColorAsState(
        targetValue = when {
            isIqomahTime -> GoldAccent
            isCurrentPrayer -> EmeraldGreen
            else -> TextSecondary
        },
        animationSpec = tween(durationMillis = STANDARD_DURATION),
        label = "cell_name"
    )

    val borderWidth = if (isIqomahTime || isCurrentPrayer) 2.dp else 1.dp
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
            .padding(vertical = cellPadding)
            .focusScale(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = name,
            color = nameColor,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(if (isMobile) 1.dp else 4.dp))
        Text(
            text = time,
            color = textColor,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}
