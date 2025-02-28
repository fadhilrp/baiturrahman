package com.example.baiturrahman.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.baiturrahman.R
import com.example.baiturrahman.ui.components.*
import com.example.baiturrahman.ui.viewmodel.MosqueDashboardViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MosqueDashboard(
    viewModel: MosqueDashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Left side - Mosque Image
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(width = 0.5.dp, Color.White)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mosque),
                    contentDescription = "Masjid Baiturrahman",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            // Right side - Clock & Mosque Info
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Header with logo
                        Header()

                        // Current Time Display
                        CurrentTimeDisplay(uiState.prayerData)

                        // Current Date Display
                        CurrentDateDisplay(uiState.prayerData)

                        // Quote Box
                        QuoteBox()
                    }
                }
            }
        }

        // Countdown overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp + 90.dp, end = 200.dp + 100.dp)
        ) {
            NextPrayerCountdown(
                prayerName = "Dzuhur",
                timeRemaining = "-00:04:28"
            )
        }

        // Prayer Times Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            PrayerTimesGrid(uiState.prayerData?.timings)
        }

        // MarqueeText - Stays at the very bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            MarqueeText()
        }
    }
}

