package com.omex.gallery.core.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.omex.gallery.core.indexer.IndexWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AIWorkerSchedulerImpl(
    private val context: Context
) : AIWorkerScheduler {

    private val workManager = WorkManager.getInstance(context)

    override fun scheduleTask(taskType: WorkerTaskType, constraint: TaskConstraint): String {
        val workConstraints = Constraints.Builder()
            .setRequiresCharging(constraint.requiresCharging)
            .setRequiresDeviceIdle(constraint.requiresDeviceIdle)
            .setRequiredNetworkType(if (constraint.requiresWifi) NetworkType.UNMETERED else NetworkType.NOT_REQUIRED)
            .build()

        val workName = getWorkName(taskType)
        val request = OneTimeWorkRequestBuilder<IndexWorker>()
            .setConstraints(workConstraints)
            .addTag(taskType.name)
            .build()

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            request
        )

        return request.id.toString()
    }

    override fun cancelTask(taskType: WorkerTaskType) {
        val workName = getWorkName(taskType)
        workManager.cancelUniqueWork(workName)
    }

    override fun observeTaskStatus(taskType: WorkerTaskType): Flow<TaskExecutionStatus> {
        val workName = getWorkName(taskType)
        return workManager.getWorkInfosForUniqueWorkFlow(workName).map { list ->
            val info = list.firstOrNull() ?: return@map TaskExecutionStatus.Idle
            val taskId = info.id.toString()
            when (info.state) {
                WorkInfo.State.ENQUEUED -> TaskExecutionStatus.Enqueued(taskId)
                WorkInfo.State.RUNNING -> TaskExecutionStatus.Running(taskId, 0.5f)
                WorkInfo.State.SUCCEEDED -> TaskExecutionStatus.Succeeded(taskId, "Task completed successfully")
                WorkInfo.State.FAILED -> TaskExecutionStatus.Failed(taskId, "Task failed")
                WorkInfo.State.BLOCKED -> TaskExecutionStatus.Enqueued(taskId)
                WorkInfo.State.CANCELLED -> TaskExecutionStatus.Idle
            }
        }
    }

    private fun getWorkName(taskType: WorkerTaskType): String {
        return "work_${taskType.name.lowercase()}"
    }
}
