package com.atif.campusrideshare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atif.campusrideshare.data.model.RideModel
import com.atif.campusrideshare.ui.theme.CampusRideShareTheme
import com.atif.campusrideshare.util.Config
import com.atif.campusrideshare.util.TimeAgo

@Composable
fun RideCard(
    ride: RideModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Driver Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                InitialsAvatar(name = ride.driverName, size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ride.driverName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    RatingBar(rating = ride.driverRating)
                }
                StatusChip(status = ride.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            VehicleBadge(
                vehicleType = ride.vehicleType,
                seatsLeft = ride.seatsLeft,
                costPerSeat = ride.costPerSeat
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Route
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ride.startAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).padding(horizontal = 4.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = ride.destinationName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Departure: ${TimeAgo.formatDepartureTime(ride.departureTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (color, text) = when (status) {
        Config.STATUS_OPEN -> Color(0xFF2E7D32) to "Open"
        Config.STATUS_FULL -> Color(0xFFFFC107) to "Full"
        Config.STATUS_COMPLETED -> Color.Gray to "Completed"
        Config.STATUS_CANCELLED -> Color.Red to "Cancelled"
        else -> Color.Gray to status.replaceFirstChar { it.uppercase() }
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RideCardPreview() {
    CampusRideShareTheme {
        RideCard(
            ride = RideModel(
                driverName = "Atif Shahzad",
                driverRating = 4.5,
                startAddress = "Home",
                destinationName = "Office",
                departureTime = System.currentTimeMillis(),
                seatsLeft = 3,
                costPerSeat = 150.0,
                vehicleType = Config.VEHICLE_CAR,
                status = Config.STATUS_OPEN
            ),
            onClick = {}
        )
    }
}
