package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.UserNotification
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeUserNotifications(userId: String): Flow<List<UserNotification>> = callbackFlow {
        val listener = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationRepo", "Error observing notifications", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val notifications = snapshot.documents.mapNotNull { it.toObject(UserNotification::class.java) }
                        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    trySend(notifications)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendNotification(
        userId: String,
        title: String,
        body: String,
        itemId: String = "",
        matchId: String = ""
    ): Result<Unit> {
        return try {
            val payload = mapOf(
                "userId" to userId,
                "title" to title,
                "body" to body,
                "itemId" to itemId,
                "matchId" to matchId,
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("notifications").add(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Failed to send notification", e)
            Result.failure(e)
        }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            db.collection("notifications").document(notificationId)
                .update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Failed to mark notification as read", e)
            Result.failure(e)
        }
    }
}
