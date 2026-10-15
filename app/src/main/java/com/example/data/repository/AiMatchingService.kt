package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.example.data.model.ItemMatch
import com.example.data.model.ItemReport
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.*

class AiMatchingService(
    private val db: FirebaseFirestore,
    private val notificationRepository: NotificationRepository
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        NotificationRepository(context)
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun findMatchesForItem(targetItem: ItemReport, allItems: List<ItemReport>): List<ItemMatch> = withContext(Dispatchers.IO) {
        val candidateItems = allItems.filter { candidate ->
            candidate.id != targetItem.id &&
            candidate.type != targetItem.type &&
            candidate.status != "RESOLVED" &&
            candidate.status != "FLAGGED_SPAM"
        }

        if (candidateItems.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<ItemMatch>()

        for (candidate in candidateItems) {
            val lost = if (targetItem.type == "LOST") targetItem else candidate
            val found = if (targetItem.type == "FOUND") targetItem else candidate

            val matchResult = evaluateMatch(lost, found)
            if (matchResult.similarityScore >= 50.0) {
                // Save to Firestore matches collection
                try {
                    val matchData = matchResult.toMap() + mapOf(
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    val docRef = db.collection("matches").add(matchData).await()
                    val savedMatch = matchResult.copy(id = docRef.id)
                    results.add(savedMatch)

                    // Notify both users
                    notificationRepository.sendNotification(
                        userId = lost.userId,
                        title = "Kecocokan AI: ${lost.title}",
                        body = "Barang Anda kemungkinan ditemukan! Skor kecocokan: ${matchResult.similarityScore.toInt()}%. Alasan: ${matchResult.matchReason}",
                        itemId = lost.id,
                        matchId = docRef.id
                    )

                    notificationRepository.sendNotification(
                        userId = found.userId,
                        title = "Kecocokan AI Temuan: ${found.title}",
                        body = "Barang yang Anda temukan cocok dengan laporan kehilangan. Skor: ${matchResult.similarityScore.toInt()}%.",
                        itemId = found.id,
                        matchId = docRef.id
                    )
                } catch (e: Exception) {
                    Log.e("AiMatchingService", "Error saving match", e)
                    results.add(matchResult)
                }
            }
        }

        results.sortedByDescending { it.similarityScore }
    }

    private suspend fun evaluateMatch(lost: ItemReport, found: ItemReport): ItemMatch {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    Bandingkan dua laporan barang ini dan tentukan apakah keduanya merupakan barang yang sama:
                    Laporan Barang Hilang:
                    - Judul: ${lost.title}
                    - Kategori: ${lost.category}
                    - Deskripsi: ${lost.description}
                    - Lokasi: ${lost.locationName} (${lost.latitude}, ${lost.longitude})
                    - Waktu: ${lost.dateTimeLostOrFound}

                    Laporan Barang Ditemukan:
                    - Judul: ${found.title}
                    - Kategori: ${found.category}
                    - Deskripsi: ${found.description}
                    - Lokasi: ${found.locationName} (${found.latitude}, ${found.longitude})
                    - Waktu: ${found.dateTimeLostOrFound}

                    Kembalikan format JSON persis seperti ini:
                    {
                      "similarityScore": <angka 0 hingga 100>,
                      "matchReason": "<ringkasan 1-2 kalimat alasan kecocokan atau perbedaan karakteristik>"
                    }
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", prompt))
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("responseMimeType", "application/json")
                        put("temperature", 0.2)
                    })
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
                val body = requestJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).post(body).build()

                val response = httpClient.newCall(request).execute()
                val responseString = response.body?.string() ?: ""

                if (response.isSuccessful && responseString.isNotBlank()) {
                    val root = JSONObject(responseString)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val text = candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")

                        val parsed = JSONObject(text)
                        val score = parsed.optDouble("similarityScore", 0.0)
                        val reason = parsed.optString("matchReason", "Karakteristik barang menunjukkan kemiripan visual dan konteks.")

                        return ItemMatch(
                            lostItemId = lost.id,
                            foundItemId = found.id,
                            lostUserId = lost.userId,
                            foundUserId = found.userId,
                            similarityScore = score,
                            matchReason = reason,
                            status = "PENDING"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w("AiMatchingService", "Gemini API fallback to local heuristic", e)
            }
        }

        // Robust Local Heuristic Comparison Algorithm
        return localHeuristicMatch(lost, found)
    }

    private fun localHeuristicMatch(lost: ItemReport, found: ItemReport): ItemMatch {
        var score = 0.0
        val reasons = mutableListOf<String>()

        // 1. Category match (30 points)
        if (lost.category.equals(found.category, ignoreCase = true)) {
            score += 30.0
            reasons.add("Kategori sama (${lost.category})")
        }

        // 2. Title & Description token overlap (Jaccard similarity up to 40 points)
        val lostTokens = tokenize("${lost.title} ${lost.description}")
        val foundTokens = tokenize("${found.title} ${found.description}")
        val commonTokens = lostTokens.intersect(foundTokens)

        if (lostTokens.isNotEmpty() && foundTokens.isNotEmpty()) {
            val overlapRatio = commonTokens.size.toDouble() / (lostTokens.size + foundTokens.size - commonTokens.size)
            val textScore = (overlapRatio * 100).coerceIn(0.0, 40.0)
            score += textScore
            if (commonTokens.isNotEmpty()) {
                val matchedWords = commonTokens.take(3).joinToString(", ")
                reasons.add("Kata kunci serupa: $matchedWords")
            }
        }

        // 3. Location proximity (up to 30 points)
        val distanceKm = calculateDistanceKm(lost.latitude, lost.longitude, found.latitude, found.longitude)
        if (distanceKm < 2.0) {
            score += 30.0
            reasons.add("Lokasi sangat dekat (~${String.format("%.1f", distanceKm)} km)")
        } else if (distanceKm < 10.0) {
            score += 20.0
            reasons.add("Area lokasi berdekatan (~${String.format("%.1f", distanceKm)} km)")
        } else if (lost.locationName.isNotBlank() && found.locationName.isNotBlank()) {
            if (lost.locationName.contains(found.locationName, true) || found.locationName.contains(lost.locationName, true)) {
                score += 15.0
                reasons.add("Nama lokasi beririsan")
            }
        }

        val finalScore = score.coerceIn(10.0, 98.0)
        val reasonText = if (reasons.isNotEmpty()) {
            reasons.joinToString("; ")
        } else {
            "Terdapat kesamaan ciri barang dan estimasi waktu pelaporan."
        }

        return ItemMatch(
            lostItemId = lost.id,
            foundItemId = found.id,
            lostUserId = lost.userId,
            foundUserId = found.userId,
            similarityScore = finalScore,
            matchReason = reasonText,
            status = "PENDING"
        )
    }

    private fun tokenize(text: String): Set<String> {
        val stopwords = setOf("yang", "di", "dan", "dengan", "untuk", "pada", "adalah", "ini", "itu", "ke", "dari", "ada", "saya")
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in stopwords }
            .toSet()
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1 == 0.0 && lon1 == 0.0 || lat2 == 0.0 && lon2 == 0.0) return 999.0
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
