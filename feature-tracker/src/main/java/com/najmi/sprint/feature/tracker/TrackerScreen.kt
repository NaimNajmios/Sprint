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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.model.Session
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun TrackerScreen(
    viewModel: TrackerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Today's Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Hero Chart
        TimeDistributionChart(
            timeSpentPerContext = state.timeSpentPerContext,
            contexts = state.contexts
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Timeline",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (state.todaySessions.isEmpty()) {
            Text(
                text = "No tracking data yet for today.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.todaySessions) { session ->
                    val context = state.contexts.find { it.id == session.contextId }
                    SessionCard(session, context)
                }
            }
        }
    }
}

@Composable
fun TimeDistributionChart(
    timeSpentPerContext: Map<String, Long>,
    contexts: List<Context>
) {
    val totalTime = timeSpentPerContext.values.sum().coerceAtLeast(1L) // prevent div by zero
    
    // Animation state
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationPlayed = true }

    val sweepAngle = animateFloatAsState(
        targetValue = if (animationPlayed) 360f else 0f,
        animationSpec = tween(durationMillis = 1500),
        label = "sweepAngle"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(200.dp)) {
                var startAngle = -90f
                for ((contextId, duration) in timeSpentPerContext) {
                    val context = contexts.find { it.id == contextId }
                    val color = parseColor(context?.colorHex ?: "#888888")
                    val fraction = duration.toFloat() / totalTime
                    val sweep = fraction * sweepAngle.value

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 40.dp.toPx(), cap = StrokeCap.Butt),
                        size = Size(size.width, size.height)
                    )
                    startAngle += sweep
                }
                
                // Draw a placeholder if no data
                if (timeSpentPerContext.isEmpty()) {
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 40.dp.toPx(), cap = StrokeCap.Butt),
                        size = Size(size.width, size.height)
                    )
                }
            }
            
            // Center text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatDuration(totalTime),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total Logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for ((contextId, _) in timeSpentPerContext) {
                val context = contexts.find { it.id == contextId } ?: continue
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(parseColor(context.colorHex))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = context.name, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SessionCard(session: Session, context: Context?) {
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
                    text = session.rawLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = context?.name ?: "Unclassified",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTime(session.startTime),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
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
