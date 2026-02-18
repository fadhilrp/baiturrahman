package com.example.baiturrahman.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baiturrahman.ui.theme.EmeraldMuted
import com.example.baiturrahman.ui.theme.TextPrimary
import com.example.baiturrahman.ui.theme.TextSecondary

@Composable
fun QuoteBox(
    quote: String = "\"Lorem ipsum dolor sit amet.\""
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMobile = screenWidth < 600

    val padding = if (isMobile) 12.dp else 16.dp

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        cornerRadius = 8.dp,
        backgroundColor = EmeraldMuted
    ) {
        // Decorative quote mark
        Text(
            text = "\u201C",
            fontSize = 72.sp,
            color = TextSecondary.copy(alpha = 0.2f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = quote,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
