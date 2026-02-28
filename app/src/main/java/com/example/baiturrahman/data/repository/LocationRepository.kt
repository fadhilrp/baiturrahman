package com.example.baiturrahman.data.repository

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import com.example.baiturrahman.data.remote.NominatimClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class LocationRepository {

    private val TAG = "LocationRepository"

    suspend fun searchLocations(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            NominatimClient.service.search(query).map { it.displayName }
        } catch (e: Exception) {
            Log.e(TAG, "Location search error", e)
            emptyList()
        }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        try {
            NominatimClient.service.reverse(lat, lon).displayName
        } catch (e: Exception) {
            Log.e(TAG, "Reverse geocode error", e)
            null
        }
    }

    /**
     * Returns the device's GPS coordinates, or null if no location provider is enabled
     * or permission was not granted.
     * Tries last known location first (fast), then falls back to a fresh update.
     */
    suspend fun getGpsCoordinates(context: Context): Pair<Double, Double>? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasGps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val hasNetwork = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!hasGps && !hasNetwork) return null

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            try {
                @Suppress("MissingPermission")
                val last = lm.getLastKnownLocation(provider)
                if (last != null) return Pair(last.latitude, last.longitude)
            } catch (_: SecurityException) {}
        }

        // No cached location — request a fresh one
        val provider = if (hasGps) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
        return suspendCancellableCoroutine { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    lm.removeUpdates(this)
                    if (cont.isActive) cont.resume(Pair(loc.latitude, loc.longitude))
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                override fun onProviderDisabled(p: String) {
                    if (cont.isActive) cont.resume(null)
                }
            }
            try {
                @Suppress("MissingPermission")
                lm.requestLocationUpdates(provider, 0L, 0f, listener)
                cont.invokeOnCancellation { lm.removeUpdates(listener) }
            } catch (_: SecurityException) {
                cont.resume(null)
            }
        }
    }
}
