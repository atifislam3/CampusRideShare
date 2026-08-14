package com.atif.campusrideshare.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

// Primary & Secondary (The "Safety & Trust" Palette)
val SafetyBlue = Color(0xFF1E88E5)
val ActiveGreen = Color(0xFF2E7D32)

// Accent & Neutral (The "City Transit" Palette)
val DeepCharcoal = Color(0xFF212121)
val TaxiAmber = Color(0xFFFFC107)

// RideShare Specific Colors (matching markers)
val CarBlue = SafetyBlue
val BikeGreen = ActiveGreen

/**
 * Deterministically derives a color from a name's hashCode.
 * Used exclusively for InitialsAvatar background colors.
 */
fun colorFromName(name: String): Color {
    val palette = listOf(
        Color(0xFFEF5350), // Red
        Color(0xFFEC407A), // Pink
        Color(0xFFAB47BC), // Purple
        Color(0xFF7E57C2), // Deep Purple
        Color(0xFF5C6BC0), // Indigo
        Color(0xFF42A5F5), // Blue
        Color(0xFF29B6F6), // Light Blue
        Color(0xFF26C6DA), // Cyan
        Color(0xFF26A69A), // Teal
        Color(0xFF66BB6A), // Green
        Color(0xFF9CCC65), // Light Green
        Color(0xFFD4E157), // Lime
        Color(0xFFFFCA28), // Amber
        Color(0xFFFFA726), // Orange
        Color(0xFFFF7043), // Deep Orange
        Color(0xFF8D6E63), // Brown
        Color(0xFF78909C)  // Blue Grey
    )
    if (name.isEmpty()) return palette[0]
    val index = abs(name.hashCode()) % palette.size
    return palette[index]
}
