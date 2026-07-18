package com.najmi.sprint.ui.project

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.sprint.core.data.local.entity.GithubIssueCacheEntity
import com.najmi.sprint.core.domain.model.ProjectDocument
import com.najmi.sprint.core.ui.util.DocumentSafHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDocumentViewer: (String, String) -> Unit = { _, _ -> },
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val project by viewModel.project.collectAsState()
    val githubIssues by viewModel.githubIssues.collectAsState()
    val projectDocuments by viewModel.projectDocuments.collectAsState()
    val githubPatExists by viewModel.githubPatExists.collectAsState()

    val context = LocalContext.current

    var githubOwner by remember(project) { mutableStateOf(project?.githubOwner ?: "") }
    var githubRepo by remember(project) { mutableStateOf(project?.githubRepo ?: "") }
    var githubPat by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            DocumentSafHelper.takePersistableUriPermission(context, it)
            // Just use the last path segment as a basic title for testing
            val title = it.lastPathSegment ?: "Unknown Document"
            viewModel.addDocument(it, title)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Loading...", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // --- GitHub Config Section ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("GitHub Integration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = githubOwner,
                            onValueChange = { githubOwner = it },
                            label = { Text("Owner (e.g. facebook)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = githubRepo,
                            onValueChange = { githubRepo = it },
                            label = { Text("Repo (e.g. react)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = githubPat,
                            onValueChange = { githubPat = it },
                            label = { Text(if (githubPatExists) "Update PAT (Hidden)" else "Personal Access Token") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.saveGithubConfig(githubOwner, githubRepo, githubPat) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save & Sync")
                        }
                    }
                }
            }

            // --- Reference Docs Section ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reference Docs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Attach")
                    }
                }
            }

            if (projectDocuments.isEmpty()) {
                item {
                    Text("No documents attached.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(projectDocuments) { doc ->
                    ProjectDocItem(
                        document = doc,
                        onClick = {
                            onNavigateToDocumentViewer(viewModel.projectId, doc.id)
                        },
                        onDelete = { viewModel.removeDocument(doc.id) }
                    )
                }
            }

            // --- GitHub Issues Section ---
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                Text("Open Issues", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            if (githubIssues.isEmpty()) {
                item {
                    Text("No open issues or not synced yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(githubIssues) { issue ->
                    GithubIssueItem(issue)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun ProjectDocItem(document: ProjectDocument, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(document.title, fontWeight = FontWeight.Medium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun GithubIssueItem(issue: GithubIssueCacheEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("#${issue.issueNumber} - ${issue.state}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(issue.title, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
