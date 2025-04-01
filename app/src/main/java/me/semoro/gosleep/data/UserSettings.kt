package me.semoro.gosleep.data

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atDate
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


/**
 * Data class representing user settings for the GoSleep app
 * @property wakeUpTime The time user wants to wake up
 * @property desiredSleepDuration How long the user wants to sleep
 * @property greenZoneDuration Duration of yellow zone after bedtime (default 30 minutes)
 * @property yellowZoneDuration Duration of red zone after yellow zone (default 30 minutes)
 * @property beepIntervalYellow How often to beep in yellow zone (default 10 minutes)
 * @property beepIntervalRed How often to beep in red zone (default 5 minutes)
 * @property homeWifiSSID SSID of home Wi-Fi network (null if using location instead)
 */
data class UserSettings(
    val wakeUpTime: LocalTime = LocalTime(7, 0), // Default wake up at 7:00
    val desiredSleepDuration: Duration = 8.hours, // Default 8 hours of sleep
    val greenZoneDuration: Duration = 30.minutes, // Default 30 minutes for yellow zone
    val yellowZoneDuration: Duration = 30.minutes, // Default 30 minutes for red zone
    val beepIntervalYellow: Duration = 10.minutes,
    val beepIntervalRed: Duration = 5.minutes,
    val homeWifiSSID: String? = null
) {


    fun calculateWakeUpTime(currentTime: LocalDateTime): Instant {
        val localDate = currentTime.date
        // if the bedtime starts today
        return if (currentTime < wakeUpTime.atDate(localDate)) {
            wakeUpTime.atDate(localDate)
        } else {
            val tomorrowDate = localDate + DatePeriod(days = 1)
            wakeUpTime.atDate(tomorrowDate)
        }.toInstant(TimeZone.currentSystemDefault())
    }

    /**
     * Calculate the recommended bedtime based on wake-up time and desired sleep duration
     * @return LocalTime representing when user should go to bed
     */
    fun calculateBedtimeStart(currentTime: LocalDateTime): Instant = calculateWakeUpTime(currentTime) - desiredSleepDuration

    /**
     * Calculate the start of yellow zone (after bedtime)
     * @return LocalTime representing when yellow zone starts
     */
    fun calculateYellowZoneStart(currentTime: LocalDateTime): Instant = calculateBedtimeStart(currentTime) + greenZoneDuration

    /**
     * Calculate the start of red zone (after yellow zone)
     * @return LocalTime representing when red zone starts
     */
    fun calculateRedZoneStart(currentTime: LocalDateTime): Instant = calculateYellowZoneStart(currentTime) + yellowZoneDuration
}
