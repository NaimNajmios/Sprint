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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.isSystemInDarkTheme
import android.content.SharedPreferences
import android.content.ComponentName
import android.content.pm.PackageManager
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

    object AppearanceManager {
        var pendingIconTheme: String? = null

        fun scheduleAppIconUpdate(theme: String) {
            pendingIconTheme = theme
        }

        fun applyPendingIconUpdate(context: Context) {
            val theme = pendingIconTheme ?: return
            pendingIconTheme = null
            updateAppIcon(context, theme)
        }

        private fun updateAppIcon(context: Context, theme: String) {
            val pm = context.packageManager
            val pkg = context.packageName
            val basePkg = "com.najmi.sprint"

            val systemComponent = ComponentName(pkg, "$basePkg.MainActivity")
            val lightComponent = ComponentName(pkg, "$basePkg.MainActivityLight")
            val darkComponent = ComponentName(pkg, "$basePkg.MainActivityDark")

            val targetComponent = when (theme) {
                "light" -> lightComponent
                "dark" -> darkComponent
                else -> systemComponent
            }

            val components = listOf(systemComponent, lightComponent, darkComponent)
            components.forEach { comp ->
                if (comp == targetComponent) {
                    pm.setComponentEnabledSetting(
                        comp,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } else {
                    pm.setComponentEnabledSetting(
                        comp,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        }
    }

    @Inject
    lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val sharedPrefs = getSharedPreferences("sprint_prefs", Context.MODE_PRIVATE)
            var themePref by remember { mutableStateOf(sharedPrefs.getString("theme_preference", "system") ?: "system") }

            DisposableEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                    if (key == "theme_preference") {
                        themePref = prefs.getString("theme_preference", "system") ?: "system"
                    }
                }
                sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            val darkTheme = when (themePref) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            SprintTheme(darkTheme = darkTheme) {
                val permissionViewModel: PermissionViewModel = hiltViewModel()
                val hasPermission by permissionViewModel.hasUsagePermission.collectAsState()
                
                val sharedPrefs = getSharedPreferences("sprint_prefs", Context.MODE_PRIVATE)
                var showOnboarding by remember { mutableStateOf(sharedPrefs.getBoolean("show_onboarding", true)) }
                var showAuth by remember { mutableStateOf(!authManager.isLoggedIn()) }

                LaunchedEffect(hasPermission, showAuth, showOnboarding) {
                    if (hasPermission && !showAuth && !showOnboarding) {
                        startTrackingService()
                    }
                }

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
        AppearanceManager.applyPendingIconUpdate(this)
    }
}