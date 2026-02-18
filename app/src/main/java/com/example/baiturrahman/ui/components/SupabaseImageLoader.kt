package com.example.baiturrahman.ui.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.baiturrahman.ui.theme.DarkSurface
import com.example.baiturrahman.ui.theme.TextTertiary

@Composable
fun SupabaseImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackResourceId: Int? = null
) {
    val context = LocalContext.current

    if (imageUrl.isNullOrEmpty()) {
        if (fallbackResourceId != null) {
            Image(
                painter = painterResource(id = fallbackResourceId),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        return
    }

    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        onState = { state ->
            isLoading = state is AsyncImagePainter.State.Loading
            hasError = state is AsyncImagePainter.State.Error

            if (state is AsyncImagePainter.State.Error) {
                Log.e("SupabaseImage", "Failed to load image: $imageUrl", state.result.throwable)
            }
        }
    )

    Box(modifier = modifier) {
        if (hasError && fallbackResourceId != null) {
            Image(
                painter = painterResource(id = fallbackResourceId),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else if (hasError) {
            // Subtle broken-image icon on dark surface
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkSurface, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Image failed to load",
                    tint = TextTertiary,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }

        if (isLoading) {
            ShimmerBox(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
