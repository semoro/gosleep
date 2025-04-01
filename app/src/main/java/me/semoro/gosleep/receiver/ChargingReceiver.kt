package me.semoro.gosleep.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class ChargingReceiver(
    private val onChargingStateChanged: (Boolean) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val isWirelessCharging = chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS
                onChargingStateChanged(isWirelessCharging)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                onChargingStateChanged(false)
            }
        }
    }

    companion object {
        fun register(context: Context, receiver: ChargingReceiver) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            context.registerReceiver(receiver, filter)

            // Check initial state
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryStatus?.let { intent ->
                val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val isWirelessCharging = chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS
                receiver.onChargingStateChanged(isWirelessCharging)
            }
        }

        fun unregister(context: Context, receiver: ChargingReceiver) {
            context.unregisterReceiver(receiver)
        }
    }
}
