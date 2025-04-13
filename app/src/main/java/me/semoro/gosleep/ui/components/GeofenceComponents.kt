package me.semoro.gosleep.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.semoro.gosleep.service.GeofenceHelper

/**
 * Dialog for configuring the home geofence location
 */
@Composable
fun GeofenceConfigDialog(
    geofenceSettings: me.semoro.gosleep.data.GeofenceSettings,
    onSaveGeofence: (Double?, Double?, Float) -> Unit,
    closeDialog: () -> Unit
) {
    // Use the default radius value (100f) internally, but don't show it to the user
    val defaultRadius = 100f

    // Initialize with existing coordinates or default to a central position
    val initialPosition = remember {
        if (geofenceSettings.isSet() && geofenceSettings.latitude != null && geofenceSettings.longitude != null) {
            LatLng(geofenceSettings.latitude, geofenceSettings.longitude)
        } else {
            LatLng(0.0, 0.0) // Default to center of the map
        }
    }


    // State for the marker position
    val markerState = rememberSaveable(saver = MarkerState.Saver) {
        MarkerState(initialPosition)
    }

    val currentPositionFlow = remember { MutableStateFlow<Location?>(null) }


    val context = LocalContext.current
    LaunchedEffect(Unit) {
        println("Request location")
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("No location permission")
            return@LaunchedEffect
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        println("Last known location: ${lastKnownLocation}")
        if (lastKnownLocation != null) {
            currentPositionFlow.emit(lastKnownLocation)
        }
    }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = { closeDialog() },
        title = { Text("Set Home Location") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Tap on the map to select your home location. The app will use this to determine if you're at home.",
                    style = MaterialTheme.typography.bodyMedium
                )


                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(markerState.position, 10f)
                }

                val scope = rememberCoroutineScope()
                val locationSource = remember {
                    object : LocationSource {
                        var job: Job? = null

                        override fun activate(p0: LocationSource.OnLocationChangedListener) {
                            job = scope.launch {
                                currentPositionFlow.collect {
                                    if (it != null) {
                                        p0.onLocationChanged(it)
                                    }
                                }
                            }
                        }

                        override fun deactivate() {
                            job?.cancel()
                        }

                    }
                }


                GoogleMap(
                    modifier = Modifier
                        .fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = true
                    ),
                    properties = MapProperties(isMyLocationEnabled = true),
                    locationSource = locationSource,
                    onMapClick = { latLng ->
                        // Update marker position when map is clicked
                        markerState.position = latLng
                    }
                ) {
                    Circle(markerState.position, radius = 100.0, fillColor = Color.Transparent)
                    Marker(
                        state = markerState,
                        title = "Home Location",
                        snippet = "Tap to select this location"
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Parse input values
                    onSaveGeofence(
                        markerState.position.latitude,
                        markerState.position.longitude, defaultRadius)
                    closeDialog()
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        onSaveGeofence(null, null, defaultRadius)
                        closeDialog()
                    }
                ) {
                    Text("Remove")
                }
                TextButton(
                    onClick = { closeDialog() }
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

/**
 * Chip for displaying and configuring the home geofence location
 */
@Composable
fun GeofenceConfigChip(
    geofenceSettings: me.semoro.gosleep.data.GeofenceSettings,
    onUpdateHomeGeofence: (Double?, Double?, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Check if device is within geofence
    val isInGeofence = remember(geofenceSettings) {
        if (geofenceSettings.isSet()) {
            val userSettings = me.semoro.gosleep.data.UserSettings(
                geofenceSettings = geofenceSettings
            )
            GeofenceHelper.isWithinGeofence(context, userSettings)
        } else {
            false
        }
    }


    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(
            onClick = {
                showDialog = true
            },
            label = { 
                if (geofenceSettings.isSet()) {
                    Text("Home Location")
                } else {
                    Text("Set Home Location")
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isInGeofence) Icons.Default.Home else Icons.Default.LocationOn,
                    contentDescription = null
                )
            },
            modifier = modifier
        )
    }

    if (showDialog) {
        GeofenceConfigDialog(
            geofenceSettings = geofenceSettings,
            onSaveGeofence = onUpdateHomeGeofence,
            closeDialog = { showDialog = false }
        )
    }
}
