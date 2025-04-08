package com.example.musicvibrationapp.view.spectrum.renderers

class AnticipationAudioAnalyzer {
    
    private val vocalDetectionThreshold = 0.6f
    
    fun analyzeAudio(
        data: FloatArray,
        currentVolume: Float,
        previousVolume: Float,
        volumeChange: Float,
        dominantFreqIndex: Int,
        rhythmIntensity: Int
    ): AnticipationParticleRenderer.AudioFeatures {
        
        if (currentVolume < 0.01f && data.sum() < 0.01f) {
            return AnticipationParticleRenderer.AudioFeatures(
                totalEnergy = 0f,
                lowFreqEnergy = 0f,
                midFreqEnergy = 0f,
                highFreqEnergy = 0f,
                rhythmIntensity = 0,
                volumeChange = 0f,
                hasVocals = false
            )
        }
        
        val totalEnergy = data.sum().coerceIn(0f, 1f)
        
        val (lowFreq, midFreq, highFreq) = analyzeFrequencyDistribution(data)
        
        val hasVocals = detectVocals(data, midFreq, highFreq)
        
        return AnticipationParticleRenderer.AudioFeatures(
            totalEnergy = totalEnergy,
            lowFreqEnergy = lowFreq,
            midFreqEnergy = midFreq,
            highFreqEnergy = highFreq,
            rhythmIntensity = rhythmIntensity,
            volumeChange = volumeChange,
            hasVocals = hasVocals
        )
    }
    
    private fun analyzeFrequencyDistribution(data: FloatArray): Triple<Float, Float, Float> {
        val lowBound = 0
        val midBound = data.size / 5
        val highBound = data.size / 2
        
        var lowSum = 0f
        var midSum = 0f
        var highSum = 0f
        
        for (i in data.indices) {
            when {
                i < midBound -> lowSum += data[i]
                i < highBound -> midSum += data[i]
                else -> highSum += data[i]
            }
        }
        
        val total = lowSum + midSum + highSum
        return if (total > 0) {
            Triple(
                lowSum / total,
                midSum / total,
                highSum / total
            )
        } else {
            Triple(0.33f, 0.33f, 0.33f)
        }
    }
    
    private fun detectVocals(data: FloatArray, midFreq: Float, highFreq: Float): Boolean {
        val midHighRatio = if (highFreq > 0) midFreq / highFreq else 0f
        return midHighRatio > vocalDetectionThreshold
    }
}