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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baiturrahman.ui.theme.emeraldGreen

@Composable
fun QuoteBox(
    quote: String = "\"Lorem ipsum dolor sit amet.\""
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMobile = screenWidth < 600

    // Responsive font size and padding
    val fontSize = if (isMobile) 16.sp else 20.sp
    val padding = if (isMobile) 12.dp else 16.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(emeraldGreen)
            .padding(padding)
    ) {
        Text(
            text = quote,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = fontSize,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
