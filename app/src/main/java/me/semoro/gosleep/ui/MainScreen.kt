package me.semoro.gosleep.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.semoro.gosleep.ui.components.GeofenceConfigChip
import me.semoro.gosleep.ui.components.SleepDurationSettingsChip
import me.semoro.gosleep.ui.components.TimeDisplay
import me.semoro.gosleep.ui.components.WakeUpTimeChip
import me.semoro.gosleep.ui.components.WifiConfigChip
import me.semoro.gosleep.ui.components.ZoneProgressIndicator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.mainScreenState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        TimeDisplay(
            currentTime = state.currentTime.toLocalDateTime(TimeZone.currentSystemDefault()).time,
            modifier = Modifier.padding(top = 32.dp)
        )


        // Show current zone information
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (state.currentZone) {
                        BedtimeZone.NONE -> "Not bedtime yet"
                        BedtimeZone.GREEN -> "Time to get ready for bed"
                        BedtimeZone.YELLOW -> "You should be in bed by now"
                        BedtimeZone.RED -> "You're significantly past bedtime"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        state.userSettings?.let { settings ->
            ZoneProgressIndicator(
                currentTime = state.currentTime,
                userSettings = settings,
                currentZone = state.currentZone,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }


        FlowRow(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            state.userSettings?.let { settings ->
                val isLocked = viewModel.shouldLockSettings()

                WakeUpTimeChip(
                    currentTime = settings.wakeUpTime,
                    onTimeSelected = { newTime ->
                        // Only allow changes if settings are not locked
                        if (!isLocked) {
                            viewModel.updateWakeUpTime(newTime)
                        }
                    },
                    enabled = !isLocked
                )

                SleepDurationSettingsChip(
                    settings = settings,
                    onChangeSettings = { newSettings ->
                        if (!isLocked) {
                            viewModel.updateSettings(newSettings)
                        }
                    },
                    enabled = !isLocked
                )

                // For components without enabled parameter, we need to handle locking in the onClick handlers
                WifiConfigChip(
                    requestCurrentWifiNameUpdate = {
                        viewModel.checkAndUpdateWifiName()
                    },
                    currentWifiName = state.currentWifiSsid,
                    homeWifiSSID = settings.homeWifiSSID,
                    onUpdateHomeWifiSSID = { ssid ->
                        if (!isLocked) {
                            viewModel.updateHomeWifiSSID(ssid)
                        }
                    },
                    enabled = !isLocked
                )

                GeofenceConfigChip(
                    geofenceSettings = settings.geofenceSettings,
                    onUpdateHomeGeofence = { latitude, longitude, radius ->
                        if (!isLocked) {
                            viewModel.updateHomeGeofence(latitude, longitude, radius)
                        }
                    },
                    enabled = !isLocked
                )

                AssistChip(
                    onClick = {
                        // Only allow changing the toggle if not in bedtime
                        viewModel.updateLockSettingsDuringBedtime(!settings.lockSettingsDuringBedtime)
                    },
                    label = {
                        if (settings.lockSettingsDuringBedtime) {
                            Text("Unlock Settings During Bedtime")
                        } else {
                            Text("Lock Settings During Bedtime")
                        }
                    },
                    leadingIcon = {

                        Icon(
                            painter = if (settings.lockSettingsDuringBedtime) {
                                painterResource(me.semoro.gosleep.R.drawable.baseline_lock_24)
                            } else {
                                painterResource(me.semoro.gosleep.R.drawable.baseline_lock_open_24)
                            },
                            contentDescription = null
                        )
                    },
                    enabled = !isLocked // Can only toggle when not in bedtime
                )
            }

            AssistChip(
                onClick = {
                    viewModel.checkAndUpdateChargingState()
                },
                label = { Text(state.chargingState.plugStatus ?: "Not connected") },
                leadingIcon = {
                    Icon(
                        imageVector = if (state.chargingState.isCharging || state.chargingState.plugStatus != null) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null
                    )
                }
            )


            val nextAlarmTime = remember(state.nextAlarmTime) {
                state.nextAlarmTime?.toLocalDateTime(TimeZone.currentSystemDefault())
            }
            AssistChip(
                onClick = {
                    viewModel.triggerInitialAlarm()
                },
                label = {
                    // get the next alarm time
                    Text(nextAlarmTime?.toString() ?: "<error>")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null
                    )
                }
            )
        }
    }
}
