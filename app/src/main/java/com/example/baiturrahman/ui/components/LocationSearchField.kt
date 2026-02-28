package com.example.baiturrahman.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.baiturrahman.ui.theme.EmeraldGreen

@Composable
fun LocationSearchField(
    value: String,
    suggestions: List<String>,
    isSearching: Boolean,
    isGettingGps: Boolean,
    isError: Boolean,
    onValueChange: (String) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onGpsRequested: () -> Unit,
    onClearSuggestions: () -> Unit,
    colors: TextFieldColors,
    modifier: Modifier = Modifier,
    supportingText: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    var dropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(suggestions) {
        dropdownExpanded = suggestions.isNotEmpty()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) onGpsRequested()
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { new ->
                onValueChange(new)
                onSearchQueryChanged(new)
            },
            label = { Text("Alamat Waktu Sholat") },
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            isError = isError,
            supportingText = supportingText,
            singleLine = true,
            trailingIcon = {
                when {
                    isGettingGps || isSearching -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = EmeraldGreen
                    )
                    else -> IconButton(onClick = {
                        val fineGranted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val coarseGranted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (fineGranted || coarseGranted) {
                            onGpsRequested()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Gunakan GPS",
                            tint = EmeraldGreen
                        )
                    }
                }
            }
        )

        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = {
                dropdownExpanded = false
                onClearSuggestions()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = suggestion,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onSuggestionSelected(suggestion)
                        dropdownExpanded = false
                        onClearSuggestions()
                    }
                )
            }
        }
    }
}
