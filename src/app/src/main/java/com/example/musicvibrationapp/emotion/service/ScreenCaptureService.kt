package com.example.musicvibrationapp.emotion.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.musicvibrationapp.R
import com.example.musicvibrationapp.service.AudioCaptureManager
import com.example.musicvibrationapp.emotion.image.TextRecognitionCallback
import com.example.musicvibrationapp.emotion.TaskCompletionManager

@RequiresApi(Build.VERSION_CODES.Q)
class ScreenCaptureService(
    private val context: Context,
    private val audioCaptureManager: AudioCaptureManager,
    private val textRecognitionCallback: TextRecognitionCallback
) {
    private val TAG = "ScreenCaptureService"
    private var service: ScreenCaptureServiceForeground? = null
    private var bound = false
    
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "Service connected")
            service = (binder as ScreenCaptureServiceForeground.LocalBinder).getService()
            bound = true
            audioCaptureManager.getMediaProjection()?.let { mediaProjection ->
                service?.initializeProjection(mediaProjection)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            service = null
            bound = false
        }
    }

    private var currentTextView: TextView? = null
    private var currentHandler: Handler? = null
    private var windowManager: WindowManager? = null

    private val taskManager = TaskCompletionManager.getInstance()

    private val toastCallback = object : TextRecognitionCallback {
        override fun onTextRecognized(text: String) {
            Handler(Looper.getMainLooper()).post {
                try {
                    // Remove existing text view
                    removeCurrentTextView()

                    // Create new text view
                    val textView = TextView(context).apply {
                        setText(text)
                        setTextColor(Color.WHITE)
                        textSize = 20f
                        typeface = Typeface.DEFAULT_BOLD
                        setPadding(40, 30, 40, 30)
                        gravity = Gravity.START
                        setBackgroundColor(Color.argb(230, 0, 0, 0))
                        maxWidth = (context.resources.displayMetrics.widthPixels * 0.8).toInt()
                    }

                    // Set layout parameters
                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        y = 150 // Offset from the top
                    }

                    // Get or initialize WindowManager
                    if (windowManager == null) {
                        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    }

                    // Display text view
                    // windowManager?.addView(textView, params) //TODO: Temporarily disable text view
                    currentTextView = textView

                    // Set timer to remove text view
                    currentHandler = Handler(Looper.getMainLooper()).apply {
                        postDelayed({
                            removeCurrentTextView()
                        }, 3000)
                    }

                    // Notify task completion
                    taskManager.completeTask()
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing text overlay", e)
                }
            }
            textRecognitionCallback.onTextRecognized(text)
        }

        override fun onError(error: Exception) {
            textRecognitionCallback.onError(error)
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
            Log.e(TAG, "Error removing text overlay", e)
        }
    }

    init {
        startAndBindService()
    }

    private fun startAndBindService() {
        val intent = Intent(context, ScreenCaptureServiceForeground::class.java)
        try {
            context.startService(intent)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting/binding service", e)
        }
    }

    suspend fun captureScreen() {
        try {
            service?.captureScreen(toastCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screen", e)
            // Don't mark as complete on error, allow task to retry
        }
    }

    fun release() {
        try {
            removeCurrentTextView()
            windowManager = null
            if (bound) {
                context.unbindService(connection)
                bound = false
            }
            context.stopService(Intent(context, ScreenCaptureServiceForeground::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing service", e)
        }
    }
} 