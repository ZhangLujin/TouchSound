package com.example.musicvibrationapp.view.spectrum.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow
import kotlin.math.max
import kotlin.math.min

class DisgustWaveProcessor {
    
    private val wavePoints = mutableListOf<Float>()
    private val distortionOffsets = mutableListOf<Float>()
    private val waveAmplitudes = mutableListOf<Float>()
    private val controlPoints = mutableListOf<Float>() 
    
    private var width = 0
    private var height = 0
    private var isVertical = false
    private var pointCount = 0
    
    private var baseAmplitude = 0f
    private var waveFrequency = 1f
    private var wavePhase = 0f
    
    private var turbulenceStrength = 0f 
    private var viscosityFactor = 0.5f 
    private var blobFactor = 0f 
    private var corrosionFactor = 0f 
    
    private val noiseGenerator = PerlinNoise()
    private var noiseScale = 0.1f
    private var noiseTime = 0f
    
    private val wavePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    
    private val wavePath = Path()
    private val blobPath = Path()
    
    fun initialize(width: Int, height: Int, isVertical: Boolean) {
        this.width = width
        this.height = height
        this.isVertical = isVertical
        
        pointCount = if (isVertical) height / 2 else width / 2
        
        wavePoints.clear()
        distortionOffsets.clear()
        waveAmplitudes.clear()
        controlPoints.clear()
        
        for (i in 0 until pointCount) {
            wavePoints.add(0f)
            distortionOffsets.add(0f)
            waveAmplitudes.add(0f)
            controlPoints.add(0f)
        }
        
        baseAmplitude = if (isVertical) width * 0.15f else height * 0.15f
        
        noiseTime = 0f
    }
    
    fun update(
        lowFreqEnergy: Float,
        midFreqEnergy: Float,
        highFreqEnergy: Float,
        totalEnergy: Float,
        isRhythmPeak: Boolean,
        rhythmIntensity: Float,
        distortionIntensity: Float,
        deltaTime: Float,
        smoothingFactor: Float
    ) {
        updateDistortionParameters(
            lowFreqEnergy, 
            midFreqEnergy, 
            highFreqEnergy, 
            totalEnergy,
            isRhythmPeak,
            rhythmIntensity,
            distortionIntensity,
            deltaTime,
            smoothingFactor
        )
        
        val frequencyResponseFactor = 1.0f + smoothingFactor * 2.0f
        waveFrequency = 3.5f + midFreqEnergy * 6f * frequencyResponseFactor + distortionIntensity * 4f
        
        val phaseSpeed = (waveFrequency * 5f + 4.0f) * (1.0f - viscosityFactor * 0.3f) * 
                        (0.8f + smoothingFactor * 0.7f)  
        wavePhase += deltaTime * phaseSpeed
        if (wavePhase > 2 * PI.toFloat()) wavePhase -= 2 * PI.toFloat()
        
        noiseTime += deltaTime * (2.0f + distortionIntensity * 4.0f) * 
                    (0.7f + smoothingFactor * 1.0f)  
        
        for (i in 0 until pointCount) {
            val normalizedPos = i.toFloat() / pointCount
            
            val baseWave = sin(normalizedPos * 10f + wavePhase) * 0.7f + 
                          sin(normalizedPos * 20f + wavePhase * 1.5f) * 0.5f +
                          sin(normalizedPos * 5f + wavePhase * 0.8f) * 0.4f
            
            val noiseX = normalizedPos * 8f
            val noiseY = wavePhase * 0.8f
            val turbulence = noiseGenerator.noise(noiseX, noiseY, noiseTime) * 3f - 1.5f
            
            val viscosity = calculateViscosityEffect(normalizedPos, lowFreqEnergy, smoothingFactor)
            
            val corrosion = calculateCorrosionEffect(normalizedPos, i, highFreqEnergy, smoothingFactor)
            
            val blob = calculateBlobEffect(normalizedPos, midFreqEnergy, smoothingFactor)
            
            val effectIntensityFactor = 0.8f + smoothingFactor * 0.8f
            val combinedOffset = baseWave * 0.7f + 
                               turbulence * turbulenceStrength * 1.5f * effectIntensityFactor + 
                               viscosity * viscosityFactor * 1.8f * effectIntensityFactor +
                               corrosion * corrosionFactor * 2.0f * effectIntensityFactor +
                               blob * blobFactor * 2.2f * effectIntensityFactor
            
            val amplitudeFactor = 0.5f + smoothingFactor * 0.5f
            val targetOffset = combinedOffset * baseAmplitude * 
                             (0.5f + distortionIntensity * 1.2f) * amplitudeFactor
            
            val transitionRate = smoothingFactor * 8f * (1.0f - viscosityFactor * 0.5f)
            distortionOffsets[i] += (targetOffset - distortionOffsets[i]) * 
                                  transitionRate * deltaTime
            
            if (i > 0 && i < pointCount - 1) {
                val prevOffset = distortionOffsets[i-1]
                val nextOffset = distortionOffsets[i+1]
                val currentOffset = distortionOffsets[i]
                
                val controlInfluence = 0.25f + viscosityFactor * 0.5f
                controlPoints[i] = currentOffset + 
                                 (nextOffset - prevOffset) * controlInfluence
            }
            
            val frequencyFactor = when {
                normalizedPos < 0.33f -> 0.5f + lowFreqEnergy * 1.0f * (0.8f + smoothingFactor * 0.6f)
                normalizedPos < 0.66f -> 0.5f + midFreqEnergy * 1.0f * (0.8f + smoothingFactor * 0.6f)
                else -> 0.5f + highFreqEnergy * 1.0f * (0.8f + smoothingFactor * 0.6f)
            }
            
            val targetAmplitude = baseAmplitude * frequencyFactor * 
                                (1.5f + distortionIntensity * 1.2f)
            
            waveAmplitudes[i] += (targetAmplitude - waveAmplitudes[i]) * 
                               smoothingFactor * 6f * deltaTime
        }
        
        if (pointCount > 1) {
            controlPoints[0] = distortionOffsets[0]
            controlPoints[pointCount-1] = distortionOffsets[pointCount-1]
        }
    }
    
