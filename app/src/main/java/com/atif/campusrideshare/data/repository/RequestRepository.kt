package com.atif.campusrideshare.data.repository

import com.atif.campusrideshare.data.model.RideRequestModel
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
class RequestRepository @Inject constructor(
    private val db: FirebaseDatabase,
    private val rideRepository: RideRepository
) {
    private val requestsRef = db.getReference("ride_requests")

    suspend fun sendRequest(
        rideId: String,
        passengerUid: String,
        passengerName: String,
        passengerInitialsColor: Int,
        passengerRating: Double
    ): Result<Unit> = try {
        val key = requestsRef.child(rideId).push().key ?: throw Exception("Failed to generate request key")
        val request = RideRequestModel(
            requestId = key,
            rideId = rideId,
            passengerUid = passengerUid,
            passengerName = passengerName,
            passengerInitialsColor = passengerInitialsColor,
            passengerRating = passengerRating,
            status = Config.REQUEST_PENDING,
            requestedAt = System.currentTimeMillis()
        )
        requestsRef.child(rideId).child(key).setValue(request).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun observeRequestsForRide(rideId: String): Flow<List<RideRequestModel>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requests = snapshot.children.mapNotNull { it.getValue(RideRequestModel::class.java) }
                trySend(requests)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        val ref = requestsRef.child(rideId)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Note: Given the schema /ride_requests/{rideId}/{requestId}, finding requests by passengerUid
     * across all rides requires a full scan of the /ride_requests node if we don't have a 
     * denormalized index (e.g., /user_requests/{uid}).
     * For simplicity and following the provided schema, we implement a client-side scan.
     */
    fun observeMyRequests(passengerUid: String): Flow<List<RideRequestModel>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val myRequests = mutableListOf<RideRequestModel>()
                // snapshot children are rideId nodes
                snapshot.children.forEach { rideSnapshot ->
                    rideSnapshot.children.forEach { requestSnapshot ->
                        val request = requestSnapshot.getValue(RideRequestModel::class.java)
                        if (request?.passengerUid == passengerUid) {
                            myRequests.add(request)
                        }
                    }
                }
                trySend(myRequests.sortedByDescending { it.requestedAt })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        requestsRef.addValueEventListener(listener)
        awaitClose { requestsRef.removeEventListener(listener) }
    }

    suspend fun respondToRequest(rideId: String, requestId: String, accept: Boolean): Result<Unit> = try {
        val status = if (accept) Config.REQUEST_ACCEPTED else Config.REQUEST_REJECTED
        val updates = mapOf(
            "status" to status,
            "respondedAt" to System.currentTimeMillis()
        )
        
        requestsRef.child(rideId).child(requestId).updateChildren(updates).await()
        
        if (accept) {
            rideRepository.decrementSeatsLeft(rideId).getOrThrow()
        }
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun cancelMyRequest(rideId: String, requestId: String): Result<Unit> = try {
        // Fetch current status to see if we need to increment seats
        val snapshot = requestsRef.child(rideId).child(requestId).get().await()
        val currentRequest = snapshot.getValue(RideRequestModel::class.java)
        
        requestsRef.child(rideId).child(requestId).child("status").setValue(Config.REQUEST_CANCELLED).await()
        
        // If the request was already accepted, we need to return the seat to the ride
        if (currentRequest?.status == Config.REQUEST_ACCEPTED) {
            rideRepository.incrementSeatsLeft(rideId).getOrThrow()
        }
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
