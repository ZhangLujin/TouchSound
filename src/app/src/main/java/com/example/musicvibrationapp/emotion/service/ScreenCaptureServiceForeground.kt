package com.example.musicvibrationapp.emotion.service

import android.app.Service
import android.content.Intent
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.musicvibrationapp.emotion.image.ImagePreprocessor
import com.example.musicvibrationapp.service.MyNotificationManager
import com.example.musicvibrationapp.emotion.image.TextRecognitionCallback

@RequiresApi(Build.VERSION_CODES.Q)
class ScreenCaptureServiceForeground : Service() {
    private val TAG = "ScreenCaptureService"
    private var imageReader: ImageReader? = null
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null
    private var isCapturing = false
    private val binder = LocalBinder()
    private lateinit var notificationManager: MyNotificationManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val imagePreprocessor = ImagePreprocessor(this)
    
    // Add retry-related configuration
    companion object {
        private const val MAX_CAPTURE_ATTEMPTS = 3
        private const val CAPTURE_RETRY_DELAY = 1000L // 1 second
    }

    inner class LocalBinder : Binder() {
        fun getService(): ScreenCaptureServiceForeground = this@ScreenCaptureServiceForeground
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = MyNotificationManager(this)
        notificationManager.createNotificationChannel()
        startForeground(MyNotificationManager.NOTIFICATION_ID, notificationManager.createNotification())
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun initializeProjection(mediaProjection: MediaProjection) {
        this.mediaProjection = mediaProjection
        mediaProjection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.d(TAG, "MediaProjection stopped")
                cleanupResources()
            }
        }, mainHandler)
    }

    private fun saveImage(image: android.media.Image) {
        val width = image.width
        val height = image.height
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)

        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Screenshot_$timeStamp.png"
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val file = File(directory, fileName)

            // Commented out code for saving images
            // FileOutputStream(file).use { out ->
            //     bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            // }
            Log.d(TAG, "Screenshot saved: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving screenshot", e)
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun captureScreen(callback: TextRecognitionCallback) {
        if (isCapturing) {
            Log.d(TAG, "Capture already in progress")
            return
        }
        
        try {
            isCapturing = true
            var captureAttempt = 0
            var success = false
            
            while (captureAttempt < MAX_CAPTURE_ATTEMPTS && !success) {
                try {
                    withContext(Dispatchers.Main) {
                        if (virtualDisplay == null) {
                            setupVirtualDisplay()
                        }
                    }
                    
                    delay(100) // Wait for display initialization
                    
                    withContext(Dispatchers.Default) {
                        val image = imageReader?.acquireLatestImage()
                        if (image != null) {
                            imagePreprocessor.process(image, object : TextRecognitionCallback {
                                override fun onTextRecognized(text: String) {
                                    success = true
                                    callback.onTextRecognized(text)
                                }

                                override fun onError(error: Exception) {
                                    if (captureAttempt >= MAX_CAPTURE_ATTEMPTS - 1) {
                                        callback.onError(error)
                                    }
                                    // If not the last attempt, continue retrying
                                }
                            })
                            image.close()
                        }
                    }
                    
                    if (!success) {
                        captureAttempt++
                        if (captureAttempt < MAX_CAPTURE_ATTEMPTS) {
                            delay(CAPTURE_RETRY_DELAY)
                            Log.d(TAG, "Retrying capture, attempt ${captureAttempt + 1}")
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error during capture attempt $captureAttempt", e)
                    captureAttempt++
                    if (captureAttempt >= MAX_CAPTURE_ATTEMPTS) {
                        callback.onError(e)
                    } else {
                        delay(CAPTURE_RETRY_DELAY)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screen", e)
            callback.onError(e)
        } finally {
            isCapturing = false
        }
    }

    private fun setupVirtualDisplay() {
        Log.d(TAG, "Setting up virtual display")
        val metrics = resources.displayMetrics
        imageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            android.graphics.PixelFormat.RGBA_8888,
            2
        )

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            Surface.ROTATION_0,
            imageReader?.surface,
            null,
            null
        )
        Log.d(TAG, "Virtual display setup completed")
    }

    private fun cleanupResources() {
        Log.d(TAG, "Cleaning up resources")
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupResources()
    }
} 