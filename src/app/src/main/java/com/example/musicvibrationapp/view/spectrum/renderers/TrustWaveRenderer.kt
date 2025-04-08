package com.example.musicvibrationapp.view.spectrum.renderers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.musicvibrationapp.view.SpectrumParameters
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Waveform renderer for trust emotions
 * Expression of trust emotions using smooth continuous waves and connections, integrated with SpectrumParameters parameter system
 */
class TrustWaveRenderer(context: Context) : BaseEmotionRenderer(context) {
    // Waveform Control Point System - Reduced Maximum Number
    private val waveControlPoints = mutableListOf<WaveControlPoint>()
    private val MAX_WAVE_CONTROL_POINTS = 15 // limit the maximum number of control points
    
    private val wavePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val wavePath = Path()
    
    // Connection Point System - Reduced Maximum Number
    private val connectionNodes = mutableListOf<ConnectionNode>()
    private val MAX_CONNECTION_NODES = 25 // limit the maximum number of nodes
    
    private val nodePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    // Pulse Wave System - Reduced Maximum Number
    private val pulseWaves = mutableListOf<PulseWave>()
    private val MAX_PULSE_WAVES = 10 // limit the maximum number of pulse waves
    
    private val pulsePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    // Object Pool - Reduced Object Creation
    private val nodePool = ObjectPool<ConnectionNode>(20)
    private val pulsePool = ObjectPool<PulseWave>(10)
    private val pointPool = ObjectPool<WaveControlPoint>(15)
    
    // Wave Phase
    private var wavePhase = 0f
    
    // Add wave display control variables
    private var waveVisibilityFactor = 0f  // 0 means fully collapsed, 1 means fully expanded
    private val waveVisibilityThreshold = 0.05f  // wave visibility threshold
    private val waveVisibilitySpeed = 0.8f  // wave visibility speed
    
    /**
     * Process spectrum data
     */
    override fun onProcessData(processedData: FloatArray): FloatArray {
        val basePhaseSpeed = 0.03f  // reduce from 0.05f
        val rhythmInfluence = rhythmFlowCoefficient * 0.1f  // reduce from 0.15f
        val energyInfluence = totalEnergy * 0.03f  // reduce from 0.05f
        val volumeChangeBoost = volumeChange * 0.06f  // reduce from 0.1f
        
        // rhythm peak gives a moderate speed boost, but reduced
        val peakBoost = if (isRhythmPeak) 0.05f * volumeChange else 0f  // reduce from 0.1f
        
        // overall phase speed
        val phaseSpeed = basePhaseSpeed + rhythmInfluence + energyInfluence + volumeChangeBoost + peakBoost
        
        // add smooth transition to avoid oscillations due to sudden changes
        val targetPhaseSpeed = phaseSpeed
        val currentPhaseSpeed = deltaTime * targetPhaseSpeed
        
        // use smooth interpolation to reduce sudden changes
        wavePhase += currentPhaseSpeed
        
        // update wave visibility factor - smooth transition based on volume
        val targetVisibility = if (currentVolume < waveVisibilityThreshold) {
            // when volume is very low, target is to collapse the wave
            0f
        } else {
            // when volume is enough, target is to expand the wave, and the higher the volume, the faster the expansion
            1f
        }
        
        // smooth transition to target visibility
        waveVisibilityFactor += (targetVisibility - waveVisibilityFactor) * 
                               deltaTime * waveVisibilitySpeed * 
                               (if (targetVisibility > waveVisibilityFactor) currentVolume * 2f + 0.5f else 1f)
        
        // ensure within valid range
        waveVisibilityFactor = waveVisibilityFactor.coerceIn(0f, 1f)
        
        // reduce the responsiveness of the rhythm flow coefficient, making the changes smoother
        val targetFlow = if (isRhythmPeak) {
            0.3f + volumeChange * 0.5f  // reduce from 0.4f and 0.7f
        } else {
            0.06f + totalEnergy * 0.12f  // reduce from 0.08f and 0.15f
        }
        
        // reduce the response rate, making the rhythm changes smoother, reducing oscillations
        val responseRate = if (isRhythmPeak) 1.8f else 1.2f  // reduce from 2.5f and 1.8f
        
        // use smooth interpolation to reduce sudden changes
        rhythmFlowCoefficient += (targetFlow - rhythmFlowCoefficient) * deltaTime * responseRate
        
        // update flash intensity - smooth decay
        flashIntensity *= 0.9f
        
        // update wave control points
        updateWaveControlPoints(deltaTime)
        
        // update connection points
        updateConnectionNodes(deltaTime)
        
        // update pulse waves
        updatePulseWaves(deltaTime)
        
        // generate new connection points - based on spectrum energy
        if (connectionNodes.size < MAX_CONNECTION_NODES) {
            generateConnectionNodes(processedData)
        }
        
        // improved rhythm detection - using level response system
        if (isRhythmPeak || (totalEnergy > 0.6f && volumeChange > 0.15f)) {
            // strong rhythm response
            flashIntensity = 0.8f
            generatePulseWave(totalEnergy, 3) // intensity level 3
        } 
        else if (totalEnergy > 0.5f && volumeChange > 0.1f) {
            // medium rhythm response
            flashIntensity = 0.5f
            generatePulseWave(totalEnergy, 2) // intensity level 2
        }
        else if (totalEnergy > 0.4f && volumeChange > 0.05f) {
            // mild rhythm response
            flashIntensity = 0.3f
            generatePulseWave(totalEnergy, 1) // intensity level 1
        }
        
        return processedData
    }
    
