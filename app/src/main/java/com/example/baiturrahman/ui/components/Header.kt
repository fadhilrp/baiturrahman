package com.example.baiturrahman.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.baiturrahman.R
import com.example.baiturrahman.ui.theme.TextPrimary
import com.example.baiturrahman.ui.theme.TextSecondary
import com.example.baiturrahman.ui.viewmodel.MosqueDashboardViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun Header(
    viewModel: MosqueDashboardViewModel = koinViewModel()
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMobile = screenWidth < 600

    val mosqueName by viewModel.mosqueName.collectAsState()
    val mosqueLocation by viewModel.mosqueLocation.collectAsState()
    val logoImage by viewModel.logoImage.collectAsState()

    val logoSize = if (isMobile) 48.dp else 64.dp
    val padding = if (isMobile) 8.dp else 15.dp
    val spacerWidth = if (isMobile) 8.dp else 16.dp

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = padding, vertical = padding / 2),
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (logoImage != null) {
                SupabaseImage(
                    imageUrl = logoImage,
                    contentDescription = "Mosque Logo",
                    modifier = Modifier.size(logoSize),
                    fallbackResourceId = R.drawable.logo2
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.logo2),
                    contentDescription = "Logo Masjid",
                    modifier = Modifier.size(logoSize)
                )
            }

            Spacer(modifier = Modifier.width(spacerWidth))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = mosqueName,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = mosqueLocation,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
