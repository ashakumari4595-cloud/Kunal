package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.local.AssistantLanguage
import com.example.data.local.ChatMessageEntity
import com.example.data.network.WikipediaPictureSearchClient
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.util.Locale

class FirebaseAiRepository(
    private val pictureClient: WikipediaPictureSearchClient = WikipediaPictureSearchClient()
) : AiRepository {

    override val isLiveGeminiConnected: Boolean = true

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content {
                text(
                    "You are AVYAKT (अव्यक्त), a warm, intelligent, personal voice and vision AI assistant. " +
                        "Respond naturally in Hindi (Devanagari) when the user speaks or writes in Hindi or Hinglish, " +
                        "and in clear English when English is used. Keep answers accurate, concise, and conversational " +
                        "so they sound natural when spoken aloud by the user's personal avatar."
                )
            }
        )
    }

    override suspend fun generateReply(
        history: List<ChatMessageEntity>,
        userInput: String,
        language: AssistantLanguage,
        cameraBitmap: Bitmap?
    ): AiReply {
        val trimmed = userInput.trim()

        // 1. Honor explicit requests for an existing real-world reference picture via Wikipedia
        val pictureQuery = extractExplicitPictureQuery(trimmed)
        if (pictureQuery != null) {
            val foundPicture = pictureClient.searchExistingPicture(pictureQuery)
            if (foundPicture != null) {
                val summaryPrompt = buildLanguageAwarePrompt(
                    userInput = "Introduce \"${foundPicture.title}\" in 2 short sentences based on: ${foundPicture.caption}",
                    language = language
                )
                val captionIntro = runCatching {
                    withTimeout(AI_TIMEOUT_MS) {
                        model.generateContent(summaryPrompt).text?.trim()
                    }
                }.onFailure { err ->
                    if (err is CancellationException) throw err
                    Log.w(TAG, "Optional Gemini caption summary failed; using Wikipedia summary", err)
                }.getOrNull()

                val fallbackText = if (shouldPreferHindi(trimmed, language)) {
                    "यह रही \"${foundPicture.title}\" की तस्वीर (Wikipedia से)।"
                } else {
                    "Here is a picture of \"${foundPicture.title}\" from Wikipedia."
                }

                return AiReply(
                    text = captionIntro?.takeIf { it.isNotBlank() } ?: fallbackText,
                    isPlaceholder = false,
                    picture = foundPicture
                )
            }
        }

        // 2. Multimodal Camera Vision turn
        if (cameraBitmap != null) {
            val preparedBitmap = downscaleIfNeeded(cameraBitmap, maxDimension = 1280)
            val visionPrompt = buildLanguageAwarePrompt(trimmed, language)
            return try {
                val response = withTimeout(AI_TIMEOUT_MS) {
                    model.generateContent(
                        content {
                            image(preparedBitmap)
                            text(visionPrompt)
                        }
                    )
                }
                val replyText = response.text?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Gemini Vision returned an empty response.")

                AiReply(
                    text = replyText,
                    isPlaceholder = false,
                    cameraSnapshotNote = "Gemini Vision • ${preparedBitmap.width}x${preparedBitmap.height}"
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini Vision timed out", e)
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini Vision generation failed", e)
                throw e
            } finally {
                if (preparedBitmap !== cameraBitmap && !preparedBitmap.isRecycled) {
                    preparedBitmap.recycle()
                }
            }
        }

        // 3. Multi-turn conversational text/voice generation via Gemini
        val recentHistory = mapHistoryForGemini(history)
        val prompt = buildLanguageAwarePrompt(trimmed, language)
        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                if (recentHistory.isEmpty()) {
                    model.generateContent(prompt)
                } else {
                    val chat = model.startChat(history = recentHistory)
                    chat.sendMessage(prompt)
                }
            }
            val replyText = response.text?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Gemini returned an empty response.")

            AiReply(
                text = replyText,
                isPlaceholder = false
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini chat generation timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini chat generation failed", e)
            throw e
        }
    }

    private fun mapHistoryForGemini(history: List<ChatMessageEntity>): List<Content> {
        val validMessages = history.filter { it.text.isNotBlank() }
        val pairedHistory = mutableListOf<Content>()
        var pendingUserMsg: ChatMessageEntity? = null

        for (msg in validMessages) {
            if (msg.isFromUser) {
                // Keep the latest user message in case an earlier user message never got a model reply
                pendingUserMsg = msg
            } else {
                val userMessage = pendingUserMsg
                if (userMessage != null) {
                    pairedHistory.add(content("user") { text(userMessage.text) })
                    pairedHistory.add(content("model") { text(msg.text) })
                    pendingUserMsg = null
                }
            }
        }

        // Ensure even count (user -> model pairs) and cap to MAX_HISTORY_TURNS
        return if (pairedHistory.size > MAX_HISTORY_TURNS) {
            val trimmed = pairedHistory.takeLast(MAX_HISTORY_TURNS)
            if (trimmed.firstOrNull()?.role == "model") trimmed.drop(1) else trimmed
        } else {
            pairedHistory
        }
    }

    private fun buildLanguageAwarePrompt(
        userInput: String,
        language: AssistantLanguage
    ): String {
        return when (language) {
            AssistantLanguage.HINDI -> "$userInput\n\n(कृपया प्राकृतिक हिन्दी में संक्षिप्त और स्पष्ट उत्तर दें।)"
            AssistantLanguage.ENGLISH -> "$userInput\n\n(Please respond concisely in natural English.)"
            AssistantLanguage.AUTO -> userInput
        }
    }

    private fun shouldPreferHindi(input: String, language: AssistantLanguage): Boolean {
        return when (language) {
            AssistantLanguage.HINDI -> true
            AssistantLanguage.ENGLISH -> false
            AssistantLanguage.AUTO -> input.any { it in '\u0900'..'\u097F' } ||
                listOf("namaste", "kaise", "kya", "dikhao", "batao", "tasveer")
                    .any { input.lowercase(Locale.ROOT).contains(it) }
        }
    }

    private fun downscaleIfNeeded(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width.coerceAtLeast(1)
        val height = source.height.coerceAtLeast(1)
        val longest = maxOf(width, height)
        if (longest <= maxDimension) return source

        val scale = maxDimension.toFloat() / longest.toFloat()
        val targetW = (width * scale).toInt().coerceAtLeast(1)
        val targetH = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    private fun extractExplicitPictureQuery(input: String): String? {
        val lower = input.lowercase(Locale.ROOT)
        val englishTriggers = listOf(
            "show me a picture of ",
            "show me picture of ",
            "show picture of ",
            "show a picture of ",
            "show me a photo of ",
            "show photo of ",
            "show image of ",
            "show me an image of ",
            "picture of ",
            "photo of "
        )
        for (trigger in englishTriggers) {
            val idx = lower.indexOf(trigger)
            if (idx >= 0) {
                return input.substring(idx + trigger.length)
                    .trim(' ', '.', '?', '!', '"', '\'')
                    .takeIf { it.isNotBlank() }
            }
        }

        val hindiSuffixes = listOf(
            "की तस्वीर दिखाओ",
            "का फोटो दिखाओ",
            "की फोटो दिखाओ",
            "का चित्र दिखाओ",
            "tasveer dikhao",
            "photo dikhao",
            "pic dikhao",
            "image dikhao"
        )
        for (suffix in hindiSuffixes) {
            val idx = lower.indexOf(suffix)
            if (idx > 0) {
                return input.substring(0, idx)
                    .replace("मुझे", "")
                    .replace("mujhe", "", ignoreCase = true)
                    .replace("ek", "", ignoreCase = true)
                    .replace("ka", "", ignoreCase = true)
                    .replace("ki", "", ignoreCase = true)
                    .trim(' ', '.', '?', '!')
                    .takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    companion object {
        private const val TAG = "FirebaseAiRepository"
        private const val MODEL_NAME = "gemini-3.8-flash"
        private const val AI_TIMEOUT_MS = 60_000L
        private const val MAX_HISTORY_TURNS = 16
    }
}
