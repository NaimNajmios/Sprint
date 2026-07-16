package com.najmi.sprint.tracking

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.najmi.sprint.core.sync.SyncEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncEngine: SyncEngine
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            syncEngine.syncAll()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // In a real app we might return retry(), but for MVP we return failure
            Result.failure()
        }
    }
}
