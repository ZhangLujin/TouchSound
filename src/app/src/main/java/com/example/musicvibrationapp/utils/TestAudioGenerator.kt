package com.example.musicvibrationapp.utils

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class TestAudioGenerator {
    private var audioTrack: AudioTrack? = null
    private val sampleRate = 44100
    private var isPlaying = false

    // Note frequency mapping
    private val noteFrequencies = mapOf(
        "C4" to 261.63,
        "C#4" to 277.18,
        "D4" to 293.66,
        "D#4" to 311.13,
        "E4" to 329.63,
        "F4" to 349.23,
        "F#4" to 369.99,
        "G4" to 392.00,
        "G#4" to 415.30,
        "A4" to 440.00,
        "A#4" to 466.16,
        "B4" to 493.88,
        "C5" to 523.25
    )

    // Default using A4, E4 and C4 notes
    private val defaultFrequencies = listOf("A4", "E4", "C4")

    fun startTestTone(frequency: Double = 440.0) {
        if (isPlaying) return

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        )

        isPlaying = true
        Thread {
            val buffer = ShortArray(bufferSize)
            val twoPi = 2.0 * PI
            var phase = 0.0

            audioTrack?.play()

            while (isPlaying) {
                // Randomly select a frequency
                val currentFrequency = getRandomFrequency(defaultFrequencies)
                for (i in buffer.indices) {
                    buffer[i] = (sin(phase) * Short.MAX_VALUE).toInt().toShort()
                    phase += twoPi * currentFrequency / sampleRate
                    if (phase > twoPi) phase -= twoPi
                }
                audioTrack?.write(buffer, 0, buffer.size)
                Thread.sleep(100)  // Control playback speed and rhythm
            }
        }.start()
    }

    private fun getRandomFrequency(frequencies: List<String>): Double {
        val note = frequencies[Random.nextInt(frequencies.size)]
        return noteFrequencies[note] ?: 440.0 
    }

    fun stopTestTone() {
        isPlaying = false
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
