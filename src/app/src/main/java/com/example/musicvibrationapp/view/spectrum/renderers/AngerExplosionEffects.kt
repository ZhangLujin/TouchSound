package com.example.musicvibrationapp.view.spectrum.renderers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.BlurMaskFilter
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.random.Random
import java.util.Timer
import java.util.TimerTask
import kotlin.math.max
import android.os.Handler
import android.os.Looper

/**
 * Anger emotion crack effect
 * Implements screen breaking visual effects, better expressing the destructive nature of anger
 */
class AngerExplosionEffects {

    // Crack data class
    data class Crack(
        val x: Float,
        val y: Float,
        val paths: List<CrackPath>,
        var duration: Float,
        val maxDuration: Float,
        val intensityLevel: Int,
        val fragments: List<CrackFragment> = generateFragments(intensityLevel),
        var progress: Float = 0f
    )

    // Crack path data class
    data class CrackPath(
        val path: Path,
        val length: Float,
        val width: Float,
        val angle: Float,
        val branches: List<CrackBranch> = mutableListOf(),
        var currentProgress: Float = 0f,
        var glowIntensity: Float = 1.0f
    )

    // Crack branch data class
    data class CrackBranch(
        val path: Path,
        val length: Float,
        val width: Float,
        val startDistance: Float,
        val angle: Float,
        var currentProgress: Float = 0f,
        var glowIntensity: Float = 0.8f
    )

    // Crack fragment data class
    data class CrackFragment(
        var x: Float,
        var y: Float,
        val size: Float,
        val angle: Float,
        val speed: Float,
        val rotationSpeed: Float,
        var rotation: Float = 0f,
        var opacity: Float = 1.0f,
        val vertices: List<Pair<Float, Float>>
    )
    
    // Heat distortion effect data class
    data class HeatDistortion(
        val x: Float,
        val y: Float,
        val radius: Float,
        var duration: Float,
        val maxDuration: Float,
        val intensity: Float,
        var progress: Float = 0f
    )
    
