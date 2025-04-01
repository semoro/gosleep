package me.semoro.gosleep.receiver

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import io.mockk.*
import org.junit.Before
import org.junit.Test

class ChargingReceiverTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = mockk(relaxed = true)
    }

    @Test
    fun `test wireless charging detection`() {
        var isCharging = false
        val receiver = ChargingReceiver { isCharging = it }

        // Test power connected with wireless charging
        val connectedIntent = mockk<Intent>()
        every { connectedIntent.action } returns Intent.ACTION_POWER_CONNECTED
        every { connectedIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, any()) } returns BatteryManager.BATTERY_PLUGGED_WIRELESS

        receiver.onReceive(context, connectedIntent)
        assert(isCharging) { "Should detect wireless charging" }

        // Test power connected with wired charging
        val wiredIntent = mockk<Intent>()
        every { wiredIntent.action } returns Intent.ACTION_POWER_CONNECTED
        every { wiredIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, any()) } returns BatteryManager.BATTERY_PLUGGED_AC

        receiver.onReceive(context, wiredIntent)
        assert(!isCharging) { "Should not detect wired charging as wireless" }

        // Test power disconnected
        val disconnectedIntent = mockk<Intent>()
        every { disconnectedIntent.action } returns Intent.ACTION_POWER_DISCONNECTED

        receiver.onReceive(context, disconnectedIntent)
        assert(!isCharging) { "Should detect power disconnected" }
    }

    @Test
    fun `test initial state detection`() {
        var isCharging = false
        val receiver = ChargingReceiver { isCharging = it }

        // Mock battery changed intent for initial state
        val batteryIntent = mockk<Intent>()
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, any()) } returns BatteryManager.BATTERY_PLUGGED_WIRELESS

        // Mock IntentFilter and registerReceiver calls
        mockkConstructor(IntentFilter::class)
        every { anyConstructed<IntentFilter>().addAction(any()) } returns Unit
        every { context.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { context.registerReceiver(receiver, any<IntentFilter>()) } returns null

        ChargingReceiver.register(context, receiver)
        assert(isCharging) { "Should detect initial wireless charging state" }

        // Test unregister
        ChargingReceiver.unregister(context, receiver)
        verify { context.unregisterReceiver(receiver) }
    }
}
