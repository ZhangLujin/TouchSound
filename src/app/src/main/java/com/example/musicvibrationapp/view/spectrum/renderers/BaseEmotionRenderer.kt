package com.example.musicvibrationapp.view.spectrum.renderers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.musicvibrationapp.R
import com.example.musicvibrationapp.view.DisplayMode
import com.example.musicvibrationapp.view.EmotionColorManager
import com.example.musicvibrationapp.view.SpectrumParameters
import com.example.musicvibrationapp.view.spectrum.EmotionRenderStrategy
import kotlin.math.log10
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Base abstract class for emotion renderers
 * Provides common audio analysis and basic rendering functionality
 */
abstract class BaseEmotionRenderer(protected val context: Context) : EmotionRenderStrategy {

    // Basic drawing tools
    protected val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    // Time and state tracking
    protected var lastUpdateTime = System.currentTimeMillis()
    protected var deltaTime = 0f
    
    // Audio analysis status
    protected var previousData = FloatArray(0)
    protected var currentVolume = 0f
    protected var previousVolume = 0f
    protected var volumeChange = 0f
    protected var isRhythmPeak = false
    protected var rhythmIntensity = 0 // 0=no rhythm, 1=mild, 2=moderate, 3=intense
    
    // Spectrum analysis results
    protected var dominantFreqIndex = -1
    protected var energyConcentration = 0f
    protected var totalEnergy = 0f
    
    // Energy accumulation
    protected var accumulatedEnergy = 0f
    
    // Flash effect
    protected var flashIntensity = 0f
    protected var flashDecay = 0.8f
    
    /**
     * Rhythm response coefficient - used to control the overall response of particles to rhythm
     */
    protected var rhythmPulseCoefficient: Float = 0f
    
    /**
     * Rhythm pulse decay rate - controls the duration of rhythm effects
     */
    protected var rhythmPulseDecayRate: Float = 2.5f
    
    /**
     * Music force field system - used to create overall rhythmic feel
     */
    protected var musicForceFieldStrength: Float = 0f
    protected var targetForceFieldStrength: Float = 0f
    protected var forceFieldDirection: Float = 0f  // Force field direction (radians)
    protected var forceFieldChangeRate: Float = 3.0f  // Force field change rate
    
    /**
     * Rhythm flow coefficient - used to create smooth rhythmic effects
     * This value changes smoothly with music energy and rhythm changes
     */
    protected var rhythmFlowCoefficient: Float = 0f
    
    /**
     * Target flow coefficient - the system will smoothly transition to this target value
     */
    protected var targetFlowCoefficient: Float = 0f
    
    /**
     * Flow coefficient smoothing factor - controls the smoothness of flow effects
     */
    protected var flowSmoothingFactor: Float = 5.0f
    
    /**
     * Process raw spectrum data
     * Performs common audio analysis, then calls subclass custom processing
     */
    override fun processData(rawData: FloatArray): FloatArray {
        // Calculate time difference
        val currentTime = System.currentTimeMillis()
        deltaTime = (currentTime - lastUpdateTime) / 1000f
        lastUpdateTime = currentTime
        
        // Initialize previousData
        if (previousData.size != rawData.size) {
            previousData = FloatArray(rawData.size) { 0f }
        }
        
        // Apply smoothing processing
        val smoothingFactor = SpectrumParameters.smoothingFactor.value
        val smoothedData = FloatArray(rawData.size) { i ->
            previousData[i] + (rawData[i] - previousData[i]) * smoothingFactor
        }
        previousData = smoothedData.copyOf()
        
        // Calculate total energy and volume
        totalEnergy = smoothedData.sum()
        val targetVolume = calculateVolume(smoothedData)
        
        // Improved volume change detection - using adaptive smoothing factor
        val riseSmoothingFactor = smoothingFactor * 2.5f  // Faster response when rising
        val fallSmoothingFactor = smoothingFactor * 0.7f  // Slower response when falling
        
        // Choose different smoothing factors based on volume change direction
        val adaptiveSmoothingFactor = if (targetVolume > currentVolume) 
            riseSmoothingFactor 
        else 
            fallSmoothingFactor
        
        currentVolume = previousVolume + (targetVolume - previousVolume) * adaptiveSmoothingFactor
        volumeChange = (currentVolume - previousVolume)
        previousVolume = currentVolume
        
        // Rhythm detection - using adaptive threshold
        val baseThreshold = 0.045f
        val dynamicThreshold = baseThreshold * (1.0f - energyConcentration * 0.35f)
        isRhythmPeak = volumeChange > dynamicThreshold
        
        // Rhythm intensity classification
        rhythmIntensity = when {
            volumeChange > 0.13f -> 3  // Intense rhythm
            volumeChange > 0.08f -> 2  // Moderate rhythm
            volumeChange > dynamicThreshold -> 1  // Mild rhythm
            else -> 0  // No rhythm
        }
        
        // Analyze dominant frequency and energy concentration
        val (dominantIndex, concentration) = analyzeDominantFrequency(smoothedData)
        dominantFreqIndex = dominantIndex
        energyConcentration = concentration
        
        // Update flash effect
        if (flashIntensity > 0.01f) {
            flashIntensity *= flashDecay
        } else {
            flashIntensity = 0f
        }
        
        // Update accumulated energy
        accumulatedEnergy += totalEnergy * deltaTime
        if (accumulatedEnergy > 10f) {
            accumulatedEnergy = 0f
        }
        
        // Update rhythm response system
        updateRhythmResponse(deltaTime)
        
        // Call subclass custom processing
        return onProcessData(smoothedData)
    }
    
