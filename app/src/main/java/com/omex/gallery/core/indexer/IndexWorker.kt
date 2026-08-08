package com.omex.gallery.core.indexer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.core.di.AppContainer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * WorkManager CoroutineWorker that executes media indexing in the background
 * with foreground notification updates.
 */
class IndexWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val appContainer = AppContainer(applicationContext)

        val scanner = MediaScanner(applicationContext)
        val metadataExtractor = MetadataExtractor(applicationContext)
        val hashGenerator = HashGenerator(applicationContext)
        val thumbnailGenerator = ThumbnailGenerator(applicationContext)
        val progressTracker = ProgressTracker()

        val indexer = MediaIndexer(
            scanner = scanner,
            metadataExtractor = metadataExtractor,
            hashGenerator = hashGenerator,
            thumbnailGenerator = thumbnailGenerator,
            repository = appContainer.mediaRepository,
            progressTracker = progressTracker
        )

        createNotificationChannel()
        setForeground(createForegroundInfo(0, 0, "Initializing indexer..."))

        // Launch collector job to keep foreground notification updated
        val progressJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            indexer.progressState.collectLatest { state ->
                if (state.isIndexing && state.totalCount > 0) {
                    try {
                        setForeground(
                            createForegroundInfo(
                                state.processedCount,
                                state.totalCount,
                                state.currentFileName
                            )
                        )
                    } catch (_: Exception) {}
                }
            }
        }

        return try {
            val result = indexer.executeIndexingPass()
            progressJob.cancel()

            if (result.isSuccess) {
                androidx.work.ListenableWorker.Result.success()
            } else {
                androidx.work.ListenableWorker.Result.retry()
            }
        } catch (e: Exception) {
            progressJob.cancel()
            androidx.work.ListenableWorker.Result.failure()
        }
    }

    private fun createForegroundInfo(processed: Int, total: Int, currentFile: String): ForegroundInfo {
        val title = "Indexing Gallery Media"
        val content = if (total > 0) {
            "$processed / $total - $currentFile"
        } else {
            "Scanning media store..."
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setProgress(total, processed, total == 0)
            .setOnlyAlertOnce(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OMEX Indexing Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress during media indexing passes"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "omex_indexer_channel"
        const val NOTIFICATION_ID = 4001
        const val WORK_NAME = "omex_media_index_worker"
    }
}
