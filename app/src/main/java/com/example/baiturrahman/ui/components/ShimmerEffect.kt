package com.example.baiturrahman.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.baiturrahman.ui.theme.DarkSurface
import com.example.baiturrahman.ui.theme.DarkSurfaceVariant

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            DarkSurface,
            DarkSurfaceVariant,
            DarkSurface
        ),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Box(
        modifier = modifier.background(shimmerBrush)
    )
}

@Composable
fun PrayerTimesGridSkeleton(
    isMobile: Boolean = false
) {
    if (isMobile) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(4) {
                        ShimmerBox(
                            modifier = Modifier
                                .weight(1f)
                                .height(84.dp)
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(8) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                )
            }
        }
    }
}

@Composable
fun HeaderSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(
            modifier = Modifier
                .size(64.dp)
                .background(DarkSurface, CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            ShimmerBox(
                modifier = Modifier
                    .width(180.dp)
                    .height(20.dp)
                    .background(DarkSurface, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(
                modifier = Modifier
                    .width(120.dp)
                    .height(16.dp)
                    .background(DarkSurface, RoundedCornerShape(4.dp))
            )
        }
    }
}
