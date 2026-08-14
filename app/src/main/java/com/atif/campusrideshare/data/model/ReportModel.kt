package com.atif.campusrideshare.data.model

data class ReportModel(
    val reportId: String = "",
    val reporterUid: String = "",
    val reportedUid: String = "",
    val rideId: String = "",
    val reason: String = "",
    val description: String = "",
    val status: String = "pending",
    val adminNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long = 0L
)
