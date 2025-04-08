package com.example.musicvibrationapp.ui.UIService

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.musicvibrationapp.ui.theme.MusicVibrationAppTheme

class EntryActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { 
        MainViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MusicVibrationAppTheme {
                AppNavigation(activity = this)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == MainViewModel.MEDIA_PROJECTION_REQUEST_CODE &&
            resultCode == Activity.RESULT_OK && 
            data != null
        ) {
            viewModel.handleMediaProjectionResult(this, resultCode, data)
        }
    }
} 