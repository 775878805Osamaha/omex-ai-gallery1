package com.omex.gallery.core.indexer

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * Controller for enqueueing, monitoring, and canceling WorkManager indexing jobs.
 */
class IndexScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Enqueues background media synchronization using KEEP policy so active workers are preserved.
     */
    fun enqueueNormalSync(requiresCharging: Boolean = false) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(requiresCharging)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<IndexWorker>()
            .setConstraints(constraints)
            .addTag(IndexWorker.WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            IndexWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Enqueues explicit full re-indexing work request.
     */
    fun enqueueFullReindex(requiresCharging: Boolean = false) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(requiresCharging)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<IndexWorker>()
            .setConstraints(constraints)
            .addTag(IndexWorker.WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            IndexWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Schedules periodic background indexing (e.g. every 12 hours).
     */
    fun schedulePeriodicIndexing(repeatIntervalHours: Long = 12L) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(false)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<IndexWorker>(
            repeatIntervalHours,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(IndexWorker.WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "${IndexWorker.WORK_NAME}_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    /**
     * Cancels any active or pending indexing work requests.
     */
    fun cancelIndexing() {
        workManager.cancelUniqueWork(IndexWorker.WORK_NAME)
        workManager.cancelUniqueWork("${IndexWorker.WORK_NAME}_periodic")
    }

    /**
     * Observes real-time WorkManager status of indexing job.
     */
    fun observeWorkStatus(): Flow<WorkInfo?> {
        return workManager.getWorkInfosForUniqueWorkFlow(IndexWorker.WORK_NAME)
            .map { list -> list.firstOrNull() }
    }
}
