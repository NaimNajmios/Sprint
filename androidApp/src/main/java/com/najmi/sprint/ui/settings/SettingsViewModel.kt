package com.najmi.sprint.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.najmi.sprint.core.domain.repository.SessionRepository
import com.najmi.sprint.tracking.ClassificationWorker
import com.najmi.sprint.tracking.RetroGenerationWorker
import com.najmi.sprint.tracking.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Instant
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _classifyStatus = MutableStateFlow<ActionStatus>(ActionStatus.Idle)
    val classifyStatus: StateFlow<ActionStatus> = _classifyStatus.asStateFlow()

    val lastTrackedSessionTime: StateFlow<Instant?> = sessionRepository.observeRecentSessions(1)
        .map { sessions ->
            sessions.firstOrNull()?.startTime
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _retroStatus = MutableStateFlow<ActionStatus>(ActionStatus.Idle)
    val retroStatus: StateFlow<ActionStatus> = _retroStatus.asStateFlow()

    private val _syncStatus = MutableStateFlow<ActionStatus>(ActionStatus.Idle)
    val syncStatus: StateFlow<ActionStatus> = _syncStatus.asStateFlow()

    fun triggerClassifyNow() {
        _classifyStatus.value = ActionStatus.Running
        val request = OneTimeWorkRequestBuilder<ClassificationWorker>()
            .addTag("classify_now_manual")
            .build()

        val workManager = WorkManager.getInstance(appContext)
        workManager.enqueue(request)

        workManager.getWorkInfoByIdLiveData(request.id).observeForever { info ->
            if (info != null) {
                when (info.state) {
                    WorkInfo.State.SUCCEEDED -> _classifyStatus.value = ActionStatus.Success
                    WorkInfo.State.FAILED -> _classifyStatus.value = ActionStatus.Failed
                    WorkInfo.State.RUNNING -> _classifyStatus.value = ActionStatus.Running
                    else -> { /* enqueued/blocked/cancelled — keep current */ }
                }
            }
        }
    }

    fun triggerRetroNow() {
        _retroStatus.value = ActionStatus.Running
        val request = OneTimeWorkRequestBuilder<RetroGenerationWorker>()
            .addTag("retro_now_manual")
            .build()

        val workManager = WorkManager.getInstance(appContext)
        workManager.enqueue(request)

        workManager.getWorkInfoByIdLiveData(request.id).observeForever { info ->
            if (info != null) {
                when (info.state) {
                    WorkInfo.State.SUCCEEDED -> _retroStatus.value = ActionStatus.Success
                    WorkInfo.State.FAILED -> _retroStatus.value = ActionStatus.Failed
                    WorkInfo.State.RUNNING -> _retroStatus.value = ActionStatus.Running
                    else -> { /* enqueued/blocked/cancelled — keep current */ }
                }
            }
        }
    }

    fun triggerSyncNow() {
        _syncStatus.value = ActionStatus.Running
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .addTag("sync_now_manual")
            .build()

        val workManager = WorkManager.getInstance(appContext)
        workManager.enqueue(request)

        workManager.getWorkInfoByIdLiveData(request.id).observeForever { info ->
            if (info != null) {
                when (info.state) {
                    WorkInfo.State.SUCCEEDED -> _syncStatus.value = ActionStatus.Success
                    WorkInfo.State.FAILED -> _syncStatus.value = ActionStatus.Failed
                    WorkInfo.State.RUNNING -> _syncStatus.value = ActionStatus.Running
                    else -> { /* enqueued/blocked/cancelled — keep current */ }
                }
            }
        }
    }
}

enum class ActionStatus { Idle, Running, Success, Failed }
