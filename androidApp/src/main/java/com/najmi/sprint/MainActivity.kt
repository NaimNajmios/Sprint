package com.najmi.sprint

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.sprint.core.ui.theme.SprintTheme
import com.najmi.sprint.tracking.TrackingService
import com.najmi.sprint.ui.permissions.PermissionViewModel
import com.najmi.sprint.ui.permissions.UsagePermissionScreen
import com.najmi.sprint.ui.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SprintTheme {
                val permissionViewModel: PermissionViewModel = hiltViewModel()
                val hasPermission by permissionViewModel.hasUsagePermission.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val modifier = Modifier.padding(innerPadding)
                    
                    if (!hasPermission) {
                        UsagePermissionScreen(
                            onPermissionGranted = {
                                startTrackingService()
                            },
                            viewModel = permissionViewModel
                        )
                    } else {
                        // Start service if not already started
                        startTrackingService()
                        // Show the tracking health screen for now
                        SettingsScreen()
                    }
                }
            }
        }
    }

    private fun startTrackingService() {
        val intent = Intent(this, TrackingService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}