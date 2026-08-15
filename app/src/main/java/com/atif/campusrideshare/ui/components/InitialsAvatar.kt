package com.atif.campusrideshare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atif.campusrideshare.ui.theme.colorFromName

/**
 * A circular avatar component that displays user initials.
 * Background color is deterministically derived from the user's name.
 */
@Composable
fun InitialsAvatar(
    name: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val initials = name.trim().split("\\s+".toRegex())
        .let { words ->
            when {
                words.isEmpty() || words.all { it.isEmpty() } -> "?"
                words.size == 1 -> words[0].take(1).uppercase()
                else -> (words.first().take(1) + words.last().take(1)).uppercase()
            }
        }

    Box(
        modifier = modifier
            .size(size)
            .background(color = colorFromName(name), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4).sp
            )
        )
    }
}
