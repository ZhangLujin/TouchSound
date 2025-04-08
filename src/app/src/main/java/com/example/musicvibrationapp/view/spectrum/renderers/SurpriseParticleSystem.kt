package com.example.musicvibrationapp.view.spectrum.renderers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Surprise emotion particle system
 * Based on JoyParticleRenderer's particle system, but adjusted for visual effects suitable for surprise emotion
 */
class SurpriseParticleSystem {
    // Particle system
    private val particles = mutableListOf<Particle>()
    private val particlePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    // Burst particles
    private val burstParticles = mutableListOf<BurstParticle>()
    private val burstPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    // Audio analysis status
    private var currentVolume = 0f
    private var isRhythmPeak = false
    private var totalEnergy = 0f
    private var energyConcentration = 0f
    private var rhythmIntensity = 0
    
    // Music force field system
    private var musicForceFieldStrength: Float = 0f
    private var forceFieldDirection: Float = 0f
    
    /**
     * Process spectrum data and update particle system
     */
    fun processData(
        processedData: FloatArray, 
        deltaTime: Float, 
        currentVolume: Float,
        isRhythmPeak: Boolean,
        totalEnergy: Float,
        energyConcentration: Float,
        rhythmIntensity: Int,
        musicForceFieldStrength: Float,
        forceFieldDirection: Float,
        fallSpeed: Float,
        minFallSpeed: Float,
        melSensitivity: Float,
        soloResponseStrength: Float,
        minThreshold: Float
    ) {
        // Update status
        this.currentVolume = currentVolume
        this.isRhythmPeak = isRhythmPeak
        this.totalEnergy = totalEnergy
        this.energyConcentration = energyConcentration
        this.rhythmIntensity = rhythmIntensity
        this.musicForceFieldStrength = musicForceFieldStrength
        this.forceFieldDirection = forceFieldDirection
        
        // Update existing particles
        updateParticles(deltaTime, fallSpeed, minFallSpeed, melSensitivity, soloResponseStrength)
        
        // Reduce particle generation rate - only generate when volume is sufficient
        if (currentVolume > 0.05f) {
            // Generate new particles based on spectrum energy, but reduce generation probability
            generateParticles(processedData, isRhythmPeak, minThreshold * 1.5f, soloResponseStrength)
        }
        
        // Significantly reduce the trigger conditions and quantity of burst particles
        if (isRhythmPeak && totalEnergy > 0.7f && volumeChange(deltaTime) > 0.2f) {  // Stricter conditions for strong rhythm
            // Strong rhythm burst, but reduce quantity
            generateBurst(totalEnergy * 0.6f, volumeChange(deltaTime) * 0.5f, 3, soloResponseStrength)
        } 
        else if (totalEnergy > 0.6f && volumeChange(deltaTime) > 0.15f && Random.nextFloat() < 0.5f) {  // Increase randomness, reduce trigger frequency
            // Medium rhythm burst
            generateBurst(totalEnergy * 0.5f, volumeChange(deltaTime) * 0.4f, 2, soloResponseStrength)
        }
        // Remove generation of light rhythm and weak bursts, reduce the number of particles on screen
    }
    
    /**
     * Simulate volume change - since we don't have the volume of the previous frame, simple estimation here
     */
    private fun volumeChange(deltaTime: Float): Float {
        // Estimate volume change based on total energy and rhythm intensity
        return when (rhythmIntensity) {
            3 -> 0.2f * totalEnergy  // Strong rhythm
            2 -> 0.15f * totalEnergy  // Medium rhythm
            1 -> 0.1f * totalEnergy  // Light rhythm
            else -> 0.05f * totalEnergy  // No rhythm
        }
    }
    
    /**
     * Render particle system
     */
    fun render(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        // Draw particles
        drawParticles(canvas, width, height, isVertical)
        
        // Draw burst particles
        drawBurstParticles(canvas, width, height, isVertical)
    }
    
