package com.example.musicvibrationapp.emotion.api

import android.content.Context
import android.util.Log
import com.example.musicvibrationapp.emotion.audio.AudioFeatureStats
import com.example.musicvibrationapp.emotion.audio.AudioFeatures
import com.example.musicvibrationapp.emotion.audio.AudioSamplingCallback
import com.example.musicvibrationapp.emotion.image.TextRecognitionCallback
import com.example.musicvibrationapp.emotion.TaskCompletionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AnalysisCoordinator(
    private val context: Context,
    private val llmService: LLMService
) {
    private val TAG = "LLM_Analysis"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var audioResult: AudioFeatureStats? = null
    private var audioFeatures: List<AudioFeatures>? = null
    private var textResult: String? = null
    
    private val _analysisState = MutableStateFlow<AnalysisResult?>(null)
    val analysisState: StateFlow<AnalysisResult?> = _analysisState

    private var onAnalysisComplete: ((String) -> Unit)? = null
    
    val audioCallback = object : AudioSamplingCallback {
        override fun onSamplingComplete(features: List<AudioFeatures>, stats: AudioFeatureStats) {
            Log.d(TAG, "LLM_Audio: Sampling complete with ${features.size} features")
            audioResult = stats
            audioFeatures = features.toList()
            Log.d(TAG, "LLM_Audio: First sample pitch=${features.firstOrNull()?.pitch ?: "N/A"}")
            Log.d(TAG, "LLM_Audio: Stats summary - Energy: ${stats.meanEnergy}")
            checkAndProcessResults()
        }
        
        override fun onError(error: Exception) {
            Log.e(TAG, "LLM_Audio: Sampling error", error)
            audioResult = null
            audioFeatures = null
            checkAndProcessResults()
        }
        
        override fun onCleanup() {
            Log.d(TAG, "LLM_Audio: Cleanup triggered")
            audioResult = null
            audioFeatures = null
        }
    }
    
    val textCallback = object : TextRecognitionCallback {
        override fun onTextRecognized(text: String) {
            Log.d(TAG, "LLM_Text: Recognition complete - ${text.take(50)}...")
            textResult = text
            checkAndProcessResults()
        }
        
        override fun onError(error: Exception) {
            Log.e(TAG, "LLM_Text: Recognition error", error)
            textResult = null
            checkAndProcessResults()
        }
    }
    
    private fun checkAndProcessResults() {
        // Add logs to track data
        Log.d(TAG, "LLM_Debug: Features size=${audioFeatures?.size}, Stats=${audioResult != null}")
        
        val currentResult = AnalysisResult(
            audioStats = audioResult,
            audioFeatures = audioFeatures,
            recognizedText = textResult
        )
        _analysisState.value = currentResult
        
        Log.d(TAG, "LLM_State: Audio=${audioResult != null}, Features=${audioFeatures?.size ?: 0}, Text=${textResult != null}")
        
        if (currentResult.isComplete()) {
            Log.d(TAG, "LLM_Process: Starting complete result processing")
            processCompleteResult(currentResult)
        }
    }
    
    private fun processCompleteResult(result: AnalysisResult) {
        scope.launch {
            try {
                val prompt = buildPrompt(result)
                Log.d(TAG, "LLM_Prompt: Generated prompt - ${prompt.take(100)}...")
                
                val response = llmService.sendMessage(prompt)
                Log.d(TAG, "LLM_Response: Received response - ${response.take(100)}...")
                
                result.showResult(context, response)
                
                onAnalysisComplete?.invoke(response)
                
                Log.d(TAG, "LLM_Complete: Analysis cycle completed")
                resetResults()
                
            } catch (e: Exception) {
                Log.e(TAG, "LLM_Error: Error processing result", e)
            }
        }
    }
    
    private fun buildPrompt(result: AnalysisResult): String {
        return buildString {
            append("Please base your analysis on the following audio sequence data and video text information.\n\n")

            append("Sampling Information:\n")
            append("- length of time: ${result.audioStats?.duration ?: 0}ms\n")
            append("- sample size: ${result.audioFeatures?.size ?: 0}\n\n")

            val isInvalidAudio = result.audioFeatures?.let { features ->
                val avgEnergy = features.map { it.energy }.average()
                val avgLoudness = features.map { it.loudness }.average()
                avgEnergy < 0.001f && avgLoudness < -60f
            } ?: true

            if (isInvalidAudio) {
                append("Warning: possible mute or invalid audio detected\n")
                append("- If the average energy is close to 0 and the loudness is very low, a mute recording may have been made\n")
                append("- If there are no valid audio features, the recording may have failed\n\n")
            }
            
            result.audioFeatures?.let { features ->
                append("Pitch Sequence (Hz):\n")
                append(features.map { String.format("%.1f", it.pitch) }.joinToString(", "))
                append("\n\n")
                
                append("Pitch Name Sequence:\n")
                append(features.map { it.pitchName }.joinToString(", "))
                append("\n\n")
                
                append("Loudness Sequence (dB):\n")
                append(features.map { String.format("%.1f", it.loudness) }.joinToString(", "))
                append("\n\n")
                
                append("Dynamics Sequence:\n")
                append(features.map { it.dynamics }.joinToString(", "))
                append("\n\n")
                
                append("Energy Sequence:\n")
                append(features.map { String.format("%.3f", it.energy) }.joinToString(", "))
                append("\n\n")
                
                append("Timbral Brightness Sequence:\n")
                append(features.map { String.format("%.3f", it.brightness) }.joinToString(", "))
                append("\n\n")
                
                append("Harmonic Complexity Sequence:\n")
                append(features.map { String.format("%.3f", it.harmonicComplexity) }.joinToString(", "))
                append("\n\n")
            }
            
            result.audioStats?.let { stats ->
                append("Statistical Features:\n")
                append("- Mean Energy: ${String.format("%.3f", stats.meanEnergy)}\n")
                append("- Energy Variance: ${String.format("%.3f", stats.energyVariance)}\n")
                append("- Mean Pitch: ${String.format("%.1f", stats.meanPitch)}Hz\n")
                append("- Pitch Variance: ${String.format("%.3f", stats.pitchVariance)}\n")
                append("- Mean Brightness: ${String.format("%.3f", stats.meanBrightness)}\n")
                append("- Brightness Variance: ${String.format("%.3f", stats.brightnessVariance)}\n\n")
            }
            
            append("Video Text Content:\n")
            append(result.recognizedText ?: "No text data")
            append("\n\n")
            append("Normal Emotion Categories (only use when valid audio features are detected):\n")
            append("1. Joy\n")
            append("2. Trust\n")
            append("3. Fear\n")
            append("4. Surprise\n")
            append("5. Sadness\n")
            append("6. Disgust\n")
            append("7. Anger\n")
            append("8. Anticipation\n\n")
            
            append("Special Case Categories:\n")
            append("9. Invalid (Invalid Audio) - only use in the following cases:\n")
            append("   - Mean energy is close to 0 and loudness is extremely low\n")
            append("   - No valid audio features are detected\n")
            append("   Note: If any meaningful change in audio features can be detected, even if the loudness is low, you should choose one of the 8 basic emotions above\n\n")
            
            append("Requirements:\n")
            append("1. Must choose one from the above 9 categories\n")
            append("2. Must choose from the 8 basic emotions unless strictly meeting the invalid audio conditions\n")
            append("3. The answer format must start with \"Primary Emotion: [Emotion Type]\"\n")
            append("4. You may briefly explain the basis for your judgment\n")
        }.also {
            Log.v(TAG, "LLM_Prompt: Full prompt content:\n$it")
        }
    }
    
    private fun resetResults() {
        Log.d(TAG, "LLM_Reset: Resetting analysis state")
        audioResult = null
        audioFeatures = null
        textResult = null
        _analysisState.value = null
    }
    
    fun reset() {
        resetResults()
    }
    
    fun setOnAnalysisCompleteListener(listener: (String) -> Unit) {
        onAnalysisComplete = listener
        Log.d(TAG, "LLM_Config: Analysis complete listener set")
    }
} 