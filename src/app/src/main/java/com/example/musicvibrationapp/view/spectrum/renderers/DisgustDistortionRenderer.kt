package com.example.musicvibrationapp.view.spectrum.renderers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.musicvibrationapp.view.SpectrumParameters
import kotlin.math.max
import kotlin.math.min

/**
 * Disgust emotion distortion wave renderer
 * Acts as the main renderer class, coordinating the work of other modules
 */
class DisgustDistortionRenderer(context: Context) : BaseEmotionRenderer(context) {
    
    // Submodule instances
    private val waveSystem = DisgustWaveSystem()
    private val effectsManager = DisgustEffectsManager()
    private val audioAnalyzer = DisgustAudioAnalyzer()
    private val colorScheme = DisgustColorScheme()
    private val waveProcessor = DisgustWaveProcessor()
    
    // Rendering state
    private var lastWidth = 0
    private var lastHeight = 0
    private var isInitialized = false
    
    // Disgust-specific state variables
    private var distortionIntensity = 0f      // Distortion intensity
    private var corrosionLevel = 0f           // Corrosion level
    private var bubbleGenerationRate = 0f     // Bubble generation rate
    private var dropletGenerationRate = 0f    // Droplet generation rate
    
    // Analysis result cache
    private var analysisResult = AudioAnalysisResult()
    
    /**
     * Process spectrum data
     */
    override fun onProcessData(processedData: FloatArray): FloatArray {
        // Get smoothing factor - controls effect response speed
        val smoothingFactor = SpectrumParameters.smoothingFactor.value
        
        // Use audio analyzer to analyze data
        analysisResult = audioAnalyzer.analyzeAudioData(
            processedData,
            totalEnergy,
            isRhythmPeak,
            volumeChange,
            deltaTime,
            smoothingFactor
        )
        
        // Update distortion intensity based on analysis results
        updateDistortionIntensity(analysisResult, smoothingFactor)
        
        // Update corrosion level
        updateCorrosionLevel(analysisResult, smoothingFactor)
        
        // Update generation rates
        updateGenerationRates(analysisResult, smoothingFactor)
        
        // Update wave system
        waveSystem.update(
            analysisResult,
            distortionIntensity,
            deltaTime,
            smoothingFactor
        )
        
        // Update wave processor
        waveProcessor.update(
            analysisResult.lowFrequencyEnergy,
            analysisResult.midFrequencyEnergy,
            analysisResult.highFrequencyEnergy,
            analysisResult.totalEnergy,
            analysisResult.isRhythmPeak,
            analysisResult.rhythmIntensity,
            distortionIntensity,
            deltaTime,
            smoothingFactor
        )
        
        // Update effects manager
        effectsManager.update(
            analysisResult,
            corrosionLevel,
            bubbleGenerationRate,
            dropletGenerationRate,
            deltaTime,
            smoothingFactor
        )
        
        // Update color scheme
        colorScheme.update(
            analysisResult,
            distortionIntensity,
            corrosionLevel,
            deltaTime
        )
        
        return processedData
    }
    
    /**
     * Render spectrum
     */
    override fun onRender(canvas: Canvas, data: FloatArray, width: Int, height: Int, isVertical: Boolean) {
        // // When volume is 0, display nothing (completely transparent) //TODO: Adjust based on actual needs
        // if (analysisResult.totalEnergy <= 0.001f) {
        //     return
        // }
        
        // Initialization check
        if (width != lastWidth || height != lastHeight || !isInitialized) {
            lastWidth = width
            lastHeight = height
            isInitialized = true
            
            // Initialize submodules
            waveSystem.initialize(width, height, isVertical)
            effectsManager.initialize(width, height, isVertical)
            colorScheme.initialize()
            waveProcessor.initialize(width, height, isVertical)
        }
        
        // Clear canvas - use base background color
        val backgroundColor = colorScheme.getBackgroundColor(corrosionLevel)
        canvas.drawColor(backgroundColor)
        
        // Drawing order is important: from bottom to top
        
        // 1. Draw background effects (corrosion texture)
        effectsManager.drawCorrosionTexture(
            canvas,
            colorScheme.getCorrosionColors(),
            corrosionLevel
        )
        
        // 2. Draw distorted waveform - using new wave processor
        waveProcessor.draw(
            canvas,
            colorScheme.getWaveColors(),
            distortionIntensity,
            analysisResult.lowFrequencyEnergy,
            analysisResult.midFrequencyEnergy,
            analysisResult.highFrequencyEnergy
        )
        
        // 3. Draw droplet effects
        effectsManager.drawDroplets(
            canvas,
            colorScheme.getDropletColors(),
            analysisResult.lowFrequencyEnergy
        )
        
        // 4. Draw bubble effects
        effectsManager.drawBubbles(
            canvas,
            colorScheme.getBubbleColors(),
            analysisResult.midFrequencyEnergy
        )
        
        // 5. Draw rhythm response effects
        if (analysisResult.isRhythmPeak) {
            effectsManager.drawRhythmEffect(
                canvas,
                colorScheme.getRhythmEffectColor(),
                analysisResult.rhythmIntensity
            )
        }
        
        // 6. Draw high frequency response effects (if any)
        if (analysisResult.highFrequencyEnergy > 0.4f) {
            effectsManager.drawHighFrequencyEffect(
                canvas,
                colorScheme.getHighFrequencyColor(),
                analysisResult.highFrequencyEnergy
            )
        }
    }
    
