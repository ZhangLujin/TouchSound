package com.example.musicvibrationapp.view.spectrum.renderers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.musicvibrationapp.view.EmotionColorManager
import com.example.musicvibrationapp.view.SpectrumParameters
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow
import kotlin.random.Random

/**
 * Particle Renderer for Joyful Emotions
 * Expression of joyful emotions using enhanced particle system with integrated SpectrumParameters parameter system
 */
class JoyParticleRenderer(context: Context) : BaseEmotionRenderer(context) {
    // particle system
    private val particles = mutableListOf<Particle>()
    private val particlePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    // burst particles
    private val burstParticles = mutableListOf<BurstParticle>()
    private val burstPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    /**
     * process spectrum data
     */
    override fun onProcessData(processedData: FloatArray): FloatArray {
        // update existing particles
        updateParticles(deltaTime)
        
        // generate new particles based on spectrum energy
        generateParticles(processedData, isRhythmPeak)
        
        // improved rhythm burst detection 
        if (isRhythmPeak || (totalEnergy > 0.6f && volumeChange > 0.15f)) {
            // strong rhythm burst
            generateBurst(totalEnergy, volumeChange, 3)  // intensity level 3
        } 
        else if (totalEnergy > 0.5f && volumeChange > 0.1f) {  // medium rhythm condition
            // medium rhythm burst
            generateBurst(totalEnergy, volumeChange, 2)  // intensity level 2
        }
        else if (totalEnergy > 0.4f && volumeChange > 0.05f) {  // mild rhythm condition
            // mild rhythm burst
            generateBurst(totalEnergy, volumeChange, 1)  // intensity level 1
        }
        // even if not a obvious rhythm peak, there is a small chance to generate a weak burst, increasing visual richness
        else if (totalEnergy > 0.35f && energyConcentration > 0.4f && Random.nextFloat() < 0.25f) {
            generateBurst(totalEnergy, volumeChange, 0)  // weak burst
        }
        
        return processedData
    }
    