    /**
     * render spectrum
     */
    override fun onRender(canvas: Canvas, data: FloatArray, width: Int, height: Int, isVertical: Boolean) {
        val maxWidth = width.toFloat()
        val maxHeight = height.toFloat()
        
        // calculate wave amplitude - based on energy and rhythm
        val baseAmplitude = 0.1f + totalEnergy * 0.1f
        val rhythmBoost = rhythmFlowCoefficient * 0.1f
        val finalAmplitude = (baseAmplitude + rhythmBoost).coerceAtMost(0.3f)
        
        // draw base waves
        drawBaseWaves(canvas, maxWidth, maxHeight, isVertical, finalAmplitude)
        
        // draw connection points
        drawConnectionNodes(canvas, maxWidth, maxHeight, isVertical)
        
        // draw network connections
        drawNetworkConnections(canvas, maxWidth, maxHeight, isVertical)
        
        // draw pulse waves
        drawPulseWaves(canvas, maxWidth, maxHeight, isVertical)
        
        // draw flash effect
        if (flashIntensity > 0.01f) {
            drawFlashEffect(canvas, maxWidth, maxHeight, isVertical)
        }
    }
    
    /**
     * draw base waves - multi-layer smooth waves
     */
    private fun drawBaseWaves(canvas: Canvas, width: Float, height: Float, isVertical: Boolean, amplitude: Float) {
        // if the wave is fully collapsed, do not draw
        if (waveVisibilityFactor <= 0.001f) return
        
        // get emotion color - use the method provided by BaseEmotionRenderer
        val baseColors = getEmotionGradientColors(height, width)
        val baseColor = if (baseColors.size > 2) baseColors[2] else getEmotionColor()
        
        // create 3 layers of waves, each with different frequency and phase
        val layers = 3
        
        // fix: set the starting position correctly based on direction
        val centerPosition = if (isVertical) width * 0.5f else height * 0.5f
        
        // enhance color system for depth
        val layerColors = arrayOf(
            enhanceColor(baseColor, 0.9f, -0.15f),  // bottom layer - darker and more saturated
            baseColor,                              // middle layer - standard color
            adjustComplementaryColor(baseColor, 1.3f, 0.15f)  // top layer - brighter contrast color
        )
        
        // adjust layer transparency to enhance depth
        val layerAlphas = floatArrayOf(0.95f, 0.75f, 0.55f)
        
        // add layer offsets to enhance visual depth
        val layerOffsets = floatArrayOf(0f, 0.01f, 0.02f)
        
        for (layer in 0 until layers) {
            // reduce the association strength between layer phase and rhythm, making the changes smoother
            val rhythmPhaseOffset = rhythmFlowCoefficient * 0.3f * (layer + 1)  // reduce from 0.5f
            val layerPhase = wavePhase + layer * Math.PI.toFloat() * 0.25f + rhythmPhaseOffset
            
            // reduce the association strength between frequency and rhythm, reducing high-frequency oscillations
            val baseFrequency = 3f + layer * 1.5f  // reduce from 4f and 2f
            val rhythmFrequencyBoost = rhythmFlowCoefficient * 1.0f * (layer + 1)  // reduce from 1.5f
            val layerFrequency = baseFrequency + rhythmFrequencyBoost
            
            // reduce the association strength between amplitude and rhythm, making the changes smoother
            val baseRhythmBoost = if (isRhythmPeak) volumeChange * 0.15f else 0f  // reduce from 0.2f
            val layerRhythmBoost = rhythmFlowCoefficient * 0.15f + baseRhythmBoost  // reduce from 0.2f
            
            // set different amplitudes for each layer, enhance depth and rhythm response, but reduce overall amplitude
            val layerAmplitudeMultiplier = 1f - layer * 0.15f
            val smoothedAmplitude = amplitude * layerAmplitudeMultiplier * 
                                   (0.8f + layerRhythmBoost) *
                                   waveVisibilityFactor
            
            // set wave color - use cached color
            val layerColor = layerColors[layer]
            
            // set wave transparency - use cached transparency, and adjust according to visibility factor
            val layerAlpha = layerAlphas[layer] * waveVisibilityFactor
            
            // set wave path
            wavePath.reset()
            
            // set wave path correctly based on direction, ensuring both sides move in the same direction
            if (isVertical) {
                // vertical wave (SIDES mode) - wave extends along the Y axis
                wavePath.moveTo(0f, 0f)
                
                // increase the number of steps to make the wave smoother
                val steps = 60  // increase from 50
                for (i in 0..steps) {
                    val progress = i.toFloat() / steps
                    val y = height * progress
                    
                    // completely rewrite the wave direction logic, ensuring both sides move in the same direction
                    // left view (x < width/2) and right view (x > width/2) need opposite phase calculation
                    val waveOffset = if (width < height) {
                        // left view - standard phase
                        sin(progress * layerFrequency + layerPhase) * smoothedAmplitude
                    } else {
                        // right view - use the opposite phase calculation, but keep the same movement direction
                        sin(progress * layerFrequency - layerPhase) * smoothedAmplitude
                    }
                    
                    // add layer offset to enhance visual depth
                    val layerOffset = layerOffsets[layer] * width
                    val x = centerPosition + waveOffset * width + layerOffset
                    
                    wavePath.lineTo(x, y)
                }
                
                // complete the path
                wavePath.lineTo(width, height)
                wavePath.lineTo(0f, height)
                wavePath.close()
            } else {
                // horizontal wave (TOP_BOTTOM mode) - wave extends along the X axis
                wavePath.moveTo(0f, 0f)
                
                // increase the number of steps to make the wave smoother
                val steps = 60  // increase from 50
                for (i in 0..steps) {
                    val progress = i.toFloat() / steps
                    val x = width * progress
                    
                    // completely rewrite the wave direction logic, ensuring both sides move in the same direction
                    // upper view (y < height/2) and lower view (y > height/2) need opposite phase calculation
                    val waveOffset = if (height < width) {
                        // upper view - standard phase
                        sin(progress * layerFrequency + layerPhase) * smoothedAmplitude
                    } else {
                        // lower view - use the opposite phase calculation, but keep the same movement direction
                        sin(progress * layerFrequency - layerPhase) * smoothedAmplitude
                    }
                    
                    // add layer offset to enhance visual depth
                    val layerOffset = layerOffsets[layer] * height
                    val y = centerPosition + waveOffset * height + layerOffset
                    
                    wavePath.lineTo(x, y)
                }
                
                // complete the path
                wavePath.lineTo(width, height)
                wavePath.lineTo(0f, height)
                wavePath.close()
            }
            
            // set wave color and transparency
            wavePaint.color = Color.argb(
                (layerAlpha * 255).toInt(),
                Color.red(layerColor),
                Color.green(layerColor),
                Color.blue(layerColor)
            )
            
            // draw wave
            canvas.drawPath(wavePath, wavePaint)
        }
    }
    
