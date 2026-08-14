package com.atif.campusrideshare.data.repository

import com.atif.campusrideshare.data.model.RatingModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class RatingRepository @Inject constructor(
    private val db: FirebaseDatabase
) {
    private val ratingsRef = db.getReference("ratings")
    private val usersRef = db.getReference("users")

    suspend fun submitRating(
        ratedUid: String,
        raterUid: String,
        rideId: String,
        stars: Int,
        review: String,
        raterRole: String
    ): Result<Unit> = try {
        val key = ratingsRef.child(ratedUid).push().key ?: throw Exception("Failed to generate rating key")
        val rating = RatingModel(
            ratingId = key,
            raterUid = raterUid,
            ratedUid = ratedUid,
            rideId = rideId,
            stars = stars,
            review = review,
            raterRole = raterRole,
            createdAt = System.currentTimeMillis()
        )

        // 1. Save the rating
        ratingsRef.child(ratedUid).child(key).setValue(rating).await()

        // 2. Update user average using a transaction
        // We use a transaction here to prevent race conditions. If two ratings are submitted 
        // simultaneously, a simple read-modify-write could lead to an incorrect average or count.
        val success = suspendCoroutine<Boolean> { continuation ->
            usersRef.child(ratedUid).runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val currentCount = mutableData.child("totalRatings").getValue(Int::class.java) ?: 0
                    val currentAvg = mutableData.child("averageRating").getValue(Double::class.java) ?: 0.0

                    val newCount = currentCount + 1
                    val newAvg = ((currentAvg * currentCount) + stars) / newCount

                    mutableData.child("totalRatings").value = newCount
                    mutableData.child("averageRating").value = newAvg
                    
                    return Transaction.success(mutableData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    continuation.resume(committed && error == null)
                }
            })
        }

        if (success) Result.success(Unit)
        else Result.failure(Exception("Transaction failed to update user ratings"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getRatingsForUser(uid: String): Result<List<RatingModel>> = try {
        val snapshot = ratingsRef.child(uid).get().await()
        val ratings = snapshot.children.mapNotNull { it.getValue(RatingModel::class.java) }
            .sortedByDescending { it.createdAt }
        Result.success(ratings)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
