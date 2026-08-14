package com.atif.campusrideshare.data.model

data class NotificationModel(
    val notifId: String = "",
    val uid: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    val rideId: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
