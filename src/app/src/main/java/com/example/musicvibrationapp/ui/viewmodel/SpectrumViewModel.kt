package com.example.musicvibrationapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.musicvibrationapp.view.DisplayMode
import com.example.musicvibrationapp.ui.state.SpectrumState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.musicvibrationapp.ui.storage.SpectrumSettingsStorage
import com.example.musicvibrationapp.ui.wrapper.SpectrumWrapper
import com.example.musicvibrationapp.view.SpectrumParameters

class SpectrumViewModel(
    private val storage: SpectrumSettingsStorage,
    private val wrapper: SpectrumWrapper
) : ViewModel() {
    // Use StateFlow from SpectrumParameters
    val smoothingFactor = SpectrumParameters.smoothingFactor
    val minThreshold = SpectrumParameters.minThreshold
    val fallSpeed = SpectrumParameters.fallSpeed
    val minFallSpeed = SpectrumParameters.minFallSpeed
    val melSensitivity = SpectrumParameters.melSensitivity
    val soloResponseStrength = SpectrumParameters.soloResponseStrength

    private val _displayMode = MutableStateFlow(DisplayMode.TOP_BOTTOM)
    val displayMode: StateFlow<DisplayMode> = _displayMode.asStateFlow()

    private val _showAdvancedSettings = MutableStateFlow(false)
    val showAdvancedSettings: StateFlow<Boolean> = _showAdvancedSettings.asStateFlow()

    init {
        // Load saved settings on initialization
        val savedSettings = storage.loadSettings()
        applySettings(savedSettings)
    }

    // Update methods, each update will save settings
    fun updateSmoothingFactor(value: Float) {
        SpectrumParameters.updateSmoothingFactor(value)
        saveCurrentSettings()
        wrapper.applySettings(getCurrentSettings())
    }

    fun updateMinThreshold(value: Float) {
        SpectrumParameters.updateMinThreshold(value)
        saveCurrentSettings()
        wrapper.applySettings(getCurrentSettings())
    }

    fun updateFallSpeed(value: Float) {
        SpectrumParameters.updateFallSpeed(value)
        saveCurrentSettings()
        wrapper.applySettings(getCurrentSettings())
    }

    fun updateMinFallSpeed(value: Float) {
        SpectrumParameters.updateMinFallSpeed(value)
        saveCurrentSettings()
        wrapper.applySettings(getCurrentSettings())
    }

    fun updateMelSensitivity(value: Float) {
        SpectrumParameters.updateMelSensitivity(value)
        saveCurrentSettings()
        wrapper.applySettings(getCurrentSettings())
    }

    fun updateSoloResponseStrength(value: Float) {
        SpectrumParameters.updateSoloResponseStrength(value)
        saveCurrentSettings()
        wrapper.applySettings(getCurrentSettings())
    }

    fun updateDisplayMode(mode: DisplayMode) {
        _displayMode.value = mode
        saveCurrentSettings()
        wrapper.applySettings(getCurrentSettings())
    }

    fun toggleAdvancedSettings() {
        _showAdvancedSettings.value = !_showAdvancedSettings.value
    }

    fun resetToDefaults() {
        SpectrumParameters.resetToDefaults()
        _displayMode.value = DisplayMode.TOP_BOTTOM
        saveCurrentSettings()
        wrapper.applySettings(getCurrentSettings())
    }

    private fun getCurrentSettings(): SpectrumState {
        return SpectrumState(
            displayMode = displayMode.value,
            smoothingFactor = smoothingFactor.value,
            minThreshold = minThreshold.value,
            fallSpeed = fallSpeed.value,
            minFallSpeed = minFallSpeed.value,
            melSensitivity = melSensitivity.value,
            soloResponse = soloResponseStrength.value
        )
    }

    private fun applySettings(settings: SpectrumState) {
        SpectrumParameters.updateSmoothingFactor(settings.smoothingFactor)
        SpectrumParameters.updateMinThreshold(settings.minThreshold)
        SpectrumParameters.updateFallSpeed(settings.fallSpeed)
        SpectrumParameters.updateMinFallSpeed(settings.minFallSpeed)
        SpectrumParameters.updateMelSensitivity(settings.melSensitivity)
        SpectrumParameters.updateSoloResponseStrength(settings.soloResponse)
        _displayMode.value = settings.displayMode
        wrapper.applySettings(settings)
    }

    private fun saveCurrentSettings() {
        storage.saveSettings(getCurrentSettings())
    }
}