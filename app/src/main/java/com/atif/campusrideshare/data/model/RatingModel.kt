package com.atif.campusrideshare.data.model

data class RatingModel(
    val ratingId: String = "",
    val raterUid: String = "",
    val ratedUid: String = "",
    val rideId: String = "",
    val stars: Int = 0,
    val review: String = "",
    val raterRole: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
