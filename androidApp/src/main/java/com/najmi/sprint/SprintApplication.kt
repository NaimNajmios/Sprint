package com.najmi.sprint

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.najmi.sprint.tracking.ClassificationWorker
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
        scheduleBackgroundClassification()
    }

    private fun scheduleBackgroundClassification() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        // Phase 3f: Run classification batch job every 6 hours
        val workRequest = PeriodicWorkRequestBuilder<ClassificationWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ClassificationWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
