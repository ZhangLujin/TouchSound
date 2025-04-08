package com.example.musicvibrationapp.view.spectrum.renderers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.musicvibrationapp.view.EmotionColorManager
import com.example.musicvibrationapp.view.SpectrumParameters
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Fractal renderer for fear emotions
 * Representation of fear emotions using sharp irregular fractal lines, integrated SpectrumParameters parameter system
 */
class FearFractalRenderer(context: Context) : BaseEmotionRenderer(context) {
    // Fractal Line System
    private val fractalLines = mutableListOf<FractalLine>()
    private val fractalPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    // electric current particle system
    private val currentParticles = mutableListOf<CurrentParticle>()
    private val particlePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    // lightning effect
    private val lightningFlashes = mutableListOf<LightningFlash>()
    private val flashPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    // background crack system
    private val backgroundCracks = mutableListOf<BackgroundCrack>()
    private val crackPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    
    // fear accumulator - gradually accumulates with sustained high energy or strong rhythm
    private var fearAccumulator = 0f
    private val maxFearAccumulation = 1.0f
    
    // distortion system
    private var distortionTime = 0f
    private var distortionIntensity = 0f
    
    // noise generation helper function
    private val noiseOffsetX = Random.nextFloat() * 1000f
    private val noiseOffsetY = Random.nextFloat() * 1000f
    
    /**
     * process spectrum data
     */
    override fun onProcessData(processedData: FloatArray): FloatArray {
        // update distortion time
        distortionTime += deltaTime * 0.5f
        
        // analyze fear features
        val fearFeatures = analyzeFearFeatures(processedData)
        
        // update fear accumulator
        updateFearAccumulator(fearFeatures)
        
        // update existing fractal lines
        updateFractalLines(deltaTime)
        
        // update current particles
        updateCurrentParticles(deltaTime)
        
        // update lightning effect
        updateLightningFlashes(deltaTime)
        
        // update background cracks
        updateBackgroundCracks(deltaTime)
        
        // generate new fractal lines based on rhythm intensity
        generateFractals(processedData, isRhythmPeak)
        
        // improved rhythm burst detection 
        if (isRhythmPeak || (totalEnergy > 0.5f && volumeChange > 0.1f)) {  // strong rhythm condition
            // strong rhythm burst
            generateFractalBurst(totalEnergy, volumeChange, 3)  // intensity level 3
            
            // add lightning effect at strong rhythm peak
            generateLightningFlash(totalEnergy, volumeChange, 3)
            
            // add background crack at strong rhythm peak
            generateBackgroundCrack(totalEnergy, 3)
            
            // increase distortion intensity at strong rhythm peak
            distortionIntensity = min(distortionIntensity + 0.3f, 1.0f)
        } 
        else if (totalEnergy > 0.4f && volumeChange > 0.08f) {  // medium rhythm condition, lower threshold
            // medium rhythm burst
            generateFractalBurst(totalEnergy, volumeChange, 2)  // intensity level 2
            
            // add lightning effect at medium rhythm peak
            generateLightningFlash(totalEnergy, volumeChange, 2)
            
            // add background crack at medium rhythm peak
            generateBackgroundCrack(totalEnergy, 2)
            
            // increase distortion intensity at medium rhythm peak
            distortionIntensity = min(distortionIntensity + 0.15f, 0.7f)
        }
        else if (totalEnergy > 0.3f && volumeChange > 0.04f) {  // mild rhythm condition, lower threshold
            // mild rhythm burst
            generateFractalBurst(totalEnergy, volumeChange, 1)  // intensity level 1
            
            // add lightning effect at mild rhythm peak
            generateLightningFlash(totalEnergy, volumeChange, 1)
            
            // increase distortion intensity at mild rhythm peak
            distortionIntensity = min(distortionIntensity + 0.05f, 0.4f)
        }
        else if (totalEnergy > 0.25f && Random.nextFloat() < 0.25f) {
            generateFractalBurst(totalEnergy, volumeChange, 0)  // weak burst
            
            // add weak lightning
            if (Random.nextFloat() < 0.5f) {
                generateLightningFlash(totalEnergy, volumeChange, 0)
            }
        }
        
        // distortion intensity decays naturally over time  
        distortionIntensity *= (1f - deltaTime * 1.5f)
        
        return processedData
    }
    
    /**
     * render spectrum
     */
    override fun onRender(canvas: Canvas, data: FloatArray, width: Int, height: Int, isVertical: Boolean) {
        val maxWidth = width.toFloat()
        val maxHeight = height.toFloat()
        
        // add volume check - if volume is almost 0, do not render anything
        if (currentVolume < 0.01f) {
            // volume too small, do not render anything, keep transparent
            return
        }
        
        // draw gradient background, not pure black
        drawGradientBackground(canvas, maxWidth, maxHeight)
        
        // apply overall distortion effect
        if (distortionIntensity > 0.05f) {
            applyScreenDistortion(canvas, distortionIntensity)
        }
        
        // modify rendering order - draw background cracks first
        drawBackgroundCracks(canvas, maxWidth, maxHeight, isVertical)
        
        // draw quantum collapse effect (replace original fractal lines)
        drawQuantumCollapse(canvas, maxWidth, maxHeight, isVertical)
        
        // draw lightning effect
        drawLightningFlashes(canvas, maxWidth, maxHeight, isVertical)
        
        // draw current particles (top layer)
        drawCurrentParticles(canvas, maxWidth, maxHeight, isVertical)
    }
    
    /**
     * draw gradient background
     */
    private fun drawGradientBackground(canvas: Canvas, width: Float, height: Float) {
        // create dark gradient background - deep blue purple to deep gray, more mysterious
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width, height,
                intArrayOf(
                    Color.rgb(8, 5, 20),      // deeper blue purple
                    Color.rgb(15, 10, 35),    // deep blue purple
                    Color.rgb(12, 8, 25),     // deep blue
                    Color.rgb(5, 3, 15)       // deeper gray purple
                ),
                floatArrayOf(0f, 0.3f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width, height, bgPaint)
        
        // add subtle noise texture - simulate star field effect
        val noisePaint = Paint().apply {
            color = Color.argb(10, 255, 255, 255)  // reduce opacity
            style = Paint.Style.FILL
        }
        
        // draw random noise points
        val random = Random
        repeat(100) {  // increase noise points, simulate star field
            val x = random.nextFloat() * width
            val y = random.nextFloat() * height
            val size = 0.5f + random.nextFloat() * 1.2f  // reduce noise point size
            
            // random adjust brightness, simulate star twinkling
            val brightness = 5 + random.nextInt(10)
            noisePaint.color = Color.argb(brightness, 255, 255, 255)
            
            canvas.drawCircle(x, y, size, noisePaint)
        }
    }
    