    /**
     * draw connection points
     */
    private fun drawConnectionNodes(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        // if the wave is fully collapsed, do not draw connection points
        if (waveVisibilityFactor <= 0.001f) return
        
        // get emotion color
        val baseColors = getEmotionGradientColors(height, width)
        val nodeColor = if (baseColors.size > 2) baseColors[2] else getEmotionColor()
        
        // more strongly adjust node color to make it more distinct from wave
        val safeNodeColor = createDistinctNodeColor(nodeColor)
        
        connectionNodes.forEach { node ->
            // correctly convert coordinates, whether in vertical or horizontal mode
            val x = node.x * width
            val y = node.y * height
            
            // enhance node size with rhythm intensity and volume
            val volumeBoost = currentVolume * 8f
            
            // enhance rhythm impact on node size
            val rhythmSizeBoost = if (isRhythmPeak) {
                // when the rhythm peak, significantly increase node size, and consider volume change
                (rhythmFlowCoefficient * 0.4f + volumeChange * 0.6f) * node.pulseIntensity
            } else {
                // when not at the peak, maintain moderate impact
                rhythmFlowCoefficient * 0.2f * node.pulseIntensity
            }
            
            // add volume factor to base size
            val baseSize = 5f + node.size * 15f + volumeBoost
            
            // enhance rhythm impact and pulse impact
            val rhythmBoost = rhythmSizeBoost * 20f  // enhance rhythm impact
            val pulseBoost = node.pulseIntensity * 25f  // enhance pulse impact
            
            // final size consider base size, rhythm impact and pulse impact
            val finalSize = baseSize + rhythmBoost + pulseBoost
            
            // create node color - pulse intensity affects color, more obvious difference from wave
            val coreColor = if (node.pulseIntensity > 0.3f) {
                // pulse activation state - brighter and yellow color
                enhanceNodeColor(safeNodeColor, 1.5f, 0.25f, node.pulseIntensity)
            } else {
                // normal state - more obvious difference
                enhanceNodeColor(safeNodeColor, 1.3f, 0.15f, 0f)
            }
            
            // adjust node transparency, considering wave visibility factor
            val adjustedAlpha = node.alpha * waveVisibilityFactor
            
            // create radial gradient - more concentrated halo effect, reduce radiation range
            val colors = intArrayOf(
                Color.argb(
                    (adjustedAlpha * 255).toInt().coerceIn(0, 255),
                    Color.red(coreColor),
                    Color.green(coreColor),
                    Color.blue(coreColor)
                ),
                Color.argb(
                    (adjustedAlpha * 160).toInt().coerceIn(0, 255),
                    Color.red(coreColor),
                    Color.green(coreColor),
                    Color.blue(coreColor)
                ),
                Color.argb(
                    (adjustedAlpha * 50).toInt().coerceIn(0, 255),
                    Color.red(safeNodeColor),
                    Color.green(safeNodeColor),
                    Color.blue(safeNodeColor)
                ),
                Color.TRANSPARENT
            )
            
            // adjust gradient position, making halo more concentrated
            val positions = floatArrayOf(0f, 0.15f, 0.4f, 1.0f)
            
            // ensure radius is greater than 0
            if (finalSize > 0) {
                nodePaint.shader = RadialGradient(
                    x, y,
                    finalSize,
                    colors,
                    positions,
                    Shader.TileMode.CLAMP
                )
                
                canvas.drawCircle(x, y, finalSize, nodePaint)
            }
        }
    }
    
