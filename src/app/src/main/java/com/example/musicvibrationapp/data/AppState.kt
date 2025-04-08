package com.example.musicvibrationapp.data

data class AppState(
    val isCapturing: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) 