    /**
     * render spectrum
     */
    override fun onRender(canvas: Canvas, data: FloatArray, width: Int, height: Int, isVertical: Boolean) {
        val maxWidth = width.toFloat()
        val maxHeight = height.toFloat()
        
        // draw flash effect - modified to flash only when rhythm peak, not filling the entire background
        if (flashIntensity > 0.01f) {
            val flashPaint = Paint().apply {
                color = Color.argb(
                    (flashIntensity * 180).toInt().coerceIn(0, 180), // reduce maximum opacity, avoid completely covering the background
                    255, 255, 255
                )
                style = Paint.Style.FILL
            }
            
            // create flash effect only around burst particles, not the entire canvas
            burstParticles.forEach { particle ->
                val x = if (isVertical) particle.y * maxWidth else particle.x * maxWidth
                val y = if (isVertical) particle.x * maxHeight else particle.y * maxHeight
                
                // create flash effect only around burst particles, not the entire canvas
                if (particle.size > 0.8f && particle.lifespan > particle.maxLifespan * 0.7f) {
                    val flashRadius = particle.size * 30f * flashIntensity
                    flashPaint.shader = RadialGradient(
                        x, y, flashRadius,
                        Color.argb(
                            (flashIntensity * 180).toInt().coerceIn(0, 180),
                            255, 255, 255
                        ),
                        Color.TRANSPARENT,
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawCircle(x, y, flashRadius, flashPaint)
                    flashPaint.shader = null
                }
            }
        }
        
        // draw particles
        drawParticles(canvas, maxWidth, maxHeight, isVertical)
        
        // draw burst particles
        drawBurstParticles(canvas, maxWidth, maxHeight, isVertical)
    }
    
    private fun drawParticles(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        // get emotion colors
        val baseColors = getEmotionGradientColors(height, width)
        val particleColor = if (baseColors.size > 2) baseColors[2] else Color.YELLOW
        
        // ensure base color in yellow range
        val safeParticleColor = ensureYellowColor(particleColor)
        
        // create more subtle complementary color - strictly limited to yellow range
        val complementaryColor = adjustComplementaryColor(safeParticleColor)
        
        particles.forEach { particle ->
            // adjust coordinates based on direction
            val x = if (isVertical) particle.y * width else particle.x * width
            val y = if (isVertical) particle.x * height else particle.y * height
            
            // use fixed base size, can adjust with other parameters like fallSpeed
            val baseSize = 20f + SpectrumParameters.fallSpeed.value * 50f
            val radius = particle.size * baseSize
            
            // create core color - more saturated, warmer tone, ensure in yellow range
            val coreColor = enhanceColor(safeParticleColor, 1.3f, 0.05f)
            
            // create multi-layer radial gradient - from inside to outside, all colors in yellow range
            val colors = intArrayOf(
                Color.argb(
                    (particle.alpha * 255).toInt().coerceIn(0, 255),
                    Color.red(coreColor),
                    Color.green(coreColor),
                    Color.blue(coreColor)
                ),
                Color.argb(
                    (particle.alpha * 220).toInt().coerceIn(0, 255),
                    Color.red(safeParticleColor),
                    Color.green(safeParticleColor),
                    Color.blue(safeParticleColor)
                ),
                Color.argb(
                    (particle.alpha * 180).toInt().coerceIn(0, 255),
                    Color.red(complementaryColor),
                    Color.green(complementaryColor),
                    Color.blue(complementaryColor)
                ),
                Color.TRANSPARENT
            )
            
            val positions = floatArrayOf(0f, 0.4f, 0.7f, 1.0f)
            
            val particleGradient = RadialGradient(
                x, y,
                radius,
                colors,
                positions,
                Shader.TileMode.CLAMP
            )
            
            particlePaint.shader = particleGradient
            canvas.drawCircle(x, y, radius, particlePaint)
            
            // add glow effect - only large particles have glow
            if (particle.size > 0.3f) {
                val glowRadius = radius * 1.5f
                val glowGradient = RadialGradient(
                    x, y,
                    glowRadius,
                    Color.argb(
                        (particle.alpha * 100).toInt().coerceIn(0, 255),
                        Color.red(coreColor),
                        Color.green(coreColor),
                        Color.blue(coreColor)
                    ),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                
                particlePaint.shader = glowGradient
                canvas.drawCircle(x, y, glowRadius, particlePaint)
            }
        }
    }
    
    private fun drawBurstParticles(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        // get emotion colors
        val baseColors = getEmotionGradientColors(height, width)
        val burstColor = if (baseColors.size > 2) baseColors[2] else Color.YELLOW
        
        // ensure base color in yellow range
        val safeBurstColor = ensureYellowColor(burstColor)
        
        // create more subtle complementary color - strictly limited to yellow range
        val complementaryColor = adjustComplementaryColor(safeBurstColor)
        
        burstParticles.forEach { particle ->
            // adjust coordinates based on direction
            val x = if (isVertical) particle.y * width else particle.x * width
            val y = if (isVertical) particle.x * height else particle.y * height
            
            // use fixed base size, can adjust with other parameters like fallSpeed
            val baseSize = 10f + SpectrumParameters.fallSpeed.value * 25f
            val radius = particle.size * baseSize
            
            // create core color - more saturated, warmer tone, ensure in yellow range
            val coreColor = enhanceColor(safeBurstColor, 1.3f, 0.05f)
            
            // create multi-layer radial gradient
            val colors = intArrayOf(
                Color.argb(
                    (particle.alpha * 255).toInt().coerceIn(0, 255),
                    Color.red(coreColor),
                    Color.green(coreColor),
                    Color.blue(coreColor)
                ),
                Color.argb(
                    (particle.alpha * 220).toInt().coerceIn(0, 255),
                    Color.red(safeBurstColor),
                    Color.green(safeBurstColor),
                    Color.blue(safeBurstColor)
                ),
                Color.argb(
                    (particle.alpha * 180).toInt().coerceIn(0, 255),
                    Color.red(complementaryColor),
                    Color.green(complementaryColor),
                    Color.blue(complementaryColor)
                ),
                Color.TRANSPARENT
            )
            
            val positions = floatArrayOf(0f, 0.4f, 0.7f, 1.0f)
            
            burstPaint.shader = RadialGradient(
                x, y,
                radius,
                colors,
                positions,
                Shader.TileMode.CLAMP
            )
            
            canvas.drawCircle(x, y, radius, burstPaint)
            
            // glow effect
            val glowGradient = RadialGradient(
                x, y,
                radius * 2f,
                Color.argb(
                    (particle.alpha * 100).toInt().coerceIn(0, 255),
                    Color.red(coreColor),
                    Color.green(coreColor),
                    Color.blue(coreColor)
                ),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            
            burstPaint.shader = glowGradient
            canvas.drawCircle(x, y, radius * 2f, burstPaint)
            burstPaint.shader = null
        }
    }
    
    private fun updateParticles(deltaTime: Float) {
        val fallSpeed = SpectrumParameters.fallSpeed.value
        val minFallSpeed = SpectrumParameters.minFallSpeed.value
        
        // update normal particles
        particles.removeAll { particle ->
            // apply lifespan decay - use fallSpeed parameter
            particle.lifespan -= deltaTime * (1.0f + fallSpeed * 5.0f)
            
            // get original speed
            var newVx = particle.vx
            var newVy = particle.vy
            
            // apply music force field - create overall rhythm感
            val (fieldVx, fieldVy) = applyMusicForceField(
                particle.x, particle.y, newVx, newVy
            )
            newVx = fieldVx
            newVy = fieldVy
            
            // apply rhythm response effect - increase pulse effect on force field basis
            val (pulsedVx, pulsedVy) = applyRhythmResponseToVelocity(
                newVx, newVy, 
                pulseWeight = 0.25f,  // increase from 0.2f to 0.25f
                flowWeight = 0.6f     // increase from 0.5f to 0.6f
            )
            newVx = pulsedVx
            newVy = pulsedVy
            
            // update position - use applied force field and rhythm response speed
            particle.x += newVx * deltaTime
            particle.y += newVy * deltaTime
            
            // add falling effect - use fallSpeed and minFallSpeed
            particle.vy += (fallSpeed * 0.5f + minFallSpeed) * deltaTime
            
            // add slight rotation movement - use soloResponseStrength to affect rotation intensity, specify Float type
            val rotationSpeed = 0.5f * SpectrumParameters.soloResponseStrength.value * 3.0f
            val angle = rotationSpeed * deltaTime
            val centerX = 0.5f
            val centerY = 0.5f
            val dx = particle.x - centerX
            val dy = particle.y - centerY
            
            // apply rotation - only apply to some particles
            if (Random.nextFloat() < 0.3f) {
                val newDx = dx * cos(angle) - dy * sin(angle)
                val newDy = dx * sin(angle) + dy * cos(angle)
                particle.x = centerX + newDx
                particle.y = centerY + newDy
            }
            
            // add slight random movement - use melSensitivity to affect randomness
            val randomFactor = SpectrumParameters.melSensitivity.value
            if (Random.nextFloat() < 0.2f * randomFactor) {
                particle.vx += (Random.nextFloat() - 0.5f) * 0.1f * randomFactor
                particle.vy += (Random.nextFloat() - 0.5f) * 0.1f * randomFactor
            }
            
            // limit speed
            val maxSpeed = 0.5f
            val speed = kotlin.math.sqrt(particle.vx * particle.vx + particle.vy * particle.vy)
            if (speed > maxSpeed) {
                particle.vx = particle.vx / speed * maxSpeed
                particle.vy = particle.vy / speed * maxSpeed
            }
            
            // update transparency - use nonlinear decay, start slow, end fast
            val lifeFraction = (particle.lifespan / particle.maxLifespan).coerceIn(0f, 1f)
            particle.alpha = if (lifeFraction > 0.7f) {
                lifeFraction
            } else {
                lifeFraction * lifeFraction / 0.7f
            }
            
            // particle disappearance condition
            particle.lifespan <= 0 || particle.x < -0.1f || particle.x > 1.1f || particle.y < -0.1f || particle.y > 1.1f
        }
        
        // update burst particles
        burstParticles.removeAll { particle ->
            particle.lifespan -= deltaTime * (1.0f + fallSpeed * 3.0f)
            
            // get original speed
            var newVx = particle.vx
            var newVy = particle.vy
            
            // apply music force field - burst particles also affected by force field
            val fieldFactor = if (particle.size > 1.0f) 0.35f else 0.8f  // increase from 0.3/0.7 to 0.35/0.8
            val (fieldVx, fieldVy) = applyMusicForceField(
                particle.x, particle.y, newVx, newVy
            )
            newVx = newVx * (1f - fieldFactor) + fieldVx * fieldFactor
            newVy = newVy * (1f - fieldFactor) + fieldVy * fieldFactor
            
            // apply rhythm response effect
            val pulseWeight = if (particle.size > 1.0f) 0.55f else 0.35f  // increase from 0.5/0.3 to 0.55/0.35
            val flowWeight = if (particle.size > 1.0f) 0.25f else 0.45f   // increase from 0.2/0.4 to 0.25/0.45
            val directionFactor = if (particle.size > 1.0f) 1.3f else 1.1f  // increase from 1.2/1.0 to 1.3/1.1
            
            val (pulsedVx, pulsedVy) = applyRhythmResponseToVelocity(
                newVx, newVy,
                pulseWeight = pulseWeight,
                flowWeight = flowWeight,
                directionFactor = directionFactor
            )
            newVx = pulsedVx
            newVy = pulsedVy
            
            // update position
            particle.x += newVx * deltaTime
            particle.y += newVy * deltaTime
            
            // nonlinear transparency decay - large particles use different decay curve
            val lifeFraction = (particle.lifespan / particle.maxLifespan).coerceIn(0f, 1f)
            particle.alpha = if (particle.size > 1.0f) {
                // large particles appear quickly, disappear slowly
                if (lifeFraction > 0.7f) {
                    1.0f
                } else {
                    (lifeFraction / 0.7f).pow(0.5f)
                }
            } else {
                // normal particles linear decay
                lifeFraction
            }
            
            // large particles keep size, small particles gradually shrink
            if (particle.size <= 1.0f) {
                particle.size *= 0.97f - fallSpeed * 0.1f  // use fallSpeed to affect shrinkage speed
            }
            
            // add slight random movement - use melSensitivity
            val randomFactor = SpectrumParameters.melSensitivity.value
            if (Random.nextFloat() < 0.3f * randomFactor) {
                particle.vx += (Random.nextFloat() - 0.5f) * 0.2f * randomFactor
                particle.vy += (Random.nextFloat() - 0.5f) * 0.2f * randomFactor
            }
            
            particle.lifespan <= 0
        }
    }
    
    private fun generateParticles(data: FloatArray, isRhythmPeak: Boolean) {
        // use minThreshold and soloResponseStrength to affect particle generation
        val minThreshold = SpectrumParameters.minThreshold.value
        val soloResponseStrength = SpectrumParameters.soloResponseStrength.value
        
        // limit particle count - use melSensitivity to affect maximum particle count
        val maxParticles = (200 + (SpectrumParameters.melSensitivity.value * 300).toInt())
        if (particles.size >= maxParticles) return
        
        // increase particle generation probability and speed when rhythm peak, increase multiplier
        val rhythmMultiplier = if (isRhythmPeak) 3.5f else 1.0f
        val volumeInfluence = currentVolume * 1.8f + 0.5f
        
        // generate particles based on spectrum energy
        data.forEachIndexed { index, magnitude ->
            // use minThreshold as generation threshold, but reduce threshold to make particles easier to generate
            val dynamicThreshold = minThreshold * (if (isRhythmPeak) 0.45f else 0.9f)  // reduce threshold
            
            // increase generation probability
            val generationProbability = magnitude * 0.85f * rhythmMultiplier * volumeInfluence  // increase from 0.8 to 0.85
            
            if (magnitude > dynamicThreshold && Random.nextFloat() < generationProbability) {
                // generate particles of different sizes based on frequency index
                val particleSize = when {
                    index < data.size * 0.2 -> 0.5f + Random.nextFloat() * 0.5f  // low frequency - large particles
                    index < data.size * 0.6 -> 0.3f + Random.nextFloat() * 0.3f  // medium frequency - medium particles
                    else -> 0.1f + Random.nextFloat() * 0.2f  // high frequency - small particles
                }
                
                val energyInfluence = magnitude * 3f + 0.1f
                val speedMultiplier = if (isRhythmPeak) 1.5f else 1.0f
                
                // particles generate from bottom or center of screen
                val originY = if (Random.nextFloat() < 0.6f) 
                    0.9f + Random.nextFloat() * 0.2f  // 60% from bottom
                else 
                    0.4f + Random.nextFloat() * 0.2f  // 40% from center
                
                // X coordinate random distribution
                val originX = Random.nextFloat()
                
                // initial speed - use energyInfluence and soloResponseStrength to affect speed
                val baseVx = (Random.nextFloat() - 0.5f) * 0.1f * energyInfluence
                val baseVy = -0.2f * energyInfluence * speedMultiplier  
                
                val finalVx = baseVx * (1f + soloResponseStrength * 2f)
                val finalVy = baseVy * (1f + soloResponseStrength * 2f)
                
                val lifespan = 1.0f + Random.nextFloat() * 1.0f + magnitude * 2.0f
                
                // create new particle
                particles.add(Particle(
                    x = originX,
                    y = originY,
                    vx = finalVx,
                    vy = finalVy,
                    size = particleSize,
                    lifespan = lifespan,
                    maxLifespan = lifespan,
                    hueOffset = Random.nextFloat(),
                    alpha = 1.0f
                ))
            }
        }
    }
    
    private fun generateBurst(energy: Float, volumeChange: Float, intensityLevel: Int = 1) {
        // use soloResponseStrength to affect burst effect
        val responseStrength = SpectrumParameters.soloResponseStrength.value
        
        // generate position - burst position random distribution, but closer to center
        val burstX = 0.3f + Random.nextFloat() * 0.4f  // x: 0.3-0.7
        val burstY = 0.3f + Random.nextFloat() * 0.4f  // y: 0.3-0.7
        
        // adjust particle count based on intensity level, enhance distinction
        val baseCount = when (intensityLevel) {
            3 -> (energy * 70).toInt().coerceIn(40, 90)  // strong rhythm - more particles
            2 -> (energy * 45).toInt().coerceIn(25, 55)  // medium rhythm
            1 -> (energy * 25).toInt().coerceIn(15, 30)  // weak rhythm
            else -> (energy * 12).toInt().coerceIn(6, 15)  // weak burst - fewer particles
        }
        
        val volumeBoost = when (intensityLevel) {
            3 -> (volumeChange * 150).toInt().coerceIn(10, 60)  // strong rhythm - more impact
            2 -> (volumeChange * 120).toInt().coerceIn(5, 40)   // medium rhythm
            1 -> (volumeChange * 80).toInt().coerceIn(0, 25)    // weak rhythm
            else -> (volumeChange * 40).toInt().coerceIn(0, 10) // weak burst
        }
        
        val particleCount = baseCount + volumeBoost
        
        // create burst effect
        repeat(particleCount) {
            // adjust angle distribution based on intensity level, enhance distinction
            val angle = when (intensityLevel) {
                3 -> {
                    Random.nextFloat() * Math.PI * 0.5f - Math.PI * 0.25f + Math.PI * 1.5f
                }
                2 -> {
                    Random.nextFloat() * Math.PI * 0.8f - Math.PI * 0.4f + Math.PI * 1.5f
                }
                1 -> {
                    Random.nextFloat() * Math.PI * 1.2f - Math.PI * 0.6f + Math.PI * 1.5f
                }
                else -> {
                    Random.nextFloat() * Math.PI * 1.8f - Math.PI * 0.9f + Math.PI * 1.5f
                }
            }
            
            // adjust speed based on intensity level, enhance distinction
            val baseSpeed = when (intensityLevel) {
                3 -> 0.8f + Random.nextFloat() * 0.8f  // strong rhythm - high speed
                2 -> 0.6f + Random.nextFloat() * 0.5f  // medium rhythm
                1 -> 0.4f + Random.nextFloat() * 0.4f  // weak rhythm
                else -> 0.2f + Random.nextFloat() * 0.3f  // weak burst - low speed
            }
            
            val responseMultiplier = when (intensityLevel) {
                3 -> 3.0f  // strong rhythm - more intense
                2 -> 2.5f  // medium rhythm
                1 -> 2.0f  // weak rhythm
                else -> 1.5f  // weak burst
            }
            
            val speed = baseSpeed * (1.0f + responseStrength * responseMultiplier)
            
            // adjust particle size based on intensity level, increase large particle ratio
            val size = when (intensityLevel) {
                3 -> {
                    if (Random.nextFloat() < 0.5f) {  // increase to 50% probability
                        // generate large particles
                        0.7f + Random.nextFloat() * 0.6f
                    } else {
                        // generate medium particles
                        0.4f + Random.nextFloat() * 0.3f
                    }
                }
                2 -> {
                    if (Random.nextFloat() < 0.4f) {  // increase to 40% probability
                        // generate large particles
                        0.6f + Random.nextFloat() * 0.4f
                    } else {
                        // generate medium particles
                        0.3f + Random.nextFloat() * 0.3f
                    }
                }
                else -> {
                    // weak rhythm - smaller particles
                    0.2f + Random.nextFloat() * 0.3f
                }
            }
            
            // adjust lifespan based on intensity level, reduce lifespan to make effect more concentrated
            val lifespan = when (intensityLevel) {
                3 -> {
                    if (Random.nextFloat() < 0.3f) {
                        // 30% probability generate long lifespan particles
                        1.2f + Random.nextFloat() * 0.6f
                    } else {
                        // 70% probability generate standard lifespan particles
                        0.7f + Random.nextFloat() * 0.5f
                    }
                }
                2 -> {
                    if (Random.nextFloat() < 0.2f) {
                        // 20% probability generate long lifespan particles
                        1.0f + Random.nextFloat() * 0.5f
                    } else {
                        // 80% probability generate standard lifespan particles
                        0.6f + Random.nextFloat() * 0.4f
                    }
                }
                else -> {
                    // weak rhythm - standard lifespan
                    0.5f + Random.nextFloat() * 0.5f
                }
            }
            
            burstParticles.add(BurstParticle(
                x = burstX,
                y = burstY,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed).toFloat(),
                size = size,
                lifespan = lifespan,
                maxLifespan = lifespan,
                colorVariation = Random.nextFloat(),
                alpha = when (intensityLevel) {
                    3 -> 1.5f  // strong rhythm - brighter
                    2 -> 1.3f  // medium rhythm
                    else -> 1.1f  // weak rhythm
                }
            ))
        }
        
        // add extra "shockwave" effect when strong rhythm
        if (intensityLevel >= 2) {
            // create a large, quickly disappearing halo particle
            val shockwaveSize = when (intensityLevel) {
                3 -> 2.0f + Random.nextFloat() * 0.5f  // strong rhythm - larger shockwave
                else -> 1.5f + Random.nextFloat() * 0.5f  // medium rhythm
            }
            
            burstParticles.add(BurstParticle(
                x = burstX,
                y = burstY,
                vx = 0f,
                vy = 0f,
                size = shockwaveSize,
                lifespan = 0.3f + Random.nextFloat() * 0.2f,  // shorter lifespan makes effect more concentrated
                maxLifespan = 0.5f,
                colorVariation = Random.nextFloat() * 0.2f,
                alpha = 1.0f
            ))
            
            // add multiple shockwaves when strong rhythm, forming a continuous effect
            if (intensityLevel == 3) {
                // add second shockwave, slightly delayed
                burstParticles.add(BurstParticle(
                    x = burstX + (Random.nextFloat() - 0.5f) * 0.1f,  // slightly offset position
                    y = burstY + (Random.nextFloat() - 0.5f) * 0.1f,
                    vx = 0f,
                    vy = 0f,
                    size = 1.8f + Random.nextFloat() * 0.4f,  // smaller shockwave
                    lifespan = 0.4f + Random.nextFloat() * 0.2f,  // longer lifespan, forming delay effect
                    maxLifespan = 0.6f,
                    colorVariation = Random.nextFloat() * 0.2f,
                    alpha = 0.9f  // lower transparency
                ))
                
                // add third shockwave, larger delay
                burstParticles.add(BurstParticle(
                    x = burstX + (Random.nextFloat() - 0.5f) * 0.15f,  // more offset position
                    y = burstY + (Random.nextFloat() - 0.5f) * 0.15f,
                    vx = 0f,
                    vy = 0f,
                    size = 1.5f + Random.nextFloat() * 0.3f,  // smaller shockwave
                    lifespan = 0.5f + Random.nextFloat() * 0.2f,  // longer lifespan
                    maxLifespan = 0.7f,
                    colorVariation = Random.nextFloat() * 0.2f,
                    alpha = 0.8f  // lower transparency
                ))
            }
        }
    }
    
    // particle data class
    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val size: Float,
        var lifespan: Float,
        val maxLifespan: Float,
        val hueOffset: Float,
        var alpha: Float = 1f
    )
    
    // burst particle data class
    private data class BurstParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var lifespan: Float,
        val maxLifespan: Float,
        val colorVariation: Float,
        var alpha: Float = 1f
    )
    
