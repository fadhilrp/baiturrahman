package com.example.baiturrahman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import com.example.baiturrahman.ui.theme.EmeraldGreen
import com.example.baiturrahman.ui.theme.LocalAppColors

@Composable
fun QuoteBox(
    quote: String = "\"Sesungguhnya shalat itu mencegah dari perbuatan-perbuatan keji dan mungkar.\" (QS. Al-Ankabut: 45)",
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
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
            .background(c.secondary.copy(alpha = if (c.isDark) 0.55f else 1f), shape)
            .border(1.dp, c.border.copy(alpha = if (c.isDark) 0.55f else 1f), shape)
            .padding(
                horizontal = 16.dp,
                vertical = if (isMobile) 14.dp else 12.dp
            ),
        verticalArrangement = Arrangement.Center
    ) {
        // Quote text
        Text(
            text = quoteText,
            color = c.foreground,
            fontStyle = FontStyle.Italic,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 14.sp,
                lineHeight = 18.sp
            ),
            textAlign = TextAlign.Justify,
            modifier = Modifier.fillMaxWidth()
        )

        // Attribution
        if (attribution != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = attribution,
                color = c.mutedForeground,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
