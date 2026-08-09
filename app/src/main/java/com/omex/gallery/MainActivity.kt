package com.omex.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.omex.gallery.core.di.AppContainer
import com.omex.gallery.core.indexer.IndexScheduler
import com.omex.gallery.core.indexer.MediaStoreObserver
import com.omex.gallery.ui.navigation.AppNavGraph
import com.omex.gallery.ui.theme.OmexGalleryTheme

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer
    private lateinit var mediaStoreObserver: MediaStoreObserver
    private lateinit var indexScheduler: IndexScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appContainer = AppContainer(applicationContext)
        indexScheduler = IndexScheduler(applicationContext)
        mediaStoreObserver = MediaStoreObserver(applicationContext, indexScheduler)

        // Register MediaStore observer for live content changes
        mediaStoreObserver.register()

        // Enqueue background synchronization pass (KEEP policy guarantees UI opens instantly from Room)
        indexScheduler.enqueueNormalSync()

        setContent {
            OmexGalleryTheme {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    appContainer = appContainer
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaStoreObserver.unregister()
    }
}