    /**
     * draw pulse waves
     */
    private fun drawPulseWaves(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        // if the wave is fully collapsed, do not draw pulse waves
        if (waveVisibilityFactor <= 0.001f) return
        
        // get emotion color
        val baseColors = getEmotionGradientColors(height, width)
        val pulseColor = if (baseColors.size > 2) baseColors[2] else getEmotionColor()
        
        // adjust pulse wave color, making it more yellow and brighter
        val safePulseColor = adjustNodeColor(pulseColor)
        
        // draw pulse waves one by one
        pulseWaves.forEach { pulse ->
            // fix: correctly convert coordinates based on direction
            val x = if (isVertical) pulse.x * width else pulse.x * width
            val y = if (isVertical) pulse.y * height else pulse.y * height
            
            // calculate current radius - expand with progress
            val currentRadius = pulse.maxRadius * pulse.progress * (width.coerceAtMost(height))
            
            // ensure radius is greater than 0 - fix RadialGradient exception
            val safeRadius = currentRadius.coerceAtLeast(0.1f)
            
            // calculate current transparency - reduce with progress, and consider wave visibility factor
            // reduce overall transparency, reducing radiation impact
            val alpha = pulse.initialAlpha * (1f - pulse.progress * 0.9f) * waveVisibilityFactor * 0.7f
            
            // adjust color based on intensity level
            val coreColor = when (pulse.intensityLevel) {
                3 -> enhanceNodeColor(safePulseColor, 1.4f, 0.25f, 0.8f)  // strong rhythm - brighter and yellow
                2 -> enhanceNodeColor(safePulseColor, 1.3f, 0.15f, 0.5f)  // medium rhythm
                1 -> enhanceNodeColor(safePulseColor, 1.2f, 0.1f, 0.3f)   // mild rhythm
                else -> enhanceNodeColor(safePulseColor, 1.1f, 0.05f, 0f) // weak pulse
            }
            
            // create pulse wave gradient - from inside to outside, more concentrated halo effect
            val colors = intArrayOf(
                Color.argb(
                    (alpha * 255).toInt().coerceIn(0, 255), 
                    Color.red(coreColor), 
                    Color.green(coreColor), 
                    Color.blue(coreColor)
                ),
                Color.argb(
                    (alpha * 0.6f * 255).toInt().coerceIn(0, 255),
                    Color.red(coreColor),
                    Color.green(coreColor),
                    Color.blue(coreColor)
                ),
                Color.argb(
                    (alpha * 0.2f * 255).toInt().coerceIn(0, 255),
                    Color.red(safePulseColor),
                    Color.green(safePulseColor),
                    Color.blue(safePulseColor)
                ),
                Color.argb(
                    0,
                    Color.red(safePulseColor),
                    Color.green(safePulseColor),
                    Color.blue(safePulseColor)
                )
            )
            
            // adjust gradient position, making halo more concentrated
            val stops = floatArrayOf(0f, 0.2f, 0.5f, 1f)
            
            // prevent radius from being 0
            if (safeRadius > 0) {
                // create radial gradient
                pulsePaint.shader = RadialGradient(
                    x, y,
                    safeRadius,
                    colors,
                    stops,
                    Shader.TileMode.CLAMP
                )
                
                // draw pulse wave
                canvas.drawCircle(x, y, safeRadius, pulsePaint)
            }
        }
    }
    
