package com.atif.campusrideshare.data.repository

import com.atif.campusrideshare.data.model.UserModel
import com.atif.campusrideshare.util.Config
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseDatabase
) {
    private val usersRef = db.getReference("users")

    suspend fun signUp(
        fullName: String,
        email: String,
        password: String,
        phone: String,
        university: String
    ): Result<Unit> = try {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = authResult.user?.uid ?: throw Exception("Auth failed")

        val userModel = UserModel(
            uid = uid,
            fullName = fullName,
            email = email,
            phone = phone,
            university = university,
            initialsColor = fullName.hashCode(),
            role = Config.ROLE_USER,
            banned = false,
            createdAt = System.currentTimeMillis()
        )

        usersRef.child(uid).setValue(userModel).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = try {
        auth.signInWithEmailAndPassword(email, password).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signInWithGoogle(idToken: String): Result<Unit> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun getCurrentUser(): UserModel? = try {
        val uid = auth.currentUser?.uid ?: return null
        val snapshot = usersRef.child(uid).get().await()
        snapshot.getValue(UserModel::class.java)
    } catch (e: Exception) {
        null
    }

    fun observeCurrentUser(): Flow<UserModel?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(UserModel::class.java)
                trySend(user)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        usersRef.child(uid).addValueEventListener(listener)
        awaitClose { usersRef.child(uid).removeEventListener(listener) }
    }

    suspend fun updateFcmToken(token: String): Result<Unit> = try {
        val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
        usersRef.child(uid).child("fcmToken").setValue(token).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateProfile(
        fullName: String,
        phone: String,
        university: String
    ): Result<Unit> = try {
        val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
        val updates = mapOf(
            "fullName" to fullName,
            "phone" to phone,
            "university" to university,
            "initialsColor" to fullName.hashCode()
        )
        usersRef.child(uid).updateChildren(updates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateVehicleInfo(
        vehicleType: String,
        vehicleModel: String,
        vehicleColor: String,
        vehiclePlate: String
    ): Result<Unit> = try {
        if (vehicleType != Config.VEHICLE_CAR && vehicleType != Config.VEHICLE_BIKE) {
            throw Exception("Invalid vehicle type")
        }
        val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
        val updates = mapOf(
            "vehicleType" to vehicleType,
            "vehicleModel" to vehicleModel,
            "vehicleColor" to vehicleColor,
            "vehiclePlate" to vehiclePlate
        )
        usersRef.child(uid).updateChildren(updates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun isUserBanned(uid: String): Result<Boolean> = try {
        val snapshot = usersRef.child(uid).child("banned").get().await()
        val isBanned = snapshot.getValue(Boolean::class.java) ?: false
        Result.success(isBanned)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
