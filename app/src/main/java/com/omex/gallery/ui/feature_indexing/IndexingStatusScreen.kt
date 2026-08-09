package com.omex.gallery.ui.feature_indexing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omex.gallery.R
import com.omex.gallery.core.indexer.ThumbnailGenerator
import com.omex.gallery.ui.feature_gallery.GalleryViewModel
import com.omex.gallery.ui.theme.CyanAccent
import com.omex.gallery.ui.theme.ObsidianBg
import com.omex.gallery.ui.theme.SuccessGreen
import com.omex.gallery.ui.theme.SurfaceCard
import com.omex.gallery.ui.theme.SurfaceDark
import com.omex.gallery.ui.theme.TextMutedDark
import com.omex.gallery.ui.theme.TextPrimaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndexingStatusScreen(
    galleryViewModel: GalleryViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val indexingProgress by galleryViewModel.indexingProgress.collectAsStateWithLifecycle()
    val mediaItems by galleryViewModel.mediaItems.collectAsStateWithLifecycle()

    val thumbnailGenerator = remember { ThumbnailGenerator(context) }
    val cacheSizeBytes = remember(indexingProgress) { thumbnailGenerator.getThumbnailCacheSize() }

    val indexedCount = if (indexingProgress.totalCount > 0) indexingProgress.scannedCount else mediaItems.count { it.isIndexed }
    val totalCount = if (indexingProgress.totalCount > 0) indexingProgress.totalCount else mediaItems.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.indexer_system_status), color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("indexer_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = TextPrimaryDark)
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.omex_offline_engine), color = TextPrimaryDark, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.indexing_phase_1), color = TextMutedDark, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Stat Cards Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    title = stringResource(R.string.indexed_items),
                    value = "$indexedCount / $totalCount",
                    icon = Icons.Default.CheckCircle,
                    tint = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    title = stringResource(R.string.cache_footprint),
                    value = formatBytes(cacheSizeBytes),
                    icon = Icons.Default.Storage,
                    tint = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            // Current Status
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.index_status), color = TextPrimaryDark, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = indexingProgress.message.ifEmpty { stringResource(R.string.index_status_healthy) },
                        color = CyanAccent,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Rescan Button
            Button(
                onClick = { galleryViewModel.triggerFullReindex(context) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("rescan_index_button")
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = ObsidianBg)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.trigger_full_reindex), color = ObsidianBg, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, color = TextMutedDark, fontSize = 12.sp)
            Text(value, color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) String.format("%.2f MB", mb) else String.format("%.1f KB", kb)
}
