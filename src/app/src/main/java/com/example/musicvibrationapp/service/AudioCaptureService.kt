// File: AudioCaptureService.kt
package com.example.musicvibrationapp.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi

class AudioCaptureService : Service() {

    private val binder = LocalBinder()
    private val TAG = "AudioCaptureService"

    private lateinit var notificationManager: MyNotificationManager
    private lateinit var audioCaptureManager: AudioCaptureManager
    private lateinit var fileManager: FileManager
    private lateinit var audioProcessor: AudioProcessor

    inner class LocalBinder : Binder() {
        fun getService(): AudioCaptureService = this@AudioCaptureService
    }

    companion object {
        const val ACTION_AUDIO_DATA = "com.example.musicvibrationapp.AUDIO_DATA"
        const val EXTRA_AMPLITUDE = "amplitude"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_AUDIO_FILE_PATH = "audio_file_path"
        const val EXTRA_VOCAL_FILE_PATH = "vocal_file_path"
        const val EXTRA_BGM_FILE_PATH = "bgm_file_path"
        const val EXTRA_PROCESSING_TIME_MS = "processing_time_ms"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = MyNotificationManager(this)
        notificationManager.createNotificationChannel()

        fileManager = FileManager(this)
        audioProcessor = AudioProcessor(this, fileManager)
        audioCaptureManager = AudioCaptureManager(this)
        audioCaptureManager.audioDataCallback = { audioData ->
            audioProcessor.processAudioData(audioData)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notificationManager.createNotification()
        startForeground(MyNotificationManager.NOTIFICATION_ID, notification)

        intent?.let {
            if (it.hasExtra("resultCode") && it.hasExtra("data")) {
                val resultCode = it.getIntExtra("resultCode", Activity.RESULT_CANCELED)
                val data = it.getParcelableExtra<Intent>("data")
                if (data != null) {
                    audioCaptureManager.initializeMediaProjection(resultCode, data)
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onDestroy() {
        audioCaptureManager.release()
        audioProcessor.onCaptureStopped()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun stopAudioCapture() {
        audioCaptureManager.stopAudioCapture()
        audioProcessor.onCaptureStopped()

        stopForeground(true)
        stopSelf()
    }

}