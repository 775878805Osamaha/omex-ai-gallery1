package com.omex.gallery.ui.feature_diagnostic

import android.content.Context
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omex.gallery.domain.model.MediaRepository
import com.omex.gallery.ui.theme.AmberAccent
import com.omex.gallery.ui.theme.CyanAccent
import com.omex.gallery.ui.theme.ObsidianBg
import com.omex.gallery.ui.theme.SurfaceCard
import com.omex.gallery.ui.theme.SurfaceDark
import com.omex.gallery.ui.theme.TextMutedDark
import com.omex.gallery.ui.theme.TextPrimaryDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DiagnosticData(
    val mediaStoreImagesCount: String = "...",
    val mediaStoreVideosCount: String = "...",
    val roomItemsCount: String = "...",
    val roomImagesCount: String = "...",
    val roomVideosCount: String = "...",
    val lastIndexingError: String = "None",
    val isLoading: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(
    mediaRepository: MediaRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var data by remember { mutableStateOf(DiagnosticData()) }
    val coroutineScope = rememberCoroutineScope()

    suspend fun loadDiagnosticData() {
        data = data.copy(isLoading = true)
        val msImages = queryMediaStoreImagesCount(context)
        val msVideos = queryMediaStoreVideosCount(context)
        val roomItems = mediaRepository.getRoomMediaCount()
        val roomImages = mediaRepository.getRoomPhotosCount()
        val roomVideos = mediaRepository.getRoomVideosCount()
        val errorMsg = mediaRepository.getLastIndexingError()

        data = DiagnosticData(
            mediaStoreImagesCount = msImages.toString(),
            mediaStoreVideosCount = msVideos.toString(),
            roomItemsCount = roomItems.toString(),
            roomImagesCount = roomImages.toString(),
            roomVideosCount = roomVideos.toString(),
            lastIndexingError = if (errorMsg.isEmpty()) "None" else errorMsg,
            isLoading = false
        )
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            loadDiagnosticData()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostic Screen", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("diagnostic_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("diagnostic_results_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = AmberAccent
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Diagnostic Status",
                            color = TextPrimaryDark,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    DiagnosticMetricRow(label = "MediaStore Images count", value = data.mediaStoreImagesCount)
                    DiagnosticMetricRow(label = "MediaStore Videos count", value = data.mediaStoreVideosCount)
                    DiagnosticMetricRow(label = "Room Items count", value = data.roomItemsCount)
                    DiagnosticMetricRow(label = "Room Images count", value = data.roomImagesCount)
                    DiagnosticMetricRow(label = "Room Videos count", value = data.roomVideosCount)
                    DiagnosticMetricRow(label = "Last indexing error", value = data.lastIndexingError)

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                loadDiagnosticData()
                            }
                        },
                        enabled = !data.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("refresh_diagnostic_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = ObsidianBg)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (data.isLoading) "Refreshing..." else "Refresh Diagnostic Data",
                            color = ObsidianBg,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "$label:", color = TextMutedDark, fontSize = 14.sp)
        Text(text = value, color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

private fun queryMediaStoreImagesCount(context: Context): Int {
    val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    return try {
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Images.Media._ID),
            null,
            null,
            null
        )?.use { it.count } ?: 0
    } catch (e: Exception) {
        0
    }
}

private fun queryMediaStoreVideosCount(context: Context): Int {
    val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    return try {
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Video.Media._ID),
            null,
            null,
            null
        )?.use { it.count } ?: 0
    } catch (e: Exception) {
        0
    }
}
