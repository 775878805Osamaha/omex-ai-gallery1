package com.example.ui.feature_detail

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    viewModel: MediaDetailViewModel,
    onBackClick: () -> Unit
) {
    val mediaItem by viewModel.mediaItem.collectAsStateWithLifecycle()
    val aiDetails by viewModel.mediaItemWithAi.collectAsStateWithLifecycle()
    val showExifSheet by viewModel.showExifSheet.collectAsStateWithLifecycle()
    val superResState by viewModel.superResolutionState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState()
    var showBoundingBoxes by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = mediaItem?.fileName ?: "Media Viewer",
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.runAiAnalysis(context) },
                        modifier = Modifier.testTag("run_ai_analysis_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Run AI",
                            tint = CyanAccent
                        )
                    }
                    mediaItem?.let { item ->
                        IconButton(onClick = { viewModel.toggleFavorite() }, modifier = Modifier.testTag("detail_favorite_button")) {
                            Icon(
                                imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (item.isFavorite) AmberAccent else TextPrimaryDark
                            )
                        }
                        IconButton(onClick = { viewModel.toggleExifSheet() }, modifier = Modifier.testTag("detail_info_button")) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "AI & EXIF Info",
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
                                contentDescription = "Share",
                                tint = TextPrimaryDark
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
            mediaItem?.let { item ->
                val activeDisplayPath = when (superResState) {
                    is SuperResolutionState.Success -> (superResState as SuperResolutionState.Success).upscaledPath
                    else -> item.uriString
                }

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = activeDisplayPath,
                        contentDescription = item.fileName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("full_media_preview")
                    )

                    // Overlay bounding boxes for YOLO objects and Faces
                    if (showBoundingBoxes && aiDetails != null) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw YOLO detected objects in Cyan
                            aiDetails?.objects?.forEach { obj ->
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
                            aiDetails?.faces?.forEach { face ->
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
                                    Text("Real-ESRGAN Upscaling...", color = CyanAccent, fontWeight = FontWeight.Bold)
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
                                    Text("✨ Image Upscaled with Real-ESRGAN", color = AmberAccent, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { viewModel.resetSuperResolutionState() }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextPrimaryDark)
                                    }
                                }
                            }
                        }
                        else -> {
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
                                    Text("2x Upscale", color = CyanAccent, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { viewModel.runSuperResolution(context, scaleFactor = 4) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("super_res_4x_button")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("4x Upscale", color = AmberAccent, fontSize = 12.sp)
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
                        text = "AI Intelligence & Metadata",
                        style = MaterialTheme.typography.titleLarge,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // AI Classification Top Results
                    if (aiDetails != null && aiDetails!!.classifications.isNotEmpty()) {
                        Text("MobileNetV3 Top Categories", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                aiDetails!!.classifications.take(3).forEach { cls ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${cls.category} (${cls.label})", color = TextPrimaryDark, fontSize = 13.sp)
                                        Text("${(cls.confidence * 100).toInt()}%", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // YOLO Detected Objects
                    if (aiDetails != null && aiDetails!!.objects.isNotEmpty()) {
                        Text("YOLOv8 Detected Objects (${aiDetails!!.objects.size})", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                aiDetails!!.objects.forEach { obj ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(obj.labelName, color = TextPrimaryDark, fontSize = 13.sp)
                                        Text("${(obj.score * 100).toInt()}% confidence", color = AmberAccent, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Faces & FaceNet Clusters
                    if (aiDetails != null && aiDetails!!.faces.isNotEmpty()) {
                        Text("Detected Faces (${aiDetails!!.faces.size})", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                aiDetails!!.faces.forEachIndexed { idx, face ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Face #${idx + 1} (${face.clusterId ?: "Unassigned"})", color = TextPrimaryDark, fontSize = 13.sp)
                                        Text("${(face.confidence * 100).toInt()}%", color = CyanAccent, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Perceptual Hashes
                    aiDetails?.metadata?.let { meta ->
                        Text("Perceptual Hashes", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
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
                    Text("File Attributes", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            ExifRow("File Name", item.fileName)
                            ExifRow("Resolution", "${item.width} x ${item.height}")
                            ExifRow("Size", formatFileSize(item.sizeBytes))
                            ExifRow("Date Taken", formatDate(item.dateTaken))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
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
