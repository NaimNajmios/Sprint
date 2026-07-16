package com.najmi.sprint.feature.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.model.Session
import com.najmi.sprint.core.domain.model.Project
import com.najmi.sprint.core.domain.repository.ContextRepository
import com.najmi.sprint.core.domain.repository.GlobalContextManager
import com.najmi.sprint.core.domain.repository.ProjectRepository
import com.najmi.sprint.core.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class DashboardState(
    val isLoading: Boolean = true,
    val todaySessions: List<Session> = emptyList(),
    val contexts: List<Context> = emptyList(),
    val timeSpentPerContext: Map<String, Long> = emptyMap(), // contextId to Duration in MS
    val selectedContextId: String? = null
)

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val contextRepository: ContextRepository,
    private val projectRepository: ProjectRepository,
    private val globalContextManager: GlobalContextManager
) : ViewModel() {

    private val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    val state: StateFlow<DashboardState> = combine(
        sessionRepository.observeSessionsForDate(today),
        contextRepository.observeActiveContexts(),
        globalContextManager.selectedContextId
    ) { sessions, contexts, selectedContextId ->
        
        // Filter sessions if a specific context is selected globally
        val filteredSessions = if (selectedContextId != null) {
            sessions.filter { it.contextId == selectedContextId }
        } else {
            sessions
        }

        // Calculate time spent per context
        val timeSpent = mutableMapOf<String, Long>()
        for (session in filteredSessions) {
            val contextId = session.contextId ?: continue // Skip unclassified for the chart
            val duration = session.endTime?.minus(session.startTime)?.inWholeMilliseconds ?: 0L
            timeSpent[contextId] = timeSpent.getOrDefault(contextId, 0L) + duration
        }

        DashboardState(
            isLoading = false,
            todaySessions = filteredSessions.sortedByDescending { it.startTime },
            contexts = contexts,
            timeSpentPerContext = timeSpent,
            selectedContextId = selectedContextId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    fun updateSession(session: Session) {
        viewModelScope.launch {
            sessionRepository.updateSession(session.copy(isManuallyCorrected = true))
        }
    }

    suspend fun getProjectsForContext(contextId: String): List<Project> {
        return projectRepository.observeProjectsByContext(contextId).firstOrNull() ?: emptyList()
    }
}
