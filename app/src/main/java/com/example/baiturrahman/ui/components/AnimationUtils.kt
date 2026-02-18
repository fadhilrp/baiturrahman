package com.example.baiturrahman.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import com.example.baiturrahman.ui.theme.EmeraldGreen

const val STANDARD_DURATION = 300

fun <T> standardTween() = tween<T>(durationMillis = STANDARD_DURATION)

fun Modifier.focusScale(): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(durationMillis = STANDARD_DURATION),
        label = "focus_scale"
    )

    this
        .onFocusChanged { isFocused = it.isFocused }
        .focusable()
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

fun Modifier.softGlow(): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.3f else 0f,
        animationSpec = tween(durationMillis = STANDARD_DURATION),
        label = "glow_alpha"
    )

    this
        .onFocusChanged { isFocused = it.isFocused }
        .focusable()
        .drawBehind {
            if (glowAlpha > 0f) {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        asFrameworkPaint().apply {
                            color = EmeraldGreen.copy(alpha = glowAlpha).toArgb()
                            setShadowLayer(16f, 0f, 0f, color)
                        }
                    }
                    canvas.drawRect(
                        0f, 0f, size.width, size.height, paint
                    )
                }
            }
        }
}
