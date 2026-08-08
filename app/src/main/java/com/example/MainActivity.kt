package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.core.di.AppContainer
import com.example.ui.navigation.AppNavGraph
import com.example.ui.theme.OmexGalleryTheme

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appContainer = AppContainer(applicationContext)

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
}

