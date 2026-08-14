package com.atif.campusrideshare.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.atif.campusrideshare.R
import com.atif.campusrideshare.data.model.NotificationModel
import com.atif.campusrideshare.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var db: FirebaseDatabase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            authRepository.updateFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val type = message.data["type"] ?: "general"
        val title = message.data["title"] ?: message.notification?.title ?: "Campus Ride Share"
        val body = message.data["body"] ?: message.notification?.body ?: ""
        val rideId = message.data["rideId"] ?: ""
        val uid = auth.currentUser?.uid ?: return

        // 1. Show local notification
        showNotification(title, body, type)

        // 2. Save to in-app notification history
        saveNotificationToDb(uid, title, body, type, rideId)
    }

    private fun showNotification(title: String, body: String, type: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "campus_rideshare_notifications"

        val channel = NotificationChannel(
            channelId,
            "Ride Updates",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this exists
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun saveNotificationToDb(uid: String, title: String, body: String, type: String, rideId: String) {
        val ref = db.getReference("notifications").child(uid)
        val notifId = ref.push().key ?: return
        val notification = NotificationModel(
            notifId = notifId,
            uid = uid,
            title = title,
            body = body,
            type = type,
            rideId = rideId,
            isRead = false,
            createdAt = System.currentTimeMillis()
        )
        ref.child(notifId).setValue(notification)
    }
}
