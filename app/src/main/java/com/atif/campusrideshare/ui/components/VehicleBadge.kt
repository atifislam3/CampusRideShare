package com.atif.campusrideshare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atif.campusrideshare.ui.theme.CampusRideShareTheme
import com.atif.campusrideshare.util.Config
import com.atif.campusrideshare.util.VehicleUtils

/**
 * Renders a stylized badge for a vehicle with its details.
 * Format: "🚗 Car  •  {seatsLeft} seats  •  Rs. {costPerSeat}/seat"
 */
@Composable
fun VehicleBadge(
    vehicleType: String,
    seatsLeft: Int,
    costPerSeat: Double,
) {
    val emoji = if (vehicleType == Config.VEHICLE_CAR) "🚗" else "🏍️"
    val label = VehicleUtils.displayLabel(vehicleType)
    val seatText = if (vehicleType == Config.VEHICLE_CAR) "$seatsLeft seats" else "1 seat"
    
    val text = "$emoji $label  •  $seatText  •  Rs. ${costPerSeat.toInt()}/seat"

    Box(
        modifier = Modifier
            .background(
                color = VehicleUtils.badgeColor(vehicleType).copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = VehicleUtils.badgeColor(vehicleType),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VehicleBadgePreview() {
    CampusRideShareTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            VehicleBadge(vehicleType = Config.VEHICLE_CAR, seatsLeft = 3, costPerSeat = 250.0)
            Spacer(modifier = Modifier.height(8.dp))
            VehicleBadge(vehicleType = Config.VEHICLE_BIKE, seatsLeft = 1, costPerSeat = 120.0)
        }
    }
}
