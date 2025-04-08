package com.example.musicvibrationapp.view.spectrum.renderers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.musicvibrationapp.view.EmotionColorManager
import com.example.musicvibrationapp.view.SpectrumParameters
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Flash-impulse renderer for surprise emotions
 * Expression of surprise using flash and pulse diffusion effects, with integrated SpectrumParameters parameter system.
 */
class SurpriseFlashRenderer(context: Context) : BaseEmotionRenderer(context) {
    // Pulse wave system
    private val pulseWaves = mutableListOf<PulseWave>()
    private val pulsePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    
    // lightning system
    private val lightningFlashes = mutableListOf<Lightning>()
    private val lightningPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    
    // sparkle effect
    private var flashOpacity = 0f
    private var flashDecayRate = 5.0f
    
    // visual echo system
    private val visualEchoes = mutableListOf<VisualEcho>()
    
    // vocal detection
    private var previousVocalEnergy = 0f
    
    // time distortion effect
    private var timeDistortionFactor = 1.0f
    
    // Background halo system
    private val backgroundGlows = mutableListOf<BackgroundGlow>()
    private val glowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    // Flashing dot system
    private val flashingDots = mutableListOf<FlashingDot>()
    private val dotPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    // Energy ray system
    private val energyRays = mutableListOf<EnergyRay>()
    private val rayPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    
    // ripple effect system
    private val rippleEffects = mutableListOf<RippleEffect>()
    private val ripplePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    
    // quantum collapse effect system
    private val quantumCollapseEffects = mutableListOf<QuantumCollapseEffect>()
    private val collapsePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    
    // spectrum response system
    private var spectrumResponseIntensity = 0f
    private val spectrumResponsePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    
    // particle system
    private val particleSystem = SurpriseParticleSystem()
    
    // visual echo system
    private val visualEchoSystem = SurpriseVisualEchoSystem()
    
    // music response controller
    private val musicResponseController = SurpriseMusicResponseController()
    
    /**
     * Process spectrum data
     */
    override fun onProcessData(processedData: FloatArray): FloatArray {
        // update existing pulse waves
        updatePulseWaves(deltaTime)
        
        // update lightning effect
        updateLightningFlashes(deltaTime)
        
        // update visual echo
        updateVisualEchoes(deltaTime)
        
        // update flash effect
        updateFlashEffect(deltaTime)
        
        // update background halo
        updateBackgroundGlows(deltaTime)
        
        // update flashing dots
        updateFlashingDots(deltaTime)
        
        // update energy rays
        updateEnergyRays(deltaTime)
        
        // update ripple effects
        updateRippleEffects(deltaTime)
        
        // update quantum collapse effects
        updateQuantumCollapseEffects(deltaTime)
        
        // update spectrum response intensity
        updateSpectrumResponseIntensity(deltaTime, processedData)
        
        // detect vocal range energy change
        val vocalRangeEnergy = calculateBandEnergy(processedData, processedData.size * 2 / 5, processedData.size * 4 / 5)
        val vocalChangeRate = abs(vocalRangeEnergy - previousVocalEnergy)
        previousVocalEnergy = vocalRangeEnergy
        
        // clear effects only when volume is very low
        if (currentVolume < 0.005f) {
            pulseWaves.clear()
            lightningFlashes.clear()
            visualEchoes.clear()
            backgroundGlows.clear()
            flashingDots.clear()
            flashOpacity = 0f
            return processedData
        }
        
        // limit maximum active effect count, prevent stuttering
        val maxActivePulses = 12
        val maxActiveLightnings = 6
        val maxActiveGlows = 5
        val maxActiveDots = 30
        
        // use more aggressive response curve - ensure visible effects at normal volume
        val volumeResponse = (currentVolume * 2.0f).coerceIn(0.3f, 1f)
        
        // ensure always have base background glows
        ensureBaseBackgroundGlows(maxActiveGlows, volumeResponse)
        
        // generate flashing dots
        generateFlashingDots(processedData, maxActiveDots, volumeResponse)
        
        // force generate base effects - ensure always have visual feedback
        if (pulseWaves.size < 2 && Random.nextFloat() < 0.4f) {
            val baseEffectIntensity = volumeResponse * 0.7f + 0.3f
            generatePulseWaveBurst(totalEnergy.coerceAtLeast(0.3f), 0.05f, 1, baseEffectIntensity)
        }
        
        // generate energy rays
        generateEnergyRays(processedData, volumeResponse)
        
        // generate ripple effects
        if (isRhythmPeak && rippleEffects.size < 5) {
            addRippleEffect(totalEnergy.coerceAtLeast(0.3f), volumeResponse)
        }
        
        // Significantly lower trigger thresholds and increase effect size
        if ((isRhythmPeak || (totalEnergy > 0.12f && volumeChange > 0.02f)) && pulseWaves.size < maxActivePulses) {
            // strong rhythm burst - larger and more concentrated effect
            val effectIntensity = volumeResponse * 0.6f + 0.4f
            generatePulseWaveBurst(totalEnergy.coerceAtLeast(0.3f), volumeChange.coerceAtLeast(0.05f), 3, effectIntensity)
            
            // strong rhythm flash - enhance flash effect
            triggerFlash(effectIntensity * 0.8f)
            
            // add time distortion effect
            timeDistortionFactor = 0.5f
            
            // add visual echo
            addVisualEcho(effectIntensity * 0.9f)
            
            // strong rhythm add large background halo
            addBackgroundGlow(effectIntensity * 0.9f, true)
        } 
        else if ((totalEnergy > 0.08f && volumeChange > 0.015f) && pulseWaves.size < maxActivePulses) {
            // medium rhythm burst
            val effectIntensity = volumeResponse * 0.5f + 0.3f
            generatePulseWaveBurst(totalEnergy.coerceAtLeast(0.2f), volumeChange.coerceAtLeast(0.03f), 2, effectIntensity)
            
            // medium rhythm flash
            triggerFlash(effectIntensity * 0.6f)
            
            // add mild time distortion
            timeDistortionFactor = 0.7f
            
            // add visual echo
            addVisualEcho(effectIntensity * 0.7f)
            
            // medium rhythm add medium background halo
            addBackgroundGlow(effectIntensity * 0.7f, false)
        }
        else if ((totalEnergy > 0.04f || volumeChange > 0.01f) && pulseWaves.size < maxActivePulses / 2) {
            // low rhythm burst
            val effectIntensity = volumeResponse * 0.4f + 0.3f
            generatePulseWaveBurst(totalEnergy.coerceAtLeast(0.1f), volumeChange.coerceAtLeast(0.02f), 1, effectIntensity)
            
            // low rhythm flash
            triggerFlash(effectIntensity * 0.4f)
            
            // add weak visual echo
            addVisualEcho(effectIntensity * 0.5f)
            
            // low rhythm add small background halo
            if (Random.nextFloat() < 0.5f) {
                addBackgroundGlow(effectIntensity * 0.5f, false)
            }
        }
        
        // vocal range mutation detection - trigger special vocal response effect
        if (vocalChangeRate > 0.03f && vocalRangeEnergy > 0.1f && pulseWaves.size < maxActivePulses) {
            // generate vocal response flash - warmer tone
            generateVocalFlash(vocalChangeRate.coerceAtLeast(0.05f), vocalRangeEnergy.coerceAtLeast(0.15f))
        }
        
        // time distortion factor smooth recovery
        timeDistortionFactor += (1.0f - timeDistortionFactor) * deltaTime * 2.0f
        
        // generate quantum collapse effects
        if ((isRhythmPeak || Random.nextFloat() < 0.05f * volumeResponse) && 
            quantumCollapseEffects.size < 3) {
            addQuantumCollapseEffect(volumeResponse)
        }
        
        // update particle system
        particleSystem.processData(
            processedData = processedData,
            deltaTime = deltaTime,
            currentVolume = currentVolume,
            isRhythmPeak = isRhythmPeak,
            totalEnergy = totalEnergy,
            energyConcentration = energyConcentration,
            rhythmIntensity = rhythmIntensity,
            musicForceFieldStrength = musicForceFieldStrength,
            forceFieldDirection = forceFieldDirection,
            fallSpeed = SpectrumParameters.fallSpeed.value,
            minFallSpeed = SpectrumParameters.minFallSpeed.value,
            melSensitivity = SpectrumParameters.melSensitivity.value,
            soloResponseStrength = SpectrumParameters.soloResponseStrength.value,
            minThreshold = SpectrumParameters.minThreshold.value
        )
        
        // update visual echo system
        visualEchoSystem.processData(deltaTime, currentVolume)
        
        // update music response controller
        musicResponseController.processAudioData(
            processedData = processedData,
            deltaTime = deltaTime,
            currentVolume = currentVolume,
            totalEnergy = totalEnergy,
            energyConcentration = energyConcentration
        )
        
        return processedData
    }
    
