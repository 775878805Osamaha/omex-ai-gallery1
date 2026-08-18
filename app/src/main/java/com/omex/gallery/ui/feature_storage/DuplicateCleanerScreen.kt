package com.omex.gallery.ui.feature_storage

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.omex.gallery.R
import com.omex.gallery.domain.model.DuplicateGroupWithMedia
import com.omex.gallery.domain.model.DuplicateMemberWithMedia
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-precision duplicate media detection & cleanup screen powered by SHA-256 and pHash/dHash perceptual hashes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateCleanerScreen(
    viewModel: StorageAnalyzerViewModel,
    onBackClick: () -> Unit,
    onMediaClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val duplicateGroups: List<DuplicateGroupWithMedia> by viewModel.duplicateGroups.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedMemberIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var groupToCleanSingle by remember { mutableStateOf<DuplicateGroupWithMedia?>(null) }
    var showCleanAllDialog by remember { mutableStateOf(false) }

    // Automatically calculate recommended redundant items (keep index 0 as keeper in each group)
    val recommendedRedundantIds = remember(duplicateGroups) {
        val ids = mutableSetOf<Long>()
        duplicateGroups.forEach { group ->
            if (group.members.size > 1) {
                group.members.drop(1).forEach { member ->
                    ids.add(member.mediaItem.id)
                }
            }
        }
        ids
    }

    // Default select recommended redundant copies whenever new duplicate groups are loaded
    LaunchedEffect(recommendedRedundantIds) {
        selectedMemberIds = recommendedRedundantIds
    }

    val totalDuplicateMediaCount = remember(duplicateGroups) {
        duplicateGroups.sumOf { it.members.size }
    }

    val totalRedundantCopiesCount = remember(duplicateGroups) {
        duplicateGroups.sumOf { if (it.members.size > 1) it.members.size - 1 else 0 }
    }

    val totalReclaimableBytes = remember(duplicateGroups, selectedMemberIds) {
        var bytes = 0L
        duplicateGroups.forEach { group ->
            group.members.forEach { member ->
                if (selectedMemberIds.contains(member.mediaItem.id)) {
                    bytes += member.mediaItem.sizeBytes
                }
            }
        }
        bytes
    }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.duplicate_finder_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = stringResource(R.string.duplicate_finder_subtitle),
                            fontSize = 11.sp,
                            color = CyanAccent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("duplicate_cleaner_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = CyanAccent
                        )
                    }
                },
                actions = {
                    if (totalRedundantCopiesCount > 0) {
                        TextButton(
                            onClick = {
                                selectedMemberIds = if (selectedMemberIds.size == recommendedRedundantIds.size) {
                                    emptySet()
                                } else {
                                    recommendedRedundantIds
                                }
                            },
                            modifier = Modifier.testTag("duplicate_select_recommended_btn")
                        ) {
                            Text(
                                text = if (selectedMemberIds.isNotEmpty()) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                                color = AmberAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
        modifier = modifier.fillMaxSize().testTag("duplicate_cleaner_screen")
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyanAccent)
            }
        } else if (duplicateGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.duplicate_none_found_title),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            fontSize = 16.sp
                        )
                        Text(
                            text = stringResource(R.string.duplicate_none_found_desc),
                            color = TextSecondaryDark,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
            ) {
                // Duplicate Analysis Summary Hero
                item {
                    DuplicateHeroSummaryCard(
                        groupCount = duplicateGroups.size,
                        totalDuplicates = totalDuplicateMediaCount,
                        reclaimableCount = selectedMemberIds.size,
                        reclaimableBytes = totalReclaimableBytes,
                        onCleanAllClick = { showCleanAllDialog = true }
                    )
                }

                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stringResource(R.string.duplicate_detected_groups)} (${duplicateGroups.size})",
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${selectedMemberIds.size} ${stringResource(R.string.duplicate_selected_to_remove)}",
                            color = AmberAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Duplicate Groups
                items(duplicateGroups, key = { it.groupId }) { group ->
                    DuplicateGroupCard(
                        group = group,
                        selectedIds = selectedMemberIds,
                        onToggleMemberSelection = { memberId ->
                            selectedMemberIds = if (selectedMemberIds.contains(memberId)) {
                                selectedMemberIds - memberId
                            } else {
                                selectedMemberIds + memberId
                            }
                        },
                        onMediaClick = onMediaClick,
                        onCleanGroupClick = {
                            groupToCleanSingle = group
                        }
                    )
                }
            }
        }
    }

    // Confirmation Dialog for Cleaning Single Group
    groupToCleanSingle?.let { group ->
        val redundantInGroup = group.members.filter { selectedMemberIds.contains(it.mediaItem.id) }
        val freedBytes = redundantInGroup.sumOf { it.mediaItem.sizeBytes }

        AlertDialog(
            onDismissRequest = { groupToCleanSingle = null },
            title = {
                Text(
                    text = stringResource(R.string.duplicate_clean_group_title),
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.duplicate_clean_group_confirm,
                        redundantInGroup.size,
                        StorageAnalyzerViewModel.formatBytes(freedBytes)
                    ),
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val itemsToDelete = redundantInGroup.map { it.mediaItem }
                        viewModel.deleteSelectedItems(itemsToDelete)
                        groupToCleanSingle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text(stringResource(R.string.delete_selected), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToCleanSingle = null }) {
                    Text(stringResource(R.string.cancel), color = TextMutedDark)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Confirmation Dialog for Cleaning All Selected Redundant Copies
    if (showCleanAllDialog) {
        AlertDialog(
            onDismissRequest = { showCleanAllDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.duplicate_clean_all_title),
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.duplicate_clean_all_confirm,
                        selectedMemberIds.size,
                        StorageAnalyzerViewModel.formatBytes(totalReclaimableBytes)
                    ),
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCleanAllDialog = false
                        viewModel.cleanDuplicateCopies(duplicateGroups)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text(stringResource(R.string.storage_reclaim_space), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanAllDialog = false }) {
                    Text(stringResource(R.string.cancel), color = TextMutedDark)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun DuplicateHeroSummaryCard(
    groupCount: Int,
    totalDuplicates: Int,
    reclaimableCount: Int,
    reclaimableBytes: Long,
    onCleanAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AmberAccent.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .testTag("duplicate_hero_summary_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(AmberAccent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = AmberAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.duplicate_hash_engine_title),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 15.sp
                    )
                    Text(
                        text = stringResource(R.string.duplicate_hash_engine_subtitle),
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$groupCount",
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontSize = 18.sp
                    )
                    Text(
                        text = stringResource(R.string.duplicate_groups_stat),
                        fontSize = 10.sp,
                        color = TextMutedDark
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(SurfaceVariantDark)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$reclaimableCount",
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed,
                        fontSize = 18.sp
                    )
                    Text(
                        text = stringResource(R.string.duplicate_redundant_files_stat),
                        fontSize = 10.sp,
                        color = TextMutedDark
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(SurfaceVariantDark)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = StorageAnalyzerViewModel.formatBytes(reclaimableBytes),
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen,
                        fontSize = 18.sp
                    )
                    Text(
                        text = stringResource(R.string.duplicate_reclaimable_space_stat),
                        fontSize = 10.sp,
                        color = TextMutedDark
                    )
                }
            }

            // One-Tap Smart Cleanup Button
            Button(
                onClick = onCleanAllClick,
                enabled = reclaimableCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("duplicate_clean_all_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberAccent,
                    disabledContainerColor = SurfaceDark
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = if (reclaimableCount > 0) ObsidianBg else TextMutedDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.duplicate_auto_clean_button, StorageAnalyzerViewModel.formatBytes(reclaimableBytes)),
                    fontWeight = FontWeight.Bold,
                    color = if (reclaimableCount > 0) ObsidianBg else TextMutedDark,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun DuplicateGroupCard(
    group: DuplicateGroupWithMedia,
    selectedIds: Set<Long>,
    onToggleMemberSelection: (Long) -> Unit,
    onMediaClick: (Long) -> Unit,
    onCleanGroupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalGroupBytes = remember(group) {
        group.members.sumOf { it.mediaItem.sizeBytes }
    }

    val redundantMembersInGroup = remember(group, selectedIds) {
        group.members.filter { selectedIds.contains(it.mediaItem.id) }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("duplicate_group_${group.groupId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Group Title Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(NeonPurple.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "مجموعة الهاش: ${group.groupId.take(12)}...",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceDark
                ) {
                    Text(
                        text = "${group.members.size} ملفات • ${StorageAnalyzerViewModel.formatBytes(totalGroupBytes)}",
                        fontSize = 10.sp,
                        color = CyanAccent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Media Items Grid in this duplicate group
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(group.members.indices.toList()) { index ->
                    val member = group.members[index]
                    val isOriginal = index == 0
                    val isSelectedToDelete = selectedIds.contains(member.mediaItem.id)

                    DuplicateMemberCard(
                        member = member,
                        isOriginal = isOriginal,
                        isSelectedToDelete = isSelectedToDelete,
                        onItemClick = { onMediaClick(member.mediaItem.id) },
                        onToggleDelete = { onToggleMemberSelection(member.mediaItem.id) }
                    )
                }
            }

            // Quick Cleanup action for this specific group
            if (redundantMembersInGroup.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onCleanGroupClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("clean_group_${group.groupId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تنظيف التكرار (${redundantMembersInGroup.size})",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicateMemberCard(
    member: DuplicateMemberWithMedia,
    isOriginal: Boolean,
    isSelectedToDelete: Boolean,
    onItemClick: () -> Unit,
    onToggleDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val item = member.mediaItem

    val borderStroke = when {
        isSelectedToDelete -> ErrorRed
        isOriginal -> SuccessGreen
        else -> Color.Transparent
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .width(130.dp)
            .border(1.5.dp, borderStroke, RoundedCornerShape(12.dp))
            .clickable { onItemClick() }
            .testTag("duplicate_member_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Thumbnail Image Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianBg)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.uriString.ifEmpty { item.filePath })
                        .crossfade(true)
                        .build(),
                    contentDescription = item.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Badge: Original vs Duplicate
                if (isOriginal) {
                    Surface(
                        color = SuccessGreen,
                        shape = RoundedCornerShape(bottomEnd = 6.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "الأصل (KEEP)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ObsidianBg,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        color = if (isSelectedToDelete) ErrorRed else AmberAccent,
                        shape = RoundedCornerShape(bottomEnd = 6.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "مكرر (COPY)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelectedToDelete) Color.White else ObsidianBg,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Checkbox toggle button on top right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .background(
                            if (isSelectedToDelete) ErrorRed else ObsidianBg.copy(alpha = 0.6f),
                            CircleShape
                        )
                        .clickable { onToggleDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelectedToDelete) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // File Info
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.fileName,
                    fontSize = 11.sp,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = StorageAnalyzerViewModel.formatBytes(item.sizeBytes),
                        fontSize = 10.sp,
                        color = TextSecondaryDark,
                        fontWeight = FontWeight.Bold
                    )
                    if (member.similarityScore < 1.0f) {
                        Text(
                            text = "${(member.similarityScore * 100).toInt()}% شبه",
                            fontSize = 9.sp,
                            color = CyanAccent
                        )
                    }
                }
            }
        }
    }
}
