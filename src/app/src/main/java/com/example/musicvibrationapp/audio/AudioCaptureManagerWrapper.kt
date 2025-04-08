package com.example.musicvibrationapp.audio

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicvibrationapp.service.AudioCaptureManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import com.example.musicvibrationapp.ui.components.FloatingControlButtonManager
import com.example.musicvibrationapp.emotion.service.ScreenCaptureService
import kotlinx.coroutines.launch
import android.util.Log
import com.example.musicvibrationapp.emotion.image.TextRecognitionCallback
import com.example.musicvibrationapp.emotion.audio.AudioSamplingService
import com.example.musicvibrationapp.emotion.audio.AudioSamplingCallback
import com.example.musicvibrationapp.emotion.TaskCompletionManager
import com.example.musicvibrationapp.emotion.api.AnalysisCoordinator
import com.example.musicvibrationapp.emotion.api.LLMService
import com.example.musicvibrationapp.emotion.audio.AudioFeatureStats
import com.example.musicvibrationapp.emotion.audio.AudioFeatures

class AudioCaptureManagerWrapper(private val context: Context) {
    private var audioCaptureManager: AudioCaptureManager? = null

    private val serviceScope = CoroutineScope(Job() + Dispatchers.Default)

    private val audioPreprocessor = AudioPreprocessor()

    private val floatingButtonManager = FloatingControlButtonManager(context)

    private var screenCaptureService: ScreenCaptureService? = null

    private val analysisCoordinator = AnalysisCoordinator(
        context = context,
        llmService = LLMService.getInstance(context)
    )

    private val textRecognitionCallback = analysisCoordinator.textCallback
    private val audioSamplingCallback = analysisCoordinator.audioCallback

    private val audioSamplingService = AudioSamplingService()
    private val taskManager = TaskCompletionManager.getInstance()

    @RequiresApi(Build.VERSION_CODES.Q)
    fun initializeMediaProjection(
        resultCode: Int,
        data: Intent,
        audioDataCallback: (ByteArray) -> Unit
    ) {
        floatingButtonManager.show()

        audioCaptureManager = AudioCaptureManager(context).apply {

            this.audioDataCallback = { capturedData ->
                val processedData = audioPreprocessor.process(capturedData)

                serviceScope.launch {
                    audioSamplingService.processAudioData(processedData)
                }
                audioDataCallback(processedData)
            }
            initializeMediaProjection(resultCode, data)
        }

        screenCaptureService = ScreenCaptureService(
            context, 
            audioCaptureManager!!,
            textRecognitionCallback
        )

        floatingButtonManager.setOnCaptureListener {
            serviceScope.launch {
                screenCaptureService?.captureScreen()
                audioSamplingService.startSampling(audioSamplingCallback)
            }
        }
    }

    fun release() {
        screenCaptureService?.release()
        screenCaptureService = null

        floatingButtonManager.hide()

        // testAudioManager?.toggleTestMode()
        // testAudioManager = null

        audioCaptureManager?.release()
        audioCaptureManager = null

        serviceScope.cancel()
    }

    // fun getCurrentVolume(): Float {
    //     return testAudioManager?.currentVolume?.value ?: 0.8f
    // }

    // fun setVolume(volume: Float) {
    //     testAudioManager?.setTestVolume(volume)
    // }
}