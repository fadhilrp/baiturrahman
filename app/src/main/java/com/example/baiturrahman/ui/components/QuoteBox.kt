package com.example.baiturrahman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baiturrahman.ui.theme.emeraldGreen

@Composable
fun QuoteBox(
    quote: String = "\"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec vel egestas dolor, nec dignissim metus.\""
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(emeraldGreen)
            .padding(16.dp)
    ) {
        Text(
            text = quote,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

