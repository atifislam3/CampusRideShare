package com.atif.campusrideshare.util

import androidx.compose.ui.graphics.Color
import com.atif.campusrideshare.R

object VehicleUtils {

    fun maxSeatsFor(vehicleType: String): Int {
        return when (vehicleType) {
            Config.VEHICLE_CAR -> Config.MAX_CAR_SEATS
            Config.VEHICLE_BIKE -> Config.MIN_SEATS
            else -> Config.MIN_SEATS
        }
    }

    fun defaultSeatsFor(vehicleType: String): Int {
        return when (vehicleType) {
            Config.VEHICLE_CAR -> Config.MAX_CAR_SEATS
            Config.VEHICLE_BIKE -> Config.MIN_SEATS
            else -> Config.MIN_SEATS
        }
    }

    fun iconResFor(vehicleType: String): Int {
        return when (vehicleType) {
            Config.VEHICLE_CAR -> R.drawable.ic_car_pin
            Config.VEHICLE_BIKE -> R.drawable.ic_bike_pin
            else -> R.drawable.ic_car_pin
        }
    }

    fun displayLabel(vehicleType: String): String {
        return when (vehicleType) {
            Config.VEHICLE_CAR -> "Car"
            Config.VEHICLE_BIKE -> "Bike"
            else -> "Vehicle"
        }
    }

    fun badgeColor(vehicleType: String): Color {
        return when (vehicleType) {
            Config.VEHICLE_CAR -> Color(0xFF2196F3) // Blue
            Config.VEHICLE_BIKE -> Color(0xFF4CAF50) // Green
            else -> Color.Gray
        }
    }
}
