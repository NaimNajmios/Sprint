package com.najmi.sprint.ui.context

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.repository.ContextRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ContextManagerViewModel @Inject constructor(
    private val contextRepository: ContextRepository
) : ViewModel() {

    val contexts: StateFlow<List<Context>> = contextRepository.observeActiveContexts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addContext(name: String, colorHex: String) {
        viewModelScope.launch {
            val newContext = Context(
                id = UUID.randomUUID().toString(),
                name = name,
                colorHex = colorHex,
                isActive = true
            )
            contextRepository.insertContext(newContext)
        }
    }

    fun updateContext(context: Context) {
        viewModelScope.launch {
            contextRepository.updateContext(context)
        }
    }

    fun deleteContext(contextId: String) {
        viewModelScope.launch {
            // We soft delete by setting isActive to false
            val context = contextRepository.getContextById(contextId)
            if (context != null) {
                contextRepository.updateContext(context.copy(isActive = false))
            }
        }
    }
}