    /**
     * Update distortion intensity
     */
    private fun updateDistortionIntensity(analysisResult: AudioAnalysisResult, smoothingFactor: Float) {
        // Calculate target distortion intensity based on audio analysis results
        val targetIntensity = 0.3f + 
                            analysisResult.lowFrequencyEnergy * 0.3f + 
                            analysisResult.midFrequencyEnergy * 0.2f + 
                            analysisResult.highFrequencyEnergy * 0.2f
        
        // Use smoothingFactor to adjust response speed - higher smoothingFactor will make distortion intensity respond faster to changes
        val transitionRate = 2.0f * smoothingFactor
        distortionIntensity += (targetIntensity - distortionIntensity) * transitionRate * deltaTime
    }
    
    /**
     * Update corrosion level
     */
    private fun updateCorrosionLevel(analysisResult: AudioAnalysisResult, smoothingFactor: Float) {
        // Calculate target corrosion level based on audio analysis results
        val targetCorrosion = 0.3f + 
                            analysisResult.highFrequencyEnergy * 0.4f + 
                            analysisResult.midFrequencyEnergy * 0.2f
        
        // Use smoothingFactor to adjust response speed and base values
        // Higher smoothingFactor will make corrosion level respond faster to changes and have higher base values
        val transitionRate = 1.5f * smoothingFactor
        val minCorrosion = 0.3f + smoothingFactor * 0.2f  // Minimum corrosion level affected by smoothingFactor
        
        corrosionLevel += (targetCorrosion - corrosionLevel) * transitionRate * deltaTime
        corrosionLevel = maxOf(corrosionLevel, minCorrosion)  // Ensure minimum corrosion level
    }
    
    /**
     * Update generation rates
     */
    private fun updateGenerationRates(analysisResult: AudioAnalysisResult, smoothingFactor: Float) {
        // Bubble generation rate - mainly affected by mid and high frequencies
        val targetBubbleRate = analysisResult.midFrequencyEnergy * 0.8f + 
                             analysisResult.highFrequencyEnergy * 0.4f
        
        // Droplet generation rate - mainly affected by low frequencies and rhythm
        val targetDropletRate = analysisResult.lowFrequencyEnergy * 0.7f + 
                              (if (analysisResult.isRhythmPeak) analysisResult.rhythmIntensity * 0.5f else 0f)
        
        // Use smoothingFactor to adjust response speed and base generation rate
        // Higher smoothingFactor will make generation rates respond faster to changes and have higher base rates
        val transitionRate = 2.0f * smoothingFactor
        val baseGenerationRate = 0.1f + smoothingFactor * 0.2f  // Base generation rate affected by smoothingFactor
        
        bubbleGenerationRate += (targetBubbleRate - bubbleGenerationRate) * transitionRate * deltaTime
        bubbleGenerationRate = maxOf(bubbleGenerationRate, baseGenerationRate * 0.8f)  // Ensure minimum generation rate
        
        dropletGenerationRate += (targetDropletRate - dropletGenerationRate) * transitionRate * deltaTime
        dropletGenerationRate = maxOf(dropletGenerationRate, baseGenerationRate)  // Ensure minimum generation rate
    }
    
    /**
     * Audio analysis result data class
     * Used for passing analysis data between modules
     */
    data class AudioAnalysisResult(
        var lowFrequencyEnergy: Float = 0f,    // Low frequency energy (0-1)
        var midFrequencyEnergy: Float = 0f,    // Mid frequency energy (0-1)
        var highFrequencyEnergy: Float = 0f,   // High frequency energy (0-1)
        var totalEnergy: Float = 0f,           // Total energy (0-1)
        var isRhythmPeak: Boolean = false,     // Whether it is a rhythm peak
        var rhythmIntensity: Float = 0f,       // Rhythm intensity (0-1)
        var volumeChange: Float = 0f,          // Volume change (can be negative)
        var dominantFrequency: Float = 0f,     // Dominant frequency (Hz)
        var voicePresence: Float = 0f,         // Voice presence level (0-1)
        var noiseLevel: Float = 0f             // Noise level (0-1)
    )
    
