package me.semoro.gosleep.ui

import android.app.AlarmManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.semoro.gosleep.ui.components.SleepDurationSettingsChip
import me.semoro.gosleep.ui.components.TimeDisplay
import me.semoro.gosleep.ui.components.WakeUpTimeChip
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
                WakeUpTimeChip(
                    currentTime = settings.wakeUpTime,
                    onTimeSelected = { newTime ->
                        // Only allow changes outside bedtime hours
    //                    if (state.currentZone == BedtimeZone.NONE) {
                            viewModel.updateWakeUpTime(newTime)
    //                    }
                    },
                    enabled = true//state.currentZone == BedtimeZone.NONE
                )

                SleepDurationSettingsChip(
                    settings = settings,
                    onChangeSettings = { newSettings ->
                        viewModel.updateSettings(newSettings)
                    },
                    enabled = true
                )


                AssistChip(
                    onClick = { },
                    label = { Text(state.wifiName ?: "No Wifi") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (state.wifiName == settings.homeWifiSSID && settings.homeWifiSSID != null) Icons.Default.Home else Icons.Default.LocationOn,
                            contentDescription = null
                        )
                    }
                )
            }




            AssistChip(
                onClick = { },
                label = { Text(state.chargingState.plugStatus ?: "Not connected") },
                leadingIcon = {
                    Icon(
                        imageVector = if (state.chargingState.isCharging || state.chargingState.plugStatus != null) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null
                    )
                }
            )


            val nextAlarmTime = state.nextAlarmTime
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
