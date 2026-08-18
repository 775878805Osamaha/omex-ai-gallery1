package com.omex.gallery.ui.feature_gallery

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.omex.gallery.ui.feature_gallery.components.ActiveFiltersRow
import com.omex.gallery.ui.feature_gallery.components.AdvancedFilterBottomSheet
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import java.io.File
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.omex.gallery.R
import com.omex.gallery.domain.model.Album
import com.omex.gallery.domain.model.AlbumType
import com.omex.gallery.domain.model.DuplicateGroupWithMedia
import com.omex.gallery.domain.model.IndexingStatus
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.domain.model.PersonGroup
import com.omex.gallery.domain.model.SortOrder
import com.omex.gallery.ui.theme.AmberAccent
import com.omex.gallery.ui.theme.CyanAccent
import com.omex.gallery.ui.theme.ObsidianBg
import com.omex.gallery.ui.theme.SurfaceCard
import com.omex.gallery.ui.theme.SurfaceDark
import com.omex.gallery.ui.theme.TextMutedDark
import com.omex.gallery.ui.theme.TextPrimaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun translateMlCategoryOrLabel(tag: String): String {
    return when (tag.lowercase(Locale.ROOT)) {
        "person", "human", "man", "woman", "child", "face" -> "شخص"
        "animal", "dog", "cat", "bird", "horse", "cow", "sheep", "elephant", "bear", "zebra", "giraffe" -> "حيوان"
        "food", "banana", "apple", "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake" -> "طعام"
        "vehicle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "bicycle" -> "مركبة"
        "nature", "tree", "flower", "mountain", "beach", "sky", "plant" -> "طبيعة"
        "fish" -> "سمك"
        "object" -> "عنصر"
        else -> tag
    }
}

