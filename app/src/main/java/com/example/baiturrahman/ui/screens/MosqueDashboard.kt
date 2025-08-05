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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.baiturrahman.ui.components.*
import com.example.baiturrahman.ui.theme.emeraldGreen
import com.example.baiturrahman.ui.viewmodel.MosqueDashboardViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MosqueDashboard(
    viewModel: MosqueDashboardViewModel = koinViewModel()
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    // Consider mobile if width < 600dp or if height > width (portrait mode)
    val isMobile = screenWidth < 600 || screenHeight > screenWidth

    val uiState by viewModel.uiState.collectAsState()
    // Default to mosque dashboard for both mobile and desktop
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
            if (isMobile) {
                // Mobile Layout - Vertical with marquee pinned to bottom
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    // Main content that takes up space above marquee
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Takes remaining space above marquee
                    ) {
                        // Header with logo
                        Header()

                        // Current Time Display
                        CurrentTimeDisplay(uiState.prayerData)

                        // Current Date Display
                        CurrentDateDisplay(uiState.prayerData)

                        // Mosque Image Slider - same size as desktop
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.4f) // Takes 40% of available space
                                .border(width = 0.5.dp, Color.White)
                        ) {
                            ImageSlider(
                                images = mosqueImages,
                                currentIndex = currentImageIndex,
                                onIndexChange = viewModel::setCurrentImageIndex,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Quote Box - taller on mobile
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        ) {
                            val quote by viewModel.quoteText.collectAsState()
                            QuoteBox(quote = quote)
                        }

                        // Prayer Times Grid - 2 rows on mobile
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            PrayerTimesGrid(
                                timings = uiState.prayerData?.timings,
                                isMobile = true
                            )
                        }

                        // Countdown - positioned differently on mobile
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            NextPrayerCountdown(
                                timings = uiState.prayerData?.timings
                            )
                        }
                    }

                    // MarqueeText - pinned to the bottom
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val text by viewModel.marqueeText.collectAsState()
                        MarqueeText(text = text)
                    }
                }
            } else {
                // TV/Tablet Layout - Horizontal (original layout)
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

                // Countdown overlay - only for TV/tablet
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp + 90.dp, end = 200.dp + 100.dp)
                ) {
                    NextPrayerCountdown(
                        timings = uiState.prayerData?.timings
                    )
                }

                // Prayer Times Grid - only for TV/tablet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                ) {
                    PrayerTimesGrid(
                        timings = uiState.prayerData?.timings,
                        isMobile = false
                    )
                }

                // MarqueeText - Stays at the very bottom for TV/tablet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    val text by viewModel.marqueeText.collectAsState()
                    MarqueeText(text = text)
                }
            }

            // Admin button - positioned differently for mobile vs TV
            IconButton(
                onClick = { showAdminDashboard = true },
                modifier = Modifier
                    .align(if (isMobile) Alignment.TopEnd else Alignment.TopStart)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(
                        Color.White.copy(alpha = 0.7f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Pengaturan Admin",
                    tint = emeraldGreen
                )
            }
        }
    }
}
