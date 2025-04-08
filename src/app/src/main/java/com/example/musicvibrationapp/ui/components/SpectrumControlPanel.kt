package com.example.musicvibrationapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musicvibrationapp.view.DisplayMode
import com.example.musicvibrationapp.ui.viewmodel.SpectrumViewModel
import com.example.musicvibrationapp.view.SpectrumParameters

@Composable
fun SpectrumControlPanel(
    viewModel: SpectrumViewModel,
    modifier: Modifier = Modifier
) {
    val smoothingFactor by viewModel.smoothingFactor.collectAsState()
    val minThreshold by viewModel.minThreshold.collectAsState()
    val fallSpeed by viewModel.fallSpeed.collectAsState()
    val minFallSpeed by viewModel.minFallSpeed.collectAsState()
    val melSensitivity by viewModel.melSensitivity.collectAsState()
    val soloResponseStrength by viewModel.soloResponseStrength.collectAsState()
    val showAdvancedSettings by viewModel.showAdvancedSettings.collectAsState()
    val displayMode by viewModel.displayMode.collectAsState()

    Column(modifier = modifier) {
        // Display Mode controls
        DisplayModeSelector(
            currentMode = displayMode,
            onModeSelected = { viewModel.updateDisplayMode(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Basic parameters with primary color
        ParameterSlider(
            label = "Overall Speed",
            value = smoothingFactor,
            onValueChange = { viewModel.updateSmoothingFactor(it) },
            valueRange = SpectrumParameters.Ranges.SMOOTHING_FACTOR,
            helpText = "Faster: Higher frequency of effects, greater probability\nSlower: Smoother transitions\n",
            sliderColor = MaterialTheme.colorScheme.primary
        )

        ParameterSlider(
            label = "Sound Sensitivity",
            value = melSensitivity,
            onValueChange = { viewModel.updateMelSensitivity(it) },
            valueRange = SpectrumParameters.Ranges.MEL_SENSITIVITY,
            helpText = "Lower: Emphasize deep sounds like bass\nHigher: Capture voice/instruments details\n",
            sliderColor = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Advanced Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.toggleAdvancedSettings() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Advanced Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Icon(
                imageVector = if (showAdvancedSettings) 
                    Icons.Default.ExpandLess 
                else 
                    Icons.Default.ExpandMore,
                contentDescription = if (showAdvancedSettings) 
                    "Hide Advanced Settings" 
                else 
                    "Show Advanced Settings",
                tint = MaterialTheme.colorScheme.secondary
            )
        }

        // Advanced Settings Content with secondary color
        AnimatedVisibility(visible = showAdvancedSettings) {
            Column {
                ParameterSlider(
                    label = "Noise Filter",
                    value = minThreshold,
                    onValueChange = { viewModel.updateMinThreshold(it) },
                    valueRange = SpectrumParameters.Ranges.MIN_THRESHOLD,
                    helpText = "Lower: Show faint sounds (for quiet environment)\nHigher: Filter noise (for noisy scenes)\n",
                    sliderColor = MaterialTheme.colorScheme.secondary
                )
                
                ParameterSlider(
                    label = "Fade Speed",
                    value = fallSpeed,
                    onValueChange = { viewModel.updateFallSpeed(it) },
                    valueRange = SpectrumParameters.Ranges.FALL_SPEED,
                    helpText = "Slower: Lasting effects, smoother transitions (for slow songs)\nFaster: Quick refresh (for fast beats)\n",
                    sliderColor = MaterialTheme.colorScheme.secondary
                )
                
//                ParameterSlider(
//                    label = "Min Fall Speed",
//                    value = minFallSpeed,
//                    onValueChange = { viewModel.updateMinFallSpeed(it) },
//                    valueRange = SpectrumParameters.Ranges.MIN_FALL_SPEED,
//                    helpText = "Minimum speed for falling bars",
//                    sliderColor = MaterialTheme.colorScheme.secondary
//                )
                
                ParameterSlider(
                    label = "Main Sound Boost",
                    value = soloResponseStrength,
                    onValueChange = { viewModel.updateSoloResponseStrength(it) },
                    valueRange = SpectrumParameters.Ranges.SOLO_RESPONSE,
                    helpText = "Lower: Respond equally to all sounds\nHigher: Highlight vocalist/solo instruments\n",
                    sliderColor = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset Button
        Button(
            onClick = { viewModel.resetToDefaults() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text("Restore Default Settings")
        }
    }
}