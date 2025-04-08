package com.example.musicvibrationapp.emotion

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class TaskCompletionManager private constructor() {
    private val TAG = "TaskCompletionManager"
    private val _isTaskInProgress = MutableStateFlow(false)
    val isTaskInProgress: StateFlow<Boolean> = _isTaskInProgress
    private var retryJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val TIMEOUT_DURATION = 15000L // 15 seconds timeout
    
    // Remove audio sampling related states
    private val _isAudioSamplingInProgress = MutableStateFlow(false)
    val isAudioSamplingInProgress: StateFlow<Boolean> = _isAudioSamplingInProgress

    fun startTask(retryAction: suspend () -> Unit) {
        if (_isTaskInProgress.value) return
        Log.d(TAG, "Task started")
        _isTaskInProgress.value = true
        
        retryJob = scope.launch {
            var attempts = 0
            val startTime = System.currentTimeMillis()
            
            while (_isTaskInProgress.value && attempts < 50) {
                if (System.currentTimeMillis() - startTime > TIMEOUT_DURATION) {
                    Log.d(TAG, "Task timed out after ${attempts} attempts")
                    completeTask()
                    break
                }
                
                try {
                    retryAction()
                    delay(200)
                    attempts++
                } catch (e: Exception) {
                    // Only log non-cancellation exceptions
                    if (e !is kotlinx.coroutines.CancellationException) {
                        Log.d(TAG, "Retry attempt $attempts")
                    }
                }
            }
        }
    }

    fun completeTask() {
        Log.d(TAG, "Task completed")
        retryJob?.cancel()
        retryJob = null
        _isTaskInProgress.value = false
    }

    // Simplify audio sampling task management, no retries
    fun startAudioSamplingTask(action: suspend () -> Unit) {
        if (_isAudioSamplingInProgress.value) return
        _isAudioSamplingInProgress.value = true
        
        scope.launch {
            try {
                action()
            } catch (e: Exception) {
                // Ignore cancellation exceptions
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "Audio sampling task ended")
                }
            } finally {
                _isAudioSamplingInProgress.value = false
            }
        }
    }

    fun completeAudioSamplingTask() {
        _isAudioSamplingInProgress.value = false
    }

    companion object {
        @Volatile
        private var instance: TaskCompletionManager? = null

        fun getInstance(): TaskCompletionManager =
            instance ?: synchronized(this) {
                instance ?: TaskCompletionManager().also { instance = it }
            }
    }
} 