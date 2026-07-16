package com.najmi.sprint.feature.retro

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.sprint.core.domain.model.Context

@Composable
fun RetroScreen(
    viewModel: RetroViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Alexandria: Editorial hero with left border accent
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "WEEKLY",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Retrospective",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
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
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    label = "TOTAL TRACKED",
                    value = formatMinutes(state.weeklyTotalMinutes),
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Star,
                    label = "TOP APP",
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
                fontWeight = FontWeight.Medium,
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
                fontWeight = FontWeight.Medium,
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

        // --- AI Insights ---
        if (state.retros.isNotEmpty()) {
            item {
                Text(
                    text = "AI Insights",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(state.retros) { retro ->
                // Alexandria: Left accent border for editorial feel
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
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
                                color = MaterialTheme.colorScheme.onSurface,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
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
    // Alexandria: Surface tonal cards with no borders
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f)
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
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
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

    // Alexandria: Surface card for chart
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
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
                    val barHeight = if (maxMinutes > 0) {
                        (day.totalMinutes.toFloat() / maxMinutes) * 140f * animProgress
                    } else 0f

                    if (day.perContext.isNotEmpty()) {
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
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = day.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
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

    // Alexandria: Tonal surface, no border
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
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
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(parseColor(colorHex))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = contextName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = formatMinutes(minutes),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = parseColor(colorHex),
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
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
    val parts = pkg.split(".")
    return parts.lastOrNull()?.replaceFirstChar { it.uppercase() } ?: pkg
}
