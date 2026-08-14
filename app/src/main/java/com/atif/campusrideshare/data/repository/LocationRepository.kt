package com.atif.campusrideshare.data.repository

import android.annotation.SuppressLint
import com.atif.campusrideshare.util.Config
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val db: FirebaseDatabase,
    private val locationClient: FusedLocationProviderClient
) {
    private val ridesRef = db.getReference("rides")

    /**
     * Periodically updates the driver's location in the database.
     * Returns a Job that can be cancelled to stop sharing.
     */
    @SuppressLint("MissingPermission")
    fun startSharingLocation(rideId: String, scope: CoroutineScope): Job {
        return scope.launch {
            while (isActive) {
                try {
                    val location = locationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).await()

                    location?.let {
                        val updates = mapOf(
                            "driverLat" to it.latitude,
                            "driverLng" to it.longitude,
                            "locationUpdatedAt" to System.currentTimeMillis()
                        )
                        ridesRef.child(rideId).updateChildren(updates).await()
                    }
                } catch (e: Exception) {
                    // Log error or handle failure silently to keep the loop alive if possible
                    e.printStackTrace()
                }
                delay(Config.LOCATION_UPDATE_INTERVAL_MS)
            }
        }
    }

    /**
     * Observes the driver's location for a specific ride.
     */
    fun observeDriverLocation(rideId: String): Flow<Pair<Double, Double>?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("driverLat").getValue(Double::class.java)
                val lng = snapshot.child("driverLng").getValue(Double::class.java)
                
                if (lat != null && lng != null) {
                    trySend(Pair(lat, lng))
                } else {
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        val ref = ridesRef.child(rideId)
        ref.addValueEventListener(listener)
        
        awaitClose { ref.removeEventListener(listener) }
    }
}
