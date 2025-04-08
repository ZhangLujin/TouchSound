package com.example.musicvibrationapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.WindowManager.LayoutParams
import androidx.core.app.NotificationCompat
import android.app.ForegroundServiceStartNotAllowedException
import android.content.pm.ServiceInfo
import com.example.musicvibrationapp.R
import com.example.musicvibrationapp.audio.AudioSeparator
import com.example.musicvibrationapp.manager.FloatingWindowManager
import com.example.musicvibrationapp.view.AudioSpectrumView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.jtransforms.fft.DoubleFFT_1D

class FloatingWindowService : Service() {
    private val audioSeparator = AudioSeparator()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val binder = LocalBinder()
    
    private lateinit var floatingWindowManager: FloatingWindowManager

    override fun onCreate() {
        super.onCreate()
        try {
            floatingWindowManager = FloatingWindowManager.getInstance(this)
            
            createNotificationChannel()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
            floatingWindowManager.show()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Window Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Edge audio visualization service"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateSpectrum(audioData: ByteArray) {
        try {
            val (_, bgmData) = audioSeparator.separateVocalAndBGM(audioData)
            
            Handler(Looper.getMainLooper()).post {
                // TODO: Decide whether to use bgmData or audioData
                // val fftData = processAudioData(bgmData)
                val fftData = processAudioData(audioData)
                floatingWindowManager.updateSpectrum(fftData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process spectrum data", e)
        }
    }

    private fun processAudioData(audioData: ByteArray): FloatArray {
        // Convert byte data to double precision array
        val samples = DoubleArray(audioData.size / 2)
        for (i in samples.indices) {
            val index = i * 2
            val sample = (audioData[index + 1].toInt() shl 8) or (audioData[index].toInt() and 0xFF)
            samples[i] = sample.toDouble() / 32768.0
        }

        // Perform FFT
        val fft = DoubleFFT_1D(samples.size.toLong())
        val fftData = samples.copyOf(samples.size * 2)
        fft.realForward(fftData)

        // Calculate spectrum
        val spectrum = FloatArray(64) // Use 64 frequency bands
        val bandSize = (fftData.size / 2) / spectrum.size
        
        for (i in spectrum.indices) {
            var sum = 0.0
            val startIdx = i * bandSize
            val endIdx = startIdx + bandSize
            
            for (j in startIdx until endIdx) {
                val re = fftData[2 * j]
                val im = fftData[2 * j + 1]
                sum += Math.sqrt(re * re + im * im)
            }
            
            spectrum[i] = (sum / bandSize).toFloat()
        }

        // Normalize
        val maxValue = spectrum.maxOrNull() ?: 1f
        return spectrum.map { it / maxValue }.toFloatArray()
    }

    inner class LocalBinder : Binder() {
        fun getService(): FloatingWindowService = this@FloatingWindowService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        floatingWindowManager.show()
        return START_STICKY
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Visualization")
            .setContentText("Running")
            .setSmallIcon(R.drawable.ic_notification)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val TAG = "FloatingWindowService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "FloatingWindowChannel"
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingWindowManager.hide()
    }

    fun resetService() {
        floatingWindowManager.hide()
        floatingWindowManager.show()
        Log.d(TAG, "Service reset")
    }
}