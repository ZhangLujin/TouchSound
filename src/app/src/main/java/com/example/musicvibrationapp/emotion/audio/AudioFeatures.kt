package com.example.musicvibrationapp.emotion.audio

data class AudioFeatures(
    val amplitude: Float,
    val energy: Float,
    val fundamentalFrequency: Float,
    val spectralCentroid: Float,
    val spectralSpread: Float,
    val spectralRolloff: Float,
    val brightness: Float,
    val roughness: Float,
    val spectralFlux: Float,
    val harmonicComplexity: Float,
    val spectrum: FloatArray,
    val harmonicContent: FloatArray,
    val timestamp: Long = System.currentTimeMillis(),
    val pitch: Float = fundamentalFrequency,
    val pitchName: String = "",
    val loudness: Float = 0f,
    val dynamics: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioFeatures

        if (timestamp != other.timestamp) return false
        if (amplitude != other.amplitude) return false
        if (energy != other.energy) return false
        if (!spectrum.contentEquals(other.spectrum)) return false
        if (!harmonicContent.contentEquals(other.harmonicContent)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + amplitude.hashCode()
        result = 31 * result + energy.hashCode()
        result = 31 * result + spectrum.contentHashCode()
        result = 31 * result + harmonicContent.contentHashCode()
        return result
    }

    fun clear() {
        // Clean up resources (if needed)
    }
}

data class AudioAnalysisResult(
    val duration: Long,
    val sampleCount: Int,
    val pitchSeries: List<Float>,
    val loudnessSeries: List<Float>,
    val tempoSeries: List<Float>,
    val harmonySeries: List<String>,
    
    val pitchStats: PitchStatistics,
    val rhythmStats: RhythmStatistics,
    val timbreStats: TimbreStatistics,
    val harmonyStats: HarmonyStatistics,
    val emotionStats: EmotionStatistics
) {
    data class PitchStatistics(
        val meanPitch: Float,
        val pitchRange: Float,
        val dominantPitchClass: Int,
        val pitchVariety: Float
    )
    
    data class RhythmStatistics(
        val meanTempo: Float,
        val tempoVariability: Float,
        val commonPatterns: List<String>,
        val beatStrength: Float
    )
    
    data class TimbreStatistics(
        val meanBrightness: Float,
        val brightnessVariation: Float,
        val timbralComplexity: Float,
        val spectralFlux: Float
    )
    
    data class HarmonyStatistics(
        val keySignature: String,
        val commonChords: List<String>,
        val harmonicComplexity: Float,
        val modalityStrength: Float
    )
    
    data class EmotionStatistics(
        val meanValence: Float,
        val meanArousal: Float,
        val emotionalDynamics: Float,
        val emotionalStability: Float
    )
    
    override fun toString(): String {
        return buildString {
            append("=== Music Analysis Report ===\n")
            append("Duration: ${duration}ms, Samples: $sampleCount\n\n")
            
            append("Pitch Analysis:\n")
            append("- Mean Pitch: ${pitchStats.meanPitch}Hz\n")
            append("- Pitch Range: ${pitchStats.pitchRange}Hz\n")
            append("- Dominant Pitch: ${getPitchName(pitchStats.dominantPitchClass)}\n")
            append("- Pitch Variety: ${pitchStats.pitchVariety}\n\n")
            
            append("Rhythm Analysis:\n")
            append("- Mean Tempo: ${rhythmStats.meanTempo}BPM\n")
            append("- Common Patterns: ${rhythmStats.commonPatterns.joinToString()}\n")
            append("- Beat Strength: ${rhythmStats.beatStrength}\n\n")
            
            append("Timbre Analysis:\n")
            append("- Mean Brightness: ${timbreStats.meanBrightness}\n")
            append("- Timbral Complexity: ${timbreStats.timbralComplexity}\n")
            append("- Spectral Flux: ${timbreStats.spectralFlux}\n\n")
            
            append("Harmony Analysis:\n")
            append("- Key: ${harmonyStats.keySignature}\n")
            append("- Common Chords: ${harmonyStats.commonChords.joinToString()}\n")
            append("- Harmonic Complexity: ${harmonyStats.harmonicComplexity}\n\n")
            
            append("Emotion Analysis:\n")
            append("- Mean Valence: ${emotionStats.meanValence}\n")
            append("- Mean Arousal: ${emotionStats.meanArousal}\n")
            append("- Emotional Stability: ${emotionStats.emotionalStability}\n")
        }
    }
    
    private fun getPitchName(pitchClass: Int): String {
        return arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")[pitchClass]
    }
} 