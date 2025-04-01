package me.semoro.gosleep.data

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalTime
import java.time.Duration

class UserSettingsTest {
    @Test
    fun `test default values`() {
        val settings = UserSettings()
        assertEquals(LocalTime.of(7, 0), settings.wakeUpTime)
        assertEquals(Duration.ofHours(8), settings.desiredSleepDuration)
        assertEquals(Duration.ofMinutes(10), settings.beepIntervalYellow)
        assertEquals(Duration.ofMinutes(5), settings.beepIntervalRed)
        assertNull(settings.homeWifiSSID)
    }

    @Test
    fun `test bedtime calculation`() {
        val settings = UserSettings(
            wakeUpTime = LocalTime.of(7, 0),
            desiredSleepDuration = Duration.ofHours(8)
        )
        assertEquals(LocalTime.of(23, 0), settings.calculateBedtime())
    }

    @Test
    fun `test yellow zone calculation`() {
        val settings = UserSettings(
            wakeUpTime = LocalTime.of(7, 0),
            desiredSleepDuration = Duration.ofHours(8)
        )
        assertEquals(LocalTime.of(23, 30), settings.calculateYellowZoneStart())
    }

    @Test
    fun `test red zone calculation`() {
        val settings = UserSettings(
            wakeUpTime = LocalTime.of(7, 0),
            desiredSleepDuration = Duration.ofHours(8)
        )
        assertEquals(LocalTime.of(0, 0), settings.calculateRedZoneStart())
    }

    @Test
    fun `test custom values`() {
        val settings = UserSettings(
            wakeUpTime = LocalTime.of(8, 30),
            desiredSleepDuration = Duration.ofHours(7),
            beepIntervalYellow = Duration.ofMinutes(15),
            beepIntervalRed = Duration.ofMinutes(7),
            homeWifiSSID = "MyHomeWifi"
        )
        
        assertEquals(LocalTime.of(8, 30), settings.wakeUpTime)
        assertEquals(Duration.ofHours(7), settings.desiredSleepDuration)
        assertEquals(Duration.ofMinutes(15), settings.beepIntervalYellow)
        assertEquals(Duration.ofMinutes(7), settings.beepIntervalRed)
        assertEquals("MyHomeWifi", settings.homeWifiSSID)
        
        assertEquals(LocalTime.of(1, 30), settings.calculateBedtime())
        assertEquals(LocalTime.of(2, 0), settings.calculateYellowZoneStart())
        assertEquals(LocalTime.of(2, 30), settings.calculateRedZoneStart())
    }
}