    /**
     * Distortion wave system class
     * Responsible for generating and updating distorted waveforms
     */
    inner class DisgustWaveSystem {
        // Wave data
        private val wavePoints = mutableListOf<Float>()
        private val distortionOffsets = mutableListOf<Float>()
        private val waveAmplitudes = mutableListOf<Float>()
        
        // Rendering properties
        private var width = 0
        private var height = 0
        private var isVertical = false
        private var pointCount = 0
        
        // Wave parameters
        private var baseAmplitude = 0f
        private var waveFrequency = 1f
        private var wavePhase = 0f
        
        // Drawing tools
        private val wavePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        
        /**
         * Initialize wave system
         */
        fun initialize(width: Int, height: Int, isVertical: Boolean) {
            this.width = width
            this.height = height
            this.isVertical = isVertical
            
            // Calculate point count - based on width or height
            pointCount = if (isVertical) height / 3 else width / 3
            
            // Initialize wave data
            wavePoints.clear()
            distortionOffsets.clear()
            waveAmplitudes.clear()
            
            // Fill initial data
            for (i in 0 until pointCount) {
                wavePoints.add(0f)
                distortionOffsets.add(0f)
                waveAmplitudes.add(0f)
            }
            
            // Set base amplitude
            baseAmplitude = if (isVertical) width * 0.15f else height * 0.15f
        }
        
        /**
         * Update wave system
         */
        fun update(
            result: AudioAnalysisResult,
            distortionIntensity: Float,
            deltaTime: Float,
            smoothingFactor: Float
        ) {
            // Update wave frequency - significantly increase base changes
            waveFrequency = 2.5f + result.midFrequencyEnergy * 4f
            
            // Update wave phase - significantly increase base movement speed
            wavePhase += deltaTime * (waveFrequency * 4f + 2.5f)
            if (wavePhase > 6.28f) wavePhase -= 6.28f
            
            // Update distortion offsets - increase base distortion
            for (i in 0 until pointCount) {
                // Calculate target distortion offset - increase base distortion amount
                val normalizedPos = i.toFloat() / pointCount
                val targetOffset = (kotlin.math.sin(normalizedPos * 8f + wavePhase) * 0.7f + 0.5f) * 
                                  (0.3f + distortionIntensity * 0.7f) * baseAmplitude
                
                // Speed up smooth transition
                distortionOffsets[i] += (targetOffset - distortionOffsets[i]) * 
                                      smoothingFactor * 5f * deltaTime
                
                // Calculate target amplitude - increase base amplitude
                val frequencyFactor = when {
                    normalizedPos < 0.33f -> 0.3f + result.lowFrequencyEnergy * 0.7f
                    normalizedPos < 0.66f -> 0.3f + result.midFrequencyEnergy * 0.7f
                    else -> 0.3f + result.highFrequencyEnergy * 0.7f
                }
                
                val targetAmplitude = baseAmplitude * frequencyFactor * 
                                    (1.2f + distortionIntensity * 0.8f)
                
                // Speed up smooth transition
                waveAmplitudes[i] += (targetAmplitude - waveAmplitudes[i]) * 
                                   smoothingFactor * 4f * deltaTime
            }
        }
        
        /**
         * Draw waveform
         */
        fun draw(
            canvas: Canvas,
            colors: List<Int>,
            distortionIntensity: Float,
            result: AudioAnalysisResult
        ) {
            if (pointCount <= 0) return
            
            // Set line width - increase base width
            wavePaint.strokeWidth = 4f + distortionIntensity * 6f
            
            // Calculate waveform points - add random disturbance
            for (i in 0 until pointCount) {
                val normalizedPos = i.toFloat() / pointCount
                
                // Add random disturbance
                val randomOffset = (Math.random().toFloat() - 0.5f) * baseAmplitude * 0.2f
                
                val x = if (isVertical) 
                    width * 0.5f + distortionOffsets[i] + randomOffset
                else 
                    width * normalizedPos
                
                val y = if (isVertical)
                    height * normalizedPos
                else
                    height * 0.5f + distortionOffsets[i] + randomOffset
                
                wavePoints[i] = if (isVertical) x else y
            }
            
            // Draw main waveform
            wavePaint.color = colors[0]
            wavePaint.alpha = (255 * (0.8f + distortionIntensity * 0.2f)).toInt()
            
            // Draw waveform path
            for (i in 0 until pointCount - 1) {
                val x1 = if (isVertical) wavePoints[i] else (width * i.toFloat() / pointCount)
                val y1 = if (isVertical) (height * i.toFloat() / pointCount) else wavePoints[i]
                
                val x2 = if (isVertical) wavePoints[i + 1] else (width * (i + 1).toFloat() / pointCount)
                val y2 = if (isVertical) (height * (i + 1).toFloat() / pointCount) else wavePoints[i + 1]
                
                canvas.drawLine(x1, y1, x2, y2, wavePaint)
            }
            
            // Always draw auxiliary waveform - no longer based on distortion intensity condition
            wavePaint.color = colors[1]
            wavePaint.alpha = (255 * (0.5f + distortionIntensity * 0.5f)).toInt()
            wavePaint.strokeWidth = 2f + distortionIntensity * 3f
            
            // Draw offset auxiliary waveform
            for (i in 0 until pointCount - 1) {
                val offset = baseAmplitude * 0.4f
                
                val x1 = if (isVertical) wavePoints[i] + offset else (width * i.toFloat() / pointCount)
                val y1 = if (isVertical) (height * i.toFloat() / pointCount) else wavePoints[i] + offset
                
                val x2 = if (isVertical) wavePoints[i + 1] + offset else (width * (i + 1).toFloat() / pointCount)
                val y2 = if (isVertical) (height * (i + 1).toFloat() / pointCount) else wavePoints[i + 1] + offset
                
                canvas.drawLine(x1, y1, x2, y2, wavePaint)
            }
        }
    }
    
