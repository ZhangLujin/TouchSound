package com.example.musicvibrationapp.view.spectrum.renderers

import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Sad Audio Analyzer
 * Focuses on analyzing audio features, especially low frequency response and emotional rhythm detection
 */
class SadnessAudioAnalyzer {
    
    // low frequency energy accumulation
    private var lowFrequencyEnergy = 0f
    private var lowFrequencyEnergySmoothed = 0f
    
    // rhythm detection
    private var rhythmPeakHistory = mutableListOf<Long>()
    private var lastRhythmPeakTime = 0L
    private var averageRhythmInterval = 0f
    
    // emotional features
    private var melancholyIndex = 0f  // melancholy index - low frequency persistence
    private var tensionIndex = 0f     // tension index - spectrum variation
    
    // previous frame volume
    private var previousVolume = 0f
    
    /**
     * analyze audio data
     */
    fun analyzeAudioData(data: FloatArray, deltaTime: Float): AudioAnalysisResult {
        // analyze low frequency energy (first 20% of frequencies)
        val lowFreqRange = (data.size * 0.2).toInt()
        var currentLowFreqEnergy = 0f
        for (i in 0 until lowFreqRange) {
            currentLowFreqEnergy += data[i]
        }
        currentLowFreqEnergy /= lowFreqRange
        
        // smooth low frequency energy changes
        lowFrequencyEnergy = currentLowFreqEnergy
        lowFrequencyEnergySmoothed += (lowFrequencyEnergy - lowFrequencyEnergySmoothed) * 0.1f
        
        // calculate current volume
        val currentVolume = calculateVolume(data)
        
        // calculate volume change
        val volumeChange = currentVolume - previousVolume
        previousVolume = currentVolume
        
        // calculate spectrum variation - used for tension index
        val spectrumVariation = calculateSpectrumVariation(data)
        
        // update emotional features
        updateEmotionalFeatures(lowFrequencyEnergySmoothed, spectrumVariation, deltaTime)
        
        // detect rhythm peak
        val (isRhythmPeak, rhythmIntensity) = detectRhythmPeak(currentVolume, volumeChange)
        
        // update rhythm interval
        if (isRhythmPeak) {
            val currentTime = System.currentTimeMillis()
            if (lastRhythmPeakTime > 0) {
                val interval = currentTime - lastRhythmPeakTime
                rhythmPeakHistory.add(interval)
                
                // keep history in reasonable range
                if (rhythmPeakHistory.size > 10) {
                    rhythmPeakHistory.removeAt(0)
                }
                
                // calculate average rhythm interval
                averageRhythmInterval = rhythmPeakHistory.average().toFloat()
            }
            lastRhythmPeakTime = currentTime
        }
        
        // return analysis result
        return AudioAnalysisResult(
            lowFrequencyEnergy = lowFrequencyEnergySmoothed,
            isRhythmPeak = isRhythmPeak,
            rhythmIntensity = rhythmIntensity,
            melancholyIndex = melancholyIndex,
            tensionIndex = tensionIndex,
            averageRhythmInterval = averageRhythmInterval,
            currentVolume = currentVolume,
            volumeChange = volumeChange
        )
    }
    
    /**
     * calculate volume
     */
    private fun calculateVolume(data: FloatArray): Float {
        var sum = 0f
        for (i in data.indices) {
            sum += data[i] * data[i]
        }
        return sqrt(sum / data.size)
    }
    
    /**
     * calculate spectrum variation
     */
    private fun calculateSpectrumVariation(data: FloatArray): Float {
        var variation = 0f
        for (i in 1 until data.size) {
            variation += kotlin.math.abs(data[i] - data[i-1])
        }
        return variation / data.size
    }
    
    /**
     * update emotional features
     */
    private fun updateEmotionalFeatures(
        lowFreqEnergy: Float, 
        spectrumVariation: Float, 
        deltaTime: Float
    ) {
        // update melancholy index - higher low frequency persistence, stronger melancholy
        val targetMelancholy = lowFreqEnergy.pow(1.5f) * 2f
        melancholyIndex += (targetMelancholy - melancholyIndex) * 0.1f * deltaTime
        
        // update tension index - higher spectrum variation, stronger tension
        val targetTension = spectrumVariation * 5f
        tensionIndex += (targetTension - tensionIndex) * 0.2f * deltaTime
    }
    
    /**
     * detect rhythm peak
     */
    private fun detectRhythmPeak(currentVolume: Float, volumeChange: Float): Pair<Boolean, Int> {
        // further reduce threshold, increase sensitivity
        val baseThreshold = 0.01f  // reduce from 0.015f to 0.01f
        val dynamicThreshold = baseThreshold * (1.0f - melancholyIndex * 0.2f)
        
        // detect rhythm peak - reduce threshold, increase random trigger
        val isRhythmPeak = volumeChange > dynamicThreshold || Random.nextFloat() < 0.08f  // increase from 0.05f to 0.08f
        
        // rhythm intensity分级 - reduce threshold
        val rhythmIntensity = when {
            volumeChange > 0.06f -> 3   // strong rhythm (reduce from 0.08)
            volumeChange > 0.03f -> 2  // medium rhythm (reduce from 0.04)
            volumeChange > dynamicThreshold -> 1  // mild rhythm
            else -> 1  // ensure minimum intensity is 1, not 0
        }
        
        return Pair(isRhythmPeak, rhythmIntensity)
    }
    
    /**
     * audio analysis result data class
     */
    data class AudioAnalysisResult(
        val lowFrequencyEnergy: Float,
        val isRhythmPeak: Boolean,
        val rhythmIntensity: Int,
        val melancholyIndex: Float,
        val tensionIndex: Float,
        val averageRhythmInterval: Float,
        val currentVolume: Float,
        val volumeChange: Float
    )
}