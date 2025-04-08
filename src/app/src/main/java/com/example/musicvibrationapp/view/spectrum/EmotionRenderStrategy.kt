package com.example.musicvibrationapp.view.spectrum

import android.content.Context
import android.graphics.Canvas
import com.example.musicvibrationapp.view.DisplayMode
import com.example.musicvibrationapp.view.EmotionColorManager

/**
 * Emotion rendering strategy interface
 * Defines the rendering方式 for different emotions
 */
interface EmotionRenderStrategy {
    /**
     * Preprocess spectrum data
     * @param rawData original spectrum data
     * @return processed data
     */
    fun processData(rawData: FloatArray): FloatArray
    
    /**
     * Render spectrum
     * @param canvas canvas
     * @param data processed spectrum data
     * @param displayMode display mode
     * @param width view width
     * @param height view height
     * @param viewId view ID
     */
    fun render(canvas: Canvas, data: FloatArray, displayMode: DisplayMode, width: Int, height: Int, viewId: Int)
} 