    private fun updateDistortionParameters(
        lowFreqEnergy: Float,
        midFreqEnergy: Float,
        highFreqEnergy: Float,
        totalEnergy: Float,
        isRhythmPeak: Boolean,
        rhythmIntensity: Float,
        distortionIntensity: Float,
        deltaTime: Float,
        smoothingFactor: Float  
    ) {
        val turbulenceBaseFactor = 0.4f + smoothingFactor * 0.4f
        val targetTurbulence = turbulenceBaseFactor + midFreqEnergy * 0.7f + highFreqEnergy * 0.5f
        turbulenceStrength += (targetTurbulence - turbulenceStrength) * 3f * deltaTime * 
                            (0.7f + smoothingFactor * 1.0f)
        
        val viscosityBaseFactor = 0.3f + smoothingFactor * 0.2f
        val targetViscosity = viscosityBaseFactor + lowFreqEnergy * 0.8f
        viscosityFactor += (targetViscosity - viscosityFactor) * 2.5f * deltaTime * 
                         (0.7f + smoothingFactor * 1.0f)
        
        val blobBaseFactor = 0.2f + smoothingFactor * 0.2f
        val targetBlobFactor = blobBaseFactor + midFreqEnergy * 0.7f + 
                             (if (isRhythmPeak) rhythmIntensity * 0.8f else 0f)
        blobFactor += (targetBlobFactor - blobFactor) * 4f * deltaTime * 
                    (0.7f + smoothingFactor * 1.0f)
        
        val corrosionBaseFactor = 0.3f + smoothingFactor * 0.2f
        val targetCorrosion = corrosionBaseFactor + highFreqEnergy * 1.0f
        corrosionFactor += (targetCorrosion - corrosionFactor) * 3.5f * deltaTime * 
                         (0.7f + smoothingFactor * 1.0f)
    }
    
