package com.example.musicvibrationapp.ui.UIService

import android.app.*
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.musicvibrationapp.R
import com.example.musicvibrationapp.audio.AudioCaptureManagerWrapper
import com.example.musicvibrationapp.service.AudioProcessor
import com.example.musicvibrationapp.service.FileManager
import com.example.musicvibrationapp.service.MyNotificationManager

class UIAudioCaptureService : Service() {
    private var mediaProjection: MediaProjection? = null
    private val TAG = "UIAudioCaptureService"
    private var isCapturing = false

    private lateinit var notificationManager: MyNotificationManager
    private lateinit var audioCaptureManagerWrapper: AudioCaptureManagerWrapper
    private lateinit var fileManager: FileManager
    private lateinit var audioProcessor: AudioProcessor

    override fun onCreate() {
        super.onCreate()
        notificationManager = MyNotificationManager(this)
        notificationManager.createNotificationChannel()

        fileManager = FileManager(this)
        audioProcessor = AudioProcessor(this, fileManager)
        audioCaptureManagerWrapper = AudioCaptureManagerWrapper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        when (intent.action) {
            ACTION_START_CAPTURE -> {
                if (!isCapturing) {
                    val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                    val data = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                    
                    if (data != null) {
                        val notification = notificationManager.createNotification()
                        startForeground(MyNotificationManager.NOTIFICATION_ID, notification)
                        
                        audioCaptureManagerWrapper.initializeMediaProjection(
                            resultCode,
                            data
                        ) { audioData ->
                            audioProcessor.processAudioData(audioData)
                        }
                        
                        isCapturing = true
                        sendStateBroadcast(true)
                    }
                }
            }
            ACTION_STOP_CAPTURE -> {
                if (isCapturing) {
                    stopCapture()
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun stopCapture() {
        try {
            audioProcessor.onCaptureStopped()
            audioCaptureManagerWrapper.release()
            isCapturing = false
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            
            stopSelf()
            sendStateBroadcast(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (isCapturing) {
            stopCapture()
        }
        super.onDestroy()
    }

    private fun sendStateBroadcast(isCapturing: Boolean) {
        val intent = Intent(ACTION_CAPTURE_STATE_CHANGED).apply {
            putExtra(EXTRA_IS_CAPTURING, isCapturing)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Audio Capture Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Used for audio capture service"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Capture")
            .setContentText("Capturing system audio...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "UIAudioCaptureChannel"
        const val NOTIFICATION_ID = 2
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val ACTION_START_CAPTURE = "com.example.musicvibrationapp.START_CAPTURE"
        const val ACTION_STOP_CAPTURE = "com.example.musicvibrationapp.STOP_CAPTURE"
        const val ACTION_CAPTURE_STATE_CHANGED = "com.example.musicvibrationapp.CAPTURE_STATE_CHANGED"
        const val EXTRA_IS_CAPTURING = "is_capturing"
    }
}