    /**
     * Render spectrum
     * Select appropriate rendering direction based on display mode, then call subclass custom rendering
     */
    override fun render(canvas: Canvas, data: FloatArray, displayMode: DisplayMode, width: Int, height: Int, viewId: Int) {
        when (displayMode) {
            DisplayMode.TOP_BOTTOM -> {
                if (viewId == R.id.audioSpectrumViewTop || viewId == R.id.audioSpectrumViewBottom) {
                    onRender(canvas, data, width, height, false)
                }
            }
            DisplayMode.SIDES -> {
                if (viewId == R.id.audioSpectrumViewLeft || viewId == R.id.audioSpectrumViewRight) {
                    onRender(canvas, data, width, height, true)
                }
            }
        }
    }
    
    /**
     * Calculate volume
     */
    protected fun calculateVolume(data: FloatArray): Float {
        var sum = 0f
        for (i in data.indices) {
            sum += data[i] * data[i]
        }
        return sqrt(sum / data.size)
    }
    
    /**
     * Analyze dominant frequency and energy concentration
     */
    protected fun analyzeDominantFrequency(data: FloatArray): Pair<Int, Float> {
        if (data.size <= 1) return Pair(0, 0f)
        
        var maxEnergy = 0f
        var maxIndex = 0
        var totalEnergy = 0f
        
        // Use Mel weight to calculate energy
        val melSensitivity = SpectrumParameters.melSensitivity.value
        
        for (i in 1 until data.size) {
            val freq = i * (44100f / (data.size * 2))
            val weight = getMelWeight(freq, melSensitivity)
            val energy = data[i] * data[i] * weight
            totalEnergy += energy
            
            if (energy > maxEnergy) {
                maxEnergy = energy
                maxIndex = i
            }
        }

        // Calculate energy concentration
        val range = 3
        var centralEnergy = 0f
        for (i in maxIndex - range..maxIndex + range) {
            if (i in 1 until data.size) {
                val freq = i * (44100f / (data.size * 2))
                val weight = getMelWeight(freq, melSensitivity)
                centralEnergy += data[i] * data[i] * weight
            }
        }
        
        val concentration = if (totalEnergy > 0) centralEnergy / totalEnergy else 0f
        return Pair(maxIndex, concentration)
    }
    
    /**
     * Calculate Mel weight
     */
    protected fun getMelWeight(freq: Float, sensitivity: Float): Float {
        val mel = 2595 * log10(1 + freq / 700)
        return (1 + sensitivity * mel / 500f)
    }
    
    /**
     * Get the current emotion color
     */
    protected fun getEmotionColor(alpha: Int = 255): Int {
        val emotion = EmotionColorManager.getInstance().currentEmotion.value
        val baseColor = emotion.getColorScheme(0, 1)
        return Color.argb(
            alpha,
            Color.red(baseColor),
            Color.green(baseColor),
            Color.blue(baseColor)
        )
    }
    
    /**
     * Get the current emotion gradient colors
     */
    protected fun getEmotionGradientColors(height: Float, width: Float): IntArray {
        val emotion = EmotionColorManager.getInstance().currentEmotion.value
        return emotion.getGradientColors(height, width)
    }
    
    /**
     * Generate random number with range control
     */
    protected fun randomInRange(min: Float, max: Float): Float {
        return min + Random.nextFloat() * (max - min)
    }
    
    /**
     * Limit value within specified range
     */
    protected fun clamp(value: Float, min: Float, max: Float): Float {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }
    
    /**
     * Custom data processing method that subclasses need to implement
     * @param processedData Spectrum data after basic processing
     * @return Final processed data
     */
    protected abstract fun onProcessData(processedData: FloatArray): FloatArray
    
    /**
     * Custom rendering method that subclasses need to implement
     * @param canvas Canvas
     * @param data Processed spectrum data
     * @param width View width
     * @param height View height
     * @param isVertical Whether to render in vertical direction
     */
    protected abstract fun onRender(canvas: Canvas, data: FloatArray, width: Int, height: Int, isVertical: Boolean)
    