    // Anger particle data class
    data class AngerParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var color: Int,
        var opacity: Float = 1.0f,
        var lifespan: Float,
        val maxLifespan: Float
    )

    // Fire mist particle data class - New addition
    data class FireMistParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var color: Int,
        var opacity: Float = 1.0f,
        var lifespan: Float,
        val maxLifespan: Float,
        var rotation: Float = 0f,
        var rotationSpeed: Float
    )

    private val cracks = mutableListOf<Crack>()
    
    private val heatDistortions = mutableListOf<HeatDistortion>()
    
    private val angerParticles = mutableListOf<AngerParticle>()
    
    private val fireMistParticles = mutableListOf<FireMistParticle>()
    
    var screenWidth = 0
        private set
    var screenHeight = 0
        private set
    var isVerticalMode = false
        private set
    
    private val crackPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    private val glowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
    }
    
    private val fragmentPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val particlePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    // Add this handler at the class level
    private val handler = Handler(Looper.getMainLooper())
    
    /**
     * Initialize crack effect system
     */
    fun initialize(width: Int, height: Int, isVertical: Boolean) {
        screenWidth = width
        screenHeight = height
        isVerticalMode = isVertical
        
        // Initialize paint settings
        crackPaint.apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 3f
        }
        
        glowPaint.apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
            strokeWidth = 8f
        }
    }
    
    /**
     * Update crack effects
     */
    fun update(deltaTime: Float) {
        try {
            synchronized(this) {
                updateCracks(deltaTime)
                updateHeatDistortions(deltaTime)
                updateAngerParticles(deltaTime)
                updateFireMistParticles(deltaTime)  // New addition
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Update cracks
     */
    private fun updateCracks(deltaTime: Float) {
        synchronized(cracks) {
            val cracksToRemove = mutableListOf<Crack>()
            
            for (crack in cracks) {
                // Update duration
                crack.duration -= deltaTime
                
                // Update progress - Use non-linear progress to make cracks appear quickly
                val rawProgress = 1.0f - (crack.duration / crack.maxDuration)
                
                // Use easing function to make cracks appear faster
                crack.progress = if (rawProgress < 0.3f) {
                    // Quickly reach 70% progress in the first 30% of time
                    rawProgress * (2.33f) 
                } else {
                    // Slowly complete the remaining 30% progress in 70% of the time
                    0.7f + (rawProgress - 0.3f) * (0.3f / 0.7f)
                }.coerceIn(0f, 1f)
                
                // Update crack path progress
                for (path in crack.paths) {
                    path.currentProgress = crack.progress
                    
                    // Update branch progress - branches appear with slight delay
                    for (branch in path.branches) {
                        branch.currentProgress = if (crack.progress > 0.3f) {
                            (crack.progress - 0.3f) / 0.7f
                        } else {
                            0f
                        }
                    }
                }
                
                // Check if it should be removed
                if (crack.duration <= 0) {
                    cracksToRemove.add(crack)
                }
            }
            
            // Remove expired cracks
            cracks.removeAll(cracksToRemove)
        }
    }
    
    /**
     * Update heat distortion effects
     */
    private fun updateHeatDistortions(deltaTime: Float) {
        val distortionsToRemove = mutableListOf<HeatDistortion>()
        
        val distortionsCopy = synchronized(heatDistortions) {
            ArrayList(heatDistortions)
        }
        
        for (distortion in distortionsCopy) {
            distortion.duration -= deltaTime
            
            if (distortion.duration <= 0) {
                distortionsToRemove.add(distortion)
            } else {
                // Update progress
                distortion.progress = 1.0f - (distortion.duration / distortion.maxDuration)
                
                // Reduce particle generation frequency to lower computational load
                if (Random.nextFloat() < 0.4f * distortion.intensity) {  // Reduced from 0.7f to 0.4f
                    // Reduce the number of generated particles
                    generateAngerParticles(distortion.x, distortion.y, 0.04f, 2, distortion.intensity.toInt())
                }
            }
        }
        
        // Remove expired heat distortion effects
        synchronized(heatDistortions) {
            heatDistortions.removeAll(distortionsToRemove)
        }
    }
    
    /**
     * Update anger particles
     */
    private fun updateAngerParticles(deltaTime: Float) {
        val particlesToRemove = mutableListOf<AngerParticle>()
        
        val particlesCopy = synchronized(angerParticles) {
            ArrayList(angerParticles)
        }
        
        for (particle in particlesCopy) {
            // Update lifespan
            particle.lifespan -= deltaTime
            
            if (particle.lifespan <= 0) {
                particlesToRemove.add(particle)
            } else {
                // Update position
                particle.x += particle.vx * deltaTime
                particle.y += particle.vy * deltaTime
                
                // Add some random movement
                particle.vx += (Random.nextFloat() - 0.5f) * 0.02f
                particle.vy += (Random.nextFloat() - 0.5f) * 0.02f
                
                // Update opacity
                val lifeFraction = particle.lifespan / particle.maxLifespan
                particle.opacity = lifeFraction * lifeFraction  // Non-linear decay
            }
        }
        
        // Remove expired particles
        synchronized(angerParticles) {
            angerParticles.removeAll(particlesToRemove)
        }
    }
    
    /**
     * Generate explosion effect
     */
    fun generateExplosion(x: Float, y: Float, size: Float, intensityLevel: Int) {
        // Significantly lower the trigger threshold, allowing low intensity to trigger
        if (intensityLevel < 0) return  // Only don't trigger when intensity is negative, allow 0 level to trigger
        
        // Performance optimization: limit the number of explosions existing simultaneously
        synchronized(cracks) {
            if (cracks.size >= 8) {
                // If there are too many cracks already, remove the oldest two
                if (cracks.size > 1) cracks.removeAt(0)
                if (cracks.size > 1) cracks.removeAt(0)
            }
        }
        
        // Adjust explosion size and duration based on intensity level
        val explosionSize = size * when (intensityLevel) {
            3 -> 1.5f  // Reduced from 1.8f to 1.5f to reduce drawing burden
            2 -> 1.3f  // Reduced from 1.5f to 1.3f
            1 -> 1.1f  // Reduced from 1.2f to 1.1f
            else -> 0.9f  // Reduced from 1.0f to 0.9f
        }
        
        // Reduce duration to decrease drawing time
        val duration = when (intensityLevel) {
            3 -> 2.0f + Random.nextFloat() * 0.8f  // Reduced from 2.5f+1.0f to 2.0f+0.8f
            2 -> 1.7f + Random.nextFloat() * 0.6f  // Reduced from 2.0f+0.8f to 1.7f+0.6f
            1 -> 1.3f + Random.nextFloat() * 0.5f  // Reduced from 1.5f+0.7f to 1.3f+0.5f
            else -> 0.8f + Random.nextFloat() * 0.4f  // Reduced from 1.0f+0.5f to 0.8f+0.4f
        }
        
        // Generate crack paths - Optimization: reduce the number of paths
        val paths = generateCrackPaths(x, y, explosionSize, intensityLevel)
        
        // Create new crack
        val crack = Crack(
            x = x,
            y = y,
            paths = paths,
            duration = duration,
            maxDuration = duration,
            intensityLevel = intensityLevel,
            progress = 0f
        )
        
        // Add to the crack list
        synchronized(cracks) {
            cracks.add(crack)
        }
        
        // Also generate heat distortion effect to enhance visual effect - but reduce the number
        if (Random.nextFloat() < 0.7f) {  // 70% chance to generate heat distortion, reducing total count
            generateHeatDistortion(x, y, explosionSize * 0.7f, intensityLevel)
        }
        
        // Generate some anger particles - reduce the quantity
        val particleCount = when (intensityLevel) {
            3 -> 8  // Reduced from more to 8
            2 -> 6
            1 -> 4
            else -> 2
        }
        generateAngerParticles(x, y, explosionSize * 0.4f, particleCount, intensityLevel)
        
        // Lower the threshold and number of chain cracks
        if (intensityLevel >= 1 && Random.nextFloat() < 0.4f) {  // Reduced from 0.6f to 0.4f, reducing chain explosion probability
            // Delay the generation of chain cracks
            val delay = 0.15f + Random.nextFloat() * 0.2f
            
            // Calculate offset
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2
            val distance = explosionSize * (0.6f + Random.nextFloat() * 0.2f)
            
            val newX = (x + cos(angle) * distance).coerceIn(0.05f, 0.95f)
            val newY = (y + sin(angle) * distance).coerceIn(0.05f, 0.95f)
            
            // The size and intensity of the new crack are slightly smaller
            val newSize = explosionSize * (0.6f + Random.nextFloat() * 0.2f)  // Reduced from 0.7f to 0.6f
            val newIntensity = (intensityLevel - 1).coerceAtLeast(0)  // Allow dropping to 0 level intensity
            
            // Delayed generation - use longer delay to reduce instantaneous pressure
            handler.postDelayed({
                try {
                    // Limit the recursion depth of chain explosions
                    if (intensityLevel <= 1) {
                        // Lowest level only generates heat distortion, no more cracks
                        generateHeatDistortionOnly(newX, newY, newSize, newIntensity)
                    } else {
                        generateExplosion(newX, newY, newSize, newIntensity)
                    }
                } catch (e: Exception) {
                    // Ignore exceptions
                }
            }, (delay * 1000).toLong())
        }
    }
    
    /**
     * Generate crack
     */
    fun generateCrack(x: Float, y: Float, size: Float, intensityLevel: Int) {
        // Significantly lower the trigger threshold, allowing low intensity to trigger
        if (intensityLevel < 0) return  // Only don't trigger when intensity is negative, allow 0 level to trigger
        
        // Adjust crack size and duration based on intensity level
        val crackSize = size * when (intensityLevel) {
            3 -> 1.8f
            2 -> 1.5f
            1 -> 1.2f
            else -> 1.0f  // Added support for 0 level intensity
        }
        
        // Reduce duration to make effects more responsive
        val duration = when (intensityLevel) {
            3 -> 2.5f + Random.nextFloat() * 1.0f
            2 -> 2.0f + Random.nextFloat() * 0.8f
            1 -> 1.5f + Random.nextFloat() * 0.7f
            else -> 1.0f + Random.nextFloat() * 0.5f  // Added support for 0 level intensity
        }
        
        // Generate crack paths
        val paths = generateCrackPaths(x, y, crackSize, intensityLevel)
        
        // Generate crack
        val crack = Crack(
            x = x,
            y = y,
            intensityLevel = intensityLevel,
            duration = duration,
            maxDuration = duration,
            progress = 0f,
            paths = paths
        )
        
        // Add to the crack list
        synchronized(cracks) {
            // Limit the maximum number of cracks
            if (cracks.size >= 5) { // Increased from 3 to 5
                cracks.removeAt(0)
            }
            cracks.add(crack)
        }
    }
    
    /**
     * Generate heat distortion
     */
    private fun generateHeatDistortion(x: Float, y: Float, size: Float, intensityLevel: Int) {
        // Adjust heat distortion size and intensity based on intensity level - significantly lower the trigger threshold
        val distortionSize = size * when (intensityLevel) {
            3 -> 1.5f 
            2 -> 1.3f
            1 -> 1.1f
            else -> 0.9f  // Added support for 0 level intensity, allowing it to trigger at low intensity
        }
        
        val distortionIntensity = when (intensityLevel) {
            3 -> 0.6f + Random.nextFloat() * 0.2f
            2 -> 0.4f + Random.nextFloat() * 0.2f
            1 -> 0.3f + Random.nextFloat() * 0.1f
            else -> 0.2f + Random.nextFloat() * 0.1f  // Added support for 0 level intensity
        }
        
        // Create heat distortion
        val distortion = HeatDistortion(
            x = x,
            y = y,
            radius = distortionSize,
            intensity = distortionIntensity,
            duration = 1.5f + Random.nextFloat() * 1.0f, // Reduced duration
            maxDuration = 2.5f,
            progress = 0f
        )
        
        // Add to heat distortion list
        synchronized(heatDistortions) {
            // Limit the maximum number of heat distortions
            if (heatDistortions.size >= 4) { // Reduced maximum number of heat distortions
                heatDistortions.removeAt(0)
            }
            heatDistortions.add(distortion)
        }
    }
    
    /**
     * Generate crack paths
     */
    private fun generateCrackPaths(x: Float, y: Float, size: Float, intensityLevel: Int): List<CrackPath> {
        val paths = mutableListOf<CrackPath>()
        
        // Reduce the number of cracks to lower drawing burden
        val numMainCracks = when (intensityLevel) {
            3 -> 6 + Random.nextInt(3)  // Reduced from 8-11 to 6-8 main cracks
            2 -> 4 + Random.nextInt(3)  // Reduced from 6-8 to 4-6 main cracks
            1 -> 3 + Random.nextInt(2)  // Reduced from 4-6 to 3-4 main cracks
            else -> 2 + Random.nextInt(2)  // Reduced from 2-4 to 2-3 main cracks
        }
        
        // Generate main cracks
        repeat(numMainCracks) {
            // Random angle - ensure more even distribution of cracks
            val angle = (it.toFloat() / numMainCracks) * 2 * Math.PI.toFloat() + 
                        (Random.nextFloat() - 0.5f) * 0.8f
            
            // Crack length - reduce length to lower drawing burden
            val length = (0.2f + Random.nextFloat() * 0.2f) * minOf(screenWidth, screenHeight)  // Reduced from 0.25f-0.5f to 0.2f-0.4f
            
            // Crack width - slightly reduced width
            val width = (2.0f + Random.nextFloat() * 1.5f) * intensityLevel  // Reduced from 2.5f+2.0f to 2.0f+1.5f
            
            // Create crack path
            val path = Path()
            path.moveTo(x * screenWidth, y * screenHeight)
            
            // Generate irregular crack path - reduce segment count to lower computational load
            var currentX = x * screenWidth
            var currentY = y * screenHeight
            val segments = 4 + Random.nextInt(2)  // Reduced from 5+3 to 4+2
            val segmentLength = length / segments
            
            for (i in 1..segments) {
                // Each segment has angle variation
                val segmentAngle = angle + (Random.nextFloat() - 0.5f) * 1.5f
                currentX += cos(segmentAngle) * segmentLength
                currentY += sin(segmentAngle) * segmentLength
                
                // Add random offset
                val offsetX = (Random.nextFloat() - 0.5f) * segmentLength * 0.6f  // Reduced from 0.7f to 0.6f
                val offsetY = (Random.nextFloat() - 0.5f) * segmentLength * 0.6f
                
                path.lineTo(currentX + offsetX, currentY + offsetY)
            }
            
            // Measure path length
            val pathMeasure = PathMeasure(path, false)
            val pathLength = pathMeasure.length
            
            // Generate branch cracks - reduce branch count
            val branches = generateCrackBranches(path, pathLength, width, intensityLevel)
            
            paths.add(CrackPath(
                path = path,
                length = pathLength,
                width = width,
                angle = angle,
                branches = branches
            ))
        }
        
        return paths
    }
    
    private fun generateCrackBranches(mainPath: Path, mainLength: Float, mainWidth: Float, intensityLevel: Int): List<CrackBranch> {
        val branches = mutableListOf<CrackBranch>()
        
        val numBranches = when (intensityLevel) {
            3 -> 2 + Random.nextInt(2)  
            2 -> 1 + Random.nextInt(2)  
            1 -> Random.nextInt(2)      
            else -> if (Random.nextFloat() < 0.5f) 1 else 0  
        }
        
        if (numBranches <= 0) return branches
        
        val pathMeasure = PathMeasure(mainPath, false)
        
        repeat(numBranches) {
            val startDistance = mainLength * (0.2f + Random.nextFloat() * 0.6f)
            
            val branchAngle = (Random.nextFloat() - 0.5f) * Math.PI.toFloat() * 0.8f  
            
            val branchLength = mainLength * (0.3f + Random.nextFloat() * 0.3f)  
            val branchWidth = mainWidth * (0.4f + Random.nextFloat() * 0.3f)  
            val pos = FloatArray(2)
            val tan = FloatArray(2)
            pathMeasure.getPosTan(startDistance, pos, tan)
            
            val mainAngle = Math.atan2(tan[1].toDouble(), tan[0].toDouble()).toFloat()
            
            val actualBranchAngle = mainAngle + branchAngle
            
            val branchPath = Path()
            branchPath.moveTo(pos[0], pos[1])
            
            var currentX = pos[0]
            var currentY = pos[1]
            val segments = 2 + Random.nextInt(2)  
            val segmentLength = branchLength / segments
            
            for (i in 1..segments) {
                val segmentAngle = actualBranchAngle + (Random.nextFloat() - 0.5f) * 0.6f  
                currentX += cos(segmentAngle) * segmentLength
                currentY += sin(segmentAngle) * segmentLength
                
                val offsetX = (Random.nextFloat() - 0.5f) * segmentLength * 0.2f  
                val offsetY = (Random.nextFloat() - 0.5f) * segmentLength * 0.2f
                
                branchPath.lineTo(currentX + offsetX, currentY + offsetY)
            }
            
            val branchPathMeasure = PathMeasure(branchPath, false)
            
            branches.add(CrackBranch(
                path = branchPath,
                length = branchPathMeasure.length,
                width = branchWidth,
                startDistance = startDistance,
                angle = actualBranchAngle
            ))
        }
        
        return branches
    }
    
    fun draw(canvas: Canvas, colorScheme: Any) {
        synchronized(this) {
            drawFireMistParticles(canvas)
            
            drawHeatDistortions(canvas)
            
            drawCracks(canvas)
            
            drawAngerParticles(canvas)
        }
    }
    
    private fun drawCracks(canvas: Canvas) {
        val cracksList = synchronized(cracks) {
            ArrayList(cracks)
        }
        
        for (crack in cracksList) {
            drawCrackFragments(canvas, crack)
            drawCrackPaths(canvas, crack)
        }
    }
    
    private fun drawHeatDistortions(canvas: Canvas) {
        val distortionsList = synchronized(heatDistortions) {
            ArrayList(heatDistortions)
        }
        
        for (distortion in distortionsList) {
            // Calculate screen coordinates
            val centerX = if (isVerticalMode) {
                distortion.y * screenWidth
            } else {
                distortion.x * screenWidth
            }
            
            val centerY = if (isVerticalMode) {
                distortion.x * screenHeight
            } else {
                distortion.y * screenHeight
            }
            
            // Calculate radius (in screen pixels)
            val radiusPixels = distortion.radius * minOf(screenWidth, screenHeight)
            
            // Create radial gradient - greatly enhance color brightness and opacity
            val colors = intArrayOf(
                Color.argb((220 * (1f - distortion.progress * 0.5f)).toInt(), 255, 120, 0),  // Increased from 180 and 100 to 220 and 120
                Color.argb((130 * (1f - distortion.progress * 0.5f)).toInt(), 255, 180, 0),  // Increased from 100 and 150 to 130 and 180
                Color.TRANSPARENT
            )
            
            val positions = floatArrayOf(0f, 0.7f, 1f)
            
            val gradient = android.graphics.RadialGradient(
                centerX, centerY, radiusPixels,
                colors, positions, android.graphics.Shader.TileMode.CLAMP
            )
            
            // Draw heat distortion effect
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                shader = gradient
            }
            
            canvas.drawCircle(centerX, centerY, radiusPixels, paint)
            paint.shader = null
        }
    }
    
    /**
     * Draw crack paths
     */
    private fun drawCrackPaths(canvas: Canvas, crack: Crack) {
        // Crack colors - greatly enhance color brightness
        val crackBaseColor = Color.rgb(255, 70, 70)  // Increased from 255,50,50 to 255,70,70
        val crackGlowColor = Color.rgb(255, 180, 100)  // Increased from 255,150,80 to 255,180,100
        
        for (crackPath in crack.paths) {
            if (crackPath.currentProgress <= 0) continue
            
            // Create clip path to show crack progress
            val clipPath = Path()
            val pathMeasure = PathMeasure(crackPath.path, false)
            pathMeasure.getSegment(0f, pathMeasure.length * crackPath.currentProgress, clipPath, true)
            
            // Draw crack glow effect - greatly enhance glow effect
            glowPaint.color = crackGlowColor
            glowPaint.alpha = (255 * crackPath.glowIntensity).toInt()
            glowPaint.strokeWidth = crackPath.width * 7.0f  // Increased from 6.0f to 7.0f
            canvas.drawPath(clipPath, glowPaint)
            
            // Draw crack body
            crackPaint.color = crackBaseColor
            crackPaint.strokeWidth = crackPath.width
            canvas.drawPath(clipPath, crackPaint)
            
            // Draw crack branches
            for (branch in crackPath.branches) {
                if (branch.currentProgress <= 0) continue
                
                // Create clip path to show branch progress
                val branchClipPath = Path()
                val branchPathMeasure = PathMeasure(branch.path, false)
                branchPathMeasure.getSegment(0f, branchPathMeasure.length * branch.currentProgress, branchClipPath, true)
                
                // Draw branch glow effect - greatly enhance glow effect
                glowPaint.color = crackGlowColor
                glowPaint.alpha = (240 * branch.glowIntensity).toInt()  // Increased from 220 to 240
                glowPaint.strokeWidth = branch.width * 6.0f  // Increased from 5.0f to 6.0f
                canvas.drawPath(branchClipPath, glowPaint)
                
                // Draw branch body
                crackPaint.color = crackBaseColor
                crackPaint.strokeWidth = branch.width
                canvas.drawPath(branchClipPath, crackPaint)
            }
        }
    }
    
    /**
     * Draw crack fragments
     */
    private fun drawCrackFragments(canvas: Canvas, crack: Crack) {
        // Only draw fragments after crack progress exceeds 50%
        if (crack.progress <= 0.5f) return
        
        val fragmentProgress = (crack.progress - 0.5f) * 2f // 0-1 range
        
        for (fragment in crack.fragments) {
            if (fragment.opacity <= 0.05f) continue
            
            // Calculate screen coordinates
            val centerX = if (isVerticalMode) {
                fragment.y * screenWidth
            } else {
                fragment.x * screenWidth
            }
            
            val centerY = if (isVerticalMode) {
                fragment.x * screenHeight
            } else {
                fragment.y * screenHeight
            }
            
            // Draw fragment
            fragmentPaint.color = Color.argb(
                (255 * fragment.opacity).toInt(),
                255, 30, 30
            )
            
            // Save current canvas state
            canvas.save()
            
            // Translate and rotate canvas
            canvas.translate(centerX, centerY)
            canvas.rotate(fragment.rotation)
            
            // Create fragment path
            val fragmentPath = Path()
            if (fragment.vertices.isNotEmpty()) {
                fragmentPath.moveTo(
                    fragment.vertices[0].first * fragment.size,
                    fragment.vertices[0].second * fragment.size
                )
                
                for (i in 1 until fragment.vertices.size) {
                    fragmentPath.lineTo(
                        fragment.vertices[i].first * fragment.size,
                        fragment.vertices[i].second * fragment.size
                    )
                }
                
                fragmentPath.close()
            }
            
            // Draw fragment
            canvas.drawPath(fragmentPath, fragmentPaint)
            
            // Restore canvas state
            canvas.restore()
        }
    }
    
    /**
     * Draw anger particles
     */
    private fun drawAngerParticles(canvas: Canvas) {
        val particlesList = synchronized(angerParticles) {
            ArrayList(angerParticles)
        }
        
        for (particle in particlesList) {
            // Calculate screen coordinates
            val x = if (isVerticalMode) {
                particle.y * screenWidth
            } else {
                particle.x * screenWidth
            }
            
            val y = if (isVerticalMode) {
                particle.x * screenHeight
            } else {
                particle.y * screenHeight
            }
            
            // Set particle color and opacity - enhance opacity
            particlePaint.color = Color.argb(
                ((255 * particle.opacity) * 1.2f).coerceAtMost(255f).toInt(),  // Increase opacity by 1.2 times, but not exceeding 255
                Color.red(particle.color),
                Color.green(particle.color),
                Color.blue(particle.color)
            )
            
            // Draw particle - increase size
            canvas.drawCircle(x, y, particle.size * 1.2f, particlePaint)  // Increase by 1.2 times
        }
    }
    
    /**
     * Generate fire mist effect - New method
     */
    fun generateFireMist(x: Float, y: Float, size: Float, intensity: Float) {
        synchronized(fireMistParticles) {
            // Limit the maximum number of fire mist particles - reduce maximum count
            if (fireMistParticles.size >= 200) {  // Reduced from 300 to 200
                // Remove the oldest particle
                fireMistParticles.removeAt(0)
                // If there are still many particles, remove several at once
                if (fireMistParticles.size >= 180) {
                    for (i in 0 until 5) {
                        if (fireMistParticles.isNotEmpty()) {
                            fireMistParticles.removeAt(0)
                        }
                    }
                }
            }
            
            // Reduce the number of generated particles
            val particleCount = (8 + intensity * 10).toInt().coerceIn(8, 30)  // Reduced from 10+15 and 10-50 to 8+10 and 8-30
            
            for (i in 0 until particleCount) {
                // Random speed and direction - reduce speed
                val speed = 0.04f + Random.nextFloat() * 0.08f * intensity  // Reduced from 0.05f+0.1f to 0.04f+0.08f
                val angle = Random.nextFloat() * Math.PI.toFloat() * 2
                
                // Random size - reduce size
                val particleSize = 6f + Random.nextFloat() * 10f * intensity  // Reduced from 8f+12f to 6f+10f
                
                // Flame colors - red, orange, yellow
                val hue = 5f + Random.nextFloat() * 25f
                val saturation = 0.9f + Random.nextFloat() * 0.1f
                val value = 0.9f + Random.nextFloat() * 0.1f
                
                val hsv = floatArrayOf(hue, saturation, value)
                val color = Color.HSVToColor(hsv)
                
                // Lifespan - reduce duration
                val lifespan = 0.6f + Random.nextFloat() * 1.0f * intensity  // Reduced from 0.8f+1.2f to 0.6f+1.0f
                
                // Rotation - reduce rotation speed
                val rotationSpeed = (Random.nextFloat() - 0.5f) * 70f  // Reduced from 90f to 70f
                
                fireMistParticles.add(FireMistParticle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 0.02f,
                    size = particleSize,
                    color = color,
                    opacity = 0.7f + Random.nextFloat() * 0.3f,
                    lifespan = lifespan,
                    maxLifespan = lifespan,
                    rotationSpeed = rotationSpeed
                ))
            }
        }
    }
    
    /**
     * Update fire mist particles - New method
     */
    private fun updateFireMistParticles(deltaTime: Float) {
        val particlesToRemove = mutableListOf<FireMistParticle>()
        
        val particlesCopy = synchronized(fireMistParticles) {
            ArrayList(fireMistParticles)
        }
        
        for (particle in particlesCopy) {
            // Update lifespan
            particle.lifespan -= deltaTime
            
            if (particle.lifespan <= 0) {
                particlesToRemove.add(particle)
            } else {
                // Update position
                particle.x += particle.vx * deltaTime
                particle.y += particle.vy * deltaTime
                
                // Add some random movement and rising effect
                particle.vx += (Random.nextFloat() - 0.5f) * 0.01f
                particle.vy -= 0.005f * deltaTime  // Drift upward
                
                // Update rotation
                particle.rotation += particle.rotationSpeed * deltaTime
                
                // Update opacity - lower at the beginning and end of lifespan
                val lifeFraction = particle.lifespan / particle.maxLifespan
                particle.opacity = if (lifeFraction > 0.8f) {
                    // Fade in at the beginning
                    (1.0f - (1.0f - lifeFraction) * 5.0f) * 0.8f
                } else if (lifeFraction < 0.3f) {
                    // Fade out at the end
                    lifeFraction * 3.0f * 0.8f
                } else {
                    // Stay stable during the middle period
                    0.8f
                }
                
                // Decrease size with lifespan
                particle.size *= (1.0f - 0.05f * deltaTime)
            }
        }
        
        // Remove expired particles
        synchronized(fireMistParticles) {
            fireMistParticles.removeAll(particlesToRemove)
        }
    }
    
    private fun drawFireMistParticles(canvas: Canvas) {
        val particlesList = synchronized(fireMistParticles) {
            ArrayList(fireMistParticles)
        }
        
        for (particle in particlesList) {
            val x = if (isVerticalMode) {
                particle.y * screenWidth
            } else {
                particle.x * screenWidth
            }
            
            val y = if (isVerticalMode) {
                particle.x * screenHeight
            } else {
                particle.y * screenHeight
            }
            
            particlePaint.color = Color.argb(
                (255 * particle.opacity).toInt(),
                Color.red(particle.color),
                Color.green(particle.color),
                Color.blue(particle.color)
            )
            
            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(particle.rotation)
            
            particlePaint.maskFilter = BlurMaskFilter(particle.size * 0.5f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawCircle(0f, 0f, particle.size, particlePaint)
            
            canvas.restore()
            particlePaint.maskFilter = null
        }
    }
    
    companion object {
        private fun generateFragments(intensityLevel: Int): List<CrackFragment> {
            val fragments = mutableListOf<CrackFragment>()
            
            val fragmentCount = when (intensityLevel) {
                3 -> 5 + Random.nextInt(4)  
                2 -> 3 + Random.nextInt(3)  
                1 -> 2 + Random.nextInt(2)  
                else -> Random.nextInt(2)    
            }
            
            repeat(fragmentCount) {
                val x = 0f
                val y = 0f
                
                val size = 2.5f + Random.nextFloat() * 4f * intensityLevel  
                
                val angle = Random.nextFloat() * Math.PI.toFloat() * 2
                
                val speed = 0.008f + Random.nextFloat() * 0.015f * intensityLevel  
                
                val rotationSpeed = (Random.nextFloat() - 0.5f) * 150f * intensityLevel  
                
                val vertexCount = 3 + Random.nextInt(2)  
                val vertices = mutableListOf<Pair<Float, Float>>()
                
                for (i in 0 until vertexCount) {
                    val vertexAngle = (i.toFloat() / vertexCount) * 2 * Math.PI.toFloat() +
                                     (Random.nextFloat() - 0.5f) * 0.4f  
                    val distance = 0.5f + Random.nextFloat() * 0.4f  
                    
                    vertices.add(Pair(
                        cos(vertexAngle) * distance,
                        sin(vertexAngle) * distance
                    ))
                }
                
                fragments.add(CrackFragment(
                    x = x,
                    y = y,
                    size = size,
                    angle = angle,
                    speed = speed,
                    rotationSpeed = rotationSpeed,
                    vertices = vertices
                ))
            }
            
            return fragments
        }
    }

    fun generateCrackOnly(x: Float, y: Float, size: Float, intensityLevel: Int) {
        val effectiveIntensity = max(1, intensityLevel)
        try {
            val finalX = x.coerceIn(0.05f, 0.95f)
            val finalY = y.coerceIn(0.05f, 0.95f)
            
            generateCrack(finalX, finalY, size, effectiveIntensity)
            
            generateAngerParticles(finalX, finalY, size * 0.3f, 3, effectiveIntensity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateHeatDistortionOnly(x: Float, y: Float, size: Float, intensityLevel: Int) {
        val effectiveIntensity = max(1, intensityLevel)
        try {
            val finalX = x.coerceIn(0.05f, 0.95f)
            val finalY = y.coerceIn(0.05f, 0.95f)
            generateHeatDistortion(finalX, finalY, size, effectiveIntensity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateAngerParticles(x: Float, y: Float, size: Float, count: Int, intensityLevel: Int) {
        synchronized(angerParticles) {
            if (angerParticles.size >= 200) {
                angerParticles.removeAt(0)
            }
            
            repeat(count) {
                val speed = 0.05f + Random.nextFloat() * 0.1f * intensityLevel
                val angle = Random.nextFloat() * Math.PI.toFloat() * 2
                
                val particleSize = 5f + Random.nextFloat() * 8f * intensityLevel
                
                val hue = 0f + Random.nextFloat() * 30f  
                val saturation = 0.8f + Random.nextFloat() * 0.2f
                val value = 0.8f + Random.nextFloat() * 0.2f
                
                val hsv = floatArrayOf(hue, saturation, value)
                val color = Color.HSVToColor(hsv)
                
                val lifespan = 0.5f + Random.nextFloat() * 1.0f * intensityLevel
                
                angerParticles.add(AngerParticle(
                    x = x + (Random.nextFloat() - 0.5f) * size * 2,
                    y = y + (Random.nextFloat() - 0.5f) * size * 2,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    size = particleSize,
                    color = color,
                    lifespan = lifespan,
                    maxLifespan = lifespan
                ))
            }
        }
    }
}