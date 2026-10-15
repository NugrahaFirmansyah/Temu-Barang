package com.example

import com.example.data.model.ItemReport
import com.example.data.repository.NearbyItemReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.*

class ItemRepositoryTest {

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if ((lat1 == 0.0 && lon1 == 0.0) || (lat2 == 0.0 && lon2 == 0.0)) return Double.MAX_VALUE
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    @Test
    fun `distance calculation between Monas and Bundaran HI is approximately 2 km`() {
        val monasLat = -6.1754
        val monasLon = 106.8272
        val bundaranHiLat = -6.1950
        val bundaranHiLon = 106.8230

        val distance = calculateDistanceKm(monasLat, monasLon, bundaranHiLat, bundaranHiLon)
        // Monas to Bundaran HI is roughly 2.2 km
        assertTrue("Distance should be around 2.2 km, was $distance", distance in 1.8..2.6)
    }

    @Test
    fun `nearby items sorting orders from closest to farthest`() {
        val userLat = -6.2000
        val userLon = 106.8250

        val itemClose = ItemReport(id = "1", title = "Kunci Dekat", latitude = -6.2010, longitude = 106.8255)
        val itemFar = ItemReport(id = "2", title = "Tas Jauh", latitude = -6.3644, longitude = 106.8286)

        val list = listOf(
            NearbyItemReport(item = itemFar, distanceKm = calculateDistanceKm(userLat, userLon, itemFar.latitude, itemFar.longitude)),
            NearbyItemReport(item = itemClose, distanceKm = calculateDistanceKm(userLat, userLon, itemClose.latitude, itemClose.longitude))
        ).sortedBy { it.distanceKm }

        assertEquals("1", list[0].item.id)
        assertEquals("2", list[1].item.id)
        assertTrue(list[0].distanceKm < list[1].distanceKm)
    }
}
