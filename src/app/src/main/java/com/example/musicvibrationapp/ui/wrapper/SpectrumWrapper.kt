package com.example.musicvibrationapp.ui.wrapper

import com.example.musicvibrationapp.ui.state.SpectrumState
import com.example.musicvibrationapp.view.AudioSpectrumView
import com.example.musicvibrationapp.view.SpectrumParameters.updateFallSpeed
import com.example.musicvibrationapp.view.SpectrumParameters.updateMelSensitivity
import com.example.musicvibrationapp.view.SpectrumParameters.updateMinFallSpeed
import com.example.musicvibrationapp.view.SpectrumParameters.updateMinThreshold
import com.example.musicvibrationapp.view.SpectrumParameters.updateSmoothingFactor
import com.example.musicvibrationapp.view.SpectrumParameters.updateSoloResponseStrength

class SpectrumWrapper(private val spectrumView: AudioSpectrumView) {
    fun applySettings(state: SpectrumState) {
        spectrumView.apply {
            setDisplayMode(state.displayMode)  // Directly use unified DisplayMode
            // Apply basic parameters
            setResponseSpeed(state.responseSpeed)
            setSensitivity(state.sensitivity)

            // Apply advanced parameters - Use public methods
            updateSmoothingFactor(state.smoothingFactor)
            updateMinThreshold(state.minThreshold)
            updateFallSpeed(state.fallSpeed)
            updateMinFallSpeed(state.minFallSpeed)
            updateMelSensitivity(state.melSensitivity)
            updateSoloResponseStrength(state.soloResponse)
        }
    }

    // Modified extension functions to regular methods
    private fun AudioSpectrumView.setResponseSpeed(value: Float) {
        // Map 0-1 value to actual response speed range
        updateSmoothingFactor(0.5f - (value * 0.4f)) // Higher response speed means lower smoothing factor
    }

    private fun AudioSpectrumView.setSensitivity(value: Float) {
        // Adjust sensitivity
        updateMinThreshold(0.1f - (value * 0.09f)) // Higher sensitivity means lower minimum threshold
    }
} 