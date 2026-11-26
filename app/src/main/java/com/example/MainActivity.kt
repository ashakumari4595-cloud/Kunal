package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.auth.authStateFlow
import com.example.auth.signOutFromGoogleAndFirebase
import com.example.firebase.configureAppCheck
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AvyaktTheme
import com.example.ui.viewmodel.AvyaktViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

object AvyaktRoutes {
    const val ASSISTANT = "assistant"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val factory = AvyaktViewModel.provideFactory(application)

        setContent {
            AvyaktTheme {
                AvyaktApp(viewModelFactory = factory)
            }
        }
    }
}

@Composable
fun AvyaktApp(
    viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val viewModel: AvyaktViewModel = viewModel(factory = viewModelFactory)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val credentialManager = androidx.compose.runtime.remember(context) {
        CredentialManager.create(context)
    }

    val currentUser by androidx.compose.runtime.remember {
        Firebase.auth.authStateFlow()
    }.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val faceAnchor by viewModel.faceAnchor.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val micAmplitude by viewModel.micAmplitude.collectAsStateWithLifecycle()
    val speechAmplitude by viewModel.speechAmplitude.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val isCameraActive by viewModel.isCameraActive.collectAsStateWithLifecycle()
    val useFrontCamera by viewModel.useFrontCamera.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val isSyncingCloud by viewModel.isSyncingCloud.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = AvyaktRoutes.ASSISTANT
    ) {
        composable(AvyaktRoutes.ASSISTANT) {
            AssistantScreen(
                messages = messages,
                userSettings = userSettings,
                faceAnchor = faceAnchor,
                voiceState = voiceState,
                micAmplitude = micAmplitude,
                speechAmplitude = speechAmplitude,
                partialTranscript = partialTranscript,
                isMuted = isMuted,
                isGenerating = isGenerating,
                isCameraActive = isCameraActive,
                useFrontCamera = useFrontCamera,
                isLiveGeminiConnected = viewModel.isLiveGeminiConnected,
                bannerMessage = bannerMessage,
                onClearBanner = viewModel::clearBannerMessage,
                onShowMessage = viewModel::showBannerMessage,
                onSendMessage = { text -> viewModel.sendUserMessage(text) },
                onMicClick = viewModel::onMicButtonPressed,
                onToggleMute = viewModel::toggleMute,
                onEndVoice = viewModel::endVoiceConversation,
                onSetCameraActive = viewModel::setCameraActive,
                onToggleCameraLens = viewModel::toggleCameraLens,
                onCameraFrameSampled = viewModel::updateLatestCameraFrame,
                onAnalyzeCameraView = viewModel::analyzeCurrentCameraView,
                onSpeakAloud = viewModel::speakMessageAloud,
                onCycleLanguage = viewModel::cycleLanguage,
                onToggleAvatarMode = viewModel::toggleAvatarVisualMode,
                onNewChat = viewModel::startNewChat,
                onOpenHistory = { navController.navigate(AvyaktRoutes.HISTORY) },
                onOpenSettings = { navController.navigate(AvyaktRoutes.SETTINGS) }
            )
        }

        composable(AvyaktRoutes.HISTORY) {
            HistoryScreen(
                conversations = conversations,
                activeSessionId = activeSessionId,
                onBack = { navController.popBackStack() },
                onNewChat = {
                    viewModel.startNewChat()
                    navController.popBackStack()
                },
                onSelectConversation = { sessionId ->
                    viewModel.openConversation(sessionId)
                    navController.popBackStack()
                },
                onDeleteConversation = viewModel::deleteConversation,
                onClearAll = viewModel::clearAllConversations
            )
        }

        composable(AvyaktRoutes.SETTINGS) {
            SettingsScreen(
                userSettings = userSettings,
                isLiveGeminiConnected = viewModel.isLiveGeminiConnected,
                signedInUserEmail = currentUser?.email,
                signedInUserName = currentUser?.displayName,
                isSyncingCloud = isSyncingCloud,
                onBack = { navController.popBackStack() },
                onPortraitPicked = viewModel::updatePortraitFromUri,
                onResetPortrait = viewModel::resetDefaultPortrait,
                onAvatarModeSelected = viewModel::setAvatarVisualMode,
                onLanguageSelected = viewModel::setLanguage,
                onSpeechRateChanged = viewModel::setSpeechRate,
                onSpeechPitchChanged = viewModel::setSpeechPitch,
                onAutoSpeakChanged = viewModel::setAutoSpeakReplies,
                onTestVoice = viewModel::testAssistantVoice,
                onSyncCloudNow = viewModel::syncLocalConversationsToCloud,
                onSignOut = {
                    signOutFromGoogleAndFirebase(
                        credentialManager = credentialManager,
                        scope = scope,
                        onSignOutComplete = {
                            scope.launch {
                                viewModel.showBannerMessage("Signed out of Google account.")
                            }
                        }
                    )
                },
                onAuthMessage = viewModel::showBannerMessage
            )
        }
    }
}
