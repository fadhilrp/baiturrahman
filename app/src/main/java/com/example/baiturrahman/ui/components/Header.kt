package com.example.baiturrahman.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baiturrahman.R
import com.example.baiturrahman.ui.viewmodel.MosqueDashboardViewModel
import com.example.baiturrahman.ui.components.SupabaseImage // Assuming SupabaseImage is defined in this package
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
    val context = LocalContext.current

    // Responsive sizes
    val logoSize = if (isMobile) 48.dp else 64.dp
    val nameTextSize = if (isMobile) 18.sp else 22.sp
    val locationTextSize = if (isMobile) 14.sp else 17.sp
    val padding = if (isMobile) 8.dp else 15.dp
    val spacerWidth = if (isMobile) 8.dp else 16.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo image - use Supabase image if available, otherwise use default
        if (logoImage != null) {
            SupabaseImage(
                imageUrl = logoImage,
                contentDescription = "Mosque Logo",
                modifier = Modifier.size(64.dp),
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
                fontSize = nameTextSize,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = mosqueLocation,
                fontSize = locationTextSize
            )
        }
    }
}
