package com.example.musicvibrationapp.ui.UIService

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AudioCaptureStateReceiver(
    private val onStateChanged: (Boolean) -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == UIAudioCaptureService.ACTION_CAPTURE_STATE_CHANGED) {
            val isCapturing = intent.getBooleanExtra(UIAudioCaptureService.EXTRA_IS_CAPTURING, false)
            onStateChanged(isCapturing)
        }
    }
} 