    /**
     * Effects manager class
     * Responsible for managing various visual effects
     */
    inner class DisgustEffectsManager {
        // Bubble effects
        private val bubbles = mutableListOf<Bubble>()
        private val bubblePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        // Droplet effects
        private val droplets = mutableListOf<Droplet>()
        private val dropletPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        // Corrosion texture
        private val corrosionPoints = mutableListOf<CorrosionPoint>()
        private val corrosionPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        // Rendering properties
        private var width = 0
        private var height = 0
        private var isVertical = false
        
        // Effect parameters
        private var maxBubbles = 50
        private var maxDroplets = 30
        private var corrosionPointCount = 100
        
        /**
         * Initialize effects manager
         */
        fun initialize(width: Int, height: Int, isVertical: Boolean) {
            this.width = width
            this.height = height
            this.isVertical = isVertical
            
            // Adjust maximum effect numbers based on screen size
            val screenSize = width * height
            maxBubbles = (screenSize / 5000).coerceIn(30, 100)
            maxDroplets = (screenSize / 8000).coerceIn(20, 60)
            corrosionPointCount = (screenSize / 3000).coerceIn(80, 200)
            
            // Initialize corrosion points
            initializeCorrosionPoints()
        }
        
        /**
         * Initialize corrosion points
         */
        private fun initializeCorrosionPoints() {
            corrosionPoints.clear()
            
            for (i in 0 until corrosionPointCount) {
                corrosionPoints.add(
                    CorrosionPoint(
                        x = Math.random().toFloat() * width,
                        y = Math.random().toFloat() * height,
                        size = (8f + Math.random().toFloat() * 20f),  // Increase size
                        alpha = 0.6f + Math.random().toFloat() * 0.4f  // Increase opacity
                    )
                )
            }
        }
        
        /**
         * Update effects
         */
        fun update(
            result: AudioAnalysisResult,
            corrosionLevel: Float,
            bubbleRate: Float,
            dropletRate: Float,
            deltaTime: Float,
            smoothingFactor: Float
        ) {
            // Update bubbles
            updateBubbles(result, bubbleRate, deltaTime, smoothingFactor)
            
            // Update droplets
            updateDroplets(result, dropletRate, deltaTime, smoothingFactor)
            
            // Update corrosion points
            updateCorrosionPoints(result, corrosionLevel, deltaTime)
        }
        
        /**
         * Update bubbles
         */
        private fun updateBubbles(
            result: AudioAnalysisResult,
            bubbleRate: Float,
            deltaTime: Float,
            smoothingFactor: Float
        ) {
            // Remove expired bubbles
            bubbles.removeAll { bubble ->
                bubble.lifespan -= deltaTime
                bubble.y -= bubble.speed * deltaTime
                bubble.size += bubble.growRate * deltaTime
                bubble.alpha -= bubble.fadeRate * deltaTime
                
                // Remove if lifespan ends or moves off screen
                bubble.lifespan <= 0 || bubble.y < -bubble.size || 
                bubble.alpha <= 0 || bubble.x < -bubble.size || 
                bubble.x > width + bubble.size
            }
            
            // Generate new bubbles
            val bubbleChance = bubbleRate * smoothingFactor * 60f * deltaTime
            if (bubbles.size < maxBubbles && Math.random() < bubbleChance) {
                // Bubble generation position - random in bottom area
                val x = Math.random().toFloat() * width
                val y = height - Math.random().toFloat() * height * 0.2f
                
                // Bubble size and speed - based on mid frequency energy
                val size = 5f + Math.random().toFloat() * 10f * (1f + result.midFrequencyEnergy)
                val speed = 20f + Math.random().toFloat() * 40f * (1f + result.midFrequencyEnergy * 0.5f)
                
                bubbles.add(
                    Bubble(
                        x = x,
                        y = y,
                        size = size,
                        speed = speed,
                        lifespan = 1f + Math.random().toFloat() * 2f,
                        alpha = 0.7f + Math.random().toFloat() * 0.3f,
                        growRate = size * 0.1f,
                        fadeRate = 0.2f + Math.random().toFloat() * 0.3f
                    )
                )
            }
        }
        
        /**
         * Update droplets
         */
        private fun updateDroplets(
            result: AudioAnalysisResult,
            dropletRate: Float,
            deltaTime: Float,
            smoothingFactor: Float
        ) {
            // Remove expired droplets
            droplets.removeAll { droplet ->
                droplet.lifespan -= deltaTime
                droplet.y += droplet.speed * deltaTime
                droplet.alpha -= droplet.fadeRate * deltaTime
                
                // Remove if lifespan ends or moves off screen
                droplet.lifespan <= 0 || droplet.y > height + droplet.size || 
                droplet.alpha <= 0 || droplet.x < -droplet.size || 
                droplet.x > width + droplet.size
            }
            
            // Generate new droplets
            val dropletChance = dropletRate * smoothingFactor * 40f * deltaTime
            if (droplets.size < maxDroplets && Math.random() < dropletChance) {
                // Droplet generation position - random in top area
                val x = Math.random().toFloat() * width
                val y = Math.random().toFloat() * height * 0.2f
                
                // Droplet size and speed - based on low frequency energy
                val size = 8f + Math.random().toFloat() * 12f * (1f + result.lowFrequencyEnergy)
                val speed = 30f + Math.random().toFloat() * 50f * (1f + result.lowFrequencyEnergy * 0.5f)
                
                droplets.add(
                    Droplet(
                        x = x,
                        y = y,
                        size = size,
                        speed = speed,
                        lifespan = 1f + Math.random().toFloat() * 2f,
                        alpha = 0.8f + Math.random().toFloat() * 0.2f,
                        fadeRate = 0.1f + Math.random().toFloat() * 0.2f
                    )
                )
            }
        }
        
        /**
         * Update corrosion points
         */
        private fun updateCorrosionPoints(
            result: AudioAnalysisResult,
            corrosionLevel: Float,
            deltaTime: Float
        ) {
            // Update size and opacity of corrosion points
            corrosionPoints.forEach { point ->
                // Adjust size based on low frequency energy and corrosion level
                val targetSize = point.baseSize * (1f + corrosionLevel * 0.5f) * 
                               (1f + result.lowFrequencyEnergy * 0.3f)
                
                // Smooth transition
                point.size += (targetSize - point.size) * 2f * deltaTime
                
                // Adjust opacity
                val targetAlpha = point.baseAlpha * corrosionLevel * 
                                (1f + result.lowFrequencyEnergy * 0.2f)
                
                // Smooth transition
                point.alpha += (targetAlpha - point.alpha) * 1.5f * deltaTime
            }
        }
        
        /**
         * Draw corrosion texture
         */
        fun drawCorrosionTexture(
            canvas: Canvas,
            colors: List<Int>,
            corrosionLevel: Float
        ) {
            // Set corrosion point color
            corrosionPaint.color = colors[0]
            
            // Draw corrosion points - increase size and opacity
            corrosionPoints.forEach { point ->
                // Force increase opacity
                val forcedAlpha = (point.alpha * 1.5f).coerceAtMost(1.0f)
                corrosionPaint.alpha = (forcedAlpha * 255).toInt()
                
                // Draw main corrosion point
                canvas.drawCircle(point.x, point.y, point.size * 1.2f, corrosionPaint)
                
                // Draw auxiliary corrosion point - increase texture complexity
                corrosionPaint.color = colors[1]
                corrosionPaint.alpha = (forcedAlpha * 200).toInt()
                canvas.drawCircle(
                    point.x + point.size * 0.3f,
                    point.y - point.size * 0.3f,
                    point.size * 0.6f,
                    corrosionPaint
                )
                corrosionPaint.color = colors[0]
            }
        }
        
        /**
         * Draw bubbles
         */
        fun drawBubbles(
            canvas: Canvas,
            colors: List<Int>,
            energyLevel: Float
        ) {
            // Set bubble color
            bubblePaint.color = colors[0]
            
            // Draw bubbles - using circles
            bubbles.forEach { bubble ->
                bubblePaint.alpha = (bubble.alpha * 255).toInt()
                canvas.drawCircle(bubble.x, bubble.y, bubble.size, bubblePaint)
                
                // Draw bubble highlight
                if (bubble.size > 8f) {
                    bubblePaint.color = colors[1]
                    bubblePaint.alpha = (bubble.alpha * 180).toInt()
                    canvas.drawCircle(
                        bubble.x - bubble.size * 0.3f,
                        bubble.y - bubble.size * 0.3f,
                        bubble.size * 0.3f,
                        bubblePaint
                    )
                    bubblePaint.color = colors[0]
                }
            }
        }
        
        /**
         * Draw droplets
         */
        fun drawDroplets(
            canvas: Canvas,
            colors: List<Int>,
            energyLevel: Float
        ) {
            // Set droplet color
            dropletPaint.color = colors[0]
            
            // Draw droplets - using oval shapes
            droplets.forEach { droplet ->
                dropletPaint.alpha = (droplet.alpha * 255).toInt()
                
                // Draw droplet body - stretched shape to differentiate from bubbles
                canvas.drawOval(
                    droplet.x - droplet.size * 0.7f,
                    droplet.y - droplet.size * 1.2f,
                    droplet.x + droplet.size * 0.7f,
                    droplet.y + droplet.size * 0.8f,
                    dropletPaint
                )
                
                // Draw droplet tip
                val path = android.graphics.Path()
                path.moveTo(droplet.x - droplet.size * 0.5f, droplet.y + droplet.size * 0.5f)
                path.lineTo(droplet.x, droplet.y + droplet.size * 1.2f)
                path.lineTo(droplet.x + droplet.size * 0.5f, droplet.y + droplet.size * 0.5f)
                path.close()
                canvas.drawPath(path, dropletPaint)
                
                // Draw droplet highlight
                dropletPaint.color = colors[1]
                dropletPaint.alpha = (droplet.alpha * 200).toInt()
                canvas.drawCircle(
                    droplet.x - droplet.size * 0.2f,
                    droplet.y - droplet.size * 0.5f,
                    droplet.size * 0.25f,
                    dropletPaint
                )
                dropletPaint.color = colors[0]
            }
        }
        
        /**
         * Draw rhythm effect
         */
        fun drawRhythmEffect(
            canvas: Canvas,
            color: Int,
            intensity: Float
        ) {
            // Set rhythm effect color
            val rhythmPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                alpha = (intensity * 100).toInt()
            }
            
            // Draw rhythm effect - create flashing effect on screen edges
            val flashWidth = width * 0.1f * intensity
            val flashHeight = height * 0.1f * intensity
            
            // Left flash
            canvas.drawRect(0f, 0f, flashWidth, height.toFloat(), rhythmPaint)
            
            // Right flash
            canvas.drawRect(width - flashWidth, 0f, width.toFloat(), height.toFloat(), rhythmPaint)
            
            // Top flash
            canvas.drawRect(0f, 0f, width.toFloat(), flashHeight, rhythmPaint)
            
            // Bottom flash
            canvas.drawRect(0f, height - flashHeight, width.toFloat(), height.toFloat(), rhythmPaint)
        }
        
