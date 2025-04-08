package com.example.musicvibrationapp.emotion.image

interface TextRecognitionCallback {
    fun onTextRecognized(text: String)
    fun onError(error: Exception)
} 