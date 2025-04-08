package com.example.musicvibrationapp.view.spectrum.renderers

import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

/**
 * Surprise emotion music response controller
 * Responsible for analyzing music features and controlling the timing and intensity of visual effects generation
 */
class SurpriseMusicResponseController {
    // Audio analysis status
    private var currentVolume = 0f
    private var previousVolume = 0f
    private var volumeChangeAccumulator = 0f
    
    // Spectrum analysis
    private var previousSpectrum = FloatArray(0)
    private var spectralFlux = 0f
    private var spectralFluxThreshold = 0.1f
    private var onsetDetected = false
    
    // Rhythm analysis
    private var beatEnergy = 0f
    private var beatEnergyThreshold = 0.15f
    private var beatDetected = false
    private var beatInterval = 0f
    private var timeSinceLastBeat = 0f
    
    // Melody change analysis
    private var melodyChangeAccumulator = 0f
    private var melodyChangeThreshold = 0.2f
    private var melodyChangeDetected = false
    
    // Effect generation control
    private var effectCooldowns = mutableMapOf<EffectType, Float>()
    private var lastGenerationTime = mutableMapOf<EffectType, Long>()
    private var effectProbabilities = mutableMapOf<EffectType, Float>()
    
    // Add new member variables
    private var previousSpectralFlux = 0f
    private var timeSinceLastOnset = 0f
    private var onsetStrength = 1.0f
    private var beatStrength = 1.0f
    private var timeSinceLastMelodyChange = 0f
    private var melodyChangeStrength = 1.0f
    private var energyConcentration = 0f
    private var totalEnergy = 0f
    
    // Initialization
    init {
        // Set base cooldown time (seconds) for each effect
        EffectType.values().forEach { type ->
            effectCooldowns[type] = when(type) {
                EffectType.PULSE_WAVE -> 0.8f
                EffectType.LIGHTNING -> 1.5f
                EffectType.VISUAL_ECHO -> 0.6f
                EffectType.FLASH_DOT -> 0.4f
                EffectType.BACKGROUND_GLOW -> 1.0f
                EffectType.FLASH_EFFECT -> 2.0f
            }
            lastGenerationTime[type] = 0L
            
            // Set base trigger probability for each effect
            effectProbabilities[type] = when(type) {
                EffectType.PULSE_WAVE -> 0.6f
                EffectType.LIGHTNING -> 0.3f
                EffectType.VISUAL_ECHO -> 0.5f
                EffectType.FLASH_DOT -> 0.7f
                EffectType.BACKGROUND_GLOW -> 0.4f
                EffectType.FLASH_EFFECT -> 0.2f
            }
        }
    }
    
