package me.semoro.gosleep.service

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds


object AlarmControl {
    fun setAlarm(context: Context, requestCode: Int, triggerInstant: Instant) {
        println("Setting alarm at $triggerInstant")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Intent to be triggered when the alarm goes off
        val intent = Intent(context, AlarmReceiver::class.java)
        val alarmIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // check if the alarm is outdated and correct.
        val now = Clock.System.now()
        val correctInstant = if (triggerInstant < now) {
            println("Passed incorrect instant to AlarmControl - $triggerInstant")
            now.plus(10.seconds)
        } else {
            triggerInstant
        }

        // Set the alarm
        val alarmClockInfo = AlarmClockInfo(correctInstant.toEpochMilliseconds(), alarmIntent)
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAlarmClock(alarmClockInfo, alarmIntent!!)
        } else {
            println("Failed to set alarm - no permission")
        }
    }

}