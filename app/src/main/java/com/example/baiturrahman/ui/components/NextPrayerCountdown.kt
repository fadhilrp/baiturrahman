package com.example.baiturrahman.ui.components

import android.media.MediaPlayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baiturrahman.R
import com.example.baiturrahman.data.model.PrayerTimings
import com.example.baiturrahman.ui.theme.EmeraldGreen
import com.example.baiturrahman.ui.theme.Foreground
import com.example.baiturrahman.ui.theme.GlassBorder
import com.example.baiturrahman.ui.theme.GlassWhite
import com.example.baiturrahman.ui.theme.GoldAccent
import com.example.baiturrahman.ui.theme.JetBrainsMono
import com.example.baiturrahman.ui.theme.PlusJakartaSans
import com.example.baiturrahman.ui.theme.TextOnAccent
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Composable
fun NextPrayerCountdown(
    timings: PrayerTimings?,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMobile = screenWidth < 600

    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    var isIqomahTime by remember { mutableStateOf(false) }
    var currentPrayerName by remember { mutableStateOf("") }
    var iqomahEndTime by remember { mutableStateOf<LocalTime?>(null) }
    var shouldPlayPrayerAlarm by remember { mutableStateOf(false) }
    var shouldPlayIqomahAlarm by remember { mutableStateOf(false) }

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

    val currentTimeObj = LocalTime.of(
        currentTime.hour,
        currentTime.minute,
        currentTime.second
    )

    val prayerTimes = buildPrayerTimes(timings)
    val prayerTimeObjects = buildPrayerTimeObjects(prayerTimes)

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
                    currentPrayerName = prayerName
                    iqomahEndTime = prayerTime.plusMinutes(10)
                    shouldPlayPrayerAlarm = true
                    break
                }
            }
        }
    }

    LaunchedEffect(shouldPlayPrayerAlarm) {
        if (shouldPlayPrayerAlarm) {
            playAlarmSound(prayerAlarmPlayer)
            shouldPlayPrayerAlarm = false
        }
    }

    LaunchedEffect(shouldPlayIqomahAlarm) {
        if (shouldPlayIqomahAlarm) {
            playAlarmSound(iqomahAlarmPlayer)
            shouldPlayIqomahAlarm = false
        }
    }

    val (_, nextPrayer) = if (!isIqomahTime) {
        findCurrentAndNextPrayer(currentTimeObj, prayerTimeObjects, orderedPrayers)
    } else {
        Pair(currentPrayerName, currentPrayerName)
    }

    val displayPrayerName = if (isIqomahTime) "Iqomah $currentPrayerName" else nextPrayer
    val timeRemaining = if (isIqomahTime) {
        calculateTimeRemaining(currentTimeObj, iqomahEndTime ?: LocalTime.MIDNIGHT)
    } else {
        calculateTimeRemaining(currentTimeObj, prayerTimeObjects[nextPrayer] ?: LocalTime.MIDNIGHT)
    }

    // Animated colors
    val nameColor by animateColorAsState(
        targetValue = if (isIqomahTime) GoldAccent else EmeraldGreen,
        animationSpec = tween(durationMillis = STANDARD_DURATION),
        label = "countdown_name"
    )

    val countdownColor by animateColorAsState(
        targetValue = if (isIqomahTime) TextOnAccent else Foreground.copy(alpha = 0.7f),
        animationSpec = tween(durationMillis = STANDARD_DURATION),
        label = "countdown_time"
    )

    val shape = RoundedCornerShape(
        topStart = if (isMobile) 16.dp else 24.dp,
        topEnd = if (isMobile) 16.dp else 24.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    val bgColor by animateColorAsState(
        targetValue = if (isIqomahTime) GoldAccent.copy(alpha = 0.85f) else GlassWhite,
        animationSpec = tween(durationMillis = STANDARD_DURATION),
        label = "countdown_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isIqomahTime) GoldAccent else GlassBorder,
        animationSpec = tween(durationMillis = STANDARD_DURATION),
        label = "countdown_border"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor, shape)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Prayer name with stroke outline
            val nameStyle = TextStyle(
                fontFamily = PlusJakartaSans,
                fontSize = if (isMobile) 22.sp else 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Box(contentAlignment = Alignment.Center) {
                // Stroke layer
                Text(
                    text = displayPrayerName,
                    style = nameStyle.copy(
                        drawStyle = Stroke(width = 3f, join = StrokeJoin.Round)
                    ),
                    color = Color.Black.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
                // Fill layer
                Text(
                    text = displayPrayerName,
                    style = nameStyle,
                    color = nameColor,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(if (isMobile) 2.dp else 4.dp))

            // Countdown with stroke outline
            val countdownStyle = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize = if (isMobile) 16.sp else 16.sp,
            )
            Box(contentAlignment = Alignment.Center) {
                // Stroke layer
                Text(
                    text = timeRemaining,
                    style = countdownStyle.copy(
                        drawStyle = Stroke(width = 3f, join = StrokeJoin.Round)
                    ),
                    color = Color.Black.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
                // Fill layer
                Text(
                    text = timeRemaining,
                    style = countdownStyle,
                    color = countdownColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
