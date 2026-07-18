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
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.ui.components.HeroPanel
import com.najmi.sprint.core.ui.components.SheetList

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

    // Map daily breakdown totals to wave chart floats
    val waveData = state.dailyBreakdown.map { it.totalMinutes.toFloat() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Daily Ledger: HeroPanel
        HeroPanel(
            title = "THIS WEEK",
            heroFigure = formatHeroMinutes(state.weeklyTotalMinutes),
            toggleOptions = listOf("This Week", "Last Week"),
            selectedToggle = "This Week",
            onToggleSelected = { /* TODO Phase 11 Time Navigation */ },
            chartData = if (waveData.size >= 2) waveData else if (waveData.size == 1) waveData + waveData else emptyList(),
            modifier = Modifier.zIndex(0f)
        )

        // SheetList Overlapping the Hero
        SheetList(
            modifier = Modifier
                .offset(y = (-24).dp)
                .zIndex(1f)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                
                // --- Top App Stat ---
                item {
                    Column {
                        Text(
                            text = "Top Application",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = simplifyPackageName(state.topApp),
                            style = MaterialTheme.typography.titleLarge, // Inter SemiBold
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // --- AI Insights (Editorial Feel) ---
                if (state.retros.isNotEmpty()) {
                    items(state.retros) { retro ->
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "AI Insight",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Using the DM Serif Display typography for the pull-quote feel
                            Text(
                                text = "“${retro.summaryText}”",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = MaterialTheme.typography.headlineMedium.lineHeight
                            )
                        }
                    }
                }

                // --- Context Breakdown ---
                item {
                    Text(
                        text = "Context Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
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

                // --- Daily Activity Bar Chart ---
                item {
                    Text(
                        text = "Daily Activity",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    WeeklyBarChart(
                        dailyBreakdown = state.dailyBreakdown,
                        contexts = state.contexts
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

    // Daily Ledger: Clean row, no tonal background, dot indicator
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(parseColor(colorHex))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = contextName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = formatMinutes(minutes),
                style = MaterialTheme.typography.labelMedium, // Plex Mono Data role
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { animFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = parseColor(colorHex),
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        )
        Spacer(modifier = Modifier.height(8.dp))
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

    // Daily Ledger: Clean white/surface background, removing heavy tonal boxes
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
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
                                .width(16.dp) // Thinner, crisper bars
                                .height(segmentHeight.dp)
                                .clip(
                                    if (index == day.perContext.size - 1)
                                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    else RoundedCornerShape(0.dp)
                                )
                                .background(parseColor(ctx?.colorHex ?: "#888888"))
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = day.dayLabel.take(3), // 'Mon', 'Tue'
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

private fun formatMinutes(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun formatHeroMinutes(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return "${h}h ${m.toString().padStart(2, '0')}m"
}

private fun simplifyPackageName(pkg: String?): String {
    if (pkg == null) return "N/A"
    val parts = pkg.split(".")
    return parts.lastOrNull()?.replaceFirstChar { it.uppercase() } ?: pkg
}
