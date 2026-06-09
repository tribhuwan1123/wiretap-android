package com.wiretap.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NetworkColor = Color(0xFF2196F3)
val BleColor = Color(0xFF9C27B0)
val NfcColor = Color(0xFF4CAF50)
val ErrorColor = Color(0xFFF44336)
val SuccessColor = Color(0xFF4CAF50)
val WarningColor = Color(0xFFFF9800)

@Composable
fun WireTapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6650A4),
            secondary = Color(0xFF625B71),
            tertiary = Color(0xFF7D5260),
            surface = Color(0xFF1C1B1F),
            background = Color(0xFF1C1B1F),
            onBackground = Color(0xFFE6E1E5),
            onSurface = Color(0xFFE6E1E5)
        ),
        content = content
    )
}

fun statusColor(statusCode: Int?): Color = when {
    statusCode == null -> Color.Gray
    statusCode in 200..299 -> SuccessColor
    statusCode in 300..399 -> WarningColor
    statusCode >= 400 -> ErrorColor
    else -> Color.Gray
}
