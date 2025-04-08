package com.example.musicvibrationapp.view.spectrum.renderers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.musicvibrationapp.view.SpectrumParameters
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class AngerExplosiveRenderer(context: Context) : BaseEmotionRenderer(context) {
    
    private val explosionEffects = AngerExplosionEffects()
    
    private val forceFieldSystem = AngerForceFieldSystem()
    
    private val colorScheme = AngerColorScheme()
    
    private var lastWidth = 0
    private var lastHeight = 0
    private var isInitialized = false
    
    private var shakeOffsetX = 0f
    private var shakeOffsetY = 0f
    private var shakeIntensity = 0f
    private var shakeDecay = 5.0f
    
    private var rhythmLevel = 0 
    private var lastRhythmTime = 0L
    private var rhythmCooldown = 200L 
    
    private var energyAccumulator = 0f
    private var maxEnergyCapacity = 1.0f
    private var energyDecayRate = 0.2f
    
    private var responseMultiplier = 1.0f
    
    override fun onProcessData(processedData: FloatArray): FloatArray {
        val smoothingFactor = SpectrumParameters.smoothingFactor.value
        responseMultiplier = (1.0f / smoothingFactor).pow(2.0f)
        
        updateRhythmDetection()
        
        updateEnergyAccumulator(deltaTime * 1.5f)
        
        explosionEffects.update(deltaTime)
        
        forceFieldSystem.update(
            deltaTime,
            rhythmLevel, 
            totalEnergy * 1.3f,
            responseMultiplier * 1.3f
        )
        
        colorScheme.update(
            deltaTime, 
            totalEnergy * 1.3f,
            dominantFreqIndex, 
            rhythmLevel, 
            responseMultiplier * 1.2f
        )
        
        updateShakeEffect(deltaTime * 1.5f)
        
        return processedData
    }
    
    override fun onRender(canvas: Canvas, data: FloatArray, width: Int, height: Int, isVertical: Boolean) {
        if (!isInitialized || forceFieldSystem.screenWidth != width || forceFieldSystem.screenHeight != height) {
            forceFieldSystem.initialize(width, height, isVertical)
            explosionEffects.initialize(width, height, isVertical)
            lastWidth = width
            lastHeight = height
            isInitialized = true
        }
        
        canvas.save()
        canvas.translate(shakeOffsetX, shakeOffsetY)
        

        drawBars(canvas, data, width, height, isVertical)
        
        forceFieldSystem.draw(canvas, colorScheme)
        
        explosionEffects.draw(canvas, colorScheme)
        
        drawEnergyIndicator(canvas, width, height, isVertical)
        
        canvas.restore()
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
        
        val barColors = colorScheme.getBarColors()
        
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
    
    private fun drawEnergyIndicator(canvas: Canvas, width: Int, height: Int, isVertical: Boolean) {
        if (energyAccumulator < 0.1f) return
        
        val indicatorPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = colorScheme.getEnergyIndicatorColor(energyAccumulator / maxEnergyCapacity)
        }
        
        val fillPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.argb(
                (100 * energyAccumulator / maxEnergyCapacity).toInt(),
                255, 50, 0
            )
        }
        
        val size = min(width, height) * 0.15f
        val centerX = if (isVertical) width * 0.85f else width * 0.9f
        val centerY = if (isVertical) height * 0.9f else height * 0.15f
        
        canvas.drawCircle(centerX, centerY, size * (energyAccumulator / maxEnergyCapacity), fillPaint)
        
        canvas.drawCircle(centerX, centerY, size, indicatorPaint)
        
        if (energyAccumulator / maxEnergyCapacity > 0.8f) {
            val pulseSize = size * (1.0f + 0.1f * sin(System.currentTimeMillis() / 100.0).toFloat())
            indicatorPaint.alpha = (100 + 155 * sin(System.currentTimeMillis() / 150.0).toFloat()).toInt().coerceIn(0, 255)
            canvas.drawCircle(centerX, centerY, pulseSize, indicatorPaint)
        }
    }
    
    private fun updateRhythmDetection() {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastRhythmTime < rhythmCooldown * 0.6f) {
            return
        }
        
        rhythmLevel = when {
            volumeChange > 0.05f && totalEnergy > 0.25f -> {
                triggerShake(0.25f)
                triggerShake(0.25f)
                energyAccumulator += 0.5f
                lastRhythmTime = currentTime
                3
            }
            volumeChange > 0.03f && totalEnergy > 0.15f -> {
                triggerShake(0.15f)
                energyAccumulator += 0.3f
                lastRhythmTime = currentTime
                2
            }
            volumeChange > 0.015f && totalEnergy > 0.1f -> {
                triggerShake(0.08f)
                energyAccumulator += 0.15f
                lastRhythmTime = currentTime
                1
            }
            else -> 0
        }
        
        maxEnergyCapacity = 1.0f
        if (energyAccumulator >= maxEnergyCapacity) {
            triggerSuperExplosion()
            energyAccumulator = 0f
        }
    }
    
    private fun triggerSuperExplosion() {
        for (i in 0 until 3) {
            val x = 0.2f + Random.nextFloat() * 0.6f
            val y = 0.2f + Random.nextFloat() * 0.6f
            val size = 0.2f + Random.nextFloat() * 0.15f
            
            explosionEffects.generateFireMist(x, y, size * 1.5f, 3.0f)
            
            explosionEffects.generateExplosion(x, y, size, 3)
        }
        
        triggerShake(0.2f)
        
        for (i in 0 until 3) {
            val x = 0.3f + Random.nextFloat() * 0.4f
            val y = 0.3f + Random.nextFloat() * 0.4f
            val size = 0.15f + Random.nextFloat() * 0.1f
            
            explosionEffects.generateHeatDistortionOnly(x, y, size * 2.0f, 3)
        }
    }
    
    private fun triggerShake(intensity: Float) {
        shakeIntensity = intensity * responseMultiplier
        
        val angle = Random.nextFloat() * Math.PI * 2
        val distance = shakeIntensity * min(lastWidth, lastHeight) * 0.05f
        
        shakeOffsetX = (cos(angle) * distance).toFloat()
        shakeOffsetY = (sin(angle) * distance).toFloat()
    }
    
    private fun updateShakeEffect(deltaTime: Float) {
        if (shakeIntensity > 0.001f) {
            shakeIntensity *= (1.0f - shakeDecay * deltaTime)
            
            val angle = Random.nextFloat() * Math.PI * 2
            val distance = shakeIntensity * min(lastWidth, lastHeight) * 0.05f
            
            shakeOffsetX = (cos(angle) * distance).toFloat()
            shakeOffsetY = (sin(angle) * distance).toFloat()
        } else {
            shakeIntensity = 0f
            shakeOffsetX = 0f
            shakeOffsetY = 0f
        }
    }
    
    private fun updateEnergyAccumulator(deltaTime: Float) {
        energyAccumulator = max(0f, energyAccumulator - energyDecayRate * 0.7f * deltaTime)
    }
    
    inner class AngerForceFieldSystem {
        val forceFields = mutableListOf<ForceField>()
        
        private var globalForceX = 0f
        private var globalForceY = 0f
        private var globalForceTarget = 0f
        private var globalForceAngle = 0f
        
        var screenWidth = 0
        var screenHeight = 0
        var isVerticalMode = false
        
        fun initialize(width: Int, height: Int, isVertical: Boolean) {
            screenWidth = width
            screenHeight = height
            isVerticalMode = isVertical
        }
        
        fun update(deltaTime: Float, rhythmLevel: Int, energy: Float, responseMultiplier: Float) {
            updateForceFields(deltaTime)
            
            updateGlobalForce(deltaTime, rhythmLevel, energy, responseMultiplier)
            
            if (rhythmLevel > 0) {
                val effectChance = when (rhythmLevel) {
                    3 -> 0.9f
                    2 -> 0.7f
                    else -> 0.5f
                }
                
                val adjustedChance = (effectChance * responseMultiplier).coerceAtMost(1.0f)
                
                if (Random.nextFloat() < adjustedChance) {
                    val x = 0.1f + Random.nextFloat() * 0.8f
                    val y = 0.1f + Random.nextFloat() * 0.8f
                    val size = when (rhythmLevel) {
                        3 -> 0.15f + Random.nextFloat() * 0.1f
                        2 -> 0.12f + Random.nextFloat() * 0.08f
                        else -> 0.1f + Random.nextFloat() * 0.05f
                    }
                    
                    if (Random.nextFloat() < 0.6f) {
                        explosionEffects.generateFireMist(x, y, size, rhythmLevel.toFloat())
                    } else {
                        explosionEffects.generateHeatDistortionOnly(x, y, size * 1.5f, rhythmLevel)
                    }
                }
            }
            else if (energy > 0.15f && Random.nextFloat() < 0.3f * responseMultiplier) {
                val x = 0.1f + Random.nextFloat() * 0.8f
                val y = 0.1f + Random.nextFloat() * 0.8f
                
                explosionEffects.generateHeatDistortionOnly(x, y, 0.08f, 1)
            }
        }
        
        fun generateSuperForceField() {
            explosionEffects.generateHeatDistortionOnly(0.5f, 0.5f, 0.3f, 3)
            
            repeat(4) {
                val angle = Random.nextFloat() * Math.PI * 2
                val distance = 0.15f + Random.nextFloat() * 0.1f
                
                val x = 0.5f + cos(angle).toFloat() * distance
                val y = 0.5f + sin(angle).toFloat() * distance
                
                explosionEffects.generateFireMist(
                    x.coerceIn(0.1f, 0.9f),
                    y.coerceIn(0.1f, 0.9f),
                    0.15f + Random.nextFloat() * 0.1f,
                    2.5f + Random.nextFloat() * 0.5f
                )
            }
            
            repeat(2) {
                val angle = Random.nextFloat() * Math.PI * 2
                val distance = 0.2f + Random.nextFloat() * 0.15f
                
                val x = 0.5f + cos(angle).toFloat() * distance
                val y = 0.5f + sin(angle).toFloat() * distance
                
                explosionEffects.generateExplosion(
                    x.coerceIn(0.1f, 0.9f),
                    y.coerceIn(0.1f, 0.9f),
                    0.12f,
                    2
                )
            }
        }
        
        private fun updateForceFields(deltaTime: Float) {
            forceFields.removeAll { field ->
                field.duration -= deltaTime
                
                val progress = 1.0f - (field.duration / field.maxDuration)
                field.currentStrength = if (progress < 0.2f) {
                    field.strength * (progress / 0.2f)
                } else {
                    field.strength * (1.0f - (progress - 0.2f) / 0.8f)
                }
                
                field.duration <= 0
            }
        }
        
        private fun updateGlobalForce(deltaTime: Float, rhythmLevel: Int, energy: Float, responseMultiplier: Float) {
            globalForceTarget = when (rhythmLevel) {
                3 -> 3.0f * responseMultiplier
                2 -> 2.0f * responseMultiplier
                1 -> 1.5f * responseMultiplier
                else -> 0.8f * responseMultiplier
            }
            
            val transitionSpeed = 3.0f * deltaTime * responseMultiplier
            val currentForce = sqrt(globalForceX * globalForceX + globalForceY * globalForceY)
            
            if (currentForce < globalForceTarget) {
                val increaseFactor = min(transitionSpeed, globalForceTarget - currentForce)
                if (currentForce > 0) {
                    globalForceX = globalForceX * (1 + increaseFactor / currentForce)
                    globalForceY = globalForceY * (1 + increaseFactor / currentForce)
                } else {
                    globalForceAngle = Random.nextFloat() * Math.PI.toFloat() * 2
                    globalForceX = globalForceTarget * cos(globalForceAngle)
                    globalForceY = globalForceTarget * sin(globalForceAngle)
                }
            } else if (currentForce > globalForceTarget) {
                val decreaseFactor = min(transitionSpeed, currentForce - globalForceTarget)
                globalForceX = globalForceX * (1 - decreaseFactor / currentForce)
                globalForceY = globalForceY * (1 - decreaseFactor / currentForce)
            }
            
            val angleChangeSpeed = 1.0f * deltaTime * (1 + energy * 2)
            globalForceAngle += angleChangeSpeed
            if (globalForceAngle >= Math.PI.toFloat() * 2) {
                globalForceAngle -= Math.PI.toFloat() * 2
            }
            
            val directionChangeStrength = 0.3f * deltaTime * responseMultiplier
            val targetX = globalForceTarget * cos(globalForceAngle)
            val targetY = globalForceTarget * sin(globalForceAngle)
            
            globalForceX = globalForceX * (1 - directionChangeStrength) + targetX * directionChangeStrength
            globalForceY = globalForceY * (1 - directionChangeStrength) + targetY * directionChangeStrength
        }
        
        fun getForceAt(x: Float, y: Float): Pair<Float, Float> {
            var totalForceX = globalForceX
            var totalForceY = globalForceY
            
            forceFields.forEach { field ->
                val dx = x - field.x
                val dy = y - field.y
                val distance = sqrt(dx * dx + dy * dy)
                
                if (distance < field.radius) {
                    val falloff = 1.0f - (distance / field.radius)
                    
                    when (field.type) {
                        ForceFieldType.RADIAL -> {
                            val forceMagnitude = field.currentStrength * falloff
                            val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
                            
                            totalForceX += cos(angle) * forceMagnitude
                            totalForceY += sin(angle) * forceMagnitude
                        }
                        ForceFieldType.VORTEX -> {
                            val forceMagnitude = field.currentStrength * falloff
                            val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat() + Math.PI.toFloat() / 2
                            
                            totalForceX += cos(angle) * forceMagnitude
                            totalForceY += sin(angle) * forceMagnitude
                        }
                    }
                }
            }
            
            return Pair(totalForceX, totalForceY)
        }
        
        fun draw(canvas: Canvas, colorScheme: AngerColorScheme) {
        }
    }
    
    inner class AngerColorScheme {
        private val baseRedColor = Color.rgb(255, 0, 0)
        private val baseOrangeColor = Color.rgb(255, 100, 0)
        private val baseYellowColor = Color.rgb(255, 200, 0)
        
        private var colorIntensity = 0.5f
        private var colorVariation = 0.0f
        private var colorPulse = 0.0f
        
        fun update(deltaTime: Float, energy: Float, dominantFreq: Int, rhythmLevel: Int, responseMultiplier: Float) {
            colorIntensity = colorIntensity * 0.9f + energy * 0.1f
            
            val targetVariation = dominantFreq / 128f
            colorVariation = colorVariation * 0.95f + targetVariation * 0.05f
            
            val pulseSpeed = 1.0f + rhythmLevel * 2.0f * responseMultiplier
            colorPulse = (colorPulse + deltaTime * pulseSpeed) % 1.0f
        }
        
        fun getBarColors(): IntArray {
            val startColor = adjustColor(baseRedColor, colorIntensity, -0.2f)
            val endColor = adjustColor(baseOrangeColor, colorIntensity, 0.2f)
            
            return intArrayOf(startColor, endColor)
        }
        
        fun getExplosionColors(intensityLevel: Int): IntArray {
            val coreIntensity = 0.7f + intensityLevel * 0.1f
            val coreColor = when (intensityLevel) {
                4 -> adjustColor(baseYellowColor, coreIntensity, 0.3f)
                3 -> adjustColor(baseYellowColor, coreIntensity, 0.2f)
                2 -> adjustColor(baseOrangeColor, coreIntensity, 0.1f)
                else -> adjustColor(baseRedColor, coreIntensity, 0.0f)
            }
            
            val midColor = when (intensityLevel) {
                4, 3 -> adjustColor(baseOrangeColor, coreIntensity * 0.9f, 0.1f)
                else -> adjustColor(baseRedColor, coreIntensity * 0.9f, 0.0f)
            }
            
            val outerColor = Color.argb(0, Color.red(baseRedColor), Color.green(baseRedColor), Color.blue(baseRedColor))
            
            return intArrayOf(coreColor, midColor, outerColor)
        }
        
        fun getFlameColors(variation: Float): IntArray {
            val adjustedVariation = variation * (1.0f + colorVariation)
            
            val coreColor = when {
                adjustedVariation > 0.7f -> adjustColor(baseYellowColor, colorIntensity, 0.2f)
                adjustedVariation > 0.3f -> adjustColor(baseOrangeColor, colorIntensity, 0.1f)
                else -> adjustColor(baseRedColor, colorIntensity, 0.0f)
            }
            
            val midColor = when {
                adjustedVariation > 0.5f -> adjustColor(baseOrangeColor, colorIntensity * 0.9f, 0.0f)
                else -> adjustColor(baseRedColor, colorIntensity * 0.9f, -0.1f)
            }
            
            val outerColor = Color.argb(0, Color.red(baseRedColor), Color.green(baseRedColor), Color.blue(baseRedColor))
            
            return intArrayOf(coreColor, midColor, outerColor)
        }
        
        fun getSparkColor(variation: Float): Int {
            val adjustedVariation = variation * (1.0f + colorVariation)
            
            return when {
                adjustedVariation > 0.7f -> adjustColor(baseYellowColor, colorIntensity, 0.3f)
                adjustedVariation > 0.3f -> adjustColor(baseOrangeColor, colorIntensity, 0.2f)
                else -> adjustColor(baseRedColor, colorIntensity, 0.1f)
            }
        }
        
        fun getSmokeColors(opacity: Float): IntArray {
            val smokeColor = Color.argb(
                (opacity * 100).toInt(),
                80, 0, 0
            )
            
            val transparentColor = Color.argb(0, 0, 0, 0)
            
            return intArrayOf(smokeColor, transparentColor)
        }
        
        fun getForceFieldColor(strength: Float, type: Int): Int {
            return when (type) {
                ForceFieldType.RADIAL.ordinal -> {
                    val pulse = (sin(colorPulse * Math.PI.toFloat() * 2) * 0.5f + 0.5f) * strength
                    adjustColor(baseRedColor, colorIntensity, pulse)
                }
                else -> {
                    val pulse = (sin(colorPulse * Math.PI.toFloat() * 2) * 0.5f + 0.5f) * strength
                    adjustColor(baseOrangeColor, colorIntensity, pulse)
                }
            }
        }
        
        fun getEnergyIndicatorColor(energyRatio: Float): Int {
            val pulse = (sin(colorPulse * Math.PI.toFloat() * 4) * 0.3f + 0.7f)
            
            return when {
                energyRatio > 0.8f -> adjustColor(baseYellowColor, pulse, 0.2f)
                energyRatio > 0.5f -> adjustColor(baseOrangeColor, pulse, 0.1f)
                else -> adjustColor(baseRedColor, pulse, 0.0f)
            }
        }
        
        private fun adjustColor(baseColor: Int, intensity: Float, variation: Float): Int {
            val r = Color.red(baseColor)
            val g = Color.green(baseColor)
            val b = Color.blue(baseColor)
            
            val intensityFactor = 0.7f + intensity * 0.6f
            val variationFactor = 1.0f + variation
            
            val newR = (r * intensityFactor).toInt().coerceIn(0, 255)
            val newG = (g * intensityFactor * variationFactor).toInt().coerceIn(0, 255)
            val newB = (b * intensityFactor * (1.0f - abs(variation) * 0.5f)).toInt().coerceIn(0, 255)
            
            return Color.rgb(newR, newG, newB)
        }
    }
    
    enum class ForceFieldType {
        RADIAL,  
        VORTEX   
    }
    
    data class ForceField(
        val x: Float,
        val y: Float,
        val radius: Float,
        val strength: Float,
        var duration: Float,
        val maxDuration: Float,
        val type: ForceFieldType,
        var currentStrength: Float = 0f
    )
    
    private fun atan2(y: Double, x: Double): Float {
        return Math.atan2(y, x).toFloat()
    }
    
    private fun abs(value: Float): Float {
        return if (value < 0) -value else value
    }
}