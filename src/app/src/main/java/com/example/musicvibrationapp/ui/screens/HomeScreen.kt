package com.example.musicvibrationapp.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicvibrationapp.ui.UIService.MainViewModel
import com.example.musicvibrationapp.ui.components.*
import com.example.musicvibrationapp.ui.storage.SpectrumSettingsStorage
import com.example.musicvibrationapp.ui.viewmodel.SpectrumViewModel
import com.example.musicvibrationapp.ui.wrapper.SpectrumWrapper
import com.example.musicvibrationapp.view.AudioSpectrumView
import com.example.musicvibrationapp.permission.PermissionManager
import com.example.musicvibrationapp.permission.PermissionDialog
import androidx.compose.material3.Slider
import com.example.musicvibrationapp.view.EmotionColorManager

@Composable
private fun createSpectrumViewModelFactory(): ViewModelProvider.Factory {
    val context = LocalContext.current
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val storage = SpectrumSettingsStorage(context)
            val spectrumView = AudioSpectrumView(context)
            val wrapper = SpectrumWrapper(spectrumView)
            return SpectrumViewModel(storage, wrapper) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    activity: Activity,
    onDeveloperModeClick: () -> Unit,
    onConnectClick: () -> Unit
) {
    val context = LocalContext.current
    val mainViewModel: MainViewModel = viewModel(
        factory = MainViewModel.Factory(context)
    )
    val spectrumViewModel: SpectrumViewModel = viewModel(
        factory = createSpectrumViewModelFactory()
    )

    // Collect StateFlow values
    val showAdvancedSettings by spectrumViewModel.showAdvancedSettings.collectAsState()
    val uiState by mainViewModel.uiState.collectAsState()

    var showPermissionDialog by remember { mutableStateOf<PermissionManager.Permission?>(null) }

    // Register broadcast receiver when component is added to composition
    DisposableEffect(mainViewModel) {
        mainViewModel.registerStateReceiver(context)
        onDispose {
            mainViewModel.unregisterStateReceiver(context)
        }
    }

    // Background gradient
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Music Vibration",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    // Developer mode button - temporarily commented out
                    /* 
                    TextButton(
                        onClick = onDeveloperModeClick,
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(
                            "Dev",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }

                    // Add emotion test button
                    var showEmotionMenu by remember { mutableStateOf(false) }
                    Box {
                        TextButton(
                            onClick = { showEmotionMenu = true },
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text(
                                "Emotion",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }

                        DropdownMenu(
                            expanded = showEmotionMenu,
                            onDismissRequest = { showEmotionMenu = false }
                        ) {
                            EmotionColorManager.EmotionType.values().forEach { emotion ->
                                DropdownMenuItem(
                                    text = { Text(emotion.name) },
                                    onClick = {
                                        EmotionColorManager.getInstance().setEmotionForTesting(emotion)
                                        showEmotionMenu = false
                                    }
                                )
                            }
                        }
                    }
                    */
                    
                    // // Settings button
                    // IconButton(onClick = { /* Settings click handler */ }) {
                    //     Icon(
                    //         imageVector = Icons.Default.Settings,
                    //         contentDescription = "Settings",
                    //         tint = MaterialTheme.colorScheme.onBackground
                    //     )
                    // }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = gradientBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // // Status Card with connect button
                // StatusCard(
                //     title = "🎧 Bone Conduction Status",
                //     deviceName = null,
                //     batteryLevel = null,
                //     onConnectClick = onConnectClick
                // )

                // Spacer(modifier = Modifier.height(16.dp))

                // Main capture control button
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Button(
                        onClick = { mainViewModel.toggleCapture(activity) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isCapturing) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (uiState.isCapturing) 
                                    Icons.Default.Stop 
                                else 
                                    Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isCapturing) "Stop Capture" else "Start Capture",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                // Add permission dialog
                showPermissionDialog?.let { permission ->
                    PermissionDialog(
                        permission = permission,
                        onDismiss = { showPermissionDialog = null },
                        onGoToSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", activity.packageName, null)
                            }
                            activity.startActivity(intent)
                            showPermissionDialog = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Spectrum Control Panel
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Basic Settings title
                        Text(
                            text = "Basic Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Basic Settings content
                        SpectrumControlPanel(viewModel = spectrumViewModel)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}