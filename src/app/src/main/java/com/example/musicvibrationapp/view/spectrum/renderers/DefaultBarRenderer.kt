package com.example.musicvibrationapp.view.spectrum.renderers

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.example.musicvibrationapp.R
import com.example.musicvibrationapp.view.DisplayMode
import com.example.musicvibrationapp.view.EmotionColorManager
import com.example.musicvibrationapp.view.spectrum.EmotionRenderStrategy

class DefaultBarRenderer(private val context: Context) : EmotionRenderStrategy {
    private val paint = Paint().apply {
        style = Paint.Style.FILL
        setShadowLayer(8f, 0f, 0f, android.graphics.Color.BLUE)
    }
    
    private val spacing = 2f
    
    override fun processData(rawData: FloatArray): FloatArray {
        return rawData
    }
    
    override fun render(canvas: Canvas, data: FloatArray, displayMode: DisplayMode, width: Int, height: Int, viewId: Int) {
        when (displayMode) {
            DisplayMode.TOP_BOTTOM -> {
                if (viewId == R.id.audioSpectrumViewTop || viewId == R.id.audioSpectrumViewBottom) {
                    drawTopBottomSpectrum(canvas, data, width, height)
                }
            }
            DisplayMode.SIDES -> {
                if (viewId == R.id.audioSpectrumViewLeft || viewId == R.id.audioSpectrumViewRight) {
                    drawSideSpectrum(canvas, data, width, height, viewId)
                }
            }
        }
    }
    
    private fun drawTopBottomSpectrum(canvas: Canvas, data: FloatArray, width: Int, height: Int) {
        val maxHeight = height.toFloat()
        val totalWidth = width.toFloat()
        val barSpacing = spacing * (data.size - 1)
        val barWidth = (totalWidth - barSpacing) / data.size
        
        data.forEachIndexed { index, magnitude ->
            val barHeight = magnitude * maxHeight
            val x = index * (barWidth + spacing)
            
            if (barHeight > 0) {
                val gradient = LinearGradient(
                    x, maxHeight,
                    x, maxHeight - barHeight,
                    EmotionColorManager.getInstance().currentEmotion.value.getGradientColors(barHeight, maxHeight),
                    null,
                    Shader.TileMode.CLAMP
                )
                
                paint.shader = gradient
                canvas.drawRect(
                    x,
                    maxHeight - barHeight,
                    x + barWidth,
                    maxHeight,
                    paint
                )
                paint.shader = null
            }
        }
    }
    
    private fun drawSideSpectrum(canvas: Canvas, data: FloatArray, width: Int, height: Int, viewId: Int) {
        val maxWidth = width.toFloat()
        val totalHeight = height.toFloat()
        val barSpacing = spacing * (data.size - 1)
        val barHeight = (totalHeight - barSpacing) / data.size
        
        data.forEachIndexed { index, magnitude ->
            val barWidth = magnitude * maxWidth
            val y = when (viewId) {
                R.id.audioSpectrumViewLeft -> index * (barHeight + spacing)
                R.id.audioSpectrumViewRight -> totalHeight - (index + 1) * (barHeight + spacing)
                else -> 0f
            }
            
            if (barWidth > 0) {
                val gradient = LinearGradient(
                    0f, y,
                    barWidth, y,
                    EmotionColorManager.getInstance().currentEmotion.value.getGradientColors(barWidth, maxWidth),
                    null,
                    Shader.TileMode.CLAMP
                )
                
                paint.shader = gradient
                canvas.drawRect(
                    0f,
                    y,
                    barWidth,
                    y + barHeight,
                    paint
                )
                paint.shader = null
            }
        }
    }
} 