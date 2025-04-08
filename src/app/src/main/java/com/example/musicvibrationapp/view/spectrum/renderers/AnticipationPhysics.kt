package com.example.musicvibrationapp.view.spectrum.renderers

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class AnticipationPhysics {
    
    // Screen parameters
    private var screenWidth = 0
    private var screenHeight = 0
    
    // Physics parameters
    private var turbulenceStrength = 0.0f
    private var turbulenceFrequency = 0.0f
    private var turbulenceTime = 0.0f
    
    fun initialize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        
        // Initialize turbulence parameters
        turbulenceStrength = 0.2f
        turbulenceFrequency = 0.5f
        turbulenceTime = 0.0f
    }
    
    fun updateForceFields(
        forceFields: MutableList<AnticipationParticleRenderer.ForceField>,
        deltaTime: Float,
        audioFeatures: AnticipationParticleRenderer.AudioFeatures,
        isVertical: Boolean
    ) {
        // Update turbulence parameters
        turbulenceTime += deltaTime * turbulenceFrequency
        turbulenceStrength = 0.2f + audioFeatures.totalEnergy * 0.3f
        
        // Update spiral force field
        for (field in forceFields) {
            if (field.type == AnticipationParticleRenderer.ForceFieldType.SPIRAL) {
                field.direction += deltaTime * (1.0f + turbulenceStrength * sin(turbulenceTime + field.x * 10.0f))
                
                if (field.direction > 2 * Math.PI) {
                    field.direction -= 2 * Math.PI.toFloat()
                }
            }
        }
        
        if (audioFeatures.rhythmIntensity > 1 && Random.nextFloat() < 0.05f * audioFeatures.rhythmIntensity) {
            val x: Float
            val y: Float
            
            if (isVertical) {
                x = 0.5f + Random.nextFloat() * 0.4f
                y = 0.2f + Random.nextFloat() * 0.6f
            } else {
                x = 0.2f + Random.nextFloat() * 0.6f
                y = 0.2f + Random.nextFloat() * 0.6f
            }
            
            val radius = 0.1f + Random.nextFloat() * 0.2f
            val strength = 0.2f + Random.nextFloat() * 0.3f * audioFeatures.rhythmIntensity
            
            forceFields.add(AnticipationParticleRenderer.ForceField(
                x = x,
                y = y,
                radius = radius,
                strength = strength,
                type = AnticipationParticleRenderer.ForceFieldType.ATTRACTOR
            ))
        }
    }
    
    fun calculateTurbulence(x: Float, y: Float, time: Float): Pair<Float, Float> {
        val noiseX = sin(x * 0.01f + time) * cos(y * 0.01f + time * 0.5f)
        val noiseY = cos(x * 0.01f - time * 0.7f) * sin(y * 0.01f + time * 0.3f)
        
        return Pair(
            noiseX * turbulenceStrength * 20f,
            noiseY * turbulenceStrength * 20f
        )
    }
    
    fun applyBoundaryConstraints(x: Float, y: Float, vx: Float, vy: Float, isVertical: Boolean): Pair<Float, Float> {
        var newVx = vx
        var newVy = vy
        
        val boundaryMargin = 50f
        
        if (isVertical) {
            // Vertical mode (TOP_bottom) - Mainly constraint left and right boundaries
            
            if (x < boundaryMargin) {
                newVx += (boundaryMargin - x) * 0.2f
            }
            
            if (x > screenWidth - boundaryMargin && newVx > 0) {
                newVx *= 0.9f
            }
            
            if (y < boundaryMargin) {
                newVy += (boundaryMargin - y) * 0.1f
            } else if (y > screenHeight - boundaryMargin) {
                newVy -= (y - (screenHeight - boundaryMargin)) * 0.1f
            }
        } else {
            
            if (x < boundaryMargin) {
                newVx += (boundaryMargin - x) * 0.1f
            } else if (x > screenWidth - boundaryMargin) {
                newVx -= (x - (screenWidth - boundaryMargin)) * 0.1f
            }
            
            if (y > screenHeight - boundaryMargin / 2) {
                newVy -= (y - (screenHeight - boundaryMargin / 2)) * 0.2f
            }
            
            if (y < boundaryMargin && newVy < 0) {
                newVy *= 0.9f
            }
        }
        
        return Pair(newVx, newVy)
    }
}