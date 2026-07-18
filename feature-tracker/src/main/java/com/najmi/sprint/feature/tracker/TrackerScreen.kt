package com.najmi.sprint.feature.tracker

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.najmi.sprint.core.ui.theme.SurfaceHero
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.model.Project
import com.najmi.sprint.core.domain.model.Session
import com.najmi.sprint.core.ui.components.HeroPanel
import com.najmi.sprint.core.ui.components.PillToggle
import com.najmi.sprint.core.ui.components.SheetList
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
    var selectedSessionGroup by remember { mutableStateOf<List<Session>?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val totalTime = state.timeSpentPerContext.values.sum()
    val formattedHeroTime = formatHeroDuration(totalTime)

    // Map real session durations (in minutes) to the wave chart
    val waveData = if (state.todaySessions.isEmpty()) {
        emptyList()
    } else {
        val now = kotlinx.datetime.Clock.System.now()
        val data = state.todaySessions.map { session ->
            val end = session.endTime ?: now
            end.minus(session.startTime).inWholeMinutes.toFloat().coerceAtLeast(1f)
        }
        if (data.size == 1) data + data else data // Ensure at least 2 points for Canvas path
    }
    
    val localContext = androidx.compose.ui.platform.LocalContext.current
    var timeRange by remember { mutableStateOf("Today") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface) // Match sheet background to prevent black gap at bottom
    ) {
        // Top HeroPanel
        item {
            HeroPanel(
                title = timeRange.uppercase(),
                heroFigure = formattedHeroTime,
                toggleOptions = listOf("Today", "This Week"),
                selectedToggle = timeRange,
                onToggleSelected = { 
                    timeRange = it 
                    if (it == "This Week") {
                        android.widget.Toast.makeText(localContext, "Weekly aggregation requires backend sync", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                chartData = waveData,
                modifier = Modifier.zIndex(0f)
            )
        }

        // The Sheet Header (Handle & Live Tracking)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )
                }

                // Live Tracking Indicator (if any)
                if (state.todaySessions.any { it.endTime == null }) {
                    val activeSession = state.todaySessions.first { it.endTime == null }
                    val activeContext = state.contexts.find { it.id == activeSession.contextId }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 8.dp),
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
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (state.todaySessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-24).dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tracking data yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val groupedSessions = state.todaySessions.groupBy { it.rawLabel }.values.toList()
            
            items(groupedSessions, key = { it.first().rawLabel }) { group ->
                val latestSession = group.maxByOrNull { it.startTime } ?: return@items
                val totalDuration = group.sumOf { it.endTime?.minus(it.startTime)?.inWholeMilliseconds ?: 0L }
                val isActive = group.any { it.endTime == null }
                val context = state.contexts.find { it.id == latestSession.contextId }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-24).dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    SessionCard(
                        session = latestSession,
                        context = context,
                        overrideDurationMs = if (isActive) null else totalDuration,
                        onClick = {
                            selectedSessionGroup = group
                            scope.launch { sheetState.show() }
                        }
                    )
                }
            }
            
            // Bottom Padding Item
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-24).dp)
                        .height(80.dp)
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }

    if (selectedSessionGroup != null) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        selectedSessionGroup = null
                    }
                }
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            SessionInspectorSheet(
                sessions = selectedSessionGroup!!,
                availableContexts = state.contexts,
                viewModel = viewModel,
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        selectedSessionGroup = null
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionCard(
    session: Session, 
    context: Context?,
    overrideDurationMs: Long? = null,
    onClick: () -> Unit = {}
) {
    val localContext = LocalContext.current

    val pm = localContext.packageManager

    var appName by remember(session.rawLabel) { mutableStateOf(simplifyPackageName(session.rawLabel)) }
    var appIconBitmap by remember(session.rawLabel) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(session.rawLabel) {
        withContext(Dispatchers.IO) {
            try {
                // If it's the debug build itself, it has ".debug" suffix but we should show "Sprint"
                val pkgName = session.rawLabel ?: ""
                val appInfo = pm.getApplicationInfo(pkgName, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                
                // Get the icon and convert to Bitmap
                val drawable = pm.getApplicationIcon(appInfo)
                val bitmap = drawable.toBitmap(width = 144, height = 144).asImageBitmap()
                
                withContext(Dispatchers.Main) {
                    appName = label
                    appIconBitmap = bitmap
                }
            } catch (e: Exception) {
                // Fallback to simplifyPackageName
            }
        }
    }

    // Row anatomy from Daily Ledger spec:
    // Icon (placeholder for now) | Title | Duration | Context-color dot
    // Small corner radius for list rows (8dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    Toast.makeText(
                        localContext,
                        session.rawLabel,
                        Toast.LENGTH_LONG
                    ).show()
                }
            ),
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap!!,
                        contentDescription = appName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = appName.take(1).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatTime(session.startTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val duration = overrideDurationMs ?: (session.endTime?.minus(session.startTime)?.inWholeMilliseconds ?: 0L)
            Text(
                text = if (overrideDurationMs == null && session.endTime == null) "Active" else formatDuration(duration),
                style = MaterialTheme.typography.labelMedium, // Plex Mono Data role
                color = if (overrideDurationMs == null && session.endTime == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Context-color dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(parseColor(context?.colorHex ?: "#888888"))
            )
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

// Data precision duration
private fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m"
        else -> "${s}s"
    }
}

