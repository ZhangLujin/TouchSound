package com.example.musicvibrationapp.ui.components

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.*
import com.example.musicvibrationapp.R
import androidx.core.content.ContextCompat
import android.view.animation.LinearInterpolator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.example.musicvibrationapp.emotion.api.LLMService

class FloatingControlButtonManager(private val context: Context) {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var screenWidth = 0
    private var screenHeight = 0
    private var buttonWidth = 0
    private var buttonHeight = 0
    private val EDGE_THRESHOLD = 32 // Edge snapping threshold, in pixels
    private val COLLAPSED_WIDTH = 25 // Increased visible width in collapsed state
    private val TOUCH_EXPANSION = 35  // Pixel value to expand touch response area
    private var onCaptureListener: (() -> Unit)? = null
    private var lastCaptureTime = 0L
    private val MIN_CAPTURE_INTERVAL = 800L // Debounce interval
    
    // Added click detection parameters
    private val MAX_CLICK_DURATION = 500L    // Maximum click duration (milliseconds)
    private val MAX_CLICK_DISTANCE = 25      // Maximum click displacement (pixels)

    private var isProcessing = false
    private val deepSeekService = LLMService.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        // Get screen dimensions
        val displayMetrics = context.resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }

    fun show() {
        if (floatingView != null) return

        val inflater = LayoutInflater.from(context)
        floatingView = inflater.inflate(R.layout.floating_control_button, null)

        // Get button dimensions
        floatingView?.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        buttonWidth = floatingView?.measuredWidth ?: 0
        buttonHeight = floatingView?.measuredHeight ?: 0

        params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
        }

        setupDragListener()
        windowManager?.addView(floatingView, params)
    }

    private fun setupDragListener() {
        var isCollapsed = false
        var lastAction = 0
        var currentEdge: Edge = Edge.NONE
        var startedFromEdge = false
        var initialTouchTime = 0L
        
        floatingView?.setOnTouchListener { view, event ->
            val touchX = event.x
            val isInTouchArea = if (isCollapsed) {
                when (currentEdge) {
                    Edge.LEFT -> touchX <= (buttonWidth + TOUCH_EXPANSION)
                    Edge.RIGHT -> touchX >= -TOUCH_EXPANSION
                    Edge.NONE -> true
                }
            } else true

            if (!isInTouchArea) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchTime = System.currentTimeMillis()
                    // Record initial state
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    lastAction = event.action
                    startedFromEdge = isCollapsed
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // Check if there is enough movement intent (prevent slight jitter)
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    val hasMovementIntent = Math.abs(deltaX) > 3 || Math.abs(deltaY) > 3

                    params?.let { p ->
                        p.x = initialX + (event.rawX - initialTouchX).toInt()
                        p.y = initialY + (event.rawY - initialTouchY).toInt()

                        // If movement intent is detected in snapped state, expand immediately
                        if (isCollapsed && hasMovementIntent) {
                            // Determine expansion position based on current edge
                            p.x = when (currentEdge) {
                                Edge.LEFT -> 0
                                Edge.RIGHT -> screenWidth - buttonWidth
                                Edge.NONE -> p.x
                            }
                            expandFromEdge()
                            isCollapsed = false
                            currentEdge = Edge.NONE
                            // Update initial position to ensure smooth dragging afterwards
                            initialX = p.x
                        }

                        // Position constraints in normal state
                        if (!isCollapsed) {
                            p.x = p.x.coerceIn(0, screenWidth - buttonWidth)
                        }
                        p.y = p.y.coerceIn(0, screenHeight - buttonHeight)

                        view.post {
                            try {
                                windowManager?.updateViewLayout(floatingView, p)
                            } catch (e: Exception) {
                                // Ignore possible exceptions
                            }
                        }
                    }
                    lastAction = event.action
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Restore normal opacity
                    view.alpha = 1.0f
                    
                    // Use extracted parameters to determine if it's a click
                    val touchDuration = System.currentTimeMillis() - initialTouchTime
                    val isValidTouch = touchDuration < MAX_CLICK_DURATION && 
                        Math.abs(event.rawX - initialTouchX) < MAX_CLICK_DISTANCE && 
                        Math.abs(event.rawY - initialTouchY) < MAX_CLICK_DISTANCE
                    
                    if (isValidTouch) {
                        if (isCollapsed) {
                            // Expand button logic remains unchanged
                            params?.let { p ->
                                val jumpDistance = buttonWidth + 20
                                p.x = when (currentEdge) {
                                    Edge.LEFT -> jumpDistance
                                    Edge.RIGHT -> screenWidth - buttonWidth - jumpDistance
                                    Edge.NONE -> p.x
                                }
                                expandFromEdge()
                                isCollapsed = false
                                currentEdge = Edge.NONE
                                
                                view.animate()
                                    .translationX(0f)
                                    .setDuration(200)
                                    .withEndAction {
                                        windowManager?.updateViewLayout(floatingView, p)
                                    }
                                    .start()
                            }
                        } else {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastCaptureTime >= MIN_CAPTURE_INTERVAL && !isProcessing) {
                                isProcessing = true
                                lastCaptureTime = currentTime
                                
                                // Start rotation animation
                                view.animate()
                                    .rotation(360f)
                                    .setDuration(1000)
                                    .setInterpolator(LinearInterpolator())
                                    .withEndAction {
                                        view.rotation = 0f
                                        isProcessing = false
                                    }
                                    .start()
                                
                                // Trigger capture callback
                                onCaptureListener?.invoke()
                            }
                        }
                    } else if (!startedFromEdge) {
                        // Edge snapping detection when dragging ends from non-edge state
                        params?.let { p ->
                            when {
                                p.x <= EDGE_THRESHOLD -> {
                                    p.x = -COLLAPSED_WIDTH
                                    collapseToEdge(true)
                                    isCollapsed = true
                                    currentEdge = Edge.LEFT
                                }
                                p.x >= (screenWidth - buttonWidth - EDGE_THRESHOLD) -> {
                                    p.x = screenWidth - COLLAPSED_WIDTH
                                    collapseToEdge(false)
                                    isCollapsed = true
                                    currentEdge = Edge.RIGHT
                                }
                            }
                            windowManager?.updateViewLayout(floatingView, p)
                        }
                    }

                    lastAction = event.action
                    true
                }
                else -> false
            }
        }
    }

    private fun collapseToEdge(isLeft: Boolean) {
        floatingView?.let { view ->
            view.animate()
                .alpha(0.7f)
                .setDuration(150)
                .start()

            // Increase touch response area
            view.touchDelegate = TouchDelegate(
                Rect(
                    -TOUCH_EXPANSION,
                    -TOUCH_EXPANSION,
                    buttonWidth + TOUCH_EXPANSION,
                    buttonHeight + TOUCH_EXPANSION
                ),
                view
            )

            view.background = ContextCompat.getDrawable(
                context,
                if (isLeft) R.drawable.bg_floating_button_left_half
                else R.drawable.bg_floating_button_right_half
            )
        }
    }

    private fun expandFromEdge() {
        floatingView?.let { view ->
            view.animate()
                .alpha(1.0f)
                .setDuration(150)
                .start()

            // Restore normal touch area
            view.touchDelegate = null

            view.background = ContextCompat.getDrawable(
                context,
                R.drawable.bg_floating_button
            )
        }
    }

    fun hide() {
        floatingView?.let {
            windowManager?.removeView(it)
            floatingView = null
        }
    }

    fun setOnCaptureListener(listener: () -> Unit) {
        onCaptureListener = listener
    }

    // Added: Edge state enumeration
    private enum class Edge {
        NONE, LEFT, RIGHT
    }
} 