    /**
     * draw quantum collapse effect
     */
    private fun drawQuantumCollapse(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        // optimize volume response curve - use non-linear mapping, so even small volume can produce enough effect
        // use square root function to make curve steeper in low volume region, smoother in high volume region
        val volumeResponse = sqrt((currentVolume - 0.01f).coerceIn(0f, 1f))
        
        // use more sensitive volume factor, so even small volume can produce obvious effect
        val volumeFactor = when {
            currentVolume < 0.01f -> 0f // silent
            currentVolume < 0.05f -> volumeResponse * 0.5f // very low volume, slight effect
            currentVolume < 0.1f -> 0.3f + volumeResponse * 0.3f // low volume, basic effect
            currentVolume < 0.2f -> 0.5f + volumeResponse * 0.3f // medium low volume, enhanced effect
            else -> 0.7f + volumeResponse * 0.3f // normal volume, complete effect
        }
        
        // reduce trigger threshold, ensure effect easier to show
        val effectIntensity = (distortionIntensity.coerceAtLeast(0.2f) * volumeFactor.coerceAtLeast(0.3f)).coerceAtLeast(0.1f)
        
        // if volume is too small, reduce effect intensity
        if (volumeFactor < 0.1f) {
            return // too small volume, do not show quantum collapse effect
        }
        
        // create grid structure - inspired by TrustWaveRenderer's network implementation
        val gridSize = 6 // grid size
        val cellWidth = width / gridSize
        val cellHeight = height / gridSize
        
        // create grid points
        val gridPoints = mutableListOf<Pair<Float, Float>>()
        for (i in 0..gridSize) {
            for (j in 0..gridSize) {
                // basic grid points
                var x = i * cellWidth
                var y = j * cellHeight
                
                // add distortion - reduce threshold, increase base distortion amount, optimize volume response
                // even in low volume, keep some distortion effect
                val distortAmount = (0.05f + effectIntensity * 0.15f) * width * volumeFactor.coerceAtLeast(0.3f)
                val rhythmDistort = (0.02f + rhythmIntensity * volumeChange * 0.1f) * width * volumeFactor.coerceAtLeast(0.2f)
                
                // use simplex noise to create organic distortion
                val noiseX = simplexNoise(x * 0.01f + distortionTime, y * 0.01f) * distortAmount
                val noiseY = simplexNoise(x * 0.01f, y * 0.01f + distortionTime) * distortAmount
                
                // apply distortion
                x += noiseX + (Random.nextFloat() - 0.5f) * rhythmDistort
                y += noiseY + (Random.nextFloat() - 0.5f) * rhythmDistort
                
                gridPoints.add(Pair(x, y))
            }
        }
        
        // draw grid connection - create quantum grid effect
        val gridPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 1.5f
        }
        
        // traverse grid points, connect adjacent points
        for (i in 0 until gridPoints.size) {
            val point1 = gridPoints[i]
            
            // get base color
            val baseColor = ensureDarkColor(getEmotionColor(), 0.7f + Random.nextFloat() * 0.3f)
            
            // Apply flicker effects - create flickers using distortionTime to optimize volume response
            val flickerFactor = (sin(distortionTime * 5f + i * 0.1f) * 0.5f + 0.5f) * 
                               (0.7f + effectIntensity * 0.3f)
            // increase base opacity, ensure effect visible, optimize volume response
            // even in low volume, keep some opacity
            val alpha = ((150 + flickerFactor * 105) * volumeFactor.coerceAtLeast(0.4f)).toInt().coerceIn(0, 255)
            
            // set color
            gridPaint.color = Color.argb(
                alpha,
                Color.red(baseColor),
                Color.green(baseColor),
                Color.blue(baseColor)
            )
            
            for (j in i + 1 until gridPoints.size) {
                val point2 = gridPoints[j]
                
                // calculate distance
                val dx = point2.first - point1.first
                val dy = point2.second - point1.second
                val distance = sqrt(dx * dx + dy * dy)
                
                // increase max connection distance, ensure enough connection lines
                val maxDistance = if (isVertical) {
                    height * 0.25f  // increase connection distance
                } else {
                    width * 0.25f   // increase connection distance
                }
                
                if (distance < maxDistance) {
                    // the closer the distance, the less opaque the line
                    val distanceAlpha = (1.0f - distance / maxDistance) * alpha
                    
                    // set line color
                    gridPaint.color = Color.argb(
                        distanceAlpha.toInt().coerceIn(0, 255),
                        Color.red(baseColor),
                        Color.green(baseColor),
                        Color.blue(baseColor)
                    )
                    
                    // draw connection line
                    canvas.drawLine(point1.first, point1.second, point2.first, point2.second, gridPaint)
                    
                    // add collapse effect at strong rhythm peak - line breaks, optimize volume response
                    // reduce break threshold, even in low volume, produce certain break effect
                    if (effectIntensity > 0.15f && Random.nextFloat() < effectIntensity * 0.6f) {
                        // calculate break point
                        val breakPoint = Random.nextFloat() * 0.4f + 0.3f 
                        val breakX = point1.first + dx * breakPoint
                        val breakY = point1.second + dy * breakPoint
                        
                        // break offset - increase with rhythm intensity, optimize volume response
                        val breakOffset = effectIntensity * 5f * volumeFactor.coerceAtLeast(0.3f)
                        val offsetX = (Random.nextFloat() - 0.5f) * breakOffset
                        val offsetY = (Random.nextFloat() - 0.5f) * breakOffset
                        
                        // draw broken two lines
                        canvas.drawLine(point1.first, point1.second, breakX, breakY, gridPaint)
                        canvas.drawLine(breakX + offsetX, breakY + offsetY, point2.first, point2.second, gridPaint)
                        
                        // add particle effect at break point - increase particle generation probability, optimize volume response
                        if (Random.nextFloat() < 0.5f * volumeFactor.coerceAtLeast(0.3f)) {
                            val particlePaint = Paint().apply {
                                isAntiAlias = true
                                style = Paint.Style.FILL
                                color = Color.argb(
                                    (distanceAlpha * 1.5f).toInt().coerceIn(0, 255),
                                    Color.red(baseColor) + 30,
                                    Color.green(baseColor) + 30,
                                    Color.blue(baseColor) + 50
                                )
                            }
                            
                            // draw broken particles - increase particle count, optimize volume response
                            val particleCount = (2 + Random.nextInt(4) * volumeFactor.coerceAtLeast(0.5f)).toInt()
                            repeat(particleCount) {
                                val particleX = breakX + (Random.nextFloat() - 0.5f) * 8f
                                val particleY = breakY + (Random.nextFloat() - 0.5f) * 8f
                                val particleSize = 1.5f + Random.nextFloat() * 2.5f * volumeFactor.coerceAtLeast(0.4f)
                                
                                canvas.drawCircle(particleX, particleY, particleSize, particlePaint)
                            }
                        }
                    }
                }
            }
        }
        
        val visibleLines = fractalLines.filter { it.intensity > 0.3f * volumeFactor.coerceAtLeast(0.4f) }
        
