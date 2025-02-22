package com.example.baiturrahman

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MosqueDashboard()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MosqueDashboard() {
    Color(0xFF34D399)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Header()

            // Current Time and Date
            CurrentTimeDisplay()

            // Quote Box
            QuoteBox()

            // Prayer Times Grid
            PrayerTimesGrid()
        }

        // Marquee text at bottom
        MarqueeText(
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun Header() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Masjid Baiturrahman\nPondok Pinang",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        // Logo can be added here
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CurrentTimeDisplay() {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while(true) {
            currentTime = LocalDateTime.now()
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF34D399))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            fontSize = 72.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = currentTime.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id"))),
            fontSize = 24.sp,
            color = Color.White
        )

        Text(
            text = "5 Sha'ban 1446", // This should be calculated based on Hijri calendar
            fontSize = 24.sp,
            color = Color.White
        )
    }
}

@Composable
fun QuoteBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF34D399))
            .padding(16.dp)
    ) {
        Text(
            text = "\"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec vel egestas dolor, nec dignissim metus.\"",
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PrayerTimesGrid() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PrayerTimeCell("Imsak", "04:25")
        PrayerTimeCell("Shubuh", "04:35")
        PrayerTimeCell("Syuruq", "05:57")
        PrayerTimeCell("Dhuha", "06:22")
        PrayerTimeCell("Dzuhur", "12:13")
        PrayerTimeCell("Ashar", "15:34")
        PrayerTimeCell("Maghrib", "18:04")
        PrayerTimeCell("Isya", "19:18")
    }
}

@Composable
fun PrayerTimeCell(name: String, time: String) {
    Column(
        modifier = Modifier
            .background(Color(0xFF34D399))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = name,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = time,
            color = Color.White,
            fontSize = 20.sp
        )
    }
}

@Composable
fun MarqueeText(modifier: Modifier = Modifier) {
    var offset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while(true) {
            delay(50)
            offset -= 1f
            if (offset < -500f) offset = 500f // Reset position
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(8.dp)
    ) {
        Text(
            text = "Rolling Text Rolling Text Rolling Text",
            color = Color.White,
            modifier = Modifier.offset(x = offset.dp)
        )
    }
}