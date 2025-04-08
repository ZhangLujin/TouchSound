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
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.random.Random

/**
 * droplet renderer for sadness emotions
 * use multi-level droplet system to represent sadness emotions, create blue-tone falling particle effect
 */
class SadnessDropletRenderer(context: Context) : BaseEmotionRenderer(context) {
    
    // auxiliary systems
    private val dropletSystem = SadnessDropletSystem()
    private val effectsManager = SadnessEffectsManager()
    private val audioAnalyzer = SadnessAudioAnalyzer()
    
    // droplet system - divided into three layers
    private val foregroundDroplets = mutableListOf<Droplet>() // foreground large droplets
    private val midgroundDroplets = mutableListOf<Droplet>()  // midground medium droplets
    private val backgroundDroplets = mutableListOf<Droplet>() // background small droplets
    
    // ripple effect
    private val ripples = mutableListOf<Ripple>()
    
    // drawing tools
    private val dropletPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val ripplePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    private val trailPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    // water surface level - relative to screen height
    private var waterSurfaceLevel = 0.9f
    
    // last analysis result cache
    private var lastAnalysisResult: SadnessAudioAnalyzer.AudioAnalysisResult? = null
    
    // low frequency energy accumulation
    private var lowFrequencyEnergy = 0f
    private var lowFrequencyEnergySmoothed = 0f
    
    // fog effect
    private val fogPoints = mutableListOf<FogPoint>()
    private val fogPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    init {
        // set droplet contact water surface callback
        dropletSystem.setOnDropletHitWaterListener { x, y, size, intensity ->
            effectsManager.generateRipple(x, y, size, intensity)
        }
        
        // set water surface level
        dropletSystem.waterSurfaceLevel = waterSurfaceLevel
    }
    
    /**
     * process spectrum data
     */
    override fun onProcessData(processedData: FloatArray): FloatArray {
        // analyze audio data - use smoothingFactor to adjust sensitivity
        val analysisResult = audioAnalyzer.analyzeAudioData(processedData, deltaTime)
        lastAnalysisResult = analysisResult
        
        // increase low frequency energy response - use melSensitivity to adjust enhancement, very small impact
        val lowFreqResponseFactor = 1.0f + SpectrumParameters.melSensitivity.value * 0.3f
        lowFrequencyEnergy = analysisResult.lowFrequencyEnergy * 5.0f * lowFreqResponseFactor
        
        // use smoothingFactor to adjust smoothing factor, very small impact
        val smoothingRate = 0.2f + SpectrumParameters.smoothingFactor.value * 0.06f
        lowFrequencyEnergySmoothed += (lowFrequencyEnergy - lowFrequencyEnergySmoothed) * smoothingRate
        
        // update droplets
        dropletSystem.updateAllDroplets(
            deltaTime,
            SpectrumParameters.fallSpeed.value,
            SpectrumParameters.minFallSpeed.value,
            this::createCustomMusicForceField,  // use custom method
            this::createCustomRhythmResponse    // use custom method
        )
        
        // update ripples
        effectsManager.updateRipples(deltaTime)
        
        // update fog
        effectsManager.updateFog(deltaTime)
        
        // force generate droplets - use minThreshold to adjust base generation rate, very small impact
        val baseGenerationRate = 0.3f + SpectrumParameters.minThreshold.value * 0.3f
        if (Random.nextFloat() < baseGenerationRate) {
            dropletSystem.generateRhythmDroplets(
                0.5f,  // fixed higher energy value
                0.1f,  // fixed higher volume change
                1,     // minimum intensity
                SpectrumParameters.fallSpeed.value
            )
        }
        
        // generate larger droplets at rhythm points - reduce trigger threshold
        if (analysisResult.isRhythmPeak || analysisResult.volumeChange > 0.01f || Random.nextFloat() < 0.1f) {
            dropletSystem.generateRhythmDroplets(
                totalEnergy.coerceAtLeast(0.3f),  // ensure minimum energy
                volumeChange.coerceAtLeast(0.05f),  // ensure minimum volume change
                analysisResult.rhythmIntensity.coerceAtLeast(1),  // ensure minimum intensity
                SpectrumParameters.fallSpeed.value
            )
        }
        
        return processedData
    }
    
