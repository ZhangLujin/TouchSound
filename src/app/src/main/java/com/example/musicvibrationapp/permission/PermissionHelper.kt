package com.example.musicvibrationapp.permission

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class PermissionHelper(private val context: Context) {
    private val permissionManager = PermissionManager.getInstance(context)

    fun checkAndRequestPermissions(
        activity: Activity,
        permissions: List<PermissionManager.Permission>,
        onAllGranted: () -> Unit,
        onDenied: (PermissionManager.Permission) -> Unit
    ) {
        var allGranted = true
        val notGranted = mutableListOf<PermissionManager.Permission>()

        for (permission in permissions) {
            if (!permissionManager.checkPermission(permission)) {
                allGranted = false
                notGranted.add(permission)
            }
        }

        if (allGranted) {
            onAllGranted()
        } else {
            // Check if any permissions are permanently denied
            val permanentlyDenied = notGranted.filter { permission ->
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    permission.manifestPermission
                )
            }

            if (permanentlyDenied.isNotEmpty()) {
                // If there are permanently denied permissions, show a prompt
                val missingPermissions = permanentlyDenied.joinToString(", ") { 
                    when (it) {
                        is PermissionManager.Permission.Microphone -> "Microphone"
                        is PermissionManager.Permission.Notification -> "Notification"
                        is PermissionManager.Permission.SystemAlertWindow -> "Overlay"
                        is PermissionManager.Permission.ForegroundService -> "Background Service"
                    }
                }
                Toast.makeText(
                    context,
                    "Missing required permissions: $missingPermissions. Please enable them manually.",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Continue with the original permission request logic
            notGranted.forEach { permission ->
                permissionManager.requestPermission(activity, permission) { result ->
                    when (result) {
                        is PermissionManager.PermissionResult.Granted -> {
                            permissionManager.updatePermissionState()
                            if (permissionManager.checkPermission(permission)) {
                                if (notGranted.all { permissionManager.checkPermission(it) }) {
                                    onAllGranted()
                                }
                            }
                        }
                        else -> onDenied(permission)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionDialog(
    permission: PermissionManager.Permission,
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                onDismiss()
            },
            title = { Text("Permission Required") },
            text = {
                Text(getPermissionRationale(permission))
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onGoToSettings()
                }) {
                    Text("Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    onDismiss()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun getPermissionRationale(permission: PermissionManager.Permission): String {
    return when (permission) {
        is PermissionManager.Permission.Microphone -> 
            "Microphone access is required to capture audio."
        is PermissionManager.Permission.Notification -> 
            "Notification permission is required to show capture status."
        is PermissionManager.Permission.SystemAlertWindow -> 
            "Overlay permission is required to show audio visualization."
        is PermissionManager.Permission.ForegroundService -> 
            "Background service permission is required to capture audio."
    }
} 