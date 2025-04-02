package me.semoro.gosleep

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import me.semoro.gosleep.ui.MainScreen
import me.semoro.gosleep.ui.theme.GoSleepTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        


        enableEdgeToEdge()
        checkAndRequestPermissions()
        setContent {
            GoSleepTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }


    private fun checkAndRequestPermissions() {
        println("Checking permissions: $packageName")

        val alarmManager = applicationContext.getSystemService(AlarmManager::class.java)


        if (!alarmManager.canScheduleExactAlarms()) {
            println("Requesting permissions")
            val intent = Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.canUseFullScreenIntent()) {
                // Prompt user to grant permission
                val intent = Intent(ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }
}