    /**
     * Process audio data and analyze music features
     */
    fun processAudioData(
        processedData: FloatArray,
        deltaTime: Float,
        currentVolume: Float,
        totalEnergy: Float,
        energyConcentration: Float
    ) {
        // Update volume change - use smoother transition
        this.previousVolume = this.currentVolume
        this.currentVolume = currentVolume
        val volumeChange = abs(this.currentVolume - this.previousVolume)
        // Use lower weight to make accumulator change smoother
        volumeChangeAccumulator = (volumeChangeAccumulator * 0.8f) + (volumeChange * 0.2f)
        
        // Detect music onsets - improved spectrum analysis
        if (previousSpectrum.isEmpty()) {
            previousSpectrum = processedData.copyOf()
        } else {
            // Calculate spectral flux - detect spectrum changes
            var flux = 0f
            val bandWeights = generateBandWeights(processedData.size)
            
            // Analysis by frequency bands - separate low, mid, high frequencies
            var lowFreqFlux = 0f
            var midFreqFlux = 0f
            var highFreqFlux = 0f
            
            val lowFreqCutoff = processedData.size / 5
            val highFreqStart = processedData.size * 3 / 5
            
            for (i in processedData.indices) {
                // Only consider positive changes (increased energy)
                val diff = processedData[i] - previousSpectrum[i]
                if (diff > 0) {
                    // Give higher weight to mid-frequency bands
                    flux += diff * bandWeights[i]
                    
                    // Accumulate by frequency bands
                    when {
                        i < lowFreqCutoff -> lowFreqFlux += diff * 1.2f  // Low frequency weighting
                        i < highFreqStart -> midFreqFlux += diff * 1.5f  // Mid frequency higher weighting
                        else -> highFreqFlux += diff * 1.0f  // High frequency standard weighting
                    }
                }
            }
            
            // Update spectral flux, using weighted average
            spectralFlux = flux
            
            // Dynamically adjust threshold - based on current volume and energy concentration
            val dynamicThreshold = spectralFluxThreshold * 
                                  (0.5f + currentVolume * 0.5f) * 
                                  (0.8f + energyConcentration * 0.4f)
            
            // Detect onset - stricter conditions
            onsetDetected = spectralFlux > dynamicThreshold && 
                            spectralFlux > previousSpectralFlux * 1.2f && // Require significant growth
                            timeSinceLastOnset > 0.15f // Prevent too frequent detection
            
            if (onsetDetected) {
                timeSinceLastOnset = 0f
                onsetStrength = (spectralFlux / dynamicThreshold).coerceIn(1.0f, 3.0f)
            } else {
                timeSinceLastOnset += deltaTime
            }
            
            previousSpectralFlux = spectralFlux
            previousSpectrum = processedData.copyOf()
        }
        
        // Rhythm analysis - mainly focus on low frequency energy, reference JoyParticleRenderer implementation
        var lowFreqEnergy = 0f
        val lowFreqCutoff = processedData.size / 5 // About the first 20% of frequency bands
        
        for (i in 0 until lowFreqCutoff) {
            lowFreqEnergy += processedData[i]
        }
        lowFreqEnergy /= lowFreqCutoff
        
        // Beat detection - use smoother energy tracking
        val prevBeatEnergy = beatEnergy
        // Slower energy accumulation, reduce false triggers
        beatEnergy = (beatEnergy * 0.8f) + (lowFreqEnergy * 0.2f)
        
        // Calculate energy change rate
        val beatEnergyChange = beatEnergy - prevBeatEnergy
        
        // Dynamically adjust beat threshold - based on current volume and energy concentration
        val dynamicBeatThreshold = beatEnergyThreshold * 
                                  (0.7f + currentVolume * 0.6f) * 
                                  (0.8f + energyConcentration * 0.4f)
        
        // Detect beats - stricter conditions
        // 1. Current energy must exceed threshold
        // 2. Energy change must be significant
        // 3. Must have sufficient time since last beat
        beatDetected = beatEnergy > dynamicBeatThreshold && 
                      beatEnergyChange > dynamicBeatThreshold * 0.25f &&
                      timeSinceLastBeat > 0.25f // Increase minimum interval, reduce frequent triggers
        
        if (beatDetected) {
            beatInterval = timeSinceLastBeat
            timeSinceLastBeat = 0f
            // Calculate beat strength - used to adjust effect intensity
            beatStrength = (beatEnergy / dynamicBeatThreshold).coerceIn(1.0f, 3.0f)
        } else {
            timeSinceLastBeat += deltaTime
        }
        
        // Melody change analysis - focus on mid-high frequency bands, using more detailed analysis
        var melodyEnergy = 0f
        var midFreqEnergy = 0f
        var highFreqEnergy = 0f
        
        val midFreqStart = processedData.size / 5
        val highFreqStart = processedData.size * 3 / 5
        
        // Calculate energy by frequency bands
        for (i in midFreqStart until highFreqStart) {
            midFreqEnergy += processedData[i]
        }
        midFreqEnergy /= (highFreqStart - midFreqStart)
        
        for (i in highFreqStart until processedData.size) {
            highFreqEnergy += processedData[i]
        }
        highFreqEnergy /= (processedData.size - highFreqStart)
        
        // Weighted average, mid-frequency has more influence on melody
        melodyEnergy = midFreqEnergy * 0.7f + highFreqEnergy * 0.3f
        
        // Save previous frame's melody energy
        val previousMelodyEnergy = melodyChangeAccumulator
        
        // Smoother melody energy accumulation
        melodyChangeAccumulator = (melodyChangeAccumulator * 0.85f) + (melodyEnergy * 0.15f)
        
        // Calculate melody change rate
        val melodyChangeRate = abs(melodyChangeAccumulator - previousMelodyEnergy)
        
        // Dynamically adjust melody change threshold
        val dynamicMelodyThreshold = melodyChangeThreshold * 
                                    (0.7f + currentVolume * 0.6f) * 
                                    (0.8f + energyConcentration * 0.4f)
        
        // Detect significant melody changes - stricter conditions
        melodyChangeDetected = melodyChangeRate > dynamicMelodyThreshold && 
                              timeSinceLastMelodyChange > 0.3f // Prevent too frequent detection
        
        if (melodyChangeDetected) {
            timeSinceLastMelodyChange = 0f
            melodyChangeStrength = (melodyChangeRate / dynamicMelodyThreshold).coerceIn(1.0f, 3.0f)
        } else {
            timeSinceLastMelodyChange += deltaTime
        }
        
        // Update energy concentration - used to determine music "focus"
        this.energyConcentration = energyConcentration
        
        // Update total energy - used for overall intensity control
        this.totalEnergy = totalEnergy
    }
    
