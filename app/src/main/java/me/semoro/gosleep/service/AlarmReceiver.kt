package me.semoro.gosleep.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.semoro.gosleep.data.UserSettingsRepository
import me.semoro.gosleep.ui.BedtimeZone
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        println("Received intent alarm: $intent")

        // wake lock to finish the check


        runBlocking {
            // Get user settings
            val userSettingsRepository = UserSettingsRepository(context)
            val userSettings = userSettingsRepository.userSettingsFlow.first()

            // Check if device is at home (connected to home WiFi)
            val currentSsid = AlarmTriggerPrecondition.getWifiNetwork(context)
            val isAtHome = userSettings.homeWifiSSID != null && currentSsid == userSettings.homeWifiSSID

            // Check if device is charging
            val (isCharging, plugStatus) = AlarmTriggerPrecondition.getChargingState(context)

            // Calculate current bedtime zone
            val currentTime = Clock.System.now()
            val currentDateTime = currentTime.toLocalDateTime(TimeZone.currentSystemDefault())

            val currentZone = userSettings.calculateCurrentZone(currentTime)

            println("Alarm received at $currentTime, current zone: $currentZone, isAtHome: $isAtHome, isCharging: $isCharging")


            val bedtimeStart = userSettings.calculateBedtimeStart(currentDateTime)

            // Set the next alarm based on the current zone
            var nextAlarmTime = when (currentZone) {
                BedtimeZone.NONE -> bedtimeStart + 1.seconds
                BedtimeZone.GREEN -> Clock.System.now() + userSettings.beepIntervalGreen
                BedtimeZone.YELLOW ->  Clock.System.now() + userSettings.beepIntervalYellow
                BedtimeZone.RED ->  Clock.System.now() + userSettings.beepIntervalRed
            }



            // If it's bedtime and the user is at home or the device is charging, show the alarm
            if (currentZone != BedtimeZone.NONE) {
                if (isAtHome) {
                    if (isCharging || plugStatus != null) {
                        println("Declining alarm: Charging (isCharging=$isCharging), (plugStatus=$plugStatus) => sleeping")

                        val in1day = Clock.System.now().plus(1.days).toLocalDateTime(TimeZone.currentSystemDefault())

                        nextAlarmTime = userSettings.calculateBedtimeStart(in1day) + 1.seconds
                    } else {
                        println("Showing alarm")
                        // Start FullScreenAlarmActivity
                        AlarmNotification.showAlarmNotification(context, currentZone)
                    }
                } else {
                    println("Declining alarm: Not at home (${currentSsid} != ${userSettings.homeWifiSSID})")
                    // Re-try in an hour
                    nextAlarmTime = Clock.System.now() + 60.minutes
                }
            } else {
                println("Declining alarm: Not in bedtime")
                // Let it reschedule.
            }



            // Set the next alarm
            AlarmControl.setAlarm(
                context,
                0,
                nextAlarmTime
            )
        }
    }
}

object AlarmTriggerPrecondition {
    data class ChargingState(val isCharging: Boolean, val plugStatus: String?)

    fun getWifiNetwork(context: Context): String? {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val currentSsid = wifiInfo?.ssid?.replace("\"", "") // Remove quotes from SSID
        return currentSsid
    }

    fun getChargingState(context: Context): ChargingState {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
        val plugStatus = when (
            batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        ) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> null
        }
        return ChargingState(isCharging, plugStatus)
    }
}