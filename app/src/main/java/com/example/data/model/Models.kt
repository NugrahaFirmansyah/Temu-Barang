package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ItemReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val type: String = "LOST", // "LOST" or "FOUND"
    val title: String = "",
    val category: String = "Elektronik",
    val description: String = "",
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val dateTimeLostOrFound: String = "",
    val imageUrl: String = "",
    val status: String = "OPEN", // "OPEN", "MATCHED", "RESOLVED", "FLAGGED_SPAM"
    val isVerified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "userName" to userName,
        "userEmail" to userEmail,
        "type" to type,
        "title" to title,
        "category" to category,
        "description" to description,
        "locationName" to locationName,
        "latitude" to latitude,
        "longitude" to longitude,
        "dateTimeLostOrFound" to dateTimeLostOrFound,
        "imageUrl" to imageUrl,
        "status" to status,
        "isVerified" to isVerified
    ).filterValues { it != null }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val phone: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "email" to email,
        "photoUrl" to photoUrl,
        "phone" to phone
    ).filterValues { it != null }
}

@IgnoreExtraProperties
data class ItemMatch(
    @DocumentId val id: String = "",
    val lostItemId: String = "",
    val foundItemId: String = "",
    val lostUserId: String = "",
    val foundUserId: String = "",
    val similarityScore: Double = 0.0,
    val matchReason: String = "",
    val status: String = "PENDING", // "PENDING", "ACCEPTED", "REJECTED"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "lostItemId" to lostItemId,
        "foundItemId" to foundItemId,
        "lostUserId" to lostUserId,
        "foundUserId" to foundUserId,
        "similarityScore" to similarityScore,
        "matchReason" to matchReason,
        "status" to status
    ).filterValues { it != null }
}

@IgnoreExtraProperties
data class ChatRoom(
    @DocumentId val id: String = "",
    val itemId: String = "",
    val itemTitle: String = "",
    val participantUids: List<String> = emptyList(),
    val lastMessage: String = "",
    @ServerTimestamp val lastMessageTime: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "itemId" to itemId,
        "itemTitle" to itemTitle,
        "participantUids" to participantUids,
        "lastMessage" to lastMessage
    ).filterValues { it != null }
}

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "chatId" to chatId,
        "senderId" to senderId,
        "senderName" to senderName,
        "text" to text
    ).filterValues { it != null }
}

@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val itemId: String = "",
    val matchId: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "title" to title,
        "body" to body,
        "itemId" to itemId,
        "matchId" to matchId,
        "isRead" to isRead
    ).filterValues { it != null }
}
