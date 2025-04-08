package com.example.musicvibrationapp.view.spectrum.renderers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.BlurMaskFilter
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Visual echo system for the emotion of surprise
 * Used to create stigmata and visual delay effects to enhance the visual impact of surprise
 */
class SurpriseVisualEchoSystem {
    // visual echo list
    private val visualEchoes = mutableListOf<VisualEcho>()
    
    // drawing tool
    private val echoPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f  // increase line width
        color = Color.WHITE
        // add glow effect
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }
    
    private val echoFillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    
    // add control parameters for visual echo
    private var lastEchoTime = 0L
    private val minEchoInterval = 50L // reduce minimum echo interval (from 100ms to 50ms)
    
    /**
     * add new visual echo
     * @param intensity intensity 0-1
     * @param centerX centerX coordinate (0-1)
     * @param centerY centerY coordinate (0-1)
     * @param shapeType shape type (optional)
     * @return whether the new visual echo is successfully added
     */
    fun addVisualEcho(
        intensity: Float,
        centerX: Float = 0.5f,
        centerY: Float = 0.5f,
        shapeType: Int = -1
    ): Boolean {
        // control the frequency of adding, but allow high-intensity echoes to appear more frequently
        val currentTime = System.currentTimeMillis()
        val adjustedInterval = if (intensity > 0.8f) minEchoInterval / 2 else minEchoInterval
        if (currentTime - lastEchoTime < adjustedInterval) {
            return false
        }
        lastEchoTime = currentTime
        
        // increase echo effect parameters
        val lifespan = 0.8f + intensity * 0.8f  // increase lifespan
        val size = 0.15f + intensity * 0.5f     // increase size
        
        // randomly select shape type, if not specified
        val actualShapeType = if (shapeType >= 0) shapeType else Random.nextInt(5)
        
        // create more dynamic random offset
        val offsetX = (Random.nextFloat() - 0.5f) * 0.2f
        val offsetY = (Random.nextFloat() - 0.5f) * 0.2f
        
        // select a brighter, more contrast color
        val baseColor = when {
            intensity > 0.8f -> Color.rgb(255, 255, 255)  // pure white
            intensity > 0.6f -> Color.rgb(220, 240, 255)  // bright blue white
            intensity > 0.4f -> Color.rgb(200, 220, 255)  // light blue
            else -> Color.rgb(180, 200, 255)              // light purple blue
        }
        
        // add extra effect to high-intensity echoes - create several echoes in the same position to form an echo cloud
        if (intensity > 0.75f) {
            // add 2-3 additional echoes
            val extraEchoes = 2 + (intensity * 2).toInt().coerceAtMost(3)
            repeat(extraEchoes) { index ->
                val extraOffset = 0.05f * (index + 1)
                val extraSize = size * (0.9f - index * 0.15f)
                val extraLifespan = lifespan * (0.9f - index * 0.1f)
                
                visualEchoes.add(
                    VisualEcho(
                        centerX = centerX + offsetX * (1f + index * 0.5f),
                        centerY = centerY + offsetY * (1f + index * 0.5f),
                        size = extraSize,
                        lifespan = extraLifespan,
                        maxLifespan = extraLifespan,
                        shapeType = actualShapeType,
                        color = baseColor,
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = (Random.nextFloat() - 0.5f) * 60f,  // increase rotation speed
                        speedX = (Random.nextFloat() - 0.5f) * 0.08f,       // increase moving speed
                        speedY = (Random.nextFloat() - 0.5f) * 0.08f,
                        opacity = (0.7f + intensity * 0.3f) * (1f - index * 0.2f)
                    )
                )
            }
        }
        
        // add main echo
        visualEchoes.add(
            VisualEcho(
                centerX = centerX + offsetX,
                centerY = centerY + offsetY,
                size = size,
                lifespan = lifespan,
                maxLifespan = lifespan,
                shapeType = actualShapeType,
                color = baseColor,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 60f,  // increase rotation speed
                speedX = (Random.nextFloat() - 0.5f) * 0.08f,       // increase moving speed
                speedY = (Random.nextFloat() - 0.5f) * 0.08f,
                opacity = 0.9f + intensity * 0.1f                   // increase opacity
            )
        )
        
        return true
    }
    
    /**
     * deal with data and update visual echoes
     */
    fun processData(deltaTime: Float, currentVolume: Float) {
        // if the volume is low, accelerate the echo disappearance
        val volumeFactor = if (currentVolume < 0.1f) 2.0f else 1.0f
        
        // update existing echoes
        visualEchoes.removeAll { echo ->
            // apply lifespan decay
            echo.lifespan -= deltaTime * (1.0f + currentVolume * 0.7f) * volumeFactor
            
            // update rotation
            echo.rotation += echo.rotationSpeed * deltaTime
            
            // update position offset, with speed changes based on lifespan
            val lifeFactor = echo.lifespan / echo.maxLifespan
            // when nearing disappearance, accelerate movement
            val speedMultiplier = if (lifeFactor < 0.3f) 2.0f - lifeFactor * 3.0f else 1.0f
            
            echo.offsetX += echo.speedX * deltaTime * speedMultiplier
            echo.offsetY += echo.speedY * deltaTime * speedMultiplier
            
            // update size - first expand then shrink
            echo.currentSize = if (lifeFactor > 0.7f) {
                // initial expansion
                echo.size * (0.8f + 0.2f * (1.0f - (lifeFactor - 0.7f) / 0.3f))
            } else {
                // later shrink
                echo.size * (0.3f + 0.7f * lifeFactor / 0.7f)
            }
            
            // update opacity - keep high opacity then quickly disappear
            echo.currentOpacity = if (lifeFactor > 0.3f) {
                echo.opacity
            } else {
                echo.opacity * (lifeFactor / 0.3f)
            }
            
            // remove condition
            echo.lifespan <= 0
        }
        
        // limit the maximum number of echoes to prevent performance issues
        if (visualEchoes.size > 40) {
            // remove the oldest echo
            visualEchoes.sortBy { it.lifespan / it.maxLifespan }
            while (visualEchoes.size > 30) {
                visualEchoes.removeAt(0)
            }
        }
    }
    
    /**
     * render visual echoes
     */
    fun render(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        // sort by opacity to ensure semi-transparent objects are drawn correctly
        val sortedEchoes = visualEchoes.sortedBy { it.currentOpacity }
        
        sortedEchoes.forEach { echo ->
            // calculate actual coordinates
            val centerX = if (isVertical) echo.centerY * width else echo.centerX * width
            val centerY = if (isVertical) echo.centerX * height else echo.centerY * height
            
            // calculate actual size - increase size
            val size = echo.currentSize * (if (isVertical) height else width) * 0.6f
            
            // apply offset
            val offsetX = echo.offsetX * width
            val offsetY = echo.offsetY * height
            
            // set opacity
            val alpha = (echo.currentOpacity * 255).toInt().coerceIn(0, 255)
            echoPaint.alpha = alpha
            
            // set fill opacity based on shape type - different opacity for different shapes
            when (echo.shapeType) {
                0 -> echoFillPaint.alpha = (alpha * 0.4f).toInt().coerceIn(0, 255)  // rectangle more transparent
                1 -> echoFillPaint.alpha = (alpha * 0.5f).toInt().coerceIn(0, 255)  // circle medium transparent
                2 -> echoFillPaint.alpha = (alpha * 0.45f).toInt().coerceIn(0, 255) // triangle
                3 -> echoFillPaint.alpha = (alpha * 0.6f).toInt().coerceIn(0, 255)  // star relatively opaque
                else -> echoFillPaint.alpha = (alpha * 0.5f).toInt().coerceIn(0, 255)
            }
            
            // set color and add glow effect
            echoPaint.color = echo.color
            
            // set gradient effect for fill
            val fillColors = intArrayOf(
                Color.argb(
                    echoFillPaint.alpha,
                    Color.red(echo.color),
                    Color.green(echo.color),
                    Color.blue(echo.color)
                ),
                Color.argb(
                    (echoFillPaint.alpha * 0.6f).toInt(),
                    Color.red(echo.color),
                    Color.green(echo.color),
                    Color.blue(echo.color)
                ),
                Color.argb(
                    0,
                    Color.red(echo.color),
                    Color.green(echo.color),
                    Color.blue(echo.color)
                )
            )
            
            // add radial gradient for large echoes
            if (size > 100) {
                echoFillPaint.shader = RadialGradient(
                    0f, 0f, size,
                    fillColors,
                    floatArrayOf(0f, 0.6f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            } else {
                echoFillPaint.color = echo.color
                echoFillPaint.shader = null
            }
            
            // set line width based on size
            echoPaint.strokeWidth = 2f + size * 0.02f
            
            // save current canvas state
            canvas.save()
            
            // apply rotation and translation
            canvas.translate(centerX + offsetX, centerY + offsetY)
            canvas.rotate(echo.rotation)
            
            // draw based on shape type
            when (echo.shapeType) {
                0 -> drawEchoRectangle(canvas, size)
                1 -> drawEchoCircle(canvas, size)
                2 -> drawEchoTriangle(canvas, size)
                3 -> drawEchoStar(canvas, size)
                4 -> drawEchoPolygon(canvas, size, 5 + Random.nextInt(3))
                else -> drawEchoCircle(canvas, size)
            }
            
            // restore canvas state
            canvas.restore()
            
            // clear shader
            echoFillPaint.shader = null
        }
    }
    
    /**
     * draw rectangle echo
     */
    private fun drawEchoRectangle(canvas: Canvas, size: Float) {
        val rect = RectF(-size, -size, size, size)
        
        // draw fill first
        canvas.drawRect(rect, echoFillPaint)
        
        // draw stroke
        canvas.drawRect(rect, echoPaint)
    }
    
    /**
     * draw circle echo
     */
    private fun drawEchoCircle(canvas: Canvas, size: Float) {
        // draw fill first
        canvas.drawCircle(0f, 0f, size, echoFillPaint)
        
        // draw stroke
        canvas.drawCircle(0f, 0f, size, echoPaint)
    }
    
    /**
     * draw triangle echo
     */
    private fun drawEchoTriangle(canvas: Canvas, size: Float) {
        val path = Path()
        path.moveTo(0f, -size)
        path.lineTo(-size, size)
        path.lineTo(size, size)
        path.close()
        
        // draw fill first
        canvas.drawPath(path, echoFillPaint)
        
        // draw stroke
        canvas.drawPath(path, echoPaint)
    }
    
    /**
     * draw star echo
     */
    private fun drawEchoStar(canvas: Canvas, size: Float) {
        val path = Path()
        val outerRadius = size
        val innerRadius = size * 0.4f
        val numPoints = 5
        
        for (i in 0 until numPoints * 2) {
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val angle = Math.PI * i / numPoints
            val x = (cos(angle) * radius).toFloat()
            val y = (sin(angle) * radius).toFloat()
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        
        // draw fill first
        canvas.drawPath(path, echoFillPaint)
        
        // draw stroke
        canvas.drawPath(path, echoPaint)
    }
    
    /**
     * draw polygon echo
     */
    private fun drawEchoPolygon(canvas: Canvas, size: Float, sides: Int) {
        val path = Path()
        val angle = 2.0 * Math.PI / sides
        
        for (i in 0 until sides) {
            val x = (cos(angle * i) * size).toFloat()
            val y = (sin(angle * i) * size).toFloat()
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        
        // draw fill first
        canvas.drawPath(path, echoFillPaint)
        
        // draw stroke
        canvas.drawPath(path, echoPaint)
    }
    
    /**
     * get current echo count
     */
    fun getEchoCount(): Int {
        return visualEchoes.size
    }
    
    /**
     * clear all echoes
     */
    fun clearEchoes() {
        visualEchoes.clear()
    }
    
    /**
     * visual echo data class
     */
    private data class VisualEcho(
        val centerX: Float,
        val centerY: Float,
        val size: Float,
        var currentSize: Float = size,
        var lifespan: Float,
        val maxLifespan: Float,
        val shapeType: Int,  // 0=rectangle, 1=circle, 2=triangle, 3=star, 4=polygon
        val color: Int,
        var rotation: Float,
        val rotationSpeed: Float,
        var offsetX: Float = 0f,
        var offsetY: Float = 0f,
        var speedX: Float,
        var speedY: Float,
        val opacity: Float,
        var currentOpacity: Float = opacity
    )
} 