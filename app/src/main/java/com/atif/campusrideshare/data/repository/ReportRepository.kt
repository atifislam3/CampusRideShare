package com.atif.campusrideshare.data.repository

import com.atif.campusrideshare.data.model.ReportModel
import com.atif.campusrideshare.util.Config
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val db: FirebaseDatabase
) {
    private val reportsRef = db.getReference("reports")

    suspend fun fileReport(
        reporterUid: String,
        reportedUid: String,
        rideId: String,
        reason: String,
        description: String
    ): Result<Unit> = try {
        val key = reportsRef.push().key ?: throw Exception("Failed to generate report key")
        val report = ReportModel(
            reportId = key,
            reporterUid = reporterUid,
            reportedUid = reportedUid,
            rideId = rideId,
            reason = reason,
            description = description,
            status = Config.REPORT_PENDING,
            createdAt = System.currentTimeMillis()
        )
        reportsRef.child(key).setValue(report).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
