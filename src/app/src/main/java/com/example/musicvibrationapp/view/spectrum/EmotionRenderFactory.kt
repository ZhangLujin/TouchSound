package com.example.musicvibrationapp.view.spectrum

import android.content.Context
import com.example.musicvibrationapp.view.EmotionColorManager
import com.example.musicvibrationapp.view.spectrum.renderers.JoyParticleRenderer
import com.example.musicvibrationapp.view.spectrum.renderers.DefaultBarRenderer
import com.example.musicvibrationapp.view.spectrum.renderers.TrustWaveRenderer
import com.example.musicvibrationapp.view.spectrum.renderers.FearFractalRenderer
import com.example.musicvibrationapp.view.spectrum.renderers.SurpriseFlashRenderer
import com.example.musicvibrationapp.view.spectrum.renderers.SadnessDropletRenderer
import com.example.musicvibrationapp.view.spectrum.renderers.DisgustDistortionRenderer
import com.example.musicvibrationapp.view.spectrum.renderers.AngerExplosiveRenderer
import com.example.musicvibrationapp.view.spectrum.renderers.AnticipationParticleRenderer
/**
 * Emotion rendering strategy factory
 * Creates corresponding rendering strategies based on emotion types
 */
object EmotionRenderFactory {
    fun createStrategy(
        emotionType: EmotionColorManager.EmotionType,
        context: Context
    ): EmotionRenderStrategy {
        return when (emotionType) {
            EmotionColorManager.EmotionType.JOY -> JoyParticleRenderer(context)
            EmotionColorManager.EmotionType.TRUST -> TrustWaveRenderer(context)
            EmotionColorManager.EmotionType.FEAR -> FearFractalRenderer(context)
            EmotionColorManager.EmotionType.SURPRISE -> SurpriseFlashRenderer(context)
            EmotionColorManager.EmotionType.SADNESS -> SadnessDropletRenderer(context)
            EmotionColorManager.EmotionType.DISGUST -> DisgustDistortionRenderer(context)
            EmotionColorManager.EmotionType.ANGER -> AngerExplosiveRenderer(context)
            EmotionColorManager.EmotionType.ANTICIPATION -> AnticipationParticleRenderer(context)
            // other situations temporarily use default renderer
            else -> DefaultBarRenderer(context)
        }
    }
} 