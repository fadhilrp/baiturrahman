package com.example.baiturrahman.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.baiturrahman.ui.components.*
import com.example.baiturrahman.ui.theme.DarkBackground
import com.example.baiturrahman.ui.theme.EmeraldGreen
import com.example.baiturrahman.ui.theme.GlassWhite
import com.example.baiturrahman.ui.viewmodel.MosqueDashboardViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MosqueDashboard(
    viewModel: MosqueDashboardViewModel = koinViewModel()
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    val isMobile = screenWidth < 600 || screenHeight > screenWidth

    val uiState by viewModel.uiState.collectAsState()
    var showAdminDashboard by remember { mutableStateOf(false) }
    val mosqueImages by viewModel.mosqueImages.collectAsState()
    val currentImageIndex by viewModel.currentImageIndex.collectAsState()

    // Fade-in on initial load
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }

    if (showAdminDashboard) {
        AdminDashboard(
            viewModel = viewModel,
            onClose = { showAdminDashboard = false }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn()
            ) {
                if (isMobile) {
                    // Mobile Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBackground)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Header()

                            Spacer(modifier = Modifier.height(8.dp))

                            CurrentTimeDisplay(uiState.prayerData)

                            Spacer(modifier = Modifier.height(8.dp))

                            CurrentDateDisplay(uiState.prayerData)

                            Spacer(modifier = Modifier.height(8.dp))

                            // Image Slider
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.4f)
                            ) {
                                ImageSlider(
                                    images = mosqueImages,
                                    currentIndex = currentImageIndex,
                                    onIndexChange = viewModel::setCurrentImageIndex,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quote Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                            ) {
                                val quote by viewModel.quoteText.collectAsState()
                                QuoteBox(quote = quote)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Prayer Times Grid
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                PrayerTimesGrid(
                                    timings = uiState.prayerData?.timings,
                                    isMobile = true
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Countdown
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

                        // MarqueeText pinned to bottom
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val text by viewModel.marqueeText.collectAsState()
                            MarqueeText(text = text)
                        }
                    }
                } else {
                    // TV/Tablet Layout - Box for countdown overlay, Column for flow layout
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main flow layout: image+info row, then grid, then marquee
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(DarkBackground)
                            ) {
                                // Left side - Image Slider
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
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
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Header()
                                            Spacer(modifier = Modifier.height(12.dp))
                                            CurrentTimeDisplay(uiState.prayerData)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            CurrentDateDisplay(uiState.prayerData)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            val quote by viewModel.quoteText.collectAsState()
                                            QuoteBox(quote = quote)
                                        }
                                    }
                                }
                            }

                            // Prayer Times Grid - flows below the image+info row
                            PrayerTimesGrid(
                                timings = uiState.prayerData?.timings,
                                isMobile = false
                            )

                            // MarqueeText at the very bottom
                            val text by viewModel.marqueeText.collectAsState()
                            MarqueeText(text = text)
                        }

                        // Countdown overlay - centered over the image area
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth(fraction = 0.667f)
                                .padding(bottom = 130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            NextPrayerCountdown(
                                timings = uiState.prayerData?.timings
                            )
                        }
                    }
                }
            }

            // Settings button
            IconButton(
                onClick = { showAdminDashboard = true },
                modifier = Modifier
                    .align(if (isMobile) Alignment.TopEnd else Alignment.TopStart)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(GlassWhite, shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Pengaturan Admin",
                    tint = EmeraldGreen
                )
            }
        }
    }
}
