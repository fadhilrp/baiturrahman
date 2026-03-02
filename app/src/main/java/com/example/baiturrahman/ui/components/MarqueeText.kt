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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
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
    lines: List<String>,
) {
    val c = LocalAppColors.current
    val localDensity = LocalDensity.current
    val widthDp = with(localDensity) { LocalWindowInfo.current.containerSize.width.toDp() }
    val isMobile = widthDp < 600.dp

    var currentLineIndex by remember { mutableIntStateOf(0) }
    val currentLine = if (lines.isEmpty()) "" else lines[currentLineIndex % lines.size]

    var offset by remember { mutableFloatStateOf(0f) }
    // Large default prevents a premature reset before onSizeChanged fires for long text
    var textWidthDp by remember { mutableIntStateOf(4000) }

    // Target ~250 px/s regardless of screen density (14 ms per tick)
    val speed = 3.5f / localDensity.density

    // Always start off-screen to the right, adapting to any screen size (TV, tablet, phone)
    val resetPosition = widthDp.value + 100f

    LaunchedEffect(lines) {
        // Reset to first line whenever the list changes
        currentLineIndex = 0
        offset = resetPosition
        while (true) {
            delay(14)
            offset -= speed
            if (offset < -textWidthDp.toFloat()) {
                // Advance to next line (cycles back to 0 at the end)
                currentLineIndex = (currentLineIndex + 1) % lines.size.coerceAtLeast(1)
                offset = resetPosition
            }
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
                text = currentLine,
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
                    .wrapContentWidth(unbounded = true)
                    .onSizeChanged { size ->
                        textWidthDp = with(localDensity) { size.width.toDp() }.value.toInt() + 50
                    }
            )
        }
    }
}
