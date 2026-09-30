package com.example.drugdetector.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {

    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val locationName: String
    )

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): LocationData {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return LocationData(
                latitude = 0.0,
                longitude = 0.0,
                accuracy = 0.0f,
                locationName = "Location Permission Required"
            )
        }

        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            // Step 1: Active High Accuracy Fresh GPS Fix
            val cancellationTokenSource = CancellationTokenSource()
            val freshLoc: Location? = suspendCancellableCoroutine { continuation ->
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { loc ->
                    continuation.resume(loc)
                }.addOnFailureListener {
                    continuation.resume(null)
                }
            }

            // Step 2: Try Fused Location Client Last Known Location (only if fresh < 60s)
            val fusedLastLoc: Location? = suspendCancellableCoroutine { continuation ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { loc ->
                        if (loc != null && (System.currentTimeMillis() - loc.time) < 60000) {
                            continuation.resume(loc)
                        } else {
                            continuation.resume(null)
                        }
                    }
                    .addOnFailureListener { continuation.resume(null) }
            }

            // Step 3: System Location Manager Providers (GPS / Network)
            val systemLoc = getSystemLocation(context)

            val bestLocation = freshLoc ?: fusedLastLoc ?: systemLoc

            if (bestLocation != null && (bestLocation.latitude != 0.0 || bestLocation.longitude != 0.0)) {
                val addressText = getAddressName(context, bestLocation.latitude, bestLocation.longitude)
                LocationData(
                    latitude = bestLocation.latitude,
                    longitude = bestLocation.longitude,
                    accuracy = if (bestLocation.accuracy > 0f) bestLocation.accuracy else 5.0f,
                    locationName = addressText
                )
            } else {
                LocationData(
                    latitude = 0.0,
                    longitude = 0.0,
                    accuracy = 0.0f,
                    locationName = "Turn on Phone GPS / Location Services"
                )
            }
        } catch (e: Exception) {
            LocationData(
                latitude = 0.0,
                longitude = 0.0,
                accuracy = 0.0f,
                locationName = "Location Service Unavailable"
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getSystemLocation(context: Context): Location? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val now = System.currentTimeMillis()
            listOfNotNull(gpsLoc, networkLoc)
                .filter { (now - it.time) < 120000 } // Only use fixes less than 2 minutes old
                .maxByOrNull { it.time }
        } catch (e: Exception) {
            null
        }
    }

    private fun getAddressName(context: Context, latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val street = address.thoroughfare ?: address.subLocality ?: address.featureName ?: ""
                val city = address.locality ?: address.subAdminArea ?: ""
                val state = address.adminArea ?: ""
                val country = address.countryName ?: ""

                val parts = listOf(street, city, state, country)
                    .filter { it.isNotBlank() }
                    .distinct()

                if (parts.isNotEmpty()) {
                    parts.joinToString(", ") + " (${"%.4f".format(Locale.US, latitude)}°, ${"%.4f".format(Locale.US, longitude)}°)"
                } else {
                    "Lat: ${"%.5f".format(Locale.US, latitude)}°, Lon: ${"%.5f".format(Locale.US, longitude)}°"
                }
            } else {
                "Lat: ${"%.5f".format(Locale.US, latitude)}°, Lon: ${"%.5f".format(Locale.US, longitude)}°"
            }
        } catch (e: Exception) {
            "Lat: ${"%.5f".format(Locale.US, latitude)}°, Lon: ${"%.5f".format(Locale.US, longitude)}°"
        }
    }
}
