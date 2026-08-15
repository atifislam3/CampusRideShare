package com.atif.campusrideshare

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

@HiltAndroidApp
class CampusRideShareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Disable persistence for initial debugging to ensure real-time errors are visible
        // FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        
        // Initialize MapLibre SDK
        MapLibre.getInstance(this)
    }
}
