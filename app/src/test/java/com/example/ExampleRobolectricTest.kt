package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app name matches AVYAKT AI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("AVYAKT AI", context.getString(R.string.app_name))
    }

    @Test
    fun `startup renders avatar stage controls and navigation`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("AVYAKT AI").assertIsDisplayed()
        composeTestRule.onNodeWithTag("preview_mode_notice").assertDoesNotExist()
        composeTestRule.onNodeWithTag("main_mic_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camera_toggle_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_text_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("send_message_button").assertIsDisplayed()
    }

    @Test
    fun `navigate to history and settings screens cleanly`() {
        composeTestRule.waitForIdle()

        // Navigate to History
        composeTestRule.onNodeWithTag("history_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Conversation History").assertIsDisplayed()
        composeTestRule.onNodeWithTag("history_back_button").performClick()
        composeTestRule.waitForIdle()

        // Navigate to Settings
        composeTestRule.onNodeWithTag("settings_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Personal AI Avatar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("change_portrait_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_back_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("AVYAKT AI").assertIsDisplayed()
    }

    @Test
    fun `settings displays live firebase ai logic status and google sign in option`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("google_sign_in_button").assertExists()
        composeTestRule
            .onAllNodes(androidx.compose.ui.test.hasText("Firebase AI Logic Connected", substring = true))
            .fetchSemanticsNodes()
            .let { assert(it.isNotEmpty()) }
    }

    @Test
    fun `avatar mode toggle switches between photo portrait and animated character`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("avatar_mode_toggle").assertIsDisplayed()
        composeTestRule.onNodeWithTag("avatar_mode_toggle").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule
                .onAllNodes(androidx.compose.ui.test.hasText("AI Character", substring = true))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("AI Character").assertIsDisplayed()
    }
}
