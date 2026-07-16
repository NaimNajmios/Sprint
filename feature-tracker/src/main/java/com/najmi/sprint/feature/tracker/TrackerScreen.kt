package com.najmi.sprint.feature.tracker

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.model.Project
import com.najmi.sprint.core.domain.model.Session
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    viewModel: TrackerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedSession by remember { mutableStateOf<Session?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Alexandria: Editorial label + headline
        Text(
            text = "TODAY",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Live Tracking Pill
        if (state.todaySessions.any { it.endTime == null }) {
            val activeSession = state.todaySessions.first { it.endTime == null }
            val activeContext = state.contexts.find { it.id == activeSession.contextId }
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Tracking: ${activeContext?.name ?: "Unknown"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Hero Chart
        TimeDistributionChart(
            timeSpentPerContext = state.timeSpentPerContext,
            contexts = state.contexts
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Timeline",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (state.todaySessions.isEmpty()) {
            Text(
                text = "No tracking data yet for today.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.todaySessions) { session ->
                    val context = state.contexts.find { it.id == session.contextId }
                    SessionCard(
                        session = session, 
                        context = context,
                        onClick = {
                            selectedSession = session
                            scope.launch { sheetState.show() }
                        }
                    )
                }
            }
        }
    }

    if (selectedSession != null) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        selectedSession = null
                    }
                }
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            SessionInspectorSheet(
                session = selectedSession!!,
                availableContexts = state.contexts,
                viewModel = viewModel,
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        selectedSession = null
                    }
                }
            )
        }
    }
}

@Composable
fun TimeDistributionChart(
    timeSpentPerContext: Map<String, Long>,
    contexts: List<Context>
) {
    val totalTime = timeSpentPerContext.values.sum().coerceAtLeast(1L)
    
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationPlayed = true }

    val sweepAngle = animateFloatAsState(
        targetValue = if (animationPlayed) 360f else 0f,
        animationSpec = tween(durationMillis = 1500),
        label = "sweepAngle"
    )

    // Alexandria: Context bar (horizontal stacked bar) instead of donut
    Column(modifier = Modifier.fillMaxWidth()) {
        // Stacked horizontal bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (timeSpentPerContext.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    )
                } else {
                    timeSpentPerContext.forEach { (contextId, duration) ->
                        val ctx = contexts.find { it.id == contextId }
                        val fraction = duration.toFloat() / totalTime
                        Box(
                            modifier = Modifier
                                .weight(fraction.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(parseColor(ctx?.colorHex ?: "#888888"))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            for ((contextId, duration) in timeSpentPerContext) {
                val context = contexts.find { it.id == contextId } ?: continue
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(parseColor(context.colorHex))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = context.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionCard(
    session: Session, 
    context: Context?,
    onClick: () -> Unit = {}
) {
    // Alexandria: No hard borders. Use tonal surface shifts.
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Context indicator line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(parseColor(context?.colorHex ?: "#888888"))
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = simplifyPackageName(session.rawLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = context?.name ?: "Unclassified",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTime(session.startTime),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                val duration = session.endTime?.minus(session.startTime)?.inWholeMilliseconds ?: 0L
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}

private fun formatTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val h = local.hour.toString().padStart(2, '0')
    val m = local.minute.toString().padStart(2, '0')
    return "$h:$m"
}

private fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionInspectorSheet(
    session: Session,
    availableContexts: List<Context>,
    viewModel: TrackerViewModel,
    onClose: () -> Unit
) {
    var selectedContextId by remember { mutableStateOf(session.contextId) }
    var selectedProjectId by remember { mutableStateOf(session.projectId) }
    var projectsForContext by remember { mutableStateOf<List<Project>>(emptyList()) }
    var isContextDropdownExpanded by remember { mutableStateOf(false) }
    var isProjectDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedContextId) {
        if (selectedContextId != null) {
            projectsForContext = viewModel.getProjectsForContext(selectedContextId!!)
            if (projectsForContext.none { it.id == selectedProjectId }) {
                selectedProjectId = null
            }
        } else {
            projectsForContext = emptyList()
            selectedProjectId = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Session Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Raw Label
        Text(
            "APP / WINDOW",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            simplifyPackageName(session.rawLabel),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(20.dp))

        // Timestamps
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    "START",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatTime(session.startTime),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "END",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    session.endTime?.let { formatTime(it) } ?: "Active",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (session.endTime == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))

        // AI Confidence
        val conf = session.classificationConfidence
        if (conf != null) {
            val confPercent = (conf * 100).toInt()
            val confColor = when {
                confPercent >= 85 -> Color(0xFF4CAF50)
                confPercent >= 50 -> Color(0xFFFF9800)
                else -> Color(0xFFF44336)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "AI CONFIDENCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "$confPercent%",
                    style = MaterialTheme.typography.labelMedium,
                    color = confColor,
                    fontWeight = FontWeight.Bold
                )
                if (session.isManuallyCorrected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "(Manual)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Context Dropdown
        ExposedDropdownMenuBox(
            expanded = isContextDropdownExpanded,
            onExpandedChange = { isContextDropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = availableContexts.find { it.id == selectedContextId }?.name ?: "Unclassified",
                onValueChange = {},
                readOnly = true,
                label = { Text("Context") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isContextDropdownExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = isContextDropdownExpanded,
                onDismissRequest = { isContextDropdownExpanded = false }
            ) {
                availableContexts.forEach { ctx ->
                    DropdownMenuItem(
                        text = { Text(ctx.name, style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            selectedContextId = ctx.id
                            isContextDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Project Dropdown
        if (selectedContextId != null && projectsForContext.isNotEmpty()) {
            ExposedDropdownMenuBox(
                expanded = isProjectDropdownExpanded,
                onExpandedChange = { isProjectDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = projectsForContext.find { it.id == selectedProjectId }?.name ?: "No Project",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Project (Optional)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProjectDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = isProjectDropdownExpanded,
                    onDismissRequest = { isProjectDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("No Project", style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            selectedProjectId = null
                            isProjectDropdownExpanded = false
                        }
                    )
                    projectsForContext.forEach { proj ->
                        DropdownMenuItem(
                            text = { Text(proj.name, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                selectedProjectId = proj.id
                                isProjectDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Save Button — Alexandria: gradient-like primary fill
        Button(
            onClick = {
                val updatedSession = session.copy(
                    contextId = selectedContextId,
                    projectId = selectedProjectId
                )
                viewModel.updateSession(updatedSession)
                onClose()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                "Save Changes",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun simplifyPackageName(pkg: String?): String {
    if (pkg == null) return "Unknown"
    val parts = pkg.split(".")
    return parts.lastOrNull()?.replaceFirstChar { it.uppercase() } ?: pkg
}
