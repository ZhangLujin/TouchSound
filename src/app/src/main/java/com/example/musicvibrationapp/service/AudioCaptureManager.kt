// File: AudioCaptureManager.kt
package com.example.musicvibrationapp.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.musicvibrationapp.audio.SystemAudioCaptureService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class AudioCaptureManager(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var audioCaptureService: SystemAudioCaptureService? = null
    private val serviceScope = CoroutineScope(Job() + Dispatchers.Default)
    private val TAG = "AudioCaptureManager"

    var audioDataCallback: ((ByteArray) -> Unit)? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    fun initializeMediaProjection(resultCode: Int, data: Intent) {
        val mediaProjectionManager =
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
        mediaProjection?.let {
            audioCaptureService = SystemAudioCaptureService(context, it)
            startAudioCapture()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startAudioCapture() {
        serviceScope.launch {
            try {
                audioCaptureService?.startCapture()?.collect { audioData ->
                    // Pass audio data to callback function
                    audioDataCallback?.invoke(audioData)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during audio capture", e)
            }
        }
    }

    fun stopAudioCapture() {
        audioCaptureService?.stopCapture()
        mediaProjection?.stop()
        mediaProjection = null
    }

    fun release() {
        stopAudioCapture()
        mediaProjection?.stop()
        mediaProjection = null
        serviceScope.cancel()
    }

    fun getMediaProjection(): MediaProjection? {
        return mediaProjection
    }
}