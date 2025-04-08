package com.example.musicvibrationapp.view.spectrum.renderers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.musicvibrationapp.view.EmotionColorManager
import com.example.musicvibrationapp.view.SpectrumParameters
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class AnticipationParticleRenderer(context: Context) : BaseEmotionRenderer(context) {
    
    private val particleSystem = AnticipationParticleSystem()
    
    private val effectSystem = AnticipationEffectSystem()
    
    private val audioAnalyzer = AnticipationAudioAnalyzer()
    
    private val colorManager = AnticipationColorManager()
    
    private val physicsSystem = AnticipationPhysics()
    
    private var screenWidth = 0
    private var screenHeight = 0
    private var isVerticalMode = false
    private var isInitialized = false
    
    private var baseParticleRate = 5f 
    private var particleRateMultiplier = 1f  
    
    private var burstThreshold = 0.1f  
    private var glowThreshold = 0.05f  
    
    private var forceFields = mutableListOf<ForceField>()
    private var mainForceFieldStrength = 0f
    
    private var audioFeatures = AudioFeatures()
    
    override fun onProcessData(processedData: FloatArray): FloatArray {
        val smoothingFactor = SpectrumParameters.smoothingFactor.value
        val responseMultiplier = (1.0f / smoothingFactor).coerceIn(1.8f, 8.0f)
        
        audioFeatures = audioAnalyzer.analyzeAudio(
            processedData, 
            currentVolume,
            previousVolume,
            volumeChange,
            dominantFreqIndex,
            rhythmIntensity
        )
        
        val hasSoundInput = audioFeatures.totalEnergy > 0.01f || currentVolume > 0.01f
        
        if (hasSoundInput) {
            updateParticleGenerationRate(responseMultiplier)
            
            updateForceFields(deltaTime, responseMultiplier)
            
            if (isRhythmPeak || volumeChange > burstThreshold * responseMultiplier) {
                triggerRhythmEffects(responseMultiplier)
            }
        } else {
            particleRateMultiplier = 0f
            
            for (field in forceFields) {
                if (field.type != ForceFieldType.UPWARD) {
                    field.strength *= (1f - deltaTime * 2f)
                } else {
                    field.strength = Math.max(0.1f, field.strength * (1f - deltaTime))
                }
            }
        }
        
        particleSystem.update(deltaTime, forceFields, audioFeatures)
        effectSystem.update(deltaTime, audioFeatures)
        
        return processedData
    }
    
    override fun onRender(canvas: Canvas, data: FloatArray, width: Int, height: Int, isVertical: Boolean) {
        if (!isInitialized || screenWidth != width || screenHeight != height || isVerticalMode != isVertical) {
            initialize(width, height, isVertical)
        }
        
        particleSystem.render(canvas, colorManager)
        
        effectSystem.render(canvas, colorManager)
        
        drawBars(canvas, data, width, height, isVertical)
    }
    
    private fun initialize(width: Int, height: Int, isVertical: Boolean) {
        screenWidth = width
        screenHeight = height
        isVerticalMode = isVertical
        
        particleSystem.initialize(width, height, isVertical)
        
        effectSystem.initialize(width, height, isVertical)
        
        colorManager.initialize()
        
        physicsSystem.initialize(width, height)
        
        createBaseForceFields(isVertical)
        
        isInitialized = true
    }
    
    private fun createBaseForceFields(isVertical: Boolean) {
        forceFields.clear()
        
        if (isVertical) {
            
            forceFields.add(ForceField(
                x = 0.5f,
                y = 0.5f,
                radius = 2.0f,
                strength = 0.5f,
                type = ForceFieldType.UPWARD,  
                direction = 0f  
            ))
            
            val spiralCount = 3
            for (i in 0 until spiralCount) {
                val x = 0.3f + Random.nextFloat() * 0.4f
                val y = 0.2f + (i * 0.3f)  
                
                forceFields.add(ForceField(
                    x = x,
                    y = y,
                    radius = 0.3f + Random.nextFloat() * 0.2f,
                    strength = 0.2f + Random.nextFloat() * 0.2f,
                    type = ForceFieldType.SPIRAL,
                    direction = Random.nextFloat() * (2 * Math.PI.toFloat())
                ))
            }
        } else {
            
            forceFields.add(ForceField(
                x = 0.5f,
                y = 0.5f,
                radius = 2.0f,
                strength = 0.5f,
                type = ForceFieldType.UPWARD,
                direction = Math.PI.toFloat() * 1.5f  
            ))
            
            val spiralCount = 3
            for (i in 0 until spiralCount) {
                val x = 0.2f + (i * 0.3f)
                val y = 0.3f + Random.nextFloat() * 0.4f
                
                forceFields.add(ForceField(
                    x = x,
                    y = y,
                    radius = 0.3f + Random.nextFloat() * 0.2f,
                    strength = 0.2f + Random.nextFloat() * 0.2f,
                    type = ForceFieldType.SPIRAL,
                    direction = Random.nextFloat() * (2 * Math.PI.toFloat())
                ))
            }
        }
    }
    
    private fun updateParticleGenerationRate(responseMultiplier: Float) {
        val energyFactor = audioFeatures.totalEnergy * 1.8f
        val highFreqFactor = audioFeatures.highFreqEnergy * 2.7f
        
        particleRateMultiplier = (energyFactor + highFreqFactor) * responseMultiplier
        
        if (audioFeatures.totalEnergy > 0.02f) {
            val particlesToGenerate = (baseParticleRate * particleRateMultiplier * deltaTime).toInt()
            if (particlesToGenerate > 0) {
                particleSystem.generateParticles(particlesToGenerate, audioFeatures)
            }
        }
    }
    
    private fun updateForceFields(deltaTime: Float, responseMultiplier: Float) {
        val targetStrength = 0.5f + audioFeatures.totalEnergy * 0.45f * responseMultiplier
        mainForceFieldStrength += (targetStrength - mainForceFieldStrength) * deltaTime * 2.7f
        
        for (field in forceFields) {
            when (field.type) {
                ForceFieldType.UPWARD -> {
                    field.strength = mainForceFieldStrength
                }
                ForceFieldType.SPIRAL -> {
                    field.strength = mainForceFieldStrength * 0.6f
                    if (isRhythmPeak) {
                        field.direction = (field.direction + deltaTime * 10f) % (2 * Math.PI.toFloat())
                    }
                }
                ForceFieldType.PULSE -> {
                    field.strength *= (1f - deltaTime * 2f)
                }
                ForceFieldType.ATTRACTOR -> {
                    field.strength *= (1f - deltaTime * 1.5f)
                }
            }
        }
        
        forceFields.removeAll { field -> 
            (field.type == ForceFieldType.PULSE || field.type == ForceFieldType.ATTRACTOR) && 
            field.strength < 0.05f 
        }
        
        physicsSystem.updateForceFields(forceFields, deltaTime, audioFeatures,isVerticalMode)
    }
    
    private fun triggerRhythmEffects(responseMultiplier: Float) {
        if (audioFeatures.totalEnergy <= 0.03f) return
        
        val randomX: Float
        val randomY: Float
        
        if (isVerticalMode) {
            randomX = 0.5f + Random.nextFloat() * 0.4f  
            randomY = 0.3f + Random.nextFloat() * 0.4f  
        } else {
            randomX = 0.3f + Random.nextFloat() * 0.4f  
            randomY = 0.3f + Random.nextFloat() * 0.4f  
        }
        
        when (rhythmIntensity) {
            3 -> { 
                createPulseForceField(randomX, randomY, 0.8f * responseMultiplier)
                
                effectSystem.triggerEffect(
                    EffectType.WAVE,
                    randomX,
                    randomY,
                    0.8f * responseMultiplier
                )
                
                val extraX: Float
                val extraY: Float
                
                if (isVerticalMode) {
                    extraX = 0.4f + Random.nextFloat() * 0.5f  
                    extraY = 0.2f + Random.nextFloat() * 0.6f  
                } else {
                    extraX = 0.2f + Random.nextFloat() * 0.6f  
                    extraY = 0.2f + Random.nextFloat() * 0.6f  
                }
                
                effectSystem.triggerEffect(
                    EffectType.WAVE,
                    extraX,
                    extraY,
                    0.7f * responseMultiplier
                )
            }
            2 -> { 
                createPulseForceField(randomX, randomY, 0.5f * responseMultiplier)
                
                effectSystem.triggerEffect(
                    EffectType.WAVE,
                    randomX,
                    randomY,
                    0.5f * responseMultiplier
                )
                
                val waveX = 0.2f + Random.nextFloat() * 0.6f
                val waveY = 0.2f + Random.nextFloat() * 0.6f
                effectSystem.triggerEffect(
                    EffectType.WAVE,
                    waveX,
                    waveY,
                    0.4f * responseMultiplier
                )
            }
            1 -> { 
                createPulseForceField(randomX, randomY, 0.3f * responseMultiplier)
                
                effectSystem.triggerEffect(
                    EffectType.WAVE,
                    randomX,
                    randomY,
                    0.3f * responseMultiplier
                )
            }
        }
        
        if (audioFeatures.hasVocals && audioFeatures.midFreqEnergy > 0.1f) {
            particleSystem.generateVocalParticles(5 + (5 * responseMultiplier).toInt())
            
            effectSystem.triggerEffect(
                EffectType.PULSE,
                0.3f + Random.nextFloat() * 0.4f,
                0.3f + Random.nextFloat() * 0.4f,
                0.4f * responseMultiplier
            )
        }
        
        if (audioFeatures.totalEnergy > 0.05f && Random.nextFloat() < 0.3f) {
            val smallWaveX = 0.1f + Random.nextFloat() * 0.8f
            val smallWaveY = 0.1f + Random.nextFloat() * 0.8f
            effectSystem.triggerEffect(
                EffectType.WAVE,
                smallWaveX,
                smallWaveY,
                0.2f * responseMultiplier
            )
        }
    }
    
    private fun createPulseForceField(x: Float, y: Float, strength: Float) {
        if (audioFeatures.totalEnergy <= 0.02f) return
        
        forceFields.add(ForceField(
            x = x,
            y = y,
            radius = 0.5f + Random.nextFloat() * 0.5f,
            strength = strength,
            type = ForceFieldType.PULSE,
            direction = if (isVerticalMode) 0f else Math.PI.toFloat() * 1.5f
        ))
    }
    
    private fun drawBars(canvas: Canvas, data: FloatArray, width: Int, height: Int, isVertical: Boolean) {
        val barPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        val barCount = data.size
        val barWidth = if (isVertical) height.toFloat() / barCount else width.toFloat() / barCount
        val maxBarSize = if (isVertical) width * 0.9f else height * 0.9f
        val spacing = barWidth * 0.2f
        
        val barColors = colorManager.getBarGradientColors(audioFeatures.totalEnergy)
        
        for (i in 0 until barCount) {
            val value = data[i].coerceIn(0f, 1f)
            val barSize = value * maxBarSize
            
            val shader = if (isVertical) {
                LinearGradient(
                    0f, 0f, barSize, 0f,
                    barColors,
                    null,
                    Shader.TileMode.CLAMP
                )
            } else {
                LinearGradient(
                    0f, height.toFloat(), 0f, height - barSize,
                    barColors,
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            
            barPaint.shader = shader
            
            if (isVertical) {
                val left = 0f
                val top = i * barWidth + spacing / 2
                val right = barSize
                val bottom = (i + 1) * barWidth - spacing / 2
                
                canvas.drawRect(left, top, right, bottom, barPaint)
            } else {
                val left = i * barWidth + spacing / 2
                val top = height - barSize
                val right = (i + 1) * barWidth - spacing / 2
                val bottom = height.toFloat()
                
                canvas.drawRect(left, top, right, bottom, barPaint)
            }
        }
        
        barPaint.shader = null
    }
    
    enum class ForceFieldType {
        UPWARD,    
        SPIRAL,    
        PULSE,     
        ATTRACTOR  
    }
    
    data class ForceField(
        val x: Float,
        val y: Float,
        val radius: Float,
        var strength: Float,
        val type: ForceFieldType,
        var direction: Float = 0f
    )
    
    enum class EffectType {
        BURST,  
        WAVE,   
        GLOW,   
        PULSE   
    }
    
    data class AudioFeatures(
        val totalEnergy: Float = 0f,
        val lowFreqEnergy: Float = 0f,
        val midFreqEnergy: Float = 0f,
        val highFreqEnergy: Float = 0f,
        val rhythmIntensity: Int = 0,
        val volumeChange: Float = 0f,
        val hasVocals: Boolean = false
    )
}
