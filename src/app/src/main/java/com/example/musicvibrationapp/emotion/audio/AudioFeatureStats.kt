package com.example.musicvibrationapp.emotion.audio

data class AudioFeatureStats(
    val duration: Long,          // Sampling duration (ms)
    val sampleCount: Int,        // Sample count
    val meanEnergy: Float,       // Mean energy
    val energyVariance: Float,   // Energy variance
    val meanPitch: Float,        // Mean pitch
    val pitchVariance: Float,    // Pitch variance
    val meanBrightness: Float,   // Mean brightness
    val brightnessVariance: Float // Brightness variance
) {
    override fun toString(): String {
        return buildString {
            append("Duration: ${duration}ms, Samples: $sampleCount\n")
            append("Energy: mean=$meanEnergy, var=$energyVariance\n")
            append("Pitch: mean=$meanPitch, var=$pitchVariance\n")
            append("Brightness: mean=$meanBrightness, var=$brightnessVariance")
        }
    }
}