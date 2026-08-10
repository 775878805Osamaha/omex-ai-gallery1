package com.omex.gallery.ui.feature_detail

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.omex.gallery.R
import com.omex.gallery.domain.model.MediaItem
import com.omex.gallery.ui.feature_gallery.translateMlCategoryOrLabel
import com.omex.gallery.ui.feature_gallery.translatePersonName
import com.omex.gallery.ui.theme.AmberAccent
import com.omex.gallery.ui.theme.CyanAccent
import com.omex.gallery.ui.theme.ObsidianBg
import com.omex.gallery.ui.theme.SurfaceCard
import com.omex.gallery.ui.theme.SurfaceDark
import com.omex.gallery.ui.theme.TextMutedDark
import com.omex.gallery.ui.theme.TextPrimaryDark
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    viewModel: MediaDetailViewModel,
    onBackClick: () -> Unit,
    onAskAiClick: ((Long) -> Unit)? = null
) {
    val mediaItemList by viewModel.mediaItemList.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentIndex.collectAsStateWithLifecycle()
    val mediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val aiDetails by viewModel.mediaItemWithAi.collectAsStateWithLifecycle()
    val showExifSheet by viewModel.showExifSheet.collectAsStateWithLifecycle()
    val superResState by viewModel.superResolutionState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState()
    var showBoundingBoxes by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = currentIndex.coerceAtLeast(0),
        pageCount = { mediaItemList.size.coerceAtLeast(1) }
    )

    // Sync Pager state with ViewModel index
    LaunchedEffect(currentIndex) {
        if (currentIndex in mediaItemList.indices && pagerState.currentPage != currentIndex) {
            pagerState.scrollToPage(currentIndex)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page in mediaItemList.indices) {
                viewModel.setCurrentIndex(page)
            }
        }
    }

    if (showDeleteDialog && mediaItem != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("حذف الوسائط", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت تأكد من رغبتك في حذف هذا الملف من المعرض والجهاز؟", color = TextMutedDark) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteCurrentMedia(onDeletedAll = onBackClick)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("حذف", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("إلغاء", color = TextPrimaryDark)
                }
            },
            containerColor = SurfaceDark
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = mediaItem?.fileName ?: stringResource(R.string.media_viewer),
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    mediaItem?.let { item ->
                        IconButton(
                            onClick = { onAskAiClick?.invoke(item.id) },
                            modifier = Modifier.testTag("detail_ask_ai_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = stringResource(R.string.ask_image_title),
                                tint = AmberAccent
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.runAiAnalysis(context) },
                        modifier = Modifier.testTag("run_ai_analysis_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = stringResource(R.string.run_ai),
                            tint = CyanAccent
                        )
                    }
                    mediaItem?.let { item ->
                        IconButton(onClick = { viewModel.toggleFavorite() }, modifier = Modifier.testTag("detail_favorite_button")) {
                            Icon(
                                imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = stringResource(R.string.favorite),
                                tint = if (item.isFavorite) AmberAccent else TextPrimaryDark
                            )
                        }
                        IconButton(onClick = { viewModel.toggleExifSheet() }, modifier = Modifier.testTag("detail_info_button")) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = stringResource(R.string.ai_exif_info),
                                tint = CyanAccent
                            )
                        }
                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = item.mimeType
                                    putExtra(Intent.EXTRA_STREAM, Uri.parse(item.uriString))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                            },
                            modifier = Modifier.testTag("detail_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share),
                                tint = TextPrimaryDark
                            )
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.testTag("detail_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
            )
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBg),
            contentAlignment = Alignment.Center
        ) {
            if (mediaItemList.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val item = mediaItemList.getOrNull(page)
                    if (item != null) {
                        MediaItemContentViewer(
                            item = item,
                            superResState = superResState,
                            showBoundingBoxes = showBoundingBoxes,
                            aiDetails = aiDetails
                        )
                    }
                }
            }

            // Super Resolution Overlay Banner
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                when (val state = superResState) {
                    is SuperResolutionState.Processing -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.upscaling_progress), color = CyanAccent, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = CyanAccent,
                                    trackColor = SurfaceCard
                                )
                            }
                        }
                    }
                    is SuperResolutionState.Success -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.upscaled_success), color = AmberAccent, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { viewModel.resetSuperResolutionState() }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.clear), tint = TextPrimaryDark)
                                }
                            }
                        }
                    }
                    else -> {
                        if (mediaItem?.isVideo == false) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.runSuperResolution(context, scaleFactor = 2) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("super_res_2x_button")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.upscale_2x), color = CyanAccent, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { viewModel.runSuperResolution(context, scaleFactor = 4) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("super_res_4x_button")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.upscale_4x), color = AmberAccent, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // AI & EXIF Sheet
        if (showExifSheet && mediaItem != null) {
            val item = mediaItem!!
            ModalBottomSheet(
                onDismissRequest = { viewModel.toggleExifSheet() },
                sheetState = sheetState,
                containerColor = SurfaceDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                        .testTag("exif_info_sheet")
                ) {
                    Text(
                        text = stringResource(R.string.ai_intelligence_metadata),
                        style = MaterialTheme.typography.titleLarge,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // AI Classification Top Results
                    if (aiDetails != null && aiDetails!!.classifications.isNotEmpty()) {
                        Text(stringResource(R.string.mobilenet_categories), color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                aiDetails!!.classifications.take(3).forEach { cls ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${translateMlCategoryOrLabel(cls.category)} (${translateMlCategoryOrLabel(cls.label)})", color = TextPrimaryDark, fontSize = 13.sp)
                                        Text("${(cls.confidence * 100).toInt()}%", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Extracted OCR Text
                    if (aiDetails != null && !aiDetails!!.ocrText?.extractedText.isNullOrBlank()) {
                        Text(stringResource(R.string.ocr_text_extracted), color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = aiDetails!!.ocrText!!.extractedText,
                                    color = AmberAccent,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // YOLO Detected Objects
                    if (aiDetails != null && aiDetails!!.objects.isNotEmpty()) {
                        Text(stringResource(R.string.yolo_objects_detected, aiDetails!!.objects.size), color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                aiDetails!!.objects.forEach { obj ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(translateMlCategoryOrLabel(obj.labelName), color = TextPrimaryDark, fontSize = 13.sp)
                                        Text(stringResource(R.string.confidence_percent, (obj.score * 100).toInt()), color = AmberAccent, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Faces & FaceNet Clusters
                    if (aiDetails != null && aiDetails!!.faces.isNotEmpty()) {
                        Text(stringResource(R.string.faces_detected_count, aiDetails!!.faces.size), color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                aiDetails!!.faces.forEachIndexed { idx, face ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${stringResource(R.string.face_number, idx + 1)} (${face.clusterId?.let { translatePersonName(it) } ?: stringResource(R.string.unassigned)})", color = TextPrimaryDark, fontSize = 13.sp)
                                        Text("${(face.confidence * 100).toInt()}%", color = CyanAccent, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Perceptual Hashes
                    aiDetails?.metadata?.let { meta ->
                        Text(stringResource(R.string.perceptual_hashes), color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                ExifRow("SHA-256", meta.sha256Hash.take(16) + "...")
                                ExifRow("aHash", meta.aHash.toString(16))
                                ExifRow("dHash", meta.dHash.toString(16))
                                ExifRow("pHash", meta.pHash.toString(16))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // EXIF & File Attributes
                    Text(stringResource(R.string.file_attributes), color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            ExifRow(stringResource(R.string.file_name_label), item.fileName)
                            ExifRow(stringResource(R.string.resolution_label), "${item.width} x ${item.height}")
                            ExifRow(stringResource(R.string.size_label), formatFileSize(item.sizeBytes))
                            ExifRow(stringResource(R.string.date_taken_label), formatDate(item.dateTaken))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun MediaItemContentViewer(
    item: MediaItem,
    superResState: SuperResolutionState,
    showBoundingBoxes: Boolean,
    aiDetails: com.omex.gallery.domain.model.MediaItemWithAi?
) {
    if (item.isVideo) {
        // Native Interactive Video View Player
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        val mediaController = MediaController(context)
                        mediaController.setAnchorView(this)
                        setMediaController(mediaController)
                        setVideoURI(Uri.parse(item.uriString))
                        requestFocus()
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("video_player_view")
            )
        }
    } else {
        // Zoomable & Pannable Image Viewer
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        val activeDisplayPath = when (superResState) {
            is SuperResolutionState.Success -> (superResState as SuperResolutionState.Success).upscaledPath
            else -> item.uriString
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 2.5f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = activeDisplayPath,
                contentDescription = item.fileName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .testTag("full_media_preview")
            )

            // Overlay bounding boxes for YOLO objects and Faces
            if (showBoundingBoxes && aiDetails != null && scale == 1f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw YOLO detected objects in Cyan
                    aiDetails.objects.forEach { obj ->
                        val left = obj.left * w
                        val top = obj.top * h
                        val width = (obj.right - obj.left) * w
                        val height = (obj.bottom - obj.top) * h

                        drawRect(
                            color = Color(0xFF00E5FF),
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            style = Stroke(width = 4f)
                        )
                    }

                    // Draw Faces in Amber
                    aiDetails.faces.forEach { face ->
                        val left = face.left * w
                        val top = face.top * h
                        val width = (face.right - face.left) * w
                        val height = (face.bottom - face.top) * h

                        drawRect(
                            color = AmberAccent,
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            style = Stroke(width = 4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExifRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMutedDark, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, color = TextPrimaryDark, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format("%.2f MB", mb)
        kb >= 1.0 -> String.format("%.2f KB", kb)
        else -> "$bytes B"
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return "Unknown"
    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
