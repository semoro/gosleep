package me.semoro.gosleep.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import me.semoro.gosleep.R
import me.semoro.gosleep.ui.BedtimeZone
import me.semoro.gosleep.ui.FullScreenAlarmActivity

object AlarmNotification {
    private const val CHANNEL_ID = "alarm_channel"

    fun showAlarmNotification(context: Context, zone: BedtimeZone = BedtimeZone.NONE) {
        // Create an explicit intent for an Activity in your app
        val intent = Intent(context, FullScreenAlarmActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("zone", zone.name)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create notification channel if necessary
        createNotificationChannel(context)


        val notificationText = when (zone) {
            BedtimeZone.GREEN -> "It's bedtime! Time to prepare for sleep."
            BedtimeZone.YELLOW -> "You should be in bed by now!"
            BedtimeZone.RED -> "GO TO SLEEP NOW!"
            BedtimeZone.NONE -> "No alarm"
        }

        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_alarm_24)
            .setContentTitle("Go sleep")
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setSound(null)


        // Show the notification
        val notificationManager = context.getSystemService<NotificationManager>(NotificationManager::class.java)
        notificationManager.notify(0, builder.build())

    }

    private fun createNotificationChannel(context: Context) {
        val name: CharSequence = "Alarm Notifications"
        val description = "Channel for alarm notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        channel.setBypassDnd(true)
        channel.setDescription(description)
        channel.setSound(null, null)

        // Register the channel with the system
        val notificationManager = context.getSystemService<NotificationManager>(NotificationManager::class.java)
        notificationManager.deleteNotificationChannel(CHANNEL_ID)
        notificationManager.createNotificationChannel(channel)
    }
}