        /**
         * Draw high frequency response effect
         */
        fun drawHighFrequencyEffect(
            canvas: Canvas,
            color: Int,
            intensity: Float
        ) {
            // Set high frequency effect color - increase brightness and opacity
            val highFreqPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 3f + intensity * 5f
                this.color = color
                alpha = (intensity * 200).toInt()
            }
            
            // Draw high frequency response effect in screen center - using lightning pattern
            val centerX = width / 2f
            val centerY = height / 2f
            val radius = min(width, height) * 0.3f * intensity
            
            // Draw lightning-like lines
            val segments = 8
            val angleStep = (Math.PI * 2) / segments
            
            for (i in 0 until segments) {
                val startAngle = i * angleStep
                val endAngle = startAngle + angleStep
                
                val innerX = centerX + kotlin.math.cos(startAngle).toFloat() * radius * 0.3f
                val innerY = centerY + kotlin.math.sin(startAngle).toFloat() * radius * 0.3f
                
                val midX = centerX + kotlin.math.cos(startAngle + angleStep * 0.5f).toFloat() * radius * 0.7f
                val midY = centerY + kotlin.math.sin(startAngle + angleStep * 0.5f).toFloat() * radius * 0.7f
                
                val outerX = centerX + kotlin.math.cos(endAngle).toFloat() * radius
                val outerY = centerY + kotlin.math.sin(endAngle).toFloat() * radius
                
                // Draw jagged lines
                val path = android.graphics.Path()
                path.moveTo(innerX, innerY)
                path.lineTo(midX, midY)
                path.lineTo(outerX, outerY)
                canvas.drawPath(path, highFreqPaint)
            }
            
