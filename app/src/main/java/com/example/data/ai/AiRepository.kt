package com.example.data.ai

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.local.AssistantLanguage
import com.example.data.local.ChatMessageEntity
import com.example.data.network.PictureSearchResult
import com.example.data.network.WikipediaPictureSearchClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

data class AiReply(
    val text: String,
    val isPlaceholder: Boolean = true,
    val picture: PictureSearchResult? = null,
    val cameraSnapshotNote: String? = null
)

/**
 * Single AI repository abstraction routing all text, vision, and picture-request operations.
 * Currently backed by [PlaceholderAiRepository] because Firebase AI Logic setup was declined.
 * Connect a FirebaseAiRepository implementation once Firebase is enabled in AI Studio.
 */
interface AiRepository {
    val isLiveGeminiConnected: Boolean

    suspend fun generateReply(
        history: List<ChatMessageEntity>,
        userInput: String,
        language: AssistantLanguage,
        cameraBitmap: Bitmap? = null
    ): AiReply
}

class PlaceholderAiRepository(
    private val pictureClient: WikipediaPictureSearchClient = WikipediaPictureSearchClient()
) : AiRepository {

    override val isLiveGeminiConnected: Boolean = false

    override suspend fun generateReply(
        history: List<ChatMessageEntity>,
        userInput: String,
        language: AssistantLanguage,
        cameraBitmap: Bitmap?
    ): AiReply = withContext(Dispatchers.Default) {
        // Brief realistic processing cadence for natural thinking-to-speaking transition
        delay(350L)

        val trimmed = userInput.trim()
        val preferHindi = shouldRespondInHindi(trimmed, language)

        // 1. Check if the user explicitly asked to display an existing picture
        val pictureQuery = extractExplicitPictureQuery(trimmed)
        if (pictureQuery != null) {
            val foundPicture = pictureClient.searchExistingPicture(pictureQuery)
            return@withContext if (foundPicture != null) {
                val replyText = if (preferHindi) {
                    "यह रही \"${foundPicture.title}\" की उपलब्ध तस्वीर (Wikipedia से)।\n\n[Preview Mode: लाइव Gemini AI उत्तर के लिए AI Studio में Firebase सक्षम करें]"
                } else {
                    "Here is an existing picture of \"${foundPicture.title}\" from Wikipedia.\n\n[Preview Mode: Enable Firebase in AI Studio to connect live Gemini AI]"
                }
                AiReply(
                    text = replyText,
                    isPlaceholder = true,
                    picture = foundPicture
                )
            } else {
                val replyText = if (preferHindi) {
                    "मुझे \"$pictureQuery\" की कोई सुरक्षित सार्वजनिक तस्वीर नहीं मिली। कृपया किसी अन्य वस्तु, जानवर या स्थान का नाम बताएं।\n\n[Preview Mode • Firebase AI Logic अक्षम है]"
                } else {
                    "I couldn't find a public reference picture for \"$pictureQuery\". Please check your internet connection or try a clearer object/place name.\n\n[Preview Mode • Firebase AI Logic not enabled]"
                }
                AiReply(text = replyText, isPlaceholder = true)
            }
        }

        // 2. Check if Camera Vision frame is attached or user asked about what camera sees
        if (cameraBitmap != null) {
            val sceneStats = summarizeCameraBitmap(cameraBitmap)
            val replyText = if (preferHindi) {
                "कैमरा प्रीव्यू सक्रिय है! मैं आपके कैमरे से अभी का फ्रेम देख पा रहा हूँ: ${sceneStats.hindiSummary}।\n\n[Preview Mode: वस्तुओं और दृश्यों की गहरी Gemini Vision पहचान के लिए AI Studio में Firebase AI Logic सक्षम करें]"
            } else {
                "Live Camera Vision is active! From your current camera frame, I detect ${sceneStats.englishSummary}.\n\n[Preview Mode: Deep object, text, and scene understanding requires enabling Firebase AI Logic in AI Studio]"
            }
            return@withContext AiReply(
                text = replyText,
                isPlaceholder = true,
                cameraSnapshotNote = sceneStats.compactBadge
            )
        }

        // 3. Simple safe arithmetic check for immediate educational/utility responsiveness
        val mathResult = tryEvaluateSimpleMath(trimmed)
        if (mathResult != null) {
            val replyText = if (preferHindi) {
                "इस गणना का उत्तर **$mathResult** है।\n\n[Preview Mode: जटिल गणित, विज्ञान और Gemini Live बातचीत के लिए Firebase AI Logic जोड़ें]"
            } else {
                "The answer to that calculation is **$mathResult**.\n\n[Preview Mode: Enable Firebase AI Logic in AI Studio for full Gemini reasoning]"
            }
            return@withContext AiReply(text = replyText, isPlaceholder = true)
        }

        // 4. General sample conversational placeholder response (clearly labeled)
        val contextCount = history.size
        val replyText = if (preferHindi) {
            buildHindiSampleReply(trimmed, contextCount)
        } else {
            buildEnglishSampleReply(trimmed, contextCount)
        }

        AiReply(
            text = replyText,
            isPlaceholder = true
        )
    }

    private fun shouldRespondInHindi(input: String, language: AssistantLanguage): Boolean {
        return when (language) {
            AssistantLanguage.HINDI -> true
            AssistantLanguage.ENGLISH -> false
            AssistantLanguage.AUTO -> {
                val hasDevanagari = input.any { it in '\u0900'..'\u097F' }
                if (hasDevanagari) return true
                val lower = input.lowercase(Locale.ROOT)
                val hindiMarkers = listOf(
                    "namaste", "kaise", "kya", "hai", "hain", "batao", "dikhao",
                    "samjhao", "hindi", "mera", "meri", "tum", "aap", "kyun", "kab"
                )
                hindiMarkers.any { marker -> lower.contains(marker) }
            }
        }
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

    private fun tryEvaluateSimpleMath(input: String): String? {
        val cleaned = input.lowercase(Locale.ROOT)
            .replace("what is", "")
            .replace("calculate", "")
            .replace("plus", "+")
            .replace("minus", "-")
            .replace("times", "*")
            .replace("x", "*")
            .replace("divided by", "/")
            .trim(' ', '?', '=')

        val regex = Regex("^(-?\\d+(?:\\.\\d+)?)\\s*([+\\-*/])\\s*(-?\\d+(?:\\.\\d+)?)$")
        val match = regex.matchEntire(cleaned) ?: return null
        val a = match.groupValues[1].toDoubleOrNull() ?: return null
        val op = match.groupValues[2]
        val b = match.groupValues[3].toDoubleOrNull() ?: return null
        val res = when (op) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b != 0.0) a / b else return null
            else -> return null
        }
        return if (res % 1.0 == 0.0) res.toLong().toString() else String.format(Locale.US, "%.2f", res)
    }

    private data class SceneSummary(
        val englishSummary: String,
        val hindiSummary: String,
        val compactBadge: String
    )

    private fun summarizeCameraBitmap(bitmap: Bitmap): SceneSummary {
        val stepX = (bitmap.width / 16).coerceAtLeast(1)
        val stepY = (bitmap.height / 16).coerceAtLeast(1)
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0

        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val px = bitmap.getPixel(x, y)
                rSum += Color.red(px)
                gSum += Color.green(px)
                bSum += Color.blue(px)
                count++
                x += stepX
            }
            y += stepY
        }

        val c = count.coerceAtLeast(1)
        val avgR = (rSum / c).toInt()
        val avgG = (gSum / c).toInt()
        val avgB = (bSum / c).toInt()
        val brightness = (0.299 * avgR + 0.587 * avgG + 0.114 * avgB).toInt()

        val lightingEn = when {
            brightness > 175 -> "brightly lit"
            brightness > 90 -> "balanced indoor/daylight"
            else -> "dim / low-light"
        }
        val lightingHi = when {
            brightness > 175 -> "तेज़ रोशनी वाला दृश्य"
            brightness > 90 -> "संतुलित रोशनी वाला दृश्य"
            else -> "कम रोशनी वाला दृश्य"
        }
        val toneEn = when {
            avgR > avgB + 25 -> "warm-toned surroundings"
            avgB > avgR + 25 -> "cool-toned surroundings"
            avgG > avgR + 15 && avgG > avgB + 15 -> "greenish surroundings"
            else -> "neutral-toned surroundings"
        }
        val toneHi = when {
            avgR > avgB + 25 -> "गर्म रंगों (warm tones) का वातावरण"
            avgB > avgR + 25 -> "नीले/ठंडे रंगों (cool tones) का वातावरण"
            avgG > avgR + 15 && avgG > avgB + 15 -> "हरे रंग का वातावरण"
            else -> "संतुलित रंगों का वातावरण"
        }

        return SceneSummary(
            englishSummary = "a $lightingEn scene (${bitmap.width}x${bitmap.height}) with $toneEn",
            hindiSummary = "$lightingHi (${bitmap.width}x${bitmap.height}) और $toneHi",
            compactBadge = "Frame ${bitmap.width}x${bitmap.height} • Luma $brightness"
        )
    }

    private fun buildHindiSampleReply(input: String, historyCount: Int): String {
        val lower = input.lowercase(Locale.ROOT)
        return when {
            lower.contains("namaste") || lower.contains("hello") || lower.contains("hi") || input.contains("नमस्ते") ->
                "नमस्ते! मैं AVYAKT (अव्यक्त) हूँ, आपका पर्सनल AI असिस्टेंट। अभी ऐप प्रीव्यू (Placeholder) मोड में चल रहा है क्योंकि Firebase सेटअप बंद है, लेकिन आप मेरी आवाज़, लिप-सिंक अवतार, लाइव कैमरा और चैट हिस्ट्री का पूरा अनुभव ले सकते हैं।"
            else ->
                "मैंने आपका संदेश सुना: \"$input\" (चैट संदर्भ: $historyCount संदेश)।\n\n[Sample Preview Response: असली Gemini AI ज्ञान, वेब सर्च और लाइव वॉइस बातचीत चालू करने के लिए AI Studio में Firebase AI Logic को स्वीकार (Enable) करें।]"
        }
    }

    private fun buildEnglishSampleReply(input: String, historyCount: Int): String {
        val lower = input.lowercase(Locale.ROOT)
        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("who are you") ->
                "Hello! I am AVYAKT, your personal AI assistant. I am currently running in Preview Mode because Firebase setup was declined, so AI replies use clearly labeled sample responses while voice, lip-sync avatar, camera vision, and local chat history work live."
            else ->
                "I received your message: \"$input\" (context: $historyCount saved messages).\n\n[Sample Preview Response: Live Gemini answers, web search grounding, and Gemini Live voice require enabling Firebase AI Logic in AI Studio.]"
        }
    }
}
