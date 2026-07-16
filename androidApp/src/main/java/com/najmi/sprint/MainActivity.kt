package com.najmi.sprint

import android.content.Context
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.sprint.core.ui.theme.SprintTheme
import com.najmi.sprint.core.sync.auth.AuthManager
import com.najmi.sprint.tracking.TrackingService
import com.najmi.sprint.ui.auth.LoginScreen
import com.najmi.sprint.ui.main.MainScreen
import com.najmi.sprint.ui.onboarding.OnboardingScreen
import com.najmi.sprint.ui.permissions.PermissionViewModel
import com.najmi.sprint.ui.permissions.UsagePermissionScreen
import com.najmi.sprint.widget.SprintWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.updateAll
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SprintTheme {
                val permissionViewModel: PermissionViewModel = hiltViewModel()
                val hasPermission by permissionViewModel.hasUsagePermission.collectAsState()
                
                val sharedPrefs = getSharedPreferences("sprint_prefs", Context.MODE_PRIVATE)
                var showOnboarding by mutableStateOf(sharedPrefs.getBoolean("show_onboarding", true))
                var showAuth by mutableStateOf(!authManager.isLoggedIn())

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val modifier = Modifier.padding(innerPadding)
                    
                    if (showAuth) {
                        LoginScreen(onAuthSuccess = { showAuth = false })
                    } else if (!hasPermission) {
                        UsagePermissionScreen(
                            onPermissionGranted = {
                                startTrackingService()
                            },
                            viewModel = permissionViewModel
                        )
                    } else if (showOnboarding) {
                        startTrackingService()
                        OnboardingScreen(onComplete = { 
                            sharedPrefs.edit().putBoolean("show_onboarding", false).apply()
                            showOnboarding = false 
                        })
                    } else {
                        MainScreen()
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

    override fun onStop() {
        super.onStop()
        // Phase 7 Polish: Instantly update widget when user exits the app
        lifecycleScope.launch {
            SprintWidget().updateAll(this@MainActivity)
        }
    }
}