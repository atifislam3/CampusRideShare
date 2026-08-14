package com.atif.campusrideshare.data.model

data class RideRequestModel(
    val requestId: String = "",
    val rideId: String = "",
    val passengerUid: String = "",
    val passengerName: String = "",
    val passengerInitialsColor: Int = 0,
    val passengerRating: Double = 0.0,
    val status: String = "pending",
    val requestedAt: Long = System.currentTimeMillis(),
    val respondedAt: Long = 0L
)
