package com.example.musicvibrationapp.view.spectrum.renderers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.musicvibrationapp.view.SpectrumParameters
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Sadness Droplet System
 * Responsible for managing droplet generation, update and rendering
 */
class SadnessDropletSystem {
    
    // Droplet system - divided into three layers
    private val foregroundDroplets = mutableListOf<Droplet>() // Foreground large droplets
    private val midgroundDroplets = mutableListOf<Droplet>()  // Midground medium droplets
    private val backgroundDroplets = mutableListOf<Droplet>() // Background small droplets
    
    // Drawing tools
    private val dropletPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val trailPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    // Water surface height - ratio relative to screen height
    var waterSurfaceLevel = 0.9f
    
    // Callback interface - triggered when droplet touches water surface
    private var onDropletHitWater: ((x: Float, y: Float, size: Float, intensity: Float) -> Unit)? = null
    
    /**
     * Set callback for droplet hitting water surface
     */
    fun setOnDropletHitWaterListener(listener: (x: Float, y: Float, size: Float, intensity: Float) -> Unit) {
        onDropletHitWater = listener
    }
    
    /**
     * Get current droplet count
     */
    fun getDropletCount(): Triple<Int, Int, Int> {
        return Triple(foregroundDroplets.size, midgroundDroplets.size, backgroundDroplets.size)
    }
    
    /**
     * Clear all droplets
     */
    fun clearDroplets() {
        foregroundDroplets.clear()
        midgroundDroplets.clear()
        backgroundDroplets.clear()
    }
    
    /**
     * Draw all layers of droplets
     */
    fun drawAllDroplets(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        // Draw background droplets
        drawDroplets(canvas, backgroundDroplets, width, height, isVertical, 0.7f)
        
        // Draw midground droplets
        drawDroplets(canvas, midgroundDroplets, width, height, isVertical, 0.85f)
        
        // Draw foreground droplets
        drawDroplets(canvas, foregroundDroplets, width, height, isVertical, 1.0f)
    }
    