    /**
     * Generate band weights - give higher weight to mid-frequency bands
     */
    private fun generateBandWeights(size: Int): FloatArray {
        val weights = FloatArray(size)
        val midPoint = size / 2
        
        for (i in 0 until size) {
            // Create a bell curve, mid frequencies get highest weight
            val normalizedPos = abs(i - midPoint) / (size / 2f)
            weights[i] = (1f - normalizedPos.pow(2)).coerceIn(0.2f, 1f)
        }
        
        return weights
    }
    
    /**
     * Check if a specific type of effect should be generated
     * @param effectType Effect type
     * @param intensity Current audio intensity (0-1)
     * @return Whether to generate the effect and its intensity
     */
    fun shouldGenerateEffect(effectType: EffectType, intensity: Float): Pair<Boolean, Float> {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastGenerationTime[effectType] ?: 0L
        
        // Get base cooldown time
        val baseCooldown = effectCooldowns[effectType] ?: 1.0f
        
        // Dynamic cooldown time - based on music state
        val dynamicCooldown = calculateDynamicCooldown(effectType, baseCooldown)
        
        // Base cooldown check
        val timeSinceLastGeneration = (currentTime - lastTime) / 1000f
        if (timeSinceLastGeneration < dynamicCooldown) {
            return Pair(false, 0f)
        }
        
        // Calculate generation probability based on effect type and music features
        val generationProbability = calculateProbability(effectType, intensity)
        
        // Apply random factors - using layered random system
        val randomValue = Random.nextFloat()
        val shouldGenerate = randomValue < generationProbability
        
        if (shouldGenerate) {
            // Update last generation time
            lastGenerationTime[effectType] = currentTime
            
            // Calculate effect intensity
            val effectIntensity = calculateEffectIntensity(effectType, intensity)
            
            return Pair(true, effectIntensity)
        }
        
        return Pair(false, 0f)
    }
    
    /**
     * Calculate dynamic cooldown time - based on music state
     */
    private fun calculateDynamicCooldown(effectType: EffectType, baseCooldown: Float): Float {
        var cooldown = baseCooldown
        
        // Increase cooldown time at low volume
        if (currentVolume < 0.3f) {
            cooldown *= 1.0f + (0.3f - currentVolume) * 5.0f
        }
        
        // Adjust cooldown time based on effect type and music features
        when (effectType) {
            EffectType.PULSE_WAVE -> {
                // Pulse wave is closely related to beats
                if (beatDetected) {
                    // Significantly reduce cooldown when beat detected
                    cooldown *= 0.3f
                } else if (timeSinceLastBeat < beatInterval * 0.5f) {
                    // Slightly reduce cooldown during brief period after beat
                    cooldown *= 0.7f
                } else {
                    // Increase cooldown at other times
                    cooldown *= 1.5f
                }
            }
            EffectType.LIGHTNING -> {
                // Lightning is sensitive to sudden volume changes
                if (volumeChangeAccumulator > 0.15f) {
                    cooldown *= 0.5f
                } else {
                    // Significantly increase cooldown when volume change is small
                    cooldown *= 2.0f
                }
            }
            EffectType.VISUAL_ECHO -> {
                // Visual echo is sensitive to melody changes and onsets
                if (melodyChangeDetected || onsetDetected) {
                    cooldown *= 0.6f
                } else {
                    cooldown *= 1.3f
                }
            }
            EffectType.FLASH_DOT -> {
                // Flash dots are sensitive to high frequency energy and beats
                if (onsetDetected) {
                    cooldown *= 0.7f
                } else if (beatDetected) {
                    cooldown *= 0.8f
                } else {
                    cooldown *= 1.2f
                }
            }
            EffectType.BACKGROUND_GLOW -> {
                // Background glow is sensitive to overall volume and melody changes
                if (currentVolume > 0.6f || melodyChangeDetected) {
                    cooldown *= 0.8f
                } else {
                    cooldown *= 1.1f
                }
            }
            EffectType.FLASH_EFFECT -> {
                // Flash effect is sensitive to extreme volume changes
                if (volumeChangeAccumulator > 0.2f) {
                    cooldown *= 0.4f
                } else {
                    // Significantly increase cooldown to make flash effects more rare
                    cooldown *= 3.0f
                }
            }
        }
        
        // Ensure cooldown time is within reasonable range
        return cooldown.coerceIn(baseCooldown * 0.2f, baseCooldown * 5.0f)
    }
    
