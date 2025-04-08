package com.example.musicvibrationapp.audio

import org.jtransforms.fft.DoubleFFT_1D
import kotlin.math.PI

class AudioSeparator {
    companion object {
        private const val SAMPLE_RATE = 44100
        private const val FRAME_SIZE = 1024
        private const val HOP_SIZE = FRAME_SIZE / 2 // 50% overlap
    }

    fun separateVocalAndBGM(audioData: ByteArray): Pair<ByteArray, ByteArray> {
        val numSamples = audioData.size / 2
        val samples = byteArrayToShortArray(audioData)

        // Precompute window function (Hanning window)
        val window = precomputeHanningWindow(FRAME_SIZE)

        // Precompute filter coefficients
        val filterCoefficients = precomputeFilterCoefficients(FRAME_SIZE)

        // Pre-allocate FFT related arrays
        val fft = DoubleFFT_1D(FRAME_SIZE.toLong())
        val fftData = DoubleArray(FRAME_SIZE * 2)
        val windowedSamples = DoubleArray(FRAME_SIZE)

        // Pre-allocate output buffers
        val vocal = DoubleArray(numSamples)
        val bgm = DoubleArray(numSamples)

        // Calculate total number of frames
        val numFrames = Math.ceil(numSamples.toDouble() / HOP_SIZE).toInt()

        for (frame in 0 until numFrames) {
            val start = frame * HOP_SIZE

            // Apply window to the current frame
            for (i in 0 until FRAME_SIZE) {
                val sampleIndex = start + i
                windowedSamples[i] = if (sampleIndex < numSamples) {
                    samples[sampleIndex].toDouble() * window[i]
                } else {
                    0.0 // Zero-padding
                }
            }

            // Perform FFT
            System.arraycopy(windowedSamples, 0, fftData, 0, FRAME_SIZE)
            fft.realForward(fftData)

            // Apply precomputed filter coefficients
            for (i in 0 until FRAME_SIZE / 2) {
                fftData[2 * i] *= filterCoefficients[i]
                fftData[2 * i + 1] *= filterCoefficients[i]
            }

            // Perform IFFT
            fft.realInverse(fftData, true)

            // Overlap-add to output buffers
            for (i in 0 until FRAME_SIZE) {
                val sampleIndex = start + i
                if (sampleIndex < numSamples) {
                    vocal[sampleIndex] += fftData[i] * window[i]
                    bgm[sampleIndex] += (samples[sampleIndex].toDouble() - fftData[i]) * window[i]
                }
            }
        }

        // Convert Double arrays back to Byte arrays
        val vocalData = shortArrayToByteArray(vocal)
        val bgmData = shortArrayToByteArray(bgm)

        return Pair(vocalData, bgmData)
    }

    // Precompute Hanning window coefficients
    private fun precomputeHanningWindow(frameSize: Int): DoubleArray {
        return DoubleArray(frameSize) { i ->
            0.5 - 0.5 * Math.cos(2.0 * PI * i / (frameSize - 1))
        }
    }

    // Precompute filter coefficients based on frequency bins
    private fun precomputeFilterCoefficients(frameSize: Int): DoubleArray {
        val binSize = SAMPLE_RATE.toDouble() / frameSize
        return DoubleArray(frameSize / 2) { i ->
            val frequency = i * binSize
            when {
                frequency < 300 -> frequency / 300.0 // Ramp-up from 0 to 1
                frequency > 3000 -> (SAMPLE_RATE / 2 - frequency) / (SAMPLE_RATE / 2 - 3000.0) // Ramp-down from 1 to 0
                else -> 1.0 // Pass through
            }
        }
    }

    // Convert byte array to short array
    private fun byteArrayToShortArray(audioData: ByteArray): ShortArray {
        val numSamples = audioData.size / 2
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val low = (audioData[2 * i].toInt() and 0xFF)
            val high = (audioData[2 * i + 1].toInt() shl 8)
            samples[i] = (high or low).toShort()
        }
        return samples
    }

    // Convert Double array to byte array
    private fun shortArrayToByteArray(samples: DoubleArray): ByteArray {
        val byteArray = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val sample = samples[i].toInt()
            byteArray[2 * i] = (sample and 0xFF).toByte()
            byteArray[2 * i + 1] = ((sample shr 8) and 0xFF).toByte()
        }
        return byteArray
    }
}