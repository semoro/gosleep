package me.semoro.gosleep.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import me.semoro.gosleep.data.UserSettings

/**
 * Helper class for geofencing operations
 */
class GeofenceHelper(private val context: Context) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    companion object {
        const val GEOFENCE_ID = "HOME_GEOFENCE"

        /**
         * Check if the device is within the geofenced area
         * @param userSettings User settings containing geofence coordinates
         * @return true if within geofence, false otherwise
         */
        fun isWithinGeofence(context: Context, userSettings: UserSettings): Boolean {
            // If geofence coordinates are not set, return false
            if (!userSettings.geofenceSettings.isSet()) {
                return false
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Check for location permission
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }

            // Get last known location
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (lastKnownLocation == null) {
                return false
            }

            // Calculate distance between current location and geofence center
            val distance = calculateDistance(
                lastKnownLocation.latitude, lastKnownLocation.longitude,
                userSettings.geofenceSettings.latitude!!, userSettings.geofenceSettings.longitude!!
            )

            // Check if within radius
            return distance <= userSettings.geofenceSettings.radius
        }

        /**
         * Calculate distance between two coordinates using simple Euclidean distance
         * This is a simplified calculation and not as accurate as the haversine formula for long distances
         */
        private fun calculateDistance(
            lat1: Double, lon1: Double,
            lat2: Double, lon2: Double
        ): Float {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            return results[0]
        }
    }

    /**
     * Set up geofence monitoring
     * @param userSettings User settings containing geofence coordinates
     */
    fun setupGeofencing(userSettings: UserSettings) {
        // If geofence coordinates are not set, return
        if (!userSettings.geofenceSettings.isSet()) {
            removeGeofencing()
            return
        }

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Get geofence settings
        val geofenceSettings = userSettings.geofenceSettings
        val latitude = geofenceSettings.latitude
        val longitude = geofenceSettings.longitude

        // Double-check that latitude and longitude are not null
        if (latitude == null || longitude == null) {
            return
        }

        // Create geofence
        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(
                latitude,
                longitude,
                geofenceSettings.radius
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        // Create geofencing request
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        // Create pending intent for geofence transitions
        val geofencePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GeofenceReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Register geofence
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
    }

    /**
     * Remove geofence monitoring
     */
    fun removeGeofencing() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        geofencingClient.removeGeofences(
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, GeofenceReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }
}