    /**
     * Calculate effect generation probability
     */
    private fun calculateProbability(effectType: EffectType, intensity: Float): Float {
        // Base probability - overall reduction to decrease generation frequency
        var probability = (effectProbabilities[effectType] ?: 0.5f) * 0.7f
        
        // Adjust probability based on music features
        when (effectType) {
            EffectType.PULSE_WAVE -> {
                // Pulse wave is sensitive to beats and volume changes
                if (beatDetected) {
                    // Significantly increase probability when beat detected
                    probability *= 3.0f * beatStrength
                } else if (timeSinceLastBeat < beatInterval * 0.3f) {
                    // Slightly increase probability during brief period after beat
                    probability *= 1.5f
                } else {
                    // Significantly decrease probability at other times
                    probability *= 0.2f
                }
                
                // Volume change influence
                probability *= (1.0f + volumeChangeAccumulator * 2.0f)
            }
            EffectType.LIGHTNING -> {
                // Lightning is sensitive to sudden volume changes and high intensity
                if (volumeChangeAccumulator > 0.15f) {
                    probability *= 2.0f + volumeChangeAccumulator * 5.0f
                } else {
                    // Almost no generation when volume change is small
                    probability *= 0.1f
                }
                
                // Easier to generate at high intensity
                if (intensity > 0.7f) probability *= 1.5f
                
                // Easier to generate when energy concentration is high
                if (energyConcentration > 0.6f) probability *= 1.3f
            }
            EffectType.VISUAL_ECHO -> {
                // Visual echo is sensitive to melody changes and onsets
                if (melodyChangeDetected) {
                    probability *= 2.5f * melodyChangeStrength
                } else if (onsetDetected) {
                    probability *= 2.0f * onsetStrength
                } else {
                    // Significantly decrease probability when no special events
                    probability *= 0.15f
                }
            }
            EffectType.FLASH_DOT -> {
                // Flash dots are sensitive to high frequency energy and beats
                if (onsetDetected) {
                    probability *= 2.0f * onsetStrength
                } else if (beatDetected) {
                    probability *= 1.5f * beatStrength
                } else {
                    // Decrease probability when no special events
                    probability *= 0.3f
                }
                
                // Total energy influence
                probability *= (0.5f + totalEnergy * 1.5f)
            }
            EffectType.BACKGROUND_GLOW -> {
                // Background glow is sensitive to overall volume and melody changes
                probability *= (0.3f + currentVolume * 1.5f)
                
                if (melodyChangeDetected) {
                    probability *= 1.8f * melodyChangeStrength
                }
                
                // Easier to generate when energy concentration is low (music is more dispersed)
                if (energyConcentration < 0.4f) probability *= 1.3f
            }
            EffectType.FLASH_EFFECT -> {
                // Flash effect is sensitive to extreme volume changes
                if (volumeChangeAccumulator > 0.2f) {
                    probability *= 3.0f + volumeChangeAccumulator * 5.0f
                } else {
                    // Almost no generation when volume change is small
                    probability *= 0.05f
                }
                
                // Easier to generate when total energy is high
                if (totalEnergy > 0.7f) probability *= 1.5f
            }
        }
        
        // Global volume influence - significantly decrease probability at low volume
        if (currentVolume < 0.3f) {
            probability *= currentVolume * 2.0f
        }
        
        // Ensure probability is within reasonable range - upper limit reduced to 0.85 to decrease generation frequency
        return probability.coerceIn(0.01f, 0.85f)
    }
    
