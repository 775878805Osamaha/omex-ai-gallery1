package com.omex.gallery.ui.feature_gallery.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omex.gallery.R
import com.omex.gallery.core.data.local.MediaCategoryEntity
import com.omex.gallery.domain.model.DateFilterOption
import com.omex.gallery.domain.model.DimensionFilterOption
import com.omex.gallery.domain.model.FileSizeFilterOption
import com.omex.gallery.domain.model.SearchFilterState
import com.omex.gallery.ui.theme.AmberAccent
import com.omex.gallery.ui.theme.CyanAccent
import com.omex.gallery.ui.theme.ObsidianBg
import com.omex.gallery.ui.theme.SurfaceCard
import com.omex.gallery.ui.theme.TextPrimaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActiveFiltersRow(
    filterState: SearchFilterState,
    categories: List<MediaCategoryEntity>,
    onRemoveCategory: (String) -> Unit,
    onRemoveMediaType: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onRemoveDate: () -> Unit,
    onRemoveFileSize: () -> Unit,
    onRemoveExtension: (String) -> Unit,
    onRemoveDimension: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val categoryNameMap = remember(categories) {
        categories.associate { it.categoryId.uppercase() to it.nameArabic }
    }

    AnimatedVisibility(
        visible = filterState.hasActiveFilters,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clear All Chip/Button
            TextButton(
                onClick = onClearAll,
                modifier = Modifier.testTag("clear_all_active_filters_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterAltOff,
                    contentDescription = null,
                    tint = AmberAccent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "مسح الكل",
                    color = AmberAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Media Type Filter Chip
            if (filterState.isVideo != null) {
                ActiveChip(
                    label = if (filterState.isVideo == true) "فيديوهات" else "صور",
                    onRemove = onRemoveMediaType,
                    testTag = "active_filter_media_type"
                )
            }

            // Favorite State Chip
            if (filterState.isFavorite != null) {
                ActiveChip(
                    label = if (filterState.isFavorite == true) "المفضلة" else "غير المفضلة",
                    onRemove = onRemoveFavorite,
                    testTag = "active_filter_favorite"
                )
            }

            // Category Chips
            filterState.allSelectedCategories.forEach { catId ->
                val name = categoryNameMap[catId.uppercase()] ?: catId
                ActiveChip(
                    label = name,
                    onRemove = { onRemoveCategory(catId) },
                    testTag = "active_filter_cat_$catId"
                )
            }

            // Date Filter Chip
            if (filterState.dateFilterOption != DateFilterOption.ALL) {
                val dateLabel = when (filterState.dateFilterOption) {
                    DateFilterOption.TODAY -> "اليوم"
                    DateFilterOption.LAST_7_DAYS -> "آخر 7 أيام"
                    DateFilterOption.LAST_30_DAYS -> "آخر 30 يوم"
                    DateFilterOption.THIS_YEAR -> "هذا العام"
                    DateFilterOption.CUSTOM -> {
                        val from = filterState.startDateMs?.let { dateFormatter.format(Date(it)) } ?: "..."
                        val to = filterState.endDateMs?.let { dateFormatter.format(Date(it)) } ?: "..."
                        "$from - $to"
                    }
                    else -> ""
                }
                ActiveChip(
                    label = dateLabel,
                    onRemove = onRemoveDate,
                    testTag = "active_filter_date"
                )
            }

            // File Size Chip
            if (filterState.fileSizeOption != FileSizeFilterOption.ALL) {
                val sizeLabel = when (filterState.fileSizeOption) {
                    FileSizeFilterOption.LESS_THAN_1MB -> "< 1 MB"
                    FileSizeFilterOption.BETWEEN_1_5MB -> "1–5 MB"
                    FileSizeFilterOption.BETWEEN_5_50MB -> "5–50 MB"
                    FileSizeFilterOption.GREATER_THAN_50MB -> "> 50 MB"
                    else -> ""
                }
                ActiveChip(
                    label = sizeLabel,
                    onRemove = onRemoveFileSize,
                    testTag = "active_filter_size"
                )
            }

            // Extension Chips
            filterState.selectedExtensions.forEach { ext ->
                ActiveChip(
                    label = ext,
                    onRemove = { onRemoveExtension(ext) },
                    testTag = "active_filter_ext_$ext"
                )
            }

            // Dimension Chip
            if (filterState.dimensionOption != DimensionFilterOption.ALL) {
                val dimLabel = when (filterState.dimensionOption) {
                    DimensionFilterOption.SMALL -> "صور صغيرة"
                    DimensionFilterOption.MEDIUM -> "صور متوسطة"
                    DimensionFilterOption.HIGH_RES -> "دقة عالية (4K+)"
                    else -> ""
                }
                ActiveChip(
                    label = dimLabel,
                    onRemove = onRemoveDimension,
                    testTag = "active_filter_dimension"
                )
            }
        }
    }
}

@Composable
private fun ActiveChip(
    label: String,
    onRemove: () -> Unit,
    testTag: String
) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = ObsidianBg
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = ObsidianBg,
                modifier = Modifier.size(12.dp)
            )
        },
        colors = InputChipDefaults.inputChipColors(
            selectedContainerColor = CyanAccent,
            selectedLabelColor = ObsidianBg,
            selectedTrailingIconColor = ObsidianBg
        ),
        border = null,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag(testTag)
    )
}
