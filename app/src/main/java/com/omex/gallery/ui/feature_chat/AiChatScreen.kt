package com.omex.gallery.ui.feature_chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileUpload
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omex.gallery.R
import com.omex.gallery.core.data.local.ChatMessageEntity
import com.omex.gallery.ui.theme.AmberAccent
import com.omex.gallery.ui.theme.ObsidianBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val modelInfo by viewModel.modelInfo.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            viewModel.importModel(context, uri) { success, msg ->
                isImporting = false
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Auto-scroll to bottom on new message or stream chunk
    LaunchedEffect(messages.size, uiState.streamingResponse) {
        val totalCount = messages.size + if (uiState.streamingResponse.isNotEmpty()) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ai_chat_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.createNewSession() },
                        modifier = Modifier.testTag("new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.new_chat),
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearCurrentConversation() },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.clear_chat_history),
                            tint = Color.Gray
                        )
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
        ) {
            // Model Status Banner
            ModelStatusBanner(
                isInstalled = modelInfo.isInstalled,
                isValidated = modelInfo.isValidated,
                statusMessage = modelInfo.statusMessage,
                isImporting = isImporting,
                modelPath = modelInfo.modelDirectory + "/" + modelInfo.modelFileName,
                onImportClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageBubble(
                        message = msg,
                        isModelReady = modelInfo.isInstalled && modelInfo.isValidated
                    )
                }

                // Streaming / Generating chunk
                if (uiState.isGenerating && uiState.streamingResponse.isNotEmpty()) {
                    item {
                        ChatMessageBubble(
                            message = ChatMessageEntity(
                                sessionId = uiState.currentSessionId ?: 0,
                                role = "assistant",
                                content = uiState.streamingResponse
                            ),
                            isModelReady = modelInfo.isInstalled && modelInfo.isValidated
                        )
                    }
                } else if (uiState.isGenerating && uiState.streamingResponse.isEmpty()) {
                    item {
                        GeneratingIndicator()
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A20)),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.ai_chat_disclaimer),
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    stringResource(R.string.ai_chat_input_placeholder),
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = false,
                            maxLines = 4,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberAccent,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedContainerColor = Color(0xFF22222A),
                                unfocusedContainerColor = Color(0xFF1E1E26),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_chat_input_field")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (uiState.isGenerating) {
                            IconButton(
                                onClick = { viewModel.cancelGeneration() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Red.copy(alpha = 0.8f))
                                    .testTag("stop_generation_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = stringResource(R.string.stop_generation),
                                    tint = Color.White
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val prompt = inputText
                                    inputText = ""
                                    viewModel.sendMessage(prompt)
                                },
                                enabled = inputText.isNotBlank(),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (inputText.isNotBlank()) AmberAccent else Color.DarkGray)
                                    .testTag("send_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (inputText.isNotBlank()) Color.Black else Color.Gray
                                )
                            }
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
                        text = if (isReady) stringResource(R.string.ai_model_installed_ready) else if (isInstalled && !isValidated) "Invalid or incompatible AI model." else stringResource(R.string.ai_model_not_installed),
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_model_button")
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري استيراد وتحقق النموذج...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.import_model_button), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessageEntity,
    isModelReady: Boolean = false
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AmberAccent.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AmberAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            // Badge Indicator
            if (!isUser) {
                val badgeText = if (isModelReady) stringResource(R.string.real_local_model_badge) else stringResource(R.string.demo_fallback_badge)
                val badgeColor = if (isModelReady) Color(0xFF2E7D32) else Color(0xFFE65100)
                Box(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor)
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
                    containerColor = if (isUser) AmberAccent else Color(0xFF24242C)
                ),
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .testTag("chat_bubble_${message.id}")
            ) {
                Text(
                    text = message.content,
                    color = if (isUser) Color.Black else Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun GeneratingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        CircularProgressIndicator(
            color = AmberAccent,
            strokeWidth = 2.dp,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "جارٍ التوليد محلياً...",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}