    /**
     * Draw basic particles
     */
    private fun drawParticles(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        particles.forEach { particle ->
            // Adjust coordinates based on direction
            val x = if (isVertical) particle.y * width else particle.x * width
            val y = if (isVertical) particle.x * height else particle.y * height
            
            // Reduce base size
            val baseSize = 10f + particle.size * 10f  // Reduced from 15f+15f to 10f+10f
            val radius = particle.size * baseSize
            
            // Adjust color to match SurpriseFlashRenderer's style
            val coreColor = createSurpriseColor(particle.intensity, true)
            val outerColor = createSurpriseColor(particle.intensity * 0.7f, false)
            
            // Reduce transparency
            val adjustedAlpha = particle.alpha * 0.7f  // Reduced transparency
            
            // Create multi-level radial gradient
            val colors = intArrayOf(
                Color.argb(
                    (adjustedAlpha * 255).toInt().coerceIn(0, 255),
                    Color.red(coreColor),
                    Color.green(coreColor),
                    Color.blue(coreColor)
                ),
                Color.argb(
                    (adjustedAlpha * 180).toInt().coerceIn(0, 255),  // Reduced from 220 to 180
                    Color.red(outerColor),
                    Color.green(outerColor),
                    Color.blue(outerColor)
                ),
                Color.TRANSPARENT
            )
            
            val positions = floatArrayOf(0f, 0.5f, 1.0f)  // Changed from 0.6f to 0.5f, making particle edges softer
            
            val particleGradient = RadialGradient(
                x, y,
                radius,
                colors,
                positions,
                Shader.TileMode.CLAMP
            )
            
            particlePaint.shader = particleGradient
            canvas.drawCircle(x, y, radius, particlePaint)
            
            // Only add glow for larger particles, and reduce glow intensity
            if (particle.size > 0.4f) {  // Increased from 0.3f to 0.4f
                val glowRadius = radius * 1.3f  // Reduced from 1.5f to 1.3f
                val glowGradient = RadialGradient(
                    x, y,
                    glowRadius,
                    Color.argb(
                        (adjustedAlpha * 70).toInt().coerceIn(0, 255),  // Reduced from 100 to 70
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
            
            // Clear shader
            particlePaint.shader = null
        }
    }
    
    /**
     * Draw burst particles
     */
    private fun drawBurstParticles(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        burstParticles.forEach { particle ->
            // Adjust coordinates based on direction
            val x = if (isVertical) particle.y * width else particle.x * width
            val y = if (isVertical) particle.x * height else particle.y * height
            
            // Base size
            val baseSize = 10f + particle.size * 20f
            val radius = particle.size * baseSize
            
            // Create core color - burst particles use brighter colors
            val coreColor = createSurpriseColor(1.0f, true)
            val outerColor = createSurpriseColor(0.8f, false)
            
            // Create multi-level radial gradient
            val colors = intArrayOf(
                Color.argb(
                    (particle.alpha * 255).toInt().coerceIn(0, 255),
                    Color.red(coreColor),
                    Color.green(coreColor),
                    Color.blue(coreColor)
                ),
                Color.argb(
                    (particle.alpha * 220).toInt().coerceIn(0, 255),
                    Color.red(outerColor),
                    Color.green(outerColor),
                    Color.blue(outerColor)
                ),
                Color.TRANSPARENT
            )
            
            val positions = floatArrayOf(0f, 0.5f, 1.0f)
            
            burstPaint.shader = RadialGradient(
                x, y,
                radius,
                colors,
                positions,
                Shader.TileMode.CLAMP
            )
            
            canvas.drawCircle(x, y, radius, burstPaint)
            
            // Glow effect
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
    
    /**
     * Update particle states
     */
    private fun updateParticles(
        deltaTime: Float, 
        fallSpeed: Float, 
        minFallSpeed: Float, 
        melSensitivity: Float,
        soloResponseStrength: Float
    ) {
        // Update regular particles
        particles.removeAll { particle ->
            // Apply lifecycle decay
            particle.lifespan -= deltaTime * (1.0f + fallSpeed * 5.0f)
            
            // Get original velocity
            var newVx = particle.vx
            var newVy = particle.vy
            
            // Apply music force field - create overall rhythm feel
            val (fieldVx, fieldVy) = applyMusicForceField(
                particle.x, particle.y, newVx, newVy
            )
            newVx = fieldVx
            newVy = fieldVy
            
            // Update position
            particle.x += newVx * deltaTime
            particle.y += newVy * deltaTime
            
            // Add falling effect - surprise emotion particles are more upward or scattered
            if (Random.nextFloat() < 0.3f) {
                particle.vy -= (fallSpeed * 0.3f + minFallSpeed) * deltaTime
            } else {
                particle.vy += (fallSpeed * 0.2f + minFallSpeed) * deltaTime
            }
            
            // Add slight random motion - surprise emotion particles move more irregularly
            val randomFactor = melSensitivity * 1.5f
            if (Random.nextFloat() < 0.4f * randomFactor) {
                particle.vx += (Random.nextFloat() - 0.5f) * 0.15f * randomFactor
                particle.vy += (Random.nextFloat() - 0.5f) * 0.15f * randomFactor
            }
            
            // Limit velocity
            val maxSpeed = 0.6f
            val speed = sqrt(particle.vx * particle.vx + particle.vy * particle.vy)
            if (speed > maxSpeed) {
                particle.vx = particle.vx / speed * maxSpeed
                particle.vy = particle.vy / speed * maxSpeed
            }
            
            // Update transparency - use non-linear decay
            val lifeFraction = (particle.lifespan / particle.maxLifespan).coerceIn(0f, 1f)
            particle.alpha = if (lifeFraction > 0.7f) {
                lifeFraction
            } else {
                lifeFraction * lifeFraction / 0.7f
            }
            
            // Particle disappearance conditions
            particle.lifespan <= 0 || particle.x < -0.1f || particle.x > 1.1f || particle.y < -0.1f || particle.y > 1.1f
        }
        
        // Update burst particles
        burstParticles.removeAll { particle ->
            particle.lifespan -= deltaTime * (1.0f + fallSpeed * 3.0f)
            
            // Get original velocity
            var newVx = particle.vx
            var newVy = particle.vy
            
            // Apply music force field - burst particles are also affected by the force field
            val fieldFactor = if (particle.size > 1.0f) 0.35f else 0.8f
            val (fieldVx, fieldVy) = applyMusicForceField(
                particle.x, particle.y, newVx, newVy
            )
            newVx = newVx * (1f - fieldFactor) + fieldVx * fieldFactor
            newVy = newVy * (1f - fieldFactor) + fieldVy * fieldFactor
            
            // Update position
            particle.x += newVx * deltaTime
            particle.y += newVy * deltaTime
            
            // Non-linear transparency decay - large particles use different decay curve
            val lifeFraction = (particle.lifespan / particle.maxLifespan).coerceIn(0f, 1f)
            particle.alpha = if (particle.size > 1.0f) {
                // Large particles appear quickly, disappear slowly
                if (lifeFraction > 0.7f) {
                    1.0f
                } else {
                    (lifeFraction / 0.7f).pow(0.5f)
                }
            } else {
                // Regular particles linear decay
                lifeFraction
            }
            
            // Large particles maintain size, small particles gradually shrink
            if (particle.size <= 1.0f) {
                particle.size *= 0.97f - fallSpeed * 0.1f
            }
            
            // Add slight random motion
            val randomFactor = melSensitivity * 1.5f
            if (Random.nextFloat() < 0.3f * randomFactor) {
                particle.vx += (Random.nextFloat() - 0.5f) * 0.2f * randomFactor
                particle.vy += (Random.nextFloat() - 0.5f) * 0.2f * randomFactor
            }
            
            particle.lifespan <= 0
        }
    }
    
    /**
     * Generate basic particles
     */
    private fun generateParticles(
        data: FloatArray, 
        isRhythmPeak: Boolean, 
        minThreshold: Float,
        soloResponseStrength: Float
    ) {
        // Significantly reduce maximum particle count
        val maxParticles = 60  // Reduced from 150 to 60
        if (particles.size >= maxParticles) return
        
        // Reduce particle generation multiplier during rhythm peaks
        val rhythmMultiplier = if (isRhythmPeak) 2.0f else 0.8f  // Reduced from 3.5 to 2.0
        val volumeInfluence = currentVolume * 1.2f + 0.3f  // Reduced volume influence
        
        // Generate particles based on spectrum energy, but reduce generation probability
        data.forEachIndexed { index, magnitude ->
            // Increase generation threshold
            val dynamicThreshold = minThreshold * (if (isRhythmPeak) 0.7f else 1.2f)  // Increase threshold
            
            // Reduce generation probability
            val generationProbability = magnitude * 0.4f * rhythmMultiplier * volumeInfluence  // Reduced from 0.85 to 0.4
            
            if (magnitude > dynamicThreshold && Random.nextFloat() < generationProbability) {
                // Reduce particle size
                val particleSize = when {
                    index < data.size * 0.2 -> 0.3f + Random.nextFloat() * 0.3f  // Low frequency - Reduced from 0.5-1.0 to 0.3-0.6
                    index < data.size * 0.6 -> 0.2f + Random.nextFloat() * 0.2f  // Mid frequency - Reduced from 0.3-0.6 to 0.2-0.4
                    else -> 0.1f + Random.nextFloat() * 0.1f  // High frequency - Reduced from 0.1-0.3 to 0.1-0.2
                }
                
                // Reduce generation speed
                val energyInfluence = magnitude * 2f + 0.1f  // Reduced from 3f to 2f
                val speedMultiplier = if (isRhythmPeak) 1.2f else 0.8f  // Reduced from 1.5 to 1.2
                
                // Adjust particle generation position, more from bottom
                val originY = when {
                    Random.nextFloat() < 0.6f -> 0.9f + Random.nextFloat() * 0.2f  // 60% generate from bottom
                    Random.nextFloat() < 0.3f -> 0.1f - Random.nextFloat() * 0.2f  // 30% generate from top
                    else -> Random.nextFloat()  // 10% generate from random position
                }
                
                // X coordinate randomly distributed
                val originX = Random.nextFloat()
                
                // Reduce initial velocity
                val angle = Random.nextFloat() * Math.PI * 2
                val speed = (0.05f + Random.nextFloat() * 0.15f) * energyInfluence * speedMultiplier  // Reduce base speed
                
                val baseVx = cos(angle).toFloat() * speed
                val baseVy = sin(angle).toFloat() * speed
                
                val finalVx = baseVx * (1f + soloResponseStrength * 1.5f)  // Reduced from 2f to 1.5f
                val finalVy = baseVy * (1f + soloResponseStrength * 1.5f)
                
                // Reduce lifespan
                val lifespan = 0.6f + Random.nextFloat() * 0.6f + magnitude * 1.0f  // Reduced from 0.8-1.6+1.5m to 0.6-1.2+1.0m
                
                // Create new particle
                particles.add(Particle(
                    x = originX,
                    y = originY,
                    vx = finalVx,
                    vy = finalVy,
                    size = particleSize,
                    lifespan = lifespan,
                    maxLifespan = lifespan,
                    intensity = magnitude.coerceIn(0.2f, 0.8f),  // Reduce maximum intensity from 1.0 to 0.8
                    alpha = 0.7f  // Reduce initial transparency from 1.0 to 0.7
                ))
            }
        }
    }
    
    /**
     * Generate burst particles
     */
    private fun generateBurst(
        energy: Float, 
        volumeChange: Float, 
        intensityLevel: Int = 1,
        soloResponseStrength: Float
    ) {
        // Limit maximum burst particle count
        val maxBurstParticles = 100
        if (burstParticles.size >= maxBurstParticles) return
        
        // Generate position - burst positions randomly distributed, but more towards center area
        val burstX = 0.3f + Random.nextFloat() * 0.4f
        val burstY = 0.3f + Random.nextFloat() * 0.4f
        
        // Significantly reduce particle count
        val baseCount = when (intensityLevel) {
            3 -> (energy * 30).toInt().coerceIn(15, 40)  // Reduced from 70/40-90 to 30/15-40
            2 -> (energy * 20).toInt().coerceIn(10, 25)  // Reduced from 45/25-55 to 20/10-25
            1 -> (energy * 10).toInt().coerceIn(5, 15)   // Reduced from 25/15-30 to 10/5-15
            else -> (energy * 5).toInt().coerceIn(3, 8)  // Reduced from 12/6-15 to 5/3-8
        }
        
        // Reduce volume change influence on particle count
        val volumeBoost = when (intensityLevel) {
            3 -> (volumeChange * 60).toInt().coerceIn(5, 25)  // Reduced from 150/10-60 to 60/5-25
            2 -> (volumeChange * 40).toInt().coerceIn(3, 15)  // Reduced from 120/5-40 to 40/3-15
            1 -> (volumeChange * 20).toInt().coerceIn(0, 10)  // Reduced from 80/0-25 to 20/0-10
            else -> (volumeChange * 10).toInt().coerceIn(0, 5)  // Reduced from 40/0-10 to 10/0-5
        }
        
        val particleCount = baseCount + volumeBoost
        
        // Create burst effect, but reduce particle count
        repeat(particleCount) {
            // Random angle
            val angle = Random.nextFloat() * Math.PI * 2
            
            // Reduce speed
            val baseSpeed = when (intensityLevel) {
                3 -> 0.5f + Random.nextFloat() * 0.5f  // Reduced from 0.8-1.6 to 0.5-1.0
                2 -> 0.4f + Random.nextFloat() * 0.3f  // Reduced from 0.6-1.1 to 0.4-0.7
                1 -> 0.3f + Random.nextFloat() * 0.2f  // Reduced from 0.4-0.8 to 0.3-0.5
                else -> 0.1f + Random.nextFloat() * 0.2f  // Reduced from 0.2-0.5 to 0.1-0.3
            }
            
            // Reduce response strength influence
            val responseMultiplier = when (intensityLevel) {
                3 -> 2.0f  // Reduced from 3.0 to 2.0
                2 -> 1.5f  // Reduced from 2.5 to 1.5
                1 -> 1.2f  // Reduced from 2.0 to 1.2
                else -> 1.0f  // Reduced from 1.5 to 1.0
            }
            
            val speed = baseSpeed * (1.0f + soloResponseStrength * responseMultiplier)
            
            // Reduce particle size
            val size = when (intensityLevel) {
                3 -> {
                    if (Random.nextFloat() < 0.3f) {  // Reduced from 0.5 to 0.3, reduce large particle ratio
                        // Generate large particles, but reduce size
                        0.5f + Random.nextFloat() * 0.4f  // Reduced from 0.7-1.3 to 0.5-0.9
                    } else {
                        // Generate medium particles
                        0.3f + Random.nextFloat() * 0.2f  // Reduced from 0.4-0.7 to 0.3-0.5
                    }
                }
                2 -> {
                    if (Random.nextFloat() < 0.2f) {  // Reduced from 0.4 to 0.2, reduce large particle ratio
                        // Generate larger particles
                        0.4f + Random.nextFloat() * 0.3f  // Reduced from 0.6-1.0 to 0.4-0.7
                    } else {
                        // Generate medium particles
                        0.2f + Random.nextFloat() * 0.2f  // Reduced from 0.3-0.6 to 0.2-0.4
                    }
                }
                else -> {
                    // Light/weak rhythm - smaller particles
                    0.1f + Random.nextFloat() * 0.2f  // Reduced from 0.2-0.5 to 0.1-0.3
                }
            }
            
            // Reduce lifespan
            val lifespan = when (intensityLevel) {
                3 -> {
                    if (Random.nextFloat() < 0.2f) {  // Reduced from 0.3 to 0.2, reduce long-life particles ratio
                        // Generate long-life particles
                        0.8f + Random.nextFloat() * 0.4f  // Reduced from 1.2-1.8 to 0.8-1.2
                    } else {
                        // Generate standard life particles
                        0.5f + Random.nextFloat() * 0.3f  // Reduced from 0.7-1.2 to 0.5-0.8
                    }
                }
                2 -> {
                    if (Random.nextFloat() < 0.1f) {  // Reduced from 0.2 to 0.1, reduce long-life particles ratio
                        // Generate long-life particles
                        0.7f + Random.nextFloat() * 0.3f  // Reduced from 1.0-1.5 to 0.7-1.0
                    } else {
                        // Generate standard life particles
                        0.4f + Random.nextFloat() * 0.3f  // Reduced from 0.6-1.0 to 0.4-0.7
                    }
                }
                else -> {
                    // Light/weak rhythm - standard life
                    0.3f + Random.nextFloat() * 0.3f  // Reduced from 0.5-1.0 to 0.3-0.6
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
                    3 -> 1.0f  // Reduced from 1.5 to 1.0
                    2 -> 0.9f  // Reduced from 1.3 to 0.9
                    else -> 0.8f  // Reduced from 1.1 to 0.8
                }
            ))
        }
        
        // Add shockwave effect on strong rhythm, but reduce size and transparency
        if (intensityLevel >= 2 && Random.nextFloat() < 0.7f) {  // Increase randomness, not generated every time
            // Create a large, quickly disappearing glow particle, but reduce size
            val shockwaveSize = when (intensityLevel) {
                3 -> 1.5f + Random.nextFloat() * 0.3f  // Reduced from 2.0-2.5 to 1.5-1.8
                else -> 1.2f + Random.nextFloat() * 0.3f  // Reduced from 1.5-2.0 to 1.2-1.5
            }
            
            burstParticles.add(BurstParticle(
                x = burstX,
                y = burstY,
                vx = 0f,
                vy = 0f,
                size = shockwaveSize,
                lifespan = 0.2f + Random.nextFloat() * 0.2f,  // Reduced from 0.3-0.5 to 0.2-0.4
                maxLifespan = 0.4f,  // Reduced from 0.5 to 0.4
                colorVariation = Random.nextFloat() * 0.2f,
                alpha = 0.7f  // Reduced from 1.0 to 0.7
            ))
        }
    }
    
    /**
     * Apply music force field to particle velocity
     */
    private fun applyMusicForceField(x: Float, y: Float, vx: Float, vy: Float): Pair<Float, Float> {
        if (musicForceFieldStrength <= 0.01f) return Pair(vx, vy)
        
        // Calculate distance and direction from particle to center
        val centerX = 0.5f
        val centerY = 0.5f
        val dx = x - centerX
        val dy = y - centerY
        val distanceToCenter = sqrt(dx * dx + dy * dy)
        
        // Calculate unit vector of force field direction
        val forceX = cos(forceFieldDirection).toFloat()
        val forceY = sin(forceFieldDirection).toFloat()
        
        // Force field strength varies with distance from center, creating wave effect
        val distanceFactor = sin(distanceToCenter * Math.PI * 2).toFloat() * 0.5f + 0.5f
        val appliedForce = musicForceFieldStrength * distanceFactor
        
        // Apply force field - surprise emotion's force field has stronger influence
        val forceFactor = 0.15f * (1f + appliedForce * 0.6f)
        val newVx = vx + forceX * appliedForce * forceFactor
        val newVy = vy + forceY * appliedForce * forceFactor
        
        return Pair(newVx, newVy)
    }
    
    /**
     * Create surprise emotion color
     */
    private fun createSurpriseColor(intensity: Float, isCore: Boolean): Int {
        // Adjust colors to better match SurpriseFlashRenderer's style
        // Use more white and light blue, reduce purple
        val hue = when {
            Random.nextFloat() < 0.7f -> 200f + Random.nextFloat() * 40f  // 70% use blue tones (200-240)
            Random.nextFloat() < 0.2f -> 270f + Random.nextFloat() * 20f  // 20% use purple tones (270-290)
            else -> 0f  // 10% use white (hue doesn't matter as saturation will be very low)
        }
        
        // Reduce saturation, making colors closer to white, more suitable for flash effects
        val saturation = if (isCore) {
            (0.1f + intensity * 0.2f).coerceIn(0.1f, 0.3f)  // Reduced from 0.2-0.5 to 0.1-0.3
        } else {
            (0.2f + intensity * 0.3f).coerceIn(0.2f, 0.5f)  // Reduced from 0.4-0.8 to 0.2-0.5
        }
        
        // Maintain high brightness
        val brightness = if (isCore) {
            (0.9f + intensity * 0.1f).coerceIn(0.9f, 1.0f)  // Keep unchanged
        } else {
            (0.8f + intensity * 0.1f).coerceIn(0.8f, 0.9f)  // Increased from 0.7-0.9 to 0.8-0.9
        }
        
        // Convert HSV to RGB
        val hsv = floatArrayOf(hue, saturation, brightness)
        return Color.HSVToColor(hsv)
    }
    
    /**
     * Get current particle count
     */
    fun getParticleCount(): Int {
        return particles.size + burstParticles.size
    }
    
    /**
     * Clear all particles
     */
    fun clearParticles() {
        particles.clear()
        burstParticles.clear()
    }
    
    /**
     * Basic particle data class
     */
    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val size: Float,
        var lifespan: Float,
        val maxLifespan: Float,
        val intensity: Float,  // Used for color and effect intensity
        var alpha: Float = 1f
    )
    
    /**
     * Burst particle data class
     */
    private data class BurstParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var lifespan: Float,
        val maxLifespan: Float,
        val colorVariation: Float,  // Used for color variation
        var alpha: Float = 1f
    )
}