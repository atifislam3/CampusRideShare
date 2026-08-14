package com.atif.campusrideshare.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StarSelector(
    selected: Int,
    onSelectedChange: (Int) -> Unit,
    totalStars: Int = 5,
    activeColor: Color = Color(0xFFFFC107) // Taxi Amber
) {
    Row {
        for (i in 1..totalStars) {
            Icon(
                imageVector = if (i <= selected) Icons.Filled.Star else Icons.Filled.StarOutline,
                contentDescription = "Rate $i stars",
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onSelectedChange(i) }
                    .padding(4.dp),
                tint = activeColor
            )
        }
    }
}


