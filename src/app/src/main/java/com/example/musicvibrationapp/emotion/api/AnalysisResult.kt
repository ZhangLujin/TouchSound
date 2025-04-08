package com.example.musicvibrationapp.emotion.api

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import android.util.Log
import org.json.JSONObject
import com.example.musicvibrationapp.emotion.audio.AudioFeatureStats
import com.example.musicvibrationapp.emotion.audio.AudioFeatures
import com.example.musicvibrationapp.view.EmotionColorManager

data class AnalysisResult(
    val audioStats: AudioFeatureStats?,
    val audioFeatures: List<AudioFeatures>?,
    val recognizedText: String?,
    val timestamp: Long = System.currentTimeMillis()
) {
    private val TAG = "AnalysisResult"
    private var currentTextView: TextView? = null
    private var currentHandler: Handler? = null
    private var windowManager: WindowManager? = null

    fun isComplete(): Boolean = audioStats != null && audioFeatures != null && recognizedText != null
    
    fun showResult(context: Context, jsonResponse: String) {
        val content = try {
            val jsonObject = JSONObject(jsonResponse)
            val choices = jsonObject.getJSONArray("choices")
            if (choices.length() > 0) {
                choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            } else {
                "Unable to parse response content"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing LLM response", e)
            jsonResponse 
        }

        EmotionColorManager.getInstance().updateEmotion(content)

        Handler(Looper.getMainLooper()).post {
            try {
                
                removeCurrentTextView()

                val textView = TextView(context).apply {
                    text = content
                    setTextColor(Color.WHITE)
                    textSize = 20f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(40, 30, 40, 30)
                    gravity = Gravity.START
                    setBackgroundColor(Color.argb(230, 0, 0, 0))
                    maxWidth = (context.resources.displayMetrics.widthPixels * 0.8).toInt()
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = 150 
                }

                if (windowManager == null) {
                    windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                }

                // windowManager?.addView(textView, params)
                currentTextView = textView

                currentHandler = Handler(Looper.getMainLooper()).apply {
                    postDelayed({
                        removeCurrentTextView()
                    }, 5000) 
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error showing result overlay", e)
            }
        }
    }

    private fun removeCurrentTextView() {
        try {
            currentTextView?.let { textView ->
                windowManager?.removeView(textView)
                currentTextView = null
            }
            currentHandler?.removeCallbacksAndMessages(null)
            currentHandler = null
        } catch (e: Exception) {
            Log.e(TAG, "Error removing result overlay", e)
        }
    }

    override fun toString(): String {
        return buildString {
            append("Analysis Result at ${timestamp}ms\n")
            append("Audio Stats: ${audioStats?.toString() ?: "Not available"}\n")
            append("Samples: ${audioFeatures?.size ?: 0}\n")
            append("Recognized Text: ${recognizedText ?: "Not available"}")
        }
    }
} 