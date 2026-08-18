package com.omex.gallery.ui.feature_gallery.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.PhotoSizeSelectSmall
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.omex.gallery.ui.theme.SurfaceDark
import com.omex.gallery.ui.theme.TextMutedDark
import com.omex.gallery.ui.theme.TextPrimaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val availableExtensions = listOf(
    "JPG", "JPEG", "PNG", "WEBP", "HEIC", "MP4", "MKV", "MOV"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedFilterBottomSheet(
    currentFilterState: SearchFilterState,
    categories: List<MediaCategoryEntity>,
    onApplyFilters: (SearchFilterState) -> Unit,
    onDismiss: () -> Unit,
    matchingCount: Int? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var draftState by remember(currentFilterState) { mutableStateOf(currentFilterState) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        contentColor = TextPrimaryDark,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(TextMutedDark.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            )
        },
        modifier = Modifier.testTag("advanced_filter_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.filters_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    if (draftState.hasActiveFilters) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(AmberAccent, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${draftState.activeFilterCount}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBg
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (draftState.hasActiveFilters) {
                        TextButton(
                            onClick = { draftState = SearchFilterState(query = draftState.query) },
                            modifier = Modifier.testTag("filter_reset_button")
                        ) {
                            Text(
                                text = stringResource(R.string.reset_filters),
                                color = AmberAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("filter_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear),
                            tint = TextMutedDark
                        )
                    }
                }
            }

            HorizontalDivider(
                color = Color.DarkGray.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: نوع الوسائط (Media Type)
                FilterSectionHeader(
                    icon = Icons.Default.Image,
                    title = stringResource(R.string.filter_type_media)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterOptionChip(
                        selected = draftState.isVideo == null,
                        label = stringResource(R.string.tab_all),
                        onClick = { draftState = draftState.copy(isVideo = null) },
                        modifier = Modifier.testTag("filter_chip_media_all")
                    )
                    FilterOptionChip(
                        selected = draftState.isVideo == false,
                        label = stringResource(R.string.tab_photos),
                        leadingIcon = Icons.Default.Image,
                        onClick = { draftState = draftState.copy(isVideo = false) },
                        modifier = Modifier.testTag("filter_chip_media_photos")
                    )
                    FilterOptionChip(
                        selected = draftState.isVideo == true,
                        label = stringResource(R.string.tab_videos),
                        leadingIcon = Icons.Default.Videocam,
                        onClick = { draftState = draftState.copy(isVideo = true) },
                        modifier = Modifier.testTag("filter_chip_media_videos")
                    )
                }

                // Section 2: الحالة (Status / Favorite)
                FilterSectionHeader(
                    icon = Icons.Default.Favorite,
                    title = stringResource(R.string.filter_status)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterOptionChip(
                        selected = draftState.isFavorite == null,
                        label = stringResource(R.string.tab_all),
                        onClick = { draftState = draftState.copy(isFavorite = null) },
                        modifier = Modifier.testTag("filter_chip_favorite_all")
                    )
                    FilterOptionChip(
                        selected = draftState.isFavorite == true,
                        label = stringResource(R.string.filter_status_favorite),
                        leadingIcon = Icons.Default.Favorite,
                        onClick = { draftState = draftState.copy(isFavorite = true) },
                        modifier = Modifier.testTag("filter_chip_favorite_true")
                    )
                    FilterOptionChip(
                        selected = draftState.isFavorite == false,
                        label = stringResource(R.string.filter_status_non_favorite),
                        onClick = { draftState = draftState.copy(isFavorite = false) },
                        modifier = Modifier.testTag("filter_chip_favorite_false")
                    )
                }

                // Section 3: التصنيفات الذكية (Smart Categories)
                FilterSectionHeader(
                    icon = Icons.Default.Category,
                    title = stringResource(R.string.filter_categories)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val allCategories = if (categories.isNotEmpty()) {
                        categories
                    } else {
                        listOf(
                            MediaCategoryEntity("PERSON", "الأشخاص", "person"),
                            MediaCategoryEntity("PRODUCT", "المنتجات", "product"),
                            MediaCategoryEntity("TRADING", "التداول", "trading"),
                            MediaCategoryEntity("SCREENSHOT", "لقطات الشاشة", "screenshot"),
                            MediaCategoryEntity("DOCUMENT", "المستندات", "document"),
                            MediaCategoryEntity("CAR", "السيارات", "car"),
                            MediaCategoryEntity("FOOD", "الطعام", "food"),
                            MediaCategoryEntity("NATURE", "الطبيعة", "nature"),
                            MediaCategoryEntity("TRAVEL", "السفر", "travel"),
                            MediaCategoryEntity("WORK", "صور العمل", "work")
                        )
                    }

                    allCategories.forEach { category ->
                        val isSelected = draftState.allSelectedCategories.contains(category.categoryId)
                        val icon = getCategoryIcon(category.categoryId)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val currentCats = draftState.allSelectedCategories.toMutableSet()
                                if (isSelected) {
                                    currentCats.remove(category.categoryId)
                                } else {
                                    currentCats.add(category.categoryId)
                                }
                                draftState = draftState.copy(
                                    categoryId = null,
                                    selectedCategoryIds = currentCats
                                )
                            },
                            label = {
                                Text(
                                    text = category.nameArabic,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
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
                                containerColor = SurfaceCard,
                                labelColor = TextPrimaryDark,
                                iconColor = AmberAccent
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = null,
                            modifier = Modifier.testTag("filter_chip_cat_${category.categoryId}")
                        )
                    }
                }

                // Section 4: التاريخ (Date Range)
                FilterSectionHeader(
                    icon = Icons.Default.DateRange,
                    title = stringResource(R.string.filter_date)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterOptionChip(
                        selected = draftState.dateFilterOption == DateFilterOption.ALL,
                        label = stringResource(R.string.tab_all),
                        onClick = { draftState = draftState.copy(dateFilterOption = DateFilterOption.ALL, startDateMs = null, endDateMs = null) },
                        modifier = Modifier.testTag("filter_date_all")
                    )
                    FilterOptionChip(
                        selected = draftState.dateFilterOption == DateFilterOption.TODAY,
                        label = stringResource(R.string.filter_date_today),
                        onClick = { draftState = draftState.copy(dateFilterOption = DateFilterOption.TODAY) },
                        modifier = Modifier.testTag("filter_date_today")
                    )
                    FilterOptionChip(
                        selected = draftState.dateFilterOption == DateFilterOption.LAST_7_DAYS,
                        label = stringResource(R.string.filter_date_7days),
                        onClick = { draftState = draftState.copy(dateFilterOption = DateFilterOption.LAST_7_DAYS) },
                        modifier = Modifier.testTag("filter_date_7days")
                    )
                    FilterOptionChip(
                        selected = draftState.dateFilterOption == DateFilterOption.LAST_30_DAYS,
                        label = stringResource(R.string.filter_date_30days),
                        onClick = { draftState = draftState.copy(dateFilterOption = DateFilterOption.LAST_30_DAYS) },
                        modifier = Modifier.testTag("filter_date_30days")
                    )
                    FilterOptionChip(
                        selected = draftState.dateFilterOption == DateFilterOption.THIS_YEAR,
                        label = stringResource(R.string.filter_date_this_year),
                        onClick = { draftState = draftState.copy(dateFilterOption = DateFilterOption.THIS_YEAR) },
                        modifier = Modifier.testTag("filter_date_this_year")
                    )
                    FilterOptionChip(
                        selected = draftState.dateFilterOption == DateFilterOption.CUSTOM,
                        label = stringResource(R.string.filter_date_custom),
                        onClick = { draftState = draftState.copy(dateFilterOption = DateFilterOption.CUSTOM) },
                        modifier = Modifier.testTag("filter_date_custom")
                    )
                }

                // Custom Date Range Pickers
                AnimatedVisibility(visible = draftState.dateFilterOption == DateFilterOption.CUSTOM) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showStartDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_date_picker_button")
                        ) {
                            Text(
                                text = draftState.startDateMs?.let {
                                    stringResource(R.string.custom_date_from, dateFormatter.format(Date(it)))
                                } ?: "تاريخ البدء",
                                fontSize = 11.sp,
                                color = CyanAccent
                            )
                        }
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("end_date_picker_button")
                        ) {
                            Text(
                                text = draftState.endDateMs?.let {
                                    stringResource(R.string.custom_date_to, dateFormatter.format(Date(it)))
                                } ?: "تاريخ الانتهاء",
                                fontSize = 11.sp,
                                color = AmberAccent
                            )
                        }
                    }
                }

                // Section 5: حجم الملف (File Size)
                FilterSectionHeader(
                    icon = Icons.Default.SdStorage,
                    title = stringResource(R.string.filter_size)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterOptionChip(
                        selected = draftState.fileSizeOption == FileSizeFilterOption.ALL,
                        label = stringResource(R.string.tab_all),
                        onClick = { draftState = draftState.copy(fileSizeOption = FileSizeFilterOption.ALL) },
                        modifier = Modifier.testTag("filter_size_all")
                    )
                    FilterOptionChip(
                        selected = draftState.fileSizeOption == FileSizeFilterOption.LESS_THAN_1MB,
                        label = stringResource(R.string.filter_size_lt_1mb),
                        onClick = { draftState = draftState.copy(fileSizeOption = FileSizeFilterOption.LESS_THAN_1MB) },
                        modifier = Modifier.testTag("filter_size_lt_1mb")
                    )
                    FilterOptionChip(
                        selected = draftState.fileSizeOption == FileSizeFilterOption.BETWEEN_1_5MB,
                        label = stringResource(R.string.filter_size_1_5mb),
                        onClick = { draftState = draftState.copy(fileSizeOption = FileSizeFilterOption.BETWEEN_1_5MB) },
                        modifier = Modifier.testTag("filter_size_1_5mb")
                    )
                    FilterOptionChip(
                        selected = draftState.fileSizeOption == FileSizeFilterOption.BETWEEN_5_50MB,
                        label = stringResource(R.string.filter_size_5_50mb),
                        onClick = { draftState = draftState.copy(fileSizeOption = FileSizeFilterOption.BETWEEN_5_50MB) },
                        modifier = Modifier.testTag("filter_size_5_50mb")
                    )
                    FilterOptionChip(
                        selected = draftState.fileSizeOption == FileSizeFilterOption.GREATER_THAN_50MB,
                        label = stringResource(R.string.filter_size_gt_50mb),
                        onClick = { draftState = draftState.copy(fileSizeOption = FileSizeFilterOption.GREATER_THAN_50MB) },
                        modifier = Modifier.testTag("filter_size_gt_50mb")
                    )
                }

                // Section 6: نوع الملف / الامتداد (File Extensions)
                FilterSectionHeader(
                    icon = Icons.Default.TextFields,
                    title = stringResource(R.string.filter_extension)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableExtensions.forEach { ext ->
                        val isSelected = draftState.selectedExtensions.contains(ext)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val current = draftState.selectedExtensions.toMutableSet()
                                if (isSelected) current.remove(ext) else current.add(ext)
                                draftState = draftState.copy(selectedExtensions = current)
                            },
                            label = {
                                Text(
                                    text = ext,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent,
                                selectedLabelColor = ObsidianBg,
                                containerColor = SurfaceCard,
                                labelColor = TextPrimaryDark
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = null,
                            modifier = Modifier.testTag("filter_chip_ext_$ext")
                        )
                    }
                }

                // Section 7: أبعاد الصورة (Dimensions / Resolution)
                FilterSectionHeader(
                    icon = Icons.Default.PhotoSizeSelectActual,
                    title = stringResource(R.string.filter_dimension)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterOptionChip(
                        selected = draftState.dimensionOption == DimensionFilterOption.ALL,
                        label = stringResource(R.string.tab_all),
                        onClick = { draftState = draftState.copy(dimensionOption = DimensionFilterOption.ALL) },
                        modifier = Modifier.testTag("filter_dim_all")
                    )
                    FilterOptionChip(
                        selected = draftState.dimensionOption == DimensionFilterOption.SMALL,
                        label = stringResource(R.string.filter_dim_small),
                        leadingIcon = Icons.Default.PhotoSizeSelectSmall,
                        onClick = { draftState = draftState.copy(dimensionOption = DimensionFilterOption.SMALL) },
                        modifier = Modifier.testTag("filter_dim_small")
                    )
                    FilterOptionChip(
                        selected = draftState.dimensionOption == DimensionFilterOption.MEDIUM,
                        label = stringResource(R.string.filter_dim_medium),
                        leadingIcon = Icons.Default.PhotoSizeSelectLarge,
                        onClick = { draftState = draftState.copy(dimensionOption = DimensionFilterOption.MEDIUM) },
                        modifier = Modifier.testTag("filter_dim_medium")
                    )
                    FilterOptionChip(
                        selected = draftState.dimensionOption == DimensionFilterOption.HIGH_RES,
                        label = stringResource(R.string.filter_dim_high_res),
                        leadingIcon = Icons.Default.AutoAwesome,
                        onClick = { draftState = draftState.copy(dimensionOption = DimensionFilterOption.HIGH_RES) },
                        modifier = Modifier.testTag("filter_dim_high_res")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        draftState = SearchFilterState()
                        onApplyFilters(SearchFilterState())
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filter_clear_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.clear_all),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }

                Button(
                    onClick = {
                        onApplyFilters(draftState)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("filter_apply_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (matchingCount != null) {
                            stringResource(R.string.apply_filters_with_count, matchingCount)
                        } else {
                            stringResource(R.string.apply_filters)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Material 3 Date Picker Dialogs for Custom Range
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = draftState.startDateMs ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        draftState = draftState.copy(startDateMs = datePickerState.selectedDateMillis)
                        showStartDatePicker = false
                    }
                ) {
                    Text("تم", color = CyanAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(R.string.cancel), color = TextMutedDark)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = draftState.endDateMs ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        draftState = draftState.copy(endDateMs = datePickerState.selectedDateMillis)
                        showEndDatePicker = false
                    }
                ) {
                    Text("تم", color = CyanAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(stringResource(R.string.cancel), color = TextMutedDark)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun FilterSectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanAccent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark
        )
    }
}

@Composable
private fun FilterOptionChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
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
        modifier = modifier
    )
}

private fun getCategoryIcon(categoryId: String): ImageVector {
    return when (categoryId.uppercase()) {
        "PERSON" -> Icons.Default.Person
        "PRODUCT" -> Icons.Default.AutoAwesome
        "TRADING" -> Icons.Default.ShowChart
        "SCREENSHOT" -> Icons.Default.Crop
        "DOCUMENT" -> Icons.Default.Description
        "CAR" -> Icons.Default.DirectionsCar
        "FOOD" -> Icons.Default.Restaurant
        "NATURE" -> Icons.Default.Park
        "TRAVEL" -> Icons.Default.Flight
        "WORK" -> Icons.Default.Business
        else -> Icons.Default.Category
    }
}
