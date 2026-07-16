package com.najmi.sprint

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.najmi.sprint.tracking.ClassificationWorker
import com.najmi.sprint.tracking.RetroGenerationWorker
import com.najmi.sprint.tracking.SyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Sprint Application class — entry point for Hilt dependency injection.
 */
@HiltAndroidApp
class SprintApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleAllBackgroundWorkers()
    }

    private fun scheduleAllBackgroundWorkers() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Wi-Fi or Mobile Data
            .setRequiresBatteryNotLow(true)
            .build()

        val workManager = WorkManager.getInstance(this)

        // 1. Classification Worker (Every 6 hours)
        val classificationRequest = PeriodicWorkRequestBuilder<ClassificationWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "ClassificationWorkerPeriodic",
            ExistingPeriodicWorkPolicy.KEEP,
            classificationRequest
        )

        // 2. Cloud Sync Worker (Every 4 hours)
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(4, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "SyncWorkerPeriodic",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        // 3. Weekly Retro Worker (Every 24 hours - gracefully exits if already generated)
        val retroRequest = PeriodicWorkRequestBuilder<RetroGenerationWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "RetroWorkerPeriodic",
            ExistingPeriodicWorkPolicy.KEEP,
            retroRequest
        )
    }
}
