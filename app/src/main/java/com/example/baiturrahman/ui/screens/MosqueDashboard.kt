package com.example.baiturrahman.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.baiturrahman.R
import com.example.baiturrahman.ui.components.*
import com.example.baiturrahman.ui.theme.EmeraldGreen
import com.example.baiturrahman.ui.theme.LocalAppColors
import com.example.baiturrahman.ui.viewmodel.AuthViewModel
import com.example.baiturrahman.ui.viewmodel.MosqueDashboardViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MosqueDashboard(
    viewModel: MosqueDashboardViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val c = LocalAppColors.current
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
        Box(modifier = Modifier.fillMaxSize().background(c.background)) {
            // Islamic pattern background (tiled)
            val pattern = ImageBitmap.imageResource(R.drawable.islamic_pattern)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val patternWidth = 400
                val patternHeight = 400
                val cols = (size.width / patternWidth).toInt() + 1
                val rows = (size.height / patternHeight).toInt() + 1
                for (row in 0..rows) {
                    for (col in 0..cols) {
                        drawImage(
                            image = pattern,
                            dstOffset = IntOffset(col * patternWidth, row * patternHeight),
                            dstSize = IntSize(patternWidth, patternHeight),
                            alpha = 0.15f
                        )
                    }
                }
            }

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                c.background.copy(alpha = 0.95f),
                                c.background.copy(alpha = 0.90f),
                                c.background.copy(alpha = 0.95f),
                            )
                        )
                    )
            )

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn()
            ) {
                if (isMobile) {
                    MobileDashboardLayout(
                        viewModel = viewModel,
                        uiState = uiState,
                        mosqueImages = mosqueImages,
                        currentImageIndex = currentImageIndex,
                    )
                } else {
                    TvDashboardLayout(
                        viewModel = viewModel,
                        uiState = uiState,
                        mosqueImages = mosqueImages,
                        currentImageIndex = currentImageIndex,
                    )
                }
            }

            // Settings button
            IconButton(
                onClick = { if (isLoggedIn) showAdminDashboard = true else showLoginScreen = true },
                modifier = Modifier
                    .align(if (isMobile) Alignment.TopEnd else Alignment.TopStart)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(c.glassWhite, shape = CircleShape)
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

@Composable
private fun TvDashboardLayout(
    viewModel: MosqueDashboardViewModel,
    uiState: MosqueDashboardViewModel.MosqueDashboardUiState,
    mosqueImages: List<String>,
    currentImageIndex: Int,
) {
    val c = LocalAppColors.current
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top section: Image slider (3) + Info panel (2)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left: Image slider with overlays
                Box(
                    modifier = Modifier
                        .weight(4f)
                        .fillMaxHeight()
                ) {
                    ImageSlider(
                        images = mosqueImages,
                        currentIndex = currentImageIndex,
                        onIndexChange = viewModel::setCurrentImageIndex,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Next prayer indicator overlay at bottom center of slider
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter),
                    ) {
                        NextPrayerCountdown(
                            timings = uiState.prayerData?.timings
                        )
                    }
                }

                // Right: Info panel
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mosque Identity
                    Header()

                    // Clock + Date card
                    val clockDateShape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(clockDateShape)
                            .background(c.secondary.copy(alpha = 0.4f), clockDateShape)
                            .border(1.dp, c.border.copy(alpha = 0.4f), clockDateShape)
                            .padding(top = 2.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CurrentTimeDisplay()
                            Spacer(modifier = Modifier.height(4.dp))
                            CurrentDateDisplay(uiState.prayerData)
                        }
                    }

                    // Quote - fills remaining vertical space
                    val quote by viewModel.quoteText.collectAsState()
                    QuoteBox(
                        quote = quote,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Bottom: Prayer times grid (8 columns)
            PrayerTimesGrid(
                timings = uiState.prayerData?.timings,
                isMobile = false
            )
        }

        // Marquee pinned to very bottom, outside padding
        val text by viewModel.marqueeText.collectAsState()
        MarqueeText(text = text)
    }
}

@Composable
private fun MobileDashboardLayout(
    viewModel: MosqueDashboardViewModel,
    uiState: MosqueDashboardViewModel.MosqueDashboardUiState,
    mosqueImages: List<String>,
    currentImageIndex: Int,
) {
    val c = LocalAppColors.current
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mosque Identity
            Header()

            // Clock + Date
            val clockDateShape = RoundedCornerShape(12.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(clockDateShape)
                    .background(c.secondary.copy(alpha = 0.4f), clockDateShape)
                    .border(1.dp, c.border.copy(alpha = 0.4f), clockDateShape)
                    .padding(top = 4.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CurrentTimeDisplay()
                Spacer(modifier = Modifier.height(8.dp))
                CurrentDateDisplay(uiState.prayerData)
            }

            // Image Slider with next prayer overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
            ) {
                ImageSlider(
                    images = mosqueImages,
                    currentIndex = currentImageIndex,
                    onIndexChange = viewModel::setCurrentImageIndex,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                ) {
                    NextPrayerCountdown(
                        timings = uiState.prayerData?.timings
                    )
                }
            }

            // Quote Box
            val quote by viewModel.quoteText.collectAsState()
            QuoteBox(quote = quote)

            // Prayer Times Grid
            PrayerTimesGrid(
                timings = uiState.prayerData?.timings,
                isMobile = true
            )
        }

        // MarqueeText pinned to bottom
        val text by viewModel.marqueeText.collectAsState()
        MarqueeText(text = text)
    }
}
