package com.najmi.sprint.feature.kanban

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.model.Task
import com.najmi.sprint.core.domain.model.TaskStatus
import com.najmi.sprint.core.ui.components.HeroPanel
import com.najmi.sprint.core.ui.components.SheetList

@Composable
fun KanbanScreen(
    viewModel: KanbanViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddTaskDialog by remember { mutableStateOf(false) }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val activeCount = (state.tasksByStatus[TaskStatus.BACKLOG]?.size ?: 0) + (state.tasksByStatus[TaskStatus.IN_PROGRESS]?.size ?: 0)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Daily Ledger: HeroPanel
            HeroPanel(
                title = "TASK BOARD",
                heroFigure = "$activeCount Active",
                toggleOptions = listOf("Board", "List"),
                selectedToggle = "Board",
                onToggleSelected = { /* TODO Phase 11 View toggle */ },
                chartData = listOf(1f, 2f, 1.5f, 3f, 2f), // Subtle dummy wave
                modifier = Modifier.zIndex(0f)
            )

            // SheetList overlapping HeroPanel
            SheetList(
                modifier = Modifier
                    .offset(y = (-24).dp)
                    .zIndex(1f)
            ) {
                // Horizontal Board
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp)
                ) {
                    TaskStatus.entries.forEach { status ->
                        KanbanColumn(
                            status = status,
                            tasks = state.tasksByStatus[status] ?: emptyList(),
                            contexts = state.contexts,
                            onMoveTask = { taskId, newStatus -> viewModel.moveTask(taskId, newStatus) },
                            onDeleteTask = { viewModel.deleteTask(it) }
                        )
                    }
                }
            }
        }

        // FAB positioned bottom right
        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
                .zIndex(2f),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp) // Large tier shape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }

        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false },
                onAdd = { title ->
                    viewModel.addTask(title)
                    showAddTaskDialog = false
                }
            )
        }
    }
}

@Composable
fun KanbanColumn(
    status: TaskStatus,
    tasks: List<Task>,
    contexts: List<Context>,
    onMoveTask: (String, TaskStatus) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    val isActive = status == TaskStatus.IN_PROGRESS

    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .padding(8.dp)
    ) {
        // Column header: Daily Ledger styling (crisp, transparent surface, small dot indicator)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = status.name.replace("_", " "),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Task count pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                modifier = Modifier.defaultMinSize(minWidth = 24.dp)
            ) {
                Text(
                    text = tasks.size.toString(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp), // Padding for FAB
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                val context = contexts.find { it.id == task.contextId }
                TaskCard(
                    task = task,
                    context = context,
                    onMoveTask = onMoveTask,
                    onDeleteTask = onDeleteTask
                )
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    context: Context?,
    onMoveTask: (String, TaskStatus) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    // Daily Ledger: Card with outline or very subtle elevation on white surface
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp, // Subtle depth
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Context tag: Now using the dot indicator approach
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(parseColor(context?.colorHex ?: "#888888"))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = context?.name ?: "Global",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { expandedMenu = true },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        TaskStatus.entries.forEach { status ->
                            if (status != task.status) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Move to ${status.name.replace("_", " ")}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        onMoveTask(task.id, status)
                                        expandedMenu = false
                                    }
                                )
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                onDeleteTask(task.id)
                                expandedMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "New Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onAdd(title) },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                )
            ) {
                Text("Add", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = RoundedCornerShape(24.dp), // Daily Ledger large shape
        containerColor = MaterialTheme.colorScheme.surface
    )
}

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}
