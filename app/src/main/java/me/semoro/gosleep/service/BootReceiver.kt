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
import kotlin.time.Duration.Companion.seconds

/**
 * BroadcastReceiver that handles device boot completed events.
 * Restores alarms after device reboot to ensure they continue to function properly.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            println("Boot completed, restoring alarm")
            
            runBlocking {
                // Get user settings
                val userSettingsRepository = UserSettingsRepository(context)
                val userSettings = userSettingsRepository.userSettingsFlow.first()
                
                // Calculate current time and bedtime zone
                val currentTime = Clock.System.now()
                val currentDateTime = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())
                val currentZone = userSettings.calculateCurrentZone(currentTime)
                
                println("Boot receiver restoring alarm at $currentTime, current zone: $currentZone")
                
                // Set the next alarm based on the current zone
                val nextAlarmTime = when (currentZone) {
                    BedtimeZone.NONE -> userSettings.calculateBedtimeStart(currentDateTime) + 1.seconds
                    BedtimeZone.GREEN -> Clock.System.now() + userSettings.beepIntervalGreen
                    BedtimeZone.YELLOW -> Clock.System.now() + userSettings.beepIntervalYellow
                    BedtimeZone.RED -> Clock.System.now() + userSettings.beepIntervalRed
                }
                
                // Set the next alarm
                AlarmControl.setAlarm(
                    context,
                    0,
                    nextAlarmTime
                )
                
                println("Alarm restored after boot, next alarm at: $nextAlarmTime")
            }
        }
    }
}