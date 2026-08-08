package com.omex.gallery.core.log

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

enum class LogCategory {
    AI, DATABASE, SCANNER, WORKERS, UI, PERFORMANCE, ERRORS
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val category: LogCategory,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
)

object OmexLogger {

    private const val TAG_PREFIX = "OMEX_"
    private val logBuffer = ConcurrentLinkedQueue<LogEntry>()
    private const val MAX_BUFFER_SIZE = 1000

    fun d(category: LogCategory, tag: String, message: String) {
        log(Log.DEBUG, category, tag, message, null)
    }

    fun i(category: LogCategory, tag: String, message: String) {
        log(Log.INFO, category, tag, message, null)
    }

    fun w(category: LogCategory, tag: String, message: String, throwable: Throwable? = null) {
        log(Log.WARN, category, tag, message, throwable)
    }

    fun e(category: LogCategory, tag: String, message: String, throwable: Throwable? = null) {
        log(Log.ERROR, category, tag, message, throwable)
    }

    private fun log(level: Int, category: LogCategory, tag: String, message: String, throwable: Throwable?) {
        val fullTag = "$TAG_PREFIX[${category.name}]_$tag"
        when (level) {
            Log.DEBUG -> Log.d(fullTag, message, throwable)
            Log.INFO -> Log.i(fullTag, message, throwable)
            Log.WARN -> Log.w(fullTag, message, throwable)
            Log.ERROR -> Log.e(fullTag, message, throwable)
        }

        val entry = LogEntry(
            category = category,
            tag = tag,
            message = message,
            throwable = throwable
        )
        logBuffer.add(entry)
        if (logBuffer.size > MAX_BUFFER_SIZE) {
            logBuffer.poll()
        }
    }

    fun getRecentLogs(limit: Int = 100): List<LogEntry> {
        return logBuffer.toList().takeLast(limit)
    }

    fun formatLogsForExport(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return logBuffer.joinToString("\n") { entry ->
            val timeStr = sdf.format(Date(entry.timestamp))
            "[$timeStr] [${entry.category}] [${entry.tag}]: ${entry.message}" +
                    if (entry.throwable != null) " | Exception: ${entry.throwable.localizedMessage}" else ""
        }
    }
}
