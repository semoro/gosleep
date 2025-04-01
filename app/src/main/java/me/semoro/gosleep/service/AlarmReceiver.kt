package me.semoro.gosleep.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        println("Received intent alarm: $intent")
        // determine current zone and set next alarm

    }
}