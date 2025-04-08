package com.example.musicvibrationapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicvibrationapp.view.DisplayMode

@Composable
fun DisplayModeSelector(
    currentMode: DisplayMode,
    onModeSelected: (DisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Display Mode",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DisplayMode.values().forEach { mode ->
                OutlinedButton(
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (mode == currentMode) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = when (mode) {
                            DisplayMode.TOP_BOTTOM -> "Top & Bottom"
                            DisplayMode.SIDES -> "Left & Right"
//                            DisplayMode.BOTH -> "BOTH"
                        },
                        color = if (mode == currentMode)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
} 