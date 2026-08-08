package com.omex.gallery.core.workers

import kotlinx.coroutines.flow.Flow

/**
 * Background task category for asynchronous AI indexing and batch inference.
 */
enum class WorkerTaskType {
    MEDIA_SCAN_INDEX,
    CLASSIFICATION_BATCH,
    OBJECT_DETECTION_BATCH,
    FACE_CLUSTERING_BATCH,
    DUPLICATE_DETECTION
}

/**
 * Execution constraint requirements for background jobs.
 */
data class TaskConstraint(
    val requiresCharging: Boolean = true,
    val requiresDeviceIdle: Boolean = false,
    val requiresWifi: Boolean = false
)

/**
 * Status state of background AI processing worker.
 */
sealed class TaskExecutionStatus {
    data object Idle : TaskExecutionStatus()
    data class Enqueued(val taskId: String) : TaskExecutionStatus()
    data class Running(val taskId: String, val progress: Float) : TaskExecutionStatus()
    data class Succeeded(val taskId: String, val resultSummary: String) : TaskExecutionStatus()
    data class Failed(val taskId: String, val error: String) : TaskExecutionStatus()
}

/**
 * Interface contract for scheduling and managing background AI worker tasks via WorkManager.
 */
interface AIWorkerScheduler {
    /**
     * Schedules a background AI task with given constraint requirements.
     */
    fun scheduleTask(taskType: WorkerTaskType, constraint: TaskConstraint): String

    /**
     * Cancels an active or pending background task.
     */
    fun cancelTask(taskType: WorkerTaskType)

    /**
     * Observes real-time execution state of task type.
     */
    fun observeTaskStatus(taskType: WorkerTaskType): Flow<TaskExecutionStatus>
}