        // draw fractal lines - as energy lines of quantum collapse
        visibleLines.forEach { fractalLine ->
            // line width related to intensity, keep reasonable range, optimize volume response
            val baseWidth = (2.0f + fractalLine.intensity * 6.0f) * volumeFactor.coerceAtLeast(0.4f)
            fractalPaint.strokeWidth = baseWidth
            
            // get base color
            val baseColor = ensureDarkColor(getEmotionColor(), fractalLine.intensity)
            
            // Apply flicker effects - increase base opacity, optimize volume response
            val flickerFactor = fractalLine.flickerIntensity * volumeFactor.coerceAtLeast(0.5f)
            val alpha = ((180 + flickerFactor * 75) * volumeFactor.coerceAtLeast(0.5f)).toInt().coerceIn(0, 255)
            
            // set color
            fractalPaint.color = Color.argb(
                alpha,
                Color.red(baseColor),
                Color.green(baseColor),
                Color.blue(baseColor)
            )
            
            // create path
            val path = Path()
            
            // if not enough segments, skip
            if (fractalLine.segments.size < 2) return@forEach
            
            // get first segment's start point
            val firstSegment = fractalLine.segments.first()
            val startX = if (isVertical) firstSegment.first.second * width else firstSegment.first.first * width
            val startY = if (isVertical) firstSegment.first.first * height else firstSegment.first.second * height
            
            // move to start point
            path.moveTo(startX, startY)
            
            // create quantum collapse effect - line breaks and distortion, optimize volume response
            for (i in 0 until fractalLine.segments.size - 1) {
                val current = fractalLine.segments[i]
                val next = fractalLine.segments[i + 1]
                
                val currentEnd = if (isVertical) 
                    Pair(current.second.second * width, current.second.first * height)
                else 
                    Pair(current.second.first * width, current.second.second * height)
                    
                val nextStart = if (isVertical)
                    Pair(next.first.second * width, next.first.first * height)
                else
                    Pair(next.first.first * width, next.first.second * height)
                
                // apply quantum collapse effect - reduce break threshold, increase break probability, optimize volume response
                // even in low volume, produce certain break effect
                if (effectIntensity > 0.15f && Random.nextFloat() < effectIntensity * 0.7f) {
                    // draw current segment
                    path.lineTo(currentEnd.first, currentEnd.second)
                    
                    // end current path and draw
                    canvas.drawPath(path, fractalPaint)
                    
                    // add collapse particles at break point - increase particle generation probability, optimize volume response
                    if (Random.nextFloat() < 0.7f * volumeFactor.coerceAtLeast(0.4f)) {
                        val particlePaint = Paint().apply {
                            isAntiAlias = true
                            style = Paint.Style.FILL
                            color = Color.argb(
                                (alpha * 1.2f).toInt().coerceIn(0, 255),
                                Color.red(baseColor) + 40,
                                Color.green(baseColor) + 40,
                                Color.blue(baseColor) + 60
                            )
                        }
                        
                        // draw collapse particles - increase particle count and size, optimize volume response
                        val particleCount = (3 + Random.nextInt(5) * volumeFactor.coerceAtLeast(0.5f)).toInt()
                        repeat(particleCount) {
                            val particleX = currentEnd.first + (Random.nextFloat() - 0.5f) * 12f
                            val particleY = currentEnd.second + (Random.nextFloat() - 0.5f) * 12f
                            val particleSize = 2f + Random.nextFloat() * 3f * volumeFactor.coerceAtLeast(0.4f)
                            
                            canvas.drawCircle(particleX, particleY, particleSize, particlePaint)
                        }
                    }
                    
                    // start new path
                    path.reset()
                    path.moveTo(nextStart.first, nextStart.second)
                } else {
                    // normal connection - use bezier curve to create smooth transition
                    val controlX1 = currentEnd.first + (nextStart.first - currentEnd.first) * 0.5f
                    val controlY1 = currentEnd.second + (nextStart.second - currentEnd.second) * 0.5f
                    
                    // add distortion - increase base distortion amount, optimize volume response
                    val distortAmount = (0.05f + effectIntensity * 0.1f) * width * volumeFactor.coerceAtLeast(0.3f)
                    val noiseX = simplexNoise(controlX1 * 0.01f + distortionTime, controlY1 * 0.01f) * distortAmount
                    val noiseY = simplexNoise(controlX1 * 0.01f, controlY1 * 0.01f + distortionTime) * distortAmount
                    
                    path.quadTo(
                        controlX1 + noiseX, 
                        controlY1 + noiseY, 
                        nextStart.first, 
                        nextStart.second
                    )
                }
            }
            
            // draw last segment
            canvas.drawPath(path, fractalPaint)
            
            // add glow effect - reduce threshold, increase glow probability, optimize volume response
            if (fractalLine.intensity > 0.4f * volumeFactor.coerceAtLeast(0.5f)) {
                val glowPaint = Paint(fractalPaint)
                glowPaint.strokeWidth = baseWidth * 0.6f
                glowPaint.color = Color.argb(
                    (alpha * 0.5f).toInt(),
                    Color.red(baseColor),
                    Color.green(baseColor),
                    Color.blue(baseColor)
                )
                
                // draw outer glow
                canvas.drawPath(path, glowPaint)
            }
        }
        
