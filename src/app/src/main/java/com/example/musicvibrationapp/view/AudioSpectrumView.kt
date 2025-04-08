package com.example.musicvibrationapp.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.example.musicvibrationapp.R
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.abs
import com.example.musicvibrationapp.view.spectrum.EmotionRenderFactory
import com.example.musicvibrationapp.view.spectrum.EmotionRenderStrategy

class AudioSpectrumView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.argb(180, 0, 255, 255)  // increase transparency  
        style = Paint.Style.FILL
        setShadowLayer(8f, 0f, 0f, Color.BLUE)  // more obvious shadow
    }

    private var spectrumData: FloatArray = FloatArray(64) { 0f }
    private var barWidth = 0f
    private var spacing = 2f  // decrease spacing
    private var isReverse = false

    private val smoothingFactor = 0.28f // control the speed of change of spectrum, the smaller the value, the slower the change
    private val minThreshold = 0.05f
    private val previousData = FloatArray(64) { 0f }
    private var currentVolume = 0f
    private val volumeSmoothingFactor = 0.1f  // control the smoothness of volume change
    private var previousVolume = 0f
    private var dominantFreqIndex = -1
    private var energyConcentration = 0f

    // add display mode control: TOP_BOTTOM, SIDES, BOTH
    private var displayMode = SpectrumParameters.displayMode.value // get current value directly

    // gaussian distribution weight array
    private val gaussianWeights by lazy {
        FloatArray(spectrumData.size) { i ->
            val x = (i.toFloat() / spectrumData.size) * 2 - 1  // map to [-1,1]
            val sigma = 0.3f  // decrease sigma to make distribution more concentrated
            val weight = exp(-(x * x) / (2 * sigma * sigma))
            // adjust weight range, ensure edge not too low
            0.4f + (weight * 0.6f)  // lowest not less than 0.4, highest is 1.0
        }
    }

    // add for tracking current display height of each spectrum bar
    private var currentBarHeights = FloatArray(64) { 0f }
    // fall speed (the ratio of decrease each time)
    private val fallSpeed = 0.08f
    // minimum fall speed (ensure even the smallest value will fall slowly)
    private val minFallSpeed = 0.02f // minimum fall speed

    // add constant definition
    companion object {
        private const val TOP_BARS_COUNT = 5  // consider the highest bars number
        private const val MAX_HEIGHT_RATIO = 1.35f  // maximum bar allowed to exceed the reference height
        private const val MIN_ADJUSTED_RATIO = 1.2f  // adjusted minimum ratio
        private const val ADJUST_RANGE = 0.07f  // adjusted range
        
        // mel frequency response sensitivity (0.2-1.0)
        // the smaller the value, the wider the frequency response curve, the more moderate the effect on human voice
        private const val MEL_SENSITIVITY = 0.3f
        
        // solo state response strength (0.1-0.3)
        // the smaller the value, the more moderate the effect of acceleration/deceleration in solo state
        private const val SOLO_RESPONSE_STRENGTH = 0.15f

        // top bottom mode spectrum bars number
        private const val TOP_BOTTOM_BARS_COUNT = 40
        
        // side mode spectrum bars number
        private const val SIDE_BARS_COUNT = 40
    }

    private var currentRenderer: EmotionRenderStrategy? = null
    private var lastEmotionType: EmotionColorManager.EmotionType? = null

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AudioSpectrumView,
            0, 0
        ).apply {
            try {
                isReverse = getBoolean(R.styleable.AudioSpectrumView_reverseDirection, false)
                // if reverse, directly rotate 180 degrees
                if (isReverse) {
                    rotation = 180f
                }
            } finally {
                recycle()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // adjust calculation logic: total width = (number of bars * bar width) + (number of bars - 1) * spacing
        barWidth = (w - (spectrumData.size - 1) * spacing) / spectrumData.size
    }

    private fun calculateVolume(data: FloatArray): Float {
        // same as using only the first half of the data to calculate volume
        var sum = 0f
        val halfSize = data.size / 2
        for (i in 1 until halfSize) {
            sum += data[i] * data[i]
        }
        return kotlin.math.sqrt(sum / (halfSize - 1))
    }

    private fun calculateMagnitudeSpectrum(fftData: FloatArray): FloatArray {
        val size = fftData.size / 2
        val sampleRate = 44100f
        
        // calculate the index corresponding to the original frequency range
        val minFreq = 20f  // minimum frequency 20Hz
        val maxFreq = 12000f  // maximum frequency 
        val minIndex = (minFreq * size / (sampleRate / 2)).toInt().coerceAtLeast(1)
        val maxIndex = (maxFreq * size / (sampleRate / 2)).toInt().coerceAtMost(size - 1)
        
        // select the output array size based on the current display mode
        val outputSize = when (displayMode) {
            DisplayMode.TOP_BOTTOM -> TOP_BOTTOM_BARS_COUNT
            DisplayMode.SIDES -> SIDE_BARS_COUNT
            else -> TOP_BOTTOM_BARS_COUNT  // default use the number of top bottom mode
        }
        
        return FloatArray(outputSize) { i ->
            // map the output index back to the original frequency range
            val position = i.toFloat() / (outputSize - 1)
            val originalIndex = (minIndex + (maxIndex - minIndex) * position).toInt()
            
            // use linear interpolation to get the value between two adjacent points
            val index1 = originalIndex.coerceIn(minIndex, maxIndex - 1)
            val index2 = (index1 + 1).coerceIn(minIndex, maxIndex)
            val fraction = (originalIndex - index1).toFloat()
            
            // calculate the magnitude of two adjacent points
            val mag1 = kotlin.math.sqrt(
                fftData[2 * index1] * fftData[2 * index1] + 
                fftData[2 * index1 + 1] * fftData[2 * index1 + 1]
            )
            val mag2 = kotlin.math.sqrt(
                fftData[2 * index2] * fftData[2 * index2] + 
                fftData[2 * index2 + 1] * fftData[2 * index2 + 1]
            )
            
            // linear interpolation
            mag1 + (mag2 - mag1) * fraction
        }
    }

    // calculate the frequency weight of the mel scale
    private fun getMelWeight(freq: Float): Float {
        val mel = 2595 * log10(1 + freq / 700)
        // use the current melSensitivity parameter
        return (1 + SpectrumParameters.melSensitivity.value * mel / 500f)
    }

    private fun analyzeDominantFrequency(data: FloatArray): Pair<Int, Float> {
        var maxEnergy = 0f
        var maxIndex = 0
        var totalEnergy = 0f
        
        // use weighted energy calculation
        for (i in 1 until data.size) {
            val freq = i * (44100f / (data.size * 2)) // assume the sampling rate is 44.1kHz
            // TODO: determine if mel weight is needed
            val weight = getMelWeight(freq)  // mel weight
            // val weight = 1f
            val energy = data[i] * data[i] * weight
            totalEnergy += energy

            if (energy > maxEnergy) {
                maxEnergy = energy
                maxIndex = i
            }
        }

        // calculate the weighted energy concentration
        val range = 3 // consider the range around the main frequency
        var centralEnergy = 0f
        for (i in maxIndex - range..maxIndex + range) {
            if (i in 1 until data.size) {
                val freq = i * (44100f / (data.size * 2))
                val weight = getMelWeight(freq)
                centralEnergy += data[i] * data[i] * weight
            }
        }
        
        val concentration = if (totalEnergy > 0) centralEnergy / totalEnergy else 0f
        return Pair(maxIndex, concentration)
    }

    private fun calculateSensitivity(index: Int, dominantIndex: Int, concentration: Float, size: Int): Float {
        val distance = abs(index - size/2)  // calculate the distance from the center
        val maxDistance = size/2f
        
        // use a smoother attenuation curve
        val distanceRatio = distance / maxDistance
        val baseSensitivity = exp(-distanceRatio * 1.2f)  // a slower exponential decay
        
        // adjust based on the energy concentration
        return if (concentration > 0.7f) {
            // when high energy concentration, use a more uniform response
            0.4f + 0.6f * baseSensitivity
        } else {
            // when energy spread, allow a larger range of change
            0.3f + 0.7f * baseSensitivity
        }
    }

    private fun reorderSpectrum(data: FloatArray): FloatArray {
        val result = FloatArray(data.size)
        val center = data.size / 2
        
        // calculate the spectrum energy
        val energyProfile = FloatArray(data.size) { i ->
            if (i == 0) 0f else {
                val freq = i * (44100f / (data.size * 2))
                val melWeight = getMelWeight(freq)
                data[i] * data[i] * melWeight
            }
        }
        
        // find the most significant frequency and put it in the center
        val maxEnergyIndex = energyProfile.indices
            .filter { it > 0 }
            .maxByOrNull { energyProfile[it] } ?: 1
        result[center] = data[maxEnergyIndex]
        
        // create two sorted sequences, used for left and right sides
        val sortedIndices = energyProfile.indices
            .filter { it > 0 && it != maxEnergyIndex }
            .sortedByDescending { energyProfile[it] }
        
        // assign the remaining frequencies to the left and right sides, ensuring energy decreases gradually
        var leftIndex = center - 1
        var rightIndex = center + 1
        var currentIndicesIndex = 0
        
        while (leftIndex >= 0 || rightIndex < data.size) {
            if (currentIndicesIndex < sortedIndices.size) {
                val srcIndex = sortedIndices[currentIndicesIndex]
                
                // alternate fill the left and right sides
                if (leftIndex >= 0 && (rightIndex >= data.size || currentIndicesIndex % 2 == 0)) {
                    result[leftIndex] = data[srcIndex]
                    leftIndex--
                } else if (rightIndex < data.size) {
                    result[rightIndex] = data[srcIndex]
                    rightIndex++
                }
                
                currentIndicesIndex++
            } else {
                // if there are still unfilled positions, fill with 0
                if (leftIndex >= 0) {
                    result[leftIndex] = 0f
                    leftIndex--
                }
                if (rightIndex < data.size) {
                    result[rightIndex] = 0f
                    rightIndex++
                }
            }
        }
        
        return result
    }

    private fun preprocessData(data: FloatArray): FloatArray {
        val (dominantIndex, concentration) = analyzeDominantFrequency(data)
        dominantFreqIndex = dominantIndex
        energyConcentration = concentration

        return FloatArray(data.size) { i ->
            val value = data[i]
            if (value > 0) {
                // use the current minThreshold parameter
                val minThreshold = SpectrumParameters.minThreshold.value
                val logValue = log10(1 + value * 20)
                val noise = (Math.random() * 0.1 - 0.05).toFloat()
                val sensitivity = calculateSensitivity(i, dominantIndex, concentration, data.size)
                max((logValue + noise) * sensitivity, minThreshold)
            } else {
                SpectrumParameters.minThreshold.value
            }
        }
    }

    private fun applyVolumeEffect(data: FloatArray): FloatArray {
        val targetVolume = calculateVolume(data)
        currentVolume = previousVolume + (targetVolume - previousVolume) * 0.5f
        
        val volumeChange = (currentVolume - previousVolume)
        previousVolume = currentVolume

        // more sensitive rhythm detection
        val isRhythmPeak = volumeChange > 0.08f

        return FloatArray(data.size) { i ->
            val baseValue = data[i]
            
            // basic volume effect
            val volumeEffect = 0.3f + currentVolume * 0.7f
            
            // stronger rhythm response
            val rhythmFactor = if (isRhythmPeak) {
                1.0f + volumeChange * 5f
            } else {
                1.0f + volumeChange.coerceIn(-0.2f, 0.2f)
            }
            
            baseValue * volumeEffect * rhythmFactor
        }
    }

    private fun smoothData(data: FloatArray): FloatArray {
        return FloatArray(data.size) { i ->
            // use the current smoothingFactor and soloResponseStrength parameter
            val smoothingFactor = SpectrumParameters.smoothingFactor.value
            val soloResponseStrength = SpectrumParameters.soloResponseStrength.value
            
            val dynamicSmoothingFactor = if (energyConcentration > 0.8f) {
                if (abs(i - dominantFreqIndex) <= 3) {
                    smoothingFactor * (1f + soloResponseStrength)
                } else {
                    smoothingFactor * (1f - soloResponseStrength)
                }
            } else {
                smoothingFactor
            }
            
            val smoothed = previousData[i] + (data[i] - previousData[i]) * dynamicSmoothingFactor
            previousData[i] = smoothed
            smoothed
        }
    }

    private fun normalizeData(data: FloatArray): FloatArray {
        val maxValue = data.maxOrNull() ?: 1f
        if (maxValue < 0.05f) {
            return FloatArray(data.size) { i ->
                val currentHeight = currentBarHeights[i]
                // use the current fallSpeed and minFallSpeed parameter
                val fallSpeed = SpectrumParameters.fallSpeed.value
                val minFallSpeed = SpectrumParameters.minFallSpeed.value
                val fallAmount = max(currentHeight * fallSpeed, minFallSpeed)
                currentBarHeights[i] = max(0f, currentHeight - fallAmount)
                currentBarHeights[i]
            }
        }
        
        // use indexOfFirst to find the index of the maximum value
        val maxIndex = data.indexOfFirst { it == maxValue }
        
        return FloatArray(data.size) { i ->
            if (maxValue > 0) {
                // determine the actual data index to display at the current position
                val sourceIndex = if (i >= maxIndex) i + 1 else i
                // if out of array range, return 0
                val value = if (sourceIndex < data.size) data[sourceIndex] else 0f
                
                val normalizedValue = value / maxValue
                
                // use a single power function for nonlinear mapping
                val power = 0.8f
                val enhancedValue = normalizedValue.pow(power)
                
                // keep a small random change
                val randomFactor = 1f + (Math.random() * 0.03f - 0.015f).toFloat()
                currentBarHeights[i] = enhancedValue * randomFactor
            } else {
                // use the current fallSpeed and minFallSpeed parameter
                val fallSpeed = SpectrumParameters.fallSpeed.value
                val minFallSpeed = SpectrumParameters.minFallSpeed.value
                val fallAmount = max(currentBarHeights[i] * fallSpeed, minFallSpeed)
                currentBarHeights[i] = max(0f, currentBarHeights[i] - fallAmount)
            }
            currentBarHeights[i]
        }.also { normalizedData ->
            val sortedHeights = normalizedData.sortedDescending()
            if (sortedHeights.size >= TOP_BARS_COUNT) {
                val highest = sortedHeights[0]
                
                // calculate the weighted average of the top N bars as a reference (start from index 1, skip the highest value)
                val weightedSum = (1 until TOP_BARS_COUNT).sumOf { 
                    sortedHeights[it].toDouble() * (TOP_BARS_COUNT - it)
                }
                val weightSum = (1 until TOP_BARS_COUNT).sum()
                val referenceHeight = (weightedSum / weightSum).toFloat()
                
                if (highest > referenceHeight * MAX_HEIGHT_RATIO) {
                    val adjustedHeight = referenceHeight * (MIN_ADJUSTED_RATIO + Math.random() * ADJUST_RANGE).toFloat()
                    
                    // find the index of the highest bar (use approximate matching)
                    val maxIndex = normalizedData.indices.firstOrNull { 
                        abs(normalizedData[it] - highest) < 0.0001f 
                    }
                    if (maxIndex != null) {
                        currentBarHeights[maxIndex] = adjustedHeight
                        normalizedData[maxIndex] = adjustedHeight
                    }
                }
            }
        }
    }

    fun updateSpectrum(newSpectrum: FloatArray) {
        // get the current emotion
        val currentEmotion = EmotionColorManager.getInstance().currentEmotion.value
        
        // check if the emotion has changed, if so, create a new renderer
        if (currentEmotion != lastEmotionType) {
            currentRenderer = EmotionRenderFactory.createStrategy(currentEmotion, context)
            lastEmotionType = currentEmotion
        }
        
        // calculate the magnitude spectrum
        val magnitudeSpectrum = calculateMagnitudeSpectrum(newSpectrum)
        
        // reorder the spectrum
        val reorderedSpectrum = reorderSpectrum(magnitudeSpectrum)
        
        // post-processing
        val preprocessed = preprocessData(reorderedSpectrum)
        val volumeAdjusted = applyVolumeEffect(preprocessed)
        val smoothed = smoothData(volumeAdjusted)
        spectrumData = normalizeData(smoothed)
        
        // use the renderer to process the data
        currentRenderer?.processData(spectrumData)
        
        invalidate()
    }

    fun setDisplayMode(mode: DisplayMode) {
        displayMode = mode
        SpectrumParameters.updateDisplayMode(mode) // update SpectrumParameters
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // get the latest displayMode in each drawing
        displayMode = SpectrumParameters.displayMode.value
        
        // use the renderer to draw
        currentRenderer?.render(canvas, spectrumData, displayMode, width, height, id) ?: run {
            // if there is no renderer, use the default drawing method
            when (displayMode) {
                DisplayMode.TOP_BOTTOM -> {
                    if (id == R.id.audioSpectrumViewTop || id == R.id.audioSpectrumViewBottom) {
                        drawTopBottomSpectrum(canvas)
                    }
                }
                DisplayMode.SIDES -> {
                    if (id == R.id.audioSpectrumViewLeft || id == R.id.audioSpectrumViewRight) {
                        drawSideSpectrum(canvas)
                    }
                }
            }
        }
    }

    private fun drawTopBottomSpectrum(canvas: Canvas) {
        val maxHeight = height.toFloat()
        val totalWidth = width.toFloat()
        val barSpacing = spacing * (spectrumData.size - 1)
        val barWidth = (totalWidth - barSpacing) / spectrumData.size

        spectrumData.forEachIndexed { index, magnitude ->
            val barHeight = magnitude * maxHeight
            val x = index * (barWidth + spacing)

            if (barHeight > 0) {
                // create a linear gradient
                val gradient = LinearGradient(
                    x, maxHeight,  // start point (bottom)
                    x, maxHeight - barHeight,  // end point (top)
                    EmotionColorManager.getInstance().currentEmotion.value.getGradientColors(barHeight, maxHeight),
                    null,  // use uniform distribution
                    Shader.TileMode.CLAMP
                )

                // apply the gradient
                paint.shader = gradient
                
                // draw a rectangle with a gradient
                canvas.drawRect(
                    x,
                    maxHeight - barHeight,
                    x + barWidth,
                    maxHeight,
                    paint
                )
                
                // clear the gradient to avoid affecting other drawings
                paint.shader = null
            }
        }
    }

    private fun drawSideSpectrum(canvas: Canvas) {
        val maxWidth = width.toFloat()
        val totalHeight = height.toFloat()
        val barSpacing = spacing * (spectrumData.size - 1)
        val barHeight = (totalHeight - barSpacing) / spectrumData.size

        spectrumData.forEachIndexed { index, magnitude ->
            val barWidth = magnitude * maxWidth
            val y = when (id) {
                R.id.audioSpectrumViewLeft -> index * (barHeight + spacing)
                R.id.audioSpectrumViewRight -> totalHeight - (index + 1) * (barHeight + spacing)
                else -> 0f
            }

            if (barWidth > 0) {
                // create a horizontal linear gradient
                val gradient = LinearGradient(
                    0f, y,  // start point (left)
                    barWidth, y,  // end point (right)
                    EmotionColorManager.getInstance().currentEmotion.value.getGradientColors(barWidth, maxWidth),
                    null,  // use uniform distribution
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

    private fun getColorByIndex(index: Int): Int {
        return EmotionColorManager.getInstance().currentEmotion.value
            .getColorScheme(index, spectrumData.size)
    }
}