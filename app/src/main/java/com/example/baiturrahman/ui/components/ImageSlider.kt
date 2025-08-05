package com.example.baiturrahman.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.baiturrahman.R
import kotlinx.coroutines.launch
import com.example.baiturrahman.ui.components.SupabaseImage // Assuming SupabaseImage is defined in this package or imported

@Composable
fun ImageSlider(
    images: List<String>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { maxOf(1, images.size) }
    )
    val coroutineScope = rememberCoroutineScope()

    // Sync external index with pager state
    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex && images.isNotEmpty()) {
            pagerState.animateScrollToPage(currentIndex)
        }
    }

    // Sync pager state with external index
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentIndex && images.isNotEmpty()) {
            onIndexChange(pagerState.currentPage)
        }
    }

    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (images.isEmpty()) {
                    // Default image if no images are available
                    Image(
                        painter = painterResource(id = R.drawable.mosque),
                        contentDescription = "Gambar Masjid Default",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Use SupabaseImage for cloud images
                    SupabaseImage(
                        imageUrl = images[page],
                        contentDescription = "Mosque Image ${page + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        fallbackResourceId = R.drawable.mosque
                    )
                }
            }
        }

        // Indicators
        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(images.size) { index ->
                    val isSelected = index == pagerState.currentPage
                    val size by animateFloatAsState(
                        targetValue = if (isSelected) 10f else 8f,
                        label = "ukuran indikator"
                    )

                    Box(
                        modifier = Modifier
                            .size(size.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                            )
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                    )
                }
            }
        }
    }
}