    /**
     * Render spectrum
     */
    override fun onRender(canvas: Canvas, data: FloatArray, width: Int, height: Int, isVertical: Boolean) {
        // check if volume is 0 or close to 0
        if (currentVolume < 0.005f) {
            // volume close to 0, do not draw anything and return
            return
        }
        
        val maxWidth = width.toFloat()
        val maxHeight = height.toFloat()
        
        // draw dark background
        drawDarkBackground(canvas, maxWidth, maxHeight)
        
        // draw spectrum response effect (on background layer)
        drawSpectrumResponse(canvas, data, maxWidth, maxHeight, isVertical)
        
        // draw background halo
        // drawBackgroundGlows(canvas, maxWidth, maxHeight, isVertical)
        
        // draw flashing dots
        drawFlashingDots(canvas, maxWidth, maxHeight, isVertical)
        
        // draw visual echo
        // drawVisualEchoes(canvas, maxWidth, maxHeight, isVertical)
        
        // draw ripple effects (before pulse waves)
        drawRippleEffects(canvas, maxWidth, maxHeight, isVertical)
        
        // draw pulse waves
        drawPulseWaves(canvas, maxWidth, maxHeight, isVertical)
        
        // draw energy rays (after pulse waves)
        drawEnergyRays(canvas, maxWidth, maxHeight, isVertical)
        
        // draw lightning effect
        drawLightningFlashes(canvas, maxWidth, maxHeight, isVertical)
        
        // draw quantum collapse effects (between pulse waves and energy rays)
        drawQuantumCollapseEffects(canvas, maxWidth, maxHeight, isVertical)
        
        // draw flash effect
        // drawFlashEffect(canvas, maxWidth, maxHeight, flashOpacity)
        
        // draw interaction enhancements
        drawInteractionEnhancements(canvas, maxWidth, maxHeight, isVertical)
        
        // draw particle system
        particleSystem.render(canvas, maxWidth, maxHeight, isVertical)
    }
    
