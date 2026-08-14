package com.atif.campusrideshare

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

@HiltAndroidApp
class CampusRideShareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Enable Firebase Realtime Database offline persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        
        // Initialize MapLibre SDK
        MapLibre.getInstance(this)
    }
}
