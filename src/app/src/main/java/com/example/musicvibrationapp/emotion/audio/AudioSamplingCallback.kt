package com.example.musicvibrationapp.emotion.audio

interface AudioSamplingCallback {
    fun onSamplingComplete(features: List<AudioFeatures>, stats: AudioFeatureStats)
    fun onError(error: Exception)
    fun onCleanup()
}