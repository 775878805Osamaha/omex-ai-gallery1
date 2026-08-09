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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.omex.gallery.R
import com.omex.gallery.domain.model.DuplicateGroupWithMedia
import com.omex.gallery.domain.model.IndexingStatus
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.ui.util.recompositionHighlighter
import com.omex.gallery.domain.model.PersonGroup
import com.omex.gallery.domain.model.SearchFilterOptions
import com.omex.gallery.domain.model.SearchFilterState
import com.omex.gallery.ui.theme.AmberAccent
import com.omex.gallery.ui.theme.CyanAccent
import com.omex.gallery.ui.theme.ObsidianBg
import com.omex.gallery.ui.theme.SurfaceCard
import com.omex.gallery.ui.theme.SurfaceDark
import com.omex.gallery.ui.theme.TextMutedDark
import com.omex.gallery.ui.theme.TextPrimaryDark
import java.io.File
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
    onOpenIndexingStatus: () -> Unit
) {
    val context = LocalContext.current
    val pagedItems = viewModel.pagedMediaItems.collectAsLazyPagingItems()
    val mediaItems by viewModel.mediaItems.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchFilterState by viewModel.searchFilterState.collectAsStateWithLifecycle()
    val filterOptions by viewModel.filterOptions.collectAsStateWithLifecycle()
    val indexingProgress by viewModel.indexingProgress.collectAsStateWithLifecycle()
    val personGroups by viewModel.personGroups.collectAsStateWithLifecycle()
    val duplicateGroups by viewModel.duplicateGroups.collectAsStateWithLifecycle()
    val selectedItemIds by viewModel.selectedItemIds.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(true) }
    var isFilterPanelExpanded by remember { mutableStateOf(false) }

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
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
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

    val isPartialPermissionGranted = remember(permissionsState.permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val userSelected = permissionsState.permissions.find { it.permission == "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" }
            val fullImages = permissionsState.permissions.find { it.permission == Manifest.permission.READ_MEDIA_IMAGES }
            val fullVideos = permissionsState.permissions.find { it.permission == Manifest.permission.READ_MEDIA_VIDEO }
            userSelected?.status is PermissionStatus.Granted &&
                    (fullImages?.status !is PermissionStatus.Granted || fullVideos?.status !is PermissionStatus.Granted)
        } else {
            false
        }
    }

    val shouldShowRationale = permissionsState.shouldShowRationale

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
                    IconButton(
                        onClick = { viewModel.triggerAiScan(context) },
                        modifier = Modifier.testTag("run_full_ai_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Scan",
                            tint = AmberAccent
                        )
                    }
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.testTag("search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchActive) CyanAccent else TextPrimaryDark
                        )
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
                        onClick = { viewModel.triggerGalleryScan(context) },
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
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Expanded Search & Filter Controls
            AnimatedVisibility(visible = isSearchActive) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .padding(bottom = 8.dp)
                ) {
                    // Search Bar Row with Filter Drawer Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchFilterState.query,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text(stringResource(R.string.search_placeholder), color = TextMutedDark, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CyanAccent)
                            },
                            trailingIcon = {
                                if (searchFilterState.query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = stringResource(R.string.clear), tint = TextMutedDark)
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_text_input"),
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

                        Spacer(modifier = Modifier.width(8.dp))

                        // Advanced Filter Tune Button with Badge
                        IconButton(
                            onClick = { isFilterPanelExpanded = !isFilterPanelExpanded },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isFilterPanelExpanded || searchFilterState.hasActiveFilters) CyanAccent.copy(alpha = 0.2f) else SurfaceCard)
                                .testTag("toggle_advanced_filter_button")
                        ) {
                            if (searchFilterState.activeFilterCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = AmberAccent, contentColor = ObsidianBg) {
                                            Text(searchFilterState.activeFilterCount.toString(), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = stringResource(R.string.filter_media_collection),
                                        tint = CyanAccent
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = stringResource(R.string.filter_media_collection),
                                    tint = if (isFilterPanelExpanded) CyanAccent else TextPrimaryDark
                                )
                            }
                        }
                    }

                    // Active Filters Bar
                    if (searchFilterState.hasActiveFilters) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().testTag("active_filter_chips_row")
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(AmberAccent.copy(alpha = 0.2f))
                                        .clickable { viewModel.clearAllFilters() }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.clear_all_filters), color = AmberAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (searchFilterState.query.isNotBlank()) {
                                item {
                                    ActiveFilterChip(label = stringResource(R.string.search_label, searchFilterState.query)) {
                                        viewModel.updateSearchQuery("")
                                    }
                                }
                            }

                            searchFilterState.cameraModel?.let { model ->
                                item {
                                    ActiveFilterChip(label = stringResource(R.string.camera_label, model)) {
                                        viewModel.setCameraModelFilter(null)
                                    }
                                }
                            }

                            searchFilterState.cameraMake?.let { make ->
                                item {
                                    ActiveFilterChip(label = stringResource(R.string.make_label, make)) {
                                        viewModel.setCameraMakeFilter(null)
                                    }
                                }
                            }

                            searchFilterState.mlCategory?.let { category ->
                                item {
                                    ActiveFilterChip(label = stringResource(R.string.category_label, translateMlCategoryOrLabel(category))) {
                                        viewModel.setMlCategoryFilter(null)
                                    }
                                }
                            }

                            searchFilterState.mlLabel?.let { tag ->
                                item {
                                    ActiveFilterChip(label = stringResource(R.string.ml_tag_label, translateMlCategoryOrLabel(tag))) {
                                        viewModel.setMlLabelFilter(null)
                                    }
                                }
                            }

                            if (searchFilterState.isGpsOnly) {
                                item {
                                    ActiveFilterChip(label = stringResource(R.string.gps_location_chip)) {
                                        viewModel.toggleGpsOnlyFilter()
                                    }
                                }
                            }
                        }
                    }

                    // Quick Tag Suggestions Bar
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().testTag("quick_tag_suggestions_row")
                    ) {
                        item {
                            QuickTagChip(
                                label = "📍 GPS",
                                isSelected = searchFilterState.isGpsOnly,
                                onClick = { viewModel.toggleGpsOnlyFilter() }
                            )
                        }

                        // Default / Discovered Categories
                        val popularCategories = listOf("Animal", "Vehicle", "Food", "Nature", "Object", "Person", "Fish")
                        popularCategories.forEach { cat ->
                            item {
                                QuickTagChip(
                                    label = "🏷️ ${translateMlCategoryOrLabel(cat)}",
                                    isSelected = searchFilterState.mlCategory == cat,
                                    onClick = { viewModel.setMlCategoryFilter(cat) }
                                )
                            }
                        }

                        // Popular TFLite ML labels if present
                        filterOptions.mlLabels.take(10).forEach { tag ->
                            item {
                                QuickTagChip(
                                    label = "🔍 ${translateMlCategoryOrLabel(tag)}",
                                    isSelected = searchFilterState.mlLabel == tag,
                                    onClick = { viewModel.setMlLabelFilter(tag) }
                                )
                            }
                        }
                    }

                    // Expandable Advanced Filter Panel Drawer
                    AnimatedVisibility(
                        visible = isFilterPanelExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("advanced_filter_panel")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = stringResource(R.string.filter_media_collection),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CyanAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                // EXIF Camera Models Section
                                if (filterOptions.cameraModels.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.camera_models_exif), color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        filterOptions.cameraModels.forEach { model ->
                                            FilterChip(
                                                selected = searchFilterState.cameraModel == model,
                                                onClick = { viewModel.setCameraModelFilter(model) },
                                                label = { Text(model, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = AmberAccent,
                                                    selectedLabelColor = ObsidianBg,
                                                    containerColor = SurfaceDark,
                                                    labelColor = TextPrimaryDark
                                                ),
                                                border = null,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                // TFLite ML Categories Section
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Category, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.tflite_ml_categories), color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val availableCats = (filterOptions.mlCategories + listOf("Animal", "Vehicle", "Food", "Nature", "Object", "Fish")).distinct()
                                    availableCats.forEach { category ->
                                        FilterChip(
                                            selected = searchFilterState.mlCategory == category,
                                            onClick = { viewModel.setMlCategoryFilter(category) },
                                            label = { Text(translateMlCategoryOrLabel(category), fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CyanAccent,
                                                selectedLabelColor = ObsidianBg,
                                                containerColor = SurfaceDark,
                                                labelColor = TextPrimaryDark
                                            ),
                                            border = null,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // GPS Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.gps_only), color = TextPrimaryDark, fontSize = 12.sp)
                                    }
                                    Switch(
                                        checked = searchFilterState.isGpsOnly,
                                        onCheckedChange = { viewModel.toggleGpsOnlyFilter() },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = ObsidianBg,
                                            checkedTrackColor = CyanAccent,
                                            uncheckedThumbColor = TextMutedDark,
                                            uncheckedTrackColor = SurfaceDark
                                        ),
                                        modifier = Modifier.testTag("gps_only_switch")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Filter Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaFilterTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        label = {
                            Text(
                                text = when (tab) {
                                    MediaFilterTab.ALL -> if (searchFilterState.hasActiveFilters) "${stringResource(R.string.tab_results)} (${mediaItems.size})" else "${stringResource(R.string.tab_all)} (${mediaItems.size})"
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

            // Indexing Status Banner
            if (indexingProgress.status == IndexingStatus.SCANNING ||
                indexingProgress.status == IndexingStatus.GENERATING_THUMBNAILS ||
                indexingProgress.status == IndexingStatus.INDEXING_EXIF
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = indexingProgress.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (indexingProgress.totalCount > 0) {
                            Text(
                                text = "${indexingProgress.scannedCount}/${indexingProgress.totalCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimaryDark
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = CyanAccent,
                        trackColor = SurfaceCard
                    )
                }
            }

            // Active Filter Result Count Banner
            if (searchFilterState.hasActiveFilters) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.found_matching_items, mediaItems.size),
                        fontSize = 12.sp,
                        color = CyanAccent,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.clear),
                        fontSize = 12.sp,
                        color = AmberAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.clearAllFilters() }
                    )
                }
            }

            if (!isMediaPermissionGranted) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(16.dp).testTag("permission_request_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(CyanAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.permission_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    stringResource(R.string.permission_desc_tiramisu)
                                } else {
                                    stringResource(R.string.permission_desc_legacy)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMutedDark,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { permissionsState.launchMultiplePermissionRequest() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("grant_permission_button")
                                ) {
                                    Text(
                                        text = if (shouldShowRationale) stringResource(R.string.request_permission) else stringResource(R.string.grant_permission),
                                        color = ObsidianBg,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { openAppSettings(context) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                                    modifier = Modifier.testTag("open_settings_button")
                                ) {
                                    Text(stringResource(R.string.app_settings), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                if (isPartialPermissionGranted) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AmberAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.limited_access_notice),
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberAccent,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.manage_access),
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { permissionsState.launchMultiplePermissionRequest() }
                        )
                    }
                }

                if (indexingProgress.status == com.omex.gallery.domain.model.IndexingStatus.INDEXING_EXIF ||
                    indexingProgress.status == com.omex.gallery.domain.model.IndexingStatus.SCANNING ||
                    indexingProgress.status == com.omex.gallery.domain.model.IndexingStatus.GENERATING_THUMBNAILS) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCard)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = CyanAccent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = indexingProgress.message.ifEmpty { stringResource(R.string.indexing_phase_1) },
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimaryDark,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = stringResource(R.string.index_status),
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onOpenIndexingStatus() }
                        )
                    }
                }

                when (selectedTab) {
                MediaFilterTab.PEOPLE -> PeopleView(personGroups = personGroups, onMediaClick = onMediaClick)
                MediaFilterTab.DUPLICATES -> DuplicatesView(duplicateGroups = duplicateGroups, onMediaClick = onMediaClick)
                else -> {
                    val isGridEmpty = if (searchFilterState.hasActiveFilters) mediaItems.isEmpty() else pagedItems.itemCount == 0
                    if (isGridEmpty) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceCard),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (searchFilterState.hasActiveFilters) stringResource(R.string.no_matches_filters) else "أصبحت مكتبتك فارغة",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = TextPrimaryDark,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchFilterState.hasActiveFilters) "جرب تغيير كلمات البحث أو مسح الفلاتر" else "لم نجد أي صور أو فيديوهات في التخزين. اضغط للبحث عن وسائط جديدة.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMutedDark,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                if (searchFilterState.hasActiveFilters) {
                                    Button(
                                        onClick = { viewModel.clearAllFilters() },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(stringResource(R.string.clear_filters), color = ObsidianBg, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.triggerGalleryScan(context) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("إعادة فحص الوسائط", color = ObsidianBg, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(1.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.fillMaxSize().testTag("gallery_media_grid")
                        ) {
                            if (searchFilterState.hasActiveFilters) {
                                items(
                                    items = mediaItems,
                                    key = { it.id },
                                    contentType = { "media_item" }
                                ) { item ->
                                    val onItemClick = remember(item.id, onMediaClick) { { onMediaClick(item.id) } }
                                    val onFavClick = remember(item.id, viewModel) { { viewModel.toggleFavorite(item) } }
                                    val onSelectClick = remember(item.id, viewModel) { { viewModel.toggleSelection(item.id) } }
                                    MediaGridItemCell(
                                        item = item,
                                        onClick = onItemClick,
                                        onFavoriteClick = onFavClick,
                                        isSelected = selectedItemIds.contains(item.id),
                                        onSelectClick = onSelectClick,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                    )
                                }
                            } else {
                                items(
                                    count = pagedItems.itemCount,
                                    key = pagedItems.itemKey { it.id },
                                    contentType = pagedItems.itemContentType { "media_item" }
                                ) { index ->
                                    val item = pagedItems[index]
                                    if (item != null) {
                                        val onItemClick = remember(item.id, onMediaClick) { { onMediaClick(item.id) } }
                                        val onFavClick = remember(item.id, viewModel) { { viewModel.toggleFavorite(item) } }
                                        val onSelectClick = remember(item.id, viewModel) { { viewModel.toggleSelection(item.id) } }
                                        MediaGridItemCell(
                                            item = item,
                                            onClick = onItemClick,
                                            onFavoriteClick = onFavClick,
                                            isSelected = selectedItemIds.contains(item.id),
                                            onSelectClick = onSelectClick,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(SurfaceCard)
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
}
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
                            Icon(imageVector = Icons.Default.CopyAll, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(20.dp))
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

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%d:%02d", min, sec)
}

private fun openAppSettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
