package com.atif.campusrideshare.data.repository

import com.atif.campusrideshare.data.model.ReportModel
import com.atif.campusrideshare.data.model.UserModel
import com.atif.campusrideshare.util.Config
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val db: FirebaseDatabase
) {
    private val usersRef = db.getReference("users")
    private val reportsRef = db.getReference("reports")

    /**
     * Defense-in-depth check to ensure the caller is an admin.
     */
    private suspend fun verifyAdmin(uid: String) {
        val snapshot = usersRef.child(uid).child("role").get().await()
        val role = snapshot.getValue(String::class.java)
        if (role != Config.ROLE_ADMIN) {
            throw Exception("Access denied: Admin role required")
        }
    }

    fun getAllReports(currentUid: String, statusFilter: String? = null): Flow<List<ReportModel>> = callbackFlow {
        try {
            verifyAdmin(currentUid)
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reports = snapshot.children.mapNotNull { it.getValue(ReportModel::class.java) }
                    .filter { statusFilter == null || it.status == statusFilter }
                    .sortedByDescending { it.createdAt }
                trySend(reports)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        reportsRef.addValueEventListener(listener)
        awaitClose { reportsRef.removeEventListener(listener) }
    }

    suspend fun resolveReport(
        currentUid: String,
        reportId: String,
        adminNote: String,
        newStatus: String
    ): Result<Unit> = try {
        verifyAdmin(currentUid)
        val updates = mapOf(
            "adminNote" to adminNote,
            "status" to newStatus,
            "resolvedAt" to System.currentTimeMillis()
        )
        reportsRef.child(reportId).updateChildren(updates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun banUser(currentUid: String, targetUid: String): Result<Unit> = try {
        verifyAdmin(currentUid)
        usersRef.child(targetUid).child("banned").setValue(true).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun unbanUser(currentUid: String, targetUid: String): Result<Unit> = try {
        verifyAdmin(currentUid)
        usersRef.child(targetUid).child("banned").setValue(false).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getAllUsers(currentUid: String): Flow<List<UserModel>> = callbackFlow {
        try {
            verifyAdmin(currentUid)
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.mapNotNull { it.getValue(UserModel::class.java) }
                    .sortedBy { it.fullName }
                trySend(users)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        usersRef.addValueEventListener(listener)
        awaitClose { usersRef.removeEventListener(listener) }
    }
}
