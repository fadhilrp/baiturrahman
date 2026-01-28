package com.example.baiturrahman.ui.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
        // Show fallback image if no URL provided
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

            // Log errors for debugging
            if (state is AsyncImagePainter.State.Error) {
                Log.e("SupabaseImage", "═══════════════════════════════════════")
                Log.e("SupabaseImage", "❌ FAILED TO LOAD IMAGE")
                Log.e("SupabaseImage", "URL: $imageUrl")
                Log.e("SupabaseImage", "Error: ${state.result.throwable.message}")
                Log.e("SupabaseImage", "Exception type: ${state.result.throwable.javaClass.simpleName}")
                Log.e("SupabaseImage", "═══════════════════════════════════════", state.result.throwable)
            }
        }
    )

    Box(modifier = modifier) {
        if (hasError && fallbackResourceId != null) {
            // Show fallback image on error
            Image(
                painter = painterResource(id = fallbackResourceId),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            // Show the loaded image
            Image(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }

        // Show loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center)
            )
        }
    }
}
