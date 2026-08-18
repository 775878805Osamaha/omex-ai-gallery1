package com.omex.gallery.ui.feature_search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.omex.gallery.R
import com.omex.gallery.core.search.SmartSearchHelper
import com.omex.gallery.ui.feature_gallery.components.ActiveFiltersRow
import com.omex.gallery.ui.feature_gallery.components.AdvancedFilterBottomSheet
import com.omex.gallery.ui.theme.AmberAccent
import com.omex.gallery.ui.theme.ObsidianBg

private data class CategoryChipItem(
    val id: String,
    val labelAr: String,
    val iconEmoji: String
)

private val smartCategoryChips = listOf(
    CategoryChipItem("TRADING", "تداول", "📈"),
    CategoryChipItem("PRODUCT", "منتجات", "🛍️"),
    CategoryChipItem("DOCUMENT", "مستندات", "📄"),
    CategoryChipItem("CAR", "سيارات", "🚗"),
    CategoryChipItem("FOOD", "طعام", "🍔"),
    CategoryChipItem("NATURE", "طبيعة", "🏔️"),
    CategoryChipItem("TRAVEL", "سفر", "✈️"),
    CategoryChipItem("PERSON", "أشخاص", "👤"),
    CategoryChipItem("WORK", "عمل", "💼"),
    CategoryChipItem("SCREENSHOT", "لقطات شاشة", "📱")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onMediaClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedMediaTab by viewModel.selectedMediaTab.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchFilterState by viewModel.searchFilterState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val recentQueries by viewModel.recentQueries.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    var showAdvancedFiltersBottomSheet by remember { mutableStateOf(false) }

    val hasActiveFilters = query.isNotBlank() ||
            selectedMediaTab != SearchMediaTab.ALL ||
            selectedCategory != null ||
            searchFilterState.hasActiveFilters

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.search_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("search_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // Open Advanced Filters BottomSheet Button with Badge
                    IconButton(
                        onClick = { showAdvancedFiltersBottomSheet = true },
                        modifier = Modifier.testTag("search_open_filters_button")
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
                                tint = if (searchFilterState.hasActiveFilters) AmberAccent else Color.White
                            )
                        }
                    }

                    if (hasActiveFilters) {
                        IconButton(
                            onClick = {
                                viewModel.onQueryChange("")
                                viewModel.clearFilters()
                            },
                            modifier = Modifier.testTag("clear_all_search_filters_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterAltOff,
                                contentDescription = stringResource(R.string.clear_filters),
                                tint = AmberAccent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBg,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = ObsidianBg,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar Input
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder = {
                    Text(
                        stringResource(R.string.search_placeholder_text_ocr),
                        color = Color.Gray,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = AmberAccent
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onQueryChange("") },
                            modifier = Modifier.testTag("clear_search_input_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear),
                                tint = Color.Gray
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberAccent,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedContainerColor = Color(0xFF1E1E24),
                    unfocusedContainerColor = Color(0xFF18181C),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input_field")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Media Type Filter Row (All, Photos, Videos, Favorites)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedMediaTab == SearchMediaTab.ALL,
                    onClick = { viewModel.onMediaTabSelect(SearchMediaTab.ALL) },
                    label = { Text("الكل", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberAccent,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF1E1E24),
                        labelColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("filter_tab_all")
                )
                FilterChip(
                    selected = selectedMediaTab == SearchMediaTab.PHOTOS,
                    onClick = { viewModel.onMediaTabSelect(SearchMediaTab.PHOTOS) },
                    label = { Text("📷 صور", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberAccent,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF1E1E24),
                        labelColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("filter_tab_photos")
                )
                FilterChip(
                    selected = selectedMediaTab == SearchMediaTab.VIDEOS,
                    onClick = { viewModel.onMediaTabSelect(SearchMediaTab.VIDEOS) },
                    label = { Text("🎬 فيديوهات", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberAccent,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF1E1E24),
                        labelColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("filter_tab_videos")
                )
                FilterChip(
                    selected = selectedMediaTab == SearchMediaTab.FAVORITES,
                    onClick = { viewModel.onMediaTabSelect(SearchMediaTab.FAVORITES) },
                    label = { Text("⭐ المفضلة", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberAccent,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF1E1E24),
                        labelColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("filter_tab_favorites")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // AI Smart Categories Horizontal Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                smartCategoryChips.forEach { cat ->
                    val isSelected = selectedCategory == cat.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onCategorySelect(cat.id) },
                        label = {
                            Text(
                                text = "${cat.iconEmoji} ${cat.labelAr}",
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD97706),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF24242C),
                            labelColor = Color(0xFFCCCCCC)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("filter_category_${cat.id}")
                    )
                }
            }

            // Active Filters Row (Phase 2)
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
                onClearAll = { viewModel.clearFilters() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Recent Searches section when query is empty and no category is selected
            AnimatedVisibility(visible = !hasActiveFilters && recentQueries.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.recent_searches_header),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        TextButton(
                            onClick = { viewModel.clearSearchHistory() },
                            modifier = Modifier.testTag("clear_all_history_button")
                        ) {
                            Text(
                                text = stringResource(R.string.clear_all),
                                color = AmberAccent,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        recentQueries.forEach { recentItem ->
                            InputChip(
                                selected = false,
                                onClick = { viewModel.submitSearch(recentItem) },
                                label = { Text(recentItem, color = Color.White, fontSize = 13.sp) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { viewModel.removeRecentQuery(recentItem) }
                                    )
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor = Color(0xFF2A2A32)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.testTag("recent_query_chip_$recentItem")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Results count header when search is active
            if (hasActiveFilters) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.search_results_count, searchResults.size),
                        color = AmberAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    if (selectedCategory != null) {
                        Text(
                            text = "#${SmartSearchHelper.getCategoryNameArabic(selectedCategory!!)}",
                            color = Color(0xFFFBBF24),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Empty state when search produces no results
            if (hasActiveFilters && searchResults.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (query.isNotBlank()) stringResource(R.string.no_ocr_results_found, query)
                            else stringResource(R.string.no_matches_filters),
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.search_empty_hint),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else if (!hasActiveFilters && recentQueries.isEmpty()) {
                // Initial prompt when screen opens
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.DarkGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ابحث بالاسم، OCR، الكاميرا، أو اختر فئة ذكية",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "يدعم البحث الفوري عن التداول، المنتجات، المستندات، والسيارات",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                // Search Results Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults, key = { it.mediaItem.id }) { itemWithOcr ->
                        SearchResultCard(
                            result = itemWithOcr,
                            query = query,
                            onClick = { onMediaClick(itemWithOcr.mediaItem.id) }
                        )
                    }
                }
            }
        }

        if (showAdvancedFiltersBottomSheet) {
            AdvancedFilterBottomSheet(
                currentFilterState = searchFilterState,
                categories = categories,
                matchingCount = searchResults.size,
                onApplyFilters = { newState ->
                    viewModel.setFilterState(newState)
                },
                onDismiss = { showAdvancedFiltersBottomSheet = false }
            )
        }
    }
}

@Composable
private fun SearchResultCard(
    result: SearchResultWithOcr,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val media = result.mediaItem
    val ocrText = result.ocrText?.extractedText.orEmpty()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("search_result_card_${media.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFF2A2A32))
            ) {
                AsyncImage(
                    model = media.thumbnailPath ?: media.uriString,
                    contentDescription = media.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Favorite Icon badge
                if (media.isFavorite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Video Badge
                if (media.isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "فيديو",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // OCR Badge
                if (ocrText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "OCR",
                                color = AmberAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = media.fileName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Matched Categories or Camera Model
                if (result.matchedCategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = result.matchedCategories.joinToString(" ") { "#${SmartSearchHelper.getCategoryNameArabic(it)}" },
                        color = Color(0xFFFBBF24),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (!media.cameraModel.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📷 ${media.cameraModel}",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (ocrText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatOcrSnippet(ocrText, query),
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun formatOcrSnippet(text: String, query: String): String {
    if (query.isBlank()) return text.take(60)
    val index = text.indexOf(query, ignoreCase = true)
    return if (index >= 0) {
        val start = (index - 15).coerceAtLeast(0)
        val end = (index + query.length + 30).coerceAtMost(text.length)
        "..." + text.substring(start, end).replace("\n", " ") + "..."
    } else {
        text.take(60).replace("\n", " ")
    }
}
