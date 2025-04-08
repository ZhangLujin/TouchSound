package com.example.musicvibrationapp.view.spectrum.renderers

import android.graphics.Color
import kotlin.math.min

class AnticipationColorManager {
    
    private val minHue = 30f 
    private val maxHue = 45f  
    
    private var cachedColors = mutableMapOf<String, Int>()
    
    fun initialize() {
        cachedColors.clear()
    }
    
    fun getBackgroundGradientColors(energy: Float): IntArray {
        val saturation = 0.7f + energy * 0.3f
        val brightness = 0.5f + energy * 0.3f
        
        return intArrayOf(
            ensureOrangeColor(minHue, saturation, brightness * 0.7f, 255),
            ensureOrangeColor(minHue + (maxHue - minHue) * 0.3f, saturation, brightness * 0.85f, 255),
            ensureOrangeColor(maxHue, saturation * 0.9f, brightness, 255)
        )
    }
    
    fun getBarGradientColors(energy: Float): IntArray {
        val saturation = 0.8f + energy * 0.2f
        val brightness = 0.6f + energy * 0.4f
        
        return intArrayOf(
            ensureOrangeColor(maxHue, saturation, brightness, 255),
            ensureOrangeColor(minHue + (maxHue - minHue) * 0.5f, saturation * 0.9f, brightness * 0.8f, 200)
        )
    }
    
    fun getBasicParticleColor(progress: Float): Int {
        val key = "basic_$progress"
        return cachedColors.getOrPut(key) {
            val hue = minHue + (maxHue - minHue) * progress
            val saturation = 0.8f + progress * 0.2f
            val brightness = 0.7f + progress * 0.3f
            
            ensureOrangeColor(hue, saturation, brightness, 255)
        }
    }
    
    fun getGlowParticleColor(progress: Float): Int {
        val key = "glow_$progress"
        return cachedColors.getOrPut(key) {
            val hue = minHue + (maxHue - minHue) * min(1f, progress * 1.2f)
            val saturation = 0.7f + progress * 0.3f
            val brightness = 0.8f + progress * 0.2f
            
            ensureOrangeColor(hue, saturation, brightness, 255)
        }
    }
    
    fun getTrailParticleColor(progress: Float): Int {
        val key = "trail_$progress"
        return cachedColors.getOrPut(key) {
            val hue = minHue + (maxHue - minHue) * progress * 0.7f
            val saturation = 0.9f
            val brightness = 0.6f + progress * 0.3f
            
            ensureOrangeColor(hue, saturation, brightness, 255)
        }
    }
    
    fun getVocalParticleColor(progress: Float): Int {
        val key = "vocal_$progress"
        return cachedColors.getOrPut(key) {
            val hue = maxHue - (maxHue - minHue) * (1f - progress) * 0.3f
            val saturation = 0.8f
            val brightness = 0.9f
            
            ensureOrangeColor(hue, saturation, brightness, 255)
        }
    }
    
    private fun ensureOrangeColor(hue: Float, saturation: Float, brightness: Float, alpha: Int): Int {
        val clampedHue = hue.coerceIn(minHue, maxHue)
        val hsv = floatArrayOf(clampedHue, saturation, brightness)
        return Color.HSVToColor(alpha, hsv)
    }
    
    fun getEffectColor(type: AnticipationParticleRenderer.EffectType, progress: Float, intensity: Float): Int {
        val key = "effect_${type}_${progress}_$intensity"
        return cachedColors.getOrPut(key) {
            when (type) {
                AnticipationParticleRenderer.EffectType.BURST -> {
                    val hue = maxHue - (maxHue - minHue) * (1f - progress) * 0.2f
                    val saturation = 0.7f + intensity * 0.3f
                    val brightness = 0.8f + intensity * 0.2f
                    
                    ensureOrangeColor(hue, saturation, brightness, 255)
                }
                AnticipationParticleRenderer.EffectType.WAVE -> {
                    val hue = minHue + (maxHue - minHue) * 0.3f
                    val saturation = 0.6f + intensity * 0.2f
                    val brightness = 0.7f + intensity * 0.2f
                    
                    ensureOrangeColor(hue, saturation, brightness, (200 * (1f - progress * 0.7f)).toInt())
                }
                AnticipationParticleRenderer.EffectType.GLOW -> {
                    val hue = maxHue
                    val saturation = 0.4f + intensity * 0.2f
                    val brightness = 0.8f
                    
                    ensureOrangeColor(hue, saturation, brightness, (255 * (1f - progress * 0.9f)).toInt())
                }
                AnticipationParticleRenderer.EffectType.PULSE -> {
                    val hue = minHue + (maxHue - minHue) * 0.2f
                    val saturation = 0.8f + intensity * 0.2f
                    val brightness = 0.6f + intensity * 0.3f
                    
                    ensureOrangeColor(hue, saturation, brightness, (200 * (1f - progress * 0.6f)).toInt())
                }
            }
        }
    }
} 