    private fun calculateViscosityEffect(normalizedPos: Float, lowFreqEnergy: Float, smoothingFactor: Float): Float {
        val viscosityWave = sin(normalizedPos * 6f + wavePhase * 0.7f) * 
                           (1.0f + smoothingFactor * 0.6f)
        
        return viscosityWave * lowFreqEnergy * (1.0f + smoothingFactor * 0.4f)
    }
    
    private fun calculateCorrosionEffect(normalizedPos: Float, index: Int, highFreqEnergy: Float, smoothingFactor: Float): Float {
        val noiseFactor = 20f + smoothingFactor * 20f
        val corrosionNoise = noiseGenerator.noise(
            normalizedPos * noiseFactor, 
            wavePhase * 0.5f, 
            noiseTime * 0.7f
        )
        
        val corrosionStrength = highFreqEnergy * (1.5f + smoothingFactor * 0.6f)
        
        val threshold = 0.5f - smoothingFactor * 0.2f
        return if (corrosionNoise > threshold) {
            (corrosionNoise - threshold) * (3.0f + smoothingFactor * 1.0f) * corrosionStrength
        } else {
            0f
        }
    }
    
    private fun calculateBlobEffect(normalizedPos: Float, midFreqEnergy: Float, smoothingFactor: Float): Float {
        val speedFactor = 0.7f + smoothingFactor * 0.5f
        val blobPosition1 = (sin(wavePhase * speedFactor) * 0.5f + 0.5f)
        val blobPosition2 = (cos(wavePhase * speedFactor * 1.3f) * 0.5f + 0.5f)
        
        val distanceFromBlob1 = kotlin.math.abs(normalizedPos - blobPosition1)
        val distanceFromBlob2 = kotlin.math.abs(normalizedPos - blobPosition2)
        
        val distanceFromBlob = kotlin.math.min(distanceFromBlob1, distanceFromBlob2)
        
        val gaussianWidth = 0.01f - smoothingFactor * 0.005f
        val blobShape = kotlin.math.exp(-(distanceFromBlob * distanceFromBlob) / gaussianWidth)
        
        return blobShape * midFreqEnergy * (1.8f + smoothingFactor * 0.8f)
    }
    
    fun draw(
        canvas: Canvas,
        colors: List<Int>,
        distortionIntensity: Float,
        lowFreqEnergy: Float,
        midFreqEnergy: Float,
        highFreqEnergy: Float
    ) {
        if (pointCount <= 0) return
        
        drawMainWave(canvas, colors[0], distortionIntensity)
        
        drawBlobEffects(canvas, colors[0], colors[1], midFreqEnergy, distortionIntensity)
        
        drawSecondaryWave(canvas, colors[1], distortionIntensity)
        
        drawCorrosionEffects(canvas, colors[1], highFreqEnergy, distortionIntensity)
    }
    
    private fun drawMainWave(canvas: Canvas, color: Int, distortionIntensity: Float) {
        wavePaint.strokeWidth = 5f + distortionIntensity * 7f + viscosityFactor * 5f
        wavePaint.color = color
        wavePaint.alpha = (255 * (0.9f + distortionIntensity * 0.1f)).toInt()
        
        wavePath.reset()
        
        for (i in 0 until pointCount) {
            val normalizedPos = i.toFloat() / pointCount
            
            val x = if (isVertical) 
                width * 0.5f + distortionOffsets[i]
            else 
                width * normalizedPos
            
            val y = if (isVertical)
                height * normalizedPos
            else
                height * 0.5f + distortionOffsets[i]
            
            if (i == 0) {
                wavePath.moveTo(x, y)
            } else {
                val prevX = if (isVertical) 
                    width * 0.5f + distortionOffsets[i-1]
                else 
                    width * (i-1).toFloat() / pointCount
                
                val prevY = if (isVertical)
                    height * (i-1).toFloat() / pointCount
                else
                    height * 0.5f + distortionOffsets[i-1]
                
                val controlX = if (isVertical)
                    width * 0.5f + controlPoints[i-1]
                else
                    width * (i-0.5f) / pointCount
                
                val controlY = if (isVertical)
                    height * (i-0.5f) / pointCount
                else
                    height * 0.5f + controlPoints[i-1]
                
                wavePath.quadTo(controlX, controlY, x, y)
            }
        }
        
        canvas.drawPath(wavePath, wavePaint)
    }
    
