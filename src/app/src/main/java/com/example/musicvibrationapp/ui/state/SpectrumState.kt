package com.example.musicvibrationapp.ui.state

import com.example.musicvibrationapp.view.DisplayMode

data class SpectrumState(
    // Basic settings
    val displayMode: DisplayMode = DisplayMode.TOP_BOTTOM,
    val responseSpeed: Float = 0.5f,
    val sensitivity: Float = 0.6f,
    
    // Advanced settings
    val smoothingFactor: Float = 0.28f,
    val minThreshold: Float = 0.05f,
    val fallSpeed: Float = 0.1f,
    val minFallSpeed: Float = 0.02f,
    val melSensitivity: Float = 0.6f,
    val soloResponse: Float = 0.2f,
    
    // UI state
    val showAdvancedSettings: Boolean = false
) {
    companion object {
        val DEFAULT = SpectrumState()
    }
}