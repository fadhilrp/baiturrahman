package com.example.baiturrahman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.baiturrahman.ui.theme.DarkBackground
import com.example.baiturrahman.ui.theme.DarkSurface
import com.example.baiturrahman.ui.theme.EmeraldGreen
import com.example.baiturrahman.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun MarqueeText(
    text: String = "Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling",
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMobile = screenWidth < 600

    var offset by remember { mutableFloatStateOf(0f) }

    val baseSpeed = 1f
    val speedMultiplier = maxOf(0.8f, minOf(1.5f, 100f / text.length.toFloat()))
    val speed = baseSpeed * speedMultiplier

    val resetPosition = if (isMobile) screenWidth.toFloat() + 100f else 980f

    LaunchedEffect(text) {
        while(true) {
            delay(14)
            offset -= speed
            if (offset < -800f) offset = resetPosition
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Thin emerald top border
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            thickness = 1.dp,
            color = EmeraldGreen.copy(alpha = 0.3f)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(DarkBackground, DarkSurface, DarkBackground)
                    )
                )
                .height(if (isMobile) 35.dp else 40.dp)
        ) {
            Text(
                text = text,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Left,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                softWrap = false,
                modifier = Modifier
                    .graphicsLayer { translationX = offset * density }
                    .padding(vertical = if (isMobile) 6.dp else 8.dp)
                    .wrapContentWidth()
            )
        }
    }
}
