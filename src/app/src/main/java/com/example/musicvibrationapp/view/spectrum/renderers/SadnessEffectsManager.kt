package com.example.musicvibrationapp.view.spectrum.renderers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.musicvibrationapp.view.SpectrumParameters
import kotlin.math.sin
import kotlin.random.Random

/**
 * Grief effects manager
 * Responsible for managing special effects such as ripples and fogs
 */
class SadnessEffectsManager {
    
    // ripple effect
    private val ripples = mutableListOf<Ripple>()
    
    // fog effect
    private val fogPoints = mutableListOf<FogPoint>()
    
    // drawing tool
    private val ripplePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    private val fogPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    /**
     * draw water surface
     */
    fun drawWaterSurface(
        canvas: Canvas, 
        width: Float, 
        height: Float, 
        isVertical: Boolean, 
        waterSurfaceLevel: Float,
        lowFrequencyEnergySmoothed: Float
    ) {
        // Water surface position
        val adjustedWaterLevel = waterSurfaceLevel * 0.85f
        val surfaceY = if (isVertical) width * adjustedWaterLevel else height * adjustedWaterLevel
        
        // Water surface color - much brighter blue for better visibility
        val waterColor = Color.rgb(60, 120, 220)  // Brighter, more saturated blue
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.argb(150, Color.red(waterColor), Color.green(waterColor), Color.blue(waterColor))  // Increased opacity
        }
        
        // Draw water surface with gradient for depth
        val waterGradient = android.graphics.LinearGradient(
            0f, surfaceY, 0f, if (isVertical) width else height,
            Color.argb(150, 60, 120, 220),
            Color.argb(180, 20, 60, 150),
            Shader.TileMode.CLAMP
        )
        
        paint.shader = waterGradient
        
        if (isVertical) {
            canvas.drawRect(surfaceY, 0f, width, height, paint)
        } else {
            canvas.drawRect(0f, surfaceY, width, height, paint)
        }
        paint.shader = null
        
        // Draw water surface line with higher contrast and glow effect
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f  // Thicker line
        paint.color = Color.argb(200, 120, 180, 255)  // Much brighter blue for the line
        
        // Create wave path - enhance wave effect dramatically
        val path = Path()
        // use smoothingFactor to adjust wave amplitude response, very small impact
        val responsiveness = 1.0f + (1.0f - SpectrumParameters.smoothingFactor.value) * 0.1f
        val amplitude = 6f + lowFrequencyEnergySmoothed * 30f * responsiveness
        val frequency = 0.05f
        val phase = (System.currentTimeMillis() % 10000) / 10000f * 2f * Math.PI.toFloat()
        
        if (isVertical) {
            path.moveTo(surfaceY, 0f)
            for (x in 0 until height.toInt() step 5) {
                val y = surfaceY + sin(x * frequency + phase) * amplitude
                path.lineTo(y, x.toFloat())
            }
            canvas.drawPath(path, paint)
            
            // Add glow effect
            paint.strokeWidth = 6f
            paint.color = Color.argb(80, 120, 180, 255)
            canvas.drawPath(path, paint)
        } else {
            path.moveTo(0f, surfaceY)
            for (x in 0 until width.toInt() step 5) {
                val y = surfaceY + sin(x * frequency + phase) * amplitude
                path.lineTo(x.toFloat(), y)
            }
            canvas.drawPath(path, paint)
            
            // Add glow effect
            paint.strokeWidth = 6f
            paint.color = Color.argb(80, 120, 180, 255)
            canvas.drawPath(path, paint)
        }
        
        // Add subtle reflections on water surface
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(30, 255, 255, 255)
        
