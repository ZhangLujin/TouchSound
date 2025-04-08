package com.example.musicvibrationapp.bluetooth

import android.Manifest
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class A2DPTransmissionManager private constructor(private val context: Context) {

    private var bluetoothA2dp: BluetoothA2dp? = null
    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var audioTrack: AudioTrack? = null
    private var connectedDevice: BluetoothDevice? = null
    private var isA2dpReady = false

    private val _transmissionState = MutableStateFlow<TransmissionState>(TransmissionState.Disconnected)
    val transmissionState: StateFlow<TransmissionState> = _transmissionState

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED)
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    handleA2dpStateChange(state, device)
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    handleBluetoothStateChange(state)
                }
            }
        }
    }

    init {
        registerBluetoothReceiver()
        initializeA2dpProxy()
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
    }

    private fun initializeA2dpProxy() {
        if (checkBluetoothPermissions()) {
            bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.A2DP) {
                        bluetoothA2dp = proxy as BluetoothA2dp
                        checkConnectedDevice()
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    if (profile == BluetoothProfile.A2DP) {
                        bluetoothA2dp = null
                        _transmissionState.value = TransmissionState.Disconnected
                    }
                }
            }, BluetoothProfile.A2DP)
        }
    }

    private fun checkConnectedDevice() {
        if (!checkBluetoothPermissions()) return

        bluetoothA2dp?.connectedDevices?.firstOrNull()?.let { device ->
            connectedDevice = device
            _transmissionState.value = TransmissionState.Connected(device)
            initializeAudioTrack()
        }
    }

    private fun handleA2dpStateChange(state: Int, device: BluetoothDevice?) {
        when (state) {
            BluetoothA2dp.STATE_CONNECTED -> {
                connectedDevice = device
                _transmissionState.value = TransmissionState.Connected(device!!)
                initializeAudioTrack()
            }
            BluetoothA2dp.STATE_DISCONNECTED -> {
                connectedDevice = null
                _transmissionState.value = TransmissionState.Disconnected
                releaseAudioTrack()
            }
        }
    }

    private fun handleBluetoothStateChange(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_OFF -> {
                _transmissionState.value = TransmissionState.BluetoothDisabled
                releaseAudioTrack()
            }
            BluetoothAdapter.STATE_ON -> {
                initializeA2dpProxy()
            }
        }
    }

    private fun initializeAudioTrack() {
        releaseAudioTrack()
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(44100)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build())
            .setBufferSizeInBytes(BUFFER_SIZE)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        
        audioTrack?.play()
        isA2dpReady = true
    }

    fun transmitAudio(audioData: ByteArray): Boolean {
        if (!isA2dpReady || audioTrack == null) return false

        try {
            val written = audioTrack?.write(audioData, 0, audioData.size) ?: 0
            return written > 0
        } catch (e: Exception) {
            Log.e(TAG, "error", e)
            return false
        }
    }

    fun checkAndRequestPermissions(permissionLauncher: ActivityResultLauncher<Array<String>>) {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (!hasPermission(Manifest.permission.BLUETOOTH)) {
                permissions.add(Manifest.permission.BLUETOOTH)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.BLUETOOTH)
        }
    }

    fun release() {
        try {
            context.unregisterReceiver(bluetoothReceiver)
            releaseAudioTrack()
            bluetoothA2dp?.let { a2dp ->
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, a2dp)
            }
            bluetoothA2dp = null
            connectedDevice = null
            isA2dpReady = false
        } catch (e: Exception) {
            Log.e(TAG, "error", e)
        }
    }

    private fun releaseAudioTrack() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            isA2dpReady = false
        } catch (e: Exception) {
            Log.e(TAG, "error", e)
        }
    }

    sealed class TransmissionState {
        object Disconnected : TransmissionState()
        object BluetoothDisabled : TransmissionState()
        data class Connected(val device: BluetoothDevice) : TransmissionState()
        data class Error(val message: String) : TransmissionState()
    }

    companion object {
        private const val TAG = "A2DPTransmissionManager"
        private const val BUFFER_SIZE = 4096

        @Volatile
        private var instance: A2DPTransmissionManager? = null

        fun getInstance(context: Context): A2DPTransmissionManager {
            return instance ?: synchronized(this) {
                instance ?: A2DPTransmissionManager(context).also { instance = it }
            }
        }
    }
} 