fun translatePersonName(name: String): String {
    return if (name.startsWith("Person ")) {
        "شخص " + name.removePrefix("Person ")
    } else {
        name
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onMediaClick: (Long) -> Unit,
    onOpenIndexingStatus: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenAiChat: () -> Unit = {},
    onOpenStorageAnalyzer: () -> Unit = {}
) {
    val context = LocalContext.current
    val navTab by viewModel.selectedNavTab.collectAsStateWithLifecycle()
    val mediaItems by viewModel.mediaItems.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val columnCount by viewModel.gridColumnCount.collectAsStateWithLifecycle()
    val searchFilterState by viewModel.searchFilterState.collectAsStateWithLifecycle()
    val filterOptions by viewModel.filterOptions.collectAsStateWithLifecycle()
    val indexingProgress by viewModel.indexingProgress.collectAsStateWithLifecycle()
    val personGroups by viewModel.personGroups.collectAsStateWithLifecycle()
    val duplicateGroups by viewModel.duplicateGroups.collectAsStateWithLifecycle()
    val selectedItemIds by viewModel.selectedItemIds.collectAsStateWithLifecycle()
    val selectedCategoryIds by viewModel.selectedCategoryIds.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val aiAlbumSuggestions by viewModel.aiAlbumSuggestions.collectAsStateWithLifecycle()

    var isFilterPanelExpanded by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showAdvancedFiltersBottomSheet by remember { mutableStateOf(false) }

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val permissionsState = rememberMultiplePermissionsState(permissionsToRequest)

    val isMediaPermissionGranted = remember(permissionsState.permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionsState.permissions.any { perm ->
                (perm.permission == Manifest.permission.READ_MEDIA_IMAGES ||
                 perm.permission == Manifest.permission.READ_MEDIA_VIDEO ||
                 perm.permission == "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") &&
                        perm.status is PermissionStatus.Granted
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsState.permissions.any { perm ->
                (perm.permission == Manifest.permission.READ_MEDIA_IMAGES ||
                 perm.permission == Manifest.permission.READ_MEDIA_VIDEO) &&
                        perm.status is PermissionStatus.Granted
            }
        } else {
            permissionsState.permissions.any { perm ->
                perm.permission == Manifest.permission.READ_EXTERNAL_STORAGE &&
                        perm.status is PermissionStatus.Granted
            }
        }
    }

    LaunchedEffect(isMediaPermissionGranted) {
        if (isMediaPermissionGranted) {
            viewModel.triggerGalleryScan(context)
            viewModel.classifyUnclassifiedMedia(context)
        } else {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(CyanAccent, MaterialTheme.colorScheme.secondary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = ObsidianBg,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = TextPrimaryDark
                            )
                            Text(
                                text = stringResource(R.string.app_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent
                            )
                        }
                    }
                },
                actions = {
                    // Open Local AI Chat
                    IconButton(
                        onClick = onOpenAiChat,
                        modifier = Modifier.testTag("open_ai_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = stringResource(R.string.ai_chat_title),
                            tint = AmberAccent
                        )
                    }

                    // Open Local OCR Text Search
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier.testTag("open_ocr_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_title),
                            tint = Color.White
                        )
                    }

                    // Open Storage Analyzer
                    IconButton(
                        onClick = onOpenStorageAnalyzer,
                        modifier = Modifier.testTag("open_storage_analyzer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = stringResource(R.string.storage_analyzer_title),
                            tint = CyanAccent
                        )
                    }

                    if (navTab == NavTab.GALLERY) {
                        // Toggle Column Count
                        IconButton(
                            onClick = { viewModel.toggleGridColumnCount() },
                            modifier = Modifier.testTag("toggle_grid_columns_button")
                        ) {
                            Icon(
                                imageVector = if (columnCount == 2) Icons.Default.GridView else Icons.Default.GridOn,
                                contentDescription = "Grid Columns",
                                tint = TextPrimaryDark
                            )
                        }

                        // Sorting Dropdown
                        Box {
                            IconButton(
                                onClick = { isSortMenuExpanded = true },
                                modifier = Modifier.testTag("sort_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    tint = CyanAccent
                                )
                            }
                            DropdownMenu(
                                expanded = isSortMenuExpanded,
                                onDismissRequest = { isSortMenuExpanded = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("الأحدث أولاً (Newest)", color = TextPrimaryDark) },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.NEWEST_FIRST)
                                        isSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("الأقدم أولاً (Oldest)", color = TextPrimaryDark) },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.OLDEST_FIRST)
                                        isSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("الأكبر حجماً (Largest)", color = TextPrimaryDark) },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.LARGEST_FIRST)
                                        isSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("الأصغر حجماً (Smallest)", color = TextPrimaryDark) },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.SMALLEST_FIRST)
                                        isSortMenuExpanded = false
                                    }
                                )
                            }
                        }

                        // Advanced Filters Button with Badge
                        IconButton(
                            onClick = { showAdvancedFiltersBottomSheet = true },
                            modifier = Modifier.testTag("open_filters_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (searchFilterState.hasActiveFilters) {
                                        Badge(
                                            containerColor = AmberAccent,
                                            contentColor = ObsidianBg
                                        ) {
                                            Text("${searchFilterState.activeFilterCount}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = stringResource(R.string.filters_title),
                                    tint = if (searchFilterState.hasActiveFilters) AmberAccent else TextPrimaryDark
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onOpenIndexingStatus,
                        modifier = Modifier.testTag("indexing_status_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Indexer Dashboard",
                            tint = TextPrimaryDark
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isMediaPermissionGranted) {
                                viewModel.triggerGalleryScan(context)
                            } else {
                                permissionsState.launchMultiplePermissionRequest()
                            }
                        },
                        modifier = Modifier.testTag("refresh_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rescan",
                            tint = CyanAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = TextPrimaryDark
            ) {
                NavigationBarItem(
                    selected = navTab == NavTab.GALLERY,
                    onClick = { viewModel.selectNavTab(NavTab.GALLERY) },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery") },
                    label = { Text("الوسائط", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBg,
                        selectedTextColor = CyanAccent,
                        indicatorColor = CyanAccent,
                        unselectedIconColor = TextMutedDark,
                        unselectedTextColor = TextMutedDark
                    ),
                    modifier = Modifier.testTag("nav_gallery_tab")
                )
                NavigationBarItem(
                    selected = navTab == NavTab.ALBUMS,
                    onClick = { viewModel.selectNavTab(NavTab.ALBUMS) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Albums") },
                    label = { Text("الألبومات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBg,
                        selectedTextColor = AmberAccent,
                        indicatorColor = AmberAccent,
                        unselectedIconColor = TextMutedDark,
                        unselectedTextColor = TextMutedDark
                    ),
                    modifier = Modifier.testTag("nav_albums_tab")
                )
                NavigationBarItem(
                    selected = navTab == NavTab.SEARCH,
                    onClick = { viewModel.selectNavTab(NavTab.SEARCH) },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("البحث", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBg,
                        selectedTextColor = CyanAccent,
                        indicatorColor = CyanAccent,
                        unselectedIconColor = TextMutedDark,
                        unselectedTextColor = TextMutedDark
                    ),
                    modifier = Modifier.testTag("nav_search_tab")
                )
                NavigationBarItem(
                    selected = navTab == NavTab.AI_STUDIO,
                    onClick = { viewModel.selectNavTab(NavTab.AI_STUDIO) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Studio") },
                    label = { Text("استوديو AI", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBg,
                        selectedTextColor = AmberAccent,
                        indicatorColor = AmberAccent,
                        unselectedIconColor = TextMutedDark,
                        unselectedTextColor = TextMutedDark
                    ),
                    modifier = Modifier.testTag("nav_aistudio_tab")
                )
            }
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Batch Delete Confirmation Dialog
            if (showBatchDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showBatchDeleteDialog = false },
                    title = {
                        Text(
                            text = stringResource(R.string.batch_delete_dialog_title, selectedItemIds.size),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.batch_delete_dialog_msg),
                            color = TextMutedDark,
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteSelected()
                                showBatchDeleteDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("confirm_batch_delete_button")
                        ) {
                            Text(stringResource(R.string.delete_confirm), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showBatchDeleteDialog = false },
                            modifier = Modifier.testTag("cancel_batch_delete_button")
                        ) {
                            Text(stringResource(R.string.cancel), color = CyanAccent)
                        }
                    },
                    containerColor = SurfaceDark,
                    modifier = Modifier.testTag("batch_delete_dialog")
                )
            }

            // Batch Selection Floating Toolbar / Action Bar
            AnimatedVisibility(
                visible = selectedItemIds.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    CyanAccent.copy(alpha = 0.2f),
                                    AmberAccent.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("selection_action_bar"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.clearSelection() },
                            modifier = Modifier.testTag("clear_selection_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear),
                                tint = TextPrimaryDark
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.selection_mode_title, selectedItemIds.size),
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val allSelected = mediaItems.isNotEmpty() && selectedItemIds.size >= mediaItems.size
                        IconButton(
                            onClick = {
                                if (allSelected) {
                                    viewModel.clearSelection()
                                } else {
                                    viewModel.selectAll(mediaItems)
                                }
                            },
                            modifier = Modifier.testTag("select_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = stringResource(if (allSelected) R.string.deselect_all else R.string.select_all),
                                tint = CyanAccent
                            )
                        }
                        IconButton(
                            onClick = { viewModel.favoriteSelected() },
                            modifier = Modifier.testTag("favorite_selected_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.favorite_selected),
                                tint = AmberAccent
                            )
                        }
                        IconButton(
                            onClick = { showBatchDeleteDialog = true },
                            modifier = Modifier.testTag("delete_selected_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_selected),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Screen Content according to active Navigation Tab
            when (navTab) {
                NavTab.GALLERY -> {
                    // Selected Album Banner
                    if (selectedAlbum != null) {
                        val isThemedAi = selectedAlbum!!.albumType == AlbumType.THEMED_AI
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("active_album_banner"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isThemedAi) AmberAccent.copy(alpha = 0.15f) else SurfaceDark
                            ),
                            border = if (isThemedAi) {
                                androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f))
                            } else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isThemedAi) Icons.Default.AutoAwesome else Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (isThemedAi) AmberAccent else CyanAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isThemedAi) {
                                                stringResource(R.string.ai_suggestions_active_banner, selectedAlbum!!.title, selectedAlbum!!.itemCount)
                                            } else {
                                                "ألبوم: ${selectedAlbum!!.title} (${selectedAlbum!!.itemCount})"
                                            },
                                            color = if (isThemedAi) AmberAccent else TextPrimaryDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.selectAlbum(null) },
                                    modifier = Modifier.testTag("clear_selected_album_button")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Album", tint = TextMutedDark, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // AI Album Suggestions Section (local ML tag based)
                    if (selectedAlbum == null && aiAlbumSuggestions.isNotEmpty()) {
                        AiSuggestionsSection(
                            suggestions = aiAlbumSuggestions,
                            onExploreSuggestion = { viewModel.selectSuggestionAsAlbum(it) },
                            onSaveAsAlbum = { viewModel.createThemedAlbumFromSuggestion(it) },
                            onDismissSuggestion = { viewModel.dismissSuggestion(it) },
                            onCreateAllSuggestions = { viewModel.createAllSuggestedAlbums() }
                        )
                    }

                    // Gallery Filter Chips Row
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MediaFilterTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            item(key = "tab_${tab.name}") {
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectTab(tab) },
                                    label = {
                                        Text(
                                            text = when (tab) {
                                                MediaFilterTab.ALL -> "${stringResource(R.string.tab_all)} (${mediaItems.size})"
                                                MediaFilterTab.PHOTOS -> stringResource(R.string.tab_photos)
                                                MediaFilterTab.VIDEOS -> stringResource(R.string.tab_videos)
                                                MediaFilterTab.FAVORITES -> stringResource(R.string.tab_favorites)
                                                MediaFilterTab.PEOPLE -> "${stringResource(R.string.tab_people)} (${personGroups.size})"
                                                MediaFilterTab.DUPLICATES -> "${stringResource(R.string.tab_duplicates)} (${duplicateGroups.size})"
                                            },
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanAccent,
                                        selectedLabelColor = ObsidianBg,
                                        containerColor = SurfaceCard,
                                        labelColor = TextPrimaryDark
                                    ),
                                    border = null,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }

                    // Smart Virtual Folders Section Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = AmberAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "المجلدات الذكية",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberAccent
                                )
                            }
                            if (selectedCategoryIds.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.clearCategoryFilters() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("إلغاء التصفية (${selectedCategoryIds.size})", fontSize = 11.sp, color = CyanAccent)
                                }
                            }
                        }

                        // Smart Virtual Folder Chips
                        LazyRow(
                            contentPadding = PaddingValues(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories, key = { it.categoryId }) { category ->
                                val isCatSelected = selectedCategoryIds.contains(category.categoryId)
                                FilterChip(
                                    selected = isCatSelected,
                                    onClick = { viewModel.toggleCategoryFilter(category.categoryId) },
                                    label = {
                                        Text(
                                            text = category.nameArabic,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        val icon = when (category.categoryId) {
                                            "PERSON" -> Icons.Default.Person
                                            "PRODUCT" -> Icons.Default.AutoAwesome
                                            "TRADING" -> Icons.Default.ShowChart
                                            "SCREENSHOT" -> Icons.Default.CropFree
                                            "DOCUMENT" -> Icons.Default.Description
                                            "CAR" -> Icons.Default.DirectionsCar
                                            "FOOD" -> Icons.Default.Restaurant
                                            "NATURE" -> Icons.Default.Park
                                            "TRAVEL" -> Icons.Default.Flight
                                            "WORK" -> Icons.Default.Work
                                            else -> Icons.Default.Category
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberAccent,
                                        selectedLabelColor = ObsidianBg,
                                        selectedLeadingIconColor = ObsidianBg,
                                        containerColor = SurfaceDark,
                                        labelColor = TextPrimaryDark,
                                        iconColor = AmberAccent
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    border = null,
                                    modifier = Modifier.testTag("smart_folder_chip_${category.categoryId}")
                                )
                            }
                        }
                    }

                    // Active Advanced Filters Row (Phase 2)
                    ActiveFiltersRow(
                        filterState = searchFilterState,
                        categories = categories,
                        onRemoveCategory = { viewModel.removeCategoryFilter(it) },
                        onRemoveMediaType = { viewModel.removeMediaTypeFilter() },
                        onRemoveFavorite = { viewModel.removeFavoriteFilter() },
                        onRemoveDate = { viewModel.removeDateFilter() },
                        onRemoveFileSize = { viewModel.removeFileSizeFilter() },
                        onRemoveExtension = { viewModel.removeExtensionFilter(it) },
                        onRemoveDimension = { viewModel.removeDimensionFilter() },
                        onClearAll = { viewModel.clearAllAdvancedFilters() }
                    )

                    if (!isMediaPermissionGranted) {
                        MediaPermissionStateCard(
                            permissionsState = permissionsState,
                            context = context,
                            onRequestPermission = { permissionsState.launchMultiplePermissionRequest() }
                        )
                    } else if (mediaItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("لا توجد وسائط متاحة حالياً", color = TextMutedDark, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        // Date Grouped Gallery Grid
                        val grouped = remember(mediaItems) { groupMediaByDate(mediaItems) }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnCount),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize().testTag("gallery_media_grid")
                        ) {
                            grouped.forEach { (dateHeader, items) ->
                                item(key = "date_header_$dateHeader", span = { GridItemSpan(columnCount) }) {
                                    Text(
                                        text = dateHeader,
                                        color = CyanAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp)
                                    )
                                }
                                items(items, key = { it.id }) { item ->
                                    val isSelected = selectedItemIds.contains(item.id)
                                    MediaGridTile(
                                        item = item,
                                        isSelected = isSelected,
                                        isSelectionMode = selectedItemIds.isNotEmpty(),
                                        onClick = {
                                            if (selectedItemIds.isNotEmpty()) {
                                                viewModel.toggleSelection(item.id)
                                            } else {
                                                onMediaClick(item.id)
                                            }
                                        },
                                        onLongClick = { viewModel.toggleSelection(item.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                NavTab.ALBUMS -> {
                    if (!isMediaPermissionGranted) {
                        MediaPermissionStateCard(
                            permissionsState = permissionsState,
                            context = context,
                            onRequestPermission = { permissionsState.launchMultiplePermissionRequest() }
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            Text(
                                text = "الألبومات والمجلدات الذكية",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            if (aiAlbumSuggestions.isNotEmpty()) {
                                AiSuggestionsSection(
                                    suggestions = aiAlbumSuggestions,
                                    onExploreSuggestion = { viewModel.selectSuggestionAsAlbum(it) },
                                    onSaveAsAlbum = { viewModel.createThemedAlbumFromSuggestion(it) },
                                    onDismissSuggestion = { viewModel.dismissSuggestion(it) },
                                    onCreateAllSuggestions = { viewModel.createAllSuggestedAlbums() },
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            if (albums.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("لا توجد ألبومات متاحة حالياً", color = TextMutedDark)
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize().testTag("albums_grid")
                                ) {
                                    items(albums, key = { it.id }) { album ->
                                        AlbumCardTile(
                                            album = album,
                                            onClick = { viewModel.selectAlbum(album) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                NavTab.SEARCH -> {
                    if (!isMediaPermissionGranted) {
                        MediaPermissionStateCard(
                            permissionsState = permissionsState,
                            context = context,
                            onRequestPermission = { permissionsState.launchMultiplePermissionRequest() }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            OutlinedTextField(
                                value = searchFilterState.query,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text("ابحث في الصور والفيديوهات بالاسم أو المعلمات...", color = TextMutedDark, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanAccent) },
                                trailingIcon = {
                                    if (searchFilterState.query.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMutedDark)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_tab_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceCard,
                                    unfocusedContainerColor = SurfaceCard,
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize().testTag("search_results_grid")
                            ) {
                                items(mediaItems, key = { it.id }) { item ->
                                    MediaGridTile(
                                        item = item,
                                        isSelected = false,
                                        onClick = { onMediaClick(item.id) },
                                        onLongClick = {}
                                    )
                                }
                            }
                        }
                    }
                }

                NavTab.AI_STUDIO -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "استوديو الذكاء الاصطناعي (AI Studio)",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "تحليل الصور، التصنيف التلقائي، اكتشاف الكائنات، التعرف على الوجوه، وتحسين الدقة محلياً بالكامل.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedDark
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("ai_studio_status_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("حالة مفهرس الذكاء الاصطناعي", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(indexingProgress.message, color = AmberAccent, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.triggerAiScan(context) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("بدء الفحص الذكي", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.triggerFullReindex(context) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("إعادة الفهرسة الكلية", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenStorageAnalyzer() }
                                .testTag("ai_studio_storage_analyzer_card")
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = CyanAccent.copy(alpha = 0.15f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.PieChart, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.storage_analyzer_title),
                                        color = TextPrimaryDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.storage_analyzer_subtitle),
                                        color = TextMutedDark,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("الميزات المستقلية المخططة (المراحل القادمة)", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberAccent)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Ask Image & AI Chat (الدردشة التفاعلية مع الصور)", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("سؤال الذكاء الاصطناعي واستخراج النصوص والمعلومات من معرضك.", color = TextMutedDark, fontSize = 11.sp)
                                }
                            }
                        }

                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = CyanAccent)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("البحث الدلالي (Semantic AI Search)", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("البحث باستخدام التضمينات والمفاهيم الطبيعية.", color = TextMutedDark, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAdvancedFiltersBottomSheet) {
            AdvancedFilterBottomSheet(
                currentFilterState = searchFilterState,
                categories = categories,
                matchingCount = mediaItems.size,
                onApplyFilters = { newState ->
                    viewModel.setSearchFilterState(newState)
                },
                onDismiss = { showAdvancedFiltersBottomSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridTile(
    item: MediaItem,
    isSelected: Boolean,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) CyanAccent else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("media_item_${item.id}")
    ) {
        val context = LocalContext.current
        val imageModel = remember(item.id, item.thumbnailPath, item.uriString) {
            ImageRequest.Builder(context)
                .data(if (!item.thumbnailPath.isNullOrEmpty()) item.thumbnailPath else item.uriString)
                .size(360, 360)
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = imageModel,
            contentDescription = item.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(formatDuration(item.durationMs), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (item.isFavorite && !isSelected) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = AmberAccent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CyanAccent.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = CyanAccent,
                    modifier = Modifier.size(28.dp).testTag("selected_check_${item.id}")
                )
            }
        } else if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircleOutline,
                    contentDescription = "Unselected",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AlbumCardTile(
    album: Album,
    onClick: () -> Unit
) {
    val isAiThemed = album.albumType == AlbumType.THEMED_AI

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
        border = if (isAiThemed) androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("album_card_${album.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                if (!album.coverUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = album.coverUri,
                        contentDescription = album.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = when (album.albumType) {
                            AlbumType.CAMERA -> Icons.Default.CameraAlt
                            AlbumType.SCREENSHOTS -> Icons.Default.Image
                            AlbumType.DOWNLOADS -> Icons.Default.Folder
                            AlbumType.VIDEOS -> Icons.Default.PlayArrow
                            AlbumType.FAVORITES -> Icons.Default.Favorite
                            AlbumType.FOLDER -> Icons.Default.Folder
                            AlbumType.THEMED_AI -> Icons.Default.AutoAwesome
                        },
                        contentDescription = null,
                        tint = if (isAiThemed) AmberAccent else CyanAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }

                if (isAiThemed) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(AmberAccent, CyanAccent)
                                ),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ObsidianBg,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = stringResource(R.string.ai_suggestions_album_badge),
                                color = ObsidianBg,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(ObsidianBg.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("${album.itemCount}", color = if (isAiThemed) AmberAccent else CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = album.title,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private val arabicDateFormatter = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ar"))
private val globalDateHeaderCache = android.util.LruCache<Long, String>(1024)

private fun groupMediaByDate(items: List<MediaItem>): Map<String, List<MediaItem>> {
    return items.groupBy { item ->
        if (item.dateTaken > 0) {
            val dayKey = item.dateTaken / (24 * 60 * 60 * 1000L)
            var formatted = globalDateHeaderCache.get(dayKey)
            if (formatted == null) {
                formatted = synchronized(arabicDateFormatter) {
                    arabicDateFormatter.format(Date(item.dateTaken))
                }
                globalDateHeaderCache.put(dayKey, formatted)
            }
            formatted
        } else {
            "وسائط غير مؤرخة"
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = durationMs / (1000 * 60 * 60)
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

@Composable
fun ActiveFilterChip(label: String, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CyanAccent.copy(alpha = 0.15f))
            .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable { onRemove() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.clear), tint = CyanAccent, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
fun QuickTagChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) CyanAccent else SurfaceCard)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) ObsidianBg else TextPrimaryDark,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun PeopleView(personGroups: List<PersonGroup>, onMediaClick: (Long) -> Unit) {
    if (personGroups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_people_detected), color = TextMutedDark)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().testTag("people_grid")
        ) {
            items(personGroups) { group ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable {
                        group.mediaItems.firstOrNull()?.let { onMediaClick(it.id) }
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SurfaceDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(translatePersonName(group.personName), color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(stringResource(R.string.photos_count, group.faceCount), color = TextMutedDark, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicatesView(duplicateGroups: List<DuplicateGroupWithMedia>, onMediaClick: (Long) -> Unit) {
    if (duplicateGroups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_duplicates_found), color = TextMutedDark)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().testTag("duplicates_list")
        ) {
            items(duplicateGroups) { group ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.group_label, group.groupId.take(12), group.groupType), color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            group.members.forEach { member ->
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onMediaClick(member.mediaItem.id) }
                                ) {
                                    AsyncImage(
                                        model = member.mediaItem.thumbnailPath ?: member.mediaItem.uriString,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaGridItemCell(
    item: MediaItem,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    isSelected: Boolean = false,
    onSelectClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageModel = remember(item.thumbnailPath, item.uriString) {
        if (!item.thumbnailPath.isNullOrEmpty() && File(item.thumbnailPath).exists()) {
            File(item.thumbnailPath)
        } else {
            item.uriString
        }
    }

    val imageRequest = remember(context, imageModel) {
        ImageRequest.Builder(context)
            .data(imageModel)
            .size(300, 300)
            .crossfade(true)
            .build()
    }

    val formattedDuration = remember(item.durationMs) { formatDuration(item.durationMs) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .background(SurfaceCard)
            .then(
                if (isSelected) Modifier.border(2.dp, CyanAccent, RoundedCornerShape(2.dp)) else Modifier
            )
            .clickable { onClick() }
            .testTag("media_item_${item.id}")
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Semi-transparent selection tint if selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CyanAccent.copy(alpha = 0.25f))
            )
        }

        // Selection Overlay Icon (Top-Start / Top-Right corner)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) CyanAccent else Color.Black.copy(alpha = 0.35f))
                .clickable { onSelectClick?.invoke() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.CheckCircleOutline,
                contentDescription = "Select",
                tint = if (isSelected) ObsidianBg else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(14.dp)
            )
        }

        // Video Badge
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White
                    )
                }
            }
        }

        // GPS Badge if coordinates exist
        if (item.latitude != null && item.longitude != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 3.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "GPS",
                    tint = CyanAccent,
                    modifier = Modifier.size(9.dp)
                )
            }
        }

        // Favorite Button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable { onFavoriteClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (item.isFavorite) AmberAccent else Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MediaPermissionStateCard(
    permissionsState: com.google.accompanist.permissions.MultiplePermissionsState,
    context: Context,
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("media_permission_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(CyanAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "إذن الوصول للصور والفيديوهات مطلوب",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Text(
                    text = "يحتاج التطبيق إلى إذن الوصول إلى الصور والفيديوهات على جهازك لبدء فحص المعرض وفهرسته محلياً بالكامل.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedDark,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = ObsidianBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grant_permission_button")
                ) {
                    Text("منح إذن الوسائط", fontWeight = FontWeight.Bold)
                }

                val allPermanentlyDenied = permissionsState.permissions.all { perm ->
                    perm.status is PermissionStatus.Denied && !(perm.status as PermissionStatus.Denied).shouldShowRationale
                }

                if (allPermanentlyDenied || !permissionsState.shouldShowRationale) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_settings_button")
                    ) {
                        Text("فتح إعدادات التطبيق", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
