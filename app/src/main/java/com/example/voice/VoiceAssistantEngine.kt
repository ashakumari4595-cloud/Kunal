package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.local.AssistantLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.math.sin

enum class AssistantVoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    MUTED
}

class VoiceAssistantEngine(
    context: Context,
    private val scope: CoroutineScope,
    private val onSpeechRecognized: (String) -> Unit,
    private val onVoiceError: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var lipSyncJob: Job? = null

    private val _voiceState = MutableStateFlow(AssistantVoiceState.IDLE)
    val voiceState: StateFlow<AssistantVoiceState> = _voiceState.asStateFlow()

    private val _micAmplitude = MutableStateFlow(0f)
    val micAmplitude: StateFlow<Float> = _micAmplitude.asStateFlow()

    private val _speechAmplitude = MutableStateFlow(0f)
    val speechAmplitude: StateFlow<Float> = _speechAmplitude.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    val isSpeechRecognitionSupported: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    init {
        textToSpeech = TextToSpeech(appContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    mainHandler.post {
                        _voiceState.value = AssistantVoiceState.SPEAKING
                        startLipSyncPulse()
                    }
                }

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    // Boost mouth openness on each new spoken word/syllable range
                    _speechAmplitude.value = (0.55f + ((end - start).coerceIn(1, 8) / 16f)).coerceIn(0.4f, 1.0f)
                }

                override fun onDone(utteranceId: String?) {
                    mainHandler.post {
                        stopLipSyncPulse()
                        _speechAmplitude.value = 0f
                        _voiceState.value = if (_isMuted.value) AssistantVoiceState.MUTED else AssistantVoiceState.IDLE
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    mainHandler.post {
                        stopLipSyncPulse()
                        _speechAmplitude.value = 0f
                        _voiceState.value = if (_isMuted.value) AssistantVoiceState.MUTED else AssistantVoiceState.IDLE
                    }
                }
            })
        } else {
            Log.w(TAG, "TextToSpeech init failed with status $status")
        }
    }

    fun startListening(language: AssistantLanguage) {
        mainHandler.post {
            if (_isMuted.value) {
                _isMuted.value = false
            }
            // Interrupt speaking immediately (barge-in support)
            stopSpeakingInternal()

            if (!isSpeechRecognitionSupported) {
                _voiceState.value = AssistantVoiceState.IDLE
                onVoiceError("On-device speech recognition service is unavailable on this device. Please use Text Chat below.")
                return@post
            }

            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _voiceState.value = AssistantVoiceState.LISTENING
                            _partialTranscript.value = ""
                        }

                        override fun onBeginningOfSpeech() {
                            _voiceState.value = AssistantVoiceState.LISTENING
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // Normalize SpeechRecognizer rmsdB (-2..10 typical) to 0..1
                            val norm = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
                            _micAmplitude.value = norm
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _micAmplitude.value = 0f
                            if (_voiceState.value == AssistantVoiceState.LISTENING) {
                                _voiceState.value = AssistantVoiceState.THINKING
                            }
                        }

                        override fun onError(error: Int) {
                            _micAmplitude.value = 0f
                            _partialTranscript.value = ""
                            _voiceState.value = if (_isMuted.value) AssistantVoiceState.MUTED else AssistantVoiceState.IDLE
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH,
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                    "Didn't catch any speech. Tap the mic and speak clearly."
                                SpeechRecognizer.ERROR_AUDIO ->
                                    "Microphone audio error. Please check mic permissions."
                                SpeechRecognizer.ERROR_NETWORK,
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                    "Speech service network issue. You can still type in Text Chat."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                    "Microphone permission is required for voice chat."
                                else -> "Voice input stopped (code $error). Try again or use Text Chat."
                            }
                            onVoiceError(msg)
                        }

                        override fun onResults(results: Bundle?) {
                            _micAmplitude.value = 0f
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val spoken = matches?.firstOrNull()?.trim().orEmpty()
                            _partialTranscript.value = ""
                            if (spoken.isNotBlank()) {
                                _voiceState.value = AssistantVoiceState.THINKING
                                onSpeechRecognized(spoken)
                            } else {
                                _voiceState.value = AssistantVoiceState.IDLE
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                .orEmpty()
                            _partialTranscript.value = partial
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.bcp47)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                _voiceState.value = AssistantVoiceState.LISTENING
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SpeechRecognizer", e)
                _voiceState.value = AssistantVoiceState.IDLE
                onVoiceError("Unable to start voice recognition on this device. Text Chat remains ready.")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            runCatching { speechRecognizer?.stopListening() }
            _micAmplitude.value = 0f
            _partialTranscript.value = ""
            if (_voiceState.value == AssistantVoiceState.LISTENING) {
                _voiceState.value = AssistantVoiceState.IDLE
            }
        }
    }

    fun toggleMute() {
        val nextMuted = !_isMuted.value
        _isMuted.value = nextMuted
        if (nextMuted) {
            mainHandler.post {
                runCatching { speechRecognizer?.cancel() }
                _micAmplitude.value = 0f
                _partialTranscript.value = ""
                if (_voiceState.value == AssistantVoiceState.LISTENING ||
                    _voiceState.value == AssistantVoiceState.IDLE
                ) {
                    _voiceState.value = AssistantVoiceState.MUTED
                }
            }
        } else if (_voiceState.value == AssistantVoiceState.MUTED) {
            _voiceState.value = AssistantVoiceState.IDLE
        }
    }

    fun endConversationSession() {
        mainHandler.post {
            runCatching { speechRecognizer?.cancel() }
            stopSpeakingInternal()
            _micAmplitude.value = 0f
            _speechAmplitude.value = 0f
            _partialTranscript.value = ""
            _voiceState.value = if (_isMuted.value) AssistantVoiceState.MUTED else AssistantVoiceState.IDLE
        }
    }

    fun setThinking() {
        if (_voiceState.value != AssistantVoiceState.SPEAKING) {
            _voiceState.value = AssistantVoiceState.THINKING
        }
    }

    fun setIdleIfNotSpeaking() {
        if (_voiceState.value != AssistantVoiceState.SPEAKING) {
            _voiceState.value = if (_isMuted.value) AssistantVoiceState.MUTED else AssistantVoiceState.IDLE
        }
    }

    fun speak(
        text: String,
        language: AssistantLanguage,
        speechRate: Float,
        speechPitch: Float
    ) {
        val cleanText = text
            .replace(Regex("\\[Preview Mode:.*?\\]"), "")
            .replace(Regex("\\[Sample Preview Response:.*?\\]"), "")
            .replace("**", "")
            .trim()
        if (cleanText.isBlank()) {
            setIdleIfNotSpeaking()
            return
        }

        mainHandler.post {
            if (!isTtsReady) {
                setIdleIfNotSpeaking()
                return@post
            }
            val locale = when {
                language == AssistantLanguage.HINDI ||
                    cleanText.any { it in '\u0900'..'\u097F' } -> Locale.forLanguageTag("hi-IN")
                language == AssistantLanguage.ENGLISH -> Locale.forLanguageTag("en-IN")
                else -> Locale.forLanguageTag("en-IN")
            }
            textToSpeech?.language = locale
            textToSpeech?.setSpeechRate(speechRate)
            textToSpeech?.setPitch(speechPitch)

            val utteranceId = UUID.randomUUID().toString()
            _voiceState.value = AssistantVoiceState.SPEAKING
            startLipSyncPulse()
            val result = textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result == TextToSpeech.ERROR) {
                stopLipSyncPulse()
                _speechAmplitude.value = 0f
                setIdleIfNotSpeaking()
            }
        }
    }

    fun interruptSpeech() {
        mainHandler.post {
            stopSpeakingInternal()
            _voiceState.value = if (_isMuted.value) AssistantVoiceState.MUTED else AssistantVoiceState.IDLE
        }
    }

    private fun stopSpeakingInternal() {
        runCatching { textToSpeech?.stop() }
        stopLipSyncPulse()
        _speechAmplitude.value = 0f
    }

    private fun startLipSyncPulse() {
        lipSyncJob?.cancel()
        lipSyncJob = scope.launch(Dispatchers.Default) {
            var phase = 0f
            while (isActive && _voiceState.value == AssistantVoiceState.SPEAKING) {
                // Smooth syllable envelope combined with UtteranceProgressListener range spikes
                val cadence = (0.35f + 0.40f * kotlin.math.abs(sin(phase))).coerceIn(0.15f, 0.95f)
                val current = _speechAmplitude.value
                _speechAmplitude.value = (current * 0.45f + cadence * 0.55f).coerceIn(0.1f, 1.0f)
                phase += 0.42f
                delay(45L)
            }
            _speechAmplitude.value = 0f
        }
    }

    private fun stopLipSyncPulse() {
        lipSyncJob?.cancel()
        lipSyncJob = null
    }

    fun release() {
        mainHandler.post {
            stopLipSyncPulse()
            runCatching { speechRecognizer?.destroy() }
            speechRecognizer = null
            runCatching {
                textToSpeech?.stop()
                textToSpeech?.shutdown()
            }
            textToSpeech = null
        }
    }

    companion object {
        private const val TAG = "VoiceAssistantEngine"
    }
}
