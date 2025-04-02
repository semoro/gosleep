package me.semoro.gosleep.ui

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import me.semoro.gosleep.data.UserSettings
import me.semoro.gosleep.data.UserSettingsRepository
import me.semoro.gosleep.service.AlarmControl
import me.semoro.gosleep.service.AlarmTriggerPrecondition
import java.util.*
import kotlin.time.Duration.Companion.seconds

data class MainScreenState(
    val currentTime: Instant,
    val userSettings: UserSettings? = null,
    val wifiName: String? = null,
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




    private val wifiName = MutableStateFlow(run {
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
        wifiName,
        chargingState
    ) { currentTime, userSettings, wifiName, chargingState ->
        val alarmManager = application.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        MainScreenState(
            currentTime = currentTime,
            userSettings = userSettings,
            wifiName = wifiName,
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


}
