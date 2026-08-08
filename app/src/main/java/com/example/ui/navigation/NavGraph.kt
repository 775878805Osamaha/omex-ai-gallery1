package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.core.di.AppContainer
import com.example.ui.feature_detail.MediaDetailScreen
import com.example.ui.feature_detail.MediaDetailViewModel
import com.example.ui.feature_gallery.GalleryScreen
import com.example.ui.feature_gallery.GalleryViewModel
import com.example.ui.feature_indexing.IndexingStatusScreen

sealed class Screen(val route: String) {
    data object Gallery : Screen("gallery")
    data object MediaDetail : Screen("media_detail/{mediaId}") {
        fun createRoute(mediaId: Long) = "media_detail/$mediaId"
    }
    data object IndexingStatus : Screen("indexing_status")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val galleryViewModel: GalleryViewModel = viewModel(
        factory = GalleryViewModel.Factory(appContainer.mediaRepository)
    )

    NavHost(
        navController = navController,
        startDestination = Screen.Gallery.route,
        modifier = modifier
    ) {
        composable(Screen.Gallery.route) {
            GalleryScreen(
                viewModel = galleryViewModel,
                onMediaClick = { mediaId ->
                    navController.navigate(Screen.MediaDetail.createRoute(mediaId))
                },
                onOpenIndexingStatus = {
                    navController.navigate(Screen.IndexingStatus.route)
                }
            )
        }

        composable(
            route = Screen.MediaDetail.route,
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
            val detailViewModel: MediaDetailViewModel = viewModel(
                factory = MediaDetailViewModel.Factory(appContainer.mediaRepository, mediaId)
            )

            MediaDetailScreen(
                viewModel = detailViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.IndexingStatus.route) {
            IndexingStatusScreen(
                galleryViewModel = galleryViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
