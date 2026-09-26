package com.clawd.mobile.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable

@Composable
fun connectionFieldColors(): TextFieldColors {
    val colors = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.onSurface,
        unfocusedTextColor = colors.onSurface,
        disabledTextColor = colors.onSurface.copy(alpha = 0.6f),
        focusedContainerColor = colors.surface,
        unfocusedContainerColor = colors.surface,
        disabledContainerColor = colors.surfaceVariant,
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.outline,
        disabledBorderColor = colors.outline.copy(alpha = 0.6f),
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = colors.onSurfaceVariant,
        disabledLabelColor = colors.onSurfaceVariant,
        focusedPlaceholderColor = colors.onSurfaceVariant,
        unfocusedPlaceholderColor = colors.onSurfaceVariant,
        disabledPlaceholderColor = colors.onSurfaceVariant.copy(alpha = 0.6f),
        cursorColor = colors.primary,
    )
}
