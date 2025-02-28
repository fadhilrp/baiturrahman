package com.example.baiturrahman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baiturrahman.ui.theme.emeraldGreen

@Composable
fun NextPrayerCountdown(
    prayerName: String,
    timeRemaining: String
) {
    Box(
        modifier = Modifier
            .clip(
                RoundedCornerShape(
                    topStart = 100.dp,
                    topEnd = 100.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            )
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$prayerName $timeRemaining",
            color = emeraldGreen,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}