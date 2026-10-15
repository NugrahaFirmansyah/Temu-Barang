package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemReportDao {

    @Query("SELECT * FROM item_reports ORDER BY createdAtEpoch DESC")
    fun getAllReports(): Flow<List<ItemReportEntity>>

    @Query("SELECT * FROM item_reports WHERE type = :type ORDER BY createdAtEpoch DESC")
    fun getReportsByType(type: String): Flow<List<ItemReportEntity>>

    @Query("SELECT * FROM item_reports WHERE userId = :userId ORDER BY createdAtEpoch DESC")
    fun getReportsByUser(userId: String): Flow<List<ItemReportEntity>>

    @Query("SELECT * FROM item_reports WHERE id = :id LIMIT 1")
    suspend fun getReportById(id: String): ItemReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ItemReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reports: List<ItemReportEntity>)

    @Update
    suspend fun updateReport(report: ItemReportEntity)

    @Query("DELETE FROM item_reports WHERE id = :id")
    suspend fun deleteReportById(id: String)

    @Query("DELETE FROM item_reports")
    suspend fun clearAll()
}
