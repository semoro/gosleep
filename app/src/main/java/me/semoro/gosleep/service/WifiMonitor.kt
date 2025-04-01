package me.semoro.gosleep.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class WifiMonitor(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun monitorHomeWifi(homeSSID: String?): Flow<Boolean> = callbackFlow {
        if (homeSSID == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                if (isWifi) {
                    val info = wifiManager.connectionInfo
                    val currentSSID = info.ssid.trim('"')
                    trySend(currentSSID == homeSSID)
                }
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                if (isWifi) {
                    val info = wifiManager.connectionInfo
                    val currentSSID = info.ssid.trim('"')
                    trySend(currentSSID == homeSSID)
                } else {
                    trySend(false)
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)

        // Check initial state
        val currentNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(currentNetwork)
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        if (isWifi) {
            val info = wifiManager.connectionInfo
            val currentSSID = info.ssid.trim('"')
            trySend(currentSSID == homeSSID)
        } else {
            trySend(false)
        }

        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
}