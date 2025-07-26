package me.semoro.gosleep.ui

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import me.semoro.gosleep.data.UserSettings
import me.semoro.gosleep.data.UserSettingsRepository
import me.semoro.gosleep.service.AlarmControl
import me.semoro.gosleep.service.AlarmTriggerPrecondition
import java.util.*
import kotlin.time.Duration.Companion.seconds

data class MainScreenState(
    val currentTime: Instant,
    val userSettings: UserSettings? = null,
    val currentWifiSsid: String? = null,
    val chargingState: AlarmTriggerPrecondition.ChargingState = AlarmTriggerPrecondition.ChargingState(false, null),
    val currentZone: BedtimeZone = BedtimeZone.NONE,
    val nextAlarmTime: Instant? = null
)

enum class BedtimeZone {
    NONE,
    GREEN,
    YELLOW,
    RED
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val userSettingsRepository = UserSettingsRepository(application)
    private val timer = Timer()

    private val _currentTime = MutableStateFlow(Clock.System.now())




    private val currentWifiSsid = MutableStateFlow(run {
        AlarmTriggerPrecondition.getWifiNetwork(application)
    })
    private val chargingState = MutableStateFlow(run {
        AlarmTriggerPrecondition.getChargingState(application)
    })

    init {
        startTimeUpdates()
        userSettingsRepository.userSettingsFlow.onEach { settings ->
            triggerInitialAlarm()
        }.launchIn(viewModelScope)
    }


    fun triggerInitialAlarm() {
        AlarmControl.setAlarm(getApplication(), 0, Clock.System.now() + 10.seconds)
    }

    private fun startTimeUpdates() {
        timer.schedule(object : TimerTask() {
            override fun run() {
                _currentTime.value = Clock.System.now()
            }
        }, 0, 1000) // Update every second
    }

    override fun onCleared() {
        super.onCleared()
        timer.cancel()
    }

    val mainScreenState: StateFlow<MainScreenState> = combine(
        _currentTime,
        userSettingsRepository.userSettingsFlow,
        currentWifiSsid,
        chargingState
    ) { currentTime, userSettings, currentWifiSsid, chargingState ->
        val alarmManager = application.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        MainScreenState(
            currentTime = currentTime,
            userSettings = userSettings,
            currentWifiSsid = currentWifiSsid,
            chargingState = chargingState,
            currentZone = userSettings.calculateCurrentZone(currentTime),
            nextAlarmTime = alarmManager.nextAlarmClock?.triggerTime?.let { Instant.fromEpochMilliseconds(it) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainScreenState(Clock.System.now())
    )

    fun updateWakeUpTime(time: LocalTime) {
        viewModelScope.launch {
            userSettingsRepository.updateWakeUpTime(time)
        }
    }

    fun updateSettings(settings: UserSettings) {
        viewModelScope.launch {
            userSettingsRepository.updateWakeUpTime(settings.wakeUpTime)
            userSettingsRepository.updateSleepDuration(settings.desiredSleepDuration)
            userSettingsRepository.updateZoneDurations(settings.greenZoneDuration, settings.yellowZoneDuration)
            userSettingsRepository.updateBeepIntervals(settings.beepIntervalYellow, settings.beepIntervalRed)
            userSettingsRepository.updateHomeWifiSSID(settings.homeWifiSSID)
        }
    }

    fun checkAndUpdateChargingState() {
        chargingState.value = AlarmTriggerPrecondition.getChargingState(getApplication())
    }

    fun checkAndUpdateWifiName() {
        currentWifiSsid.value = AlarmTriggerPrecondition.getWifiNetwork(getApplication())
    }

    fun updateHomeWifiSSID(ssid: String?) {
        viewModelScope.launch {
            userSettingsRepository.updateHomeWifiSSID(ssid)
        }
    }

    /**
     * Update home geofence coordinates
     * @param latitude Latitude of home location
     * @param longitude Longitude of home location
     * @param radius Radius of geofence in meters
     */
    fun updateHomeGeofence(latitude: Double?, longitude: Double?, radius: Float = 100f) {
        viewModelScope.launch {
            // Create GeofenceSettings object
            val geofenceSettings = me.semoro.gosleep.data.GeofenceSettings(
                latitude = latitude,
                longitude = longitude,
                radius = radius
            )

            // Update geofence settings
            userSettingsRepository.updateHomeGeofence(geofenceSettings)

            // Set up geofencing after updating coordinates
            if (geofenceSettings.isSet()) {
                val geofenceHelper = me.semoro.gosleep.service.GeofenceHelper(getApplication())
                val userSettings = userSettingsRepository.userSettingsFlow.first()
                geofenceHelper.setupGeofencing(userSettings)
            }
        }
    }

    /**
     * Update the setting to lock settings during bedtime
     * @param lock Boolean value indicating whether to lock settings during bedtime
     */
    fun updateLockSettingsDuringBedtime(lock: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.updateLockSettingsDuringBedtime(lock)
        }
    }

    /**
     * Check if settings should be locked based on current zone and user preference
     * @return true if settings should be locked, false otherwise
     */
    fun shouldLockSettings(): Boolean {
        val state = mainScreenState.value
        // Lock settings if it's bedtime (any zone other than NONE) and the lock setting is enabled
        return state.userSettings?.lockSettingsDuringBedtime == true && 
               state.currentZone != BedtimeZone.NONE
    }
}
