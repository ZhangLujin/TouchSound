package com.example.musicvibrationapp.view.spectrum.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Color
import kotlin.random.Random

class AnticipationEffectSystem {
    
    private val effects = mutableListOf<Effect>()
    
    private var screenWidth = 0
    private var screenHeight = 0
    private var isVerticalMode = false
    
    private val effectPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    fun initialize(width: Int, height: Int, isVertical: Boolean) {
        screenWidth = width
        screenHeight = height
        isVerticalMode = isVertical
    }
    
    fun triggerEffect(type: AnticipationParticleRenderer.EffectType, x: Float, y: Float, intensity: Float) {
        val effect = when (type) {
            AnticipationParticleRenderer.EffectType.BURST -> createBurstEffect(x, y, intensity)
            AnticipationParticleRenderer.EffectType.WAVE -> createWaveEffect(x, y, intensity)
            AnticipationParticleRenderer.EffectType.GLOW -> createGlowEffect(x, y, intensity)
            AnticipationParticleRenderer.EffectType.PULSE -> createPulseEffect(x, y, intensity)
        }
        
        effects.add(effect)
    }
    
    fun update(deltaTime: Float, audioFeatures: AnticipationParticleRenderer.AudioFeatures) {
        val expiredEffects = mutableListOf<Effect>()
        
        for (effect in effects) {
            effect.duration -= deltaTime
            
            if (effect.duration <= 0) {
                expiredEffects.add(effect)
                continue
            }
            
            effect.progress = 1.0f - (effect.duration / effect.maxDuration)
            
            updateEffectProperties(effect, deltaTime, audioFeatures)
        }
        
        effects.removeAll(expiredEffects)
    }
    
    fun render(canvas: Canvas, colorManager: AnticipationColorManager) {
        for (effect in effects) {
            renderEffect(canvas, effect, colorManager)
        }
    }
    
    private fun createBurstEffect(x: Float, y: Float, intensity: Float): Effect {
        val size = 0.2f + intensity * 0.3f
        val duration = 0.8f + intensity * 0.4f
        
        return Effect(
            x = x,
            y = y,
            size = size,
            intensity = intensity,
            duration = duration,
            maxDuration = duration,
            type = AnticipationParticleRenderer.EffectType.BURST
        )
    }
    
    private fun createWaveEffect(x: Float, y: Float, intensity: Float): Effect {
        val size = 0.1f + intensity * 0.2f
        val duration = 1.0f + intensity * 0.5f
        
        return Effect(
            x = x,
            y = y,
            size = size,
            intensity = intensity,
            duration = duration,
            maxDuration = duration,
            type = AnticipationParticleRenderer.EffectType.WAVE
        )
    }
    
    private fun createGlowEffect(x: Float, y: Float, intensity: Float): Effect {
        val size = 0.12f + intensity * 0.2f
        val duration = 1.0f + intensity * 0.5f
        
        return Effect(
            x = x,
            y = y,
            size = size,
            intensity = intensity,
            duration = duration,
            maxDuration = duration,
            type = AnticipationParticleRenderer.EffectType.GLOW
        )
    }
    
    private fun createPulseEffect(x: Float, y: Float, intensity: Float): Effect {
        val size = 0.1f + intensity * 0.15f
        val duration = 0.6f + intensity * 0.3f
        
        return Effect(
            x = x,
            y = y,
            size = size,
            intensity = intensity,
            duration = duration,
            maxDuration = duration,
            type = AnticipationParticleRenderer.EffectType.PULSE
        )
    }
    
    private fun updateEffectProperties(effect: Effect, deltaTime: Float, audioFeatures: AnticipationParticleRenderer.AudioFeatures) {
        when (effect.type) {
            AnticipationParticleRenderer.EffectType.BURST -> {
                val growthRate = 1.0f + effect.intensity * 2.0f
                effect.size += deltaTime * growthRate
            }
            AnticipationParticleRenderer.EffectType.WAVE -> {
                val growthRate = 0.5f + effect.intensity * 1.5f
                effect.size += deltaTime * growthRate
            }
            AnticipationParticleRenderer.EffectType.GLOW -> {
                val pulseRate = 2.0f + effect.intensity * 3.0f
                val pulseFactor = 0.1f * Math.sin(effect.progress * pulseRate * Math.PI).toFloat()
                effect.size = effect.size * (1.0f + pulseFactor)
            }
            AnticipationParticleRenderer.EffectType.PULSE -> {
                val growthRate = 2.0f + effect.intensity * 3.0f
                effect.size += deltaTime * growthRate
            }
        }
        
        if (audioFeatures.totalEnergy > 0.5f) {
            effect.intensity = Math.min(1.0f, effect.intensity + deltaTime * audioFeatures.totalEnergy * 0.5f)
        }
    }
    