    private fun drawSecondaryWave(canvas: Canvas, color: Int, distortionIntensity: Float) {
        wavePaint.color = color
        wavePaint.alpha = (255 * (0.7f + distortionIntensity * 0.3f)).toInt()
        wavePaint.strokeWidth = 3f + distortionIntensity * 4f
        
        wavePath.reset()
        
        val offset = baseAmplitude * 0.3f
        
        for (i in 0 until pointCount) {
            val normalizedPos = i.toFloat() / pointCount
            
            val extraDistortion = sin(normalizedPos * 25f + wavePhase * 1.8f) * 
                                baseAmplitude * 0.25f * distortionIntensity
            
            val x = if (isVertical) 
                width * 0.5f + distortionOffsets[i] + offset + extraDistortion
            else 
                width * normalizedPos
            
            val y = if (isVertical)
                height * normalizedPos
            else
                height * 0.5f + distortionOffsets[i] + offset + extraDistortion
            
            if (i == 0) {
                wavePath.moveTo(x, y)
            } else {
                wavePath.lineTo(x, y)
            }
        }
        
        canvas.drawPath(wavePath, wavePaint)
    }
    
    private fun drawBlobEffects(
        canvas: Canvas, 
        mainColor: Int, 
        highlightColor: Int,
        midFreqEnergy: Float,
        distortionIntensity: Float
    ) {
        if (blobFactor < 0.3f) return
        
        wavePaint.style = Paint.Style.FILL
        wavePaint.color = mainColor
        wavePaint.alpha = (200 * blobFactor).toInt()
        
        for (i in 1 until pointCount - 1) {
            val prev = distortionOffsets[i-1]
            val current = distortionOffsets[i]
            val next = distortionOffsets[i+1]
            
            if (current > prev && current > next && current > baseAmplitude * 0.4f) {
                val normalizedPos = i.toFloat() / pointCount
                
                val blobSize = current * 0.6f * (0.5f + midFreqEnergy * 0.5f)
                
                val x = if (isVertical) 
                    width * 0.5f + current
                else 
                    width * normalizedPos
                
                val y = if (isVertical)
                    height * normalizedPos
                else
                    height * 0.5f + current
                
                canvas.drawCircle(x, y, blobSize, wavePaint)
                
                wavePaint.color = highlightColor
                wavePaint.alpha = (150 * blobFactor).toInt()
                canvas.drawCircle(
                    x - blobSize * 0.3f,
                    y - blobSize * 0.3f,
                    blobSize * 0.4f,
                    wavePaint
                )
                
                wavePaint.color = mainColor
            }
        }
        
        wavePaint.style = Paint.Style.STROKE
    }
    
