package com.najmi.sprint.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.sprint.core.domain.model.ClassificationRule
import com.najmi.sprint.core.domain.model.Context
import com.najmi.sprint.core.domain.repository.ContextRepository
import com.najmi.sprint.core.domain.repository.RuleRepository
import com.najmi.sprint.tracking.TrackingEngine
import com.najmi.sprint.tracking.UsageStatsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val usageStatsTracker: UsageStatsTracker,
    private val contextRepository: ContextRepository,
    private val ruleRepository: RuleRepository,
    private val trackingEngine: TrackingEngine
) : ViewModel() {

    private val _topApps = MutableStateFlow<List<String>>(emptyList())
    val topApps: StateFlow<List<String>> = _topApps.asStateFlow()

    private val _contexts = MutableStateFlow<List<Context>>(emptyList())
    val contexts: StateFlow<List<Context>> = _contexts.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _contexts.value = contextRepository.observeActiveContexts().first()
            _topApps.value = usageStatsTracker.getTopRecentApps(days = 3, limit = 5)
        }
    }

    fun assignContextToApp(packageName: String, contextId: String) {
        viewModelScope.launch {
            val rule = ClassificationRule(
                packageName = packageName,
                contextId = contextId,
                lastConfirmedAt = Clock.System.now()
            )
            ruleRepository.insertOrUpdateRule(rule)
            
            // Remove from the list so the user sees progress
            _topApps.value = _topApps.value.filter { it != packageName }
        }
    }
    fun completeOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            // Seed the app with the last 3 days of historical tracking data!
            trackingEngine.backfillHistoricalData(days = 3)
            onFinished()
        }
    }
}
