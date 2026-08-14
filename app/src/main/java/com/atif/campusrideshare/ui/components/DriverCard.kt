package com.atif.campusrideshare.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atif.campusrideshare.data.model.RideModel
import com.atif.campusrideshare.ui.theme.CampusRideShareTheme

@Composable
fun DriverCard(ride: RideModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(name = ride.driverName, size = 64.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = ride.driverName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    RatingBar(rating = ride.driverRating, starSize = 20.dp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            VehicleBadge(
                vehicleType = ride.vehicleType,
                seatsLeft = ride.seatsLeft,
                costPerSeat = ride.costPerSeat
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${ride.vehicleModel} • ${ride.vehicleColor} • ${ride.vehiclePlate}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DriverCardPreview() {
    CampusRideShareTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DriverCard(
                ride = RideModel(
                    driverName = "Atif Shahzad",
                    driverRating = 4.5,
                    vehicleType = "car",
                    vehicleModel = "Honda Civic",
                    vehicleColor = "White",
                    vehiclePlate = "ABC-123",
                    seatsLeft = 3,
                    costPerSeat = 150.0
                )
            )
        }
    }
}
