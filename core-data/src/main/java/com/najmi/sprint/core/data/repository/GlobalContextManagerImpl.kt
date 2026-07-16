package com.najmi.sprint.core.data.repository

import com.najmi.sprint.core.domain.repository.GlobalContextManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalContextManagerImpl @Inject constructor() : GlobalContextManager {
    
    private val _selectedContextId = MutableStateFlow<String?>(null)
    override val selectedContextId: StateFlow<String?> = _selectedContextId.asStateFlow()

    override fun selectContext(contextId: String?) {
        _selectedContextId.value = contextId
    }
}
