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
import com.example.baiturrahman.ui.viewmodel.AuthViewModel
import com.example.baiturrahman.ui.viewmodel.MosqueDashboardViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MosqueDashboard(
    viewModel: MosqueDashboardViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val isMobile = screenWidth < 600 || screenHeight > screenWidth

    val uiState by viewModel.uiState.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    var showAdminDashboard by remember { mutableStateOf(false) }
    var showLoginScreen by remember { mutableStateOf(false) }
    var showRegisterScreen by remember { mutableStateOf(false) }
    val mosqueImages by viewModel.mosqueImages.collectAsState()
    val currentImageIndex by viewModel.currentImageIndex.collectAsState()

    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }

    // After login: close login screen and open admin dashboard
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && showLoginScreen) {
            showLoginScreen = false
            showRegisterScreen = false
            showAdminDashboard = true
        }
        // After logout: close admin dashboard
        if (!isLoggedIn) {
            showAdminDashboard = false
        }
    }

    if (showAdminDashboard) {
        AdminDashboard(
            viewModel = viewModel,
            authViewModel = authViewModel,
            onClose = { showAdminDashboard = false }
        )
    } else if (showLoginScreen) {
        if (showRegisterScreen) {
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { showRegisterScreen = false },
                onBack = { showRegisterScreen = false }
            )
        } else {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = { showRegisterScreen = true },
                onBack = { showLoginScreen = false }
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
            AnimatedVisibility(visible = contentVisible, enter = fadeIn()) {
                if (isMobile) {
                    Column(
                        modifier = Modifier.fillMaxSize().background(DarkBackground)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            Header()
                            Spacer(Modifier.height(8.dp))
                            CurrentTimeDisplay(uiState.prayerData)
                            Spacer(Modifier.height(8.dp))
                            CurrentDateDisplay(uiState.prayerData)
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().weight(0.4f)) {
                                ImageSlider(
                                    images = mosqueImages,
                                    currentIndex = currentImageIndex,
                                    onIndexChange = viewModel::setCurrentImageIndex,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                                val quote by viewModel.quoteText.collectAsState()
                                QuoteBox(quote = quote)
                            }
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                PrayerTimesGrid(timings = uiState.prayerData?.timings, isMobile = true)
                            }
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                NextPrayerCountdown(timings = uiState.prayerData?.timings)
                            }
                        }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val text by viewModel.marqueeText.collectAsState()
                            MarqueeText(text = text)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.weight(1f).fillMaxWidth().background(DarkBackground)
                            ) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    ImageSlider(
                                        images = mosqueImages,
                                        currentIndex = currentImageIndex,
                                        onIndexChange = viewModel::setCurrentImageIndex,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Box(modifier = Modifier.weight(0.5f).fillMaxHeight()) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Spacer(Modifier.height(8.dp))
                                            Header()
                                            Spacer(Modifier.height(12.dp))
                                            CurrentTimeDisplay(uiState.prayerData)
                                            Spacer(Modifier.height(8.dp))
                                            CurrentDateDisplay(uiState.prayerData)
                                            Spacer(Modifier.height(12.dp))
                                            val quote by viewModel.quoteText.collectAsState()
                                            QuoteBox(quote = quote)
                                        }
                                    }
                                }
                            }
                            PrayerTimesGrid(timings = uiState.prayerData?.timings, isMobile = false)
                            val text by viewModel.marqueeText.collectAsState()
                            MarqueeText(text = text)
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth(fraction = 0.667f)
                                .padding(bottom = 130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            NextPrayerCountdown(timings = uiState.prayerData?.timings)
                        }
                    }
                }
            }

            // Settings button
            IconButton(
                onClick = { if (isLoggedIn) showAdminDashboard = true else showLoginScreen = true },
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