    /**
     * ensure color in yellow range - completely eliminate the possibility of cyan
     */
    private fun ensureYellowColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        
        // strictly limit in yellow range (45-75 degrees)
        hsv[0] = hsv[0].coerceIn(45f, 75f)
        
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
    
    /**
     * create harmonious complementary color - strictly limit in yellow theme range
     */
    private fun adjustComplementaryColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        
        // force in yellow range (50-70 degrees), narrower range to ensure safety
        hsv[0] = 50f + Random.nextFloat() * 20f  // 50-70 degrees, standard yellow is 60 degrees
        
        // adjust saturation and brightness, create deep and shallow changes but keep yellow theme
        hsv[1] = (hsv[1] * (0.85f + Random.nextFloat() * 0.15f)).coerceIn(0.7f, 1.0f)
        hsv[2] = (hsv[2] * (0.85f + Random.nextFloat() * 0.15f)).coerceIn(0.6f, 0.95f)
        
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
    
    /**
     * enhance color saturation and contrast - stay in yellow theme range
     */
    private fun enhanceColor(color: Int, saturationFactor: Float, brightnessAdjust: Float = 0f): Int {
        // convert to HSV
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        
        // ensure hue in narrower yellow range (50-70 degrees)
        hsv[0] = hsv[0].coerceIn(50f, 70f)
        
        // enhance saturation, but ensure not over saturated
        hsv[1] = (hsv[1] * saturationFactor).coerceIn(0.7f, 1.0f)
        
        // adjust brightness, ensure enough brightness but not too bright
        hsv[2] = (hsv[2] + brightnessAdjust).coerceIn(0.7f, 0.95f)
        
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
}