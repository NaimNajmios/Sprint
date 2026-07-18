package com.najmi.sprint.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.sprint.core.domain.model.ClassificationRule
import com.najmi.sprint.core.domain.repository.RuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IgnoredPackagesViewModel @Inject constructor(
    private val ruleRepository: RuleRepository
) : ViewModel() {

    val ignoredPackages: StateFlow<List<ClassificationRule>> = ruleRepository.observeIgnoredRules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun unignorePackage(packageName: String) {
        viewModelScope.launch {
            ruleRepository.setPackageIgnored(packageName, false)
        }
    }
}
