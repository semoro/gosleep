package me.semoro.gosleep.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.semoro.gosleep.data.UserSettingsRepository
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Receiver for geofence transition events
 */
class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        // Check if the geofence transition is an enter event
        if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            println("Entered geofenced area")

            // Trigger a re-check of the bedtime condition
            runBlocking {
                // Get user settings
                val userSettingsRepository = UserSettingsRepository(context)
                val userSettings = userSettingsRepository.userSettingsFlow.first()

                // Calculate current bedtime zone
                val currentTime = Clock.System.now()
                val currentZone = userSettings.calculateCurrentZone(currentTime)

                println("Geofence entered at $currentTime, current zone: $currentZone")

                // Set an immediate alarm to trigger the bedtime check
                AlarmControl.setAlarm(
                    context,
                    0,
                    Clock.System.now().plus(1.seconds) // 1 second delay
                )
            }
        }
    }
}
