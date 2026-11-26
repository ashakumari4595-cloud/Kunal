package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.auth.onGoogleSignInClicked
import com.example.data.local.AssistantLanguage
import com.example.data.local.AvatarVisualMode
import com.example.data.local.UserSettings
import com.example.ui.theme.AbyssBlack
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MutedIce
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    userSettings: UserSettings,
    isLiveGeminiConnected: Boolean,
    signedInUserEmail: String? = null,
    signedInUserName: String? = null,
    isSyncingCloud: Boolean = false,
    onBack: () -> Unit,
    onPortraitPicked: (Uri) -> Unit,
    onResetPortrait: () -> Unit,
    onAvatarModeSelected: (AvatarVisualMode) -> Unit,
    onLanguageSelected: (AssistantLanguage) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    onSpeechPitchChanged: (Float) -> Unit,
    onAutoSpeakChanged: (Boolean) -> Unit,
    onTestVoice: () -> Unit,
    onSyncCloudNow: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onAuthMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isSigningIn by remember { mutableStateOf(false) }

    // Compliant Zero-Permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPortraitPicked(uri)
        }
    }

    val micGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
    val cameraGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val imageModel = remember(userSettings.customPortraitPath) {
        val file = userSettings.customPortraitPath?.let { File(it) }
        if (file != null && file.exists()) {
            ImageRequest.Builder(context).data(file).crossfade(true).build()
        } else {
            ImageRequest.Builder(context).data(R.drawable.default_avatar_portrait).crossfade(true).build()
        }
    }

    Scaffold(
        containerColor = AbyssBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AbyssBlack)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Personal AI Avatar Portrait
                SettingsCard(title = stringResource(R.string.settings_avatar_section)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = stringResource(R.string.cd_user_avatar),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 86.dp, height = 108.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.5.dp, ElectricCyan, RoundedCornerShape(18.dp))
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "AVYAKT uses your real portrait photo as your personal AI avatar without altering your identity.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedIce
                            )
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("change_portrait_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.settings_change_portrait))
                            }
                            if (!userSettings.customPortraitPath.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = onResetPortrait,
                                    border = BorderStroke(1.dp, HairlineBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reset_portrait_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Restore,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.settings_reset_portrait),
                                        color = ElectricCyan
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Avatar Visual Style",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AvatarVisualMode.entries.forEach { mode ->
                            val selected = userSettings.avatarVisualMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { onAvatarModeSelected(mode) },
                                label = { Text(mode.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = ElevatedSlate,
                                    labelColor = MutedIce
                                )
                            )
                        }
                    }
                }

                // 2. Voice & Language
                SettingsCard(title = stringResource(R.string.settings_voice_section)) {
                    Text(
                        text = "Conversation Language",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistantLanguage.entries.forEach { lang ->
                            val selected = userSettings.language == lang
                            FilterChip(
                                selected = selected,
                                onClick = { onLanguageSelected(lang) },
                                label = { Text(lang.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = ElevatedSlate,
                                    labelColor = MutedIce
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Speak Replies",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                            Text(
                                text = "Speak answers aloud with avatar lip-sync",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedIce
                            )
                        }
                        Switch(
                            checked = userSettings.autoSpeakReplies,
                            onCheckedChange = onAutoSpeakChanged,
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Speech Speed: ${"%.2f".format(userSettings.speechRate)}x",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Slider(
                        value = userSettings.speechRate,
                        onValueChange = onSpeechRateChanged,
                        valueRange = 0.7f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan
                        )
                    )

                    Text(
                        text = "Voice Pitch: ${"%.2f".format(userSettings.speechPitch)}x",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Slider(
                        value = userSettings.speechPitch,
                        onValueChange = onSpeechPitchChanged,
                        valueRange = 0.75f..1.35f,
                        colors = SliderDefaults.colors(
                            thumbColor = LuminousViolet,
                            activeTrackColor = LuminousViolet
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onTestVoice,
                        border = BorderStroke(1.dp, ElectricCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test AVYAKT Voice & Lip-Sync", color = ElectricCyan)
                    }
                }

                // 3. Privacy & Hardware Permissions
                SettingsCard(title = stringResource(R.string.settings_permissions_section)) {
                    PermissionStatusRow(
                        icon = Icons.Rounded.Mic,
                        title = "Microphone (Voice Chat)",
                        granted = micGranted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionStatusRow(
                        icon = Icons.Rounded.CameraAlt,
                        title = "Camera (Live Vision)",
                        granted = cameraGranted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "AVYAKT never uses the camera or microphone secretly. You can turn them off anytime.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedIce
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        },
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Android App Permissions", color = Color.White)
                    }
                }

                // 4. Google Account & Cloud Chat Sync
                SettingsCard(title = "Google Account & Cloud Sync") {
                    if (signedInUserEmail.isNullOrBlank()) {
                        Text(
                            text = "Sign in with Google to back up and sync your AVYAKT conversation history to Cloud Firestore.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedIce
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                isSigningIn = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isSigningIn = false
                                        onAuthMessage("Signed in with Google. Cloud sync ready.")
                                    },
                                    onAuthError = { msg ->
                                        isSigningIn = false
                                        onAuthMessage(msg)
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isSigningIn = false
                                    }
                                )
                            },
                            enabled = !isSigningIn,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_sign_in_button")
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (isSigningIn) "Signing in…" else "Sign in with Google")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = EmeraldLive,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = signedInUserName?.takeIf { it.isNotBlank() } ?: "Google Account Connected",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Text(
                                    text = signedInUserEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedIce
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onSyncCloudNow,
                            enabled = !isSyncingCloud,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sync_cloud_button")
                        ) {
                            if (isSyncingCloud) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isSyncingCloud) "Syncing Conversations…" else "Sync Chats to Cloud")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onSignOut,
                            border = BorderStroke(1.dp, HairlineBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = null,
                                tint = MutedIce,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out", color = Color.White)
                        }
                    }
                }

                // 5. Honest API Limits & Hindi Setup Guide
                SettingsCard(title = stringResource(R.string.settings_limits_section)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = if (isLiveGeminiConnected) EmeraldLive else AmberNotice,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLiveGeminiConnected) {
                                "Firebase AI Logic Connected (gemini-3.8-flash)"
                            } else {
                                "Preview Mode Active"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isLiveGeminiConnected) EmeraldLive else AmberNotice
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isLiveGeminiConnected) {
                            "सरल हिंदी जानकारी:\n" +
                                "1. अब AVYAKT में लाइव Firebase AI Logic (gemini-3.8-flash) और App Check सक्रिय है—हिंदी और English में स्मार्ट बातचीत व लाइव कैमरा विज़न अब सीधे Gemini से जुड़े हैं।\n" +
                                "2. सुरक्षित क्लाउड बैकअप के लिए आप ऊपर 'Sign in with Google' करके अपनी चैट हिस्ट्री Firestore में सिंक कर सकते हैं।\n" +
                                "3. ईमानदार सीमाएं: क्लाउड AI के मुफ्त टियर में दैनिक/प्रति-मिनट उपयोग कोटा होता है; सीमा पूरी होने पर ऐप बिना क्रैश हुए स्पष्ट सूचना दिखाएगा।"
                        } else {
                            "सरल हिंदी जानकारी:\n" +
                                "1. अभी ऐप Sample/Preview Mode में चल रहा है।\n" +
                                "2. लाइव Gemini AI के लिए AI Studio में Firebase AI Logic को Accept करें।"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedIce
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = MidnightSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = ElectricCyan
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun PermissionStatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    granted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
        Surface(
            color = (if (granted) EmeraldLive else AmberNotice).copy(alpha = 0.16f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (granted) "Granted" else "Tap Mic/Cam to Allow",
                style = MaterialTheme.typography.labelSmall,
                color = if (granted) EmeraldLive else AmberNotice,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
