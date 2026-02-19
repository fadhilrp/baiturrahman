package com.example.baiturrahman.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable

@Composable
fun mosqueTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = EmeraldGreen,
    unfocusedBorderColor = GlassBorder,
    focusedLabelColor = EmeraldGreen,
    unfocusedLabelColor = TextSecondary,
    cursorColor = EmeraldGreen,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
