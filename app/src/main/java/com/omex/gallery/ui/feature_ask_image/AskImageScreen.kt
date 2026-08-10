package com.omex.gallery.ui.feature_ask_image

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.omex.gallery.R
import com.omex.gallery.core.ai.multimodal.AskImageMessage
import com.omex.gallery.ui.theme.AmberAccent
import com.omex.gallery.ui.theme.CyanAccent
import com.omex.gallery.ui.theme.ObsidianBg
import com.omex.gallery.ui.theme.SurfaceCard
import com.omex.gallery.ui.theme.SurfaceDark
import com.omex.gallery.ui.theme.TextMutedDark
import com.omex.gallery.ui.theme.TextPrimaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskImageScreen(
    viewModel: AskImageViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val modelInfo by viewModel.modelInfo.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            viewModel.importModel(context, uri)
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        uiState.mediaItem?.let { item ->
                            AsyncImage(
                                model = item.uriString,
                                contentDescription = item.fileName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, CyanAccent, RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.ask_image_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.ask_image_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedDark,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("ask_image_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    if (uiState.messages.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearMessages() },
                            modifier = Modifier.testTag("ask_image_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.clear_chat_history),
                                tint = TextMutedDark
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
            )
        },
        containerColor = ObsidianBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBg)
        ) {
            // Model Status Banner
            ModelStatusBanner(
                isInstalled = modelInfo.isInstalled,
                isValidated = modelInfo.isValidated,
                statusMessage = modelInfo.statusMessage,
                isImporting = uiState.isImportingModel,
                modelPath = modelInfo.modelDirectory,
                onImportClick = {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    filePickerLauncher.launch(Intent.createChooser(intent, "اختر ملف نموذج Gemma 3n (.litertlm)"))
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Error Banner if import or engine error occurs
            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Messages List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.messages.isEmpty()) {
                    // Quick Suggestions Empty State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "استفسر من الذكاء الاصطناعي المحلي حول تفاصيل هذه الصورة",
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.ai_chat_disclaimer),
                            color = TextMutedDark,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        // Quick suggestion pills
                        val suggestions = listOf(
                            stringResource(R.string.suggestion_describe_image),
                            stringResource(R.string.suggestion_main_objects),
                            stringResource(R.string.suggestion_extract_text)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            suggestions.forEach { suggestion ->
                                SuggestionChip(
                                    onClick = {
                                        inputText = suggestion
                                        viewModel.sendMessage(context, suggestion)
                                    },
                                    label = { Text(suggestion, color = CyanAccent, fontSize = 13.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = SurfaceCard),
                                    border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = CyanAccent.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.messages) { msg ->
                            AskImageMessageBubble(
                                message = msg,
                                isModelReady = modelInfo.isInstalled && modelInfo.isValidated
                            )
                        }
                    }
                }
            }

            // Input Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.ask_image_input_placeholder),
                                color = TextMutedDark,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ask_image_input_field")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (uiState.isGenerating) {
                        IconButton(
                            onClick = { viewModel.cancelGeneration() },
                            modifier = Modifier
                                .background(Color.Red.copy(alpha = 0.2f), CircleShape)
                                .testTag("ask_image_stop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = stringResource(R.string.stop_generation),
                                tint = Color.Red
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val prompt = inputText
                                    inputText = ""
                                    viewModel.sendMessage(context, prompt)
                                }
                            },
                            enabled = inputText.isNotBlank(),
                            modifier = Modifier
                                .background(
                                    if (inputText.isNotBlank()) CyanAccent else SurfaceCard,
                                    CircleShape
                                )
                                .testTag("ask_image_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank()) ObsidianBg else TextMutedDark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelStatusBanner(
    isInstalled: Boolean,
    isValidated: Boolean,
    statusMessage: String,
    isImporting: Boolean,
    modelPath: String,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReady = isInstalled && isValidated
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isReady) Color(0xFF1B382B) else Color(0xFF3D2C1D)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (isReady) Icons.Default.AutoAwesome else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isReady) Color(0xFF4CAF50) else AmberAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isReady) stringResource(R.string.litert_model_installed_ready) else if (isInstalled && !isValidated) "Invalid or incompatible LiteRT-LM model." else stringResource(R.string.litert_model_not_installed),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    if (!isReady) {
                        Text(
                            text = if (statusMessage.isNotEmpty()) statusMessage else stringResource(R.string.ai_model_info_msg, modelPath),
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            if (!isReady) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onImportClick,
                    enabled = !isImporting,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_litert_model_button")
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = ObsidianBg,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري استيراد .litertlm...", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.import_litert_model_button), color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AskImageMessageBubble(
    message: AskImageMessage,
    isModelReady: Boolean = false
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            // Badge Indicator
            if (!isUser) {
                val badgeText = if (message.isMultimodalReal || isModelReady) stringResource(R.string.real_multimodal_badge) else stringResource(R.string.fallback_vision_badge)
                val badgeColor = if (message.isMultimodalReal || isModelReady) Color(0xFF2E7D32) else Color(0xFFE65100)
                Box(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .background(badgeColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) CyanAccent else SurfaceCard
                ),
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text = message.content,
                    color = if (isUser) ObsidianBg else TextPrimaryDark,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
