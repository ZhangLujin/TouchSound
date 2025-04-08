package com.example.musicvibrationapp.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin

class TestAudioManager private constructor(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var originalVolume = 0

    private val _testModeEnabled = MutableStateFlow(false)
    val testModeEnabled: StateFlow<Boolean> = _testModeEnabled.asStateFlow()

    private val _currentVolume = MutableStateFlow(0.8f)
    val currentVolume: StateFlow<Float> = _currentVolume.asStateFlow()

    fun toggleTestMode() {
        if (_testModeEnabled.value) {
            stopTest()
        } else {
            startTest()
        }
        _testModeEnabled.value = !_testModeEnabled.value
    }

    private fun startTest() {
        try {
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            
            _currentVolume.value = originalVolume.toFloat() / maxVolume.toFloat()
            
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)

            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            audioTrack?.setVolume(_currentVolume.value)
            audioTrack?.play()
            isPlaying = true

        } catch (e: Exception) {
            Log.e("TestAudioManager", "Error starting test", e)
        }
    }

    private fun stopTest() {
        isPlaying = false
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
    }

    fun setTestVolume(volume: Float) {
        _currentVolume.value = volume.coerceIn(0f, 1f)
        audioTrack?.setVolume(_currentVolume.value)
    }

    fun playRealAudio(audioData: ByteArray) {
        if (isPlaying && audioTrack != null) {
            try {
                audioTrack?.write(audioData, 0, audioData.size)
            } catch (e: Exception) {
                Log.e("TestAudioManager", "Error playing real audio", e)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: TestAudioManager? = null

        fun getInstance(context: Context): TestAudioManager {
            return instance ?: synchronized(this) {
                instance ?: TestAudioManager(context).also { instance = it }
            }
        }
    }
} 