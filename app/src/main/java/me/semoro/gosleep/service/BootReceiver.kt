package me.semoro.gosleep.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.semoro.gosleep.data.UserSettingsRepository
import me.semoro.gosleep.ui.BedtimeZone
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * BroadcastReceiver that handles device boot completed events.
 * Restores alarms after device reboot to ensure they continue to function properly.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            // Set the next alarm within a minute, it will reschedule properly afterward
            AlarmControl.setAlarm(
                context,
                0,
                Clock.System.now() + 1.minutes
            )
        }
    }
}