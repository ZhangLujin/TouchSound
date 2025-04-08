// File: AudioProcessor.kt
package com.example.musicvibrationapp.service

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.AudioAttributes
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.musicvibrationapp.audio.AudioSeparator
import com.example.musicvibrationapp.audio.AudioTransmissionManager
import com.example.musicvibrationapp.manager.FloatingWindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jtransforms.fft.DoubleFFT_1D
import kotlin.math.max
import kotlin.math.sqrt

class AudioProcessor(private val context: Context, private val fileManager: FileManager) {

    companion object {
        private const val TAG = "AudioProcessor"
        private const val VIBRATION_THRESHOLD = 0  
    }

    private val audioSeparator = AudioSeparator()
    private val MAX_BUFFER_SIZE = 2 * 1024 * 1024 // 2MB in bytes
    private val vocalBuffer = CircularByteBuffer(MAX_BUFFER_SIZE)
    private val bgmBuffer = CircularByteBuffer(MAX_BUFFER_SIZE)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var floatingWindowService: FloatingWindowService? = null
    private var serviceConnection: ServiceConnection? = null
    private val floatingWindowManager = FloatingWindowManager.getInstance(context)

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private var isRecording = false
    private var audioRecord: AudioRecord? = null

    private val audioTransmissionManager = AudioTransmissionManager.getInstance(context).apply {
        configure {
            inputSource = AudioTransmissionManager.InputSource.BGM_ONLY
            enableFrequencyBoost = false  // Default: frequency boost disabled
            frequencyBoostRange = AudioTransmissionManager.FrequencyRange(100f, 1000f)
            amplitudeMultiplier = 1.0f
        }
        startTransmission()
    }

    init {
        bindFloatingWindowService()
    }

