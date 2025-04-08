package com.example.musicvibrationapp.manager

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.example.musicvibrationapp.R
import com.example.musicvibrationapp.view.AudioSpectrumView

class FloatingWindowManager private constructor(private val context: Context) {
    private var windowManager: WindowManager? = null
    private var topView: FrameLayout? = null
    private var bottomView: FrameLayout? = null
    private var leftView: FrameLayout? = null
    private var rightView: FrameLayout? = null
    private var audioSpectrumViewTop: AudioSpectrumView? = null
    private var audioSpectrumViewBottom: AudioSpectrumView? = null
    private var audioSpectrumViewLeft: AudioSpectrumView? = null
    private var audioSpectrumViewRight: AudioSpectrumView? = null
    private var isShowing = false
    
    // Get height from layout file
    private val spectrumHeight by lazy {
        val layoutId = R.layout.floating_window_layout
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(layoutId, null)
        val spectrumView = view.findViewById<AudioSpectrumView>(R.id.audioSpectrumViewTop)
        spectrumView.layoutParams.height
    }

    init {
        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WindowManager", e)
        }
    }

    fun show() {
        if (isShowing) return
        try {
            createFloatingWindows()
            isShowing = true
            Log.d(TAG, "Edge floating windows displayed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display floating windows", e)
        }
    }

    fun hide() {
        if (!isShowing) return
        try {
            removeFloatingWindows()
            isShowing = false
            Log.d(TAG, "Floating windows hidden successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide floating windows", e)
        }
    }

    fun updateSpectrum(fftData: FloatArray) {
        try {
            audioSpectrumViewTop?.updateSpectrum(fftData)
            audioSpectrumViewBottom?.updateSpectrum(fftData)
            audioSpectrumViewLeft?.updateSpectrum(fftData)
            audioSpectrumViewRight?.updateSpectrum(fftData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update spectrum", e)
        }
    }

    private fun createFloatingWindows() {
        if (topView != null || bottomView != null || leftView != null || rightView != null) return

        try {
            createTopWindow()
            createBottomWindow()
            createLeftWindow()
            createRightWindow()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create floating windows", e)
            removeFloatingWindows()
        }
    }

    private fun createTopWindow() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.floating_window_layout, null)
        val templateView = view.findViewById<AudioSpectrumView>(R.id.audioSpectrumViewTop)
        
        topView = FrameLayout(context).apply {
            // Re-create view from layout file
            audioSpectrumViewTop = inflater.inflate(
                R.layout.floating_window_layout, null
            ).findViewById(R.id.audioSpectrumViewTop)
            
            // Remove from parent view, then add to new container
            (audioSpectrumViewTop?.parent as? ViewGroup)?.removeView(audioSpectrumViewTop)
            addView(audioSpectrumViewTop, templateView.layoutParams)
        }

        val params = createWindowParams().apply {
            height = templateView.layoutParams.height
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }

        windowManager?.addView(topView, params)
    }

    private fun createBottomWindow() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.floating_window_layout, null)
        val templateView = view.findViewById<AudioSpectrumView>(R.id.audioSpectrumViewBottom)
        
        bottomView = FrameLayout(context).apply {
            // Re-create view from layout file
            audioSpectrumViewBottom = inflater.inflate(
                R.layout.floating_window_layout, null
            ).findViewById(R.id.audioSpectrumViewBottom)
            
            // Remove from parent view, then add to new container
            (audioSpectrumViewBottom?.parent as? ViewGroup)?.removeView(audioSpectrumViewBottom)
            addView(audioSpectrumViewBottom, templateView.layoutParams)
        }

        val params = createWindowParams().apply {
            height = templateView.layoutParams.height
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        windowManager?.addView(bottomView, params)
    }

    private fun createLeftWindow() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.floating_window_layout, null)
        val templateView = view.findViewById<AudioSpectrumView>(R.id.audioSpectrumViewLeft)
        
        leftView = FrameLayout(context).apply {
            audioSpectrumViewLeft = inflater.inflate(
                R.layout.floating_window_layout, null
            ).findViewById(R.id.audioSpectrumViewLeft)
            
            (audioSpectrumViewLeft?.parent as? ViewGroup)?.removeView(audioSpectrumViewLeft)
            addView(audioSpectrumViewLeft, templateView.layoutParams)
        }

        val params = createWindowParams().apply {
            width = templateView.layoutParams.width
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }

        windowManager?.addView(leftView, params)
    }

    private fun createRightWindow() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.floating_window_layout, null)
        val templateView = view.findViewById<AudioSpectrumView>(R.id.audioSpectrumViewRight)
        
        rightView = FrameLayout(context).apply {
            audioSpectrumViewRight = inflater.inflate(
                R.layout.floating_window_layout, null
            ).findViewById(R.id.audioSpectrumViewRight)
            
            (audioSpectrumViewRight?.parent as? ViewGroup)?.removeView(audioSpectrumViewRight)
            addView(audioSpectrumViewRight, templateView.layoutParams)
        }

        val params = createWindowParams().apply {
            width = templateView.layoutParams.width
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        windowManager?.addView(rightView, params)
    }

    private fun createWindowParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            
            type = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                }
                else -> WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }

            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

            format = PixelFormat.TRANSLUCENT

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = 
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun removeFloatingWindows() {
        try {
            topView?.let {
                windowManager?.removeView(it)
                topView = null
                audioSpectrumViewTop = null
            }
            bottomView?.let {
                windowManager?.removeView(it)
                bottomView = null
                audioSpectrumViewBottom = null
            }
            leftView?.let {
                windowManager?.removeView(it)
                leftView = null
                audioSpectrumViewLeft = null
            }
            rightView?.let {
                windowManager?.removeView(it)
                rightView = null
                audioSpectrumViewRight = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove floating windows", e)
        }
    }

    companion object {
        private const val TAG = "FloatingWindowManager"
        @Volatile
        private var instance: FloatingWindowManager? = null

        fun getInstance(context: Context): FloatingWindowManager {
            return instance ?: synchronized(this) {
                instance ?: FloatingWindowManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}