    /**
     * Draw droplets
     */
    private fun drawDroplets(
        canvas: Canvas, 
        droplets: List<Droplet>, 
        width: Float, 
        height: Float, 
        isVertical: Boolean,
        opacityFactor: Float
    ) {
        droplets.forEach { droplet ->
            // Adjust coordinates based on direction
            val x = if (isVertical) droplet.y * width else droplet.x * width
            val y = if (isVertical) droplet.x * height else droplet.y * height
            
            // Draw trail effect - lower speed threshold to allow more droplets to have trails
            if (droplet.speed > 0.03f && droplet.size > 0.15f) {  // Reduced from 0.05f/0.2f to 0.03f/0.15f
                drawDropletTrail(canvas, droplet, x, y, width, height, isVertical, opacityFactor)
            }
            
            // Calculate droplet size - increase base size
            val baseSize = 8f + droplet.fallSpeedFactor * 40f  // Increased from 5f+30f to 8f+40f
            val radius = droplet.size * baseSize
            
            // Create droplet color - brighter and more vivid blue
            val innerColor = Color.rgb(200, 230, 255)  // Brighter core
            val middleColor = Color.rgb(130, 190, 255)  // Brighter middle layer
            val outerColor = Color.rgb(70, 130, 230)    // Brighter outer layer
            
            // Create multi-level radial gradient - increase opacity
            val colors = intArrayOf(
                Color.argb(
                    (droplet.alpha * 255 * opacityFactor).toInt().coerceIn(0, 255),
                    Color.red(innerColor),
                    Color.green(innerColor),
                    Color.blue(innerColor)
                ),
                Color.argb(
                    (droplet.alpha * 240 * opacityFactor).toInt().coerceIn(0, 255),  // Increased from 230 to 240
                    Color.red(middleColor),
                    Color.green(middleColor),
                    Color.blue(middleColor)
                ),
                Color.argb(
                    (droplet.alpha * 220 * opacityFactor).toInt().coerceIn(0, 255),  // Increased from 200 to 220
                    Color.red(outerColor),
                    Color.green(outerColor),
                    Color.blue(outerColor)
                ),
                Color.TRANSPARENT
            )
            
            val positions = floatArrayOf(0f, 0.4f, 0.7f, 1.0f)
            
            dropletPaint.shader = RadialGradient(
                x, y,
                radius,
                colors,
                positions,
                Shader.TileMode.CLAMP
            )
            
            // Draw the droplet
            if (droplet.stretchFactor > 1.0f) {
                // Stretched droplet - using path
                val path = Path()
                val stretchedHeight = radius * droplet.stretchFactor
                
                // Create teardrop shape
                path.moveTo(x, y - stretchedHeight)
                
                // Right curve
                path.quadTo(
                    x + radius, y - stretchedHeight * 0.5f,
                    x, y + radius * 0.5f
                )
                
                // Left curve
                path.quadTo(
                    x - radius, y - stretchedHeight * 0.5f,
                    x, y - stretchedHeight
                )
                
                dropletPaint.style = Paint.Style.FILL
                canvas.drawPath(path, dropletPaint)
                
                // Add edge highlight to enhance visual effect
                dropletPaint.style = Paint.Style.STROKE
                dropletPaint.strokeWidth = 1.5f
                dropletPaint.color = Color.argb(
                    (droplet.alpha * 180 * opacityFactor).toInt().coerceIn(0, 255),
                    220, 240, 255
                )
                canvas.drawPath(path, dropletPaint)
                dropletPaint.style = Paint.Style.FILL
            } else {
                // Circular droplet
                canvas.drawCircle(x, y, radius, dropletPaint)
                
                // Add edge highlight
                dropletPaint.style = Paint.Style.STROKE
                dropletPaint.strokeWidth = 1.5f
                dropletPaint.color = Color.argb(
                    (droplet.alpha * 180 * opacityFactor).toInt().coerceIn(0, 255),
                    220, 240, 255
                )
                canvas.drawCircle(x, y, radius, dropletPaint)
                dropletPaint.style = Paint.Style.FILL
            }
            
            // Add droplet highlight - increase highlight size
            if (droplet.size > 0.2f) {  // Reduced from 0.3f to 0.2f to allow more droplets to have highlights
                val highlightRadius = radius * 0.4f  // Increased from 0.3f to 0.4f
                val highlightX = x - radius * 0.2f
                val highlightY = y - radius * 0.2f
                
                dropletPaint.shader = RadialGradient(
                    highlightX, highlightY,
                    highlightRadius,
                    Color.argb(
                        (droplet.alpha * 220 * opacityFactor).toInt().coerceIn(0, 255),  // Increased from 160 to 220
                        255, 255, 255
                    ),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                
                canvas.drawCircle(highlightX, highlightY, highlightRadius, dropletPaint)
            }
            
            dropletPaint.shader = null
        }
    }
    
    /**
     * Draw droplet trail
     */
    private fun drawDropletTrail(
        canvas: Canvas, 
        droplet: Droplet, 
        x: Float, 
        y: Float, 
        width: Float, 
        height: Float, 
        isVertical: Boolean,
        opacityFactor: Float
    ) {
        val baseSize = 8f + droplet.fallSpeedFactor * 40f  // Consistent with droplet size
        val radius = droplet.size * baseSize
        
        // Trail length proportional to speed - increase length
        val trailLength = droplet.speed * 150f * droplet.size  // Increased from 120f to 150f
        
        // Trail direction opposite to velocity
        val dirX = -droplet.vx / droplet.speed
        val dirY = -droplet.vy / droplet.speed
        
        // Trail start point
        val startX = x
        val startY = y
        
        // Trail end point
        val endX = x + dirX * trailLength
        val endY = y + dirY * trailLength
        
        // Trail color - brighter and more visible
        val trailColor = Color.rgb(150, 200, 255)  // Changed from 120,180,255 to brighter 150,200,255
        
        // Create linear gradient
        val trailGradient = android.graphics.LinearGradient(
            startX, startY, endX, endY,
            Color.argb(
                (droplet.alpha * 200 * opacityFactor).toInt().coerceIn(0, 255),  // Increased from 150 to 200
                Color.red(trailColor),
                Color.green(trailColor),
                Color.blue(trailColor)
            ),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        
        trailPaint.shader = trailGradient
        
        // Draw trail
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)
        
        // Draw trail - increase width
        trailPaint.strokeWidth = radius * 1.2f  // Increased from 1.0f to 1.2f
        trailPaint.style = Paint.Style.STROKE
        trailPaint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(path, trailPaint)
        
        // Add additional inner trail to enhance visual effect
        trailPaint.shader = android.graphics.LinearGradient(
            startX, startY, endX, endY,
            Color.argb(
                (droplet.alpha * 230 * opacityFactor).toInt().coerceIn(0, 255),
                220, 240, 255
            ),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        trailPaint.strokeWidth = radius * 0.6f
        canvas.drawPath(path, trailPaint)
        
        trailPaint.shader = null
    }
    
    /**
     * Update all droplets
     */
    fun updateAllDroplets(
        deltaTime: Float, 
        fallSpeed: Float, 
        minFallSpeed: Float,
        applyForceField: (x: Float, y: Float, vx: Float, vy: Float) -> Pair<Float, Float>,
        applyRhythmResponse: (vx: Float, vy: Float, pulseWeight: Float, flowWeight: Float) -> Pair<Float, Float>
    ) {
        // Update foreground droplets
        updateDropletLayer(
            foregroundDroplets, 
            deltaTime, 
            fallSpeed, 
            minFallSpeed, 
            1.0f,
            applyForceField,
            applyRhythmResponse
        )
        
        // Update midground droplets
        updateDropletLayer(
            midgroundDroplets, 
            deltaTime, 
            fallSpeed, 
            minFallSpeed, 
            0.8f,
            applyForceField,
            applyRhythmResponse
        )
        
        // Update background droplets
        updateDropletLayer(
            backgroundDroplets, 
            deltaTime, 
            fallSpeed, 
            minFallSpeed, 
            0.6f,
            applyForceField,
            applyRhythmResponse
        )
    }
    
    /**
     * Update specified layer of droplets
     */
    private fun updateDropletLayer(
        droplets: MutableList<Droplet>,
        deltaTime: Float,
        fallSpeed: Float,
        minFallSpeed: Float,
        speedFactor: Float,
        applyForceField: (x: Float, y: Float, vx: Float, vy: Float) -> Pair<Float, Float>,
        applyRhythmResponse: (vx: Float, vy: Float, pulseWeight: Float, flowWeight: Float) -> Pair<Float, Float>
    ) {
        droplets.removeAll { droplet ->
            // Apply lifecycle decay
            droplet.lifespan -= deltaTime
            
            // Get original velocity
            var newVx = droplet.vx
            var newVy = droplet.vy
            
            // Apply music force field - create breeze effect
            val (fieldVx, fieldVy) = applyForceField(droplet.x, droplet.y, newVx, newVy)
            newVx = fieldVx
            newVy = fieldVy
            
            // Apply rhythm response effect - use smoothingFactor to adjust response weight, with very small impact
            val responseFactor = 1.0f + (1.0f - SpectrumParameters.smoothingFactor.value) * 0.08f
            val pulseWeight = 0.15f * responseFactor
            val flowWeight = 0.3f * responseFactor
            val (pulsedVx, pulsedVy) = applyRhythmResponse(newVx, newVy, pulseWeight, flowWeight)
            newVx = pulsedVx
            newVy = pulsedVy
            
            // Apply gravity - use fallSpeed and minFallSpeed
            val gravity = (fallSpeed * 0.3f + minFallSpeed) * speedFactor
            newVy += gravity * deltaTime
            
            // Apply drag - droplet falling speed affected by drag, maintaining slow elegant motion
            val drag = 0.02f + (1f - speedFactor) * 0.03f
            newVx *= (1f - drag)
            newVy *= (1f - drag * 0.5f) // Vertical direction has less drag
            
            // Update velocity
            droplet.vx = newVx
            droplet.vy = newVy
            
            // Calculate velocity magnitude
            droplet.speed = sqrt(newVx * newVx + newVy * newVy)
            
            // Update position
            droplet.x += newVx * deltaTime
            droplet.y += newVy * deltaTime
            
            // Update droplet shape - faster speed, more stretching
            droplet.stretchFactor = 1f + droplet.speed * 5f
            
            // Update opacity
            val lifeFraction = (droplet.lifespan / droplet.maxLifespan).coerceIn(0f, 1f)
            droplet.alpha = if (lifeFraction > 0.7f) {
                lifeFraction
            } else {
                lifeFraction * lifeFraction / 0.7f
            }
            
            // Check if touching water surface
            if (droplet.y >= waterSurfaceLevel && !droplet.hitWater) {
                droplet.hitWater = true
                
                // Trigger callback
                onDropletHitWater?.invoke(droplet.x, waterSurfaceLevel, droplet.size, droplet.intensity)
                
                // Droplet disappears after touching water surface
                return@removeAll true
            }
            
            // Droplet disappearance conditions
            droplet.lifespan <= 0 || droplet.x < -0.1f || droplet.x > 1.1f || droplet.y < -0.1f || droplet.y > 1.1f
        }
    }
    
    /**
     * Generate droplets
     */
    fun generateDroplets(
        data: FloatArray, 
        isRhythmPeak: Boolean, 
        minThreshold: Float,
        lowFrequencyEnergySmoothed: Float
    ) {
        // Limit total droplet count - use smoothingFactor to adjust maximum count, with very small impact
        val dropletDensityFactor = 1.0f + (1.0f - SpectrumParameters.smoothingFactor.value) * 0.06f
        val maxForegroundDroplets = (60 * dropletDensityFactor).toInt()
        val maxMidgroundDroplets = (100 * dropletDensityFactor).toInt()
        val maxBackgroundDroplets = (150 * dropletDensityFactor).toInt()
        
        // Low frequency energy affects droplet generation rate - use melSensitivity to adjust impact level, with very small impact
        val energyResponseFactor = 1.0f + SpectrumParameters.melSensitivity.value * 0.08f
        val lowFreqMultiplier = 3f + lowFrequencyEnergySmoothed * 15f * energyResponseFactor
        
        // Increase droplet generation probability at rhythm peaks - use soloResponseStrength for adjustment, with very small impact
        val rhythmBoost = 1.0f + SpectrumParameters.soloResponseStrength.value * 0.5f
        val rhythmMultiplier = if (isRhythmPeak) 5f * rhythmBoost else 3f
        
        // Ensure base generation rate - use minThreshold for adjustment, with very small impact
        val baseGenerationRate = 0.08f + SpectrumParameters.minThreshold.value * 0.06f
        
        // Generate droplets based on spectrum energy
        data.forEachIndexed { index, magnitude ->
            // Use minThreshold as generation threshold, but lower the barrier
            val dynamicThreshold = minThreshold * 0.3f  // Reduced from 0.5f to 0.3f
            
            // Generation probability more affected by low frequencies
            val frequencyFactor = if (index < data.size * 0.3f) 3f else 1.5f  // Increased from 2f/1f to 3f/1.5f
            val generationProbability = (magnitude * 0.8f * rhythmMultiplier * lowFreqMultiplier * frequencyFactor)
                .coerceAtLeast(baseGenerationRate)  // Ensure minimum generation rate
            
            if ((magnitude > dynamicThreshold || Random.nextFloat() < baseGenerationRate) && 
                Random.nextFloat() < generationProbability) {
                // Determine droplet size - low frequencies produce larger droplets
                val dropletSize = when {
                    index < data.size * 0.2 -> 0.5f + Random.nextFloat() * 0.5f  // Low freq - large droplets (increased)
                    index < data.size * 0.5 -> 0.3f + Random.nextFloat() * 0.3f  // Mid freq - medium droplets (increased)
                    else -> 0.15f + Random.nextFloat() * 0.25f  // High freq - small droplets (increased)
                }
                
                // Determine droplet layer
                val layer = when {
                    dropletSize > 0.5f && foregroundDroplets.size < maxForegroundDroplets -> 0  // Foreground
                    dropletSize > 0.3f && midgroundDroplets.size < maxMidgroundDroplets -> 1    // Midground
                    backgroundDroplets.size < maxBackgroundDroplets -> 2                         // Background
                    else -> -1  // Don't generate
                }
                
                if (layer >= 0) {
                    // Generation position - generate from random position at screen top
                    val originX = Random.nextFloat()
                    val originY = -0.1f - Random.nextFloat() * 0.1f
                    
                    // Initial velocity - slight horizontal velocity, vertical velocity near 0
                    val baseVx = (Random.nextFloat() - 0.5f) * 0.05f
                    val baseVy = 0.01f + Random.nextFloat() * 0.02f
                    
                    // Lifespan
                    val lifespan = 5f + Random.nextFloat() * 5f
                    
                    // Create droplet
                    val droplet = Droplet(
                        x = originX,
                        y = originY,
                        vx = baseVx,
                        vy = baseVy,
                        size = dropletSize,
                        lifespan = lifespan,
                        maxLifespan = lifespan,
                        intensity = 0.7f + magnitude * 0.3f,
                        alpha = 1f,
                        speed = sqrt(baseVx * baseVx + baseVy * baseVy),
                        stretchFactor = 1f,
                        hitWater = false,
                        fallSpeedFactor = 1f
                    )
                    
                    // Add to corresponding layer
                    when (layer) {
                        0 -> foregroundDroplets.add(droplet)
                        1 -> midgroundDroplets.add(droplet)
                        2 -> backgroundDroplets.add(droplet)
                    }
                }
            }
        }
        
        // Force generation of some base droplets - ensure constant visual effect
        if (foregroundDroplets.size < 5 || Random.nextFloat() < 0.2f) {
            repeat(3) {
                val originX = Random.nextFloat()
                val originY = -0.1f - Random.nextFloat() * 0.1f
                val baseVx = (Random.nextFloat() - 0.5f) * 0.05f
                val baseVy = 0.02f + Random.nextFloat() * 0.03f
                val lifespan = 5f + Random.nextFloat() * 5f
                
                val droplet = Droplet(
                    x = originX,
                    y = originY,
                    vx = baseVx,
                    vy = baseVy,
                    size = 0.4f + Random.nextFloat() * 0.3f,  // Larger droplets
                    lifespan = lifespan,
                    maxLifespan = lifespan,
                    intensity = 0.8f,
                    alpha = 1f,
                    speed = sqrt(baseVx * baseVx + baseVy * baseVy),
                    stretchFactor = 1f,
                    hitWater = false,
                    fallSpeedFactor = 1f
                )
                
                foregroundDroplets.add(droplet)
            }
        }
    }
    
    /**
     * Generate larger droplets at rhythm points
     */
    fun generateRhythmDroplets(energy: Float, volumeChange: Float, intensityLevel: Int, fallSpeedFactor: Float) {
        // Use soloResponseStrength to fine-tune droplet count, with very small impact
        val countMultiplier = 1.0f + SpectrumParameters.soloResponseStrength.value * 0.1f
        
        val dropletCount = when (intensityLevel) {
            3 -> (energy * 15 * countMultiplier).toInt().coerceIn(5, 15)  // Strong rhythm - more droplets
            2 -> (energy * 10 * countMultiplier).toInt().coerceIn(3, 10)  // Medium rhythm
            1 -> (energy * 5 * countMultiplier).toInt().coerceIn(2, 5)    // Slight rhythm
            else -> (energy * 3 * countMultiplier).toInt().coerceIn(1, 3) // Weak rhythm
        }
        
        // Volume change affects droplet size
        val sizeBoost = when (intensityLevel) {
            3 -> volumeChange * 2.0f  // Strong rhythm - larger droplets
            2 -> volumeChange * 1.5f  // Medium rhythm
            1 -> volumeChange * 1.0f  // Slight rhythm
            else -> volumeChange * 0.5f  // Weak rhythm
        }
        
        // Generate rhythm droplets
        repeat(dropletCount) {
            // Generation position - generate from random position at screen top
            val originX = 0.2f + Random.nextFloat() * 0.6f  // Concentrated in middle area
            val originY = -0.1f - Random.nextFloat() * 0.1f
            
            // Droplet size - rhythm droplets are larger
            val dropletSize = 0.5f + Random.nextFloat() * 0.3f + sizeBoost
            
            // Initial velocity - slight horizontal velocity, vertical velocity near 0
            val baseVx = (Random.nextFloat() - 0.5f) * 0.05f
            val baseVy = 0.02f + Random.nextFloat() * 0.03f
            
            // Lifespan
            val lifespan = 5f + Random.nextFloat() * 5f
            
            // Create droplet
            val droplet = Droplet(
                x = originX,
                y = originY,
                vx = baseVx,
                vy = baseVy,
                size = dropletSize,
                lifespan = lifespan,
                maxLifespan = lifespan,
                intensity = 0.8f + intensityLevel * 0.05f,
                alpha = 1f,
                speed = sqrt(baseVx * baseVx + baseVy * baseVy),
                stretchFactor = 1f,
                hitWater = false,
                fallSpeedFactor = fallSpeedFactor
            )
            
            // Add to foreground droplets
            foregroundDroplets.add(droplet)
        }
    }
    
    /**
     * Ensure color is in blue range
     */
    private fun ensureBlueColor(color: Int, intensity: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        
        // Strictly limit to blue range (220-240 degrees)
        hsv[0] = 220f + (intensity * 20f).coerceIn(0f, 20f)
        
        // Adjust saturation and brightness, creating depth variation while maintaining blue theme
        hsv[1] = (0.7f + intensity * 0.3f).coerceIn(0.5f, 1.0f)
        hsv[2] = (0.5f + intensity * 0.5f).coerceIn(0.3f, 1.0f)
        
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
    
    /**
     * Droplet data class
     */
    data class Droplet(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val size: Float,
        var lifespan: Float,
        val maxLifespan: Float,
        val intensity: Float,
        var alpha: Float = 1f,
        var speed: Float = 0f,
        var stretchFactor: Float = 1f,
        var hitWater: Boolean = false,
        val fallSpeedFactor: Float = 1f
    )
}