        for (i in 0 until 20) {
            val reflectionX = Random.nextFloat() * width
            val reflectionY = if (isVertical) {
                Random.nextFloat() * height
            } else {
                surfaceY + Random.nextFloat() * (height - surfaceY) * 0.3f
            }
            val reflectionSize = 2f + Random.nextFloat() * 5f
            
            canvas.drawCircle(reflectionX, reflectionY, reflectionSize, paint)
        }
    }
    
    /**
     * draw ripples
     */
    fun drawRipples(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        ripples.forEach { ripple ->
            // adjust coordinates according to direction
            val x = if (isVertical) ripple.y * width else ripple.x * width
            val y = if (isVertical) ripple.x * height else ripple.y * height
            
            // ripple color - brighter and more visible
            val rippleColor = Color.rgb(140, 200, 255)  // Brighter blue
            
            // Draw multiple rings with different opacities for better visibility
            for (i in 0 until 3) {
                val progressOffset = i * 0.1f
                val progress = (ripple.progress - progressOffset).coerceIn(0f, 1f)
                
                if (progress > 0) {
                    val alpha = ((1f - progress) * ripple.alpha * 200).toInt().coerceIn(0, 255)
                    
            ripplePaint.color = Color.argb(
                        alpha,
                Color.red(rippleColor),
                Color.green(rippleColor),
                Color.blue(rippleColor)
            )
            
                    ripplePaint.strokeWidth = 3f * (1f - progress * 0.7f)
                    
                    // draw ripple circle
                    canvas.drawCircle(x, y, ripple.radius * progress, ripplePaint)
                }
            }
            
            // Add a center highlight for each ripple
            val centerAlpha = ((1f - ripple.progress) * ripple.alpha * 150).toInt().coerceIn(0, 255)
            if (centerAlpha > 0) {
                val centerPaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = Color.argb(centerAlpha, 200, 230, 255)
                }
                canvas.drawCircle(x, y, ripple.radius * 0.1f, centerPaint)
            }
        }
    }
    
    /**
     * draw fog effect
     */
    fun drawFog(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        fogPoints.forEach { fogPoint ->
            // 根据方向调整坐标
            val x = if (isVertical) fogPoint.y * width else fogPoint.x * width
            val y = if (isVertical) fogPoint.x * height else fogPoint.y * height
            
            // fog color - brighter and more visible blue tone
            val fogColor = Color.rgb(130, 180, 255)
            
            // Create a more visible fog effect with multiple layers
            val outerAlpha = (fogPoint.alpha * 100).toInt().coerceIn(0, 255)
            val innerAlpha = (fogPoint.alpha * 150).toInt().coerceIn(0, 255)
            
            // Outer glow
            fogPaint.shader = RadialGradient(
                x, y,
                fogPoint.size,
                Color.argb(
                    outerAlpha,
                    Color.red(fogColor),
                    Color.green(fogColor),
                    Color.blue(fogColor)
                ),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            
            canvas.drawCircle(x, y, fogPoint.size, fogPaint)
            
            // Inner core - brighter
            fogPaint.shader = RadialGradient(
                x, y,
                fogPoint.size * 0.6f,
                Color.argb(
                    innerAlpha,
                    Color.red(fogColor) + 30,
                    Color.green(fogColor) + 30,
                    Color.blue(fogColor)
                ),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            
            canvas.drawCircle(x, y, fogPoint.size * 0.6f, fogPaint)
            fogPaint.shader = null
        }
    }
    
    /**
     * update ripples
     */
    fun updateRipples(deltaTime: Float) {
        ripples.removeAll { ripple ->
            // update progress
            ripple.progress += deltaTime / ripple.duration
            
            // update alpha
            ripple.alpha = 1f - ripple.progress
            
            // ripple disappearance condition
            ripple.progress >= 1f
        }
    }
    
    /**
     * update fog
     */
    fun updateFog(deltaTime: Float) {
        // limit fog point count - use smoothingFactor to adjust maximum count, very small impact
        val fogDensityFactor = 1.0f + (1.0f - SpectrumParameters.smoothingFactor.value) * 0.08f
        val maxFogPoints = (100 * fogDensityFactor).toInt()
        
        // increase generation probability - use smoothingFactor to adjust generation rate, very small impact
        val generationRate = 0.25f * (1.0f + (1.0f - SpectrumParameters.smoothingFactor.value) * 0.06f)
        if (fogPoints.size < maxFogPoints && Random.nextFloat() < generationRate) {
            // randomly generate new fog points
            fogPoints.add(FogPoint(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = 60f + Random.nextFloat() * 100f,  // significantly increase size
                alpha = 0.6f + Random.nextFloat() * 0.4f,  // increase opacity
                lifespan = 5f + Random.nextFloat() * 5f,
                maxLifespan = 10f
            ))
        }
        
        // ensure always some basic fog
        if (fogPoints.size < 20) {  // increase from 10 to 20
            repeat(10) {  // increase from 5 to 10
                fogPoints.add(FogPoint(
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    size = 70f + Random.nextFloat() * 90f,  // increase size
                    alpha = 0.7f,  // increase opacity
                    lifespan = 8f,
                    maxLifespan = 10f
                ))
            }
        }
        
        fogPoints.removeAll { fogPoint ->
            // apply lifespan decay
            fogPoint.lifespan -= deltaTime
            
            // slowly move
            fogPoint.x += (Random.nextFloat() - 0.5f) * 0.01f * deltaTime
            fogPoint.y += (Random.nextFloat() - 0.5f) * 0.01f * deltaTime
            
            // update alpha
            val lifeFraction = (fogPoint.lifespan / fogPoint.maxLifespan).coerceIn(0f, 1f)
            fogPoint.alpha = lifeFraction * 0.6f
            
            // fog disappearance condition
            fogPoint.lifespan <= 0
        }
    }
    
    /**
     * generate ripples
     */
    fun generateRipple(x: Float, y: Float, size: Float, intensity: Float) {
        // ripple radius proportional to droplet size - use minThreshold to adjust base size, very small impact
        val baseRadius = 20f + SpectrumParameters.minThreshold.value * 40f
        val rippleRadius = baseRadius + size * 80f
        
        // ripple duration - use smoothingFactor to adjust, very small impact
        val durationFactor = 1.0f + SpectrumParameters.smoothingFactor.value * 0.08f
        val duration = (2.0f + size * 2.0f) * durationFactor
        
        // create ripple
        ripples.add(Ripple(
            x = x,
            y = y,
            radius = rippleRadius,
            progress = 0f,
            duration = duration,
            intensity = intensity,
            alpha = 1f
        ))
        
        // large droplets produce multiple ripples - increase ripple count and variation
        if (size > 0.3f) {  // reduce from 0.5f to 0.3f, allow more droplets to produce multiple ripples
            // add second ripple, slightly delayed
            ripples.add(Ripple(
                x = x,
                y = y,
                radius = rippleRadius * 0.7f,
                progress = -0.15f,  // reduce delay
                duration = duration * 0.8f,
                intensity = intensity * 0.9f,
                alpha = 0.9f
            ))
            
            // add third ripple, more delay
            ripples.add(Ripple(
                x = x,
                y = y,
                radius = rippleRadius * 0.5f,
                progress = -0.3f,  // reduce delay
                duration = duration * 0.6f,
                intensity = intensity * 0.8f,
                alpha = 0.8f
            ))
            
            // add fourth ripple, maximum delay
            ripples.add(Ripple(
                x = x,
                y = y,
                radius = rippleRadius * 0.3f,
                progress = -0.45f,
                duration = duration * 0.4f,
                intensity = intensity * 0.7f,
                alpha = 0.7f
            ))
        }
    }
    
    /**
     * draw gradient background
     */
    fun drawGradientBackground(canvas: Canvas, width: Float, height: Float) {
        // Create a more dramatic gradient with deeper contrast
        val darkestBlue = Color.rgb(2, 5, 20)  // Almost black-blue for base
        val darkBlue = Color.rgb(5, 15, 40)    // Dark blue for middle
        val midBlue = Color.rgb(15, 30, 70)    // Medium blue for top
        
        // Three-color gradient for more depth
        val colors = intArrayOf(darkestBlue, darkBlue, midBlue)
        val positions = floatArrayOf(0f, 0.6f, 1.0f)
        
        val gradient = android.graphics.LinearGradient(
            0f, 0f, 0f, height,
            colors, positions,
            Shader.TileMode.CLAMP
        )
        
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null
        
        // Add more visible stars/noise points for texture and depth
        val noiseCount = (width * height / 3000).toInt().coerceIn(50, 300)
        
        for (i in 0 until noiseCount) {
            val x = Random.nextFloat() * width
            val y = Random.nextFloat() * height
            
            // Vary the size and brightness to create a starfield effect
            val sizeFactor = Random.nextFloat()
            val size = when {
                sizeFactor > 0.95f -> 2f + Random.nextFloat() * 2f  // Larger stars (5%)
                sizeFactor > 0.8f -> 1f + Random.nextFloat() * 1f   // Medium stars (15%)
                else -> 0.5f + Random.nextFloat() * 0.5f            // Small stars (80%)
            }
            
            // Vary the brightness
            val brightnessFactor = Random.nextFloat()
            val alpha = when {
                sizeFactor > 0.95f -> 70 + Random.nextInt(60)  // Brighter for larger stars
                sizeFactor > 0.8f -> 40 + Random.nextInt(40)   // Medium brightness
                else -> 20 + Random.nextInt(30)                // Dimmer for small stars
            }
            
            // Use blue-white color for stars
            paint.color = Color.argb(
                alpha,
                180 + Random.nextInt(75),  // More white-blue for stars
                200 + Random.nextInt(55),
                255
            )
            
            canvas.drawCircle(x, y, size, paint)
        }
    }
    
    /**
     * Ensure color is in blue range but with better contrast
     */
    private fun ensureBlueColor(color: Int, intensity: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        
        // Wider blue range (200-240 degrees) for more variety
        hsv[0] = 200f + (intensity * 40f).coerceIn(0f, 40f)
        
        // Higher saturation and brightness for better visibility
        hsv[1] = (0.7f + intensity * 0.3f).coerceIn(0.6f, 1.0f)
        hsv[2] = (0.6f + intensity * 0.4f).coerceIn(0.4f, 1.0f)
        
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
    
    /**
     * ripple data class
     */
    data class Ripple(
        val x: Float,
        val y: Float,
        val radius: Float,
        var progress: Float,
        val duration: Float,
        val intensity: Float,
        var alpha: Float = 1f
    )
    
    /**
     * fog point data class
     */
    data class FogPoint(
        var x: Float,
        var y: Float,
        val size: Float,
        var alpha: Float,
        var lifespan: Float,
        val maxLifespan: Float
    )
}