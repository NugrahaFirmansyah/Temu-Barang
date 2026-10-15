package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeUserChats(userId: String): Flow<List<ChatRoom>> = callbackFlow {
        val listener = db.collection("chats")
            .whereArrayContains("participantUids", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error observing chats: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { it.toObject(ChatRoom::class.java) }
                        .sortedByDescending { it.lastMessageTime?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L }
                    trySend(chats)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = db.collection("chats").document(chatId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error observing messages: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
                    trySend(messages)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getOrCreateChat(
        lostUserId: String,
        foundUserId: String,
        itemId: String,
        itemTitle: String
    ): Result<String> {
        return try {
            // Predictable room ID for item + pair
            val sortedUids = listOf(lostUserId, foundUserId).sorted()
            val chatId = "chat_${itemId}_${sortedUids[0].take(6)}_${sortedUids[1].take(6)}"

            val docRef = db.collection("chats").document(chatId)
            val doc = docRef.get().await()

            if (!doc.exists()) {
                val newRoom = mapOf(
                    "itemId" to itemId,
                    "itemTitle" to itemTitle,
                    "participantUids" to sortedUids,
                    "lastMessage" to "Percakapan aman dimulai.",
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(newRoom).await()
            }
            Result.success(chatId)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error finding/creating chat", e)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        text: String
    ): Result<Unit> {
        return try {
            val msgData = mapOf(
                "chatId" to chatId,
                "senderId" to senderId,
                "senderName" to senderName,
                "text" to text,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("chats").document(chatId).collection("messages").add(msgData).await()

            // Update parent chat room last message
            db.collection("chats").document(chatId).update(
                mapOf(
                    "lastMessage" to text,
                    "lastMessageTime" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message", e)
            Result.failure(e)
        }
    }
}
