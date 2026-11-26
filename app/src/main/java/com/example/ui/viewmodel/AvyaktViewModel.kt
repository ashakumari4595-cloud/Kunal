package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.avatar.AvatarFaceAnchor
import com.example.avatar.PortraitFaceAnalyzer
import com.example.data.ai.AiRepository
import com.example.data.ai.FirebaseAiRepository
import com.example.data.local.AssistantLanguage
import com.example.data.local.AvatarVisualMode
import com.example.data.local.AvyaktDatabase
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationSessionEntity
import com.example.data.local.PreferencesRepository
import com.example.data.local.UserSettings
import com.example.data.remote.FirestoreChatRepository
import com.example.voice.AssistantVoiceState
import com.example.voice.VoiceAssistantEngine
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class AvyaktViewModel(
    application: Application,
    private val chatDao: ChatDao,
    private val preferencesRepository: PreferencesRepository,
    private val aiRepository: AiRepository,
    private val firestoreChatRepository: FirestoreChatRepository? = null
) : AndroidViewModel(application) {

    val userSettings: StateFlow<UserSettings> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserSettings()
        )

    val conversations: StateFlow<List<ConversationSessionEntity>> =
        chatDao.observeAllConversations()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private val _activeSessionId = MutableStateFlow(-1L)
    val activeSessionId: StateFlow<Long> = _activeSessionId.asStateFlow()

    val messages: StateFlow<List<ChatMessageEntity>> = _activeSessionId
        .flatMapLatest { id ->
            if (id <= 0L) flowOf(emptyList()) else chatDao.observeMessagesForSession(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isCameraActive = MutableStateFlow(false)
    val isCameraActive: StateFlow<Boolean> = _isCameraActive.asStateFlow()

    private val _useFrontCamera = MutableStateFlow(false)
    val useFrontCamera: StateFlow<Boolean> = _useFrontCamera.asStateFlow()

    private val _faceAnchor = MutableStateFlow(AvatarFaceAnchor())
    val faceAnchor: StateFlow<AvatarFaceAnchor> = _faceAnchor.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    private val _aiErrorMessage = MutableStateFlow<String?>(null)
    val aiErrorMessage: StateFlow<String?> = _aiErrorMessage.asStateFlow()

    private var lastPromptForRetry: String? = null
    private var lastAttachCameraForRetry: Boolean = false
    private var lastFromVoiceForRetry: Boolean = false

    @Volatile
    private var latestCameraBitmap: Bitmap? = null

    private val voiceEngine = VoiceAssistantEngine(
        context = application,
        scope = viewModelScope,
        onSpeechRecognized = { spokenText ->
            sendUserMessage(spokenText, attachCameraIfActive = true, fromVoice = true)
        },
        onVoiceError = { errorMsg ->
            _bannerMessage.value = errorMsg
        }
    )

    val voiceState: StateFlow<AssistantVoiceState> = voiceEngine.voiceState
    val micAmplitude: StateFlow<Float> = voiceEngine.micAmplitude
    val speechAmplitude: StateFlow<Float> = voiceEngine.speechAmplitude
    val partialTranscript: StateFlow<String> = voiceEngine.partialTranscript
    val isMuted: StateFlow<Boolean> = voiceEngine.isMuted
    val isSpeechRecognitionSupported: Boolean
        get() = voiceEngine.isSpeechRecognitionSupported

    val isLiveGeminiConnected: Boolean
        get() = aiRepository.isLiveGeminiConnected

    init {
        viewModelScope.launch {
            val savedSettings = preferencesRepository.settingsFlow.first()
            if (savedSettings.activeSessionId > 0L) {
                val existing = chatDao.getConversationById(savedSettings.activeSessionId)
                if (existing != null) {
                    _activeSessionId.value = existing.id
                }
            }
            if (_activeSessionId.value <= 0L) {
                val latest = chatDao.observeAllConversations().first().firstOrNull()
                if (latest != null) {
                    _activeSessionId.value = latest.id
                    preferencesRepository.setActiveSessionId(latest.id)
                }
            }
            refreshFaceAnchor(savedSettings.customPortraitPath)
        }
    }

    private fun refreshFaceAnchor(customPortraitPath: String?) {
        viewModelScope.launch {
            _faceAnchor.value = PortraitFaceAnalyzer.analyzePortrait(
                context = getApplication(),
                customPortraitPath = customPortraitPath,
                fallbackDrawableRes = R.drawable.default_avatar_portrait
            )
        }
    }

    fun clearBannerMessage() {
        _bannerMessage.value = null
    }

    fun showBannerMessage(message: String) {
        _bannerMessage.value = message
    }

    fun dismissAiError() {
        _aiErrorMessage.value = null
    }

    fun retryLastMessage() {
        val prompt = lastPromptForRetry ?: return
        _aiErrorMessage.value = null
        sendUserMessage(
            rawText = prompt,
            attachCameraIfActive = lastAttachCameraForRetry,
            fromVoice = lastFromVoiceForRetry
        )
    }

    fun onMicButtonPressed() {
        when (voiceEngine.voiceState.value) {
            AssistantVoiceState.LISTENING -> voiceEngine.stopListening()
            AssistantVoiceState.SPEAKING -> {
                // Barge-in: interrupt AVYAKT and immediately start listening
                voiceEngine.startListening(userSettings.value.language)
            }
            AssistantVoiceState.THINKING -> {
                _bannerMessage.value = "AVYAKT is thinking…"
            }
            AssistantVoiceState.IDLE,
            AssistantVoiceState.MUTED -> {
                voiceEngine.startListening(userSettings.value.language)
            }
        }
    }

    fun toggleMute() {
        voiceEngine.toggleMute()
    }

    fun endVoiceConversation() {
        voiceEngine.endConversationSession()
    }

    fun interruptSpeaking() {
        voiceEngine.interruptSpeech()
    }

    fun speakMessageAloud(text: String) {
        val settings = userSettings.value
        voiceEngine.speak(
            text = text,
            language = settings.language,
            speechRate = settings.speechRate,
            speechPitch = settings.speechPitch
        )
    }

    fun setCameraActive(active: Boolean) {
        _isCameraActive.value = active
        if (!active) {
            latestCameraBitmap = null
        }
    }

    fun toggleCameraLens() {
        _useFrontCamera.value = !_useFrontCamera.value
    }

    fun updateLatestCameraFrame(bitmap: Bitmap) {
        latestCameraBitmap = bitmap
    }

    fun analyzeCurrentCameraView() {
        if (!_isCameraActive.value) {
            _bannerMessage.value = "Turn on the camera first so AVYAKT can see."
            return
        }
        val frame = latestCameraBitmap
        if (frame == null) {
            _bannerMessage.value = "Camera is warming up. Please wait a second and try again."
            return
        }
        val prompt = if (userSettings.value.language == AssistantLanguage.HINDI) {
            "मेरे कैमरे में अभी क्या दिख रहा है?"
        } else {
            "What do you see in my camera right now?"
        }
        sendUserMessage(prompt, attachCameraIfActive = true, fromVoice = false)
    }

    fun sendUserMessage(
        rawText: String,
        attachCameraIfActive: Boolean = false,
        fromVoice: Boolean = false
    ) {
        val cleanText = rawText.trim()
        if (cleanText.isBlank()) return

        lastPromptForRetry = cleanText
        lastAttachCameraForRetry = attachCameraIfActive
        lastFromVoiceForRetry = fromVoice
        _aiErrorMessage.value = null

        viewModelScope.launch {
            _isGenerating.value = true
            voiceEngine.setThinking()
            try {
                val sessionId = ensureActiveSessionId(cleanText)
                val historySnapshot = chatDao.getMessagesForSessionOnce(sessionId)

                val userMsg = ChatMessageEntity(
                    sessionId = sessionId,
                    isFromUser = true,
                    text = cleanText,
                    timestampMillis = System.currentTimeMillis()
                )
                chatDao.insertMessage(userMsg)
                updateSessionHeader(sessionId, historySnapshot.isEmpty(), cleanText)

                val frameForVision = if (_isCameraActive.value &&
                    (attachCameraIfActive || isVisualQuestion(cleanText))
                ) {
                    latestCameraBitmap
                } else {
                    null
                }

                val settings = userSettings.value
                val reply = withTimeout(60_000L) {
                    aiRepository.generateReply(
                        history = historySnapshot,
                        userInput = cleanText,
                        language = settings.language,
                        cameraBitmap = frameForVision
                    )
                }

                val assistantMsg = ChatMessageEntity(
                    sessionId = sessionId,
                    isFromUser = false,
                    text = reply.text,
                    timestampMillis = System.currentTimeMillis(),
                    isPlaceholderResponse = reply.isPlaceholder,
                    pictureUrl = reply.picture?.imageUrl,
                    pictureCaption = reply.picture?.let { "${it.title} — ${it.caption}" },
                    pictureSourceUrl = reply.picture?.sourcePageUrl,
                    cameraSnapshotNote = reply.cameraSnapshotNote
                )
                chatDao.insertMessage(assistantMsg)
                updateSessionHeader(sessionId, isFirstUserMessage = false, preview = reply.text)
                _aiErrorMessage.value = null

                if (fromVoice || settings.autoSpeakReplies) {
                    voiceEngine.speak(
                        text = reply.text,
                        language = settings.language,
                        speechRate = settings.speechRate,
                        speechPitch = settings.speechPitch
                    )
                } else {
                    voiceEngine.setIdleIfNotSpeaking()
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Assistant reply timed out", e)
                val msg = "Gemini AI request timed out after 60 seconds. Please check internet connection and retry."
                _aiErrorMessage.value = msg
                _bannerMessage.value = msg
                voiceEngine.setIdleIfNotSpeaking()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process message", e)
                val detail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: e::class.java.simpleName
                val msg = "AVYAKT AI could not reach Gemini: $detail"
                _aiErrorMessage.value = msg
                _bannerMessage.value = msg
                voiceEngine.setIdleIfNotSpeaking()
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private suspend fun ensureActiveSessionId(initialTitleSeed: String): Long {
        val current = _activeSessionId.value
        if (current > 0L && chatDao.getConversationById(current) != null) {
            return current
        }
        val title = initialTitleSeed.take(36).ifBlank { "New Conversation" }
        val now = System.currentTimeMillis()
        val newId = chatDao.insertConversation(
            ConversationSessionEntity(
                title = title,
                lastMessagePreview = initialTitleSeed.take(80),
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        _activeSessionId.value = newId
        preferencesRepository.setActiveSessionId(newId)
        return newId
    }

    private suspend fun updateSessionHeader(
        sessionId: Long,
        isFirstUserMessage: Boolean,
        preview: String
    ) {
        val existing = chatDao.getConversationById(sessionId) ?: return
        val newTitle = if (isFirstUserMessage || existing.title == "New Conversation") {
            preview.take(38).trim().ifBlank { "Conversation" }
        } else {
            existing.title
        }
        chatDao.updateConversationSummary(
            sessionId = sessionId,
            title = newTitle,
            preview = preview.replace("\n", " ").take(90),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun startNewChat() {
        viewModelScope.launch {
            voiceEngine.endConversationSession()
            val now = System.currentTimeMillis()
            val newId = chatDao.insertConversation(
                ConversationSessionEntity(
                    title = "New Conversation",
                    lastMessagePreview = "",
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
            _activeSessionId.value = newId
            preferencesRepository.setActiveSessionId(newId)
        }
    }

    fun openConversation(sessionId: Long) {
        viewModelScope.launch {
            voiceEngine.endConversationSession()
            _activeSessionId.value = sessionId
            preferencesRepository.setActiveSessionId(sessionId)
        }
    }

    fun deleteConversation(sessionId: Long) {
        viewModelScope.launch {
            chatDao.deleteConversationById(sessionId)
            if (_activeSessionId.value == sessionId) {
                val remaining = chatDao.observeAllConversations().first().firstOrNull()
                val nextId = remaining?.id ?: -1L
                _activeSessionId.value = nextId
                preferencesRepository.setActiveSessionId(nextId)
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            voiceEngine.endConversationSession()
            chatDao.deleteAllConversations()
            _activeSessionId.value = -1L
            preferencesRepository.setActiveSessionId(-1L)
        }
    }

    fun updatePortraitFromUri(uri: Uri) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                try {
                    val appCtx = getApplication<Application>()
                    val input = appCtx.contentResolver.openInputStream(uri) ?: return@withContext null
                    val decoded = input.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    } ?: return@withContext null

                    val maxDim = 1024
                    val scale = minOf(
                        maxDim.toFloat() / decoded.width.coerceAtLeast(1),
                        maxDim.toFloat() / decoded.height.coerceAtLeast(1),
                        1f
                    )
                    val targetW = (decoded.width * scale).toInt().coerceAtLeast(1)
                    val targetH = (decoded.height * scale).toInt().coerceAtLeast(1)
                    val scaled = if (scale < 1f) {
                        Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                            decoded.recycle()
                        }
                    } else {
                        decoded
                    }

                    val outFile = File(appCtx.filesDir, "custom_avatar_${System.currentTimeMillis()}.jpg")
                    // Clean up older avatar files
                    appCtx.filesDir.listFiles { f -> f.name.startsWith("custom_avatar_") }
                        ?.forEach { it.delete() }

                    FileOutputStream(outFile).use { fos ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                    }
                    scaled.recycle()
                    outFile.absolutePath
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save portrait image", e)
                    null
                }
            }

            if (savedPath != null) {
                preferencesRepository.setCustomPortraitPath(savedPath)
                refreshFaceAnchor(savedPath)
                _bannerMessage.value = "Personal avatar portrait updated."
            } else {
                _bannerMessage.value = "Could not load selected portrait photo."
            }
        }
    }

    fun resetDefaultPortrait() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                getApplication<Application>().filesDir
                    .listFiles { f -> f.name.startsWith("custom_avatar_") }
                    ?.forEach { it.delete() }
            }
            preferencesRepository.setCustomPortraitPath(null)
            refreshFaceAnchor(null)
            _bannerMessage.value = "Restored your default portrait."
        }
    }

    fun setAvatarVisualMode(mode: AvatarVisualMode) {
        viewModelScope.launch { preferencesRepository.setAvatarVisualMode(mode) }
    }

    fun toggleAvatarVisualMode() {
        val next = if (userSettings.value.avatarVisualMode == AvatarVisualMode.PHOTO_PORTRAIT) {
            AvatarVisualMode.ANIMATED_CHARACTER
        } else {
            AvatarVisualMode.PHOTO_PORTRAIT
        }
        setAvatarVisualMode(next)
    }

    fun setLanguage(language: AssistantLanguage) {
        viewModelScope.launch { preferencesRepository.setLanguage(language) }
    }

    fun cycleLanguage() {
        val all = AssistantLanguage.entries
        val next = all[(all.indexOf(userSettings.value.language) + 1) % all.size]
        setLanguage(next)
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch { preferencesRepository.setSpeechRate(rate) }
    }

    fun setSpeechPitch(pitch: Float) {
        viewModelScope.launch { preferencesRepository.setSpeechPitch(pitch) }
    }

    fun setAutoSpeakReplies(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoSpeakReplies(enabled) }
    }

    fun testAssistantVoice() {
        val settings = userSettings.value
        val sample = when (settings.language) {
            AssistantLanguage.HINDI -> "नमस्ते! मैं अव्यक्त हूँ, आपका पर्सनल एआई असिस्टेंट।"
            AssistantLanguage.ENGLISH -> "Hello! I am AVYAKT, your personal AI assistant."
            AssistantLanguage.AUTO -> "नमस्ते! I am AVYAKT, ready to talk in Hindi and English."
        }
        voiceEngine.speak(
            text = sample,
            language = settings.language,
            speechRate = settings.speechRate,
            speechPitch = settings.speechPitch
        )
    }

    private fun isVisualQuestion(text: String): Boolean {
        val lower = text.lowercase()
        val keywords = listOf(
            "see", "look", "camera", "what is this", "identify", "around me",
            "देख", "दिख", "क्या है", "कैमरा", "पहचान"
        )
        return keywords.any { lower.contains(it) }
    }

    fun syncLocalConversationsToCloud() {
        val repo = firestoreChatRepository
        if (repo == null) {
            _bannerMessage.value = "Cloud sync is not configured."
            return
        }
        if (Firebase.auth.currentUser == null) {
            _bannerMessage.value = "Please sign in with Google first to sync conversations."
            return
        }
        if (_isSyncingCloud.value) return

        viewModelScope.launch {
            _isSyncingCloud.value = true
            try {
                val localConversations = chatDao.observeAllConversations().first()
                if (localConversations.isEmpty()) {
                    _bannerMessage.value = "No local conversations to sync yet."
                    return@launch
                }
                var syncedCount = 0
                for (session in localConversations) {
                    val convResult = repo.createConversation(
                        title = session.title,
                        initialPreview = session.lastMessagePreview
                    )
                    val remoteId = convResult.getOrElse { err ->
                        _bannerMessage.value = err.localizedMessage ?: "Cloud sync failed."
                        return@launch
                    }
                    val localMessages = chatDao.getMessagesForSessionOnce(session.id)
                    for (msg in localMessages) {
                        val msgResult = repo.addMessage(
                            conversationId = remoteId,
                            fromUser = msg.isFromUser,
                            text = msg.text,
                            placeholderResponse = msg.isPlaceholderResponse,
                            pictureUrl = msg.pictureUrl,
                            pictureCaption = msg.pictureCaption,
                            pictureSourceUrl = msg.pictureSourceUrl,
                            cameraSnapshotNote = msg.cameraSnapshotNote
                        )
                        if (msgResult.isFailure) {
                            _bannerMessage.value = msgResult.exceptionOrNull()?.localizedMessage
                                ?: "Failed to sync message."
                            return@launch
                        }
                    }
                    syncedCount++
                }
                _bannerMessage.value = "Synced $syncedCount conversation(s) to Firestore."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Cloud sync error", e)
                _bannerMessage.value = e.localizedMessage ?: "Cloud sync failed."
            } finally {
                _isSyncingCloud.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.release()
        latestCameraBitmap = null
    }

    companion object {
        private const val TAG = "AvyaktViewModel"

        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (FirebaseApp.getApps(application).isEmpty()) {
                        FirebaseApp.initializeApp(application)
                    }
                    val db = AvyaktDatabase.getInstance(application)
                    val prefs = PreferencesRepository(application)
                    val aiRepo = FirebaseAiRepository()
                    val firestoreRepo = FirestoreChatRepository.create(
                        databaseId = application.getString(R.string.firestore_database_id)
                    )
                    return AvyaktViewModel(
                        application = application,
                        chatDao = db.chatDao(),
                        preferencesRepository = prefs,
                        aiRepository = aiRepo,
                        firestoreChatRepository = firestoreRepo
                    ) as T
                }
            }
    }
}
