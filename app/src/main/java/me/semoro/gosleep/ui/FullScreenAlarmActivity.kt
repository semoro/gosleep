package me.semoro.gosleep.ui

import android.app.NotificationManager
import android.media.AudioTrack
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.semoro.gosleep.audio.MidiSoundGenerator
import me.semoro.gosleep.data.UserSettingsRepository
import me.semoro.gosleep.ui.components.CyberpunkAccessPuzzle
import me.semoro.gosleep.ui.components.TimeDisplay
import me.semoro.gosleep.ui.components.ZoneProgressIndicator
import me.semoro.gosleep.ui.theme.GoSleepTheme

class FullScreenAlarmActivity : ComponentActivity() {
    // AudioTrack instance to keep track of the sound being played
    private var audioTrack: AudioTrack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println("Creating FullScreenAlarmActivity")

        val zone = intent.getStringExtra("zone")?.let { BedtimeZone.valueOf(it) } ?: BedtimeZone.NONE

        val win = window
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        win.addFlags(
            (WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        )

        val notificationManager = applicationContext.getSystemService<NotificationManager>(NotificationManager::class.java)
        notificationManager.cancel(0)

        // Generate and play a random electronic sound
        audioTrack = MidiSoundGenerator.generateAndPlayRandomSound()

        val repository = UserSettingsRepository(applicationContext)

        setContent {
            GoSleepTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AlarmScreen(
                        zone = zone,
                        modifier = Modifier.padding(innerPadding),
                        repository = repository,
                        onPuzzleSolved = {
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop and release the AudioTrack when the activity is destroyed
        audioTrack?.let {
            if (it.state == AudioTrack.STATE_INITIALIZED) {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        }
        audioTrack = null
    }
}

private fun timeTickerFlow(): Flow<Instant> = flow {
    while (true) {
        emit(Clock.System.now())
        delay(1000L)
    }
}

@Composable
fun AlarmScreen(
    modifier: Modifier = Modifier,
    zone: BedtimeZone,
    repository: UserSettingsRepository,
    onPuzzleSolved: () -> Unit = {}
) {
    val currentTimeState = remember { timeTickerFlow() }.collectAsState(Clock.System.now())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        TimeDisplay(
            currentTime = currentTimeState.value.toLocalDateTime(TimeZone.currentSystemDefault()).time,
            modifier = Modifier.padding(top = 32.dp)
        )

        // Show current zone information
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (zone) {
                        BedtimeZone.NONE -> "Not bedtime yet"
                        BedtimeZone.GREEN -> "Time to get ready for bed"
                        BedtimeZone.YELLOW -> "You should be in bed by now"
                        BedtimeZone.RED -> "You're significantly past bedtime"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        val userSettingsState = remember { repository.userSettingsFlow }.collectAsState(initial = null)
        userSettingsState.value?.let { settings ->
            ZoneProgressIndicator(
                currentTime = currentTimeState.value,
                userSettings = settings,
                currentZone = zone,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        CyberpunkAccessPuzzle(
            gridSize = 4,
            targetSequenceLength = 3,
            onPuzzleSolved = onPuzzleSolved
        )
    }
}