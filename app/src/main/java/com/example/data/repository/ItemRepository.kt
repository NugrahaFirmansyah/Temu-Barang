package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.ItemReportDao
import com.example.data.local.toEntity
import com.example.data.model.ItemReport
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.*

data class NearbyItemReport(
    val item: ItemReport,
    val distanceKm: Double
)

class ItemRepository(
    private val db: FirebaseFirestore,
    private val itemDao: ItemReportDao? = null
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        AppDatabase.getDatabase(context.applicationContext).itemReportDao()
    )

    private val auth = Firebase.auth
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google")
    }

    // =========================================================================
    // 1. CREATE: Upload Report Metadata
    // =========================================================================

    /**
     * Mengunggah metadata laporan barang hilang atau temuan ke Firestore
     * dan menyinkronkannya ke Room database lokal.
     */
    suspend fun uploadReportMetadata(item: ItemReport): Result<String> {
        val uid = requireUserId()
        return try {
            val collection = db.collection("items")
            val payload = item.toMap() + mapOf(
                "userId" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val docRef = collection.add(payload).await()
            val newId = docRef.id

            // Update cache lokal di Room
            itemDao?.insertReport(item.copy(id = newId, userId = uid).toEntity())

            Result.success(newId)
        } catch (e: Exception) {
            Log.e("ItemRepository", "Error uploading report metadata", e)
            Result.failure(e)
        }
    }

    // Alias createReport for backward compatibility
    suspend fun createReport(item: ItemReport): Result<String> = uploadReportMetadata(item)

    // =========================================================================
    // 2. READ: Single Item, Real-time Observation, & Nearby Location Query
    // =========================================================================

    /**
     * Mengambil satu laporan spesifik berdasarkan ID dokumen.
     */
    suspend fun getReportById(itemId: String): Result<ItemReport?> {
        return try {
            val doc = db.collection("items").document(itemId).get().await()
            if (doc.exists()) {
                val report = doc.toObject(ItemReport::class.java)
                Result.success(report)
            } else {
                // Cek dari cache lokal Room jika dokumen belum tersinkronisasi
                val local = itemDao?.getReportById(itemId)?.toDomainModel()
                Result.success(local)
            }
        } catch (e: Exception) {
            Log.w("ItemRepository", "Error fetching document by ID, checking local Room", e)
            val local = itemDao?.getReportById(itemId)?.toDomainModel()
            Result.success(local)
        }
    }

    /**
     * Mendengarkan seluruh laporan secara real-time dari Firestore
     * dengan dukungan fallback ke Room cache.
     */
    fun observeAllItems(): Flow<List<ItemReport>> = callbackFlow {
        // Emit awal dari cache Room untuk kecepatan antarmuka
        itemDao?.let { dao ->
            ioScope.launch {
                try {
                    val cached = dao.getAllReports().first()
                    if (cached.isNotEmpty()) {
                        trySend(cached.map { it.toDomainModel() })
                    }
                } catch (e: Exception) {
                    Log.w("ItemRepository", "Error reading Room cache: ${e.message}")
                }
            }
        }

        val listener = db.collection("items")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ItemRepository", "Error observing items: ${error.message}")
                    itemDao?.let { dao ->
                        ioScope.launch {
                            try {
                                val cached = dao.getAllReports().first()
                                trySend(cached.map { it.toDomainModel() })
                            } catch (_: Exception) {}
                        }
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { it.toObject(ItemReport::class.java) }
                    trySend(items)

                    // Sinkronisasi ke Room secara asinkron
                    itemDao?.let { dao ->
                        ioScope.launch {
                            try {
                                dao.insertAll(items.map { it.toEntity() })
                            } catch (e: Exception) {
                                Log.w("ItemRepository", "Error caching to Room: ${e.message}")
                            }
                        }
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Mengambil dan memfilter daftar laporan barang berdasarkan jarak lokasi terdekat
     * dari titik koordinat pengguna (userLat, userLon) menggunakan formula Haversine.
     * Hasil diurutkan dari jarak terdekat ke terjauh.
     */
    fun getNearbyReports(
        userLat: Double,
        userLon: Double,
        maxRadiusKm: Double? = null,
        typeFilter: String? = null
    ): Flow<List<NearbyItemReport>> {
        return observeAllItems().map { allReports ->
            allReports
                .filter { report ->
                    val matchesType = typeFilter == null || report.type.equals(typeFilter, ignoreCase = true)
                    val isValidStatus = report.status != "RESOLVED" && report.status != "FLAGGED_SPAM"
                    val hasCoordinates = report.latitude != 0.0 || report.longitude != 0.0
                    matchesType && isValidStatus && hasCoordinates
                }
                .map { report ->
                    val distance = calculateDistanceKm(userLat, userLon, report.latitude, report.longitude)
                    NearbyItemReport(item = report, distanceKm = distance)
                }
                .filter { nearby ->
                    if (maxRadiusKm != null) nearby.distanceKm <= maxRadiusKm else true
                }
                .sortedBy { it.distanceKm }
        }
    }

    /**
     * Mendengarkan laporan yang dibuat oleh pengguna tertentu.
     */
    fun observeUserItems(userId: String): Flow<List<ItemReport>> = callbackFlow {
        itemDao?.let { dao ->
            ioScope.launch {
                try {
                    val cached = dao.getReportsByUser(userId).first()
                    if (cached.isNotEmpty()) {
                        trySend(cached.map { it.toDomainModel() })
                    }
                } catch (_: Exception) {}
            }
        }

        val listener = db.collection("items")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ItemRepository", "Error observing user items: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { it.toObject(ItemReport::class.java) }
                        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }

    // =========================================================================
    // 3. UPDATE: Update Metadata & Status
    // =========================================================================

    /**
     * Memperbarui metadata laporan di Firestore dan cache lokal.
     */
    suspend fun updateReportMetadata(itemId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val payload = updates + mapOf("updatedAt" to FieldValue.serverTimestamp())
            db.collection("items").document(itemId).update(payload).await()

            itemDao?.getReportById(itemId)?.let { entity ->
                val updatedStatus = (updates["status"] as? String) ?: entity.status
                val updatedVerified = (updates["isVerified"] as? Boolean) ?: entity.isVerified
                itemDao.insertReport(entity.copy(status = updatedStatus, isVerified = updatedVerified))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ItemRepository", "Error updating report metadata", e)
            Result.failure(e)
        }
    }

    suspend fun updateReport(itemId: String, updates: Map<String, Any>): Result<Unit> =
        updateReportMetadata(itemId, updates)

    suspend fun markResolved(itemId: String): Result<Unit> {
        return updateReportMetadata(itemId, mapOf("status" to "RESOLVED"))
    }

    suspend fun setVerified(itemId: String, isVerified: Boolean): Result<Unit> {
        return updateReportMetadata(itemId, mapOf("isVerified" to isVerified))
    }

    suspend fun flagSpam(itemId: String): Result<Unit> {
        return updateReportMetadata(itemId, mapOf("status" to "FLAGGED_SPAM"))
    }

    // =========================================================================
    // 4. DELETE: Remove Document from Firestore and Room
    // =========================================================================

    /**
     * Menghapus laporan dari koleksi Firestore dan basis data lokal Room.
     */
    suspend fun deleteReport(itemId: String): Result<Unit> {
        return try {
            db.collection("items").document(itemId).delete().await()
            itemDao?.deleteReportById(itemId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ItemRepository", "Error deleting report", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 5. Utility: Distance Calculation (Haversine Formula)
    // =========================================================================

    /**
     * Menghitung jarak geodesic lingkaran besar antara dua koordinat lintang/bujur dalam kilometer.
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if ((lat1 == 0.0 && lon1 == 0.0) || (lat2 == 0.0 && lon2 == 0.0)) return Double.MAX_VALUE
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
