package me.semoro.gosleep.service

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import androidx.work.*
import kotlinx.coroutines.flow.*
import me.semoro.gosleep.R
import me.semoro.gosleep.data.UserSettings
import me.semoro.gosleep.ui.BedtimeZone
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class BedtimeManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentBeepWorkRequest: OneTimeWorkRequest? = null

    fun scheduleBeep(zone: BedtimeZone, settings: UserSettings) {
        // Cancel any existing beep schedule
        cancelBeep()


        // Only schedule beeps for yellow and red zones
        val interval = when (zone) {
            BedtimeZone.YELLOW -> settings.beepIntervalYellow
            BedtimeZone.RED -> settings.beepIntervalRed
            else -> return
        }

        val workRequest = OneTimeWorkRequestBuilder<BeepWorker>()
            .setInitialDelay(interval.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueue(workRequest)

        currentBeepWorkRequest = workRequest
    }

    fun cancelBeep() {
        currentBeepWorkRequest?.let { request ->
            WorkManager.getInstance(context)
                .cancelWorkById(request.id)
        }
        currentBeepWorkRequest = null
    }

    fun playBeep() {
        mediaPlayer?.release()
//        mediaPlayer = MediaPlayer.create(context, R.raw.beep).apply {
//            setOnCompletionListener { release() }
//            start()
//        }
    }

    class BeepWorker(
        appContext: Context,
        workerParams: WorkerParameters
    ) : Worker(appContext, workerParams) {
        override fun doWork(): Result {
            val manager = BedtimeManager(applicationContext)
            manager.playBeep()
            return Result.success()
        }
    }

    companion object {
        private const val TAG = "BedtimeManager"
    }
}