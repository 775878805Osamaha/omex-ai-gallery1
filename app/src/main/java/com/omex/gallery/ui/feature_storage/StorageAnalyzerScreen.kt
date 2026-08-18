package com.omex.gallery.ui.feature_storage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.omex.gallery.R
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.ui.theme.AmberAccent
import com.omex.gallery.ui.theme.CyanAccent
import com.omex.gallery.ui.theme.ErrorRed
import com.omex.gallery.ui.theme.NeonPurple
import com.omex.gallery.ui.theme.ObsidianBg
import com.omex.gallery.ui.theme.SuccessGreen
import com.omex.gallery.ui.theme.SurfaceCard
import com.omex.gallery.ui.theme.SurfaceDark
import com.omex.gallery.ui.theme.SurfaceVariantDark
import com.omex.gallery.ui.theme.TextMutedDark
import com.omex.gallery.ui.theme.TextPrimaryDark
import com.omex.gallery.ui.theme.TextSecondaryDark
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StorageAnalyzerScreen(
    viewModel: StorageAnalyzerViewModel,
    onBackClick: () -> Unit,
    onMediaClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var itemToDeleteSingle by remember { mutableStateOf<MediaItem?>(null) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showPrivacyReport by remember { mutableStateOf(false) }
    var showDuplicateCleaner by remember { mutableStateOf(false) }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    if (showDuplicateCleaner) {
        DuplicateCleanerScreen(
            viewModel = viewModel,
            onBackClick = { showDuplicateCleaner = false },
            onMediaClick = onMediaClick,
            modifier = modifier
        )
        return
    }

    if (showPrivacyReport) {
        PrivacyReportView(
            onDismiss = { showPrivacyReport = false },
            modifier = modifier
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.storage_analyzer_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = stringResource(R.string.storage_analyzer_subtitle),
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("storage_analyzer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = CyanAccent
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDuplicateCleaner = true },
                        modifier = Modifier.testTag("storage_duplicate_cleaner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = stringResource(R.string.duplicate_finder_title),
                            tint = AmberAccent
                        )
                    }
                    IconButton(
                        onClick = { showPrivacyReport = true },
                        modifier = Modifier.testTag("storage_privacy_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = stringResource(R.string.privacy_report_button),
                            tint = SuccessGreen
                        )
                    }
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.testTag("storage_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = AmberAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBg,
                    titleContentColor = TextPrimaryDark
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ObsidianBg,
        modifier = modifier.testTag("storage_analyzer_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
            ) {
                // Storage Overview Hero Card
                item {
                    StorageSummaryHeroCard(
                        totalSizeFormatted = state.formattedTotalSize,
                        totalCount = state.totalCount,
                        photosSize = StorageAnalyzerViewModel.formatBytes(state.photosSizeBytes),
                        photosCount = state.photosCount,
                        videosSize = StorageAnalyzerViewModel.formatBytes(state.videosSizeBytes),
                        videosCount = state.videosCount,
                        duplicatesSize = StorageAnalyzerViewModel.formatBytes(state.duplicatesSizeBytes),
                        duplicatesCount = state.duplicatesCount,
                        reclaimableSize = state.formattedReclaimableSize,
                        photosFraction = if (state.totalSizeBytes > 0) state.photosSizeBytes.toFloat() / state.totalSizeBytes else 0f,
                        videosFraction = if (state.totalSizeBytes > 0) state.videosSizeBytes.toFloat() / state.totalSizeBytes else 0f,
                        duplicatesFraction = if (state.totalSizeBytes > 0) state.duplicatesSizeBytes.toFloat() / state.totalSizeBytes else 0f
                    )
                }

                // Privacy & Offline AI Transparency Banner
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SuccessGreen.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .clickable { showPrivacyReport = true }
                            .testTag("storage_privacy_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.privacy_report_title),
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = stringResource(R.string.privacy_report_subtitle),
                                        fontSize = 10.sp,
                                        color = SuccessGreen,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SuccessGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "100% OFFLINE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Duplicate Media Detection & Cleanup Quick Action Banner
                if (state.duplicatesCount > 0) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AmberAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .clickable { showDuplicateCleaner = true }
                                .testTag("storage_duplicate_action_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(AmberAccent.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CleaningServices,
                                            contentDescription = null,
                                            tint = AmberAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = stringResource(R.string.duplicate_finder_title),
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${state.duplicatesCount} ملف مكرر • وفّر ${StorageAnalyzerViewModel.formatBytes(state.duplicatesSizeBytes)}",
                                            fontSize = 10.sp,
                                            color = AmberAccent,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AmberAccent.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.storage_reclaim_space),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // D3 Chart Grouping & Metric Mode Selectors
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.storage_chart_title),
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontSize = 15.sp
                        )

                        // Mode Selector Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(StorageGroupingMode.entries) { mode ->
                                val isSelected = state.selectedGroupingMode == mode
                                val label = when (mode) {
                                    StorageGroupingMode.BY_FILE_TYPE -> stringResource(R.string.storage_group_format)
                                    StorageGroupingMode.BY_CLASSIFICATION -> stringResource(R.string.storage_group_category)
                                    StorageGroupingMode.BY_MEDIA_KIND -> stringResource(R.string.storage_group_media_type)
                                    StorageGroupingMode.BY_SIZE -> stringResource(R.string.storage_group_size)
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setGroupingMode(mode) },
                                    label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (mode) {
                                                StorageGroupingMode.BY_FILE_TYPE -> Icons.Default.Category
                                                StorageGroupingMode.BY_CLASSIFICATION -> Icons.Default.AutoAwesome
                                                StorageGroupingMode.BY_MEDIA_KIND -> Icons.Default.PieChart
                                                StorageGroupingMode.BY_SIZE -> Icons.Default.CleaningServices
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanAccent,
                                        selectedLabelColor = ObsidianBg,
                                        selectedLeadingIconColor = ObsidianBg,
                                        containerColor = SurfaceCard,
                                        labelColor = TextPrimaryDark,
                                        iconColor = CyanAccent
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    border = null,
                                    modifier = Modifier.testTag("grouping_mode_${mode.name}")
                                )
                            }
                        }

                        // Metric Selector (Size vs Count)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.storage_chart_interactive_hint),
                                color = TextMutedDark,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val isSize = state.selectedMetricMode == StorageMetricMode.SIZE
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSize) AmberAccent else SurfaceDark,
                                    modifier = Modifier
                                        .clickable { viewModel.setMetricMode(StorageMetricMode.SIZE) }
                                        .testTag("metric_size_btn")
                                ) {
                                    Text(
                                        text = stringResource(R.string.storage_metric_size),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSize) ObsidianBg else TextSecondaryDark,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                val isCount = state.selectedMetricMode == StorageMetricMode.COUNT
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCount) AmberAccent else SurfaceDark,
                                    modifier = Modifier
                                        .clickable { viewModel.setMetricMode(StorageMetricMode.COUNT) }
                                        .testTag("metric_count_btn")
                                ) {
                                    Text(
                                        text = stringResource(R.string.storage_metric_count),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCount) ObsidianBg else TextSecondaryDark,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // D3 Interactive Chart Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("d3_chart_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val d3Json = remember(state.distributionItems, state.selectedMetricMode, state.formattedTotalSize, state.totalCount) {
                                viewModel.buildD3JsonString(state)
                            }

                            D3StorageChart(
                                jsonData = d3Json,
                                groupingMode = state.selectedGroupingMode,
                                metricMode = state.selectedMetricMode,
                                selectedSliceKey = state.selectedSliceKey,
                                onSliceSelected = { key, label, _, _ ->
                                    viewModel.selectSlice(key, label)
                                }
                            )

                            // Active Filter Banner
                            if (state.selectedSliceKey != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "المحدد: ${state.selectedSliceLabel ?: state.selectedSliceKey}",
                                        color = CyanAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { viewModel.clearSliceSelection() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = CyanAccent, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Storage Breakdown Distribution List
                item {
                    Text(
                        text = stringResource(R.string.storage_distribution_breakdown),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 15.sp
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.distributionItems.forEach { item ->
                            val isSelected = state.selectedSliceKey == item.key
                            StorageBreakdownTile(
                                item = item,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelected) viewModel.clearSliceSelection()
                                    else viewModel.selectSlice(item.key, item.label)
                                }
                            )
                        }
                    }
                }

                // Cleanup Recommendations & Quick Clean Cards
                if (state.quickCleanSuggestions.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = stringResource(R.string.storage_quick_cleanup_title),
                                fontWeight = FontWeight.Bold,
                                color = AmberAccent,
                                fontSize = 15.sp
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(state.quickCleanSuggestions) { sugg ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .width(220.dp)
                                            .border(1.dp, AmberAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .clickable { viewModel.setQuickFilter(sugg.filter) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = sugg.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = TextPrimaryDark
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = AmberAccent.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = sugg.formattedSize,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AmberAccent,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${sugg.count} ملفات • ${sugg.reason}",
                                                fontSize = 10.sp,
                                                color = TextSecondaryDark,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Cleanup Recommendations & Quick Filters
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.storage_quick_clean),
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontSize = 15.sp
                        )

                        // Quick Filter Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(StorageQuickFilter.entries) { filter ->
                                val isSelected = state.quickFilter == filter && state.selectedSliceKey == null
                                val label = when (filter) {
                                    StorageQuickFilter.ALL -> stringResource(R.string.storage_filter_all)
                                    StorageQuickFilter.LARGE_VIDEOS_50MB -> stringResource(R.string.storage_filter_large_videos)
                                    StorageQuickFilter.LARGE_VIDEOS_100MB -> stringResource(R.string.storage_filter_large_videos_100mb)
                                    StorageQuickFilter.LARGE_PHOTOS_5MB -> stringResource(R.string.storage_filter_large_photos)
                                    StorageQuickFilter.LARGE_FILES_5MB -> stringResource(R.string.storage_filter_large_files_5mb)
                                    StorageQuickFilter.LARGE_FILES_50MB -> stringResource(R.string.storage_filter_large_files_50mb)
                                    StorageQuickFilter.LARGE_FILES_100MB -> stringResource(R.string.storage_filter_large_files_100mb)
                                    StorageQuickFilter.DUPLICATES -> stringResource(R.string.storage_filter_duplicates)
                                    StorageQuickFilter.SCREENSHOTS -> stringResource(R.string.storage_filter_screenshots)
                                    StorageQuickFilter.LARGEST_FILES -> stringResource(R.string.storage_filter_largest_all)
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setQuickFilter(filter) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberAccent,
                                        selectedLabelColor = ObsidianBg,
                                        containerColor = SurfaceDark,
                                        labelColor = TextPrimaryDark
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    border = null,
                                    modifier = Modifier.testTag("quick_filter_${filter.name}")
                                )
                            }
                        }

                        // Sorting Mode Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.storage_sort_title),
                                color = TextMutedDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(StorageSortMode.entries) { sort ->
                                    val isSelected = state.selectedSortMode == sort
                                    val label = when (sort) {
                                        StorageSortMode.LARGEST_FIRST -> stringResource(R.string.storage_sort_largest)
                                        StorageSortMode.SMALLEST_FIRST -> stringResource(R.string.storage_sort_smallest)
                                        StorageSortMode.NEWEST_FIRST -> stringResource(R.string.storage_sort_newest)
                                        StorageSortMode.OLDEST_FIRST -> stringResource(R.string.storage_sort_oldest)
                                        StorageSortMode.BY_TYPE -> stringResource(R.string.storage_sort_type)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) CyanAccent else SurfaceDark,
                                        modifier = Modifier.clickable { viewModel.setSortMode(sort) }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) ObsidianBg else TextSecondaryDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Space Consuming Files Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stringResource(R.string.storage_largest_files)} (${state.filteredMediaItems.size})",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            fontSize = 15.sp
                        )
                        if (state.filteredMediaItems.isNotEmpty()) {
                            val allSelected = state.selectedItemIds.size >= state.filteredMediaItems.size
                            TextButton(
                                onClick = {
                                    if (allSelected) viewModel.clearSelection()
                                    else viewModel.selectAll(state.filteredMediaItems)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (allSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                                    fontSize = 12.sp,
                                    color = CyanAccent
                                )
                            }
                        }
                    }
                }

                // Media Items List
                if (state.filteredMediaItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.storage_no_items_filter),
                                color = TextMutedDark,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(state.filteredMediaItems, key = { it.id }) { item ->
                        val isSelected = state.selectedItemIds.contains(item.id)
                        SpaceConsumingItemCard(
                            item = item,
                            isSelected = isSelected,
                            onItemClick = { onMediaClick(item.id) },
                            onToggleSelection = { viewModel.toggleItemSelection(item.id) },
                            onDeleteClick = { itemToDeleteSingle = item }
                        )
                    }
                }
            }

            // Floating Batch Delete Toolbar
            AnimatedVisibility(
                visible = state.selectedItemIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val selectedItems = state.filteredMediaItems.filter { state.selectedItemIds.contains(it.id) }
                val freedBytes = selectedItems.sumOf { it.sizeBytes }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .testTag("storage_batch_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${state.selectedItemIds.size} ملفات محددة",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "تحرير: ${StorageAnalyzerViewModel.formatBytes(freedBytes)}",
                                color = CyanAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = TextMutedDark)
                            }
                            Button(
                                onClick = { showBatchDeleteDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("storage_batch_delete_btn")
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حذف وتحرير", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Single Item Delete Confirmation Dialog
            if (itemToDeleteSingle != null) {
                val target = itemToDeleteSingle!!
                AlertDialog(
                    onDismissRequest = { itemToDeleteSingle = null },
                    title = {
                        Text(
                            text = "حذف ملف الوسائط؟",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.storage_delete_single_confirm, StorageAnalyzerViewModel.formatBytes(target.sizeBytes)),
                            color = TextSecondaryDark,
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteSingleItem(target)
                                itemToDeleteSingle = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            modifier = Modifier.testTag("confirm_single_delete_btn")
                        ) {
                            Text(stringResource(R.string.delete_confirm), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { itemToDeleteSingle = null }) {
                            Text(stringResource(R.string.cancel), color = CyanAccent)
                        }
                    },
                    containerColor = SurfaceDark
                )
            }

            // Batch Delete Confirmation Dialog
            if (showBatchDeleteDialog) {
                val selectedItems = state.filteredMediaItems.filter { state.selectedItemIds.contains(it.id) }
                val freedBytes = selectedItems.sumOf { it.sizeBytes }

                AlertDialog(
                    onDismissRequest = { showBatchDeleteDialog = false },
                    title = {
                        Text(
                            text = stringResource(R.string.batch_delete_dialog_title, state.selectedItemIds.size),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.storage_batch_delete_confirm, state.selectedItemIds.size, StorageAnalyzerViewModel.formatBytes(freedBytes)),
                            color = TextSecondaryDark,
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteSelectedItems(selectedItems)
                                showBatchDeleteDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            modifier = Modifier.testTag("confirm_batch_storage_delete_btn")
                        ) {
                            Text(stringResource(R.string.delete_confirm), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBatchDeleteDialog = false }) {
                            Text(stringResource(R.string.cancel), color = CyanAccent)
                        }
                    },
                    containerColor = SurfaceDark
                )
            }

            // Info Dialog
            if (showInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showInfoDialog = false },
                    title = {
                        Text(
                            text = "حول محلل التخزين الذكي (D3 Analyzer)",
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "محلل التخزين يستخدم تقنية الرسوم البيانية التفاعلية D3.js لتحليل وتوزيع ملفات الوسائط وفق الحجم الفعلي والعدد.",
                                color = TextPrimaryDark,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "• انقر على أي قطاع في الرسم التفاعلي لتصفية الملفات التابعة له فوراً.\n• يمكنك التبديل بين التصنيف حسب الصيغة (MP4/JPG/PNG)، أو التصنيف الذكي (Screenshots/Documents/Nature)، أو نوع الوسائط.\n• يتيح لك تنظيف الملفات الكبيرة والمكررة مباشرة وتحرير الذاكرة بأمان.",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showInfoDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                        ) {
                            Text("حسناً", color = ObsidianBg, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = SurfaceDark
                )
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CyanAccent)
                }
            }
        }
    }
}

