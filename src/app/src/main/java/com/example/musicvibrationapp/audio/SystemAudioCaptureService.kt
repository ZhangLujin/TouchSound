// File: SystemAudioCaptureService.kt
package com.example.musicvibrationapp.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.nio.ByteBuffer

@RequiresApi(Build.VERSION_CODES.Q)
class SystemAudioCaptureService(
    private val context: Context,
    private val mediaProjection: MediaProjection
) {
    private var audioRecord: AudioRecord? = null
    private var isCapturing = false
    
    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    fun startCapture(): Flow<ByteArray> = flow {
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AUDIO_FORMAT)
            .setChannelMask(CHANNEL_CONFIG)
            .build()

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        try {
            audioRecord = AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(config)
                .build()
        } catch (e: SecurityException) {
            throw IllegalStateException("Failed to create AudioRecord: ${e.message}")
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("Invalid audio configuration: ${e.message}")
        }

        if (audioRecord == null || audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord initialization failed")
        }

        val buffer = ByteBuffer.allocateDirect(bufferSize)
        val audioData = ByteArray(bufferSize)
        
        audioRecord?.startRecording()
        isCapturing = true

        while (isCapturing) {
            val bytesRead = audioRecord?.read(buffer, bufferSize) ?: 0
            if (bytesRead > 0) {
                buffer.get(audioData, 0, bytesRead)
                emit(audioData.copyOf(bytesRead))
                buffer.clear()
            }
        }
        
        stopCapture()
    }

    fun stopCapture() {
        isCapturing = false
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e("SystemAudioCapture", "Error stopping capture: ${e.message}")
        }
    }
}