    private fun bindFloatingWindowService() {
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                floatingWindowService = (service as? FloatingWindowService.LocalBinder)?.getService()
                Log.d(TAG, "Floating window service bound")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                floatingWindowService = null
                Log.d(TAG, "Floating window service disconnected")
            }
        }

        val intent = Intent(context, FloatingWindowService::class.java)
        context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
    }

    @SuppressLint("MissingPermission")
    fun startProcessing(): Flow<Float> = flow {
        try {
            isRecording = true

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            ).apply {
                if (state != AudioRecord.STATE_INITIALIZED) {
                    throw IllegalStateException("AudioRecord initialization failed")
                }
                startRecording()
            }

            val audioBuffer = ShortArray(bufferSize)
            val fftBuffer = DoubleArray(bufferSize * 2)
            val fft = DoubleFFT_1D(bufferSize.toLong())

            while (isRecording) {
                val readSize = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                if (readSize > 0) {
                    // Convert audio data to double array for FFT
                    for (i in 0 until bufferSize) {
                        fftBuffer[i] = audioBuffer[i].toDouble()
                    }

                    // Perform FFT
                    fft.realForward(fftBuffer)

                    // Calculate spectrum energy
                    var sum = 0.0
                    for (i in 0 until bufferSize / 2) {
                        val re = fftBuffer[2 * i]
                        val im = fftBuffer[2 * i + 1]
                        sum += sqrt(re * re + im * im)
                    }

                    // Normalize and emit result
                    emit((sum / bufferSize).toFloat().coerceIn(0f, 1f))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process audio data", e)
        } finally {
            stopProcessing()
        }
    }

    fun stopProcessing() {
        isRecording = false
        try {
            audioRecord?.apply {
                stop()
                release()
            }
            audioTransmissionManager.stopTransmission()
        } finally {
            audioRecord = null
        }
    }

    fun startCapture() {
        try {
            // Restart floating window service
            val intent = Intent(context, FloatingWindowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            // If service isn't bound, rebind it
            if (floatingWindowService == null) {
                bindFloatingWindowService()
            }
            Log.d(TAG, "Capture started, floating window service launched")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start capture", e)
        }
    }

    fun stopCapture() {
        try {
            // Unbind service but don't stop it
            serviceConnection?.let {
                context.unbindService(it)
                serviceConnection = null
            }
            floatingWindowService = null
            Log.d(TAG, "Capture stopped, service unbound")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop capture", e)
        }
    }

    fun processAudioData(audioData: ByteArray) {
        val startTime = System.nanoTime()
        try {
            val (vocalData, bgmData) = audioSeparator.separateVocalAndBGM(audioData)
            
            // Use circular buffer to write data
            vocalBuffer.write(vocalData)
            bgmBuffer.write(bgmData)

            // Update spectrum display (using original audio data)
            floatingWindowService?.updateSpectrum(audioData)
            
            // Process and get audio data to transmit
            val processedBgmData = audioTransmissionManager.processAndTransmit(audioData, bgmData)
            
            // Store data
            val vocalPath = fileManager.saveVocalData(vocalBuffer.readAll())
            val bgmPath = fileManager.saveBGMData(bgmBuffer.readAll())

            val endTime = System.nanoTime()
            val durationMs = (endTime - startTime) / 1_000_000.0

            val intent = Intent(AudioCaptureService.ACTION_AUDIO_DATA).apply {
                putExtra(AudioCaptureService.EXTRA_AMPLITUDE, calculateAmplitude(audioData))
                putExtra(AudioCaptureService.EXTRA_APP_NAME, getCurrentAudioApp())
                putExtra(AudioCaptureService.EXTRA_VOCAL_FILE_PATH, vocalPath)
                putExtra(AudioCaptureService.EXTRA_BGM_FILE_PATH, bgmPath)
                putExtra(AudioCaptureService.EXTRA_PROCESSING_TIME_MS, durationMs)
            }
            context.sendBroadcast(intent)

            fileManager.saveAudioToFile(audioData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process audio data", e)
        }
    }

    private fun writeToAudioTrack(bgmData: ByteArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build())
                .setBufferSizeInBytes(bgmData.size)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack.play()
            audioTrack.write(bgmData, 0, bgmData.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write audio data", e)
        }
    }

    private fun isVoiceDetected(audioData: ByteArray): Boolean {
        val rms = calculateAmplitude(audioData)
        val threshold = 500 
        return rms > threshold
    }

    private fun calculateAmplitude(audioData: ByteArray): Int {
        var sum = 0.0
        val numSamples = audioData.size / 4  
        for (i in 0 until numSamples) {
            val index = i * 4
            if (index + 3 >= audioData.size) break
            val leftSample = (audioData[index + 1].toInt() shl 8) or (audioData[index].toInt() and 0xFF)
            val leftSampleSigned = leftSample.toShort().toInt()
            val rightSample = (audioData[index + 3].toInt() shl 8) or (audioData[index + 2].toInt() and 0xFF)
            val rightSampleSigned = rightSample.toShort().toInt()
            sum += leftSampleSigned * leftSampleSigned + rightSampleSigned * rightSampleSigned
        }
        val rms = sqrt(sum / (numSamples * 2))
        return rms.toInt()
    }

    private fun getCurrentAudioApp(): String {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val activePlaybackConfigs = audioManager.activePlaybackConfigurations
                if (activePlaybackConfigs.isNotEmpty()) {
                    val config = activePlaybackConfigs[0]
                    return try {
                        // Try to get package name using reflection
                        val method = config.javaClass.getMethod("getClientPackageName")
                        method.invoke(config) as String
                    } catch (e: Exception) {
                        // If retrieval fails, return a default value
                        "Unknown App"
                    }
                }
            }
            return "Unknown App"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get audio app", e)
            return "Unknown App"
        }
    }

    private fun vibrate(amplitude: Int) {
        try {
            val strength = ((amplitude - VIBRATION_THRESHOLD) * 2).toInt().coerceIn(1, 255)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(50, strength)
                )
            } else {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(50, strength)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during vibration", e)
        }
    }

    fun onCaptureStopped() {
        fileManager.convertPcmToWav()
        val vocalPath = fileManager.getVocalWavFilePath()
        val bgmPath = fileManager.getBgmWavFilePath()
        val capturedPath = fileManager.getCapturedWavFilePath()

        val intent = Intent(AudioCaptureService.ACTION_AUDIO_DATA).apply {
            putExtra(AudioCaptureService.EXTRA_AUDIO_FILE_PATH, capturedPath)
            putExtra("sample_rate", FileManager.SAMPLE_RATE)
            putExtra("bit_depth", FileManager.BIT_DEPTH)
            putExtra("channels", FileManager.CHANNELS)

            if (vocalPath != null) {
                putExtra(AudioCaptureService.EXTRA_VOCAL_FILE_PATH, vocalPath)
            }
            if (bgmPath != null) {
                putExtra(AudioCaptureService.EXTRA_BGM_FILE_PATH, bgmPath)
            }
        }
        context.sendBroadcast(intent)

        try {
            // Unbind and stop service
            serviceConnection?.let {
                context.unbindService(it)
            }
            context.stopService(Intent(context, FloatingWindowService::class.java))
            floatingWindowManager.hide()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping floating window service", e)
        }
    }

    fun onDestroy() {
        try {
            serviceConnection?.let {
                context.unbindService(it)
            }
            audioTransmissionManager.release()
            floatingWindowService = null
            serviceConnection = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to destroy", e)
        }
    }

    private fun ShortArray.toByteArray(): ByteArray {
        val bytes = ByteArray(size * 2)
        for (i in indices) {
            bytes[i * 2] = (this[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = (this[i].toInt() shr 8 and 0xFF).toByte()
        }
        return bytes
    }

    // Circular buffer implementation
    private class CircularByteBuffer(private val capacity: Int) {
        private val buffer = ByteArray(capacity)
        private var writePosition = 0
        private var size = 0

        @Synchronized
        fun write(data: ByteArray) {
            val dataLength = data.size
            if (dataLength >= capacity) {
                // If new data is greater than or equal to capacity, only keep the last capacity bytes
                System.arraycopy(data, dataLength - capacity, buffer, 0, capacity)
                writePosition = 0
                size = capacity
                return
            }

            // Calculate available space
            val spaceToEnd = capacity - writePosition
            if (dataLength <= spaceToEnd) {
                // If data can be written directly to the end
                System.arraycopy(data, 0, buffer, writePosition, dataLength)
                writePosition = (writePosition + dataLength) % capacity
            } else {
                // Need to write in two parts
                System.arraycopy(data, 0, buffer, writePosition, spaceToEnd)
                System.arraycopy(data, spaceToEnd, buffer, 0, dataLength - spaceToEnd)
                writePosition = dataLength - spaceToEnd
            }
            size = Math.min(capacity, size + dataLength)
        }

        @Synchronized
        fun readAll(): ByteArray {
            if (size == 0) return ByteArray(0)
            
            val result = ByteArray(size)
            val readPosition = (writePosition - size + capacity) % capacity
            
            if (readPosition < writePosition) {
                // Data is continuous
                System.arraycopy(buffer, readPosition, result, 0, size)
            } else {
                // Data is segmented
                val firstPart = capacity - readPosition
                System.arraycopy(buffer, readPosition, result, 0, firstPart)
                System.arraycopy(buffer, 0, result, firstPart, writePosition)
            }
            
            return result
        }
    }
}