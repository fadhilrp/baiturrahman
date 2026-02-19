package com.example.baiturrahman.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable

@Composable
fun mosqueTextFieldColors(): TextFieldColors {
    val c = LocalAppColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = EmeraldGreen,
        unfocusedBorderColor = c.glassBorder,
        focusedLabelColor = EmeraldGreen,
        unfocusedLabelColor = c.textSecondary,
        cursorColor = EmeraldGreen,
        focusedTextColor = c.textPrimary,
        unfocusedTextColor = c.textPrimary
    )
}
