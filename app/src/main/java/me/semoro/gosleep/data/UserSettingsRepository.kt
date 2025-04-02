package me.semoro.gosleep.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime
import me.semoro.gosleep.HOME_SSID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserSettingsRepository(private val context: Context) {
    private object PreferencesKeys {
        val WAKE_UP_TIME_HOUR = intPreferencesKey("wake_up_time_hour")
        val WAKE_UP_TIME_MINUTE = intPreferencesKey("wake_up_time_minute")
        val DESIRED_SLEEP_DURATION = longPreferencesKey("desired_sleep_duration")
        val YELLOW_ZONE_DURATION = longPreferencesKey("yellow_zone_duration")
        val RED_ZONE_DURATION = longPreferencesKey("red_zone_duration")
        val BEEP_INTERVAL_YELLOW = longPreferencesKey("beep_interval_yellow")
        val BEEP_INTERVAL_RED = longPreferencesKey("beep_interval_red")
        val HOME_WIFI_SSID = stringPreferencesKey("home_wifi_ssid")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val hour = preferences[PreferencesKeys.WAKE_UP_TIME_HOUR] ?: 7
        val minute = preferences[PreferencesKeys.WAKE_UP_TIME_MINUTE] ?: 0
        val sleepDuration = preferences[PreferencesKeys.DESIRED_SLEEP_DURATION]?.milliseconds
            ?: 8.hours
        val yellowZoneDuration = preferences[PreferencesKeys.YELLOW_ZONE_DURATION]?.milliseconds
            ?: 30.minutes
        val redZoneDuration = preferences[PreferencesKeys.RED_ZONE_DURATION]?.milliseconds
            ?: 30.minutes
//        val yellowInterval = preferences[PreferencesKeys.BEEP_INTERVAL_YELLOW]?.milliseconds
//            ?: 10.minutes
//        val redInterval = preferences[PreferencesKeys.BEEP_INTERVAL_RED]?.milliseconds
//            ?: 5.minutes
        val homeWifiSSID = preferences[PreferencesKeys.HOME_WIFI_SSID] ?: HOME_SSID

        UserSettings(
            wakeUpTime = LocalTime(hour, minute, 0, 0),
            desiredSleepDuration = sleepDuration,
            greenZoneDuration = yellowZoneDuration,
            yellowZoneDuration = redZoneDuration,
//            beepIntervalYellow = yellowInterval,
//            beepIntervalRed = redInterval,
            homeWifiSSID = homeWifiSSID
        )
    }

    suspend fun updateWakeUpTime(time: LocalTime) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WAKE_UP_TIME_HOUR] = time.hour
            preferences[PreferencesKeys.WAKE_UP_TIME_MINUTE] = time.minute
        }
    }

    suspend fun updateSleepDuration(duration: Duration) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DESIRED_SLEEP_DURATION] = duration.inWholeMilliseconds
        }
    }

    suspend fun updateBeepIntervals(yellow: Duration, red: Duration) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BEEP_INTERVAL_YELLOW] = yellow.inWholeMilliseconds
            preferences[PreferencesKeys.BEEP_INTERVAL_RED] = red.inWholeMilliseconds
        }
    }

    suspend fun updateHomeWifiSSID(ssid: String?) {
        context.dataStore.edit { preferences ->
            if (ssid != null) {
                preferences[PreferencesKeys.HOME_WIFI_SSID] = ssid
            } else {
                preferences.remove(PreferencesKeys.HOME_WIFI_SSID)
            }
        }
    }

    suspend fun updateZoneDurations(yellowZoneDuration: Duration, redZoneDuration: Duration) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.YELLOW_ZONE_DURATION] = yellowZoneDuration.inWholeMilliseconds
            preferences[PreferencesKeys.RED_ZONE_DURATION] = redZoneDuration.inWholeMilliseconds
        }
    }
}
