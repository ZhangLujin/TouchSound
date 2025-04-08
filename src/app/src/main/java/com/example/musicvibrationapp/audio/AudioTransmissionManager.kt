package com.example.musicvibrationapp.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.util.Log
import org.jtransforms.fft.DoubleFFT_1D
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AudioTransmissionManager private constructor(private val context: Context) {

    enum class InputSource {
        SYSTEM_AUDIO,
        BGM_ONLY
    }


    data class FrequencyRange(val low: Float, val high: Float)


    data class Configuration(
        var inputSource: InputSource = InputSource.BGM_ONLY,
        var enableFrequencyBoost: Boolean = false,
        var frequencyBoostRange: FrequencyRange = FrequencyRange(100f, 1000f),
        var amplitudeMultiplier: Float = 1.0f
    )

    private var config = Configuration()
    private var isTransmitting = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val fft = DoubleFFT_1D(BUFFER_SIZE.toLong())


    fun configure(block: Configuration.() -> Unit) {
        config.apply(block)
    }

    fun processAndTransmit(audioData: ByteArray, bgmData: ByteArray? = null): ByteArray {
        if (!isTransmitting) return ByteArray(0)

        try {
            val sourceData = when (config.inputSource) {
                InputSource.SYSTEM_AUDIO -> audioData
                InputSource.BGM_ONLY -> bgmData ?: return ByteArray(0)
            }


            return preprocessAudio(sourceData)
        } catch (e: Exception) {
            Log.e(TAG, "error", e)
            return ByteArray(0)
        }
    }

    private fun preprocessAudio(data: ByteArray): ByteArray {
        if (!config.enableFrequencyBoost && config.amplitudeMultiplier == 1.0f) {
            return data
        }

        val samples = ByteArray(data.size)
        System.arraycopy(data, 0, samples, 0, data.size)

        val fftBuffer = DoubleArray(BUFFER_SIZE * 2)
        for (i in 0 until min(BUFFER_SIZE, data.size / 2)) {
            val sample = (data[i * 2 + 1].toInt() shl 8) or (data[i * 2].toInt() and 0xFF)
            fftBuffer[i] = sample.toDouble()
        }

        if (config.enableFrequencyBoost) {
            fft.realForward(fftBuffer)

            val lowIndex = (config.frequencyBoostRange.low * BUFFER_SIZE / SAMPLE_RATE).toInt()
            val highIndex = (config.frequencyBoostRange.high * BUFFER_SIZE / SAMPLE_RATE).toInt()

            for (i in lowIndex..highIndex) {
                if (i < BUFFER_SIZE / 2) {
                    fftBuffer[i * 2] *= FREQUENCY_BOOST_FACTOR
                    fftBuffer[i * 2 + 1] *= FREQUENCY_BOOST_FACTOR
                }
            }

            fft.realInverse(fftBuffer, true)
        }

        for (i in 0 until min(BUFFER_SIZE, data.size / 2)) {
            var sample = fftBuffer[i].toInt()
            sample = (sample * config.amplitudeMultiplier).toInt()
            sample = max(min(sample, Short.MAX_VALUE.toInt()), Short.MIN_VALUE.toInt())

            samples[i * 2] = (sample and 0xFF).toByte()
            samples[i * 2 + 1] = (sample shr 8).toByte()
        }

        return samples
    }

    fun startTransmission() {
        isTransmitting = true
    }

    fun stopTransmission() {
        isTransmitting = false
    }

    fun release() {
        isTransmitting = false
    }

    companion object {
        private const val TAG = "AudioTransmissionManager"
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = 2048
        private const val FREQUENCY_BOOST_FACTOR = 2.0

        @Volatile
        private var instance: AudioTransmissionManager? = null

        fun getInstance(context: Context): AudioTransmissionManager {
            return instance ?: synchronized(this) {
                instance ?: AudioTransmissionManager(context).also { instance = it }
            }
        }
    }
}