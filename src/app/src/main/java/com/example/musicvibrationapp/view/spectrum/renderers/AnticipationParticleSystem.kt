package com.example.musicvibrationapp.view.spectrum.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class AnticipationParticleSystem {
    
    private val particles = mutableListOf<Particle>()
    
    private var screenWidth = 0
    private var screenHeight = 0
    private var isVerticalMode = false
    
    private val particlePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val particlePool = mutableListOf<Particle>()
    
    private val maxParticles = 300
    
    fun initialize(width: Int, height: Int, isVertical: Boolean) {
        screenWidth = width
        screenHeight = height
        isVerticalMode = isVertical
        
        while (particlePool.size < 100) {
            particlePool.add(createParticle())
        }
    }
    
    fun generateParticles(count: Int, audioFeatures: AnticipationParticleRenderer.AudioFeatures) {
        val actualCount = minOf(count, maxParticles - particles.size)
        
        for (i in 0 until actualCount) {
            val particle = if (particlePool.isNotEmpty()) {
                particlePool.removeAt(particlePool.size - 1)
            } else {
                createParticle()
            }
            
            initializeParticle(particle, audioFeatures)
            
            particles.add(particle)
        }
    }
    
    fun generateVocalParticles(count: Int) {
    }
    
    fun update(deltaTime: Float, forceFields: List<AnticipationParticleRenderer.ForceField>, 
               audioFeatures: AnticipationParticleRenderer.AudioFeatures) {
        val expiredParticles = mutableListOf<Particle>()
        
        for (particle in particles) {
            particle.lifespan -= deltaTime
            
            if (particle.lifespan <= 0) {
                expiredParticles.add(particle)
                continue
            }
            
            updateParticlePhysics(particle, deltaTime, forceFields, audioFeatures)
            
            updateParticleProperties(particle, deltaTime, audioFeatures)
        }
        
        if (expiredParticles.isNotEmpty()) {
            particles.removeAll(expiredParticles)
            particlePool.addAll(expiredParticles)
        }
    }
    
    fun render(canvas: Canvas, colorManager: AnticipationColorManager) {
        for (particle in particles) {
            renderParticle(canvas, particle, colorManager)
        }
    }
    
    data class Particle(
        var x: Float = 0f,
        var y: Float = 0f,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var size: Float = 0f,
        var color: Int = 0,
        var alpha: Float = 0f,
        var rotation: Float = 0f,
        var lifespan: Float = 0f,
        var maxLifespan: Float = 0f,
        var type: ParticleType = ParticleType.BASIC
    )
    
    enum class ParticleType {
        BASIC,
        GLOW,
        TRAIL,
        VOCAL   
    }
    
    private fun createParticle(): Particle {
        return Particle()
    }
    
    private fun initializeParticle(particle: Particle, audioFeatures: AnticipationParticleRenderer.AudioFeatures) {
        if (isVerticalMode) {
            particle.x = -Random.nextFloat() * (screenWidth * 0.1f)
            particle.y = Random.nextFloat() * screenHeight
            
            particle.vx = 100f + Random.nextFloat() * 150f + audioFeatures.totalEnergy * 100f
            particle.vy = (Random.nextFloat() - 0.5f) * 50f
        } else {
            particle.x = Random.nextFloat() * screenWidth
            particle.y = screenHeight + Random.nextFloat() * (screenHeight * 0.1f)
            
            particle.vx = (Random.nextFloat() - 0.5f) * 50f
            particle.vy = -100f - Random.nextFloat() * 150f - audioFeatures.totalEnergy * 100f
        }
        
        val baseSize = 3f + Random.nextFloat() * 5f
        particle.size = baseSize * (0.8f + audioFeatures.totalEnergy * 0.4f)
        
        particle.maxLifespan = 2f + Random.nextFloat() * 3f
        particle.lifespan = particle.maxLifespan
        
        particle.rotation = Random.nextFloat() * 360f
        
        particle.type = when {
            Random.nextFloat() < 0.1f + audioFeatures.highFreqEnergy * 0.2f -> ParticleType.GLOW
            Random.nextFloat() < 0.2f + audioFeatures.midFreqEnergy * 0.3f -> ParticleType.TRAIL
            else -> ParticleType.BASIC
        }
        
        particle.alpha = 0.7f + Random.nextFloat() * 0.3f
    }
    
    private fun updateParticlePhysics(
        particle: Particle, 
        deltaTime: Float, 
        forceFields: List<AnticipationParticleRenderer.ForceField>,
        audioFeatures: AnticipationParticleRenderer.AudioFeatures
    ) {
        particle.vy += 10f * deltaTime
        
        for (field in forceFields) {
            val dx = (field.x * screenWidth) - particle.x
            val dy = (field.y * screenHeight) - particle.y
            val distanceSquared = dx * dx + dy * dy
            val maxDistanceSquared = field.radius * field.radius * screenWidth * screenHeight
            
            if (distanceSquared < maxDistanceSquared) {
                val strength = field.strength * (1f - distanceSquared / maxDistanceSquared)
                
                when (field.type) {
                    AnticipationParticleRenderer.ForceFieldType.UPWARD -> {
                        particle.vy -= 200f * strength * deltaTime
                    }
                    AnticipationParticleRenderer.ForceFieldType.SPIRAL -> {
                        val distance = Math.sqrt(distanceSquared.toDouble()).toFloat()
                        if (distance > 0) {
                            val normalizedDx = dx / distance
                            val normalizedDy = dy / distance
                            
                            val tangentX = -normalizedDy
                            val tangentY = normalizedDx
                            
                            particle.vx += tangentX * 150f * strength * deltaTime
                            particle.vy += tangentY * 150f * strength * deltaTime
                        }
                    }
                    AnticipationParticleRenderer.ForceFieldType.PULSE -> {
                        val distance = Math.sqrt(distanceSquared.toDouble()).toFloat()
                        if (distance > 0) {
                            val normalizedDx = dx / distance
                            val normalizedDy = dy / distance
                            
                            particle.vx += normalizedDx * 300f * strength * deltaTime
                            particle.vy += normalizedDy * 300f * strength * deltaTime
                        }
                    }
                    AnticipationParticleRenderer.ForceFieldType.ATTRACTOR -> {
                        val distance = Math.sqrt(distanceSquared.toDouble()).toFloat()
                        if (distance > 0) {
                            val normalizedDx = dx / distance
                            val normalizedDy = dy / distance
                            
                            particle.vx += normalizedDx * 200f * strength * deltaTime
                            particle.vy += normalizedDy * 200f * strength * deltaTime
                        }
                    }
                }
            }
        }
        
        particle.vx *= (1f - 0.5f * deltaTime)
        particle.vy *= (1f - 0.3f * deltaTime)
        
        particle.x += particle.vx * deltaTime
        particle.y += particle.vy * deltaTime
        
        particle.rotation += deltaTime * 20f * (particle.vx + particle.vy) / 100f
    }
    
    private fun updateParticleProperties(
        particle: Particle, 
        deltaTime: Float, 
        audioFeatures: AnticipationParticleRenderer.AudioFeatures
    ) {
        val progress = 1f - (particle.lifespan / particle.maxLifespan)
        
        particle.alpha = when {
            progress < 0.1f -> progress * 10f * 0.7f
            progress > 0.8f -> (1f - (progress - 0.8f) * 5f) * 0.7f
            else -> 0.7f
        }
        
        val originalSize = particle.size
        
        when (particle.type) {
            ParticleType.GLOW -> {
                val sizeFactor = 1f + 0.2f * sin(progress * 10f + particle.x * 0.01f)
                particle.size *= sizeFactor
                
                particle.size = minOf(particle.size, originalSize * 1.5f)
                
                particle.alpha *= 1.2f
            }
            ParticleType.TRAIL -> {
                particle.vx *= 1.01f
                particle.vy *= 1.01f
                
                particle.size *= 0.99f
            }
            ParticleType.VOCAL -> {
                val sizeFactor = 1f + 0.4f * sin(progress * 15f)
                particle.size *= sizeFactor
                
                particle.size = minOf(particle.size, originalSize * 2f)
                
                particle.alpha *= 1.3f
            }
            else -> {
                val sizeFactor = 1f + 0.1f * sin(progress * 5f)
                particle.size *= sizeFactor
                
                particle.size = minOf(particle.size, originalSize * 1.3f)
            }
        }
        
        particle.size = minOf(particle.size, 15f)
    }
    
    private fun renderParticle(canvas: Canvas, particle: Particle, colorManager: AnticipationColorManager) {
        val progress = 1f - (particle.lifespan / particle.maxLifespan)
        val color = when (particle.type) {
            ParticleType.GLOW -> colorManager.getGlowParticleColor(progress)
            ParticleType.TRAIL -> colorManager.getTrailParticleColor(progress)
            ParticleType.VOCAL -> colorManager.getVocalParticleColor(progress)
            else -> colorManager.getBasicParticleColor(progress)
        }
        
        particlePaint.color = color
        particlePaint.alpha = (particle.alpha * 255).toInt()
        
        when (particle.type) {
            ParticleType.GLOW -> {
                val gradient = RadialGradient(
                    particle.x, particle.y,
                    particle.size * 2f,
                    color,
                    0x00FFFFFF,
                    Shader.TileMode.CLAMP
                )
                particlePaint.shader = gradient
                canvas.drawCircle(particle.x, particle.y, particle.size * 2f, particlePaint)
                particlePaint.shader = null
            }
            ParticleType.TRAIL -> {
                val trailLength = 20f
                val trailEndX = particle.x - particle.vx / 50f * trailLength
                val trailEndY = particle.y - particle.vy / 50f * trailLength
                
                particlePaint.alpha = (particle.alpha * 100).toInt()
                canvas.drawLine(particle.x, particle.y, trailEndX, trailEndY, particlePaint)
                
                particlePaint.alpha = (particle.alpha * 255).toInt()
                canvas.drawCircle(particle.x, particle.y, particle.size, particlePaint)
            }
            else -> {
                canvas.drawCircle(particle.x, particle.y, particle.size, particlePaint)
            }
        }
    }
}