            // Add center circle
            highFreqPaint.style = Paint.Style.FILL
            highFreqPaint.alpha = (intensity * 150).toInt()
            canvas.drawCircle(centerX, centerY, radius * 0.2f, highFreqPaint)
        }
    }
    
    /**
     * Color scheme class
     * Responsible for managing color schemes and dynamic color changes
     */
    inner class DisgustColorScheme {
        // Base colors
        private var baseHue = 150f        // Base hue - cyan-green
        private var baseSaturation = 0.7f // Base saturation
        private var baseBrightness = 0.5f // Base brightness
        
        // Dynamic color changes
        private var hueOffset = 0f
        private var saturationOffset = 0f
        private var brightnessOffset = 0f
        
        /**
         * Initialize color scheme
         */
        fun initialize() {
            // Initialize base colors - cyan-green range for disgust emotion
            baseHue = 150f + (Math.random() * 10 - 5).toFloat()
            baseSaturation = 0.7f
            baseBrightness = 0.5f
            
            // Initialize offsets
            hueOffset = 0f
            saturationOffset = 0f
            brightnessOffset = 0f
        }
        
        /**
         * Update color scheme
         */
        fun update(
            result: AudioAnalysisResult,
            distortionIntensity: Float,
            corrosionLevel: Float,
            deltaTime: Float
        ) {
            // Update hue offset - based on low frequency energy
            val targetHueOffset = result.lowFrequencyEnergy * 15f - 7.5f
            hueOffset += (targetHueOffset - hueOffset) * 1.5f * deltaTime
            
            // Update saturation offset - based on mid frequency energy
            val targetSaturationOffset = result.midFrequencyEnergy * 0.2f - 0.1f
            saturationOffset += (targetSaturationOffset - saturationOffset) * 2f * deltaTime
            
            // Update brightness offset - based on high frequency energy and distortion intensity
            val targetBrightnessOffset = result.highFrequencyEnergy * 0.15f - 
                                       distortionIntensity * 0.1f
            brightnessOffset += (targetBrightnessOffset - brightnessOffset) * 2.5f * deltaTime
            
            // Limit offset ranges
            hueOffset = hueOffset.coerceIn(-10f, 10f)
            saturationOffset = saturationOffset.coerceIn(-0.2f, 0.2f)
            brightnessOffset = brightnessOffset.coerceIn(-0.2f, 0.2f)
        }
        
        /**
         * Get background color
         */
        fun getBackgroundColor(corrosionLevel: Float): Int {
            // Background color is darker, leaning towards cyan-green dark tones
            val h = (baseHue + hueOffset).coerceIn(140f, 160f)
            val s = (0.3f + saturationOffset).coerceIn(0.2f, 0.5f)
            val b = (0.2f + brightnessOffset - corrosionLevel * 0.1f).coerceIn(0.1f, 0.3f)
            
            return Color.HSVToColor(floatArrayOf(h, s, b))
        }
        
        /**
         * Get wave colors
         */
        fun getWaveColors(): List<Int> {
            // Main wave color - brighter cyan-green
            val mainH = (baseHue + hueOffset).coerceIn(145f, 155f)
            val mainS = (baseSaturation + saturationOffset).coerceIn(0.6f, 0.8f)
            val mainB = (baseBrightness + brightnessOffset + 0.2f).coerceIn(0.6f, 0.8f)
            
            // Secondary wave color - brighter and more saturated
            val secondaryH = (baseHue + hueOffset + 5f).coerceIn(145f, 160f)
            val secondaryS = (baseSaturation + saturationOffset + 0.1f).coerceIn(0.7f, 0.9f)
            val secondaryB = (baseBrightness + brightnessOffset + 0.3f).coerceIn(0.7f, 0.9f)
            
            return listOf(
                Color.HSVToColor(floatArrayOf(mainH, mainS, mainB)),
                Color.HSVToColor(floatArrayOf(secondaryH, secondaryS, secondaryB)))
        }
        
        /**
         * Get corrosion colors
         */
        fun getCorrosionColors(): List<Int> {
            // Main corrosion color - darker cyan-green
            val mainH = (baseHue + hueOffset - 5f).coerceIn(140f, 150f)
            val mainS = (baseSaturation + saturationOffset - 0.1f).coerceIn(0.5f, 0.7f)
            val mainB = (baseBrightness + brightnessOffset - 0.2f).coerceIn(0.2f, 0.4f)
            
            // Secondary corrosion color - darker and murkier
            val secondaryH = (baseHue + hueOffset - 10f).coerceIn(135f, 145f)
            val secondaryS = (baseSaturation + saturationOffset - 0.2f).coerceIn(0.4f, 0.6f)
            val secondaryB = (baseBrightness + brightnessOffset - 0.3f).coerceIn(0.1f, 0.3f)
            
            return listOf(
                Color.HSVToColor(floatArrayOf(mainH, mainS, mainB)),
                Color.HSVToColor(floatArrayOf(secondaryH, secondaryS, secondaryB)))
        }
        
        /**
         * Get bubble colors
         */
        fun getBubbleColors(): List<Int> {
            // Main bubble color - semi-transparent cyan-green
            val mainH = (baseHue + hueOffset + 5f).coerceIn(150f, 160f)
            val mainS = (baseSaturation + saturationOffset).coerceIn(0.6f, 0.8f)
            val mainB = (baseBrightness + brightnessOffset + 0.1f).coerceIn(0.5f, 0.7f)
            
            // Bubble highlight color - brighter and whiter
            val highlightH = (baseHue + hueOffset).coerceIn(145f, 155f)
            val highlightS = (baseSaturation + saturationOffset - 0.3f).coerceIn(0.3f, 0.5f)
            val highlightB = (baseBrightness + brightnessOffset + 0.4f).coerceIn(0.8f, 1.0f)
            
            return listOf(
                Color.HSVToColor(floatArrayOf(mainH, mainS, mainB)),
                Color.HSVToColor(floatArrayOf(highlightH, highlightS, highlightB)))
        }
        
        /**
         * Get droplet colors
         */
        fun getDropletColors(): List<Int> {
            // Main droplet color - deeper cyan-green
            val mainH = (baseHue + hueOffset - 5f).coerceIn(140f, 150f)
            val mainS = (baseSaturation + saturationOffset + 0.1f).coerceIn(0.7f, 0.9f)
            val mainB = (baseBrightness + brightnessOffset - 0.1f).coerceIn(0.3f, 0.5f)
            
            // Droplet highlight color - brighter
            val highlightH = (baseHue + hueOffset).coerceIn(145f, 155f)
            val highlightS = (baseSaturation + saturationOffset - 0.2f).coerceIn(0.4f, 0.6f)
            val highlightB = (baseBrightness + brightnessOffset + 0.3f).coerceIn(0.7f, 0.9f)
            
            return listOf(
                Color.HSVToColor(floatArrayOf(mainH, mainS, mainB)),
                Color.HSVToColor(floatArrayOf(highlightH, highlightS, highlightB)))
        }
        
        /**
         * Get rhythm effect color
         */
        fun getRhythmEffectColor(): Int {
            // Rhythm effect color - bright cyan-green
            val h = (baseHue + hueOffset + 10f).coerceIn(155f, 165f)
            val s = (baseSaturation + saturationOffset + 0.2f).coerceIn(0.8f, 1.0f)
            val b = (baseBrightness + brightnessOffset + 0.4f).coerceIn(0.8f, 1.0f)
            
            return Color.HSVToColor(floatArrayOf(h, s, b))
        }
        
        /**
         * Get high frequency color
         */
        fun getHighFrequencyColor(): Int {
            // High frequency effect color - eye-catching cyan-green
            val h = (baseHue + hueOffset + 15f).coerceIn(160f, 170f)
            val s = (baseSaturation + saturationOffset + 0.3f).coerceIn(0.9f, 1.0f)
            val b = (baseBrightness + brightnessOffset + 0.5f).coerceIn(0.9f, 1.0f)
            
            return Color.HSVToColor(floatArrayOf(h, s, b))
        }
    }
    
    /**
     * Audio analyzer class
     * Responsible for analyzing audio data, extracting features for visual mapping
     */
    inner class DisgustAudioAnalyzer {
        // Frequency ranges
        private val lowFreqRange = Pair(0, 10)       // Low frequency range (index)
        private val midFreqRange = Pair(10, 30)      // Mid frequency range (index)
        private val highFreqRange = Pair(30, 100)    // High frequency range (index)
        
        // Analysis state
        private var lastLowFreqEnergy = 0f
        private var lastMidFreqEnergy = 0f
        private var lastHighFreqEnergy = 0f
        private var lastRhythmIntensity = 0f
        
        // Voice detection
        private var voiceDetectionThreshold = 0.6f
        private var voiceFreqRange = Pair(15, 25)    // Voice frequency range (index)
        
        /**
         * Analyze audio data
         */
        fun analyzeAudioData(
            data: FloatArray,
            totalEnergy: Float,
            isRhythmPeak: Boolean,
            volumeChange: Float,
            deltaTime: Float,
            smoothingFactor: Float
        ): AudioAnalysisResult {
            val result = AudioAnalysisResult()
            
            // Calculate energy for each frequency band
            val lowFreqEnergy = calculateFrequencyRangeEnergy(data, lowFreqRange)
            val midFreqEnergy = calculateFrequencyRangeEnergy(data, midFreqRange)
            val highFreqEnergy = calculateFrequencyRangeEnergy(data, highFreqRange)
            
            // Smooth processing - use smoothingFactor to control response speed
            val energySmoothingFactor = smoothingFactor * 3f * deltaTime
            
            val smoothedLowFreqEnergy = lastLowFreqEnergy + 
                                      (lowFreqEnergy - lastLowFreqEnergy) * energySmoothingFactor
            val smoothedMidFreqEnergy = lastMidFreqEnergy + 
                                      (midFreqEnergy - lastMidFreqEnergy) * energySmoothingFactor
            val smoothedHighFreqEnergy = lastHighFreqEnergy + 
                                       (highFreqEnergy - lastHighFreqEnergy) * energySmoothingFactor
            
            // Update last energy values
            lastLowFreqEnergy = smoothedLowFreqEnergy
            lastMidFreqEnergy = smoothedMidFreqEnergy
            lastHighFreqEnergy = smoothedHighFreqEnergy
            
            // Calculate rhythm intensity
            val rhythmIntensity = if (isRhythmPeak) {
                min(volumeChange * 5f, 1f)
            } else {
                0f
            }
            
            // Smooth process rhythm intensity
            val rhythmSmoothingFactor = smoothingFactor * 5f * deltaTime
            val smoothedRhythmIntensity = lastRhythmIntensity + 
                                        (rhythmIntensity - lastRhythmIntensity) * rhythmSmoothingFactor
            
            // Update last rhythm intensity
            lastRhythmIntensity = smoothedRhythmIntensity
            
            // Detect dominant frequency
            val dominantFreqIndex = findDominantFrequency(data)
            val dominantFreq = dominantFreqIndex * (44100f / (data.size * 2))
            
            // Detect voice presence
            val voiceEnergy = calculateFrequencyRangeEnergy(data, voiceFreqRange)
            val voicePresence = if (voiceEnergy > voiceDetectionThreshold && 
                                  midFreqEnergy > lowFreqEnergy && 
                                  midFreqEnergy > highFreqEnergy) {
                min(voiceEnergy * 1.5f, 1f)
            } else {
                0f
            }
            
            // Calculate noise level - ratio of high frequency energy to total energy
            val noiseLevel = if (totalEnergy > 0.1f) {
                min(highFreqEnergy / totalEnergy * 2f, 1f)
            } else {
                0f
            }
            
            // Fill results
            result.lowFrequencyEnergy = smoothedLowFreqEnergy
            result.midFrequencyEnergy = smoothedMidFreqEnergy
            result.highFrequencyEnergy = smoothedHighFreqEnergy
            result.totalEnergy = totalEnergy
            result.isRhythmPeak = isRhythmPeak
            result.rhythmIntensity = smoothedRhythmIntensity
            result.volumeChange = volumeChange
            result.dominantFrequency = dominantFreq
            result.voicePresence = voicePresence
            result.noiseLevel = noiseLevel
            
            return result
        }
        
        /**
         * Calculate energy within frequency range
         */
        private fun calculateFrequencyRangeEnergy(data: FloatArray, range: Pair<Int, Int>): Float {
            var energy = 0f
            val validStart = range.first.coerceIn(0, data.size - 1)
            val validEnd = range.second.coerceIn(0, data.size - 1)
            
            for (i in validStart until validEnd) {
                energy += data[i] * data[i]
            }
            
            // Normalize
            val rangeSize = validEnd - validStart
            return if (rangeSize > 0) {
                min(energy / rangeSize * 4f, 1f)
            } else {
                0f
            }
        }
        
        /**
         * Find dominant frequency
         */
        private fun findDominantFrequency(data: FloatArray): Int {
            var maxEnergy = 0f
            var maxIndex = 0
            
            for (i in 1 until data.size) {
                if (data[i] > maxEnergy) {
                    maxEnergy = data[i]
                    maxIndex = i
                }
            }
            
            return maxIndex
        }
    }
    
    // Data class definitions
    
    /**
     * Bubble data class
     */
    private data class Bubble(
        var x: Float,
        var y: Float,
        var size: Float,
        var speed: Float,
        var lifespan: Float,
        var alpha: Float,
        var growRate: Float,
        var fadeRate: Float
    )
    
    /**
     * Droplet data class
     */
    private data class Droplet(
        var x: Float,
        var y: Float,
        var size: Float,
        var speed: Float,
        var lifespan: Float,
        var alpha: Float,
        var fadeRate: Float
    )
    
    /**
     * Corrosion point data class
     */
    private data class CorrosionPoint(
        val x: Float,
        val y: Float,
        var size: Float,
        var alpha: Float,
        val baseSize: Float = size,
        val baseAlpha: Float = alpha
    )
}