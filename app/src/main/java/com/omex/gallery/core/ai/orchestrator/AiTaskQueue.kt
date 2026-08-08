package com.omex.gallery.core.ai.orchestrator

import com.omex.gallery.core.ai.registry.ModelType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.PriorityQueue
import java.util.UUID

/**
 * Task priority level for AI processing requests.
 */
enum class TaskPriority {
    HIGH,     // User active screen (e.g. single item detail view)
    MEDIUM,   // Visible scroll viewport
    LOW       // Background idle batch indexer
}

/**
 * Task execution status.
 */
enum class TaskState {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Priority task request payload.
 */
data class AiTask<T>(
    val id: String = UUID.randomUUID().toString(),
    val mediaId: Long,
    val modelType: ModelType,
    val priority: TaskPriority,
    val timestamp: Long = System.currentTimeMillis(),
    val payload: Any,
    val executeBlock: suspend (Any) -> Result<T>
) : Comparable<AiTask<*>> {

    override fun compareTo(other: AiTask<*>): Int {
        // Higher priority first
        val priorityComparison = priority.ordinal.compareTo(other.priority.ordinal)
        if (priorityComparison != 0) return priorityComparison
        // FIFO order for same priority level
        return timestamp.compareTo(other.timestamp)
    }
}

/**
 * Thread-safe priority queue managing incoming AI inference workloads across modules.
 */
class AiTaskQueue {

    private val mutex = Mutex()
    private val queue = PriorityQueue<AiTask<*>>()
    
    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    /**
     * Enqueues a new AI inference task based on priority.
     */
    suspend fun <T> enqueue(task: AiTask<T>) = mutex.withLock {
        queue.add(task)
        _queueSize.value = queue.size
    }

    /**
     * Dequeues the highest-priority waiting task, or returns null if empty.
     */
    suspend fun dequeue(): AiTask<*>? = mutex.withLock {
        val task = queue.poll()
        _queueSize.value = queue.size
        task
    }

    /**
     * Clears all pending tasks in the queue (e.g., when switching folders or pausing background work).
     */
    suspend fun clear() = mutex.withLock {
        queue.clear()
        _queueSize.value = 0
    }

    /**
     * Returns true if queue has no pending items.
     */
    suspend fun isEmpty(): Boolean = mutex.withLock {
        queue.isEmpty()
    }
}
