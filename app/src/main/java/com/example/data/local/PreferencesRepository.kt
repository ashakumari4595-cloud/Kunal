package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.avyaktDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "avyakt_user_preferences"
)

enum class AssistantLanguage(val label: String, val bcp47: String) {
    AUTO("Auto (हिन्दी + English)", "hi-IN"),
    HINDI("हिन्दी (Hindi)", "hi-IN"),
    ENGLISH("English", "en-IN")
}

enum class AvatarVisualMode(val label: String) {
    PHOTO_PORTRAIT("My Photo"),
    ANIMATED_CHARACTER("AI Character")
}

data class UserSettings(
    val customPortraitPath: String? = null,
    val avatarVisualMode: AvatarVisualMode = AvatarVisualMode.PHOTO_PORTRAIT,
    val language: AssistantLanguage = AssistantLanguage.AUTO,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val autoSpeakReplies: Boolean = true,
    val activeSessionId: Long = -1L
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val CUSTOM_PORTRAIT_PATH = stringPreferencesKey("custom_portrait_path")
        val AVATAR_VISUAL_MODE = stringPreferencesKey("avatar_visual_mode")
        val LANGUAGE = stringPreferencesKey("assistant_language")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak_replies")
        val ACTIVE_SESSION_ID = longPreferencesKey("active_session_id")
    }

    val settingsFlow: Flow<UserSettings> = context.avyaktDataStore.data.map { prefs ->
        val langName = prefs[Keys.LANGUAGE] ?: AssistantLanguage.AUTO.name
        val language = AssistantLanguage.entries.firstOrNull { it.name == langName }
            ?: AssistantLanguage.AUTO
        val modeName = prefs[Keys.AVATAR_VISUAL_MODE] ?: AvatarVisualMode.PHOTO_PORTRAIT.name
        val avatarMode = AvatarVisualMode.entries.firstOrNull { it.name == modeName }
            ?: AvatarVisualMode.PHOTO_PORTRAIT
        UserSettings(
            customPortraitPath = prefs[Keys.CUSTOM_PORTRAIT_PATH]?.takeIf { it.isNotBlank() },
            avatarVisualMode = avatarMode,
            language = language,
            speechRate = (prefs[Keys.SPEECH_RATE] ?: 1.0f).coerceIn(0.6f, 1.5f),
            speechPitch = (prefs[Keys.SPEECH_PITCH] ?: 1.0f).coerceIn(0.7f, 1.4f),
            autoSpeakReplies = prefs[Keys.AUTO_SPEAK] ?: true,
            activeSessionId = prefs[Keys.ACTIVE_SESSION_ID] ?: -1L
        )
    }

    suspend fun setCustomPortraitPath(path: String?) {
        context.avyaktDataStore.edit { prefs ->
            if (path.isNullOrBlank()) {
                prefs.remove(Keys.CUSTOM_PORTRAIT_PATH)
            } else {
                prefs[Keys.CUSTOM_PORTRAIT_PATH] = path
            }
        }
    }

    suspend fun setAvatarVisualMode(mode: AvatarVisualMode) {
        context.avyaktDataStore.edit { prefs ->
            prefs[Keys.AVATAR_VISUAL_MODE] = mode.name
        }
    }

    suspend fun setLanguage(language: AssistantLanguage) {
        context.avyaktDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.name
        }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.avyaktDataStore.edit { prefs ->
            prefs[Keys.SPEECH_RATE] = rate.coerceIn(0.6f, 1.5f)
        }
    }

    suspend fun setSpeechPitch(pitch: Float) {
        context.avyaktDataStore.edit { prefs ->
            prefs[Keys.SPEECH_PITCH] = pitch.coerceIn(0.7f, 1.4f)
        }
    }

    suspend fun setAutoSpeakReplies(enabled: Boolean) {
        context.avyaktDataStore.edit { prefs ->
            prefs[Keys.AUTO_SPEAK] = enabled
        }
    }

    suspend fun setActiveSessionId(sessionId: Long) {
        context.avyaktDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_SESSION_ID] = sessionId
        }
    }
}