    /**
     * Update rhythm response system
     */
    protected fun updateRhythmResponse(deltaTime: Float) {
        // Update rhythm pulse coefficient - decay over time
        rhythmPulseCoefficient = (rhythmPulseCoefficient - rhythmPulseDecayRate * deltaTime).coerceAtLeast(0f)
        
        // Detect rhythm points - using graded response system
        if (isRhythmPeak || (totalEnergy > 0.6f && volumeChange > 0.15f)) {  // Strong rhythm condition
            // Increase pulse coefficient at strong rhythm point
            rhythmPulseCoefficient += 0.4f + rhythmIntensity * 0.4f
            
            // Change force field direction at strong rhythm point - larger range
            forceFieldDirection = (Math.random() * Math.PI * 0.4f - Math.PI * 0.2f + Math.PI * 0.5f).toFloat()
            
            // Increase force field strength at strong rhythm point
            targetForceFieldStrength = (0.18f + rhythmIntensity * 0.3f) * totalEnergy
        } 
        else if (totalEnergy > 0.5f && volumeChange > 0.1f) {  // Moderate rhythm condition
            // Increase pulse coefficient at moderate rhythm point
            rhythmPulseCoefficient += 0.3f + rhythmIntensity * 0.3f
            
            // Change force field direction at moderate rhythm point - medium range
            forceFieldDirection = (Math.random() * Math.PI * 0.3f - Math.PI * 0.15f + Math.PI * 0.5f).toFloat()
            
            // Increase force field strength at moderate rhythm point
            targetForceFieldStrength = (0.15f + rhythmIntensity * 0.25f) * totalEnergy
        }
        else if (totalEnergy > 0.4f && volumeChange > 0.05f) {  // Mild rhythm condition
            // Increase pulse coefficient at mild rhythm point
            rhythmPulseCoefficient += 0.2f + rhythmIntensity * 0.2f
            
            // Change force field direction at mild rhythm point - small range
            forceFieldDirection = (Math.random() * Math.PI * 0.2f - Math.PI * 0.1f + Math.PI * 0.5f).toFloat()
            
            // Increase force field strength at mild rhythm point
            targetForceFieldStrength = (0.1f + rhythmIntensity * 0.15f) * totalEnergy
        }
        
        // Smooth transition of force field strength
        musicForceFieldStrength += (targetForceFieldStrength - musicForceFieldStrength) * 
                                  forceFieldChangeRate * deltaTime
        
        // Force field strength naturally decays over time - adjust decay rate based on intensity level
        val decayRate = 1.6f + musicForceFieldStrength * 0.5f  // Higher intensity, faster decay
        targetForceFieldStrength *= (1f - deltaTime * decayRate)
    }
    
    /**
     * Apply music force field to particle velocity
     */
    protected fun applyMusicForceField(x: Float, y: Float, vx: Float, vy: Float): Pair<Float, Float> {
        if (musicForceFieldStrength <= 0.01f) return Pair(vx, vy)
        
        // Calculate distance and direction from particle to center
        val centerX = 0.5f
        val centerY = 0.5f
        val dx = x - centerX
        val dy = y - centerY
        val distanceToCenter = kotlin.math.sqrt(dx * dx + dy * dy)
        
        // Calculate unit vector for force field direction
        val forceX = kotlin.math.cos(forceFieldDirection).toFloat()
        val forceY = kotlin.math.sin(forceFieldDirection).toFloat()
        
        // Force field strength varies with distance from center, creating wave effect
        val distanceFactor = kotlin.math.sin(distanceToCenter * Math.PI * 2).toFloat() * 0.5f + 0.5f
        val appliedForce = musicForceFieldStrength * distanceFactor
        
        // Apply force field - use non-linear mapping to enhance differentiation
        val forceFactor = 0.12f * (1f + appliedForce * 0.5f)  // Higher intensity, more noticeable effect
        val newVx = vx + forceX * appliedForce * forceFactor
        val newVy = vy + forceY * appliedForce * forceFactor
        
        return Pair(newVx, newVy)
    }
    
    /**
     * Get rhythm pulse value - used for particle velocity adjustment
     */
    protected fun getRhythmPulseValue(): Float {
        return rhythmPulseCoefficient
    }
    
    /**
     * Get current rhythm flow value - used for smooth rhythmic effects
     */
    protected fun getRhythmFlowValue(): Float {
        return rhythmFlowCoefficient
    }
    
    /**
     * Apply rhythm response to particle velocity
     */
    protected fun applyRhythmResponseToVelocity(
        vx: Float, vy: Float,
        pulseWeight: Float = 0.5f,
        flowWeight: Float = 0.5f,
        directionFactor: Float = 1.0f
    ): Pair<Float, Float> {
        // Get rhythm pulse value
        val pulseValue = getRhythmPulseValue() * pulseWeight
        
        // Calculate acceleration factor - use square relationship to make the effect more obvious
        val accelerationFactor = 1.0f + pulseValue * pulseValue * 2.0f * directionFactor
        
        // Apply acceleration
        return Pair(vx * accelerationFactor, vy * accelerationFactor)
    }
} 