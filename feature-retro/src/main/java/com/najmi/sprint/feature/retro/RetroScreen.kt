package com.najmi.sprint.feature.retro

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.sprint.core.domain.model.Context

@Composable
fun RetroScreen(
    viewModel: RetroViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Weekly Retrospective",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // --- Hero Stats Cards ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.TrendingUp,
                    label = "Total Tracked",
                    value = formatMinutes(state.weeklyTotalMinutes),
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Star,
                    label = "Top App",
                    value = simplifyPackageName(state.topApp),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // --- Weekly Bar Chart ---
        item {
            Text(
                text = "Daily Activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            WeeklyBarChart(
                dailyBreakdown = state.dailyBreakdown,
                contexts = state.contexts
            )
        }

        // --- Context Breakdown ---
        item {
            Text(
                text = "Context Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(state.weeklyPerContext.entries.toList()) { (contextId, minutes) ->
            val context = state.contexts.find { it.id == contextId }
            ContextBreakdownRow(
                contextName = context?.name ?: "Unclassified",
                colorHex = context?.colorHex ?: "#888888",
                minutes = minutes,
                totalMinutes = state.weeklyTotalMinutes
            )
        }

        // --- Past AI Retros ---
        if (state.retros.isNotEmpty()) {
            item {
                Text(
                    text = "AI Insights",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(state.retros) { retro ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Week of ${retro.weekOf}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = retro.summaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WeeklyBarChart(
    dailyBreakdown: List<DailyBreakdown>,
    contexts: List<Context>
) {
    val maxMinutes = dailyBreakdown.maxOfOrNull { it.totalMinutes }?.coerceAtLeast(1L) ?: 1L

    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationPlayed = true }

    val animProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "barAnim"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            dailyBreakdown.forEach { day ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    // Stacked bar
                    val barHeight = if (maxMinutes > 0) {
                        (day.totalMinutes.toFloat() / maxMinutes) * 140f * animProgress
                    } else 0f

                    if (day.perContext.isNotEmpty()) {
                        // Draw stacked segments
                        day.perContext.entries.forEachIndexed { index, (contextId, minutes) ->
                            val ctx = contexts.find { it.id == contextId }
                            val segmentHeight = if (day.totalMinutes > 0) {
                                (minutes.toFloat() / day.totalMinutes) * barHeight
                            } else 0f

                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(segmentHeight.dp)
                                    .clip(
                                        if (index == day.perContext.size - 1)
                                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        else RoundedCornerShape(0.dp)
                                    )
                                    .background(parseColor(ctx?.colorHex ?: "#888888"))
                            )
                        }
                    } else {
                        // Empty placeholder bar
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = day.dayLabel,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ContextBreakdownRow(
    contextName: String,
    colorHex: String,
    minutes: Long,
    totalMinutes: Long
) {
    val fraction = if (totalMinutes > 0) minutes.toFloat() / totalMinutes else 0f

    var animPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animPlayed = true }

    val animFraction by animateFloatAsState(
        targetValue = if (animPlayed) fraction else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "progressAnim"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(parseColor(colorHex))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = contextName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = formatMinutes(minutes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = parseColor(colorHex),
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
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

private fun formatMinutes(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun simplifyPackageName(pkg: String?): String {
    if (pkg == null) return "N/A"
    // "com.android.chrome" -> "Chrome"
    val parts = pkg.split(".")
    return parts.lastOrNull()?.replaceFirstChar { it.uppercase() } ?: pkg
}
