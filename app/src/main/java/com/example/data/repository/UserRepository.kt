package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val listener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepository", "Error observing profile: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(UserProfile::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveOrInitProfile(userId: String, displayName: String, email: String, photoUrl: String?): Result<Unit> {
        return try {
            val docRef = db.collection("users").document(userId)
            val doc = docRef.get().await()
            if (!doc.exists()) {
                val data = mapOf(
                    "userId" to userId,
                    "displayName" to displayName,
                    "email" to email,
                    "photoUrl" to photoUrl,
                    "phone" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(data).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving user profile", e)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(userId: String, displayName: String, phone: String?): Result<Unit> {
        return try {
            val data = mapOf(
                "displayName" to displayName,
                "phone" to (phone ?: ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(userId).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating profile", e)
            Result.failure(e)
        }
    }
}
