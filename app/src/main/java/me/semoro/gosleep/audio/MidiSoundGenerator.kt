package me.semoro.gosleep.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.sin
import kotlin.random.Random

/**
 * A class that generates keygen-like electronic sounds programmatically.
 * Uses square wave generation with arpeggiated patterns to create chiptune-like sounds
 * similar to those found in software keygens.
 */
class MidiSoundGenerator {
    companion object {
        private const val SAMPLE_RATE = 44100 // Hz (standard audio sample rate)
        private const val MAX_AMPLITUDE = 32767 // Maximum amplitude for 16-bit audio

        // Common notes in the C major scale (frequencies in Hz)
        private val NOTES = mapOf(
            "C4" to 261.63, "D4" to 293.66, "E4" to 329.63, "F4" to 349.23, 
            "G4" to 392.00, "A4" to 440.00, "B4" to 493.88, "C5" to 523.25,
            "D5" to 587.33, "E5" to 659.25, "F5" to 698.46, "G5" to 783.99
        )

        // Chord progressions commonly used in keygen music
        private val CHORD_PROGRESSIONS = listOf(
            // C major, G major, A minor, F major
            listOf(
                listOf("C4", "E4", "G4"), 
                listOf("G4", "B4", "D5"), 
                listOf("A4", "C5", "E5"), 
                listOf("F4", "A4", "C5")
            ),
            // A minor, F major, C major, G major
            listOf(
                listOf("A4", "C5", "E5"), 
                listOf("F4", "A4", "C5"), 
                listOf("C4", "E4", "G4"), 
                listOf("G4", "B4", "D5")
            )
        )

        /**
         * Generates a keygen-like sound and plays it.
         * @return The AudioTrack instance that's playing the sound
         */
        fun generateAndPlayRandomSound(): AudioTrack {
            // Select a random chord progression
            val progression = CHORD_PROGRESSIONS.random().shuffled()

            // Duration of the entire sequence (4 seconds)
            val totalDuration = 4000

            // Duration of each note in the arpeggio
            val noteDuration = Random.nextInt(100, 400)

            // Attack and decay times for each note
            val attackTime = Random.nextInt(10, 50)
            val decayTime = Random.nextInt(50, 100)

            return generateKeygenMusic(progression, totalDuration, noteDuration, attackTime, decayTime)
        }

        /**
         * Generates and plays keygen-like music with arpeggiated chord progressions.
         */
        private fun generateKeygenMusic(
            chordProgression: List<List<String>>,
            totalDuration: Int,
            noteDuration: Int,
            attackTime: Int = 10,
            decayTime: Int = 30
        ): AudioTrack {
            // Calculate buffer size based on total duration
            val bufferSize = (totalDuration * SAMPLE_RATE / 1000)
            val buffer = ShortArray(bufferSize)

            // Calculate how many samples each note takes
            val samplesPerNote = (noteDuration * SAMPLE_RATE / 1000)

            // Calculate how many notes we can fit in the total duration
            val totalNotes = bufferSize / samplesPerNote

            // Generate the waveform
            for (noteIndex in 0 until totalNotes) {
                // Determine which chord and note within the chord to play
                val chordIndex = (noteIndex / 3) % chordProgression.size
                val noteInChordIndex = noteIndex % chordProgression[chordIndex].size

                // Get the note name and its frequency
                val noteName = chordProgression[chordIndex][noteInChordIndex]
                val frequency = NOTES[noteName] ?: 440.0 // Default to A4 if not found

                // Calculate the start and end sample indices for this note
                val startSample = noteIndex * samplesPerNote
                val endSample = startSample + samplesPerNote

                // Generate the note
                for (i in startSample until endSample) {
                    if (i >= bufferSize) break

                    val time = (i - startSample).toDouble() / SAMPLE_RATE

                    // Generate square wave (for chiptune sound)
                    val cycle = (time * frequency) % 1.0
                    val rawSample = if (cycle < 0.5) 1.0 else -1.0

                    // Apply envelope (attack, decay)
                    val sampleInNote = i - startSample
                    val envelope = when {
                        sampleInNote < attackTime * SAMPLE_RATE / 1000 -> 
                            sampleInNote.toDouble() / (attackTime * SAMPLE_RATE / 1000)
                        sampleInNote > samplesPerNote - (decayTime * SAMPLE_RATE / 1000) -> {
                            val decayPosition = sampleInNote - (samplesPerNote - (decayTime * SAMPLE_RATE / 1000))
                            1.0 - (decayPosition.toDouble() / (decayTime * SAMPLE_RATE / 1000))
                        }
                        else -> 1.0
                    }

                    // Add some randomized amplitude variation for more character
                    val amplitudeVariation = 0.8 + (Random.nextDouble() * 0.2)

                    // Normalize and convert to 16-bit PCM
                    buffer[i] = (rawSample * envelope * amplitudeVariation * MAX_AMPLITUDE * 0.7).toInt().toShort()
                }
            }

            // Create and configure AudioTrack
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2) // 2 bytes per short
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            // Write the audio data to the track and play it
                audioTrack.setLoopPoints(0, buffer.size, -1)
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()


            return audioTrack
        }

    }
}
