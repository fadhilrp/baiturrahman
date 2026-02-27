package com.example.baiturrahman.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.baiturrahman.ui.theme.EmeraldGreen
import com.example.baiturrahman.ui.theme.EmeraldGlow40

@Composable
fun GlowText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    glowColor: Color = EmeraldGlow40,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    Box(modifier = modifier) {
        // Shadow/glow layer behind
        Text(
            text = text,
            style = style,
            color = glowColor,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = maxLines,
        )
        // Main text layer on top
        Text(
            text = text,
            style = style,
            color = color,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = maxLines,
        )
    }
}

@Composable
fun pulsingAlpha(
    minAlpha: Float = 0.7f,
    maxAlpha: Float = 1.0f,
    durationMillis: Int = 3000
): Float {
    val transition = rememberInfiniteTransition(label = "pulse_glow")
    val alpha by transition.animateFloat(
        initialValue = maxAlpha,
        targetValue = minAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    return alpha
}

fun Modifier.activePrayerGlow(color: Color = EmeraldGreen): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            asFrameworkPaint().apply {
                this.color = color.copy(alpha = 0.2f).toArgb()
                setShadowLayer(40f, 0f, 0f, color.copy(alpha = 0.2f).toArgb())
            }
        }
        canvas.drawRect(0f, 0f, size.width, size.height, paint)
    }
}
