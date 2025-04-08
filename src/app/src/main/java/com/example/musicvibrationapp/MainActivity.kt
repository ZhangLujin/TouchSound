// File: MainActivity.kt
package com.example.musicvibrationapp

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.musicvibrationapp.manager.FloatingWindowManager
import com.example.musicvibrationapp.service.AudioCaptureService
import com.example.musicvibrationapp.ui.theme.MusicVibrationAppTheme
import com.example.musicvibrationapp.utils.TestAudioGenerator
import java.io.IOException
import android.Manifest
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private lateinit var floatingWindowManager: FloatingWindowManager

    // audio capture service instance
    private var audioCaptureService: AudioCaptureService? = null
    // service is bound
    private var bound = false
    // current amplitude
    private var amplitude by mutableStateOf(0)
    // current app name
    private var appName by mutableStateOf("unknown app")
    // WAV file path (complete original audio)
    private var audioFilePath by mutableStateOf<String?>(null)
    // separated vocal and BGM file paths
    private var vocalFilePath by mutableStateOf<String?>(null)
    private var bgmFilePath by mutableStateOf<String?>(null)
    // audio quality parameters
    private var sampleRate by mutableStateOf(0)
    private var bitDepth by mutableStateOf(0)
    private var channels by mutableStateOf(0)
    // real-time separation processing time (milliseconds)
    private var processingTimeMs by mutableStateOf(0.0)
    // capture status
    private var isCapturing by mutableStateOf(false)

    // media player
    private var mediaPlayer: MediaPlayer? = null          // play original audio
    private var vocalMediaPlayer: MediaPlayer? = null     // play vocal audio
    private var bgmMediaPlayer: MediaPlayer? = null       // play BGM audio

    // required permissions for capture button (only RECORD_AUDIO and VIBRATE)
    private val requiredPermissions = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.VIBRATE
    )

    // permission request handler
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // all permissions are granted, continue
            requestMediaProjection()
        } else {
            showToast("Permissions required to run")
        }
    }

    // media projection request handler
    private val startMediaProjection = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            startAudioCaptureService(result.resultCode, result.data!!)
        } else {
            showToast("Failed to get media projection permission")
        }
    }

    // broadcast receiver, for receiving audio data and file paths
    private val audioDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioCaptureService.ACTION_AUDIO_DATA) {
                amplitude = intent.getIntExtra(AudioCaptureService.EXTRA_AMPLITUDE, 0)
                appName = intent.getStringExtra(AudioCaptureService.EXTRA_APP_NAME) ?: "Unknown App"

                // update only when the path exists
                intent.getStringExtra(AudioCaptureService.EXTRA_AUDIO_FILE_PATH)?.let {
                    audioFilePath = it
                }
                intent.getStringExtra(AudioCaptureService.EXTRA_VOCAL_FILE_PATH)?.let {
                    vocalFilePath = it
                }
                intent.getStringExtra(AudioCaptureService.EXTRA_BGM_FILE_PATH)?.let {
                    bgmFilePath = it
                }

                sampleRate = intent.getIntExtra("sample_rate", 0)
                bitDepth = intent.getIntExtra("bit_depth", 0)
                channels = intent.getIntExtra("channels", 0)
                processingTimeMs = intent.getDoubleExtra(AudioCaptureService.EXTRA_PROCESSING_TIME_MS, 0.0)

                // determine if the capture is over, based on whether the audio file path is set
                if (intent.hasExtra(AudioCaptureService.EXTRA_AUDIO_FILE_PATH)) {
                    isCapturing = false
                    showToast("Audio capture finished")
                }
            }
        }
    }

    private val testAudioGenerator = TestAudioGenerator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // enable the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        try {
            floatingWindowManager = FloatingWindowManager.getInstance(this)
            checkOverlayPermission()
            
            // register the broadcast receiver
            registerReceiver(
                audioDataReceiver,
                IntentFilter(AudioCaptureService.ACTION_AUDIO_DATA),
                RECEIVER_NOT_EXPORTED
            )
            
            // set the content view
            setContent {
                MusicVibrationAppTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // add the scrollable container
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            MainScreen(
                                amplitude = amplitude,
                                appName = appName,
                                audioFilePath = audioFilePath,
                                vocalFilePath = vocalFilePath,
                                bgmFilePath = bgmFilePath,
                                sampleRate = sampleRate,
                                bitDepth = bitDepth,
                                channels = channels,
                                processingTimeMs = processingTimeMs,
                                isCapturing = isCapturing,
                                onStartCapture = {
                                    checkAndRequestPermissions()
                                    isCapturing = true
                                },
                                onStopCapture = { stopAudioCapture() },
                                onPlayAudio = { playAudio() },
                                onPlayVocal = { playVocal() },
                                onPlayBGM = { playBGM() }
                            )
                            TestControls()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error", e)
        }
    }

    // Check and request permissions
    private fun checkAndRequestPermissions() {
        try {
            Log.d(TAG, "Starting permission check")
            // check the floating window permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Log.d(TAG, "Requesting overlay permission")
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
                return
            }

            // Check other permissions
            if (requiredPermissions.all { permission ->
                    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
                }) {
                Log.d(TAG, "All permissions granted, requesting media projection")
                requestMediaProjection()
            } else {
                Log.d(TAG, "Requesting necessary permissions")
                permissionLauncher.launch(requiredPermissions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Permission check failed", e)
            showToast("Permission check failed: ${e.message}")
        }
    }

    // Request media projection permission
    private fun requestMediaProjection() {
        try {
            Log.d(TAG, "Requesting media projection permission")
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
        } catch (e: Exception) {
            Log.e(TAG, "Media projection request failed", e)
            showToast("Cannot start media projection: ${e.message}")
        }
    }

    // Start audio capture service
    private fun startAudioCaptureService(resultCode: Int, data: Intent) {
        try {
            Log.d(TAG, "Starting audio capture service")
            val serviceIntent = Intent(this, AudioCaptureService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("data", data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
            Log.d(TAG, "Audio capture service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Service start failed", e)
            showToast("Service start failed: ${e.message}")
        }
    }

    // Service connection
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                val binder = service as AudioCaptureService.LocalBinder
                audioCaptureService = binder.getService()
                bound = true
            } catch (e: Exception) {
                Log.e(TAG, "Error binding service", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioCaptureService = null
            bound = false
        }
    }

    // Stop audio capture
    private fun stopAudioCapture() {
        try {
            if (bound) {
                unbindService(connection)
                bound = false
            }
            stopService(Intent(this, AudioCaptureService::class.java))
            // Release MediaPlayer resources
            mediaPlayer?.release()
            mediaPlayer = null
            vocalMediaPlayer?.release()
            vocalMediaPlayer = null
            bgmMediaPlayer?.release()
            bgmMediaPlayer = null
            isCapturing = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capture", e)
        }
    }

    // Play original audio (after capture is finished)
    private fun playAudio() {
        audioFilePath?.let { path ->
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
                try {
                    mediaPlayer?.setDataSource(path)
                    mediaPlayer?.prepare()
                    mediaPlayer?.start()
                    showToast("Playing original audio")
                } catch (e: IOException) {
                    Log.e(TAG, "Error playing audio", e)
                    showToast("Cannot play audio file")
                }
            } else {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer?.pause()
                    showToast("Original audio paused")
                } else {
                    mediaPlayer?.start()
                    showToast("Resuming original audio")
                }
            }
        } ?: run {
            showToast("No audio file available")
        }
    }

    // Play vocal audio (after capture is finished)
    private fun playVocal() {
        vocalFilePath?.let { path ->
            if (vocalMediaPlayer == null) {
                vocalMediaPlayer = MediaPlayer()
                try {
                    vocalMediaPlayer?.setDataSource(path)
                    vocalMediaPlayer?.prepare()
                    vocalMediaPlayer?.start()
                    showToast("Playing vocal audio")
                } catch (e: IOException) {
                    Log.e(TAG, "Error playing vocal audio", e)
                    showToast("Cannot play vocal audio file")
                }
            } else {
                if (vocalMediaPlayer!!.isPlaying) {
                    vocalMediaPlayer?.pause()
                    showToast("Vocal audio paused")
                } else {
                    vocalMediaPlayer?.start()
                    showToast("Resuming vocal audio")
                }
            }
        } ?: run {
            showToast("No vocal audio file available")
        }
    }

    // Play BGM audio (after capture is finished)
    private fun playBGM() {
        bgmFilePath?.let { path ->
            if (bgmMediaPlayer == null) {
                bgmMediaPlayer = MediaPlayer()
                try {
                    bgmMediaPlayer?.setDataSource(path)
                    bgmMediaPlayer?.prepare()
                    bgmMediaPlayer?.start()
                    showToast("Playing BGM audio")
                } catch (e: IOException) {
                    Log.e(TAG, "Error playing BGM audio", e)
                    showToast("Cannot play BGM audio file")
                }
            } else {
                if (bgmMediaPlayer!!.isPlaying) {
                    bgmMediaPlayer?.pause()
                    showToast("BGM audio paused")
                } else {
                    bgmMediaPlayer?.start()
                    showToast("Resuming BGM audio")
                }
            }
        } ?: run {
            showToast("No BGM audio file available")
        }
    }

    // Show Toast message
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(audioDataReceiver)
            stopAudioCapture()
            floatingWindowManager.hide()
            testAudioGenerator.stopTestTone()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
        super.onDestroy()
    }

    private fun checkOverlayPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkOverlayPermission error", e)
        }
    }

    private fun showFloatingWindow() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                // Remove floatingWindowManager.show() here
                // Floating window display will be completely controlled by AudioCaptureService
            }
        } catch (e: Exception) {
            Log.e(TAG, "showFloatingWindow error", e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Overlay permission needed to display spectrum", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onActivityResult error", e)
        }
    }

    // Handle ActionBar back button click
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()  // End current Activity, return to previous level
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Handle system back button
    override fun onBackPressed() {
        super.onBackPressed()
        finish()  // End current Activity, return to previous level
    }

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
    }

    // Add test buttons in Compose UI
    @Composable
    fun TestControls() {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { testAudioGenerator.startTestTone(440.0) }) {
                Text("Play Test Audio (440Hz)")
            }

            Button(onClick = { testAudioGenerator.stopTestTone() }) {
                Text("Stop Test Audio")
            }
        }
    }
}

