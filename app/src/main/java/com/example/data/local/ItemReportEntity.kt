package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ItemReport
import com.google.firebase.Timestamp
import java.util.Date

@Entity(tableName = "item_reports")
data class ItemReportEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val type: String, // "LOST" or "FOUND"
    val title: String,
    val category: String,
    val description: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val dateTimeLostOrFound: String,
    val imageUrl: String,
    val status: String, // "OPEN", "MATCHED", "RESOLVED", "FLAGGED_SPAM"
    val isVerified: Boolean,
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val cachedAtEpoch: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): ItemReport {
        return ItemReport(
            id = id,
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            type = type,
            title = title,
            category = category,
            description = description,
            locationName = locationName,
            latitude = latitude,
            longitude = longitude,
            dateTimeLostOrFound = dateTimeLostOrFound,
            imageUrl = imageUrl,
            status = status,
            isVerified = isVerified,
            createdAt = if (createdAtEpoch > 0) Timestamp(Date(createdAtEpoch)) else null,
            updatedAt = Timestamp(Date(cachedAtEpoch))
        )
    }
}

fun ItemReport.toEntity(): ItemReportEntity {
    return ItemReportEntity(
        id = id,
        userId = userId,
        userName = userName,
        userEmail = userEmail,
        type = type,
        title = title,
        category = category,
        description = description,
        locationName = locationName,
        latitude = latitude,
        longitude = longitude,
        dateTimeLostOrFound = dateTimeLostOrFound,
        imageUrl = imageUrl,
        status = status,
        isVerified = isVerified,
        createdAtEpoch = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
        cachedAtEpoch = System.currentTimeMillis()
    )
}