    /**
     * Calculate effect intensity
     */
    private fun calculateEffectIntensity(effectType: EffectType, baseIntensity: Float): Float {
        var intensity = baseIntensity
        
        // Adjust intensity based on music features
        when (effectType) {
            EffectType.PULSE_WAVE -> {
                // Pulse wave intensity is affected by beats
                if (beatDetected) {
                    intensity *= 1.0f + beatStrength * 0.3f
                }
                
                // Total energy influence
                intensity = (intensity + totalEnergy) / 2.0f
            }
            EffectType.LIGHTNING -> {
                // Lightning intensity is affected by volume changes
                intensity *= (1.0f + volumeChangeAccumulator * 3.0f)
                
                // Energy concentration influence - lightning is stronger when concentration is high
                if (energyConcentration > 0.6f) {
                    intensity *= 1.0f + (energyConcentration - 0.6f) * 2.0f
                }
            }
            EffectType.VISUAL_ECHO -> {
                // Visual echo intensity is affected by melody changes
                if (melodyChangeDetected) {
                    intensity *= 1.0f + melodyChangeStrength * 0.3f
                } else if (onsetDetected) {
                    intensity *= 1.0f + onsetStrength * 0.2f
                }
                
                // Current volume influence
                intensity = (intensity + currentVolume) / 2.0f
            }
            EffectType.FLASH_DOT -> {
                // Flash dot intensity is affected by onsets
                if (onsetDetected) {
                    intensity *= 1.0f + onsetStrength * 0.3f
                } else if (beatDetected) {
                    intensity *= 1.0f + beatStrength * 0.2f
                }
                
                // Total energy influence
                intensity = (intensity * 2.0f + totalEnergy) / 3.0f
            }
            EffectType.BACKGROUND_GLOW -> {
                // Background glow intensity is affected by overall volume
                intensity = (intensity + currentVolume * 2.0f) / 3.0f
                
                // Melody change influence
                if (melodyChangeDetected) {
                    intensity *= 1.0f + melodyChangeStrength * 0.2f
                }
            }
            EffectType.FLASH_EFFECT -> {
                // Flash effect intensity is affected by volume changes
                intensity *= (1.0f + volumeChangeAccumulator * 4.0f)
                
                // Total energy influence
                intensity = (intensity + totalEnergy) / 2.0f
            }
        }
        
        // Ensure intensity is within reasonable range
        return intensity.coerceIn(0.1f, 1.0f)
    }
    
    /**
     * Get current music state information
     */
    fun getMusicState(): MusicState {
        return MusicState(
            volume = currentVolume,
            volumeChange = volumeChangeAccumulator,
            isOnsetDetected = onsetDetected,
            onsetStrength = onsetStrength,
            isBeatDetected = beatDetected,
            beatStrength = beatStrength,
            isMelodyChangeDetected = melodyChangeDetected,
            melodyChangeStrength = melodyChangeStrength,
            energyConcentration = energyConcentration,
            totalEnergy = totalEnergy
        )
    }
    
    /**
     * Effect type enumeration
     */
    enum class EffectType {
        PULSE_WAVE,
        LIGHTNING,
        VISUAL_ECHO,
        FLASH_DOT,
        BACKGROUND_GLOW,
        FLASH_EFFECT
    }
    
    /**
     * Music state data class
     */
    data class MusicState(
        val volume: Float,
        val volumeChange: Float,
        val isOnsetDetected: Boolean,
        val onsetStrength: Float,
        val isBeatDetected: Boolean,
        val beatStrength: Float,
        val isMelodyChangeDetected: Boolean,
        val melodyChangeStrength: Float,
        val energyConcentration: Float,
        val totalEnergy: Float
    )
} 