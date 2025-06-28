package com.example.baiturrahman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun MarqueeText(
    text: String = "Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling",
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMobile = screenWidth < 600

    var offset by remember { mutableFloatStateOf(0f) }

    // Calculate speed based on text length
    // Shorter text should move faster to maintain visual interest
    val baseSpeed = 1f // Base speed (pixels per frame)
    val speedMultiplier = maxOf(0.8f, minOf(1.5f, 100f / text.length.toFloat()))
    val speed = baseSpeed * speedMultiplier

    // Calculate reset position based on screen size
    val resetPosition = if (isMobile) screenWidth.toFloat() + 100f else 980f

    LaunchedEffect(text) {
        while(true) {
            delay(14) // ~60fps
            offset -= speed
            if (offset < -800f) offset = resetPosition // Reset position when text moves off screen
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .height(if (isMobile) 35.dp else 40.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = if (isMobile) 16.sp else 20.sp,
            textAlign = TextAlign.Left,
            maxLines = 1, // Force single line
            overflow = TextOverflow.Visible, // Allow text to extend beyond bounds
            softWrap = false, // Prevent text wrapping
            modifier = Modifier
                .offset(x = offset.dp)
                .padding(vertical = if (isMobile) 6.dp else 8.dp)
                .wrapContentWidth() // Allow text to extend beyond container width
        )
    }
}
