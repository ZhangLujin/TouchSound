package com.example.musicvibrationapp.emotion.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.Image
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.ImageView
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ImagePreprocessor(private val context: Context) {
    companion object {
        private const val TAG = "ImagePreprocessor"
        const val BOTTOM_CROP_PERCENTAGE = 35
        private const val MAX_RETRY_COUNT = 5  // Maximum retry count
        private const val RECOGNITION_TIMEOUT = 10000L  // Recognition timeout (milliseconds)
    }

    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    private val recognitionScope = CoroutineScope(Dispatchers.Default + Job())

    init {
        Log.d(TAG, "Initializing Chinese text recognizer")
    }

    fun process(image: Image, callback: TextRecognitionCallback) {
        try {
            Log.d(TAG, "Starting local image processing")
            
            // Convert to Bitmap
            val bitmap = imageToBitmap(image)
            Log.d(TAG, "Original bitmap created: ${bitmap.width}x${bitmap.height}")
            
            try {
                // Crop bottom area
                val startY = (bitmap.height * (100 - BOTTOM_CROP_PERCENTAGE) / 100)
                val cropHeight = (bitmap.height * BOTTOM_CROP_PERCENTAGE / 100)
                val croppedBitmap = Bitmap.createBitmap(
                    bitmap,
                    0, startY,
                    bitmap.width, cropHeight
                )
                Log.d(TAG, "Cropped bitmap created: ${croppedBitmap.width}x$cropHeight, starting from y=$startY")

                // Start text recognition task with timeout
                recognitionScope.launch {
                    var retryCount = 0
                    var lastError: Exception? = null
                    
                    while (retryCount < MAX_RETRY_COUNT) {
                        try {
                            val result = withTimeout(RECOGNITION_TIMEOUT) {
                                suspendCancellableCoroutine { continuation ->
                                    val inputImage = InputImage.fromBitmap(croppedBitmap, 0)
                                    recognizer.process(inputImage)
                                        .addOnSuccessListener { visionText ->
                                            if (!continuation.isCompleted) {
                                                if (visionText.text.isNotEmpty()) {
                                                    continuation.resume(visionText)
                                                } else {
                                                    continuation.resumeWithException(
                                                        Exception("No text detected")
                                                    )
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            if (!continuation.isCompleted) {
                                                continuation.resumeWithException(e)
                                            }
                                        }
                                }
                            }
                            
                            // If recognition is successful, call callback and exit loop
                            withContext(Dispatchers.Main) {
                                callback.onTextRecognized(result.text)
                            }
                            break
                            
                        } catch (e: Exception) {
                            lastError = e
                            retryCount++
                            if (retryCount >= MAX_RETRY_COUNT) {
                                withContext(Dispatchers.Main) {
                                    callback.onError(lastError ?: Exception("Recognition failed after $MAX_RETRY_COUNT attempts"))
                                }
                            } else {
                                delay(500) // Brief delay before retry
                            }
                        }
                    }
                }

                // Commented out code block for displaying image
                // Handler(Looper.getMainLooper()).post {
                //     val params = WindowManager.LayoutParams(
                //         WindowManager.LayoutParams.MATCH_PARENT,
                //         WindowManager.LayoutParams.WRAP_CONTENT,
                //         WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                //         WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                //         WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                //         PixelFormat.TRANSLUCENT
                //     )
                //
                //     val imageView = ImageView(context).apply {
                //         setImageBitmap(croppedBitmap)
                //         layoutParams = params
                //     }
                //
                //     val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                //     windowManager.addView(imageView, imageView.layoutParams)
                //
                //     Handler(Looper.getMainLooper()).postDelayed({
                //         try {
                //             windowManager.removeView(imageView)
                //             croppedBitmap.recycle()
                //             bitmap.recycle()
                //         } catch (e: Exception) {
                //             Log.e(TAG, "Error cleaning up resources", e)
                //         }
                //     }, 3000)
                // }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing image", e)
                bitmap.recycle()
                callback.onError(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
            callback.onError(e)
        }
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val width = image.width
        val height = image.height
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        // Create byte array to store pixel data
        val pixels = ByteArray(width * height * 4)
        var offset = 0
        val rowBytes = ByteArray(rowStride)
        
        // Correctly process image data
        for (row in 0 until height) {
            buffer.position(row * rowStride)
            buffer.get(rowBytes, 0, rowStride.coerceAtMost(buffer.remaining()))
            for (col in 0 until width) {
                val pixelValue = rowBytes[col * pixelStride]
                pixels[offset++] = pixelValue // R
                pixels[offset++] = pixelValue // G
                pixels[offset++] = pixelValue // B
                pixels[offset++] = 0xFF.toByte() // A
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(pixels))
        return bitmap
    }
}