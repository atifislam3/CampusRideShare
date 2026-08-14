package com.atif.campusrideshare.data.repository

import com.atif.campusrideshare.data.model.NotificationModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val db: FirebaseDatabase
) {
    private val notificationsRef = db.getReference("notifications")

    fun observeMyNotifications(uid: String): Flow<List<NotificationModel>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(NotificationModel::class.java) }
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        val ref = notificationsRef.child(uid)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun markAsRead(uid: String, notifId: String): Result<Unit> = try {
        notificationsRef.child(uid).child(notifId).child("isRead").setValue(true).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun markAllAsRead(uid: String): Result<Unit> = try {
        val snapshot = notificationsRef.child(uid).get().await()
        val updates = mutableMapOf<String, Any>()
        snapshot.children.forEach { child ->
            val notif = child.getValue(NotificationModel::class.java)
            if (notif != null && !notif.isRead) {
                updates["${child.key}/isRead"] = true
            }
        }
        if (updates.isNotEmpty()) {
            notificationsRef.child(uid).updateChildren(updates).await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun unreadCount(uid: String): Flow<Int> = observeMyNotifications(uid).map { list ->
        list.count { !it.isRead }
    }
}