    private fun renderEffect(canvas: Canvas, effect: Effect, colorManager: AnticipationColorManager) {
        val x = effect.x * screenWidth
        val y = effect.y * screenHeight
        
        val color = colorManager.getEffectColor(effect.type, effect.progress, effect.intensity)
        
        when (effect.type) {
            AnticipationParticleRenderer.EffectType.BURST -> {
                renderBurstEffect(canvas, x, y, effect, color)
            }
            AnticipationParticleRenderer.EffectType.WAVE -> {
                renderWaveEffect(canvas, x, y, effect, color)
            }
            AnticipationParticleRenderer.EffectType.GLOW -> {
                renderGlowEffect(canvas, x, y, effect, color)
            }
            AnticipationParticleRenderer.EffectType.PULSE -> {
                renderPulseEffect(canvas, x, y, effect, color)
            }
        }
    }
    
    private fun renderBurstEffect(canvas: Canvas, x: Float, y: Float, effect: Effect, color: Int) {
        val radius = effect.size * Math.min(screenWidth, screenHeight) * 0.5f
        
        val gradient = RadialGradient(
            x, y,
            radius,
            intArrayOf(color, 0x00FFFFFF),
            floatArrayOf(0.0f, 1.0f),
            Shader.TileMode.CLAMP
        )
        
        effectPaint.shader = gradient
        
        canvas.drawCircle(x, y, radius, effectPaint)
        
        effectPaint.shader = null
    }
    
    private fun renderWaveEffect(canvas: Canvas, x: Float, y: Float, effect: Effect, color: Int) {
        val radius = effect.size * Math.min(screenWidth, screenHeight) * 0.5f
        
        val ringCount = 2
        
        for (i in 0 until ringCount) {
            val ringProgress = effect.progress * (1.0f + i * 0.5f)
            val ringRadius = radius * ringProgress.coerceIn(0f, 1f)
            val strokeWidth = radius * 0.15f * (1.0f - effect.progress * 0.5f)
            
            effectPaint.color = color
            effectPaint.style = Paint.Style.STROKE
            effectPaint.strokeWidth = strokeWidth
            
            val alpha = (255 * (1.0f - ringProgress.coerceIn(0f, 1f))).toInt().coerceIn(0, 255)
            effectPaint.alpha = alpha
            
            canvas.drawCircle(x, y, ringRadius, effectPaint)
        }
        
        effectPaint.style = Paint.Style.FILL
        effectPaint.alpha = 255
    }
    
    private fun renderGlowEffect(canvas: Canvas, x: Float, y: Float, effect: Effect, color: Int) {
        val radius = effect.size * Math.min(screenWidth, screenHeight) * 0.4f
        
        val midColor = Color.argb(
            100,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
        
        val transparentColor = Color.argb(0, 0, 0, 0)
        
        val gradient = RadialGradient(
            x, y,
            radius,
            intArrayOf(color, midColor, transparentColor),
            floatArrayOf(0.0f, 0.4f, 1.0f),
            Shader.TileMode.CLAMP
        )
        
        effectPaint.shader = gradient
        
        canvas.drawCircle(x, y, radius, effectPaint)
        
        effectPaint.shader = null
    }
    
    private fun renderPulseEffect(canvas: Canvas, x: Float, y: Float, effect: Effect, color: Int) {
        val radius = effect.size * Math.min(screenWidth, screenHeight) * 0.5f
        
        effectPaint.color = color
        effectPaint.style = Paint.Style.STROKE
        effectPaint.strokeWidth = radius * 0.1f * (1.0f - effect.progress * 0.8f)
        
        val ringCount = 3
        for (i in 0 until ringCount) {
            val ringProgress = (effect.progress + i * (1.0f / ringCount)) % 1.0f
            val ringRadius = radius * ringProgress
            
            val alpha = (255 * (1.0f - ringProgress)).toInt()
            effectPaint.alpha = alpha
            
            canvas.drawCircle(x, y, ringRadius, effectPaint)
        }
        
        effectPaint.style = Paint.Style.FILL
        effectPaint.alpha = 255
    }
    
    data class Effect(
        val x: Float,
        val y: Float,
        var size: Float,
        var intensity: Float,
        var duration: Float,
        val maxDuration: Float,
        val type: AnticipationParticleRenderer.EffectType,
        var progress: Float = 0f
    )
}