@Composable
fun StorageSummaryHeroCard(
    totalSizeFormatted: String,
    totalCount: Int,
    photosSize: String,
    photosCount: Int,
    videosSize: String,
    videosCount: Int,
    duplicatesSize: String,
    duplicatesCount: Int,
    reclaimableSize: String,
    photosFraction: Float,
    videosFraction: Float,
    duplicatesFraction: Float
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("storage_hero_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.storage_total_used),
                        fontSize = 12.sp,
                        color = TextMutedDark,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = totalSizeFormatted,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyanAccent
                    )
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = CyanAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "$totalCount عنصر",
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (duplicatesCount > 0) {
                        Text(
                            text = "استرداد: $reclaimableSize",
                            color = AmberAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Segmented Linear Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(SurfaceDark)
            ) {
                if (photosFraction > 0) {
                    Box(
                        modifier = Modifier
                            .weight(photosFraction.coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(AmberAccent)
                    )
                }
                if (videosFraction > 0) {
                    Box(
                        modifier = Modifier
                            .weight(videosFraction.coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(CyanAccent)
                    )
                }
                if (duplicatesFraction > 0) {
                    Box(
                        modifier = Modifier
                            .weight(duplicatesFraction.coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(ErrorRed)
                    )
                }
            }

            // Stats row with color legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Photos
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AmberAccent))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("صور ($photosCount)", fontSize = 11.sp, color = TextSecondaryDark)
                        Text(photosSize, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }
                }

                // Videos
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CyanAccent))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("فيديوهات ($videosCount)", fontSize = 11.sp, color = TextSecondaryDark)
                        Text(videosSize, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }
                }

                // Duplicates
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ErrorRed))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("تكرارات ($duplicatesCount)", fontSize = 11.sp, color = TextSecondaryDark)
                        Text(duplicatesSize, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                    }
                }
            }
        }
    }
}