    /**
     * create custom music force field effect
     * note: this is not overriding the parent class method, but creating a new method for internal use
     */
    private fun createCustomMusicForceField(x: Float, y: Float, vx: Float, vy: Float): Pair<Float, Float> {
        // enhance force field effect - use soloResponseStrength to adjust force field strength, very small impact
        val forceFieldFactor = 1.0f + SpectrumParameters.soloResponseStrength.value * 0.3f
        val forceFieldStrength = 0.15f + lowFrequencyEnergySmoothed * 0.4f * forceFieldFactor
        
        // create gentle wind effect - use sine wave to simulate wind wave
        val time = System.currentTimeMillis() / 1000f
        val windX = kotlin.math.sin(time * 0.5f + y * 10f) * forceFieldStrength
        
        // apply force field
        return Pair(vx + windX, vy)
    }
    
    /**
     * create custom rhythm response effect
     * note: this is not overriding the parent class method, but creating a new method for internal use
     */
    private fun createCustomRhythmResponse(
        vx: Float, 
        vy: Float, 
        pulseWeight: Float = 1.0f, 
        flowWeight: Float = 1.0f
    ): Pair<Float, Float> {
        // get last analysis result
        val analysisResult = lastAnalysisResult ?: return Pair(vx, vy)
        
        // enhance rhythm response - use soloResponseStrength to adjust response strength, very small impact
        val responseFactor = 1.0f + SpectrumParameters.soloResponseStrength.value * 0.25f
        val enhancedPulseWeight = pulseWeight * 2.5f * responseFactor
        val enhancedFlowWeight = flowWeight * 2.5f * responseFactor
        
        // apply pulse effect - increase droplet speed at rhythm points
        val pulseFactor = if (analysisResult.isRhythmPeak) {
            1.8f + analysisResult.rhythmIntensity * enhancedPulseWeight  // enhance effect
        } else {
            1.3f  // ensure base effect
        }
        
        // apply flow effect - low frequency energy affects horizontal movement of droplets
        val flowFactor = (analysisResult.lowFrequencyEnergy * enhancedFlowWeight).coerceAtLeast(0.15f)  // ensure minimum effect
        val flowX = kotlin.math.sin(System.currentTimeMillis() / 1000f) * flowFactor
        
        // apply effect
        return Pair(vx * pulseFactor + flowX, vy * pulseFactor)
    }
    
    /**
     * render spectrum
     */
    override fun onRender(canvas: Canvas, data: FloatArray, width: Int, height: Int, isVertical: Boolean) {
        val maxWidth = width.toFloat()
        val maxHeight = height.toFloat()
        
        // get current analysis result, if not then analyze again
        val analysisResult = lastAnalysisResult ?: audioAnalyzer.analyzeAudioData(data, deltaTime)
        
        // // check if volume is 0, if so then do not render anything //TODO: adjust according to actual situation
        // if (analysisResult.currentVolume <= 0.001f) {
        //     
        //     return
        // }
        
        // draw gradient background
        effectsManager.drawGradientBackground(canvas, maxWidth, maxHeight)
        
        // draw fog effect - ensure some fog even at low volume
        effectsManager.drawFog(canvas, maxWidth, maxHeight, isVertical)
        
        // draw water surface
        effectsManager.drawWaterSurface(
            canvas, 
            maxWidth, 
            maxHeight, 
            isVertical, 
            waterSurfaceLevel,
            lowFrequencyEnergySmoothed.coerceAtLeast(0.2f)  // ensure minimum effect
        )
        
        // draw droplets
        dropletSystem.drawAllDroplets(canvas, maxWidth, maxHeight, isVertical)
        
        // draw ripples
        effectsManager.drawRipples(canvas, maxWidth, maxHeight, isVertical)
        
        // force generate some ripples - ensure visual effect
        if (Random.nextFloat() < 0.05f) {
            val x = Random.nextFloat()
            val y = waterSurfaceLevel - 0.01f
            effectsManager.generateRipple(x, y, 0.5f, 0.7f)
        }
    }
    
    /**
     * draw gradient background
     */
    private fun drawGradientBackground(canvas: Canvas, width: Float, height: Float) {
        // create deep blue to deep purple gradient
        val darkBlue = ensureBlueColor(Color.rgb(10, 15, 40), 0.8f)
        val darkPurpleBlue = ensureBlueColor(Color.rgb(20, 20, 50), 0.9f)
        
        val gradient = android.graphics.LinearGradient(
            0f, 0f, 0f, height,
            darkBlue, darkPurpleBlue,
            Shader.TileMode.CLAMP
        )
        
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null
        
        // add weak noise, enhance depth
        val noiseCount = (width * height / 10000).toInt().coerceIn(10, 100)
        paint.color = Color.argb(20, 255, 255, 255)
        
        for (i in 0 until noiseCount) {
            val x = Random.nextFloat() * width
            val y = Random.nextFloat() * height
            val size = 1f + Random.nextFloat() * 2f
            canvas.drawCircle(x, y, size, paint)
        }
    }
    
