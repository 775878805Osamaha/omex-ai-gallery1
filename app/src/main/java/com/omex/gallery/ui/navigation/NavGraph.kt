package com.omex.gallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.omex.gallery.core.di.AppContainer
import com.omex.gallery.ui.feature_ask_image.AskImageScreen
import com.omex.gallery.ui.feature_ask_image.AskImageViewModel
import com.omex.gallery.ui.feature_chat.AiChatScreen
import com.omex.gallery.ui.feature_chat.AiChatViewModel
import com.omex.gallery.ui.feature_detail.MediaDetailScreen
import com.omex.gallery.ui.feature_detail.MediaDetailViewModel
import com.omex.gallery.ui.feature_gallery.GalleryScreen
import com.omex.gallery.ui.feature_gallery.GalleryViewModel
import com.omex.gallery.ui.feature_indexing.IndexingStatusScreen
import com.omex.gallery.ui.feature_search.SearchScreen
import com.omex.gallery.ui.feature_search.SearchViewModel

sealed class Screen(val route: String) {
    data object Gallery : Screen("gallery")
    data object Search : Screen("search")
    data object AiChat : Screen("ai_chat")
    data object MediaDetail : Screen("media_detail/{mediaId}") {
        fun createRoute(mediaId: Long) = "media_detail/$mediaId"
    }
    data object AskImage : Screen("ask_image/{mediaId}") {
        fun createRoute(mediaId: Long) = "ask_image/$mediaId"
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
                },
                onOpenSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onOpenAiChat = {
                    navController.navigate(Screen.AiChat.route)
                }
            )
        }

        composable(Screen.AiChat.route) {
            val chatViewModel: AiChatViewModel = viewModel(
                factory = AiChatViewModel.Factory(
                    appContainer.chatDao,
                    appContainer.aiChatEngine,
                    appContainer.generativeModelRepository
                )
            )

            AiChatScreen(
                viewModel = chatViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            val searchViewModel: SearchViewModel = viewModel(
                factory = SearchViewModel.Factory(
                    appContainer.mediaRepository,
                    appContainer.searchHistoryRepository
                )
            )

            SearchScreen(
                viewModel = searchViewModel,
                onMediaClick = { mediaId ->
                    navController.navigate(Screen.MediaDetail.createRoute(mediaId))
                },
                onBackClick = { navController.popBackStack() }
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
                onBackClick = { navController.popBackStack() },
                onAskAiClick = { targetMediaId ->
                    navController.navigate(Screen.AskImage.createRoute(targetMediaId))
                }
            )
        }

        composable(
            route = Screen.AskImage.route,
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
            val askImageViewModel: AskImageViewModel = viewModel(
                factory = AskImageViewModel.Factory(
                    appContainer.mediaRepository,
                    appContainer.askImageEngine,
                    appContainer.multimodalModelRepository,
                    mediaId
                )
            )

            AskImageScreen(
                viewModel = askImageViewModel,
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
