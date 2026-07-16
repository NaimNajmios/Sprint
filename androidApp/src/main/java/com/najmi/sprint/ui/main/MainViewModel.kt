package com.najmi.sprint.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.repository.ContextRepository
import com.najmi.sprint.core.domain.repository.GlobalContextManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MainState(
    val contexts: List<Context> = emptyList(),
    val selectedContextId: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val contextRepository: ContextRepository,
    private val globalContextManager: GlobalContextManager
) : ViewModel() {

    val state: StateFlow<MainState> = combine(
        contextRepository.observeActiveContexts(),
        globalContextManager.selectedContextId
    ) { contexts, selectedId ->
        MainState(
            contexts = contexts,
            selectedContextId = selectedId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainState()
    )

    fun selectContext(contextId: String?) {
        globalContextManager.selectContext(contextId)
    }
}
