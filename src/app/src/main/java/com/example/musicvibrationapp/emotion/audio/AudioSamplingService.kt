package com.example.musicvibrationapp.emotion.audio

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jtransforms.fft.FloatFFT_1D
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.log10

class AudioSamplingService {
    private val TAG = "AudioSamplingService"
    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private var isProcessing = false
    private var currentJob: Job? = null
    private val samples = mutableListOf<AudioFeatures>()
    private val audioAnalyzer = AudioAnalyzer()
    
    companion object {
        const val FRAME_SIZE = 2048          // Samples per frame
        const val SAMPLING_RATE = 44100      // Sampling rate
        const val MAX_SAMPLES = 50           // Maximum samples
        const val TOTAL_DURATION = 5000L     // Total sampling duration (ms)
        const val SAMPLE_INTERVAL = TOTAL_DURATION / MAX_SAMPLES  // Sampling interval (ms)
    }

    private data class BasicFeatures(
        val amplitude: Float,
        val energy: Float,
        val zeroCrossings: Int
    )

    private data class SpectralFeatures(
        val fundamentalFreq: Float,
        val centroid: Float,
        val spread: Float,
        val rolloff: Float
    )

    private data class RhythmFeatures(
        val tempo: Float,
        val strength: Float
    )

    private data class TimbreFeatures(
        val brightness: Float,
        val roughness: Float
    )

    private data class EmotionFeatures(
        val valence: Float,
        val arousal: Float
    )

    fun startSampling(callback: AudioSamplingCallback) {
        if (isProcessing) return
        isProcessing = true
        samples.clear()
        
        currentJob = scope.launch {
            try {
                val startTime = System.currentTimeMillis()
                var nextSampleTime = startTime

                while (isProcessing && samples.size < MAX_SAMPLES) {
                    val currentTime = System.currentTimeMillis()
                    
                    // Only process samples when sampling interval is reached
                    if (currentTime >= nextSampleTime) {
                        if (samples.isNotEmpty()) {
                            val elapsedTime = currentTime - startTime
                            if (elapsedTime >= TOTAL_DURATION) {
                                break
                            }
                        }
                        
                        // Process the latest audio data
                        synchronized(samples) {
                            if (samples.size < MAX_SAMPLES) {
                                val features = processLatestAudioFeatures()
                                if (features != null) {
                                    samples.add(features)
                                }
                            }
                        }
                        
                        // Update next sampling time
                        nextSampleTime = currentTime + SAMPLE_INTERVAL
                    }
                    
                    // Brief delay to avoid excessive CPU usage
                    delay(10)
                }

                // Process results after sampling is complete
                val stats = calculateStats(samples)
                callback.onSamplingComplete(samples.toList(), stats)
                
            } catch (e: Exception) {
                Log.e(TAG, "Sampling error", e)
                callback.onError(e)
            } finally {
                isProcessing = false
                samples.clear()
                callback.onCleanup()
            }
        }
    }

    private var latestAudioData: ByteArray? = null
    private val audioDataLock = Object()

    fun processAudioData(data: ByteArray) {
        if (!isProcessing) return
        synchronized(audioDataLock) {
            latestAudioData = data.copyOf()
        }
    }

    private fun processLatestAudioFeatures(): AudioFeatures? {
        val data = synchronized(audioDataLock) {
            latestAudioData?.copyOf()
        } ?: return null

        val rawFeatures = audioAnalyzer.analyze(data)
        return processAudioFeatures(rawFeatures)
    }

    private fun processAudioFeatures(rawFeatures: AudioFeatures): AudioFeatures {
        return AudioFeatures(
            amplitude = rawFeatures.amplitude,
            energy = rawFeatures.energy,
            fundamentalFrequency = rawFeatures.fundamentalFrequency,
            spectralCentroid = rawFeatures.spectralCentroid,
            spectralSpread = rawFeatures.spectralSpread,
            spectralRolloff = rawFeatures.spectralRolloff,
            brightness = rawFeatures.brightness,
            roughness = rawFeatures.roughness,
            spectralFlux = rawFeatures.spectralFlux,
            harmonicComplexity = rawFeatures.harmonicComplexity,
            spectrum = rawFeatures.spectrum,
            harmonicContent = rawFeatures.harmonicContent,
            timestamp = System.currentTimeMillis(),
            pitch = rawFeatures.fundamentalFrequency,
            pitchName = getPitchNameWithOctave(rawFeatures.fundamentalFrequency),
            loudness = calculateLoudness(rawFeatures.energy),
            dynamics = getDynamicsFromEnergy(rawFeatures.energy)
        )
    }

    private fun getPitchNameWithOctave(frequency: Float): String {
        if (frequency <= 0) return "Rest"
        val midiNote = (69 + 12 * log2(frequency / 440f)).toInt()
        val octave = (midiNote / 12) - 1
        val noteIndex = midiNote % 12
        return arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")[noteIndex] + octave
    }

    private fun getDynamicsFromEnergy(energy: Float): String {
        if (energy < 1e-10f) return "silence"
        return when {
            energy < 0.1f -> "pp"
            energy < 0.25f -> "p"
            energy < 0.4f -> "mp"
            energy < 0.6f -> "mf"
            energy < 0.8f -> "f"
            else -> "ff"
        }
    }

    private fun calculateLoudness(energy: Float): Float {
        if (energy < 1e-10f) return -Float.POSITIVE_INFINITY
        return 20 * log10(max(energy, 1e-6f))
    }

    private fun calculateStats(samples: List<AudioFeatures>): AudioFeatureStats {
        val duration = if (samples.isNotEmpty()) {
            samples.last().timestamp - samples.first().timestamp
        } else 0L

        return AudioFeatureStats(
            duration = duration,
            sampleCount = samples.size,
            meanEnergy = samples.map { it.energy }.average().toFloat(),
            energyVariance = calculateVariance(samples.map { it.energy }),
            meanPitch = samples.map { it.pitch }.average().toFloat(),
            pitchVariance = calculateVariance(samples.map { it.pitch }),
            meanBrightness = samples.map { it.brightness }.average().toFloat(),
            brightnessVariance = calculateVariance(samples.map { it.brightness })
        )
    }

    private fun calculateVariance(values: List<Float>): Float {
        val mean = values.average()
        return values.map { (it - mean) * (it - mean) }.average().toFloat()
    }

    fun stopSampling() {
        isProcessing = false
        currentJob?.cancel()
        currentJob = null
        audioAnalyzer.reset()
    }
} 