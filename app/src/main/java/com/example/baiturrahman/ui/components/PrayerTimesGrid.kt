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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baiturrahman.R
import com.example.baiturrahman.data.model.PrayerTimings
import com.example.baiturrahman.ui.theme.ActivePrayerBg
import com.example.baiturrahman.ui.theme.Border
import com.example.baiturrahman.ui.theme.EmeraldGreen
import com.example.baiturrahman.ui.theme.Foreground
import com.example.baiturrahman.ui.theme.GoldAccent
import com.example.baiturrahman.ui.theme.GoldMuted
import com.example.baiturrahman.ui.theme.JetBrainsMono
import com.example.baiturrahman.ui.theme.MutedForeground
import com.example.baiturrahman.ui.theme.NextPrayerBg
import com.example.baiturrahman.ui.theme.PlusJakartaSans
import com.example.baiturrahman.ui.theme.Secondary
import com.example.baiturrahman.ui.theme.SecondaryForeground
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
    var nextPrayerName by remember { mutableStateOf("") }

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

            val currentIndex = orderedPrayers.indexOf(currentPrayerName)
            val nextIndex = (currentIndex + 1) % orderedPrayers.size
            nextPrayerName = orderedPrayers[nextIndex]
        }
    }

    LaunchedEffect(shouldPlayPrayerAlarm) {
        if (shouldPlayPrayerAlarm) {
            playAlarmSoundGrid(context, prayerAlarmPlayer)
            shouldPlayPrayerAlarm = false
        }
    }

    LaunchedEffect(shouldPlayIqomahAlarm) {
        if (shouldPlayIqomahAlarm) {
            playAlarmSoundGrid(context, iqomahAlarmPlayer)
            shouldPlayIqomahAlarm = false
        }
    }

    if (isMobile) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Imsak", "Shubuh", "Syuruq", "Dhuha").forEach { name ->
                    PrayerTimeCard(
                        name = name,
                        time = prayerTimes[name] ?: "XX:XX",
                        isActive = name == currentPrayerName,
                        isNext = name == nextPrayerName && !isIqomahTime,
                        isIqomahTime = isIqomahTime && name == currentIqomahPrayer,
                        isMobile = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Dzuhur", "Ashar", "Maghrib", "Isya").forEach { name ->
                    PrayerTimeCard(
                        name = name,
                        time = prayerTimes[name] ?: "XX:XX",
                        isActive = name == currentPrayerName,
                        isNext = name == nextPrayerName && !isIqomahTime,
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
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            prayerTimes.forEach { (name, time) ->
                PrayerTimeCard(
                    name = name,
                    time = time,
                    isActive = name == currentPrayerName,
                    isNext = name == nextPrayerName && !isIqomahTime,
                    isIqomahTime = isIqomahTime && name == currentIqomahPrayer,
                    isMobile = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun playAlarmSoundGrid(context: Context, mediaPlayer: MediaPlayer) {
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
private fun PrayerTimeCard(
    name: String,
    time: String,
    isActive: Boolean,
    isNext: Boolean,
    isIqomahTime: Boolean,
    isMobile: Boolean,
    modifier: Modifier = Modifier
) {
    // Animated colors for smooth transitions (500ms to match web)
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isIqomahTime -> GoldMuted
            isActive -> ActivePrayerBg
            isNext -> NextPrayerBg
            else -> Secondary.copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = 500),
        label = "card_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isIqomahTime -> GoldAccent
            isActive -> EmeraldGreen.copy(alpha = 0.4f)
            isNext -> EmeraldGreen.copy(alpha = 0.2f)
            else -> Border.copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = 500),
        label = "card_border"
    )

    val nameColor by animateColorAsState(
        targetValue = when {
            isIqomahTime -> GoldAccent
            isActive -> EmeraldGreen
            isNext -> EmeraldGreen.copy(alpha = 0.7f)
            else -> MutedForeground
        },
        animationSpec = tween(durationMillis = 500),
        label = "card_name"
    )

    val timeColor by animateColorAsState(
        targetValue = when {
            isIqomahTime -> GoldAccent
            isActive -> Foreground
            isNext -> Foreground
            else -> SecondaryForeground
        },
        animationSpec = tween(durationMillis = 500),
        label = "card_time"
    )

    val shape = RoundedCornerShape(12.dp)
    val cardModifier = if (isActive && !isIqomahTime) {
        modifier
            .fillMaxHeight()
            .clip(shape)
            .activePrayerGlow()
            .background(backgroundColor, shape)
            .border(1.dp, borderColor, shape)
    } else {
        modifier
            .fillMaxHeight()
            .clip(shape)
            .background(backgroundColor, shape)
            .border(1.dp, borderColor, shape)
    }

    Column(
        modifier = cardModifier
            .padding(
                vertical = if (isMobile) 12.dp else 16.dp,
                horizontal = if (isMobile) 4.dp else 8.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Prayer name
        Text(
            text = name.uppercase(),
            style = TextStyle(
                fontFamily = PlusJakartaSans,
                fontSize = if (isMobile) 10.sp else 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
            ),
            color = nameColor,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.height(if (isMobile) 4.dp else 8.dp))

        // Time (monospace)
        if (isActive && !isIqomahTime) {
            GlowText(
                text = time,
                style = TextStyle(
                    fontFamily = JetBrainsMono,
                    fontSize = if (isMobile) 16.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
                color = timeColor,
                maxLines = 1,
            )
        } else {
            Text(
                text = time,
                style = TextStyle(
                    fontFamily = JetBrainsMono,
                    fontSize = if (isMobile) 16.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
                color = timeColor,
                maxLines = 1,
            )
        }

        // Iqomah badge
        if (isIqomahTime) {
            Spacer(modifier = Modifier.height(if (isMobile) 4.dp else 8.dp))
            Text(
                text = "IQOMAH",
                style = TextStyle(
                    fontFamily = PlusJakartaSans,
                    fontSize = if (isMobile) 7.sp else 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                ),
                color = GoldAccent,
            )
        }
    }
}