    /**
     * draw water surface
     */
    private fun drawWaterSurface(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        // water surface position
        val surfaceY = if (isVertical) width * waterSurfaceLevel else height * waterSurfaceLevel
        
        // water color - deep blue translucent
        val waterColor = ensureBlueColor(Color.rgb(30, 50, 100), 0.7f)
        paint.color = Color.argb(40, Color.red(waterColor), Color.green(waterColor), Color.blue(waterColor))
        
        // draw water surface
        if (isVertical) {
            canvas.drawRect(surfaceY, 0f, width, height, paint)
        } else {
            canvas.drawRect(0f, surfaceY, width, height, paint)
        }
        
        // draw water surface line
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.argb(60, Color.red(waterColor), Color.green(waterColor), Color.blue(waterColor))
        
        // create wave path
        val path = Path()
        val amplitude = 2f + lowFrequencyEnergySmoothed * 10f // wave amplitude
        val frequency = 0.05f // wave frequency
        val phase = (System.currentTimeMillis() % 10000) / 10000f * 2f * Math.PI.toFloat() // phase
        
        if (isVertical) {
            path.moveTo(surfaceY, 0f)
            for (x in 0 until height.toInt() step 5) {
                val y = surfaceY + sin(x * frequency + phase) * amplitude
                path.lineTo(y, x.toFloat())
            }
            canvas.drawPath(path, paint)
        } else {
            path.moveTo(0f, surfaceY)
            for (x in 0 until width.toInt() step 5) {
                val y = surfaceY + sin(x * frequency + phase) * amplitude
                path.lineTo(x.toFloat(), y)
            }
            canvas.drawPath(path, paint)
        }
        
        paint.style = Paint.Style.FILL
    }
    
