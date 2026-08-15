package com.atif.campusrideshare.data.repository

import com.atif.campusrideshare.data.model.RideModel
import com.atif.campusrideshare.util.Config
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RideRepository @Inject constructor(
    private val db: FirebaseDatabase
) {
    private val ridesRef = db.getReference("rides")

    suspend fun postRide(ride: RideModel): Result<String> = try {
        val key = ridesRef.push().key ?: throw Exception("Failed to generate ride key")
        val newRide = ride.copy(
            rideId = key,
            status = Config.STATUS_OPEN,
            seatsLeft = ride.totalSeats,
            createdAt = System.currentTimeMillis()
        )
        ridesRef.child(key).setValue(newRide).await()
        Result.success(key)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getOpenRides(): Flow<List<RideModel>> = callbackFlow {
        val now = System.currentTimeMillis()
        val query = ridesRef.orderByChild("status").equalTo(Config.STATUS_OPEN)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rides = snapshot.children.mapNotNull { it.getValue(RideModel::class.java) }
                    .filter { 
                        // Show everything for now to ensure user sees their rides
                        it.status == Config.STATUS_OPEN || it.status == Config.STATUS_FULL
                    }
                    .sortedByDescending { it.createdAt }
                trySend(rides)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    suspend fun getRideById(rideId: String): Result<RideModel> = try {
        val snapshot = ridesRef.child(rideId).get().await()
        val ride = snapshot.getValue(RideModel::class.java)
        if (ride != null) Result.success(ride)
        else Result.failure(Exception("Ride not found"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun observeRide(rideId: String): Flow<RideModel?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(RideModel::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ridesRef.child(rideId).addValueEventListener(listener)
        awaitClose { ridesRef.child(rideId).removeEventListener(listener) }
    }

    fun getMyRidesAsDriver(uid: String): Flow<List<RideModel>> = callbackFlow {
        val query = ridesRef.orderByChild("driverUid").equalTo(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rides = snapshot.children.mapNotNull { it.getValue(RideModel::class.java) }
                    .sortedByDescending { it.createdAt }
                trySend(rides)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    suspend fun updateRideStatus(rideId: String, status: String): Result<Unit> = try {
        ridesRef.child(rideId).child("status").setValue(status).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun decrementSeatsLeft(rideId: String): Result<Unit> = try {
        val result = suspendTransaction(rideId) { mutableData ->
            val ride = mutableData.getValue(RideModel::class.java) ?: return@suspendTransaction Transaction.abort()
            
            if (ride.seatsLeft > 0) {
                val newSeats = ride.seatsLeft - 1
                val newStatus = if (newSeats == 0) Config.STATUS_FULL else ride.status
                
                mutableData.child("seatsLeft").value = newSeats
                mutableData.child("status").value = newStatus
                Transaction.success(mutableData)
            } else {
                Transaction.abort()
            }
        }
        if (result) Result.success(Unit)
        else Result.failure(Exception("Transaction aborted: No seats left or ride not found"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun incrementSeatsLeft(rideId: String): Result<Unit> = try {
        val result = suspendTransaction(rideId) { mutableData ->
            val ride = mutableData.getValue(RideModel::class.java) ?: return@suspendTransaction Transaction.abort()
            
            val newSeats = ride.seatsLeft + 1
            // If it was full, it's now open again
            val newStatus = if (ride.status == Config.STATUS_FULL) Config.STATUS_OPEN else ride.status
            
            mutableData.child("seatsLeft").value = newSeats
            mutableData.child("status").value = newStatus
            Transaction.success(mutableData)
        }
        if (result) Result.success(Unit)
        else Result.failure(Exception("Transaction aborted"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun cancelRide(rideId: String): Result<Unit> = updateRideStatus(rideId, Config.STATUS_CANCELLED)

    /**
     * Helper to run Firebase transactions with coroutines
     */
    private suspend fun suspendTransaction(
        rideId: String,
        handler: (MutableData) -> Transaction.Result
    ): Boolean = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        ridesRef.child(rideId).runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                return handler(mutableData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) continuation.resumeWith(Result.failure(error.toException()))
                else continuation.resumeWith(Result.success(committed))
            }
        })
    }
}
