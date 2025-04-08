package com.example.musicvibrationapp.view

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SpectrumParameters {
    // StateFlow for each parameter
    private val _smoothingFactor = MutableStateFlow(0.28f)
    val smoothingFactor: StateFlow<Float> = _smoothingFactor.asStateFlow()

    private val _minThreshold = MutableStateFlow(0.05f)
    val minThreshold: StateFlow<Float> = _minThreshold.asStateFlow()

    private val _fallSpeed = MutableStateFlow(0.08f)
    val fallSpeed: StateFlow<Float> = _fallSpeed.asStateFlow()

    private val _minFallSpeed = MutableStateFlow(0.02f)
    val minFallSpeed: StateFlow<Float> = _minFallSpeed.asStateFlow()

    private val _melSensitivity = MutableStateFlow(0.3f)
    val melSensitivity: StateFlow<Float> = _melSensitivity.asStateFlow()

    private val _soloResponseStrength = MutableStateFlow(0.15f)
    val soloResponseStrength: StateFlow<Float> = _soloResponseStrength.asStateFlow()

    // add the StateFlow for DisplayMode
    private val _displayMode = MutableStateFlow(DisplayMode.TOP_BOTTOM)
    val displayMode: StateFlow<DisplayMode> = _displayMode.asStateFlow()

    // Parameter ranges
    object Ranges {
        val SMOOTHING_FACTOR = 0.1f..0.5f
        val MIN_THRESHOLD = 0.01f..0.1f
        val FALL_SPEED = 0.05f..0.2f
        val MIN_FALL_SPEED = 0.01f..0.05f
        val MEL_SENSITIVITY = 0.2f..1.0f
        val SOLO_RESPONSE = 0.1f..0.3f
    }

    // Update functions
    fun updateSmoothingFactor(value: Float) {
        _smoothingFactor.value = value.coerceIn(Ranges.SMOOTHING_FACTOR)
    }

    fun updateMinThreshold(value: Float) {
        _minThreshold.value = value.coerceIn(Ranges.MIN_THRESHOLD)
    }

    fun updateFallSpeed(value: Float) {
        _fallSpeed.value = value.coerceIn(Ranges.FALL_SPEED)
    }

    fun updateMinFallSpeed(value: Float) {
        _minFallSpeed.value = value.coerceIn(Ranges.MIN_FALL_SPEED)
    }

    fun updateMelSensitivity(value: Float) {
        _melSensitivity.value = value.coerceIn(Ranges.MEL_SENSITIVITY)
    }

    fun updateSoloResponseStrength(value: Float) {
        _soloResponseStrength.value = value.coerceIn(Ranges.SOLO_RESPONSE)
    }

    // add the update function for DisplayMode
    fun updateDisplayMode(mode: DisplayMode) {
        _displayMode.value = mode
    }

    // Reset to defaults
    fun resetToDefaults() {
        updateDisplayMode(DisplayMode.TOP_BOTTOM)
        updateSmoothingFactor(0.28f)
        updateMinThreshold(0.05f)
        updateFallSpeed(0.08f)
        updateMinFallSpeed(0.02f)
        updateMelSensitivity(0.3f)
        updateSoloResponseStrength(0.15f)
    }
} 