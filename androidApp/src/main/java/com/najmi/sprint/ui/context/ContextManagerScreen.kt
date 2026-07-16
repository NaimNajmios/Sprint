package com.najmi.sprint.ui.context

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.sprint.core.domain.model.Context

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextManagerScreen(
    onNavigateBack: () -> Unit,
    viewModel: ContextManagerViewModel = hiltViewModel()
) {
    val contexts by viewModel.contexts.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var contextToEdit by remember { mutableStateOf<Context?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Contexts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    contextToEdit = null
                    showDialog = true 
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Context")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Active Contexts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                )
            }
            
            items(contexts) { context ->
                ContextItem(
                    context = context,
                    onEdit = {
                        contextToEdit = context
                        showDialog = true
                    },
                    onDelete = {
                        viewModel.deleteContext(context.id)
                    }
                )
            }
        }

        if (showDialog) {
            ContextEditDialog(
                contextToEdit = contextToEdit,
                onDismiss = { showDialog = false },
                onSave = { name, colorHex ->
                    if (contextToEdit != null) {
                        viewModel.updateContext(contextToEdit!!.copy(name = name, colorHex = colorHex))
                    } else {
                        viewModel.addContext(name, colorHex)
                    }
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun ContextItem(
    context: Context,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit), // In the future, this can navigate to Project Manager
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color = Color(android.graphics.Color.parseColor(context.colorHex)), shape = CircleShape)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = context.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete Context",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ContextEditDialog(
    contextToEdit: Context?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(contextToEdit?.name ?: "") }
    var selectedColor by remember { mutableStateOf(contextToEdit?.colorHex ?: "#FF5722") }

    val presetColors = listOf(
        "#FF5722", // Deep Orange
        "#F44336", // Red
        "#E91E63", // Pink
        "#9C27B0", // Purple
        "#673AB7", // Deep Purple
        "#3F51B5", // Indigo
        "#2196F3", // Blue
        "#03A9F4", // Light Blue
        "#00BCD4", // Cyan
        "#009688", // Teal
        "#4CAF50", // Green
        "#8BC34A", // Light Green
        "#CDDC39", // Lime
        "#FFEB3B", // Yellow
        "#FFC107", // Amber
        "#FF9800", // Orange
        "#795548", // Brown
        "#607D8B"  // Blue Grey
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (contextToEdit == null) "New Context" else "Edit Context")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Context Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Select Color", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Color grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val rows = presetColors.chunked(6)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { rowColors ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowColors.forEach { colorHex ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                color = Color(android.graphics.Color.parseColor(colorHex)),
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = colorHex }
                                    ) {
                                        if (selectedColor == colorHex) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(Color.White, CircleShape)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, selectedColor)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
