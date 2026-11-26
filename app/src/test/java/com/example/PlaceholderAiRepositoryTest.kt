package com.example

import com.example.data.ai.PlaceholderAiRepository
import com.example.data.local.AssistantLanguage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceholderAiRepositoryTest {

    private val repo = PlaceholderAiRepository()

    @Test
    fun `placeholder responds to math questions accurately and stays labeled`() = runTest {
        val reply = repo.generateReply(
            history = emptyList(),
            userInput = "25 + 17",
            language = AssistantLanguage.ENGLISH
        )
        assertTrue(reply.isPlaceholder)
        assertTrue(reply.text.contains("42"))
        assertTrue(reply.text.contains("Preview Mode"))
        assertNull(reply.picture)
    }

    @Test
    fun `normal conversation does not trigger picture attachment`() = runTest {
        val reply = repo.generateReply(
            history = emptyList(),
            userInput = "नमस्ते अव्यक्त कैसे हो",
            language = AssistantLanguage.AUTO
        )
        assertTrue(reply.isPlaceholder)
        assertTrue(reply.text.contains("नमस्ते"))
        assertNull(reply.picture)
    }
}