// Hero format duration (0h 00m)
private fun formatHeroDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    return "${h}h ${m.toString().padStart(2, '0')}m"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionInspectorSheet(
    sessions: List<Session>,
    availableContexts: List<Context>,
    viewModel: TrackerViewModel,
    onClose: () -> Unit
) {
    val primarySession = sessions.maxByOrNull { it.startTime } ?: return
    
    var selectedContextId by remember { mutableStateOf(primarySession.contextId) }
    var selectedProjectId by remember { mutableStateOf(primarySession.projectId) }
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

    var appName by remember(primarySession.rawLabel) { mutableStateOf(simplifyPackageName(primarySession.rawLabel)) }
    val pm = LocalContext.current.packageManager
    
    LaunchedEffect(primarySession.rawLabel) {
        withContext(Dispatchers.IO) {
            try {
                val pkgName = primarySession.rawLabel ?: ""
                val appInfo = pm.getApplicationInfo(pkgName, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                withContext(Dispatchers.Main) {
                    appName = label
                }
            } catch (e: Exception) {
                // Keep simplified name
            }
        }
    }

    var showSessionsList by remember { mutableStateOf(false) }

    if (showSessionsList) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                IconButton(
                    onClick = { showSessionsList = false },
                    modifier = Modifier.offset(x = (-12).dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Batch Sessions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions.sortedByDescending { it.startTime }) { s ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                formatTime(s.startTime) + " - " + (s.endTime?.let { formatTime(it) } ?: "Now"), 
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "ID: ${s.id.take(8)}...", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        val durMs = s.endTime?.minus(s.startTime)?.inWholeMilliseconds ?: 0L
                        Text(
                            if (s.endTime == null) "Active" else formatDuration(durMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (s.endTime == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 12.dp), 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                }
            }
        }
        return // End composition here if showing list
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
            appName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        // A3: Show full package name for debugging
        Text(
            primarySession.rawLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                    formatTime(primarySession.startTime),
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
                    primarySession.endTime?.let { formatTime(it) } ?: "Active",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (primarySession.endTime == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Duration and Source
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    "DURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                val totalDurationMs = sessions.sumOf { it.endTime?.minus(it.startTime)?.inWholeMilliseconds ?: 0L }
                val isActive = sessions.any { it.endTime == null }
                Text(
                    if (isActive) "Active" else formatDuration(totalDurationMs),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "SOURCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    primarySession.source.name.replace("_", " "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))

        // AI Confidence
        val conf = primarySession.classificationConfidence
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
                if (primarySession.isManuallyCorrected) {
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

        // Technical IDs
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "BATCH OF ${sessions.size} SESSION(S)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                if (sessions.size > 1) {
                    TextButton(
                        onClick = { showSessionsList = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "View All",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "PRIMARY ID: ${primarySession.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "DEVICE ID: ${primarySession.deviceId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

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

        var showConfirmDialog by remember { mutableStateOf(false) }
        
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { 
                    Text("Confirm Changes", style = MaterialTheme.typography.titleLarge) 
                },
                text = { 
                    Text("Are you sure you want to update these sessions?", style = MaterialTheme.typography.bodyMedium) 
                },
                confirmButton = {
                    Button(
                        onClick = {
                            sessions.forEach { s ->
                                val updatedSession = s.copy(
                                    contextId = selectedContextId,
                                    projectId = selectedProjectId
                                )
                                viewModel.updateSession(updatedSession)
                            }
                            showConfirmDialog = false
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp)
            )
        }

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

        Button(
            onClick = { showConfirmDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(24.dp), // Pill shape for Daily Ledger
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White // Fix: Make text white instead of black
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
