package com.example.baiturrahman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baiturrahman.ui.theme.LocalAppColors
import com.example.baiturrahman.ui.theme.PlusJakartaSans
import kotlinx.coroutines.delay

@Composable
fun MarqueeText(
    text: String = "Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling",
) {
    val c = LocalAppColors.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMobile = screenWidth < 600

    val localDensity = LocalDensity.current
    var offset by remember { mutableFloatStateOf(0f) }
    var textWidthDp by remember { mutableIntStateOf(800) }

    val baseSpeed = 1f
    val speedMultiplier = maxOf(0.8f, minOf(1.5f, 100f / text.length.toFloat()))
    val speed = baseSpeed * speedMultiplier

    val resetPosition = if (isMobile) screenWidth.toFloat() + 100f else 980f

    LaunchedEffect(text) {
        // Reset scroll position whenever text changes
        offset = resetPosition
        while(true) {
            delay(14)
            offset -= speed
            if (offset < -textWidthDp.toFloat()) offset = resetPosition
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Thin border at top
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            thickness = 1.dp,
            color = c.border.copy(alpha = 0.3f)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.background.copy(alpha = 0.8f))
                .height(if (isMobile) 35.dp else 40.dp)
        ) {
            Text(
                text = text,
                style = TextStyle(
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.sp,
                ),
                color = c.mutedForeground,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                softWrap = false,
                modifier = Modifier
                    .graphicsLayer { translationX = offset * localDensity.density }
                    .padding(vertical = if (isMobile) 8.dp else 10.dp)
                    .wrapContentWidth()
                    .onSizeChanged { size ->
                        textWidthDp = with(localDensity) { size.width.toDp() }.value.toInt() + 50
                    }
            )
        }
    }
}
