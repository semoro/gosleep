package me.semoro.gosleep.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalTime
import me.semoro.gosleep.data.UserSettings
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.minutes

@Composable
fun TimeDisplay(
    currentTime: LocalTime,
    modifier: Modifier = Modifier
) {
    Text(
        text = currentTime.toJavaLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
        style = MaterialTheme.typography.displayLarge,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeUpTimeChip(
    currentTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    // Format time for display
    val timeString = currentTime.toJavaLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))


    AssistChip(
        onClick = { if (enabled) showDialog = true },
        label = { Text(timeString) },
        leadingIcon = {
            Icon(
                painter = painterResource(me.semoro.gosleep.R.drawable.baseline_alarm_24),
                contentDescription = null
            )
        },
        enabled = enabled
    )


    if (showDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = currentTime.hour,
            initialMinute = currentTime.minute
        )

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select Wake Up Time") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onTimeSelected(LocalTime(timePickerState.hour, timePickerState.minute))
                        showDialog = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun SleepSettingsDialog(
    settings: UserSettings,
    onChangeSettings: (UserSettings) -> Unit,
    closeDialog: () -> Unit
) {
    // Initialize slider positions
    var sleepDurationSliderPosition by remember {
        val minutes = settings.desiredSleepDuration.inWholeMinutes.toFloat()
        mutableStateOf(minutes)
    }

    var greenZoneDurationSlider = remember {
        val minutes = settings.greenZoneDuration.inWholeMinutes.toFloat()
        mutableStateOf(minutes)
    }

    var yellowZoneDurationSlider = remember {
        val minutes = settings.yellowZoneDuration.inWholeMinutes.toFloat()
        mutableStateOf(minutes)
    }

    AlertDialog(
        onDismissRequest = { closeDialog() },
        title = { Text("Sleep Settings") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Sleep Duration Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Sleep Duration",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Display the selected duration
                    val sleepDurationInMinutes = sleepDurationSliderPosition.toInt().minutes
                    val sleepDurationText = sleepDurationInMinutes.toString()

                    Text(
                        text = sleepDurationText,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Slider for selecting sleep duration
                    Slider(
                        value = sleepDurationSliderPosition,
                        onValueChange = { newValue ->
                            sleepDurationSliderPosition = newValue
                        },
                        valueRange = (5f * 60)..(12f * 60),
                        steps = (12 - 5) * 2 - 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }


                ZoneDurationSlider(greenZoneDurationSlider, "Green Zone Duration")
                ZoneDurationSlider(yellowZoneDurationSlider, "Yellow Zone Duration")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onChangeSettings(settings.copy(
                        desiredSleepDuration = sleepDurationSliderPosition.toInt().minutes,
                        greenZoneDuration = greenZoneDurationSlider.value.toInt().minutes,
                        yellowZoneDuration = yellowZoneDurationSlider.value.toInt().minutes
                    ))
                    closeDialog()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { closeDialog() }
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ZoneDurationSlider(
    state: MutableState<Float>,
    text: String,
) {
    // Green Zone Duration Section
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )

        // Display the selected duration
        val greenZoneDurationInMinutes = state.value.toInt().minutes
        val greenZoneDurationText = greenZoneDurationInMinutes.toString()

        Text(
            text = greenZoneDurationText,
            style = MaterialTheme.typography.bodyMedium
        )

        // Slider for selecting yellow zone duration
        Slider(
            value = state.value,
            onValueChange = { newValue ->
                state.value = newValue
            },
            valueRange = 10f..60f,
            steps = 4,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun SleepDurationSettingsChip(
    settings: UserSettings,
    onChangeSettings: (UserSettings) -> Unit,
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    val currentDuration = settings.desiredSleepDuration

    // Format duration for display
    // Convert duration to a float representing hours
    val durationString = currentDuration.toString()

    AssistChip(
        onClick = { if (enabled) showDialog = true },
        label = { Text(durationString) },
        leadingIcon = {
            Icon(
                painter = painterResource(me.semoro.gosleep.R.drawable.baseline_bedtime_24),
                contentDescription = null
            )
        },
        enabled = enabled
    )

    if (showDialog) {
        SleepSettingsDialog(
            settings = settings,
            onChangeSettings = onChangeSettings,
            closeDialog = { showDialog = false }
        )
    }
}


@Preview
@Composable
fun SleepSettingsDialogPreview() {
    var settings by remember { mutableStateOf(UserSettings()) }

    SleepSettingsDialog(settings, {
        settings = it
    }, {
        // do nothing
    })
}