    /**
     * draw dark background
     */
    private fun drawDarkBackground(canvas: Canvas, width: Float, height: Float) {
        // create dark gradient background - adjusted to deeper blue tone
        val backgroundPaint = Paint().apply {
            shader = android.graphics.LinearGradient(
                0f, 0f, 0f, height,
                intArrayOf(
                    Color.rgb(15, 15, 25),  // deeper blue black
                    Color.rgb(25, 25, 40),  // deep blue gray
                    Color.rgb(20, 20, 30)   // middle color
                ),
                floatArrayOf(0f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        
        // draw background
        canvas.drawRect(0f, 0f, width, height, backgroundPaint)
        
        // add subtle noise texture - increase quantity
        val noisePaint = Paint().apply {
            color = Color.argb(10, 255, 255, 255)  // very pale white
            style = Paint.Style.FILL
        }
        
        // draw some random noise - increase quantity
        val noiseCount = (width * height / 3000).toInt().coerceIn(40, 300)
        repeat(noiseCount) {
            val x = Random.nextFloat() * width
            val y = Random.nextFloat() * height
            val size = 1f + Random.nextFloat() * 2f
            canvas.drawCircle(x, y, size, noisePaint)
        }
    }
    
    /**
     * update pulse waves
     */
    private fun updatePulseWaves(deltaTime: Float) {
        // apply time distortion effect - affect the update speed of pulse waves
        val adjustedDeltaTime = deltaTime * timeDistortionFactor
        
        // update and remove expired pulse waves
        pulseWaves.removeAll { pulse ->
            // update lifespan
            pulse.lifespan -= adjustedDeltaTime
            
            // update radius - apply speed and acceleration
            pulse.speed += pulse.acceleration * adjustedDeltaTime
            pulse.radius += pulse.speed * adjustedDeltaTime
            
            // update intensity - non-linear decay with lifespan
            val lifeFraction = (pulse.lifespan / pulse.maxLifespan).coerceIn(0f, 1f)
            pulse.intensity = when {
                lifeFraction > 0.7f -> lifeFraction.pow(0.5f)  // begin slow decay
                lifeFraction > 0.3f -> lifeFraction.pow(1.2f)  // middle accelerate decay
                else -> lifeFraction.pow(2f)                   // end fast decay
            }
            
            // update thickness - decrease with radius increase
            val radiusRatio = (pulse.radius / pulse.maxRadius).coerceIn(0f, 1f)
            pulse.thickness = pulse.initialThickness * (1f - radiusRatio * 0.7f)
            
            // remove condition: lifespan ends or radius exceeds maximum
            pulse.lifespan <= 0 || pulse.radius >= pulse.maxRadius
        }
    }
    
    /**
     * update lightning effect
     */
    private fun updateLightningFlashes(deltaTime: Float) {
        // apply time distortion effect
        val adjustedDeltaTime = deltaTime * timeDistortionFactor
        
        // update and remove expired lightning
        lightningFlashes.removeAll { lightning ->
            // update lifespan
            lightning.lifespan -= adjustedDeltaTime * 4f  // lightning disappears faster
            
            // update intensity - lightning intensity fast decay
            val lifeFraction = (lightning.lifespan / lightning.maxLifespan).coerceIn(0f, 1f)
            lightning.intensity = lifeFraction.pow(0.7f)  // non-linear decay
            
            // remove condition: lifespan ends
            lightning.lifespan <= 0
        }
    }
    
    /**
     * update visual echo
     */
    private fun updateVisualEchoes(deltaTime: Float) {
        // update and remove expired visual echo
        visualEchoes.removeAll { echo ->
            // update lifespan
            echo.lifespan -= deltaTime * 3f  // echo disappears faster
            
            // update opacity
            echo.opacity = (echo.lifespan / echo.maxLifespan).pow(1.5f)
            
            // remove condition: lifespan ends
            echo.lifespan <= 0
        }
    }
    
    /**
     * update flash effect
     */
    private fun updateFlashEffect(deltaTime: Float) {
        // normal flash decay
        if (flashOpacity > 0) {
            flashOpacity -= flashDecayRate * deltaTime
            if (flashOpacity < 0) flashOpacity = 0f
        }
        
        // check if should generate new flash
        if (flashOpacity < 0.1f) {  // only consider new flash when current flash is weak
            val (shouldGenerate, effectIntensity) = musicResponseController.shouldGenerateEffect(
                SurpriseMusicResponseController.EffectType.FLASH_EFFECT,
                totalEnergy
            )
            
            if (shouldGenerate) {
                // set flash with adjusted intensity
                flashOpacity = 0.3f + effectIntensity * 0.7f
                flashDecayRate = 0.5f + effectIntensity * 1.5f
            }
        }
    }
    
    /**
     * trigger flash effect
     */
    private fun triggerFlash(intensity: Float) {
        // set flash opacity
        flashOpacity = intensity.coerceIn(0f, 1f)
        
        // adjust decay rate according to intensity
        flashDecayRate = 5.0f + intensity * 3.0f
    }
    
    /**
     * add visual echo
     */
    private fun addVisualEcho(intensity: Float, centerX: Float = 0.5f, centerY: Float = 0.5f) {
        // use music response controller to decide if generate
        val (shouldGenerate, effectIntensity) = musicResponseController.shouldGenerateEffect(
            SurpriseMusicResponseController.EffectType.VISUAL_ECHO,
            intensity
        )
        
        if (shouldGenerate) {
            // use adjusted intensity to generate visual echo
            visualEchoSystem.addVisualEcho(
                intensity = effectIntensity,
                centerX = centerX,
                centerY = centerY,
                shapeType = Random.nextInt(5)
            )
        }
    }
    
    /**
     * generate pulse wave burst
     */
    private fun generatePulseWaveBurst(
        energy: Float, 
        volumeChange: Float, 
        intensityLevel: Int = 1,
        effectIntensity: Float = 1.0f
    ) {
        // use music response controller to decide if generate
        val (shouldGenerate, adjustedEffectIntensity) = musicResponseController.shouldGenerateEffect(
            SurpriseMusicResponseController.EffectType.PULSE_WAVE,
            effectIntensity
        )
        
        if (shouldGenerate) {
            // use adjusted intensity and quantity to generate pulse wave
            val adjustedCount = (intensityLevel * adjustedEffectIntensity).toInt().coerceAtLeast(1)
            repeat(adjustedCount) { layer ->
            // adjust parameters according to layer
                val layerOffset = layer * 0.1f  // decrease layer delay
                val layerScale = 1f - layer * 0.1f  // decrease layer scale
            
                // create pulse wave - increase parameters to ensure effect
            createPulseWave(
                    centerX = 0.5f + (Random.nextFloat() - 0.5f) * 0.1f,
                    centerY = 0.5f + (Random.nextFloat() - 0.5f) * 0.1f,
                    intensity = (0.9f + intensityLevel * 0.1f) * layerScale * adjustedEffectIntensity,
                    maxRadius = (1.5f + intensityLevel * 0.5f) * adjustedEffectIntensity, // increase max radius
                    initialSpeed = (0.8f + intensityLevel * 0.4f + layer * 0.1f) * adjustedEffectIntensity, // increase initial speed
                    lifespan = (1.0f + intensityLevel * 0.4f - layerOffset) * adjustedEffectIntensity.coerceAtLeast(0.7f), // increase lifespan
                distortion = when (intensityLevel) {
                        3 -> 0.3f + Random.nextFloat() * 0.2f  // strong rhythm - moderate distortion, not too complex
                        2 -> 0.2f + Random.nextFloat() * 0.15f  // medium rhythm - slight distortion
                        else -> 0.1f + Random.nextFloat() * 0.1f  // weak/weak - minimum distortion
                    } * adjustedEffectIntensity.coerceAtLeast(0.7f),
                    distortionFreq = 6 + intensityLevel * 1, // decrease sides, 6-9 sides, simpler
                    thickness = (6f + intensityLevel * 4f) * adjustedEffectIntensity.coerceAtLeast(0.8f), // increase line thickness
                    acceleration = -0.05f - intensityLevel * 0.05f  // decrease negative acceleration, pulse wave decelerates slower
                )
            }
        }
    }
    
    /**
     * create single pulse wave
     */
    private fun createPulseWave(
        centerX: Float,
        centerY: Float,
        intensity: Float,
        maxRadius: Float,
        initialSpeed: Float,
        lifespan: Float,
        distortion: Float,
        distortionFreq: Int,
        thickness: Float,
        acceleration: Float
    ) {
        // create pulse wave color - ensure in high brightness range
        val pulseColor = createSurpriseColor(intensity, true)
        
        // random select pulse wave style, but increase double layer style probability
        val pulseStyle = when {
            Random.nextFloat() < 0.6f -> 2 // 60% probability - double layer style
            Random.nextFloat() < 0.7f -> 0 // 28% probability - line style
            else -> 1 // 12% probability - fill style
        }
        
        // add to pulse wave list
        pulseWaves.add(PulseWave(
            centerX = centerX,
            centerY = centerY,
            radius = 0.02f,  // increase initial radius
            maxRadius = maxRadius,
            speed = initialSpeed,
            acceleration = acceleration,
            intensity = 1.0f,  // initial intensity maximum
            thickness = thickness,
            initialThickness = thickness,
            color = pulseColor,
            distortion = distortion,
            distortionFreq = distortionFreq,
            lifespan = lifespan,
            maxLifespan = lifespan,
            style = pulseStyle
        ))
    }
    
    /**
     * generate lightning effect
     */
    private fun generateLightning(
        centerX: Float,
        centerY: Float,
        intensity: Float,
        maxSegments: Int,
        branchProbability: Float
    ) {
        // limit lightning count, prevent stuttering
        if (lightningFlashes.size >= 8) return
        
        // random angle
        val angle = Random.nextFloat() * Math.PI.toFloat() * 2
        
        // calculate end point - lightning length varies with intensity, increase length
        val length = 0.4f + intensity * 0.8f // increase length
        val endX = centerX + cos(angle) * length
        val endY = centerY + sin(angle) * length
        
        // generate lightning segments
        val segments = generateLightningSegments(
            startX = centerX,
            startY = centerY,
            endX = endX,
            endY = endY,
            intensity = intensity,
            segmentsLeft = maxSegments,
            branchProbability = branchProbability
        )
        
        // create lightning color - increase color variety
        val lightningColor = when (Random.nextInt(4)) {
            0 -> { // blue white
            Color.argb(
                255,
                230 + Random.nextInt(25),
                240 + Random.nextInt(15),
                255
            )
            }
            1 -> { // purple white
            Color.argb(
                255,
                240 + Random.nextInt(15),
                230 + Random.nextInt(25),
                255
            )
            }            
            2 -> { // pure white
                Color.argb(
                    255,
                    250 + Random.nextInt(5),
                    250 + Random.nextInt(5),
                    250 + Random.nextInt(5)
                )
            }
            else -> { // pale yellow
                Color.argb(
                    255,
                    255,
                    250 + Random.nextInt(5),
                    230 + Random.nextInt(25)
                )
            }
        }
        
        // add to lightning list - increase lightning duration
        lightningFlashes.add(Lightning(
            startX = centerX,
            startY = centerY,
            segments = segments,
            intensity = intensity,
            lifespan = 0.25f + intensity * 0.35f, // increase lightning duration
            maxLifespan = 0.6f, // increase maximum lifespan
            color = lightningColor
        ))
    }
    
    /**
     * generate lightning segments
     */
    private fun generateLightningSegments(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        intensity: Float,
        segmentsLeft: Int,
        branchProbability: Float
    ): List<LightningSegment> {
        // basic case - no remaining segments
        if (segmentsLeft <= 0) {
            return listOf(LightningSegment(
                startX = startX,
                startY = startY,
                endX = endX,
                endY = endY,
                thickness = 2f + intensity * 3f,
                childSegments = emptyList()
            ))
        }
        
        // calculate midpoint
        val midX = (startX + endX) / 2f
        val midY = (startY + endY) / 2f
        
        // add random offset - offset amount related to intensity and remaining segments
        val offsetAmount = 0.05f + intensity * 0.1f * segmentsLeft / 5f
        val perpX = -(endY - startY)  // vertical X component
        val perpY = endX - startX     // vertical Y component
        val perpLength = sqrt(perpX * perpX + perpY * perpY)
        
        // avoid division by zero
        if (perpLength < 0.0001f) {
            return listOf(LightningSegment(
                startX = startX,
                startY = startY,
                endX = endX,
                endY = endY,
                thickness = 2f + intensity * 3f,
                childSegments = emptyList()
            ))
        }
        
        // calculate unit vertical vector
        val unitPerpX = perpX / perpLength
        val unitPerpY = perpY / perpLength
        
        // apply offset
        val offset = (Random.nextFloat() * 2f - 1f) * offsetAmount
        val newMidX = midX + unitPerpX * offset
        val newMidY = midY + unitPerpY * offset
        
        // generate first half
        val firstHalf = generateLightningSegments(
            startX = startX,
            startY = startY,
            endX = newMidX,
            endY = newMidY,
            intensity = intensity * 0.9f,  // intensity slightly weakened
            segmentsLeft = segmentsLeft - 1,
            branchProbability = branchProbability * 0.8f  // branch probability decrease
        )
        
        // generate second half
        val secondHalf = generateLightningSegments(
            startX = newMidX,
            startY = newMidY,
            endX = endX,
            endY = endY,
            intensity = intensity * 0.9f,
            segmentsLeft = segmentsLeft - 1,
            branchProbability = branchProbability * 0.8f
        )
        
        // possible generate branches
        val branches = mutableListOf<LightningSegment>()
        if (Random.nextFloat() < branchProbability && segmentsLeft > 1) {
            // branch angle
            val branchAngle = Random.nextFloat() * Math.PI.toFloat() * 0.5f + Math.PI.toFloat() * 0.25f
            
            // calculate branch direction
            val dirX = endX - startX
            val dirY = endY - startY
            val dirLength = sqrt(dirX * dirX + dirY * dirY)
            
            if (dirLength > 0.0001f) {
                val unitDirX = dirX / dirLength
                val unitDirY = dirY / dirLength
                
                // rotate direction vector
                val rotatedX = unitDirX * cos(branchAngle) - unitDirY * sin(branchAngle)
                val rotatedY = unitDirX * sin(branchAngle) + unitDirY * cos(branchAngle)
                
                // branch length
                val branchLength = dirLength * (0.3f + Random.nextFloat() * 0.3f)
                
                // branch end point
                val branchEndX = newMidX + rotatedX * branchLength
                val branchEndY = newMidY + rotatedY * branchLength
                
                // generate branch
                val branch = generateLightningSegments(
                    startX = newMidX,
                    startY = newMidY,
                    endX = branchEndX,
                    endY = branchEndY,
                    intensity = intensity * 0.7f,  // branch intensity weaker
                    segmentsLeft = segmentsLeft - 2,
                    branchProbability = branchProbability * 0.5f  // branch probability decrease
                )
                
                branches.addAll(branch)
            }
        }
        
        // merge all segments
        return firstHalf + secondHalf + branches
    }
    
    /**
     * generate vocal flash
     */
    private fun generateVocalFlash(changeRate: Float, energy: Float) {
        // trigger special vocal flash - warmer tone
        triggerFlash(changeRate * 2f)
        
        // generate center position - vocal flash position more random
        val centerX = 0.3f + Random.nextFloat() * 0.4f
        val centerY = 0.3f + Random.nextFloat() * 0.4f
        
        // create warm tone pulse wave
        createPulseWave(
            centerX = centerX,
            centerY = centerY,
            intensity = 0.7f + changeRate * 0.3f,
            maxRadius = 1.2f + energy * 0.5f,
            initialSpeed = 0.6f + changeRate * 0.4f,
            lifespan = 0.7f + energy * 0.3f,
            distortion = 0.1f + Random.nextFloat() * 0.1f,  // vocal pulse wave distortion smaller
            distortionFreq = 8 + Random.nextInt(5),  // 8-12 sides
            thickness = 4f + changeRate * 10f,
            acceleration = -0.15f  // vocal pulse wave decelerates faster
        )
        
        addVisualEcho(0.6f + changeRate * 0.4f)
    }
    
    /**
     * draw pulse wave
     */
    private fun drawPulseWaves(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        pulseWaves.forEach { pulse ->
            // calculate actual coordinates
            val centerX = if (isVertical) pulse.centerY * width else pulse.centerX * width
            val centerY = if (isVertical) pulse.centerX * height else pulse.centerY * height
            val radius = if (isVertical) 
                pulse.radius * height.coerceAtMost(width)
            else 
                pulse.radius * width.coerceAtMost(height)
            
            // set paint properties - increase basic opacity
            val alpha = (pulse.intensity * 255 * 1.2f).toInt().coerceIn(0, 255)
            
            // set paint properties - increase basic opacity
            when (pulse.style) {
                0 -> { // line style
                    pulsePaint.style = Paint.Style.STROKE
                    pulsePaint.strokeWidth = pulse.thickness
            pulsePaint.color = Color.argb(
                alpha,
                Color.red(pulse.color),
                Color.green(pulse.color),
                Color.blue(pulse.color)
            )
                }
                1 -> { // fill style - increase fill opacity
                    pulsePaint.style = Paint.Style.FILL
                    pulsePaint.color = Color.argb(
                        (alpha * 0.6f).toInt(), // increase fill opacity
                        Color.red(pulse.color),
                        Color.green(pulse.color),
                        Color.blue(pulse.color)
                    )
                }
                2 -> { // double layer style - draw fill first, then draw line
                    pulsePaint.style = Paint.Style.FILL
                    pulsePaint.color = Color.argb(
                        (alpha * 0.4f).toInt(), // increase fill opacity
                        Color.red(pulse.color),
                        Color.green(pulse.color),
                        Color.blue(pulse.color)
                    )
                }
            }
            
            // draw pulse wave - decide shape based on distortion parameters
            if (pulse.distortion < 0.05f) {
                // almost no distortion - draw circle
                canvas.drawCircle(centerX, centerY, radius, pulsePaint)
            } else {
                val path = Path()
                val points = pulse.distortionFreq
                
                for (i in 0 until points) {
                    val angle = i * (2 * Math.PI / points)
                    
                    // calculate distorted radius - use sine wave to create organic distortion
                    val distortedRadius = radius * (1f + 
                        sin(angle * 3 + pulse.lifespan * 5) * pulse.distortion * 0.5f +
                        cos(angle * 5 + pulse.lifespan * 3) * pulse.distortion * 0.5f
                    )
                    
                    val x = centerX + cos(angle) * distortedRadius
                    val y = centerY + sin(angle) * distortedRadius
                    
                    if (i == 0) {
                        path.moveTo(x.toFloat(), y.toFloat())
                    } else {
                        path.lineTo(x.toFloat(), y.toFloat())
                    }
                }
                
                path.close()
                canvas.drawPath(path, pulsePaint)
            }
            
            // if double layer style, draw another layer of line - increase line thickness
            if (pulse.style == 2) {
                pulsePaint.style = Paint.Style.STROKE
                pulsePaint.strokeWidth = pulse.thickness * 0.8f // increase line thickness
                pulsePaint.color = Color.argb(
                    alpha,
                    Color.red(pulse.color),
                    Color.green(pulse.color),
                    Color.blue(pulse.color)
                )
                
                if (pulse.distortion < 0.05f) {
                    canvas.drawCircle(centerX, centerY, radius, pulsePaint)
                } else {
                    // redraw path
                    val path = Path()
                    val points = pulse.distortionFreq
                    
                    for (i in 0 until points) {
                        val angle = i * (2 * Math.PI / points)
                        val distortedRadius = radius * (1f + 
                            sin(angle * 3 + pulse.lifespan * 5) * pulse.distortion * 0.5f +
                            cos(angle * 5 + pulse.lifespan * 3) * pulse.distortion * 0.5f
                        )
                        
                        val x = centerX + cos(angle) * distortedRadius
                        val y = centerY + sin(angle) * distortedRadius
                        
                        if (i == 0) {
                            path.moveTo(x.toFloat(), y.toFloat())
                        } else {
                            path.lineTo(x.toFloat(), y.toFloat())
                        }
                    }
                    
                    path.close()
                    canvas.drawPath(path, pulsePaint)
                }
            }
            
            // add internal glow effect to all pulse waves - decrease threshold, increase glow intensity
            if (pulse.intensity > 0.3f) { // decrease threshold, let more pulse waves have glow effect
                // create glow effect
                val glowPaint = Paint()
                glowPaint.style = Paint.Style.FILL
                
                // create radial gradient - increase glow radius and intensity
                val glowRadius = radius * 0.6f // increase glow radius
                glowPaint.shader = RadialGradient(
                    centerX, centerY, glowRadius,
                    intArrayOf(
                        Color.argb((alpha * 0.9f).toInt(), 255, 255, 255), // increase center brightness
                        Color.argb((alpha * 0.5f).toInt(), 255, 255, 255), // increase middle brightness
                        Color.argb(0, 255, 255, 255)
                    ),
                    floatArrayOf(0f, 0.4f, 1f),
                    Shader.TileMode.CLAMP
                )
                
                // draw glow effect - increase glow area
                canvas.drawCircle(centerX, centerY, radius * 0.7f, glowPaint)
            }
        }
    }
    
    /**
     * draw lightning effect
     */
    private fun drawLightningFlashes(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        lightningFlashes.forEach { lightning ->
            // calculate actual coordinates
            val startX = if (isVertical) lightning.startY * width else lightning.startX * width
            val startY = if (isVertical) lightning.startX * height else lightning.startY * height
            
            // set lightning paint properties
            val alpha = (lightning.intensity * 255).toInt().coerceIn(0, 255)
            lightningPaint.color = Color.argb(
                alpha,
                Color.red(lightning.color),
                Color.green(lightning.color),
                Color.blue(lightning.color)
            )
            
            // draw lightning segments
            drawLightningSegments(
                canvas, lightning.segments, width, height, isVertical, lightning.intensity
            )
            
            // draw lightning start point glow effect
            val glowPaint = Paint().apply {
                color = Color.argb(
                    (alpha * 0.7f).toInt(),
                    Color.red(lightning.color),
                    Color.green(lightning.color),
                    Color.blue(lightning.color)
                )
                style = Paint.Style.FILL
            }
            
            // lightning start point glow effect
            val glowRadius = 5f + lightning.intensity * 15f
            canvas.drawCircle(startX, startY, glowRadius, glowPaint)
        }
    }
    
    /**
     * recursively draw lightning segments
     */
    private fun drawLightningSegments(
        canvas: Canvas,
        segments: List<LightningSegment>,
        width: Float,
        height: Float,
        isVertical: Boolean,
        intensity: Float
    ) {
        segments.forEach { segment ->
            // calculate actual coordinates
            val startX = if (isVertical) segment.startY * width else segment.startX * width
            val startY = if (isVertical) segment.startX * height else segment.startY * height
            val endX = if (isVertical) segment.endY * width else segment.endX * width
            val endY = if (isVertical) segment.endX * height else segment.endY * height
            
            // set line thickness
            lightningPaint.strokeWidth = segment.thickness
            
            // draw lightning segments
            canvas.drawLine(startX, startY, endX, endY, lightningPaint)
            
            // recursively draw child segments
            drawLightningSegments(
                canvas, segment.childSegments, width, height, isVertical, intensity * 0.8f
            )
            
            // add some flash points randomly on lightning segments
            if (Random.nextFloat() < 0.3f * intensity) {
                val sparkX = startX + (endX - startX) * Random.nextFloat()
                val sparkY = startY + (endY - startY) * Random.nextFloat()
                val sparkRadius = 1f + Random.nextFloat() * 3f * intensity
                
                val sparkPaint = Paint().apply {
                    color = Color.WHITE
                    alpha = (200 * intensity).toInt()
                    style = Paint.Style.FILL
                }
                
                canvas.drawCircle(sparkX, sparkY, sparkRadius, sparkPaint)
            }
        }
    }
    
    /**
     * draw visual echo
     */
    private fun drawVisualEchoes(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        // use new visual echo system
        visualEchoSystem.render(canvas, width, height, isVertical)
    }
    
    /**
     * draw flash effect
     */
    private fun drawFlashEffect(canvas: Canvas, width: Float, height: Float, opacity: Float) {
        if (opacity <= 0.01f) return
        
        // create flash paint
        val flashPaint = Paint().apply {
            color = Color.WHITE
            alpha = (opacity * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.FILL
        }
        
        // draw full screen flash
        canvas.drawRect(0f, 0f, width, height, flashPaint)
    }
    
    /**
     * calculate energy of specific frequency band
     */
    private fun calculateBandEnergy(data: FloatArray, startIndex: Int, endIndex: Int): Float {
        var sum = 0f
        val validStartIndex = startIndex.coerceIn(0, data.size - 1)
        val validEndIndex = endIndex.coerceIn(0, data.size - 1)
        
        for (i in validStartIndex..validEndIndex) {
            sum += data[i] * data[i]  // square sum
        }
        
        return if (validEndIndex > validStartIndex) 
            sqrt(sum / (validEndIndex - validStartIndex + 1))
        else 
            0f
    }
    
    /**
     * create surprise color
     * @param intensity intensity coefficient
     * @param isHighlight whether it is a highlight color
     */
    private fun createSurpriseColor(intensity: Float, isHighlight: Boolean): Int {
        // basic tone - surprise emotion uses high brightness white and blue tone
        return if (isHighlight) {
            // highlight color - bright white or light blue
            if (Random.nextFloat() < 0.7f) {
                // 70% probability - bright white
                Color.rgb(
                    250 + (intensity * 5).toInt().coerceIn(0, 5),
                    250 + (intensity * 5).toInt().coerceIn(0, 5),
                    255
                )
            } else {
                // 30% probability - light blue
                Color.rgb(
                    220 + (intensity * 35).toInt().coerceIn(0, 35),
                    240 + (intensity * 15).toInt().coerceIn(0, 15),
                    255
                )
            }
        } else {
            // non-highlight color - darker blue or purple
            if (Random.nextFloat() < 0.6f) {
                // 60% probability - blue
                Color.rgb(
                    100 + (intensity * 100).toInt().coerceIn(0, 100),
                    150 + (intensity * 100).toInt().coerceIn(0, 100),
                    200 + (intensity * 55).toInt().coerceIn(0, 55))
            } else {
                // 40% probability - purple
                Color.rgb(
                    150 + (intensity * 100).toInt().coerceIn(0, 100),
                    100 + (intensity * 100).toInt().coerceIn(0, 100),
                    200 + (intensity * 55).toInt().coerceIn(0, 55))
            }
        }
    }
    
    private fun ensureBaseBackgroundGlows(maxGlows: Int, volumeResponse: Float) {
        // if background glows are too few, add some basic glows
        if (backgroundGlows.size < 2) {
            // add central large glow
            if (!backgroundGlows.any { it.size > 1.5f }) {
                addBackgroundGlow(0.5f + volumeResponse * 0.3f, false)
            }
            
            // add random position small glow
            if (Random.nextFloat() < 0.3f && backgroundGlows.size < maxGlows) {
                addBackgroundGlow(0.3f + volumeResponse * 0.2f, false)
            }
        }
    }
    
    /**
     * add background glow
     */
    private fun addBackgroundGlow(intensity: Float, isRhythmPeak: Boolean) {
        // glow position - more concentrated in the center
        val centerX = if (isRhythmPeak) {
            0.5f + (Random.nextFloat() - 0.5f) * 0.2f  // more concentrated when rhythm peak
        } else {
            0.3f + Random.nextFloat() * 0.4f  // random distribution
        }
        
        val centerY = if (isRhythmPeak) {
            0.5f + (Random.nextFloat() - 0.5f) * 0.2f  // more concentrated when rhythm peak
        } else {
            0.3f + Random.nextFloat() * 0.4f  // random distribution
        }
        
        // glow size - larger when rhythm peak
        val size = if (isRhythmPeak) {
            1.5f + Random.nextFloat() * 1.0f  // larger glow
        } else {
            0.8f + Random.nextFloat() * 0.7f  // medium glow
        }
        
        // glow color - use surprise color
        val glowColor = createSurpriseColor(intensity, false)
        
        // glow lifespan - shorter when rhythm peak
        val lifespan = if (isRhythmPeak) {
            1.0f + Random.nextFloat() * 1.0f  // short lifespan
        } else {
            2.0f + Random.nextFloat() * 2.0f  // long lifespan
        }
        
        // add to glow list
        backgroundGlows.add(BackgroundGlow(
            centerX = centerX,
            centerY = centerY,
            size = size,
            color = glowColor,
            opacity = (0.1f + intensity * 0.2f).coerceIn(0.1f, 0.3f),  // low opacity
            lifespan = lifespan,
            maxLifespan = lifespan,
            pulseRate = 0.5f + Random.nextFloat() * 1.0f  // pulse rate
        ))
    }
    
    /**
     * update background glows
     */
    private fun updateBackgroundGlows(deltaTime: Float) {
        // update and remove expired background glows
        backgroundGlows.removeAll { glow ->
            // update lifespan
            glow.lifespan -= deltaTime * 0.5f  // slow decay
            
            // update opacity - create pulse effect using sine wave
            val lifeFraction = (glow.lifespan / glow.maxLifespan).coerceIn(0f, 1f)
            val pulsePhase = (glow.maxLifespan - glow.lifespan) * glow.pulseRate
            val pulseFactor = 0.2f * (sin(pulsePhase * 6.28f) * 0.5f + 0.5f)
            
            glow.opacity = (glow.baseOpacity * lifeFraction + pulseFactor).coerceIn(0.05f, 0.4f)
            
            // remove condition: lifespan ended
            glow.lifespan <= 0
        }
    }
    
    /**
     * generate flashing dots
     */
    private fun generateFlashingDots(data: FloatArray, maxDots: Int, volumeResponse: Float) {
        // limit flashing dots count
        if (flashingDots.size >= maxDots) return
        
        // adjust generation probability based on volume response
        val generationProbability = 0.05f + volumeResponse * 0.15f
        
        // generate flashing dots
        if (Random.nextFloat() < generationProbability) {
            // random position - full screen distribution
            val x = Random.nextFloat()
            val y = Random.nextFloat()
            
            // random size - small dot
            val size = 0.05f + Random.nextFloat() * 0.1f
            
            // random lifespan
            val lifespan = 0.5f + Random.nextFloat() * 1.5f
            
            // flashing frequency
            val flickerRate = 3.0f + Random.nextFloat() * 5.0f
            
            // add to flashing dots list
            flashingDots.add(FlashingDot(
                x = x,
                y = y,
                size = size,
                color = createSurpriseColor(volumeResponse, true),
                baseOpacity = 0.7f + Random.nextFloat() * 0.3f,
                lifespan = lifespan,
                maxLifespan = lifespan,
                flickerRate = flickerRate
            ))
        }
        
        // add flashing dots at spectrum peak
        data.forEachIndexed { index, magnitude ->
            if (magnitude > 0.5f && Random.nextFloat() < 0.2f && flashingDots.size < maxDots) {
                // determine position based on frequency index
                val x = index.toFloat() / data.size
                val y = 0.3f + Random.nextFloat() * 0.4f
                
                // size related to spectrum energy
                val size = 0.08f + magnitude * 0.15f
                
                // lifespan related to energy
                val lifespan = 0.5f + magnitude * 1.0f
                
                // add to flashing dots list
                flashingDots.add(FlashingDot(
                    x = x,
                    y = y,
                    size = size,
                    color = createSurpriseColor(magnitude, true),
                    baseOpacity = 0.8f + magnitude * 0.2f,  // modify: opacity -> baseOpacity
                    lifespan = lifespan,
                    maxLifespan = lifespan,
                    flickerRate = 5.0f + magnitude * 3.0f
                ))
            }
        }
    }
    
    /**
     * update flashing dots
     */
    private fun updateFlashingDots(deltaTime: Float) {
        // update and remove expired flashing dots
        flashingDots.removeAll { dot ->
            // update lifespan
            dot.lifespan -= deltaTime
            
            // update opacity - create flickering effect using sine wave
            val lifeFraction = (dot.lifespan / dot.maxLifespan).coerceIn(0f, 1f)
            val flickerPhase = (dot.maxLifespan - dot.lifespan) * dot.flickerRate
            val flickerFactor = sin(flickerPhase * 6.28f) * 0.5f + 0.5f
            
            dot.currentOpacity = (dot.baseOpacity * lifeFraction * flickerFactor).coerceIn(0f, 1f)
            
            // remove condition: lifespan ended
            dot.lifespan <= 0
        }
    }
    
    /**
     * draw background glows
     */
    private fun drawBackgroundGlows(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        backgroundGlows.forEach { glow ->
            // calculate actual coordinates
            val centerX = if (isVertical) glow.centerY * width else glow.centerX * width
            val centerY = if (isVertical) glow.centerX * height else glow.centerY * height
            
            // calculate radius - use larger size
            val radius = glow.size * width.coerceAtMost(height) * 0.5f
            
            // set glow color and opacity
            val alpha = (glow.opacity * 255).toInt().coerceIn(0, 255)
            
            // create radial gradient
            glowPaint.shader = RadialGradient(
                centerX, centerY, radius,
                intArrayOf(
                    Color.argb(
                        alpha,
                        Color.red(glow.color),
                        Color.green(glow.color),
                        Color.blue(glow.color)
                    ),
                    Color.argb(
                        (alpha * 0.6f).toInt(),
                        Color.red(glow.color),
                        Color.green(glow.color),
                        Color.blue(glow.color)
                    ),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            
            // draw glow
            canvas.drawCircle(centerX, centerY, radius, glowPaint)
            glowPaint.shader = null
        }
    }
    
    /**
     * draw flashing dots
     */
    private fun drawFlashingDots(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        flashingDots.forEach { dot ->
            // calculate actual coordinates
            val x = if (isVertical) dot.y * width else dot.x * width
            val y = if (isVertical) dot.x * height else dot.y * height
            
            // calculate radius
            val radius = dot.size * width.coerceAtMost(height) * 0.02f
            
            // set dot color and opacity
            val alpha = (dot.currentOpacity * 255).toInt().coerceIn(0, 255)
            dotPaint.color = Color.argb(
                alpha,
                Color.red(dot.color),
                Color.green(dot.color),
                Color.blue(dot.color)
            )
            
            // draw dot
            canvas.drawCircle(x, y, radius, dotPaint)
            
            // draw glow
            val glowRadius = radius * 2.5f
            dotPaint.shader = RadialGradient(
                x, y, glowRadius,
                Color.argb(
                    (alpha * 0.7f).toInt(),
                    Color.red(dot.color),
                    Color.green(dot.color),
                    Color.blue(dot.color)
                ),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            
            canvas.drawCircle(x, y, glowRadius, dotPaint)
            dotPaint.shader = null
        }
    }
    
    /**
     * generate energy rays
     */
    private fun generateEnergyRays(data: FloatArray, volumeResponse: Float) {
        // limit energy rays count
        val maxRays = 12
        if (energyRays.size >= maxRays) return
        
        // adjust generation probability based on volume response
        val generationProbability = 0.03f + volumeResponse * 0.1f
        
        // generate energy rays
        if (Random.nextFloat() < generationProbability) {
            // random starting point
            val startX = 0.4f + Random.nextFloat() * 0.2f
            val startY = 0.4f + Random.nextFloat() * 0.2f
            
            // random angle
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2
            
            // random length
            val length = 0.3f + Random.nextFloat() * 0.5f
            
            // random width
            val width = 2f + Random.nextFloat() * 4f
            
            // random lifespan
            val lifespan = 0.5f + Random.nextFloat() * 1.0f
            
            // random color - use surprise color
            val rayColor = createSurpriseColor(volumeResponse, true)
            
            // add to energy rays list
            energyRays.add(EnergyRay(
                startX = startX,
                startY = startY,
                angle = angle,
                length = length,
                width = width,
                color = rayColor,
                opacity = 0.7f + Random.nextFloat() * 0.3f,
                lifespan = lifespan,
                maxLifespan = lifespan,
                waveFactor = 0.05f + Random.nextFloat() * 0.1f,
                waveFrequency = 10f + Random.nextFloat() * 20f
            ))
        }
        
        // add energy rays at spectrum peak
        if (isRhythmPeak && Random.nextFloat() < 0.5f) {
            // emit multiple rays from center
            val rayCount = 3 + Random.nextInt(5)
            val centerX = 0.5f
            val centerY = 0.5f
            
            repeat(rayCount) {
                val angle = it * (2 * Math.PI.toFloat() / rayCount)
                val length = 0.5f + Random.nextFloat() * 0.5f
                val width = 3f + Random.nextFloat() * 5f
                val lifespan = 0.8f + Random.nextFloat() * 0.7f
                
                energyRays.add(EnergyRay(
                    startX = centerX,
                    startY = centerY,
                    angle = angle,
                    length = length,
                    width = width,
                    color = createSurpriseColor(0.9f, true),
                    opacity = 0.8f + Random.nextFloat() * 0.2f,
                    lifespan = lifespan,
                    maxLifespan = lifespan,
                    waveFactor = 0.02f + Random.nextFloat() * 0.05f,
                    waveFrequency = 5f + Random.nextFloat() * 10f
                ))
            }
        }
    }
    
    /**
     * update energy rays
     */
    private fun updateEnergyRays(deltaTime: Float) {
        // update and remove expired energy rays
        energyRays.removeAll { ray ->
            // update lifespan
            ray.lifespan -= deltaTime * 1.5f
            
            // update opacity
            val lifeFraction = (ray.lifespan / ray.maxLifespan).coerceIn(0f, 1f)
            ray.opacity = ray.baseOpacity * lifeFraction
            
            // remove condition: lifespan ended
            ray.lifespan <= 0
        }
    }
    
    /**
     * draw energy rays
     */
    private fun drawEnergyRays(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        energyRays.forEach { ray ->
            // calculate actual coordinates
            val startX = if (isVertical) ray.startY * width else ray.startX * width
            val startY = if (isVertical) ray.startX * height else ray.startY * height
            
            // calculate end point
            val endX = startX + cos(ray.angle) * ray.length * width
            val endY = startY + sin(ray.angle) * ray.length * height
            
            // set ray color and opacity
            val alpha = (ray.opacity * 255).toInt().coerceIn(0, 255)
            rayPaint.color = Color.argb(
                alpha,
                Color.red(ray.color),
                Color.green(ray.color),
                Color.blue(ray.color)
            )
            rayPaint.strokeWidth = ray.width
            
            // create path - wavy ray
            val path = Path()
            path.moveTo(startX, startY)
            
            // calculate ray length
            val rayLength = sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
            
            // calculate unit vector
            val unitX = (endX - startX) / rayLength
            val unitY = (endY - startY) / rayLength
            
            // calculate perpendicular unit vector
            val perpX = -unitY
            val perpY = unitX
            
            // draw wavy ray
            val segments = 20
            for (i in 0..segments) {
                val t = i.toFloat() / segments
                val segmentX = startX + unitX * rayLength * t
                val segmentY = startY + unitY * rayLength * t
                
                // add wave effect
                val waveOffset = sin(t * ray.waveFrequency) * ray.waveFactor * rayLength
                val offsetX = segmentX + perpX * waveOffset
                val offsetY = segmentY + perpY * waveOffset
                
                if (i == 0) {
                    path.moveTo(offsetX, offsetY)
                } else {
                    path.lineTo(offsetX, offsetY)
                }
            }
            
            // draw path
            canvas.drawPath(path, rayPaint)
            
            // draw starting point glow
            val glowPaint = Paint().apply {
                color = Color.argb(
                    (alpha * 0.8f).toInt(),
                    Color.red(ray.color),
                    Color.green(ray.color),
                    Color.blue(ray.color)
                )
                style = Paint.Style.FILL
            }
            
            canvas.drawCircle(startX, startY, ray.width * 1.5f, glowPaint)
        }
    }
    
    /**
     * add ripple effect
     */
    private fun addRippleEffect(energy: Float, volumeResponse: Float) {
        // random position
        val centerX = 0.5f + (Random.nextFloat() - 0.5f) * 0.2f
        val centerY = 0.5f + (Random.nextFloat() - 0.5f) * 0.2f
        
        // random initial radius
        val initialRadius = 0.05f + Random.nextFloat() * 0.1f
        
        // random maximum radius
        val maxRadius = 0.8f + Random.nextFloat() * 0.4f
        
        // random expansion speed
        val expansionSpeed = 0.3f + energy * 0.5f
        
        // random lifespan
        val lifespan = 1.0f + Random.nextFloat() * 1.0f
        
        // random color - use surprise color
        val rippleColor = createSurpriseColor(volumeResponse, true)
        
        // add to ripple effects list
        rippleEffects.add(RippleEffect(
            centerX = centerX,
            centerY = centerY,
            radius = initialRadius,
            maxRadius = maxRadius,
            expansionSpeed = expansionSpeed,
            color = rippleColor,
            opacity = 0.6f + Random.nextFloat() * 0.4f,
            lifespan = lifespan,
            maxLifespan = lifespan,
            thickness = 2f + energy * 6f,
            waveCount = 2 + Random.nextInt(3)
        ))
    }
    
    /**
     * update ripple effects
     */
    private fun updateRippleEffects(deltaTime: Float) {
        // update and remove expired ripple effects
        rippleEffects.removeAll { ripple ->
            // update lifespan
            ripple.lifespan -= deltaTime
            
            // update radius
            ripple.radius += ripple.expansionSpeed * deltaTime
            
            // update opacity
            val lifeFraction = (ripple.lifespan / ripple.maxLifespan).coerceIn(0f, 1f)
            ripple.opacity = ripple.baseOpacity * lifeFraction
            
            // remove condition: lifespan ended or radius exceeds maximum
            ripple.lifespan <= 0 || ripple.radius >= ripple.maxRadius
        }
    }
    
    /**
     * draw ripple effects
     */
    private fun drawRippleEffects(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        rippleEffects.forEach { ripple ->
            // calculate actual coordinates
            val centerX = if (isVertical) ripple.centerY * width else ripple.centerX * width
            val centerY = if (isVertical) ripple.centerX * height else ripple.centerY * height
            
            // calculate radius
            val baseRadius = ripple.radius * width.coerceAtMost(height)
            
            // set ripple color and opacity
            val alpha = (ripple.opacity * 255).toInt().coerceIn(0, 255)
            ripplePaint.color = Color.argb(
                alpha,
                Color.red(ripple.color),
                Color.green(ripple.color),
                Color.blue(ripple.color)
            )
            ripplePaint.strokeWidth = ripple.thickness
            
            // draw multiple layers of ripples
            for (i in 0 until ripple.waveCount) {
                val waveRadius = baseRadius * (1f - i * 0.15f)
                if (waveRadius <= 0) continue
                
                // set opacity of each layer of ripple
                val waveAlpha = (alpha * (1f - i * 0.2f)).toInt().coerceIn(0, 255)
                ripplePaint.alpha = waveAlpha
                
                // draw ripple circle
                canvas.drawCircle(centerX, centerY, waveRadius, ripplePaint)
            }
        }
    }
    
    /**
     * add quantum collapse effect
     */
    private fun addQuantumCollapseEffect(intensity: Float) {
        // center position - always on screen center
        val centerX = 0.5f
        val centerY = 0.5f
        
        // initial radius
        val initialRadius = 0.05f + Random.nextFloat() * 0.1f
        
        // maximum radius
        val maxRadius = 0.4f + Random.nextFloat() * 0.3f
        
        // expansion speed
        val expansionSpeed = 0.2f + intensity * 0.4f
        
        // lifespan
        val lifespan = 0.8f + Random.nextFloat() * 0.8f
        
        // line count
        val lineCount = 6 + Random.nextInt(6)
        
        // rotation speed
        val rotationSpeed = (Random.nextFloat() * 2f - 1f) * 3f
        
        // color
        val color = createSurpriseColor(intensity, true)
        
        // add to quantum collapse effects list
        quantumCollapseEffects.add(QuantumCollapseEffect(
            centerX = centerX,
            centerY = centerY,
            radius = initialRadius,
            maxRadius = maxRadius,
            expansionSpeed = expansionSpeed,
            lineCount = lineCount,
            rotationSpeed = rotationSpeed,
            color = color,
            opacity = 0.8f + intensity * 0.2f,
            lifespan = lifespan,
            maxLifespan = lifespan,
            thickness = 2f + intensity * 4f,
            distortion = 0.1f + Random.nextFloat() * 0.2f
        ))
    }
    
    /**
     * update quantum collapse effects
     */
    private fun updateQuantumCollapseEffects(deltaTime: Float) {
        // update and remove expired quantum collapse effects
        quantumCollapseEffects.removeAll { effect ->
            // update lifespan
            effect.lifespan -= deltaTime
            
            // update radius
            effect.radius += effect.expansionSpeed * deltaTime
            
            // update rotation angle
            effect.rotation += effect.rotationSpeed * deltaTime
            
            // update opacity
            val lifeFraction = (effect.lifespan / effect.maxLifespan).coerceIn(0f, 1f)
            effect.opacity = effect.baseOpacity * lifeFraction
            
            // remove condition: lifespan ended or radius exceeds maximum
            effect.lifespan <= 0 || effect.radius >= effect.maxRadius
        }
    }
    
    /**
     * draw quantum collapse effects
     */
    private fun drawQuantumCollapseEffects(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        quantumCollapseEffects.forEach { effect ->
            // calculate actual coordinates
            val centerX = if (isVertical) effect.centerY * width else effect.centerX * width
            val centerY = if (isVertical) effect.centerX * height else effect.centerY * height
            
            // calculate radius
            val radius = effect.radius * width.coerceAtMost(height)
            
            // set paint properties
            val alpha = (effect.opacity * 255).toInt().coerceIn(0, 255)
            collapsePaint.color = Color.argb(
                alpha,
                Color.red(effect.color),
                Color.green(effect.color),
                Color.blue(effect.color)
            )
            collapsePaint.strokeWidth = effect.thickness
            
            // draw quantum collapse lines
            val path = Path()
            
            for (i in 0 until effect.lineCount) {
                val angle = i * (2 * Math.PI / effect.lineCount) + effect.rotation
                
                // calculate starting and ending points
                val innerRadius = radius * 0.2f
                val outerRadius = radius * (1f + sin(angle * 3) * effect.distortion)
                
                val startX = centerX + cos(angle) * innerRadius
                val startY = centerY + sin(angle) * innerRadius
                val endX = centerX + cos(angle) * outerRadius
                val endY = centerY + sin(angle) * outerRadius
                
                // draw line
                path.moveTo(startX.toFloat(), startY.toFloat())
                path.lineTo(endX.toFloat(), endY.toFloat())
            }
            
            // draw path
            canvas.drawPath(path, collapsePaint)
            
            // draw inner circle
            collapsePaint.style = Paint.Style.STROKE
            canvas.drawCircle(centerX, centerY, radius * 0.2f, collapsePaint)
            
            // draw outer circle - use dashed effect
            val dashEffect = DashPathEffect(floatArrayOf(radius * 0.05f, radius * 0.05f), 0f)
            collapsePaint.pathEffect = dashEffect
            canvas.drawCircle(centerX, centerY, radius, collapsePaint)
            collapsePaint.pathEffect = null
            
            // draw center glow
            val glowPaint = Paint().apply {
                color = Color.argb(
                    (alpha * 0.7f).toInt(),
                    Color.red(effect.color),
                    Color.green(effect.color),
                    Color.blue(effect.color)
                )
                style = Paint.Style.FILL
            }
            
            // create radial gradient
            glowPaint.shader = RadialGradient(
                centerX, centerY, radius * 0.3f,
                intArrayOf(
                    Color.argb(
                        (alpha * 0.9f).toInt(),
                        Color.red(effect.color),
                        Color.green(effect.color),
                        Color.blue(effect.color)
                    ),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            
            canvas.drawCircle(centerX, centerY, radius * 0.3f, glowPaint)
            glowPaint.shader = null
        }
    }
    
    /**
     * update spectrum response intensity
     */
    private fun updateSpectrumResponseIntensity(deltaTime: Float, data: FloatArray) {
        // calculate current spectrum average energy
        var totalEnergy = 0f
        for (value in data) {
            totalEnergy += value * value
        }
        val avgEnergy = sqrt(totalEnergy / data.size).coerceIn(0f, 1f)
        
        // smooth transition to new response intensity
        val targetIntensity = avgEnergy * 0.7f + currentVolume * 0.3f
        spectrumResponseIntensity += (targetIntensity - spectrumResponseIntensity) * deltaTime * 5f
    }
    
    /**
     * draw spectrum response effect
     */
    private fun drawSpectrumResponse(canvas: Canvas, data: FloatArray, width: Float, height: Float, isVertical: Boolean) {
        // only draw when there is enough energy
        if (spectrumResponseIntensity < 0.05f) return
        
        // set paint properties
        val alpha = (spectrumResponseIntensity * 100).toInt().coerceIn(0, 100)
        spectrumResponsePaint.color = Color.argb(
            alpha,
            200 + (spectrumResponseIntensity * 55).toInt().coerceIn(0, 55),
            220 + (spectrumResponseIntensity * 35).toInt().coerceIn(0, 35),
            255
        )
        spectrumResponsePaint.strokeWidth = 1f + spectrumResponseIntensity * 2f
        
        // draw spectrum response lines
        val path = Path()
        val barCount = data.size.coerceAtMost(128)
        val barWidth = width / barCount
        
        if (isVertical) {
            // vertical mode
            for (i in 0 until barCount) {
                val x = i * barWidth
                val magnitude = data[i] * spectrumResponseIntensity * height * 0.5f
                
                if (i == 0) {
                    path.moveTo(x, height - magnitude)
                } else {
                    path.lineTo(x, height - magnitude)
                }
            }
            
            // complete path
            path.lineTo(width, height)
            path.lineTo(0f, height)
            path.close()
            
            // draw fill
            spectrumResponsePaint.style = Paint.Style.FILL
            spectrumResponsePaint.alpha = (alpha * 0.3f).toInt()
            canvas.drawPath(path, spectrumResponsePaint)
            
            // draw line
            path.reset()
            for (i in 0 until barCount) {
                val x = i * barWidth
                val magnitude = data[i] * spectrumResponseIntensity * height * 0.5f
                
                if (i == 0) {
                    path.moveTo(x, height - magnitude)
                } else {
                    path.lineTo(x, height - magnitude)
                }
            }
            
            spectrumResponsePaint.style = Paint.Style.STROKE
            spectrumResponsePaint.alpha = alpha
            canvas.drawPath(path, spectrumResponsePaint)
        } else {
            // horizontal mode
            for (i in 0 until barCount) {
                val y = i * barWidth
                val magnitude = data[i] * spectrumResponseIntensity * width * 0.5f
                
                if (i == 0) {
                    path.moveTo(magnitude, y)
                } else {
                    path.lineTo(magnitude, y)
                }
            }
            
            // complete path
            path.lineTo(0f, height)
            path.lineTo(0f, 0f)
            path.close()
            
            // draw fill
            spectrumResponsePaint.style = Paint.Style.FILL
            spectrumResponsePaint.alpha = (alpha * 0.3f).toInt()
            canvas.drawPath(path, spectrumResponsePaint)
            
            // draw line
            path.reset()
            for (i in 0 until barCount) {
                val y = i * barWidth
                val magnitude = data[i] * spectrumResponseIntensity * width * 0.5f
                
                if (i == 0) {
                    path.moveTo(magnitude, y)
                } else {
                    path.lineTo(magnitude, y)
                }
            }
            
            spectrumResponsePaint.style = Paint.Style.STROKE
            spectrumResponsePaint.alpha = alpha
            canvas.drawPath(path, spectrumResponsePaint)
        }
    }
    
    /**
     * draw interaction enhancements
     */
    private fun drawInteractionEnhancements(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        // only draw when there is flash effect
        if (flashOpacity < 0.1f) return
        
        // draw light scattering effect
        val scatterPaint = Paint().apply {
            color = Color.WHITE
            alpha = (flashOpacity * 100).toInt().coerceIn(0, 100)
            style = Paint.Style.STROKE
            strokeWidth = 1f + flashOpacity * 2f
        }
        
        // center point
        val centerX = width / 2
        val centerY = height / 2
        
        // draw scattering rays
        val rayCount = 12
        val maxLength = width.coerceAtLeast(height) * 0.7f * flashOpacity
        
        for (i in 0 until rayCount) {
            val angle = i * (2 * Math.PI / rayCount)
            val length = maxLength * (0.5f + Random.nextFloat() * 0.5f)
            
            val endX = centerX + cos(angle) * length
            val endY = centerY + sin(angle) * length
            
            // use dashed effect
            val dashPhase = Random.nextFloat() * 10f
            val dashEffect = DashPathEffect(floatArrayOf(5f, 10f), dashPhase)
            scatterPaint.pathEffect = dashEffect
            
            canvas.drawLine(centerX, centerY, endX.toFloat(), endY.toFloat(), scatterPaint)
        }
        
        // draw halo ring
        scatterPaint.pathEffect = null
        scatterPaint.style = Paint.Style.STROKE
        scatterPaint.strokeWidth = 2f
        
        val ringCount = 3
        for (i in 0 until ringCount) {
            val ringRadius = maxLength * (0.2f + i * 0.2f) * (0.5f + Random.nextFloat() * 0.5f)
            scatterPaint.alpha = ((flashOpacity * 70) * (1f - i * 0.2f)).toInt().coerceIn(0, 70)
            
            canvas.drawCircle(centerX, centerY, ringRadius, scatterPaint)
        }
    }
    
    /**
     * pulse wave data class
     */
    private data class PulseWave(
        val centerX: Float,
        val centerY: Float,
        var radius: Float,
        val maxRadius: Float,
        var speed: Float,
        val acceleration: Float,
        var intensity: Float,
        var thickness: Float,
        val initialThickness: Float,
        val color: Int,
        val distortion: Float,
        val distortionFreq: Int,
        var lifespan: Float,
        val maxLifespan: Float,
        val style: Int = 0 // 0=wireframe, 1=fill, 2=double layer
    )
    
    /**
     * lightning data class
     */
    private data class Lightning(
        val startX: Float,
        val startY: Float,
        val segments: List<LightningSegment>,
        var intensity: Float,
        var lifespan: Float,
        val maxLifespan: Float,
        val color: Int
    )
    
    /**
     * lightning segment data class
     */
    private data class LightningSegment(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val thickness: Float,
        val childSegments: List<LightningSegment>
    )
    
    /**
     * visual echo data class
     */
    private data class VisualEcho(
        var opacity: Float,
        var lifespan: Float,
        val maxLifespan: Float,
        val offsetX: Float,
        val offsetY: Float
    )
    
    /**
     * background glow data class
     */
    private data class BackgroundGlow(
        val centerX: Float,
        val centerY: Float,
        val size: Float,
        val color: Int,
        val baseOpacity: Float = 0.2f,
        var opacity: Float = 0.2f,
        var lifespan: Float,
        val maxLifespan: Float,
        val pulseRate: Float
    )
    
    /**
     * flashing dot data class
     */
    private data class FlashingDot(
        val x: Float,
        val y: Float,
        val size: Float,
        val color: Int,
        val baseOpacity: Float = 1.0f,
        var currentOpacity: Float = 1.0f,
        var lifespan: Float,
        val maxLifespan: Float,
        val flickerRate: Float
    )
    
    /**
     * energy ray data class
     */
    private data class EnergyRay(
        val startX: Float,
        val startY: Float,
        val angle: Float,
        val length: Float,
        val width: Float,
        val color: Int,
        val baseOpacity: Float = 1.0f,
        var opacity: Float = 1.0f,
        var lifespan: Float,
        val maxLifespan: Float,
        val waveFactor: Float,
        val waveFrequency: Float
    )
    
    /**
     * ripple effect data class
     */
    private data class RippleEffect(
        val centerX: Float,
        val centerY: Float,
        var radius: Float,
        val maxRadius: Float,
        val expansionSpeed: Float,
        val color: Int,
        val baseOpacity: Float = 1.0f,
        var opacity: Float = 1.0f,
        var lifespan: Float,
        val maxLifespan: Float,
        val thickness: Float,
        val waveCount: Int
    )
    
    /**
     * quantum collapse effect data class
     */
    private data class QuantumCollapseEffect(
        val centerX: Float,
        val centerY: Float,
        var radius: Float,
        val maxRadius: Float,
        val expansionSpeed: Float,
        val lineCount: Int,
        val rotationSpeed: Float,
        var rotation: Float = 0f,
        val color: Int,
        val baseOpacity: Float = 1.0f,
        var opacity: Float = 1.0f,
        var lifespan: Float,
        val maxLifespan: Float,
        val thickness: Float,
        val distortion: Float
    )
}
