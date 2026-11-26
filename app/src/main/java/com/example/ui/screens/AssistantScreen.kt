package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.avatar.AvatarFaceAnchor
import com.example.data.local.AssistantLanguage
import com.example.data.local.ChatMessageEntity
import com.example.data.local.UserSettings
import com.example.ui.components.CameraVisionCard
import com.example.ui.components.PortraitAvatarStage
import com.example.ui.theme.AbyssBlack
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.DeepAzure
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.GlassPanel
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MutedIce
import com.example.ui.theme.NeonOrchid
import com.example.voice.AssistantVoiceState

@Composable
fun AssistantScreen(
    messages: List<ChatMessageEntity>,
    userSettings: UserSettings,
    faceAnchor: AvatarFaceAnchor,
    voiceState: AssistantVoiceState,
    micAmplitude: Float,
    speechAmplitude: Float,
    partialTranscript: String,
    isMuted: Boolean,
    isGenerating: Boolean,
    isCameraActive: Boolean,
    useFrontCamera: Boolean,
    isLiveGeminiConnected: Boolean,
    bannerMessage: String?,
    onClearBanner: () -> Unit,
    onShowMessage: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onMicClick: () -> Unit,
    onToggleMute: () -> Unit,
    onEndVoice: () -> Unit,
    onSetCameraActive: (Boolean) -> Unit,
    onToggleCameraLens: () -> Unit,
    onCameraFrameSampled: (Bitmap) -> Unit,
    onAnalyzeCameraView: () -> Unit,
    onSpeakAloud: (String) -> Unit,
    onCycleLanguage: () -> Unit,
    onToggleAvatarMode: () -> Unit,
    onNewChat: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    var inputText by rememberSaveable { mutableStateOf("") }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onMicClick()
        } else {
            onShowMessage("Microphone permission is needed for voice conversation. Text chat is still available.")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onSetCameraActive(true)
        } else {
            onShowMessage("Camera permission denied. Enable it in Settings to use Live Camera Vision.")
        }
    }

    LaunchedEffect(bannerMessage) {
        if (!bannerMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(bannerMessage)
            onClearBanner()
        }
    }

    val handleMicTap = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            onMicClick()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val handleCameraToggle = {
        if (isCameraActive) {
            onSetCameraActive(false)
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                onSetCameraActive(true)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    Scaffold(
        containerColor = AbyssBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AbyssBlack,
                            Color(0xFF0A1128),
                            AbyssBlack
                        )
                    )
                )
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopHeaderBar(
                    isCameraActive = isCameraActive,
                    onNewChat = onNewChat,
                    onOpenHistory = onOpenHistory,
                    onOpenSettings = onOpenSettings
                )

                if (!isLiveGeminiConnected) {
                    PreviewModeNoticeBanner(
                        onClick = onOpenSettings,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val isWideScreen = maxWidth >= 700.dp

                    if (isWideScreen) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(0.45f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                AvatarAndCameraStage(
                                    userSettings = userSettings,
                                    faceAnchor = faceAnchor,
                                    voiceState = voiceState,
                                    micAmplitude = micAmplitude,
                                    speechAmplitude = speechAmplitude,
                                    partialTranscript = partialTranscript,
                                    isCameraActive = isCameraActive,
                                    useFrontCamera = useFrontCamera,
                                    onFlipCamera = onToggleCameraLens,
                                    onCloseCamera = { onSetCameraActive(false) },
                                    onFrameSampled = onCameraFrameSampled,
                                    onCameraError = { msg ->
                                        onSetCameraActive(false)
                                        onShowMessage(msg)
                                    },
                                    onToggleAvatarMode = onToggleAvatarMode
                                )
                                VoiceAndCameraControlRow(
                                    voiceState = voiceState,
                                    isMuted = isMuted,
                                    isCameraActive = isCameraActive,
                                    language = userSettings.language,
                                    onMicTap = handleMicTap,
                                    onToggleMute = onToggleMute,
                                    onEndVoice = onEndVoice,
                                    onCameraToggle = handleCameraToggle,
                                    onAnalyzeCamera = onAnalyzeCameraView,
                                    onCycleLanguage = onCycleLanguage
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(0.55f)
                                    .fillMaxHeight()
                            ) {
                                ChatConversationFeed(
                                    messages = messages,
                                    isGenerating = isGenerating,
                                    onSpeakAloud = onSpeakAloud,
                                    onQuickPrompt = { prompt -> onSendMessage(prompt) },
                                    modifier = Modifier.weight(1f)
                                )
                                TextComposerBar(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    isGenerating = isGenerating,
                                    onSend = {
                                        val msg = inputText
                                        inputText = ""
                                        focusManager.clearFocus()
                                        onSendMessage(msg)
                                    }
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AvatarAndCameraStage(
                                userSettings = userSettings,
                                faceAnchor = faceAnchor,
                                voiceState = voiceState,
                                micAmplitude = micAmplitude,
                                speechAmplitude = speechAmplitude,
                                partialTranscript = partialTranscript,
                                isCameraActive = isCameraActive,
                                useFrontCamera = useFrontCamera,
                                onFlipCamera = onToggleCameraLens,
                                onCloseCamera = { onSetCameraActive(false) },
                                onFrameSampled = onCameraFrameSampled,
                                onCameraError = { msg ->
                                    onSetCameraActive(false)
                                    onShowMessage(msg)
                                },
                                onToggleAvatarMode = onToggleAvatarMode,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            ChatConversationFeed(
                                messages = messages,
                                isGenerating = isGenerating,
                                onSpeakAloud = onSpeakAloud,
                                onQuickPrompt = { prompt -> onSendMessage(prompt) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .widthIn(max = 600.dp)
                            )

                            VoiceAndCameraControlRow(
                                voiceState = voiceState,
                                isMuted = isMuted,
                                isCameraActive = isCameraActive,
                                language = userSettings.language,
                                onMicTap = handleMicTap,
                                onToggleMute = onToggleMute,
                                onEndVoice = onEndVoice,
                                onCameraToggle = handleCameraToggle,
                                onAnalyzeCamera = onAnalyzeCameraView,
                                onCycleLanguage = onCycleLanguage,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )

                            TextComposerBar(
                                value = inputText,
                                onValueChange = { inputText = it },
                                isGenerating = isGenerating,
                                onSend = {
                                    val msg = inputText
                                    inputText = ""
                                    focusManager.clearFocus()
                                    onSendMessage(msg)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 600.dp)
                                    .padding(bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopHeaderBar(
    isCameraActive: Boolean,
    onNewChat: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ElectricCyan, LuminousViolet)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = AbyssBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.brand_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.brand_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedIce
                    )
                    if (isCameraActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(EmeraldLive)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CAM",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldLive,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(
                onClick = onNewChat,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("new_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddComment,
                    contentDescription = stringResource(R.string.nav_new_chat),
                    tint = ElectricCyan
                )
            }
            IconButton(
                onClick = onOpenHistory,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("history_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = stringResource(R.string.nav_history),
                    tint = Color.White
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.nav_settings),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun PreviewModeNoticeBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MidnightSurface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AmberNotice.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("preview_mode_notice")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = AmberNotice,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Preview Mode: Sample AI replies active (Firebase AI requires setup in AI Studio). Tap for details.",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFDE68A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AvatarAndCameraStage(
    userSettings: UserSettings,
    faceAnchor: AvatarFaceAnchor,
    voiceState: AssistantVoiceState,
    micAmplitude: Float,
    speechAmplitude: Float,
    partialTranscript: String,
    isCameraActive: Boolean,
    useFrontCamera: Boolean,
    onFlipCamera: () -> Unit,
    onCloseCamera: () -> Unit,
    onFrameSampled: (Bitmap) -> Unit,
    onCameraError: (String) -> Unit,
    onToggleAvatarMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        PortraitAvatarStage(
            customPortraitPath = userSettings.customPortraitPath,
            avatarVisualMode = userSettings.avatarVisualMode,
            faceAnchor = faceAnchor,
            voiceState = voiceState,
            micAmplitude = micAmplitude,
            speechAmplitude = speechAmplitude,
            partialTranscript = partialTranscript,
            onToggleAvatarMode = onToggleAvatarMode
        )

        AnimatedVisibility(
            visible = isCameraActive,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp, top = 4.dp)
        ) {
            CameraVisionCard(
                useFrontCamera = useFrontCamera,
                onFlipCamera = onFlipCamera,
                onCloseCamera = onCloseCamera,
                onFrameSampled = onFrameSampled,
                onCameraError = onCameraError,
                modifier = Modifier.size(width = 116.dp, height = 152.dp)
            )
        }
    }
}

@Composable
private fun ChatConversationFeed(
    messages: List<ChatMessageEntity>,
    isGenerating: Boolean,
    onSpeakAloud: (String) -> Unit,
    onQuickPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (messages.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.empty_chat_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.empty_chat_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedIce
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { onQuickPrompt("नमस्ते AVYAKT, कैसे हो?") },
                    label = { Text("नमस्ते AVYAKT") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = ElevatedSlate,
                        labelColor = ElectricCyan
                    ),
                    border = BorderStroke(1.dp, HairlineBorder)
                )
                AssistChip(
                    onClick = { onQuickPrompt("Show me a picture of Taj Mahal") },
                    label = { Text("Taj Mahal photo") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = ElevatedSlate,
                        labelColor = NeonOrchid
                    ),
                    border = BorderStroke(1.dp, HairlineBorder)
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatMessageBubble(
                    message = msg,
                    onSpeakAloud = { onSpeakAloud(msg.text) }
                )
            }
            if (isGenerating) {
                item(key = "typing_indicator") {
                    Surface(
                        color = ElevatedSlate,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, LuminousViolet.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "AVYAKT is thinking…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonOrchid,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessageEntity,
    onSpeakAloud: () -> Unit
) {
    val isUser = message.isFromUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) DeepAzure.copy(alpha = 0.85f) else ElevatedSlate,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isUser) ElectricCyan.copy(alpha = 0.45f) else HairlineBorder
            ),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!message.cameraSnapshotNote.isNullOrBlank()) {
                    Surface(
                        color = AbyssBlack.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "Vision: ${message.cameraSnapshotNote}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldLive,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )

                // Display existing picture ONLY when explicitly requested & attached
                if (!message.pictureUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = message.pictureUrl,
                        contentDescription = stringResource(R.string.cd_requested_picture),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AbyssBlack)
                    )
                    if (!message.pictureCaption.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message.pictureCaption,
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedIce
                        )
                    }
                }

                if (!isUser) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (message.isPlaceholderResponse) {
                            Text(
                                text = "Preview Sample",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberNotice
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        IconButton(
                            onClick = onSpeakAloud,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeUp,
                                contentDescription = "Speak reply aloud",
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceAndCameraControlRow(
    voiceState: AssistantVoiceState,
    isMuted: Boolean,
    isCameraActive: Boolean,
    language: AssistantLanguage,
    onMicTap: () -> Unit,
    onToggleMute: () -> Unit,
    onEndVoice: () -> Unit,
    onCameraToggle: () -> Unit,
    onAnalyzeCamera: () -> Unit,
    onCycleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute Mic
        FilledTonalIconButton(
            onClick = onToggleMute,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isMuted) CrimsonAlert.copy(alpha = 0.25f) else GlassPanel,
                contentColor = if (isMuted) CrimsonAlert else Color.White
            ),
            modifier = Modifier
                .size(48.dp)
                .testTag("mute_mic_button")
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                contentDescription = stringResource(
                    if (isMuted) R.string.action_unmute_mic else R.string.action_mute_mic
                )
            )
        }

        // Camera On/Off
        FilledTonalIconButton(
            onClick = onCameraToggle,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isCameraActive) EmeraldLive.copy(alpha = 0.22f) else GlassPanel,
                contentColor = if (isCameraActive) EmeraldLive else Color.White
            ),
            modifier = Modifier
                .size(48.dp)
                .testTag("camera_toggle_button")
        ) {
            Icon(
                imageVector = if (isCameraActive) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                contentDescription = stringResource(R.string.action_camera_toggle)
            )
        }

        // Large Center Mic / Interrupt Button
        val isListening = voiceState == AssistantVoiceState.LISTENING
        val isSpeaking = voiceState == AssistantVoiceState.SPEAKING
        Surface(
            onClick = onMicTap,
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(
                width = 2.dp,
                color = when {
                    isListening -> EmeraldLive
                    isSpeaking -> NeonOrchid
                    else -> ElectricCyan
                }
            ),
            modifier = Modifier
                .size(64.dp)
                .testTag("main_mic_button")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = when {
                                isListening -> listOf(EmeraldLive, CyberBlue)
                                isSpeaking -> listOf(LuminousViolet, NeonOrchid)
                                else -> listOf(CyberBlue, LuminousViolet)
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isListening -> Icons.Rounded.Stop
                        isSpeaking -> Icons.Rounded.Mic
                        else -> Icons.Rounded.Mic
                    },
                    contentDescription = stringResource(
                        when {
                            isListening -> R.string.action_mic_stop
                            isSpeaking -> R.string.action_mic_interrupt
                            else -> R.string.action_mic_start
                        }
                    ),
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        // See Camera Now (or Language cycle when camera is off)
        if (isCameraActive) {
            FilledTonalIconButton(
                onClick = onAnalyzeCamera,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = ElectricCyan.copy(alpha = 0.22f),
                    contentColor = ElectricCyan
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("analyze_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.CenterFocusStrong,
                    contentDescription = stringResource(R.string.action_analyze_view)
                )
            }
        } else {
            FilledTonalIconButton(
                onClick = onCycleLanguage,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = GlassPanel,
                    contentColor = ElectricCyan
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("language_cycle_button")
            ) {
                val shortCode = when (language) {
                    AssistantLanguage.AUTO -> "HI/EN"
                    AssistantLanguage.HINDI -> "हिन्दी"
                    AssistantLanguage.ENGLISH -> "ENG"
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = "Switch language",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = shortCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
            }
        }

        // End Voice Conversation
        FilledTonalIconButton(
            onClick = onEndVoice,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = CrimsonAlert.copy(alpha = 0.20f),
                contentColor = CrimsonAlert
            ),
            modifier = Modifier
                .size(48.dp)
                .testTag("end_voice_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.CallEnd,
                contentDescription = stringResource(R.string.action_end_voice)
            )
        }
    }
}

@Composable
private fun TextComposerBar(
    value: String,
    onValueChange: (String) -> Unit,
    isGenerating: Boolean,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.input_placeholder_bilingual),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedIce,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MidnightSurface,
                unfocusedContainerColor = MidnightSurface,
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = HairlineBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = ElectricCyan
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (value.isNotBlank() && !isGenerating) onSend()
                }
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("chat_text_input")
        )

        Surface(
            onClick = {
                if (value.isNotBlank() && !isGenerating) onSend()
            },
            shape = CircleShape,
            color = if (value.isNotBlank() && !isGenerating) CyberBlue else GlassPanel,
            modifier = Modifier
                .size(48.dp)
                .testTag("send_message_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = stringResource(R.string.action_send_message),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
