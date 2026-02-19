package com.example.baiturrahman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baiturrahman.ui.theme.Border
import com.example.baiturrahman.ui.theme.EmeraldGreen
import com.example.baiturrahman.ui.theme.Foreground
import com.example.baiturrahman.ui.theme.MutedForeground
import com.example.baiturrahman.ui.theme.Secondary

@Composable
fun QuoteBox(
    quote: String = "\"Lorem ipsum dolor sit amet.\"",
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMobile = screenWidth < 600

    // Split quote and attribution if present
    val parts = quote.split(" — ", " - ")
    val quoteText = parts.firstOrNull() ?: quote
    val attribution = if (parts.size > 1) "— ${parts.drop(1).joinToString(" — ")}" else null

    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Secondary.copy(alpha = 0.4f), shape)
            .border(1.dp, Border.copy(alpha = 0.4f), shape)
            .padding(
                horizontal = if (isMobile) 16.dp else 16.dp,
                vertical = if (isMobile) 14.dp else 12.dp
            )
    ) {
        // Opening quote mark
        Text(
            text = "\u201C",
            fontSize = if (isMobile) 24.sp else 24.sp,
            color = EmeraldGreen,
            lineHeight = if (isMobile) 24.sp else 24.sp,
        )

        // Quote text
        Text(
            text = quoteText,
            color = Foreground.copy(alpha = 0.8f),
            fontStyle = FontStyle.Italic,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 24.sp
            ),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // Attribution
        if (attribution != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = attribution,
                color = MutedForeground,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
