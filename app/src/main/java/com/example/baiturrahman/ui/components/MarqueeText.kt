package com.example.baiturrahman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun MarqueeText(
    modifier: Modifier = Modifier,
    text: String = "Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text",
) {
    var offset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(text) {
        while(true) {
            delay(50)
            offset -= 2f
            if (offset < -2000f) offset = 800f // Reset position when text moves off screen
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .height(40.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 20.sp,
            textAlign = TextAlign.Left,
            modifier = Modifier
                .offset(x = offset.dp)
                .padding(vertical = 8.dp)
        )
    }
}

