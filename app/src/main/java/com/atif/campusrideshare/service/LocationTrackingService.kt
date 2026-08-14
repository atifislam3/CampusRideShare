package com.atif.campusrideshare.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.atif.campusrideshare.R
import com.atif.campusrideshare.data.repository.LocationRepository
import com.atif.campusrideshare.data.repository.RideRepository
import com.atif.campusrideshare.util.Config
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A foreground service is used for location tracking to ensure reliability and continuity 
 * even when the app is backgrounded or the screen is off. Android's power management 
 * would otherwise throttle or kill background coroutines/threads, causing the driver's 
 * location to stop updating for passengers.
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var rideRepository: RideRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var trackingJob: Job? = null
    private var statusObservationJob: Job? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_RIDE_ID = "EXTRA_RIDE_ID"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_tracking_channel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val rideId = intent.getStringExtra(EXTRA_RIDE_ID)
                if (rideId != null) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    startTracking(rideId)
                }
            }
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startTracking(rideId: String) {
        // Cancel any existing job
        trackingJob?.cancel()
        
        // Start sharing location
        trackingJob = locationRepository.startSharingLocation(rideId, serviceScope)

        // Observe ride status to auto-stop when ride ends
        statusObservationJob?.cancel()
        statusObservationJob = serviceScope.launch {
            rideRepository.observeRide(rideId).collectLatest { ride ->
                if (ride == null || ride.status == Config.STATUS_COMPLETED || ride.status == Config.STATUS_CANCELLED) {
                    stopSelf()
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Sharing",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Campus Ride Share")
            .setContentText("Sharing your ride location...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
