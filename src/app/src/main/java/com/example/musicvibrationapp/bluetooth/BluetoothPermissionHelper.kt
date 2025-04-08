package com.example.musicvibrationapp.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.content.Intent
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity


class BluetoothPermissionHelper(private val activity: AppCompatActivity) {

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var bluetoothEnableLauncher: ActivityResultLauncher<Intent>
    private var onPermissionGranted: (() -> Unit)? = null

    init {
        setupPermissionLaunchers()
    }

    private fun setupPermissionLaunchers() {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions: Map<String, Boolean> ->
            if (permissions.all { it.value }) {
                checkBluetoothEnabled()
            } else {
                showPermissionExplanationDialog()
            }
        }

        bluetoothEnableLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onPermissionGranted?.invoke()
            } else {
                showBluetoothEnableDialog()
            }
        }
    }

    fun checkAndRequestPermissions(onGranted: () -> Unit) {
        this.onPermissionGranted = onGranted
        A2DPTransmissionManager.getInstance(activity).checkAndRequestPermissions(permissionLauncher)
    }

    private fun checkBluetoothEnabled() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter?.isEnabled == true) {
            onPermissionGranted?.invoke()
        } else {
            showBluetoothEnableDialog()
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Bluetooth access required")
            .setMessage("In order to use the bone conduction feature, the app requires Bluetooth permissions. Please grant permissions in the settings.")
            .setPositiveButton("To setup") { dialog: DialogInterface, _: Int ->
                openAppSettings()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showBluetoothEnableDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Bluetooth access required")
            .setMessage("In order to use the bone conduction feature, Bluetooth needs to be enabled.")
            .setPositiveButton("Start") { dialog: DialogInterface, _: Int ->
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothEnableLauncher.launch(enableBtIntent)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }
} 