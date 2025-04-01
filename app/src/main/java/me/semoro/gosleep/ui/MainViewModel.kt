package me.semoro.gosleep.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import me.semoro.gosleep.data.UserSettings
import me.semoro.gosleep.data.UserSettingsRepository
import me.semoro.gosleep.receiver.ChargingReceiver
import me.semoro.gosleep.service.AlarmControl
import java.util.*
import kotlin.time.Duration.Companion.seconds

data class MainScreenState(
    val currentTime: Instant,
    val userSettings: UserSettings? = null,
    val isAtHome: Boolean = false,
    val isCharging: Boolean = false,
    val currentZone: BedtimeZone = BedtimeZone.NONE
)

enum class BedtimeZone {
    NONE,
    GREEN,
    YELLOW,
    RED
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val userSettingsRepository = UserSettingsRepository(application)
//    private val bedtimeManager = BedtimeManager(application)
    private val chargingReceiver = ChargingReceiver { isCharging ->
        _isCharging.value = isCharging
//        if (isCharging) {
//            bedtimeManager.cancelBeep()
//        }
    }
    private val timer = Timer()

    private val _currentTime = MutableStateFlow(Clock.System.now())
    private val _isAtHome = MutableStateFlow(false)
    private val _isCharging = MutableStateFlow(false)

    init {
        startTimeUpdates()
        userSettingsRepository.userSettingsFlow.onEach { settings ->
            AlarmControl.setAlarm(application.applicationContext,
                0, Clock.System.now() + 1.seconds
            )
        }.launchIn(viewModelScope)
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
        ChargingReceiver.unregister(getApplication(), chargingReceiver)
//        bedtimeManager.cancelBeep()
    }

    val mainScreenState: StateFlow<MainScreenState> = combine(
        _currentTime,
        userSettingsRepository.userSettingsFlow,
        _isAtHome,
        _isCharging
    ) { currentTime, userSettings, isAtHome, isCharging ->
        MainScreenState(
            currentTime = currentTime,
            userSettings = userSettings,
            isAtHome = isAtHome,
            isCharging = isCharging,
            currentZone = calculateCurrentZone(currentTime, userSettings)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainScreenState(Clock.System.now())
    )

    private fun calculateCurrentZone(instant: Instant, settings: UserSettings?): BedtimeZone {
        if (settings == null) return BedtimeZone.NONE

        val currentDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val newZone = when {
            instant < settings.calculateBedtimeStart(currentDateTime) -> BedtimeZone.NONE
            instant < settings.calculateYellowZoneStart(currentDateTime) -> BedtimeZone.GREEN
            instant < settings.calculateRedZoneStart(currentDateTime) -> BedtimeZone.YELLOW
            else -> BedtimeZone.RED
        }

        return newZone
    }

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