@Composable
fun MainScreen(
    amplitude: Int,
    appName: String,
    audioFilePath: String?,
    vocalFilePath: String?,
    bgmFilePath: String?,
    sampleRate: Int,
    bitDepth: Int,
    channels: Int,
    processingTimeMs: Double,
    isCapturing: Boolean,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onPlayAudio: () -> Unit,
    onPlayVocal: () -> Unit,
    onPlayBGM: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Volume visualization
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(
                        alpha = (amplitude / 255f).coerceIn(0f, 1f)
                    ),
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display current audio source app name
        Text(
            text = "Current Source: $appName",
            style = MaterialTheme.typography.bodyLarge
        )
        // Display volume value
        Text(
            text = "Volume: $amplitude",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Display real-time separation processing time
        Text(
            text = "Separation Time: %.2f ms".format(processingTimeMs),
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Control button: Start/Stop capture
        Button(
            onClick = {
                if (isCapturing) {
                    onStopCapture()
                } else {
                    onStartCapture()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isCapturing) "Stop Capture" else "Start Capture")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "Play Captured Audio" button (clickable after capture is stopped)
        Button(
            onClick = { onPlayAudio() },
            enabled = !isCapturing && audioFilePath != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Play Captured Audio")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "Play Separated Vocal" button (clickable after capture is stopped)
        Button(
            onClick = { onPlayVocal() },
            enabled = !isCapturing && vocalFilePath != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Play Separated Vocal")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "Play Separated BGM" button (clickable after capture is stopped)
        Button(
            onClick = { onPlayBGM() },
            enabled = !isCapturing && bgmFilePath != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Play Separated BGM")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Audio quality parameters
        if (audioFilePath != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                    .padding(16.dp)
            ) {
                Text(text = "Audio Parameters", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Sample Rate: $sampleRate Hz", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Bit Depth: $bitDepth bit", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Channels: $channels", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
