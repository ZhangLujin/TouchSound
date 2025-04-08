// File: MyNotificationManager.kt
package com.example.musicvibrationapp.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.musicvibrationapp.MainActivity
import com.example.musicvibrationapp.R

class MyNotificationManager(private val context: Context) {

    companion object {
        const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "AudioCaptureChannel"
        private const val CHANNEL_NAME = "Audio Capture Service"
        private const val CHANNEL_DESCRIPTION = "Running audio capture service"
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotification(): Notification {
        val pendingIntent = Intent(context, MainActivity::class.java).let {
            PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(CHANNEL_NAME)
            .setContentText("Capturing system audio...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()
    }
}