    /**
     * draw flash effect
     */
    private fun drawFlashEffect(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        // get emotion color and ensure in green range
        val baseColors = getEmotionGradientColors(height, width)
        val flashColor = if (baseColors.size > 2) baseColors[2] else getEmotionColor()
        
        // adjust flash color, making it more distinct from wave
        val safeFlashColor = createDistinctNodeColor(flashColor)
        
        // enhance flash color - brighter and closer to white yellow green
        val enhancedFlashColor = enhanceNodeColor(safeFlashColor, 1.3f, 0.35f, 0.5f)
        
        val flashPaint = Paint().apply {
            color = Color.argb(
                // reduce overall transparency, reducing radiation impact
                (flashIntensity * 90).toInt().coerceIn(0, 90),
                Color.red(enhancedFlashColor),
                Color.green(enhancedFlashColor),
                Color.blue(enhancedFlashColor)
            )
            style = Paint.Style.FILL
        }
        
        // create local flash in areas with strong pulses, not the entire canvas
        pulseWaves.forEach { pulse ->
            if (pulse.intensityLevel >= 2 && pulse.progress < 0.3f) {
                val x = if (isVertical) pulse.x * width else pulse.x * width
                val y = if (isVertical) pulse.y * height else pulse.y * height
                
                // reduce flash radius, making flash more concentrated, reducing radiation impact
                val flashRadius = pulse.maxRadius * 0.6f * (1.0f - pulse.progress)
                flashPaint.shader = RadialGradient(
                    x, y, flashRadius,
                    Color.argb(
                        // reduce overall transparency, reducing radiation impact
                        (flashIntensity * pulse.initialAlpha * 100).toInt().coerceIn(0, 100),
                        Color.red(enhancedFlashColor),
                        Color.green(enhancedFlashColor),
                        Color.blue(enhancedFlashColor)
                    ),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(x, y, flashRadius, flashPaint)
                flashPaint.shader = null
            }
        }
    }
    
    /**
     * draw network connections
     */
    private fun drawNetworkConnections(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        // if the wave is fully collapsed or there are too few nodes, do not draw connections
        if (waveVisibilityFactor <= 0.001f || connectionNodes.size < 2) return
        
        // get emotion color
        val baseColors = getEmotionGradientColors(height, width)
        val lineColor = if (baseColors.size > 2) baseColors[2] else getEmotionColor()
        
        // adjust line color, making it more yellow and brighter
        val safeLineColor = adjustNodeColor(lineColor)
        
        // set line paint
        val linePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        // iterate through all node pairs
        for (i in 0 until connectionNodes.size) {
            val node1 = connectionNodes[i]
            
            // fix: correctly convert coordinates, whether in vertical or horizontal mode
            val x1 = node1.x * width
            val y1 = node1.y * height
            
            // only connect nodes with appropriate distance
            for (j in i + 1 until connectionNodes.size) {
                val node2 = connectionNodes[j]
                
                // fix: correctly convert coordinates, whether in vertical or horizontal mode
                val x2 = node2.x * width
                val y2 = node2.y * height
                
                // calculate distance between nodes
                val dx = x2 - x1
                val dy = y2 - y1
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                
                // only connect nodes with appropriate distance
                // adjust maximum connection distance according to current view mode
                val maxDistance = if (isVertical) {
                    height * 0.2f  // use a more suitable distance in vertical mode (SIDES)
                } else {
                    width * 0.3f   // keep the original distance in horizontal mode (TOP_BOTTOM)
                }
                
                if (distance < maxDistance) {
                    // the closer the distance, the less transparent the line
                    val alpha = (1.0f - distance / maxDistance) * 0.7f
                    
                    // calculate average pulse intensity
                    val avgPulse = (node1.pulseIntensity + node2.pulseIntensity) * 0.5f
                    
                    // set line color - pulse intensity affects color
                    val finalColor = if (avgPulse > 0.3f) {
                        // pulse activation state - brighter and yellow color
                        enhanceNodeColor(safeLineColor, 1.3f, 0.15f, avgPulse)
                    } else {
                        // normal state
                        safeLineColor
                    }
                    
                    // set line transparency considering wave visibility factor
                    linePaint.color = Color.argb(
                        (alpha * 255 * node1.alpha * node2.alpha * waveVisibilityFactor).toInt().coerceIn(0, 255),
                        Color.red(finalColor),
                        Color.green(finalColor),
                        Color.blue(finalColor)
                    )
                    
                    // draw connection line
                    canvas.drawLine(x1, y1, x2, y2, linePaint)
                }
            }
        }
    }
    
    /**
     * update wave control points
     */
    private fun updateWaveControlPoints(deltaTime: Float) {
        val fallSpeed = SpectrumParameters.fallSpeed.value
        
        val iterator = waveControlPoints.iterator()
        while (iterator.hasNext()) {
            val point = iterator.next()
            // update lifespan
            point.lifespan -= deltaTime * (1.0f + fallSpeed * 3.0f)
            
            // update phase
            point.phase += deltaTime * point.speed
            
            // control point disappearance condition
            if (point.lifespan <= 0) {
                iterator.remove()
                pointPool.recycle(point) // recycle object
            }
        }
    }
    
    /**
     * update connection points
     */
    private fun updateConnectionNodes(deltaTime: Float) {
        val iterator = connectionNodes.iterator()
        while (iterator.hasNext()) {
            val node = iterator.next()
            // update lifespan
            node.lifespan -= deltaTime
            
            if (node.lifespan > 0) {
                // transparency changes with lifespan - first increase then decrease
                val normalizedLife = node.lifespan / node.maxLifespan
                node.alpha = if (normalizedLife > 0.7f) {
                    // early life, transparency increases
                    (1.0f - normalizedLife) * 3.0f
                } else {
                    // later life, transparency decreases
                    normalizedLife * 1.2f
                }.coerceIn(0f, 1f)
                
                // apply trust force field - make connection points form an orderly collective movement
                val (newVx, newVy) = applyTrustForceField(node.x, node.y, node.vx, node.vy)
                
                // apply rhythm pulse impact on speed - enhance rhythm response
                val pulseWeight = 0.3f + node.pulseIntensity * 0.2f  // pulse intensity affects weight
                val flowWeight = 0.7f + totalEnergy * 0.2f  // energy affects flow weight
                val (finalVx, finalVy) = applyRhythmResponseToVelocity(
                    newVx, newVy,
                    pulseWeight = pulseWeight,
                    flowWeight = flowWeight
                )
                
                // update position
                node.x += finalVx * deltaTime
                node.y += finalVy * deltaTime
                
                // ensure within boundaries
                node.x = node.x.coerceIn(0f, 1f)
                node.y = node.y.coerceIn(0f, 1f)
                
                // pulse intensity decay - adjust decay rate according to rhythm intensity
                // slow down decay in strong rhythm, keeping nodes larger for a longer time
                val decayRate = if (isRhythmPeak) {
                    // during rhythm peak, slow down decay
                    0.95f + volumeChange * 0.03f
                } else {
                    // during non-peak, normal decay
                    0.95f
                }
                node.pulseIntensity *= decayRate
            } else {
                iterator.remove()
                nodePool.recycle(node) // recycle object
            }
        }
    }
    
    /**
     * update pulse waves
     */
    private fun updatePulseWaves(deltaTime: Float) {
        val iterator = pulseWaves.iterator()
        while (iterator.hasNext()) {
            val pulse = iterator.next()
            // update progress
            val progressSpeed = when (pulse.intensityLevel) {
                3 -> 0.7f  // strong rhythm - spread faster
                2 -> 0.6f  // medium rhythm
                1 -> 0.5f  // mild rhythm
                else -> 0.4f  // weak pulse
            }
            
            pulse.progress += progressSpeed * deltaTime
            
            // if progress is less than 1, keep pulse wave, otherwise remove
            if (pulse.progress >= 1f) {
                iterator.remove()
                pulsePool.recycle(pulse) // recycle object
            }
        }
    }
    
    /**
     * generate connection points
     */
    private fun generateConnectionNodes(data: FloatArray) {
        // if the wave is almost fully collapsed, do not generate new nodes
        if (waveVisibilityFactor < 0.1f) return
        
        // use wave visibility factor to adjust generation probability
        val visibilityMultiplier = waveVisibilityFactor * waveVisibilityFactor  // square to make effect more obvious
        
        // use parameter system to affect node generation
        val minThreshold = SpectrumParameters.minThreshold.value
        val soloResponseStrength = SpectrumParameters.soloResponseStrength.value
        
        // during rhythm peak, increase node generation probability
        val rhythmMultiplier = if (isRhythmPeak) 2.5f else 1.0f
        val volumeInfluence = currentVolume * 1.5f + 0.5f
        
        // limit the number of nodes generated per frame
        var nodesCreatedThisFrame = 0
        val maxNodesPerFrame = 3
        
        // generate nodes based on spectrum energy
        for (index in data.indices) {
            if (nodesCreatedThisFrame >= maxNodesPerFrame) break
            
            val magnitude = data[index]
            // use minThreshold as generation threshold
            val dynamicThreshold = minThreshold * (if (isRhythmPeak) 0.6f else 0.8f)
            
            // generation probability considers wave visibility
            val generationProbability = magnitude * 0.25f * rhythmMultiplier * volumeInfluence * visibilityMultiplier
            
            if (magnitude > dynamicThreshold && Random.nextFloat() < generationProbability) {
                // generate nodes of different sizes based on frequency index
                val nodeSize = when {
                    index < data.size * 0.2 -> 0.4f + Random.nextFloat() * 0.4f  // low frequency - large node
                    index < data.size * 0.6 -> 0.3f + Random.nextFloat() * 0.2f  // medium frequency - medium node
                    else -> 0.2f + Random.nextFloat() * 0.1f  // high frequency - small node
                }
                
                // generate position - uniformly distributed
                val originX = Random.nextFloat()
                val originY = Random.nextFloat()
                
                // initial speed - small, keep stable flow
                val baseVx = (Random.nextFloat() - 0.5f) * 0.05f
                val baseVy = (Random.nextFloat() - 0.5f) * 0.05f
                
                val finalVx = baseVx * (1f + soloResponseStrength)
                val finalVy = baseVy * (1f + soloResponseStrength)
                
                // lifespan - longer to keep stability
                val lifespan = 3.0f + Random.nextFloat() * 2.0f
                
                // get or create new node from object pool
                val node = nodePool.obtain() ?: ConnectionNode(
                    x = originX,
                    y = originY,
                    vx = finalVx,
                    vy = finalVy,
                    size = nodeSize,
                    lifespan = lifespan,
                    maxLifespan = lifespan,
                    alpha = 0.1f,
                    pulseIntensity = 0f
                )
                
                // if the node is obtained from the object pool, update its properties
                if (node != connectionNodes.lastOrNull()) {
                    node.x = originX
                    node.y = originY
                    node.vx = finalVx
                    node.vy = finalVy
                    node.size = nodeSize
                    node.lifespan = lifespan
                    node.maxLifespan = lifespan
                    node.alpha = 0.1f
                    node.pulseIntensity = 0f
                }
                
                connectionNodes.add(node)
                nodesCreatedThisFrame++
            }
        }
    }
    
    /**
     * generate pulse waves
     */
    private fun generatePulseWave(energy: Float, intensityLevel: Int) {
        // if the wave is almost fully collapsed, do not generate new pulse waves
        if (waveVisibilityFactor < 0.1f) return
        
        // limit the number of pulse waves
        if (pulseWaves.size >= MAX_PULSE_WAVES) return
        
        // generate position - random or based on existing nodes
        val pulseX: Float
        val pulseY: Float
        
        // if there are connection points, there is an 80% probability to generate pulse waves at the connection points
        if (connectionNodes.isNotEmpty() && Random.nextFloat() < 0.8f) {
            // select a random connection point
            val selectedNode = connectionNodes[Random.nextInt(connectionNodes.size)]
            pulseX = selectedNode.x
            pulseY = selectedNode.y
        } else {
            // random position
            pulseX = 0.3f + Random.nextFloat() * 0.4f
            pulseY = 0.3f + Random.nextFloat() * 0.4f
        }
        
        // response strength - based on energy and intensity level, increase volume change impact
        val responseStrength = when (intensityLevel) {
            3 -> 0.8f + energy * 0.2f + volumeChange * 0.5f  // strong rhythm - increase volume change impact
            2 -> 0.6f + energy * 0.15f + volumeChange * 0.3f  // medium rhythm
            1 -> 0.4f + energy * 0.1f + volumeChange * 0.2f  // mild rhythm
            else -> 0.2f + energy * 0.05f + volumeChange * 0.1f  // weak pulse
        }
        
        // base radius - based on energy and intensity level, increase distinction
        val baseRadius = when (intensityLevel) {
            3 -> 0.3f + energy * 0.5f + volumeChange * 0.2f  // strong rhythm - larger radius, increase volume change impact
            2 -> 0.25f + energy * 0.35f + volumeChange * 0.15f  // medium rhythm
            1 -> 0.2f + energy * 0.25f + volumeChange * 0.1f  // mild rhythm
            else -> 0.15f + energy * 0.1f + volumeChange * 0.05f  // weak pulse
        }
        
        // response strength impact - increase rhythm impact
        val finalRadius = baseRadius * (1.0f + responseStrength * 0.8f)  // increase response strength impact
        
        // initial transparency adjusted according to intensity - increase visual impact
        val initialAlpha = when (intensityLevel) {
            3 -> 0.8f  // strong rhythm - more obvious
            2 -> 0.6f  // medium rhythm - increase visibility
            1 -> 0.45f  // mild rhythm - slight enhancement
            else -> 0.3f  // weak pulse
        }
        
        // get or create new pulse wave from object pool
        val pulse = pulsePool.obtain() ?: PulseWave(
            x = pulseX,
            y = pulseY,
            maxRadius = finalRadius,
            progress = 0f,
            initialAlpha = initialAlpha,
            intensityLevel = intensityLevel
        )
        
        // if obtained from object pool, update its properties
        if (pulse != pulseWaves.lastOrNull()) {
            pulse.x = pulseX
            pulse.y = pulseY
            pulse.maxRadius = finalRadius
            pulse.progress = 0f
            pulse.initialAlpha = initialAlpha
            pulse.intensityLevel = intensityLevel
        }
        
        pulseWaves.add(pulse)
        
        // activate pulse waves near strong rhythm points - enhance propagation effect
        if (intensityLevel >= 2) {
            // limit the number of nodes processed each time
            val nodesToProcess = connectionNodes.size.coerceAtMost(15)
            for (i in 0 until nodesToProcess) {
                val node = connectionNodes[i]
                // calculate the distance between the node and the pulse center
                val dx = node.x - pulseX
                val dy = node.y - pulseY
                val distance = sqrt(dx * dx + dy * dy)
                
                // if the node is within the pulse range, activate its pulse effect
                if (distance < finalRadius * 1.5f) {
                    // increase distance impact and volume impact
                    // the closer the distance, the greater the impact, and the greater the volume change impact
                    val influence = (1.0f - distance / (finalRadius * 1.5f)).coerceIn(0f, 1f)
                    
                    // increase volume impact on pulse propagation
                    val volumeBoost = volumeChange * 0.7f  // increase volume change impact
                    
                    // set node pulse intensity, considering current volume
                    node.pulseIntensity = (node.pulseIntensity + influence * 0.9f + volumeBoost).coerceAtMost(1.0f)
                }
            }
        }
    }
    
    /**
     * ensure color in trust emotion range - use EmotionColorManager provided color
     */
    private fun ensureGreenColor(color: Int): Int {
        // directly use the method provided by BaseEmotionRenderer to get the current emotion color
        return getEmotionColor(Color.alpha(color))
    }
    
    /**
     * create harmonious contrasting colors - use EmotionColorManager provided color
     * add brightness and saturation parameters to enhance contrast
     */
    private fun adjustComplementaryColor(color: Int, saturationFactor: Float = 1.0f, brightnessAdjust: Float = 0f): Int {
        // get emotion gradient colors
        val gradientColors = getEmotionGradientColors(100f, 100f)
        
        // create a purer green color using the base color
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        
        // adjust hue to a slightly cyan green, increase contrast
        hsv[0] = (hsv[0] + 15f) % 360f
        
        // increase saturation to ensure color purity
        hsv[1] = (hsv[1] * saturationFactor).coerceIn(0.7f, 0.95f)
        
        // adjust brightness appropriately
        hsv[2] = (hsv[2] + brightnessAdjust).coerceIn(0.6f, 0.9f)
        
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
    
    /**
     * enhance color - keep emotional characteristics
     */
    private fun enhanceColor(color: Int, saturationFactor: Float, brightnessAdjust: Float = 0f): Int {
        // convert to HSV
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        
        // keep hue unchanged, adjust saturation and brightness appropriately
        hsv[1] = (hsv[1] * saturationFactor).coerceIn(0.7f, 0.95f)
        hsv[2] = (hsv[2] + brightnessAdjust).coerceIn(0.6f, 0.9f)
        
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
    
    /**
     * apply trust force field - create a gentle and orderly collective movement
     */
    private fun applyTrustForceField(x: Float, y: Float, vx: Float, vy: Float): Pair<Float, Float> {
        // center point - center of the force field
        val centerX = 0.5f
        val centerY = 0.5f
        
        // distance to center
        val dx = x - centerX
        val dy = y - centerY
        val distanceToCenter = sqrt(dx * dx + dy * dy)
        
        // reduce force field phase change speed, making movement smoother
        val forceFieldPhase = (wavePhase * 0.07f + rhythmFlowCoefficient * 0.15f) % (2 * Math.PI.toFloat())  // from 0.1f and 0.2f
        
        // reduce rotation strength, making movement smoother
        val baseRotationStrength = 0.008f  // from 0.01f
        val rhythmRotationBoost = rhythmFlowCoefficient * 0.3f + volumeChange * 0.2f  // from 0.5f and 0.3f
        val rotationStrength = baseRotationStrength * (1f + rhythmRotationBoost)
        
        val rotationX = -dy * rotationStrength
        val rotationY = dx * rotationStrength
        
        // gentle center gravity - prevent particles from diverging too much
        val gravitationalStrength = 0.01f * (1f - distanceToCenter.coerceIn(0f, 1f))
        val gravitationalX = -dx * gravitationalStrength
        val gravitationalY = -dy * gravitationalStrength
        
        // reduce rhythm influence field strength, making changes smoother
        val rhythmInfluence = getRhythmPulseValue() * 0.1f + volumeChange * 0.15f  // from 0.15f and 0.2f
        
        // wave force field - create a rhythmic wave
        val waveFrequency = 2.0f * Math.PI.toFloat()
        val waveInfluence = sin(distanceToCenter * waveFrequency + forceFieldPhase) * rhythmInfluence
        
        // combine the effects of all forces, reduce the wave force field strength
        val newVx = vx + rotationX + gravitationalX + dx * waveInfluence * 0.03f  // from 0.05f
        val newVy = vy + rotationY + gravitationalY + dy * waveInfluence * 0.03f  // from 0.05f
        
        // limit maximum speed, reduce the impact of rhythm on speed
        val speedLimit = 0.08f + rhythmFlowCoefficient * 0.03f  // from 0.1f and 0.05f
        val currentSpeed = sqrt(newVx * newVx + newVy * newVy)
        
        return if (currentSpeed > speedLimit) {
            val factor = speedLimit / currentSpeed
            Pair(newVx * factor, newVy * factor)
        } else {
            Pair(newVx, newVy)
        }
    }
    
    // object pool implementation - reduce object creation and GC pressure
    private class ObjectPool<T>(private val maxSize: Int) {
        private val pool = mutableListOf<T>()
        
        fun obtain(): T? {
            return if (pool.isNotEmpty()) {
                pool.removeAt(pool.size - 1)
            } else {
                null
            }
        }
        
        fun recycle(obj: T) {
            if (pool.size < maxSize) {
                pool.add(obj)
            }
        }
    }
    
    // data class definition - use mutable properties instead of immutable data classes, reduce object creation
    private class WaveControlPoint(
        val x: Float,
        val y: Float,
        val radius: Float,     // influence radius
        val strength: Float,   // influence strength
        var phase: Float,      // current phase
        val speed: Float,      // phase change speed
        var lifespan: Float    // remaining lifespan
    )
    
    private class ConnectionNode(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var lifespan: Float,
        var maxLifespan: Float,
        var alpha: Float = 1f,
        var pulseIntensity: Float = 0f  // node pulse intensity 0-1
    )
    
    private class PulseWave(
        var x: Float,
        var y: Float,
        var maxRadius: Float,     // maximum radius
        var progress: Float,      // progress 0-1
        var initialAlpha: Float,  // initial transparency
        var intensityLevel: Int   // intensity level 0-3
    )
    
    /**
     * adjust node color - make it more yellow and brighter, form a contrast with the waveform
     */
    private fun adjustNodeColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        
        // adjust hue, make it more yellow (green is about 120, yellow is about 60)
        // adjust green to yellow green
        hsv[0] = (hsv[0] - 15f).coerceIn(70f, 110f)
        
        // increase saturation
        hsv[1] = (hsv[1] * 1.1f).coerceIn(0.7f, 0.95f)
        
        // increase brightness
        hsv[2] = (hsv[2] * 1.15f).coerceIn(0.65f, 0.9f)
        
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
    
    /**
     * enhance node color - adjust color based on pulse intensity
     */
    private fun enhanceNodeColor(color: Int, saturationFactor: Float, brightnessAdjust: Float, pulseIntensity: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        
        // adjust hue further based on pulse intensity, more yellow when strong pulse
        hsv[0] = (hsv[0] - pulseIntensity * 10f).coerceIn(60f, 110f)
        
        // keep hue unchanged, adjust saturation and brightness appropriately
        hsv[1] = (hsv[1] * saturationFactor).coerceIn(0.7f, 0.95f)
        hsv[2] = (hsv[2] + brightnessAdjust + pulseIntensity * 0.1f).coerceIn(0.6f, 0.95f)
        
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
    
    /**
     * create node color distinctly different from waveform
     */
    private fun createDistinctNodeColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        
        // more strongly adjust hue, make it more yellow (green is about 120, yellow is about 60)
        // adjust green to more obvious yellow green
        hsv[0] = (hsv[0] - 25f).coerceIn(60f, 100f)
        
        // increase saturation
        hsv[1] = (hsv[1] * 1.2f).coerceIn(0.75f, 0.95f)
        
        // increase brightness
        hsv[2] = (hsv[2] * 1.25f).coerceIn(0.7f, 0.95f)
        
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
}