    /**
     * draw droplets
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
            // adjust coordinates according to direction
            val x = if (isVertical) droplet.y * width else droplet.x * width
            val y = if (isVertical) droplet.x * height else droplet.y * height
            
            // draw trail effect
            if (droplet.speed > 0.05f && droplet.size > 0.2f) {
                drawDropletTrail(canvas, droplet, x, y, width, height, isVertical, opacityFactor)
            }
            
            // calculate droplet size
            val baseSize = 5f + SpectrumParameters.fallSpeed.value * 30f
            val radius = droplet.size * baseSize
            
            // create droplet color - from inner blue to edge deep blue
            val innerColor = ensureBlueColor(Color.rgb(150, 200, 255), droplet.intensity)
            val middleColor = ensureBlueColor(Color.rgb(100, 150, 220), droplet.intensity * 0.9f)
            val outerColor = ensureBlueColor(Color.rgb(50, 100, 180), droplet.intensity * 0.8f)
            
            // create multi-level radial gradient
            val colors = intArrayOf(
                Color.argb(
                    (droplet.alpha * 255 * opacityFactor).toInt().coerceIn(0, 255),
                    Color.red(innerColor),
                    Color.green(innerColor),
                    Color.blue(innerColor)
                ),
                Color.argb(
                    (droplet.alpha * 220 * opacityFactor).toInt().coerceIn(0, 255),
                    Color.red(middleColor),
                    Color.green(middleColor),
                    Color.blue(middleColor)
                ),
                Color.argb(
                    (droplet.alpha * 180 * opacityFactor).toInt().coerceIn(0, 255),
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
            
            // draw droplets
            if (droplet.stretchFactor > 1.0f) {
                // stretched droplets - use path
                val path = Path()
                val stretchedHeight = radius * droplet.stretchFactor
                
                // create droplet shape
                path.moveTo(x, y - stretchedHeight)
                
                // right curve
                path.quadTo(
                    x + radius, y - stretchedHeight * 0.5f,
                    x, y + radius * 0.5f
                )
                
                // left curve
                path.quadTo(
                    x - radius, y - stretchedHeight * 0.5f,
                    x, y - stretchedHeight
                )
                
                dropletPaint.style = Paint.Style.FILL
                canvas.drawPath(path, dropletPaint)
            } else {
                // circular droplet
                canvas.drawCircle(x, y, radius, dropletPaint)
            }
            
            // add droplet highlight
            if (droplet.size > 0.3f) {
                val highlightRadius = radius * 0.3f
                val highlightX = x - radius * 0.2f
                val highlightY = y - radius * 0.2f
                
                dropletPaint.shader = RadialGradient(
                    highlightX, highlightY,
                    highlightRadius,
                    Color.argb(
                        (droplet.alpha * 160 * opacityFactor).toInt().coerceIn(0, 255),
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
     * draw droplet trail
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
        val baseSize = 5f + SpectrumParameters.fallSpeed.value * 30f
        val radius = droplet.size * baseSize
        
        // trail length is proportional to speed
        val trailLength = droplet.speed * 100f * droplet.size
        
        // trail direction is opposite to speed direction
        val dirX = -droplet.vx / droplet.speed
        val dirY = -droplet.vy / droplet.speed
        
        // trail start point
        val startX = x
        val startY = y
        
        // trail end point
        val endX = x + dirX * trailLength
        val endY = y + dirY * trailLength
        
        // trail color - use droplet color but more transparent
        val trailColor = ensureBlueColor(Color.rgb(100, 150, 220), droplet.intensity * 0.7f)
        
        // create linear gradient
        val trailGradient = android.graphics.LinearGradient(
            startX, startY, endX, endY,
            Color.argb(
                (droplet.alpha * 150 * opacityFactor).toInt().coerceIn(0, 255),
                Color.red(trailColor),
                Color.green(trailColor),
                Color.blue(trailColor)
            ),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        
        trailPaint.shader = trailGradient
        
        // draw trail
        val path = Path()
        path.moveTo(startX, startY)
        
        // trail width decreases from droplet radius to 0
        val controlX = startX + dirX * trailLength * 0.5f
        val controlY = startY + dirY * trailLength * 0.5f
        
        path.lineTo(endX, endY)
        
        // draw trail
        trailPaint.strokeWidth = radius * 0.8f
        trailPaint.style = Paint.Style.STROKE
        trailPaint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(path, trailPaint)
        
        trailPaint.shader = null
    }
    
    /**
     * draw ripples
     */
    private fun drawRipples(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        ripples.forEach { ripple ->
            // adjust coordinates according to direction
            val x = if (isVertical) ripple.y * width else ripple.x * width
            val y = if (isVertical) ripple.x * height else ripple.y * height
            
            // ripple color
            val rippleColor = ensureBlueColor(Color.rgb(100, 150, 220), ripple.intensity)
            ripplePaint.color = Color.argb(
                (ripple.alpha * 150).toInt().coerceIn(0, 255),
                Color.red(rippleColor),
                Color.green(rippleColor),
                Color.blue(rippleColor)
            )
            
            // ripple width decreases with time
            ripplePaint.strokeWidth = 2f * (1f - ripple.progress)
            
            // draw ripple circle
            canvas.drawCircle(x, y, ripple.radius * ripple.progress, ripplePaint)
        }
    }
    
    /**
     * draw fog effect
     */
    private fun drawFog(canvas: Canvas, width: Float, height: Float, isVertical: Boolean) {
        fogPoints.forEach { fogPoint ->
            // adjust coordinates according to direction
            val x = if (isVertical) fogPoint.y * width else fogPoint.x * width
            val y = if (isVertical) fogPoint.x * height else fogPoint.y * height
            
            // fog color - blue tone
            val fogColor = ensureBlueColor(Color.rgb(100, 150, 220), 0.3f)
            
            fogPaint.shader = RadialGradient(
                x, y,
                fogPoint.size,
                Color.argb(
                    (fogPoint.alpha * 30).toInt().coerceIn(0, 255),
                    Color.red(fogColor),
                    Color.green(fogColor),
                    Color.blue(fogColor)
                ),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            
            canvas.drawCircle(x, y, fogPoint.size, fogPaint)
            fogPaint.shader = null
        }
    }
    
    /**
     * ensure color in blue range
     */
    private fun ensureBlueColor(color: Int, intensity: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        
        // strictly limit in blue range (220-240 degrees)
        hsv[0] = 220f + (intensity * 20f).coerceIn(0f, 20f)
        
        // adjust saturation and brightness, create deep and shallow changes but keep blue theme
        hsv[1] = (0.7f + intensity * 0.3f).coerceIn(0.5f, 1.0f)
        hsv[2] = (0.5f + intensity * 0.5f).coerceIn(0.3f, 1.0f)
        
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
    
    /**
     * droplet data class
     */
    private data class Droplet(
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
        var hitWater: Boolean = false
    )
    
    /**
     * ripple data class
     */
    private data class Ripple(
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
    private data class FogPoint(
        var x: Float,
        var y: Float,
        val size: Float,
        var alpha: Float,
        var lifespan: Float,
        val maxLifespan: Float
    )
}