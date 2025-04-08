package com.example.musicvibrationapp.permission

data class PermissionState(
    val microphoneGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val overlayGranted: Boolean = false,
    val foregroundServiceGranted: Boolean = false
) 