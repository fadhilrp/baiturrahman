package com.example.baiturrahman.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.baiturrahman.R
import com.example.baiturrahman.ui.theme.DarkBackground
import com.example.baiturrahman.ui.theme.EmeraldGreen
import com.example.baiturrahman.ui.theme.Foreground

@Composable
fun ImageSlider(
    images: List<String>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var internalIndex by remember { mutableIntStateOf(currentIndex) }

    // Sync with external index changes
    LaunchedEffect(currentIndex) {
        if (internalIndex != currentIndex) {
            internalIndex = currentIndex
        }
    }

    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp))
    ) {
        // Crossfade images
        if (images.isEmpty()) {
            Image(
                painter = painterResource(id = R.drawable.mosque),
                contentDescription = "Gambar Masjid Default",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            images.forEachIndexed { index, imageUrl ->
                val alpha by animateFloatAsState(
                    targetValue = if (index == internalIndex) 1f else 0f,
                    animationSpec = tween(durationMillis = 1000),
                    label = "crossfade_$index"
                )
                if (alpha > 0f) {
                    SupabaseImage(
                        imageUrl = imageUrl,
                        contentDescription = "Mosque Image ${index + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { this.alpha = alpha },
                        contentScale = ContentScale.Crop,
                        fallbackResourceId = null
                    )
                }
            }
        }

        // Bottom gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            DarkBackground.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        // Dot indicators at bottom
        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(images.size) { index ->
                    val isSelected = index == internalIndex
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = tween(durationMillis = 300),
                        label = "dot_width"
                    )

                    Box(
                        modifier = Modifier
                            .width(width)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) EmeraldGreen else Foreground.copy(alpha = 0.3f)
                            )
                            .clickable {
                                internalIndex = index
                                onIndexChange(index)
                            }
                    )
                }
            }
        }
    }
}
