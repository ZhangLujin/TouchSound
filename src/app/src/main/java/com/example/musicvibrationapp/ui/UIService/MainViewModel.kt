package com.example.musicvibrationapp.ui.UIService

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicvibrationapp.data.AppState
import com.example.musicvibrationapp.permission.PermissionHelper
import com.example.musicvibrationapp.permission.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val appContext: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(AppState())
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()
    
    private var stateReceiver: AudioCaptureStateReceiver? = null
    private val permissionHelper = PermissionHelper(appContext)

    fun registerStateReceiver(context: Context) {
        if (stateReceiver == null) {
            stateReceiver = AudioCaptureStateReceiver { isCapturing ->
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(isCapturing = isCapturing)
                }
            }
            
            context.registerReceiver(
                stateReceiver,
                IntentFilter(UIAudioCaptureService.ACTION_CAPTURE_STATE_CHANGED),
                Context.RECEIVER_NOT_EXPORTED
            )
        }
    }

    fun unregisterStateReceiver(context: Context) {
        stateReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                // Ignore possible exceptions
            }
            stateReceiver = null
        }
    }

    fun handleMediaProjectionResult(
        context: Context,
        resultCode: Int,
        data: Intent
    ) {
        // Start UI audio capture service
        val serviceIntent = Intent(context, UIAudioCaptureService::class.java).apply {
            action = UIAudioCaptureService.ACTION_START_CAPTURE
            putExtra(UIAudioCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(UIAudioCaptureService.EXTRA_RESULT_DATA, data)
        }
        context.startForegroundService(serviceIntent)
        
        _uiState.value = _uiState.value.copy(isCapturing = true)
    }

    fun toggleCapture(activity: Activity) {
        val requiredPermissions = listOf(
            PermissionManager.Permission.Microphone,
            PermissionManager.Permission.Notification,
            PermissionManager.Permission.SystemAlertWindow,
            PermissionManager.Permission.ForegroundService
        )

        permissionHelper.checkAndRequestPermissions(
            activity = activity,
            permissions = requiredPermissions,
            onAllGranted = {
                // Original toggleCapture logic
                val currentState = uiState.value.isCapturing
                if (!currentState) {
                    val mediaProjectionManager = activity.getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE
                    ) as MediaProjectionManager
                    activity.startActivityForResult(
                        mediaProjectionManager.createScreenCaptureIntent(),
                        MEDIA_PROJECTION_REQUEST_CODE
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isCapturing = false)
                    val stopIntent = Intent(activity, UIAudioCaptureService::class.java).apply {
                        action = UIAudioCaptureService.ACTION_STOP_CAPTURE
                    }
                    activity.startService(stopIntent)
                }
            },
            onDenied = { permission ->
                // Handle permission denial
                _uiState.value = _uiState.value.copy(
                    isCapturing = false,
                    error = "Permission denied: ${permission.manifestPermission}"
                )
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        stateReceiver = null
    }

    companion object {
        const val MEDIA_PROJECTION_REQUEST_CODE = 1
    }

    // Add Factory class
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(context.applicationContext) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 