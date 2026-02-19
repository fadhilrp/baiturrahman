package com.example.baiturrahman.ui.components

import android.media.MediaPlayer
import com.example.baiturrahman.data.model.PrayerTimings
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val orderedPrayers = listOf("Imsak", "Shubuh", "Syuruq", "Dhuha", "Dzuhur", "Ashar", "Maghrib", "Isya")

fun buildPrayerTimes(timings: PrayerTimings?): Map<String, String> {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    return mapOf(
        "Imsak" to (timings?.Imsak?.substringBefore(" ") ?: "XX:XX"),
        "Shubuh" to (timings?.Fajr?.substringBefore(" ") ?: "XX:XX"),
        "Syuruq" to (timings?.Sunrise?.substringBefore(" ") ?: "XX:XX"),
        "Dhuha" to (timings?.Sunrise?.substringBefore(" ")?.let {
            try {
                LocalTime.parse(it, timeFormatter).plusMinutes(15).format(timeFormatter)
            } catch (_: Exception) {
                "XX:XX"
            }
        } ?: "XX:XX"),
        "Dzuhur" to (timings?.Dhuhr?.substringBefore(" ") ?: "XX:XX"),
        "Ashar" to (timings?.Asr?.substringBefore(" ") ?: "XX:XX"),
        "Maghrib" to (timings?.Maghrib?.substringBefore(" ") ?: "XX:XX"),
        "Isya" to (timings?.Isha?.substringBefore(" ") ?: "XX:XX")
    )
}

fun buildPrayerTimeObjects(prayerTimes: Map<String, String>): Map<String, LocalTime?> {
    return prayerTimes.mapValues { (_, timeStr) ->
        try {
            LocalTime.parse(timeStr)
        } catch (_: Exception) {
            null
        }
    }
}

fun playAlarmSound(mediaPlayer: MediaPlayer) {
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

fun findCurrentAndNextPrayer(
    currentTime: LocalTime,
    prayerTimes: Map<String, LocalTime?>,
    prayers: List<String>
): Pair<String, String> {
    var currentPrayer = prayers.last()
    var foundCurrent = false

    val lastPrayer = prayers.last()
    val lastPrayerTime = prayerTimes[lastPrayer]

    if (lastPrayerTime != null && !currentTime.isBefore(lastPrayerTime)) {
        currentPrayer = lastPrayer
        foundCurrent = true
    }

    if (!foundCurrent) {
        for (i in prayers.indices.reversed()) {
            val prayer = prayers[i]
            val prayerTime = prayerTimes[prayer]

            if (prayerTime != null && !currentTime.isBefore(prayerTime)) {
                currentPrayer = prayer
                foundCurrent = true
                break
            }
        }
    }

    if (!foundCurrent) {
        currentPrayer = prayers.first()
    }

    val currentIndex = prayers.indexOf(currentPrayer)
    val nextIndex = (currentIndex + 1) % prayers.size
    val nextPrayer = prayers[nextIndex]

    return Pair(currentPrayer, nextPrayer)
}

fun calculateTimeRemaining(currentTime: LocalTime, targetTime: LocalTime): String {
    var targetTimeSeconds = targetTime.toSecondOfDay()
    val currentTimeSeconds = currentTime.toSecondOfDay()

    if (targetTimeSeconds < currentTimeSeconds) {
        targetTimeSeconds += 24 * 60 * 60
    }

    val diffSeconds = targetTimeSeconds - currentTimeSeconds

    val hours = diffSeconds / 3600
    val minutes = (diffSeconds % 3600) / 60
    val seconds = diffSeconds % 60

    return String.format(Locale.ROOT, "-%02d:%02d:%02d", hours, minutes, seconds)
}
