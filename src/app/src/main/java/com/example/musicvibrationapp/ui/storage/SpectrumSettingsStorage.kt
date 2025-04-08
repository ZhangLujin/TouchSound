package com.example.musicvibrationapp.ui.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.musicvibrationapp.view.DisplayMode
import com.example.musicvibrationapp.ui.state.SpectrumState

class SpectrumSettingsStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveSettings(state: SpectrumState) {
        with(prefs.edit()) {
            putString(KEY_DISPLAY_MODE, state.displayMode.name)
            putFloat(KEY_RESPONSE_SPEED, state.responseSpeed)
            putFloat(KEY_SENSITIVITY, state.sensitivity)
            putFloat(KEY_SMOOTHING_FACTOR, state.smoothingFactor)
            putFloat(KEY_MIN_THRESHOLD, state.minThreshold)
            putFloat(KEY_FALL_SPEED, state.fallSpeed)
            putFloat(KEY_MIN_FALL_SPEED, state.minFallSpeed)
            putFloat(KEY_MEL_SENSITIVITY, state.melSensitivity)
            putFloat(KEY_SOLO_RESPONSE, state.soloResponse)
            apply()
        }
    }

    fun loadSettings(): SpectrumState {
        return SpectrumState(
            displayMode = DisplayMode.valueOf(
                prefs.getString(KEY_DISPLAY_MODE, DisplayMode.TOP_BOTTOM.name) ?: DisplayMode.TOP_BOTTOM.name
            ),
            responseSpeed = prefs.getFloat(KEY_RESPONSE_SPEED, 0.5f),
            sensitivity = prefs.getFloat(KEY_SENSITIVITY, 0.6f),
            smoothingFactor = prefs.getFloat(KEY_SMOOTHING_FACTOR, 0.28f),
            minThreshold = prefs.getFloat(KEY_MIN_THRESHOLD, 0.05f),
            fallSpeed = prefs.getFloat(KEY_FALL_SPEED, 0.1f),
            minFallSpeed = prefs.getFloat(KEY_MIN_FALL_SPEED, 0.02f),
            melSensitivity = prefs.getFloat(KEY_MEL_SENSITIVITY, 0.6f),
            soloResponse = prefs.getFloat(KEY_SOLO_RESPONSE, 0.2f)
        )
    }

    companion object {
        private const val PREFS_NAME = "spectrum_settings"
        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_RESPONSE_SPEED = "response_speed"
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val KEY_SMOOTHING_FACTOR = "smoothing_factor"
        private const val KEY_MIN_THRESHOLD = "min_threshold"
        private const val KEY_FALL_SPEED = "fall_speed"
        private const val KEY_MIN_FALL_SPEED = "min_fall_speed"
        private const val KEY_MEL_SENSITIVITY = "mel_sensitivity"
        private const val KEY_SOLO_RESPONSE = "solo_response"
    }
} 