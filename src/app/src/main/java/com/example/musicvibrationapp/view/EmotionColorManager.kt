package com.example.musicvibrationapp.view

import android.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EmotionColorManager {
    private val _currentEmotion = MutableStateFlow<EmotionType>(EmotionType.NEUTRAL)
    val currentEmotion: StateFlow<EmotionType> = _currentEmotion.asStateFlow()

    enum class EmotionType {
        JOY, TRUST, FEAR, SURPRISE, SADNESS, DISGUST, ANGER, ANTICIPATION, NEUTRAL;

        fun getGradientColors(barHeight: Float, maxHeight: Float): IntArray {
            // get the basic configuration
            val config = getEmotionColorConfig(this)
            
            // calculate the dynamic base hue
            val baseHue = config.baseHue + (Math.sin(System.currentTimeMillis() / 1000.0) + 1) * config.hueRange
            
            // calculate the color intensity
            val intensity = (barHeight / maxHeight).coerceIn(0f, 1f)
            
            return intArrayOf(
                Color.WHITE,  // bottom white
                Color.HSVToColor(floatArrayOf(baseHue.toFloat(), 0.5f, 1f)),  // middle transition color
                Color.HSVToColor(floatArrayOf(baseHue.toFloat(), config.saturation, config.brightness))  // top full color
            )
        }

        private data class EmotionColorConfig(
            val baseHue: Float,      // base hue
            val hueRange: Float,     // hue range
            val saturation: Float,   // saturation
            val brightness: Float    // brightness
        )

        private fun getEmotionColorConfig(emotion: EmotionType): EmotionColorConfig {
            return when (emotion) {
                JOY -> EmotionColorConfig(
                    baseHue = 60f + (Math.sin(System.currentTimeMillis() / 1500.0).toFloat() + 1f) * 7.5f,  
                    // base hue: yellow (60°) as the baseline, in the range of 52.5°-67.5° with a 1.5 second cycle
                    // the hue range of 52.5° ~ 67.5° corresponds to warm yellow to golden yellow, conveying joy and warmth
                    hueRange = 7.5f,     // hue range: ±7.5°, staying in the yellow range with a subtle change
                    saturation = 1f + (Math.sin(System.currentTimeMillis() / 1000.0).toFloat() + 1f) * 0.1f,  
                    // saturation: 90%-100% with a 1 second cycle, keeping the color bright
                    brightness = 1f      // brightness: maximum value, representing the cheerful and bright characteristics
                )
                
                TRUST -> EmotionColorConfig(
                    baseHue = 120f + (Math.sin(System.currentTimeMillis() / 2000.0).toFloat() + 1f) * 10f,  
                    // base hue: green (120°) as the baseline, in the range of 110°-130° with a 2 second cycle
                    // this range includes the natural green tone, symbolizing trust, stability and security
                    hueRange = 10f,      // hue range: ±10°, staying in the green range with a mild change
                    saturation = 0.8f,   // saturation: 80%, medium saturation conveying a stable feeling
                    brightness = 0.9f    // brightness: 90%, bright but not刺眼, representing reliability
                )
                
                FEAR -> EmotionColorConfig(
                    baseHue = 0f,        // base hue: 0°, choosing colorless
                    hueRange = 0f,       // hue range: 0°, staying in the colorless state
                    saturation = 0f,     // saturation: 0%, completely removing color, presenting gray
                    brightness = 0.2f + (Math.sin(System.currentTimeMillis() / 1500.0).toFloat() + 1f) * 0.1f  
                    // brightness: in the range of 20%-40% with a 1.5 second cycle, creating a dark and uneasy atmosphere
                )
                
                SURPRISE -> EmotionColorConfig(
                    baseHue = 0f,        // base hue: 0°, choosing colorless
                    hueRange = 0f,       // hue range: 0°, staying in the colorless state
                    saturation = 0f,     // saturation: 0%, completely removing color, highlighting the contrast between light and dark
                    brightness = 0.9f + (Math.sin(System.currentTimeMillis() / 1000.0).toFloat() + 1f) * 0.1f  
                    // brightness: in the range of 90%-100% with a 1 second cycle, simulating a flickering effect
                )
                
                SADNESS -> EmotionColorConfig(
                    baseHue = 240f + (Math.sin(System.currentTimeMillis() / 2000.0).toFloat() + 1f) * 10f,  
                    // base hue: blue (240°) as the baseline, in the range of 230°-250° with a 2 second cycle
                    // this range of blue gives a feeling of sadness and depth
                    hueRange = 10f,      // hue range: ±10°, in the blue range with a gentle change
                    saturation = 0.7f,   // saturation: 70%, reducing saturation to represent sadness
                    brightness = 0.7f    // brightness: 70%, medium but dark, creating a low mood
                )
                
                DISGUST -> EmotionColorConfig(
                    baseHue = 150f + (Math.sin(System.currentTimeMillis() / 2000.0).toFloat() + 1f) * 10f,  
                    // base hue: cyan green (150°) as the baseline, in the range of 140°-160° with a 2 second cycle
                    // this range of hue is easy to make people feel uncomfortable
                    hueRange = 10f,      // hue range: ±10°, in the cyan green range with a gentle change
                    saturation = 0.7f,   // saturation: 70%, medium saturation adding discomfort
                    brightness = 0.5f    // brightness: 50%, dark brightness adding negative emotions
                )
                
                ANGER -> EmotionColorConfig(
                    baseHue = 0f + (Math.sin(System.currentTimeMillis() / 1500.0).toFloat() + 1f) * 5f,  
                    // base hue: red (0°) as the baseline, in the range of -5°-5° with a 1.5 second cycle
                    // staying in the pure red range, emphasizing the anger
                    hueRange = 5f,       // hue range: ±5°, staying in the red range with a small change
                    saturation = 1f,     // saturation: 100%, maximum saturation emphasizing intense emotions
                    brightness = 1f      // brightness: 100%, maximum brightness visual impact
                )
                
                ANTICIPATION -> EmotionColorConfig(
                    baseHue = 30f + (Math.sin(System.currentTimeMillis() / 2000.0).toFloat() + 1f) * 7.5f,  
                    // base hue: orange (30°) as the baseline, in the range of 22.5°-37.5° with a 2 second cycle
                    // this range of orange gives a feeling of expectation and vitality
                    hueRange = 7.5f,     // hue range: ±7.5°, in the orange range with a moderate change
                    saturation = 0.9f,   // saturation: 90%, high saturation to highlight vitality
                    brightness = 0.9f    // brightness: 90%, bright but not excessive, keeping the visual comfort
                )
                
                NEUTRAL -> EmotionColorConfig(
                    baseHue = 270f + (Math.sin(System.currentTimeMillis() / 3000.0).toFloat() + 1f) * 45f,  
                    // base hue: purple (270°) as the baseline, in the range of 225°-315° with a 3 second cycle
                    // this wide range of hue highlights the neutral and peaceful characteristics
                    hueRange = 15f,      // hue range: ±15°, a large range of change adding visual richness
                    saturation = 0.7f,   // saturation: 70%, moderate saturation maintaining calmness
                    brightness = 0.95f   // brightness: 95%, bright but not excessive, avoiding visual fatigue
                )
            }
        }

        // keep the original getColorScheme method as a backup
        fun getColorScheme(index: Int, total: Int): Int {
            val config = getEmotionColorConfig(this)
            return Color.HSVToColor(180, floatArrayOf(
                config.baseHue,
                config.saturation,
                config.brightness
            ))
        }

        fun getRenderType(): RenderType {
            return when(this) {
                JOY -> RenderType.PARTICLE_WAVE
                FEAR -> RenderType.FRACTAL_LINE
                SADNESS -> RenderType.FLUID_DROPLET
                SURPRISE -> RenderType.FLASH_PULSE
                DISGUST -> RenderType.DISTORTION_WAVE
                ANGER -> RenderType.EXPLOSIVE_BARS
                ANTICIPATION -> RenderType.RISING_PARTICLES
                TRUST -> RenderType.SMOOTH_WAVE
                NEUTRAL -> RenderType.DEFAULT_BARS
            }
        }
    }

    fun updateEmotion(llmResponse: String) {
        val emotion = when {
            llmResponse.contains(Regex("(joy|Joy|JOY)")) -> EmotionType.JOY
            llmResponse.contains(Regex("(trust|Trust|TRUST)")) -> EmotionType.TRUST
            llmResponse.contains(Regex("(fear|Fear|FEAR)")) -> EmotionType.FEAR
            llmResponse.contains(Regex("(surprise|Surprise|SURPRISE)")) -> EmotionType.SURPRISE
            llmResponse.contains(Regex("(sadness|Sadness|SADNESS)")) -> EmotionType.SADNESS
            llmResponse.contains(Regex("(disgust|Disgust|DISGUST)")) -> EmotionType.DISGUST
            llmResponse.contains(Regex("(anger|Anger|ANGER)")) -> EmotionType.ANGER
            llmResponse.contains(Regex("(anticipation|Anticipation|ANTICIPATION)")) -> EmotionType.ANTICIPATION
            llmResponse.contains(Regex("(invalid|Invalid|INVALID)")) -> EmotionType.NEUTRAL
            else -> EmotionType.NEUTRAL
        }
        _currentEmotion.value = emotion
    }

    // add a test method to directly set the emotion type
    fun setEmotionForTesting(emotionType: EmotionType) {
        _currentEmotion.value = emotionType
    }

    companion object {
        @Volatile
        private var instance: EmotionColorManager? = null

        fun getInstance(): EmotionColorManager =
            instance ?: synchronized(this) {
                instance ?: EmotionColorManager().also { instance = it }
            }
    }
}

enum class RenderType {
    DEFAULT_BARS,      // default bar chart
    PARTICLE_WAVE,     // particle + wave (joy)
    FRACTAL_LINE,      // fractal line (fear)
    FLUID_DROPLET,     // fluid droplet (sadness)
    FLASH_PULSE,       // flash pulse (surprise)
    DISTORTION_WAVE,   // distortion wave (disgust)
    EXPLOSIVE_BARS,    // explosive bars (anger)
    RISING_PARTICLES,  // rising particles (anticipation)
    SMOOTH_WAVE        // smooth wave (trust)
} 