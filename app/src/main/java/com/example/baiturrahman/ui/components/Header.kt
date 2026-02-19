package com.example.baiturrahman.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baiturrahman.R
import com.example.baiturrahman.ui.theme.LocalAppColors
import com.example.baiturrahman.ui.viewmodel.MosqueDashboardViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun Header(
    viewModel: MosqueDashboardViewModel = koinViewModel()
) {
    val c = LocalAppColors.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMobile = screenWidth < 600

    val mosqueName by viewModel.mosqueName.collectAsState()
    val mosqueLocation by viewModel.mosqueLocation.collectAsState()
    val logoImage by viewModel.logoImage.collectAsState()

    val logoSize = if (isMobile) 44.dp else 56.dp
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.secondary.copy(alpha = if (c.isDark) 0.7f else 1f), shape)
            .border(1.dp, c.border.copy(alpha = if (c.isDark) 0.5f else 1f), shape)
            .padding(horizontal = if (isMobile) 12.dp else 16.dp, vertical = if (isMobile) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        val logoShape = RoundedCornerShape(8.dp)
        if (logoImage != null) {
            SupabaseImage(
                imageUrl = logoImage,
                contentDescription = "Mosque Logo",
                modifier = Modifier
                    .size(logoSize)
                    .clip(logoShape)
                    .background(c.foreground.copy(alpha = 0.9f), logoShape),
                fallbackResourceId = R.drawable.logo2
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.logo2),
                contentDescription = "Logo Masjid",
                modifier = Modifier
                    .size(logoSize)
                    .clip(logoShape)
                    .background(c.foreground.copy(alpha = 0.9f), logoShape)
            )
        }

        Spacer(modifier = Modifier.width(if (isMobile) 10.dp else 16.dp))

        Column {
            Text(
                text = mosqueName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = if (isMobile) 18.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = c.foreground
            )
            Text(
                text = mosqueLocation,
                style = MaterialTheme.typography.bodyMedium,
                color = c.mutedForeground
            )
        }
    }
}
