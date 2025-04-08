package com.example.musicvibrationapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musicvibrationapp.ui.theme.TextSecondary

@Composable
fun StatusCard(
    title: String,
    deviceName: String?,
    batteryLevel: Int?,
    onConnectClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedButton(
                    onClick = onConnectClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Connect")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (deviceName != null) {
                Text(
                    text = "Connected: $deviceName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                Text(
                    text = "No device connected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            
            if (batteryLevel != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Battery: $batteryLevel%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
} 