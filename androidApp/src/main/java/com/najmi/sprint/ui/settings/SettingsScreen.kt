package com.najmi.sprint.ui.settings

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.sprint.MainActivity
import com.najmi.sprint.core.sync.auth.AuthManager
import com.najmi.sprint.tracking.TrackingService
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val lastTrackedTime by viewModel.lastTrackedSessionTime.collectAsState()
    val classifyStatus by viewModel.classifyStatus.collectAsState()
    val retroStatus by viewModel.retroStatus.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val context = LocalContext.current
    
    // Check if service is currently running
    val isServiceRunning = isTrackingServiceRunning(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- AI Classification Section ---
            Text(
                text = "AI Classification",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ClassifyNowCard(
                status = classifyStatus,
                onClassifyNow = { viewModel.triggerClassifyNow() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ActionCard(
                icon = Icons.Rounded.Psychology,
                title = "Generate Weekly Retro",
                status = retroStatus,
                idleText = "Tap to generate AI weekly summary",
                runningText = "AI is writing your retro\u2026",
                successText = "Retro generated \u2713",
                failedText = "Generation failed. Retry?",
                onAction = { viewModel.triggerRetroNow() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- Cloud Sync Section ---
            Text(
                text = "Cloud Sync",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ActionCard(
                icon = Icons.Rounded.CloudSync,
                title = "Sync to Supabase",
                status = syncStatus,
                idleText = "Backup and restore your data",
                runningText = "Syncing with cloud\u2026",
                successText = "Sync complete \u2713",
                failedText = "Sync failed. Retry?",
                onAction = { viewModel.triggerSyncNow() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- Tracking Health Section ---
            Text(
                text = "Tracking Health",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            HealthCard(
                title = "Foreground Service",
                subtitle = if (isServiceRunning) "Running and active" else "Service is stopped",
                isHealthy = isServiceRunning
            )

            Spacer(modifier = Modifier.height(16.dp))

            HealthCard(
                title = "Last Logged Session",
                subtitle = formatLastTrackedTime(lastTrackedTime),
                isHealthy = lastTrackedTime != null
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    val activity = context as? MainActivity
                    activity?.authManager?.clearSession()
                    // Quick restart to show login screen
                    activity?.recreate()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Out",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun HealthCard(title: String, subtitle: String, isHealthy: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isHealthy) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                contentDescription = null,
                tint = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ClassifyNowCard(
    status: ActionStatus,
    onClassifyNow: () -> Unit
) {
    ActionCard(
        icon = Icons.Rounded.AutoFixHigh,
        title = "Classify Now",
        status = status,
        idleText = "Tap to classify untagged sessions",
        runningText = "AI is classifying\u2026",
        successText = "Classification complete \u2713",
        failedText = "Classification failed. Retry?",
        onAction = onClassifyNow
    )
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    status: ActionStatus,
    idleText: String,
    runningText: String,
    successText: String,
    failedText: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (status) {
                        ActionStatus.Idle -> idleText
                        ActionStatus.Running -> runningText
                        ActionStatus.Success -> successText
                        ActionStatus.Failed -> failedText
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onAction,
                enabled = status != ActionStatus.Running
            ) {
                if (status == ActionStatus.Running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Run")
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun isTrackingServiceRunning(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Int.MAX_VALUE)) {
        if (TrackingService::class.java.name == service.service.className) {
            return true
        }
    }
    return false
}

private fun formatLastTrackedTime(instant: Instant?): String {
    if (instant == null) return "No data recorded yet"
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "Last seen at ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}
