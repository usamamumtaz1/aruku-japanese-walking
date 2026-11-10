package com.japanesewalking.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationTracker(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var lastLocation: Location? = null
    private var totalDistanceMeters = 0.0

    private val _distanceFlow = MutableStateFlow(0.0)
    val distanceFlow: StateFlow<Double> = _distanceFlow.asStateFlow()

    private val _speedKmhFlow = MutableStateFlow(0.0)
    val speedKmhFlow: StateFlow<Double> = _speedKmhFlow.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (location in result.locations) {
                processNewLocation(location)
            }
        }
    }

    private fun processNewLocation(location: Location) {
        if (location.accuracy > 25) return // Ignore noisy GPS readings

        lastLocation?.let { prev ->
            val distance = prev.distanceTo(location)
            // Filter realistic walking speeds (< 15 km/h) and avoid GPS jitter (< 1.5m)
            if (distance in 1.5..50.0) {
                totalDistanceMeters += distance
                _distanceFlow.value = totalDistanceMeters

                // Speed in m/s converted to km/h
                val speedKmh = if (location.hasSpeed()) {
                    location.speed * 3.6
                } else {
                    val timeDeltaSec = (location.time - prev.time) / 1000.0
                    if (timeDeltaSec > 0) (distance / timeDeltaSec) * 3.6 else 0.0
                }
                _speedKmhFlow.value = speedKmh.coerceIn(0.0, 15.0)
            }
        }
        lastLocation = location
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED || androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "Location permission not granted; distance tracking skipped.")
            return
        }

        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                2000L // 2 second interval
            ).setMinUpdateIntervalMillis(1000L)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
        }
    }

    fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        lastLocation = null
    }

    fun reset() {
        totalDistanceMeters = 0.0
        _distanceFlow.value = 0.0
        _speedKmhFlow.value = 0.0
        lastLocation = null
    }

    companion object {
        private const val TAG = "LocationTracker"
    }
}
