package com.najmi.sprint.feature.retro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.model.RetroEntry
import com.najmi.sprint.core.domain.model.Session
import com.najmi.sprint.core.domain.repository.ContextRepository
import com.najmi.sprint.core.domain.repository.RetroRepository
import com.najmi.sprint.core.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class DailyBreakdown(
    val dayLabel: String,         // e.g. "Mon", "Tue"
    val totalMinutes: Long,
    val perContext: Map<String, Long>  // contextId -> minutes
)

data class RetroState(
    val isLoading: Boolean = true,
    val contexts: List<Context> = emptyList(),
    val weeklyTotalMinutes: Long = 0,
    val weeklyPerContext: Map<String, Long> = emptyMap(), // contextId -> totalMinutes
    val dailyBreakdown: List<DailyBreakdown> = emptyList(),
    val retros: List<RetroEntry> = emptyList(),
    val topApp: String? = null,
    val topAppMinutes: Long = 0
)

@HiltViewModel
class RetroViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val contextRepository: ContextRepository,
    private val retroRepository: RetroRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RetroState())
    val state: StateFlow<RetroState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                contextRepository.observeActiveContexts(),
                retroRepository.observeRetros()
            ) { contexts, retros ->
                Pair(contexts, retros)
            }.collect { (contexts, retros) ->
                computeWeeklyStats(contexts, retros)
            }
        }
    }

    private suspend fun computeWeeklyStats(contexts: List<Context>, retros: List<RetroEntry>) {
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date

        // Get sessions for the last 7 days
        val weekStart = today.minus(DatePeriod(days = 6))
        val startInstant = weekStart.atStartOfDayIn(tz)
        val endInstant = today.minus(DatePeriod(days = -1)).atStartOfDayIn(tz) // tomorrow start

        val sessions = sessionRepository.getSessionsBetween(startInstant, endInstant)

        // Weekly totals per context
        val weeklyPerContext = mutableMapOf<String, Long>()
        // Daily breakdown
        val dailyMap = mutableMapOf<String, MutableMap<String, Long>>() // dayLabel -> contextId -> minutes
        // Top app tracking
        val appMinutes = mutableMapOf<String, Long>()

        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        for (session in sessions) {
            val endTime = session.endTime ?: continue
            val durationMin = (endTime.minus(session.startTime)).inWholeMinutes
            val contextId = session.contextId ?: "unclassified"

            // Weekly per context
            weeklyPerContext[contextId] = weeklyPerContext.getOrDefault(contextId, 0L) + durationMin

            // Daily breakdown
            val sessionDate = session.startTime.toLocalDateTime(tz).date
            val dayOfWeek = sessionDate.dayOfWeek.ordinal // 0=Mon ... 6=Sun
            val dayLabel = dayLabels.getOrElse(dayOfWeek) { "?" }
            val dayMap = dailyMap.getOrPut(dayLabel) { mutableMapOf() }
            dayMap[contextId] = dayMap.getOrDefault(contextId, 0L) + durationMin

            // Top app
            appMinutes[session.rawLabel] = appMinutes.getOrDefault(session.rawLabel, 0L) + durationMin
        }

        // Build ordered daily breakdown (Mon-Sun)
        val dailyBreakdown = dayLabels.map { label ->
            val perCtx = dailyMap[label] ?: emptyMap()
            DailyBreakdown(
                dayLabel = label,
                totalMinutes = perCtx.values.sum(),
                perContext = perCtx
            )
        }

        val topEntry = appMinutes.maxByOrNull { it.value }

        _state.value = RetroState(
            isLoading = false,
            contexts = contexts,
            weeklyTotalMinutes = weeklyPerContext.values.sum(),
            weeklyPerContext = weeklyPerContext,
            dailyBreakdown = dailyBreakdown,
            retros = retros,
            topApp = topEntry?.key,
            topAppMinutes = topEntry?.value ?: 0
        )
    }
}
