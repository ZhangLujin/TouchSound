package com.example.musicvibrationapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

@Composable
fun ParameterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    helpText: String,
    sliderColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    var showHelp by remember { mutableStateOf(true) }
    
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = String.format("%.2f", value),
                    style = MaterialTheme.typography.bodySmall
                )
                IconButton(
                    onClick = { showHelp = !showHelp },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = if (showHelp) "Hide Help" else "Show Help",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        if (showHelp) {
            Text(
                text = helpText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = "Range: ${String.format("%.2f", valueRange.start)} - ${String.format("%.2f", valueRange.endInclusive)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = sliderColor,
                activeTrackColor = sliderColor,
                inactiveTrackColor = sliderColor.copy(alpha = 0.24f)
            )
        )
    }
}