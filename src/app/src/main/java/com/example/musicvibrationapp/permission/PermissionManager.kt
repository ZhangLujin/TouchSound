package com.example.musicvibrationapp.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionManager private constructor(private val context: Context) {
    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    sealed class Permission(val manifestPermission: String) {
        object Microphone : Permission(Manifest.permission.RECORD_AUDIO)
        object Notification : Permission(Manifest.permission.POST_NOTIFICATIONS)
        object SystemAlertWindow : Permission(Manifest.permission.SYSTEM_ALERT_WINDOW)
        object ForegroundService : Permission(Manifest.permission.FOREGROUND_SERVICE)
    }

    sealed class PermissionResult {
        object Granted : PermissionResult()
        object Denied : PermissionResult()
        object PermanentlyDenied : PermissionResult()
        object RequiresSettings : PermissionResult()
    }

    fun checkPermission(permission: Permission): Boolean {
        return when (permission) {
            is Permission.SystemAlertWindow -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else {
                    true
                }
            }
            is Permission.Notification -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        permission.manifestPermission
                    ) == PermissionChecker.PERMISSION_GRANTED
                } else {
                    true
                }
            }
            else -> {
                ContextCompat.checkSelfPermission(
                    context,
                    permission.manifestPermission
                ) == PermissionChecker.PERMISSION_GRANTED
            }
        }
    }

    fun requestPermission(
        activity: Activity,
        permission: Permission,
        onResult: (PermissionResult) -> Unit
    ) {
        when (permission) {
            is Permission.SystemAlertWindow -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    activity.startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
                } else {
                    onResult(PermissionResult.Granted)
                }
            }
            is Permission.Notification -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(permission.manifestPermission),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                } else {
                    onResult(PermissionResult.Granted)
                }
            }
            else -> {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(permission.manifestPermission),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    fun updatePermissionState() {
        _permissionState.value = PermissionState(
            microphoneGranted = checkPermission(Permission.Microphone),
            notificationGranted = checkPermission(Permission.Notification),
            overlayGranted = checkPermission(Permission.SystemAlertWindow),
            foregroundServiceGranted = checkPermission(Permission.ForegroundService)
        )
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 101
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 102

        @Volatile
        private var instance: PermissionManager? = null

        fun getInstance(context: Context): PermissionManager {
            return instance ?: synchronized(this) {
                instance ?: PermissionManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
} 