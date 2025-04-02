package me.semoro.gosleep.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import me.semoro.gosleep.R

/**
 * Dialog for configuring the home WiFi SSID
 */
@Composable
fun WifiConfigDialog(
    currentWifiSsid: String?,
    currentHomeSSID: String?,
    onSaveSSID: (String?) -> Unit,
    closeDialog: () -> Unit
) {
    var homeSsidText by remember { mutableStateOf(currentHomeSSID ?: "") }

    AlertDialog(
        onDismissRequest = { closeDialog() },
        title = { Text(stringResource(R.string.wifi_dialog_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.wifi_dialog_message))
                
                OutlinedTextField(
                    value = homeSsidText,
                    onValueChange = { homeSsidText = it },
                    label = { Text(stringResource(R.string.wifi_dialog_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { homeSsidText = currentWifiSsid ?: "" }) {
                    Text("Set to \"$currentWifiSsid\"")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // If the text is empty, pass null to clear the setting
                    onSaveSSID(if (homeSsidText.isBlank()) null else homeSsidText)
                    closeDialog()
                }
            ) {
                Text(stringResource(R.string.wifi_dialog_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = { closeDialog() }
                ) {
                    Text(stringResource(R.string.wifi_dialog_cancel))
                }
                TextButton(
                    onClick = {
                        homeSsidText = ""
                        onSaveSSID(null)
                        closeDialog()
                    }
                ) {
                    Text(stringResource(R.string.wifi_dialog_clear))
                }
            }
        }
    )
}

/**
 * Chip for displaying and configuring the home WiFi SSID
 */
@Composable
fun WifiConfigChip(
    currentWifiName: String?,
    homeWifiSSID: String?,
    requestCurrentWifiNameUpdate: () -> Unit,
    onUpdateHomeWifiSSID: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    AssistChip(
        onClick = {
            requestCurrentWifiNameUpdate()
            showDialog = true
        },
        label = { Text(currentWifiName ?: "No Wifi") },
        leadingIcon = {

            val image = if (currentWifiName == homeWifiSSID && homeWifiSSID != null)
                Icons.Default.Home
            else
                ImageVector.vectorResource(R.drawable.baseline_not_listed_location_24)


            Icon(
                imageVector = image,
                contentDescription = null
            )
        },
        modifier = modifier
    )

    if (showDialog) {
        WifiConfigDialog(
            currentWifiSsid = currentWifiName,
            currentHomeSSID = homeWifiSSID,
            onSaveSSID = onUpdateHomeWifiSSID,
            closeDialog = { showDialog = false }
        )
    }
}