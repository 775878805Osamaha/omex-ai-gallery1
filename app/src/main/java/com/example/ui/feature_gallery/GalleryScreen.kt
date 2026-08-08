package com.example.ui.feature_gallery

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.model.DuplicateGroupWithMedia
import com.example.domain.model.IndexingStatus
import com.example.domain.model.MediaItem
import com.example.domain.model.PersonGroup
import com.example.domain.model.SearchFilterOptions
import com.example.domain.model.SearchFilterState
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onMediaClick: (Long) -> Unit,
    onOpenIndexingStatus: () -> Unit
) {
    val context = LocalContext.current
    val mediaItems by viewModel.mediaItems.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchFilterState by viewModel.searchFilterState.collectAsStateWithLifecycle()
    val filterOptions by viewModel.filterOptions.collectAsStateWithLifecycle()
    val indexingProgress by viewModel.indexingProgress.collectAsStateWithLifecycle()
    val personGroups by viewModel.personGroups.collectAsStateWithLifecycle()
    val duplicateGroups by viewModel.duplicateGroups.collectAsStateWithLifecycle()

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

    LaunchedEffect(isMediaPermissionGranted) {
        if (isMediaPermissionGranted) {
            viewModel.triggerGalleryScan()
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
                                text = "OMEX AI Gallery",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "EXIF & TFLite ML Search",
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
                        onClick = { viewModel.triggerGalleryScan() },
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
                            placeholder = { Text("Search EXIF tags, ML labels, or file names...", color = TextMutedDark, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CyanAccent)
                            },
                            trailingIcon = {
                                if (searchFilterState.query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextMutedDark)
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
                                        contentDescription = "Filter Options",
                                        tint = CyanAccent
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Filter Options",
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
                                        Text("Clear All Filters", color = AmberAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (searchFilterState.query.isNotBlank()) {
                                item {
                                    ActiveFilterChip(label = "Search: \"${searchFilterState.query}\"") {
                                        viewModel.updateSearchQuery("")
                                    }
                                }
                            }

                            searchFilterState.cameraModel?.let { model ->
                                item {
                                    ActiveFilterChip(label = "Camera: $model") {
                                        viewModel.setCameraModelFilter(null)
                                    }
                                }
                            }

                            searchFilterState.cameraMake?.let { make ->
                                item {
                                    ActiveFilterChip(label = "Make: $make") {
                                        viewModel.setCameraMakeFilter(null)
                                    }
                                }
                            }

                            searchFilterState.mlCategory?.let { category ->
                                item {
                                    ActiveFilterChip(label = "Category: $category") {
                                        viewModel.setMlCategoryFilter(null)
                                    }
                                }
                            }

                            searchFilterState.mlLabel?.let { tag ->
                                item {
                                    ActiveFilterChip(label = "ML Tag: $tag") {
                                        viewModel.setMlLabelFilter(null)
                                    }
                                }
                            }

                            if (searchFilterState.isGpsOnly) {
                                item {
                                    ActiveFilterChip(label = "📍 GPS Location") {
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
                                label = "📍 GPS Tagged",
                                isSelected = searchFilterState.isGpsOnly,
                                onClick = { viewModel.toggleGpsOnlyFilter() }
                            )
                        }

                        // Default / Discovered Categories
                        val popularCategories = listOf("Animal", "Vehicle", "Food", "Nature", "Object", "Person", "Fish")
                        popularCategories.forEach { cat ->
                            item {
                                QuickTagChip(
                                    label = "🏷️ $cat",
                                    isSelected = searchFilterState.mlCategory == cat,
                                    onClick = { viewModel.setMlCategoryFilter(cat) }
                                )
                            }
                        }

                        // Popular TFLite ML labels if present
                        filterOptions.mlLabels.take(10).forEach { tag ->
                            item {
                                QuickTagChip(
                                    label = "🔍 $tag",
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
                                    text = "Filter Media Collection",
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
                                        Text("Camera Models (EXIF)", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                    Text("TFLite ML Categories", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                            label = { Text(category, fontSize = 11.sp) },
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
                                        Text("Only Photos with GPS Coordinates", color = TextPrimaryDark, fontSize = 12.sp)
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
                                    MediaFilterTab.ALL -> if (searchFilterState.hasActiveFilters) "Results (${mediaItems.size})" else "All (${mediaItems.size})"
                                    MediaFilterTab.PHOTOS -> "Photos"
                                    MediaFilterTab.VIDEOS -> "Videos"
                                    MediaFilterTab.FAVORITES -> "Favorites"
                                    MediaFilterTab.PEOPLE -> "People (${personGroups.size})"
                                    MediaFilterTab.DUPLICATES -> "Duplicates (${duplicateGroups.size})"
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
                        text = "Found ${mediaItems.size} item(s) matching filters",
                        fontSize = 12.sp,
                        color = CyanAccent,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Clear",
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
                                text = "Media Access Permission Required",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    "OMEX AI Gallery requires access to your photos and videos to scan, organize, and classify your media collection offline."
                                } else {
                                    "OMEX AI Gallery requires storage access to scan and organize photos and videos on your device."
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
                                        text = if (shouldShowRationale) "Request Permission" else "Grant Permission",
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
                                    Text("App Settings", fontWeight = FontWeight.Bold)
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
                            text = "Limited photo access granted (Android 14+)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberAccent,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Manage Access",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { permissionsState.launchMultiplePermissionRequest() }
                        )
                    }
                }

                when (selectedTab) {
                MediaFilterTab.PEOPLE -> PeopleView(personGroups = personGroups, onMediaClick = onMediaClick)
                MediaFilterTab.DUPLICATES -> DuplicatesView(duplicateGroups = duplicateGroups, onMediaClick = onMediaClick)
                else -> {
                    if (mediaItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchFilterState.hasActiveFilters) "No media matches the selected filters" else "No media files found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextMutedDark
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (searchFilterState.hasActiveFilters) {
                                    Button(onClick = { viewModel.clearAllFilters() }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard)) {
                                        Text("Clear Filters", color = CyanAccent)
                                    }
                                } else {
                                    Button(onClick = { viewModel.triggerGalleryScan() }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard)) {
                                        Text("Scan MediaStore", color = CyanAccent)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 110.dp),
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize().testTag("gallery_media_grid")
                        ) {
                            items(items = mediaItems, key = { it.id }) { item ->
                                MediaGridItemCell(
                                    item = item,
                                    onClick = { onMediaClick(item.id) },
                                    onFavoriteClick = { viewModel.toggleFavorite(item) }
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
            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = CyanAccent, modifier = Modifier.size(12.dp))
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
            Text("No face clusters detected yet. Run AI scan above.", color = TextMutedDark)
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
                        Text(group.personName, color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${group.faceCount} photos", color = TextMutedDark, fontSize = 12.sp)
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
            Text("No duplicate photo groups found.", color = TextMutedDark)
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
                            Text("Group ${group.groupId.take(12)} (${group.groupType})", color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
    onFavoriteClick: () -> Unit
) {
    val context = LocalContext.current
    val imageModel = remember(item.thumbnailPath, item.uriString) {
        if (!item.thumbnailPath.isNullOrEmpty() && File(item.thumbnailPath).exists()) {
            File(item.thumbnailPath)
        } else {
            item.uriString
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .clickable { onClick() }
            .testTag("media_item_${item.id}")
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageModel)
                .crossfade(true)
                .build(),
            contentDescription = item.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Video Badge
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(formatDuration(item.durationMs), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.White)
                }
            }
        }

        // GPS Badge if coordinates exist
        if (item.latitude != null && item.longitude != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "GPS",
                    tint = CyanAccent,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        // Resolution Badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text("${item.width}x${item.height}", color = Color.White, fontSize = 8.sp)
        }

        // Favorite Button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onFavoriteClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (item.isFavorite) AmberAccent else Color.White,
                modifier = Modifier.size(16.dp)
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