    private fun drawCorrosionEffects(
        canvas: Canvas,
        color: Int,
        highFreqEnergy: Float,
        distortionIntensity: Float
    ) {
        if (corrosionFactor < 0.3f) return
        
        wavePaint.style = Paint.Style.STROKE
        wavePaint.color = color
        wavePaint.alpha = (180 * corrosionFactor).toInt()
        wavePaint.strokeWidth = 1f + highFreqEnergy * 2f
        
        for (i in 0 until pointCount - 1 step 3) {
            val normalizedPos = i.toFloat() / pointCount
            
            val shouldDraw = noiseGenerator.noise(
                normalizedPos * 10f,
                wavePhase * 0.2f,
                noiseTime * 0.3f
            ) > 0.5f
            
            if (shouldDraw) {
                val x = if (isVertical) 
                    width * 0.5f + distortionOffsets[i]
                else 
                    width * normalizedPos
                
                val y = if (isVertical)
                    height * normalizedPos
                else
                    height * 0.5f + distortionOffsets[i]
                
                val lineLength = 5f + highFreqEnergy * 15f * corrosionFactor
                
                val angle = (normalizedPos * 10f + noiseTime) * 2f * PI.toFloat()
                
                val endX = x + cos(angle) * lineLength
                val endY = y + sin(angle) * lineLength
                
                canvas.drawLine(x, y, endX, endY, wavePaint)
                
                if (noiseGenerator.noise(normalizedPos * 5f, noiseTime, 0f) > 0.7f) {
                    val branchAngle = angle + PI.toFloat() * 0.2f
                    val branchLength = lineLength * 0.7f
                    
                    val branchEndX = x + cos(branchAngle) * branchLength
                    val branchEndY = y + sin(branchAngle) * branchLength
                    
                    canvas.drawLine(x, y, branchEndX, branchEndY, wavePaint)
                }
            }
        }
        
        wavePaint.style = Paint.Style.STROKE
    }
    
    private inner class PerlinNoise {
        private val permutation = IntArray(512)
        
        init {
            val p = IntArray(256)
            for (i in 0 until 256) {
                p[i] = i
            }
            
            for (i in 255 downTo 0) {
                val j = (Math.random() * (i + 1)).toInt()
                val temp = p[i]
                p[i] = p[j]
                p[j] = temp
            }
            
            for (i in 0 until 512) {
                permutation[i] = p[i and 255]
            }
        }
        
        fun noise(x: Float, y: Float, z: Float): Float {
            val xi = x.toInt() and 255
            val yi = y.toInt() and 255
            val zi = z.toInt() and 255
            
            val xf = x - x.toInt()
            val yf = y - y.toInt()
            val zf = z - z.toInt()
            
            val u = fade(xf)
            val v = fade(yf)
            val w = fade(zf)
            
            val aaa = permutation[permutation[permutation[xi] + yi] + zi]
            val aba = permutation[permutation[permutation[xi] + yi + 1] + zi]
            val aab = permutation[permutation[permutation[xi] + yi] + zi + 1]
            val abb = permutation[permutation[permutation[xi] + yi + 1] + zi + 1]
            val baa = permutation[permutation[permutation[xi + 1] + yi] + zi]
            val bba = permutation[permutation[permutation[xi + 1] + yi + 1] + zi]
            val bab = permutation[permutation[permutation[xi + 1] + yi] + zi + 1]
            val bbb = permutation[permutation[permutation[xi + 1] + yi + 1] + zi + 1]
            
            val x1 = lerp(
                grad(aaa, xf, yf, zf),
                grad(baa, xf - 1, yf, zf),
                u
            )
            val x2 = lerp(
                grad(aba, xf, yf - 1, zf),
                grad(bba, xf - 1, yf - 1, zf),
                u
            )
            val y1 = lerp(x1, x2, v)
            
            val x3 = lerp(
                grad(aab, xf, yf, zf - 1),
                grad(bab, xf - 1, yf, zf - 1),
                u
            )
            val x4 = lerp(
                grad(abb, xf, yf - 1, zf - 1),
                grad(bbb, xf - 1, yf - 1, zf - 1),
                u
            )
            val y2 = lerp(x3, x4, v)
            
            return (lerp(y1, y2, w) + 1) / 2
        }
        
        private fun fade(t: Float): Float {
            return t * t * t * (t * (t * 6 - 15) + 10)
        }
        
        private fun lerp(a: Float, b: Float, t: Float): Float {
            return a + t * (b - a)
        }
        
        private fun grad(hash: Int, x: Float, y: Float, z: Float): Float {
            val h = hash and 15
            val u = if (h < 8) x else y
            val v = if (h < 4) y else if (h == 12 || h == 14) x else z
            
            return ((if (h and 1 == 0) u else -u) + 
                   (if (h and 2 == 0) v else -v))
        }
    }
} 