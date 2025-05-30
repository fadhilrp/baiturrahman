package com.example.baiturrahman.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.baiturrahman.ui.components.*
import com.example.baiturrahman.ui.theme.emeraldGreen
import com.example.baiturrahman.ui.viewmodel.MosqueDashboardViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MosqueDashboard(
    viewModel: MosqueDashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAdminDashboard by remember { mutableStateOf(false) }
    val mosqueImages by viewModel.mosqueImages.collectAsState()
    val currentImageIndex by viewModel.currentImageIndex.collectAsState()

    if (showAdminDashboard) {
        AdminDashboard(
            viewModel = viewModel,
            onClose = { showAdminDashboard = false }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                // Left side - Mosque Image Slider
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(width = 0.5.dp, Color.White)
                ) {
                    ImageSlider(
                        images = mosqueImages,
                        currentIndex = currentImageIndex,
                        onIndexChange = viewModel::setCurrentImageIndex,
                        modifier = Modifier.fillMaxSize()
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
                            val quote by viewModel.quoteText.collectAsState()
                            QuoteBox(quote = quote)
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
                    timings = uiState.prayerData?.timings
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
                val text by viewModel.marqueeText.collectAsState()
                MarqueeText(text = text)
            }

            // Admin button
            IconButton(
                onClick = { showAdminDashboard = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.7f), shape = androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Admin Settings",
                    tint = emeraldGreen
                )
            }
        }
    }
}