@Composable
fun StorageBreakdownTile(
    item: StorageDistributionItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(item.colorHex))
    } catch (e: Exception) {
        CyanAccent
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.15f) else SurfaceDark
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) color else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .testTag("storage_tile_${item.key}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.label,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 13.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.formattedSize,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${(item.percentage * 100).toInt()}%)",
                        fontSize = 11.sp,
                        color = TextMutedDark
                    )
                }
            }

            // Small horizontal bar
            LinearProgressIndicator(
                progress = { item.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = SurfaceVariantDark
            )
        }
    }
}

@Composable
fun SpaceConsumingItemCard(
    item: MediaItem,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CyanAccent.copy(alpha = 0.12f) else SurfaceDark
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) CyanAccent else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onItemClick() }
            .testTag("space_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for selection
            IconButton(
                onClick = onToggleSelection,
                modifier = Modifier.size(32.dp).testTag("select_item_btn_${item.id}")
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                    contentDescription = "Select",
                    tint = if (isSelected) CyanAccent else TextMutedDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Thumbnail
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(if (item.thumbnailPath != null && File(item.thumbnailPath).exists()) File(item.thumbnailPath) else item.uriString)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (item.isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = AmberAccent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = StorageAnalyzerViewModel.formatBytes(item.sizeBytes),
                            color = AmberAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (item.isVideo) "فيديو" else "صورة",
                        color = TextMutedDark,
                        fontSize = 11.sp
                    )
                }
            }

            // Quick Delete Single File
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp).testTag("delete_item_btn_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextMutedDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
