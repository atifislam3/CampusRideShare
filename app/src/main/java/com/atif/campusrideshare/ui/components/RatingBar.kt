package com.atif.campusrideshare.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.atif.campusrideshare.ui.theme.CampusRideShareTheme

@Composable
fun RatingBar(
    rating: Double,
    modifier: Modifier = Modifier,
    starSize: Dp = 16.dp,
    totalStars: Int = 5,
    activeColor: Color = Color(0xFFFFC107) // Taxi Amber
) {
    Row(modifier = modifier) {
        for (i in 1..totalStars) {
            val icon = when {
                rating >= i -> Icons.Filled.Star
                rating >= i - 0.5 -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Filled.StarOutline
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(starSize),
                tint = activeColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RatingBarPreview() {
    CampusRideShareTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            RatingBar(rating = 4.5)
            RatingBar(rating = 3.0)
            RatingBar(rating = 1.2)
        }
    }
}
