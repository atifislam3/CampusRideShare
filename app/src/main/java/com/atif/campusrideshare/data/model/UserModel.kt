package com.atif.campusrideshare.data.model

data class UserModel(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val university: String = "",
    val initialsColor: Int = 0,
    val role: String = "user",
    val banned: Boolean = false,
    val averageRating: Double = 0.0,
    val totalRatings: Int = 0,
    val fcmToken: String = "",
    val vehicleType: String = "",
    val vehicleModel: String = "",
    val vehicleColor: String = "",
    val vehiclePlate: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
