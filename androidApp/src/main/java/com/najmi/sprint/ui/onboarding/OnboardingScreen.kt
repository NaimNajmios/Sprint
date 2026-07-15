package com.najmi.sprint.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val topApps by viewModel.topApps.collectAsState()
    val contexts by viewModel.contexts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sprint Setup") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                text = "Let's categorize your most used apps so we don't have to ask the AI.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (topApps.isEmpty()) {
                Text("All caught up!")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onComplete) {
                    Text("Continue to App")
                }
            } else {
                LazyColumn {
                    items(topApps) { app ->
                        AppCategorizationRow(
                            packageName = app,
                            contexts = contexts,
                            onAssign = { contextId ->
                                viewModel.assignContextToApp(app, contextId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppCategorizationRow(
    packageName: String,
    contexts: List<com.najmi.sprint.core.domain.model.Context>,
    onAssign: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = packageName, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                contexts.forEach { ctx ->
                    Button(onClick = { onAssign(ctx.id) }) {
                        Text(ctx.name)
                    }
                }
            }
        }
    }
}