        // draw collapse center points - optimize volume response
        // reduce display threshold, even in low volume, show basic effect
        if (volumeFactor > 0.15f) {  // reduce volume threshold
            // create collapse center points
            val centerPoints = mutableListOf<Pair<Float, Float>>()
            
            // add fixed collapse center points, ensure effect visible
            centerPoints.add(Pair(width * 0.5f, height * 0.5f))
            centerPoints.add(Pair(width * 0.3f, height * 0.3f))
            centerPoints.add(Pair(width * 0.7f, height * 0.7f))
            
            // draw collapse center points
            val centerPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
            
            centerPoints.forEach { center ->
                // get base color - use brighter color
                val centerColor = enhanceParticleColor(getEmotionColor(), 0.8f + Random.nextFloat() * 0.2f)
                
                // create radial gradient - increase base radius, ensure effect visible, optimize volume response
                // even in low volume, keep certain radius
                val centerRadius = (15f + effectIntensity * 20f) * (0.8f + Random.nextFloat() * 0.4f) * volumeFactor.coerceAtLeast(0.4f)
                val gradient = RadialGradient(
                    center.first,
                    center.second,
                    centerRadius,
                    intArrayOf(
                        Color.WHITE,
                        centerColor,
                        Color.argb(150, Color.red(centerColor), Color.green(centerColor), Color.blue(centerColor)),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.3f, 0.6f, 1f),
                    Shader.TileMode.CLAMP
                )
                
                centerPaint.shader = gradient
                
                // draw collapse center
                canvas.drawCircle(center.first, center.second, centerRadius, centerPaint)
                
                // add energy rays - reduce threshold, increase ray probability, optimize volume response
                // even in low volume, produce certain ray effect
                if (effectIntensity > 0.15f && volumeFactor > 0.2f) {  // reduce volume threshold
                    val rayPaint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f + Random.nextFloat() * 2f * volumeFactor.coerceAtLeast(0.3f)
                        color = Color.argb(
                            (180 * effectIntensity * volumeFactor.coerceAtLeast(0.5f)).toInt().coerceIn(0, 255),
                            Color.red(centerColor),
                            Color.green(centerColor),
                            Color.blue(centerColor)
                        )
                    }
                    
                    // create rays - increase ray count, optimize volume response
                    val rayCount = (4 + Random.nextInt(5) * volumeFactor.coerceAtLeast(0.5f)).toInt()
                    repeat(rayCount) {
                        val angle = Random.nextFloat() * Math.PI.toFloat() * 2
                        val rayLength = centerRadius * (1.5f + Random.nextFloat() * 2f)
                        
                        val endX = center.first + cos(angle) * rayLength
                        val endY = center.second + sin(angle) * rayLength
                        
                        // draw ray
                        canvas.drawLine(center.first, center.second, endX, endY, rayPaint)
                    }
                }
                
                centerPaint.shader = null
            }
        }
    }
    
    /**
     * analyze fear features - extract features related to fear from audio
     */
    private fun analyzeFearFeatures(data: FloatArray): Float {
        // analyze high frequency sharp sounds (common in horror effects)
        val highFreqEnergy = analyzeFrequencyRange(data, 0.7f, 1.0f)
        
        // analyze rapid volume changes (sudden sounds)
        val volumeTransients = volumeChange.coerceIn(0f, 0.3f) / 0.3f
        
        // analyze dissonant intervals (dissonant sounds are common in horror music)
        val dissonance = energyConcentration.coerceIn(0f, 1f)
        
        return (highFreqEnergy * 0.4f + volumeTransients * 0.4f + dissonance * 0.2f).coerceIn(0f, 1f)
    }
    
    /**
     * analyze energy in specific frequency range
     */
    private fun analyzeFrequencyRange(data: FloatArray, startPercent: Float, endPercent: Float): Float {
        val startIdx = (data.size * startPercent).toInt().coerceIn(0, data.size - 1)
        val endIdx = (data.size * endPercent).toInt().coerceIn(0, data.size - 1)
        
        var sum = 0f
        for (i in startIdx..endIdx) {
            sum += data[i]
        }
        
        return (sum / (endIdx - startIdx + 1)).coerceIn(0f, 1f)
    }
    
    /**
     * update fear accumulator
     */
    private fun updateFearAccumulator(fearFeatures: Float) {
        // update fear accumulator based on fear features and rhythm intensity
        val accumRate = when (rhythmIntensity) {
            3 -> 0.4f  // strong rhythm
            2 -> 0.2f  // medium rhythm
            1 -> 0.1f  // mild rhythm
            else -> 0.05f  // no rhythm
        }
        
        // accumulate fear
        fearAccumulator += fearFeatures * accumRate * deltaTime
        
        // limit maximum value
        fearAccumulator = fearAccumulator.coerceIn(0f, maxFearAccumulation)
        
        // natural decay
        fearAccumulator *= (1f - deltaTime * 0.2f)
        
        // when accumulated value reaches threshold, trigger fear explosion
        if (fearAccumulator > 0.8f && Random.nextFloat() < 0.3f * deltaTime) {
            generateFearExplosion()
            fearAccumulator *= 0.5f  // after explosion, reduce accumulated value
        }
    }
    
    /**
     * generate fear explosion effect
     */
    private fun generateFearExplosion() {
        // generate fractal explosion in multiple positions
        repeat(5) {
            val x = Random.nextFloat()
            val y = Random.nextFloat()
            
            // generate large fractal
            generateFractalAt(x, y, 0.8f + Random.nextFloat() * 0.2f, 5, 0.7f)
            
            // generate lightning effect
            lightningFlashes.add(LightningFlash(
                x = x,
                y = y,
                radius = 0.3f + Random.nextFloat() * 0.2f,
                intensity = 0.8f + Random.nextFloat() * 0.2f,
                lifespan = 0.3f + Random.nextFloat() * 0.2f,
                maxLifespan = 0.5f
            ))
        }
        
        // increase distortion intensity
        distortionIntensity = 1.0f
    }
    
    /**
     * update fractal lines
     */
    private fun updateFractalLines(deltaTime: Float) {
        val fallSpeed = SpectrumParameters.fallSpeed.value
        
        fractalLines.removeAll { fractalLine ->
            // apply lifespan decay
            fractalLine.lifespan -= deltaTime * (1.0f + fallSpeed * 3.0f)
            
            // update flicker effect
            fractalLine.flickerPhase += deltaTime * (5.0f + Random.nextFloat() * 5.0f)
            fractalLine.flickerIntensity = (sin(fractalLine.flickerPhase * 10f) * 0.5f + 0.5f) * 
                                          fractalLine.lifespan / fractalLine.maxLifespan
            
            // update distortion effect
            fractalLine.distortionPhase += deltaTime * (1.0f + Random.nextFloat() * 2.0f)
            
            // update branch growth
            if (fractalLine.isGrowing && fractalLine.segments.size < fractalLine.maxSegments) {
                fractalLine.growthTimer -= deltaTime
                if (fractalLine.growthTimer <= 0) {
                    growFractalSegment(fractalLine)
                    fractalLine.growthTimer = fractalLine.growthInterval
                }
            }
            
            // significantly increase probability and number of current particles generation
            if (Random.nextFloat() < 0.6f * deltaTime * fractalLine.intensity) {
                // generate multiple particles
                repeat(1 + (fractalLine.intensity * 2).toInt()) {
                    generateCurrentParticle(fractalLine)
                }
            }
            
            // line disappearance condition
            fractalLine.lifespan <= 0
        }
    }
    
    /**
     * grow fractal segment
     */
    private fun growFractalSegment(fractalLine: FractalLine) {
        if (fractalLine.segments.isEmpty()) return
        
        // get last segment
        val lastSegment = fractalLine.segments.last()
        val lastPoint = lastSegment.second
        
        // calculate new growth direction
        val baseAngle = getAngle(lastSegment.first, lastSegment.second)
        val angleVariation = (Random.nextFloat() - 0.5f) * fractalLine.angleVariation
        val newAngle = baseAngle + angleVariation
        
        // calculate new segment length
        val newLength = fractalLine.baseLength * (0.8f + Random.nextFloat() * 0.4f)
        
        // calculate new end point
        val endX = lastPoint.first + cos(newAngle) * newLength
        val endY = lastPoint.second + sin(newAngle) * newLength
        
        // add new segment
        fractalLine.segments.add(Pair(lastPoint, Pair(endX, endY)))
        
        // random branching
        if (Random.nextFloat() < fractalLine.branchProbability && 
            fractalLine.branches.size < fractalLine.maxBranches) {
            
            // create branch
            val branchAngleOffset = (Random.nextFloat() - 0.5f) * Math.PI.toFloat() * 0.5f
            val branchAngle = newAngle + branchAngleOffset
            val branchLength = newLength * 0.7f
            
            val branchEndX = lastPoint.first + cos(branchAngle) * branchLength
            val branchEndY = lastPoint.second + sin(branchAngle) * branchLength
            
            // add branch starting segment
            val branch = FractalBranch(
                startPoint = lastPoint,
                segments = mutableListOf(Pair(lastPoint, Pair(branchEndX, branchEndY))),
                branchProbability = fractalLine.branchProbability * 0.7f,
                maxSegments = (fractalLine.maxSegments * 0.6f).toInt(),
                growthTimer = fractalLine.growthInterval * 1.2f,
                growthInterval = fractalLine.growthInterval * 1.2f,
                angleVariation = fractalLine.angleVariation * 1.2f,
                baseLength = fractalLine.baseLength * 0.7f
            )
            
            fractalLine.branches.add(branch)
        }
        
        // update branches
        fractalLine.branches.forEach { branch ->
            if (branch.segments.size < branch.maxSegments && Random.nextFloat() < 0.7f) {
                branch.growthTimer -= deltaTime
                if (branch.growthTimer <= 0) {
                    growBranchSegment(branch)
                    branch.growthTimer = branch.growthInterval
                }
            }
        }
    }
    
    /**
     * grow branch segment
     */
    private fun growBranchSegment(branch: FractalBranch) {
        if (branch.segments.isEmpty()) return
        
        // get last segment
        val lastSegment = branch.segments.last()
        val lastPoint = lastSegment.second
        
        // calculate new growth direction
        val baseAngle = getAngle(lastSegment.first, lastSegment.second)
        val angleVariation = (Random.nextFloat() - 0.5f) * branch.angleVariation
        val newAngle = baseAngle + angleVariation
        
        // calculate new segment length
        val newLength = branch.baseLength * (0.7f + Random.nextFloat() * 0.3f)
        
        // calculate new end point
        val endX = lastPoint.first + cos(newAngle) * newLength
        val endY = lastPoint.second + sin(newAngle) * newLength
        
        // add new segment
        branch.segments.add(Pair(lastPoint, Pair(endX, endY)))
    }
    
    /**
     * calculate angle between two points
     */
    private fun getAngle(start: Pair<Float, Float>, end: Pair<Float, Float>): Float {
        val dx = end.first - start.first
        val dy = end.second - start.second
        return kotlin.math.atan2(dy, dx)
    }
    
    /**
     * update current particles
     */
    private fun updateCurrentParticles(deltaTime: Float) {
        currentParticles.removeAll { particle ->
            // update lifespan
            particle.lifespan -= deltaTime * 2.0f
            
            // update position on line
            particle.progress += particle.speed * deltaTime
            
            // update flicker effect
            particle.flickerPhase += deltaTime * 10f
            particle.intensity = (sin(particle.flickerPhase) * 0.5f + 0.5f) * 
                               particle.lifespan / particle.maxLifespan
            
            // remove when particle reaches line end or lifespan ends
            particle.progress >= 1f || particle.lifespan <= 0
        }
    }
    
    /**
     * generate current particles
     */
    private fun generateCurrentParticle(fractalLine: FractalLine) {
        if (fractalLine.segments.isEmpty()) return
        
        // increase particle generation probability
        if (Random.nextFloat() < 0.4f * fractalLine.intensity) {
            // randomly select a segment
            val segmentIndex = Random.nextInt(fractalLine.segments.size)
            val segment = fractalLine.segments[segmentIndex]
            
            // create particle, increase size and lifespan
            currentParticles.add(CurrentParticle(
                lineIndex = fractalLine.id,
                segmentIndex = segmentIndex,
                progress = 0f,
                speed = 0.8f + Random.nextFloat() * 1.5f,  // increase speed
                size = 0.3f + Random.nextFloat() * 0.3f,   // increase size
                intensity = 0.8f + Random.nextFloat() * 0.2f,  // increase intensity
                lifespan = 0.5f + Random.nextFloat() * 0.5f,   // increase lifespan
                maxLifespan = 1.0f,
                flickerPhase = Random.nextFloat() * 10f
            ))
        }
    }
    
    /**
     * update lightning effect
     */
    private fun updateLightningFlashes(deltaTime: Float) {
        lightningFlashes.removeAll { flash ->
            // update lifespan
            flash.lifespan -= deltaTime * 3.0f
            
            // update flicker effect
            flash.flickerPhase += deltaTime * 20f
            flash.intensity = (sin(flash.flickerPhase) * 0.3f + 0.7f) * 
                            flash.lifespan / flash.maxLifespan
            
            // remove when lifespan ends
            flash.lifespan <= 0
        }
    }
    
    /**
     * update background cracks
     */
    private fun updateBackgroundCracks(deltaTime: Float) {
        val fallSpeed = SpectrumParameters.fallSpeed.value
        
        backgroundCracks.removeAll { crack ->
            // apply lifespan decay
            crack.lifespan -= deltaTime * (0.2f + fallSpeed * 0.5f)
            
            // update flicker effect
            crack.flickerPhase += deltaTime * 2f
            crack.flickerIntensity = (sin(crack.flickerPhase * 5f) * 0.3f + 0.7f) * 
                                   crack.lifespan / crack.maxLifespan
            
            // update growth
            if (crack.isGrowing && crack.points.size < crack.maxPoints) {
                crack.growthTimer -= deltaTime
                if (crack.growthTimer <= 0) {
                    growCrackPoint(crack)
                    crack.growthTimer = crack.growthInterval
                }
            }
            
            // crack disappearance condition
            crack.lifespan <= 0
        }
    }
    
    /**
     * grow crack point
     */
    private fun growCrackPoint(crack: BackgroundCrack) {
        if (crack.points.isEmpty()) return
        
        // get last point
        val lastPoint = crack.points.last()
        
        // calculate new growth direction
        val angle = crack.baseAngle + (Random.nextFloat() - 0.5f) * crack.angleVariation
        
        // calculate new point
        val distance = crack.baseLength * (0.8f + Random.nextFloat() * 0.4f)
        val newX = lastPoint.first + cos(angle) * distance
        val newY = lastPoint.second + sin(angle) * distance
        
        // add new point
        crack.points.add(Pair(newX, newY))
        
        // update base angle
        crack.baseAngle = angle
    }
    
    /**
     * generate fractals
     */
    private fun generateFractals(data: FloatArray, isRhythmPeak: Boolean) {
        // use minThreshold and soloResponseStrength to affect fractal generation
        val minThreshold = SpectrumParameters.minThreshold.value
        val soloResponseStrength = SpectrumParameters.soloResponseStrength.value
        
        // limit fractal total number - significantly reduce maximum number, improve quality
        val maxFractals = (20 + (SpectrumParameters.melSensitivity.value * 20).toInt())
        if (fractalLines.size >= maxFractals) return
        
        // increase fractal generation probability when rhythm peak
        val rhythmMultiplier = if (isRhythmPeak) 3.0f else 1.0f
        val volumeInfluence = currentVolume * 1.5f + 0.3f
        
        // reduce random generation probability, more concentrated on meaningful frequencies
        var generatedCount = 0
        
        // generate fractals based on spectrum energy
        data.forEachIndexed { index, magnitude ->
            // use minThreshold as generation threshold, increase threshold to reduce messy lines
            val dynamicThreshold = minThreshold * (if (isRhythmPeak) 0.7f else 1.1f)
            
            // reduce generation probability
            val generationProbability = magnitude * 0.15f * rhythmMultiplier * volumeInfluence
            
            if (magnitude > dynamicThreshold && Random.nextFloat() < generationProbability && generatedCount < 2) {
                // generate fractals with different characteristics based on frequency index
                val fractalIntensity = when {
                    index < data.size * 0.2 -> 0.7f + Random.nextFloat() * 0.3f  // low frequency - strong fractals
                    index < data.size * 0.6 -> 0.5f + Random.nextFloat() * 0.3f  // medium frequency - medium fractals
                    else -> 0.3f + Random.nextFloat() * 0.2f  // high frequency - weak fractals
                }
                
                // generate position - more concentrated in the center of the screen
                val x = 0.3f + Random.nextFloat() * 0.4f
                val y = 0.3f + Random.nextFloat() * 0.4f
                
                // generate fractals
                generateFractalAt(x, y, fractalIntensity, 
                                 (3 + fractalIntensity * 4).toInt(), 
                                 0.3f + fractalIntensity * 0.3f)
                
                generatedCount++
            }
        }
    }
    
    /**
     * generate fractals at specified position
     */
    private fun generateFractalAt(x: Float, y: Float, intensity: Float, maxSegments: Int, branchProbability: Float) {
        // create initial line - use more directional design
        val startX = x
        val startY = y
        
        // determine growth direction based on position - from center to outside
        val centerX = 0.5f
        val centerY = 0.5f
        val dx = x - centerX
        val dy = y - centerY
        
        // calculate base angle - from center to outside
        val baseAngle = if (dx != 0f || dy != 0f) {
            kotlin.math.atan2(dy, dx)
        } else {
            Random.nextFloat() * Math.PI.toFloat() * 2
        }
        
        // add random variation
        val angle = baseAngle + (Random.nextFloat() - 0.5f) * Math.PI.toFloat() * 0.3f
        
        // adjust length - longer initial line
        val length = 0.1f + intensity * 0.05f
        val endX = startX + cos(angle) * length
        val endY = startY + sin(angle) * length
        
        // create fractal line, increase base parameters
        val fractalLine = FractalLine(
            id = Random.nextInt(),
            startPoint = Pair(startX, startY),
            segments = mutableListOf(Pair(Pair(startX, startY), Pair(endX, endY))),
            branches = mutableListOf(),
            intensity = intensity,
            lifespan = 1.0f + intensity * 2.0f,  // increase lifespan
            maxLifespan = 1.0f + intensity * 2.0f,
            flickerPhase = Random.nextFloat() * 10f,
            flickerIntensity = 1.0f,
            distortionPhase = Random.nextFloat() * 10f,
            isGrowing = true,
            growthTimer = 0.02f,
            growthInterval = 0.04f + (1.0f - intensity) * 0.04f,  // adjust growth interval
            branchProbability = branchProbability,
            maxSegments = maxSegments,
            maxBranches = (maxSegments / 2).coerceAtLeast(1),  // increase maximum branches
            angleVariation = 0.4f + (1.0f - intensity) * 0.4f,  // adjust angle variation
            baseLength = 0.05f + intensity * 0.04f  // increase base length
        )
        
        fractalLines.add(fractalLine)
    }
    
    /**
     * generate fractal burst
     */
    private fun generateFractalBurst(energy: Float, volumeChange: Float, intensityLevel: Int = 1) {
        // use soloResponseStrength to affect burst effect
        val responseStrength = SpectrumParameters.soloResponseStrength.value
        
        // generate position - concentrated in the visual focus area
        val focusPoints = listOf(
            Pair(0.3f, 0.3f),
            Pair(0.7f, 0.3f),
            Pair(0.5f, 0.7f)
        )
        
        // select a focus point
        val focusPoint = focusPoints[Random.nextInt(focusPoints.size)]
        val burstX = focusPoint.first + (Random.nextFloat() - 0.5f) * 0.2f
        val burstY = focusPoint.second + (Random.nextFloat() - 0.5f) * 0.2f
        
        // adjust fractal number based on intensity level - reduce number, improve quality
        val baseCount = when (intensityLevel) {
            3 -> (energy * 8).toInt().coerceIn(4, 8)  // strong rhythm
            2 -> (energy * 5).toInt().coerceIn(3, 6)  // medium rhythm
            1 -> (energy * 3).toInt().coerceIn(2, 4)  // mild rhythm
            else -> (energy * 2).toInt().coerceIn(1, 2) // weak burst
        }
        
        // volume change affects fractal number
        val volumeBoost = when (intensityLevel) {
            3 -> (volumeChange * 10).toInt().coerceIn(3, 8)
            2 -> (volumeChange * 8).toInt().coerceIn(2, 5)
            1 -> (volumeChange * 5).toInt().coerceIn(1, 3)
            else -> (volumeChange * 2).toInt().coerceIn(0, 1)
        }
        
        val fractalCount = baseCount + volumeBoost
        
        // create burst effect - use more organized pattern
        val angleStep = (Math.PI * 2).toFloat() / fractalCount
        
        repeat(fractalCount) { i ->
            // use more organized angle distribution
            val baseAngle = i * angleStep
            val angle = baseAngle + (Random.nextFloat() - 0.5f) * angleStep * 0.5f
            
            // adjust distance based on intensity level
            val distance = when (intensityLevel) {
                3 -> 0.08f + Random.nextFloat() * 0.15f
                2 -> 0.05f + Random.nextFloat() * 0.1f
                else -> 0.02f + Random.nextFloat() * 0.08f
            }
            
            // calculate position
            val fractalX = burstX + cos(angle) * distance
            val fractalY = burstY + sin(angle) * distance
            
            // adjust fractal characteristics based on intensity level
            val intensity = when (intensityLevel) {
                3 -> 0.8f + Random.nextFloat() * 0.2f
                2 -> 0.6f + Random.nextFloat() * 0.3f
                1 -> 0.4f + Random.nextFloat() * 0.3f
                else -> 0.3f + Random.nextFloat() * 0.2f
            }
            
            // adjust branch probability based on intensity level
            val branchProb = when (intensityLevel) {
                3 -> 0.5f + Random.nextFloat() * 0.2f
                2 -> 0.3f + Random.nextFloat() * 0.2f
                1 -> 0.2f + Random.nextFloat() * 0.1f
                else -> 0.1f + Random.nextFloat() * 0.1f
            }
            
            // adjust maximum segments based on intensity level
            val maxSegs = when (intensityLevel) {
                3 -> (5 + Random.nextInt(3))
                2 -> (4 + Random.nextInt(2))
                1 -> (3 + Random.nextInt(2))
                else -> (2 + Random.nextInt(1))
            }
            
            // generate fractal - use named parameters to avoid type inference problem
            generateFractalAt(
                x = fractalX,
                y = fractalY, 
                intensity = intensity, 
                maxSegments = maxSegs, 
                branchProbability = branchProb
            )
        }
    }
    
    /**
     * 生成闪电效果
     */
    private fun generateLightningFlash(energy: Float, volumeChange: Float, intensityLevel: Int = 1) {
        // generate position - more concentrated in the visual focus area
        val focusPoints = listOf(
            Pair(0.3f, 0.3f),
            Pair(0.7f, 0.3f),
            Pair(0.5f, 0.7f)
        )
        
        // select a focus point
        val focusPoint = focusPoints[Random.nextInt(focusPoints.size)]
        val flashX = focusPoint.first + (Random.nextFloat() - 0.5f) * 0.15f
        val flashY = focusPoint.second + (Random.nextFloat() - 0.5f) * 0.15f
        
        // adjust lightning characteristics based on intensity level
        val radius = when (intensityLevel) {
            3 -> 0.4f + Random.nextFloat() * 0.2f  // decrease main lightning radius, more focused
            2 -> 0.3f + Random.nextFloat() * 0.15f
            1 -> 0.2f + Random.nextFloat() * 0.1f
            else -> 0.15f + Random.nextFloat() * 0.05f
        }
        
        val intensity = when (intensityLevel) {
            3 -> 1.0f
            2 -> 0.9f
            1 -> 0.8f
            else -> 0.7f
        }
        
        val lifespan = when (intensityLevel) {
            3 -> 0.8f + Random.nextFloat() * 0.3f  // decrease lifespan, faster flicker
            2 -> 0.6f + Random.nextFloat() * 0.2f
            1 -> 0.5f + Random.nextFloat() * 0.15f
            else -> 0.4f + Random.nextFloat() * 0.1f
        }
        
        // create main lightning effect
        lightningFlashes.add(LightningFlash(
            x = flashX,
            y = flashY,
            radius = radius,
            intensity = intensity,
            lifespan = lifespan,
            maxLifespan = lifespan,
            flickerPhase = Random.nextFloat() * 10f
        ))
        
        // add branch lightning to main lightning - reduce number, improve quality
        val branchCount = when (intensityLevel) {
            3 -> 3 + Random.nextInt(2)
            2 -> 2 + Random.nextInt(1)
            1 -> 1 + Random.nextInt(1)
            else -> 0
        }
        
        // add branch lightning to main lightning - use more organized distribution
        val angleStep = (Math.PI * 2).toFloat() / branchCount
        
        repeat(branchCount) { i ->
            // use more organized angle distribution
            val baseAngle = i * angleStep
            val angle = baseAngle + (Random.nextFloat() - 0.5f) * angleStep * 0.5f
            
            val distance = radius * (0.6f + Random.nextFloat() * 0.4f)
            
            val branchX = flashX + cos(angle) * distance
            val branchY = flashY + sin(angle) * distance
            
            lightningFlashes.add(LightningFlash(
                x = branchX,
                y = branchY,
                radius = radius * (0.5f + Random.nextFloat() * 0.3f),
                intensity = intensity * 0.9f,
                lifespan = lifespan * 0.7f,
                maxLifespan = lifespan * 0.7f,
                flickerPhase = Random.nextFloat() * 10f
            ))
        }
    }
    
    private fun generateBackgroundCrack(energy: Float, intensityLevel: Int = 1) {
        // if cracks too many, stop generating
        if (backgroundCracks.size > 8) return  // reduce maximum number
        
        // generate position - more concentrated on screen edge
        val crackX: Float
        val crackY: Float
        
        // generate from screen edge
        val side = Random.nextInt(4)
        when (side) {
            0 -> { // top edge
                crackX = Random.nextFloat()
                crackY = 0f
            }
            1 -> { // right edge
                crackX = 1f
                crackY = Random.nextFloat()
            }
            2 -> { // bottom edge
                crackX = Random.nextFloat()
                crackY = 1f
            }
            else -> { // left edge
                crackX = 0f
                crackY = Random.nextFloat()
            }
        }
        
        // adjust crack characteristics based on intensity level
        val maxPoints = when (intensityLevel) {
            3 -> 12 + Random.nextInt(6)
            2 -> 8 + Random.nextInt(4)
            else -> 5 + Random.nextInt(3)
        }
        
        // calculate angle towards screen center
        val centerX = 0.5f
        val centerY = 0.5f
        val dx = centerX - crackX
        val dy = centerY - crackY
        val baseAngle = kotlin.math.atan2(dy, dx)
        
        val angleVariation = when (intensityLevel) {
            3 -> 0.2f + Random.nextFloat() * 0.1f  // decrease variation, more directional
            2 -> 0.3f + Random.nextFloat() * 0.1f
            else -> 0.4f + Random.nextFloat() * 0.2f
        }
        
        val baseLength = when (intensityLevel) {
            3 -> 0.06f + Random.nextFloat() * 0.03f  // increase length
            2 -> 0.05f + Random.nextFloat() * 0.02f
            else -> 0.04f + Random.nextFloat() * 0.02f
        }
        
        val lifespan = when (intensityLevel) {
            3 -> 4.0f + Random.nextFloat() * 2.0f  // increase lifespan
            2 -> 3.0f + Random.nextFloat() * 1.5f
            else -> 2.0f + Random.nextFloat() * 1.0f
        }
        
        // create crack
        backgroundCracks.add(BackgroundCrack(
            points = mutableListOf(Pair(crackX, crackY)),
            baseAngle = baseAngle,
            angleVariation = angleVariation,
            baseLength = baseLength,
            maxPoints = maxPoints,
            lifespan = lifespan,
            maxLifespan = lifespan,
            isGrowing = true,
            growthTimer = 0.05f,
            growthInterval = 0.08f,  // slow growth speed
            flickerPhase = Random.nextFloat() * 10f,
            flickerIntensity = 1.0f
        ))
    }
    
    /**
     * draw current particles
     */
    private fun drawCurrentParticles(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        currentParticles.forEach { particle ->
            // find corresponding fractal line
            val fractalLine = fractalLines.find { it.id == particle.lineIndex } ?: return@forEach
            
            // if segment index out of range, skip
            if (particle.segmentIndex >= fractalLine.segments.size) return@forEach
            
            // get segment
            val segment = fractalLine.segments[particle.segmentIndex]
            
            // calculate particle position on segment
            val startPoint = segment.first
            val endPoint = segment.second
            val x = startPoint.first + (endPoint.first - startPoint.first) * particle.progress
            val y = startPoint.second + (endPoint.second - startPoint.second) * particle.progress
            
            // adjust coordinates based on direction
            val screenX = if (isVertical) y * width else x * width
            val screenY = if (isVertical) x * height else y * height
            
            // apply distortion effect
            val distortionAmount = distortionIntensity * 0.05f * width
            val noiseX = simplexNoise(screenX * 0.01f + distortionTime, screenY * 0.01f) * distortionAmount
            val noiseY = simplexNoise(screenX * 0.01f, screenY * 0.01f + distortionTime) * distortionAmount
            
            // particle size - significantly increase
            val particleSize = particle.size * 30f * (0.8f + particle.intensity * 0.4f)
            
            // get particle color - use brighter electric blue
            val particleColor = enhanceParticleColor(getEmotionColor(), particle.intensity)
            
            // create radial gradient - inner bright core, outer gradient
            val gradient = RadialGradient(
                screenX + noiseX, 
                screenY + noiseY,
                particleSize,
                intArrayOf(
                    Color.WHITE,  // center is white
                    Color.rgb(200, 240, 255),  // light blue white
                    particleColor,
                    Color.argb(
                        100,
                        Color.red(particleColor),
                        Color.green(particleColor),
                        Color.blue(particleColor)
                    ),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.2f, 0.5f, 0.8f, 1f),
                Shader.TileMode.CLAMP
            )
            
            particlePaint.shader = gradient
            
            // draw particle core
            canvas.drawCircle(
                screenX + noiseX, 
                screenY + noiseY, 
                particleSize, 
                particlePaint
            )
            
            // draw particle tail - increase dynamic length
            if (particle.speed > 0.5f) {  // reduce speed threshold, let more particles have tail
                val pathPaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeWidth = particleSize * 0.8f  // increase tail width
                    strokeCap = Paint.Cap.ROUND
                    
                    // tail gradient - increase length
                    val tailLength = particleSize * 4f * particle.speed  // increase tail length
                    val dx = (startPoint.first - endPoint.first) * tailLength / segment.first.distanceTo(segment.second)
                    val dy = (startPoint.second - endPoint.second) * tailLength / segment.first.distanceTo(segment.second)
                    
                    shader = LinearGradient(
                        screenX + noiseX, screenY + noiseY,
                        screenX + noiseX + dx, screenY + noiseY + dy,
                        intArrayOf(
                            particleColor,
                            Color.argb(
                                150,
                                Color.red(particleColor),
                                Color.green(particleColor),
                                Color.blue(particleColor)
                            ),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.6f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                
                val path = Path()
                path.moveTo(screenX + noiseX, screenY + noiseY)
                
                // calculate tail end - along segment in the opposite direction, increase length
                val tailLength = particleSize * 4f * particle.speed
                val dx = (startPoint.first - endPoint.first) * tailLength / segment.first.distanceTo(segment.second)
                val dy = (startPoint.second - endPoint.second) * tailLength / segment.first.distanceTo(segment.second)
                
                path.lineTo(screenX + noiseX + dx, screenY + noiseY + dy)
                canvas.drawPath(path, pathPaint)
            }
            
            particlePaint.shader = null
        }
    }
    
    private fun drawLightningFlashes(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        if (lightningFlashes.any { it.intensity > 0.7f }) {
            // create full screen lighting effect
            val maxIntensity = lightningFlashes.maxOfOrNull { it.intensity } ?: 0f
            val alpha = (30 * maxIntensity).toInt().coerceIn(0, 60)
            
            val screenGlowPaint = Paint().apply {
                color = Color.argb(
                    alpha,
                    220, 230, 255
                )
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width, height, screenGlowPaint)
        }
        
        lightningFlashes.forEach { flash ->
            // adjust coordinates based on direction
            val x = if (isVertical) flash.y * width else flash.x * width
            val y = if (isVertical) flash.x * height else flash.y * height
            
            // adjust lightning radius - larger size
            val radius = flash.radius * min(width, height) * 1.2f
            
            // get lightning color - ensure very high brightness
            val flashColor = getLightningColor(flash.intensity)
            
            // draw outer glow - increase size and brightness
            flashPaint.shader = null
            flashPaint.color = Color.argb(
                (120 * flash.intensity).toInt().coerceIn(0, 180),
                Color.red(flashColor),
                Color.green(flashColor),
                Color.blue(flashColor)
            )
            canvas.drawCircle(x, y, radius * 1.8f, flashPaint)
            
            // create radial gradient - ensure high contrast
            val gradient = RadialGradient(
                x, y,
                radius,
                intArrayOf(
                    Color.WHITE,  // center is white
                    Color.rgb(240, 250, 255),  // brighter light blue white
                    flashColor,
                    Color.argb(
                        (100 * flash.intensity).toInt(),
                        Color.red(flashColor),
                        Color.green(flashColor),
                        Color.blue(flashColor)
                    ),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.1f, 0.4f, 0.7f, 1f),  // adjust gradient distribution
                Shader.TileMode.CLAMP
            )
            
            flashPaint.shader = gradient
            
            // draw lightning core
            canvas.drawCircle(x, y, radius, flashPaint)
            
            // add lightning branch lines - reduce intensity threshold, let more lightning have branches
            if (flash.intensity > 0.5f) {
                drawLightningBranches(canvas, x, y, radius, flashColor, flash.intensity)
            }
            
            flashPaint.shader = null
        }
    }
    
    /**
     * draw lightning branches
     */
    private fun drawLightningBranches(canvas: Canvas, x: Float, y: Float, radius: Float, baseColor: Int, intensity: Float) {
        val branchPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            
            // increase line width
            strokeWidth = 3f + intensity * 5f
            
            // set color - use high brightness lightning color
            val alpha = (220 * intensity).toInt().coerceIn(0, 255)
            this.color = Color.argb(
                alpha,
                Color.red(baseColor),
                Color.green(baseColor),
                Color.blue(baseColor)
            )
        }
        
        // create 3-5 branches - increase number
        val branchCount = 3 + (intensity * 2).toInt()
        
        // use more organized angle distribution
        val angleStep = (Math.PI * 2).toFloat() / branchCount
        
        repeat(branchCount) { i ->
            val path = Path()
            path.moveTo(x, y)
            
            // lightning starting angle - more organized distribution
            val baseAngle = i * angleStep
            val angle = baseAngle + (Random.nextFloat() - 0.5f) * angleStep * 0.3f
            
            var currentX = x
            var currentY = y
            
            // create jagged lightning path - increase segments
            val segments = 4 + (intensity * 3).toInt()
            val maxLength = radius * 1.8f  // increase length
            
            repeat(segments) { j ->
                val segmentLength = maxLength / segments * (0.8f + Random.nextFloat() * 0.4f)
                
                // angle variation more natural
                val angleVariation = (Random.nextFloat() - 0.5f) * Math.PI.toFloat() * 0.4f
                val segmentAngle = angle + angleVariation * (j + 1) / segments
                
                val endX = currentX + cos(segmentAngle) * segmentLength
                val endY = currentY + sin(segmentAngle) * segmentLength
                
                path.lineTo(endX, endY)
                currentX = endX
                currentY = endY
            }
            
            // draw lightning branch
            canvas.drawPath(path, branchPaint)
            
            // add small light point at lightning end - increase size and brightness
            val glowColor = Color.argb(
                (180 * intensity).toInt().coerceIn(0, 255),
                Color.red(baseColor),
                Color.green(baseColor),
                Color.blue(baseColor)
            )
            
            val glowPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = glowColor
            }
            
            canvas.drawCircle(currentX, currentY, 4f + intensity * 4f, glowPaint)
        }
    }
    
    /**
     * apply screen distortion effect
     */
    private fun applyScreenDistortion(canvas: Canvas, fearIntensity: Float) {
        // in actual application, this can use more complex distortion effect
        // for example, use custom shader or RenderScript
        // in this simplified version, we just add a comment to explain
        
        // note: complete implementation of screen distortion effect requires more complex graphics processing technology
        // for example, use OpenGL or custom View drawing method
    }
    
    /**
     * ensure color in dark range, but keep enough visibility
     * redesign color scheme, more suitable for the fear theme
     */
    private fun ensureDarkColor(color: Int, intensity: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        
        val baseHue = 240f  // base blue
        val hueVariation = 40f  // hue variation range
        hsv[0] = (baseHue + (intensity - 0.5f) * hueVariation).coerceIn(220f, 280f)
        
        // increase saturation, ensure enough visibility
        hsv[1] = (0.7f + intensity * 0.3f).coerceIn(0.5f, 1.0f)
        
        // increase brightness, ensure enough visibility
        hsv[2] = (0.4f + intensity * 0.4f).coerceIn(0.3f, 0.8f)
        
        // when high intensity, introduce a small amount of red elements
        if (intensity > 0.8f && Random.nextFloat() < 0.3f) {
            hsv[0] = 350f  // dark red
            hsv[1] = 0.8f
            hsv[2] = 0.5f
        }
        
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
    
    /**
     * enhance particle color - use high contrast electric blue
     */
    private fun enhanceParticleColor(color: Int, intensity: Float): Int {
        // directly use fixed electric blue, not dependent on incoming color
        val hsv = FloatArray(3)
        
        // electric blue base
        hsv[0] = 195f + Random.nextFloat() * 15f  // electric blue, small random variation
        hsv[1] = 0.95f  // higher saturation
        hsv[2] = 0.8f + intensity * 0.2f  // higher brightness, but varies with intensity
        
        return Color.HSVToColor(hsv)
    }
    
    /**
     * get lightning color - use cold tone high brightness color
     */
    private fun getLightningColor(intensity: Float): Int {
        // randomly select blue white or purple white, increase brightness
        val hue = if (Random.nextFloat() < 0.7f) {
            210f + Random.nextFloat() * 10f  // blue white
        } else {
            270f + Random.nextFloat() * 10f  // purple white
        }
        
        val hsv = FloatArray(3)
        hsv[0] = hue
        hsv[1] = 0.2f + intensity * 0.3f  // reduce saturation, closer to white
        hsv[2] = 0.9f + intensity * 0.1f  // higher brightness
        
        return Color.HSVToColor(hsv)
    }
    
    /**
     * simplified simplex noise function
     * note: actual application should use complete simplex noise implementation
     */
    private fun simplexNoise(x: Float, y: Float): Float {
        // this is a simplified noise function, actual application should use complete simplex noise implementation
        return sin(x * 10f + noiseOffsetX) * cos(y * 10f + noiseOffsetY) * 0.5f
    }
    
    /**
     * get position on line
     */
    private fun getPositionOnLine(
        segment: Pair<Pair<Float, Float>, Pair<Float, Float>>,
        progress: Float
    ): Pair<Float, Float> {
        val startX = segment.first.first
        val startY = segment.first.second
        val endX = segment.second.first
        val endY = segment.second.second
        
        val x = startX + (endX - startX) * progress
        val y = startY + (endY - startY) * progress
        
        return Pair(x, y)
    }
    
    /**
     * draw background cracks
     */
    private fun drawBackgroundCracks(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        backgroundCracks.forEach { crack ->
            if (crack.points.size < 2) return@forEach
            
            // create path
            val path = Path()
            
            // get first point
            val firstPoint = crack.points[0]
            val firstX = if (isVertical) firstPoint.second * width else firstPoint.first * width
            val firstY = if (isVertical) firstPoint.first * height else firstPoint.second * height
            
            // move to first point
            path.moveTo(firstX, firstY)
            
            // add subsequent points
            for (i in 1 until crack.points.size) {
                val point = crack.points[i]
                val x = if (isVertical) point.second * width else point.first * width
                val y = if (isVertical) point.first * height else point.second * height
                path.lineTo(x, y)
            }
            
            // increase crack width, make it more obvious
            crackPaint.strokeWidth = 2.0f
            
            // get crack color - use deep blue purple tone, increase visibility
            val crackColor = Color.rgb(60, 50, 100)  // deep blue purple
            
            // apply flickering effect
            val alpha = (150 * crack.flickerIntensity).toInt().coerceIn(0, 255)
            
            // set color
            crackPaint.color = Color.argb(
                alpha,
                Color.red(crackColor),
                Color.green(crackColor),
                Color.blue(crackColor)
            )
            
            // draw crack
            canvas.drawPath(path, crackPaint)
            
            // add glow effect at crack nodes
            if (crack.points.size > 2 && crack.flickerIntensity > 0.6f) {
                // add glow node every few points
                for (i in 1 until crack.points.size step 2) {
                    val point = crack.points[i]
                    val x = if (isVertical) point.second * width else point.first * width
                    val y = if (isVertical) point.first * height else point.second * height
                    
                    // create glow effect
                    val glowPaint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.FILL
                        color = Color.argb(
                            (120 * crack.flickerIntensity).toInt(),
                            100, 90, 180  // brighter blue purple
                        )
                    }
                    
                    // draw glow point
                    canvas.drawCircle(x, y, 3.0f, glowPaint)
                }
            }
        }
    }
    
    // data class definition
    
    /**
     * fractal line data class
     */
    private data class FractalLine(
        val id: Int,
        val startPoint: Pair<Float, Float>,
        val segments: MutableList<Pair<Pair<Float, Float>, Pair<Float, Float>>>,
        val branches: MutableList<FractalBranch>,
        val intensity: Float,
        var lifespan: Float,
        val maxLifespan: Float,
        var flickerPhase: Float,
        var flickerIntensity: Float,
        var distortionPhase: Float,
        val isGrowing: Boolean,
        var growthTimer: Float,
        val growthInterval: Float,
        val branchProbability: Float,
        val maxSegments: Int,
        val maxBranches: Int,
        val angleVariation: Float,
        val baseLength: Float
    )
    
    /**
     * fractal branch data class
     */
    private data class FractalBranch(
        val startPoint: Pair<Float, Float>,
        val segments: MutableList<Pair<Pair<Float, Float>, Pair<Float, Float>>>,
        val branchProbability: Float,
        val maxSegments: Int,
        var growthTimer: Float,
        val growthInterval: Float,
        val angleVariation: Float,
        val baseLength: Float
    )
    
    /**
     * current particle data class
     */
    private data class CurrentParticle(
        val lineIndex: Int,
        val segmentIndex: Int,
        var progress: Float,
        val speed: Float,
        val size: Float,
        var intensity: Float,
        var lifespan: Float,
        val maxLifespan: Float,
        var flickerPhase: Float
    )
    
    /**
     * lightning effect data class
     */
    private data class LightningFlash(
        val x: Float,
        val y: Float,
        val radius: Float,
        var intensity: Float,
        var lifespan: Float,
        val maxLifespan: Float,
        var flickerPhase: Float = 0f
    )
    
    /**
     * background crack data class
     */
    private data class BackgroundCrack(
        val points: MutableList<Pair<Float, Float>>,
        var baseAngle: Float,
        val angleVariation: Float,
        val baseLength: Float,
        val maxPoints: Int,
        var lifespan: Float,
        val maxLifespan: Float,
        val isGrowing: Boolean,
        var growthTimer: Float,
        val growthInterval: Float,
        var flickerPhase: Float,
        var flickerIntensity: Float
    )
    
    // auxiliary extension function - calculate distance between two points
    private fun Pair<Float, Float>.distanceTo(other: Pair<Float, Float>): Float {
        val dx = this.first - other.first
        val dy = this.second - other.second
        return sqrt(dx * dx + dy * dy)
    }
}