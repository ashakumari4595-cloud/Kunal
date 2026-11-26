package com.example

import com.example.data.remote.FirestoreConversation
import com.example.data.remote.FirestoreMessage
import com.google.firebase.firestore.FieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreChatModelsTest {

    @Test
    fun conversationDefaults_supportNoArgConstruction() {
        val conversation = FirestoreConversation()
        assertEquals("", conversation.id)
        assertEquals("", conversation.userId)
        assertEquals("", conversation.title)
        assertEquals("", conversation.lastMessagePreview)
        assertNull(conversation.createdAt)
        assertNull(conversation.updatedAt)
    }

    @Test
    fun conversationPayload_excludesDocumentIdAndUsesServerTimestamps() {
        val payload = FirestoreConversation.createPayload(
            userId = "user_123",
            title = "Morning Check-in",
            lastMessagePreview = "Namaste!",
        )
        assertFalse(payload.containsKey("id"))
        assertEquals("user_123", payload["userId"])
        assertEquals("Morning Check-in", payload["title"])
        assertEquals("Namaste!", payload["lastMessagePreview"])
        assertEquals(FieldValue.serverTimestamp(), payload["createdAt"])
        assertEquals(FieldValue.serverTimestamp(), payload["updatedAt"])
    }

    @Test
    fun messagePayload_filtersNullAndBlankOptionalFields() {
        val payload = FirestoreMessage.createPayload(
            userId = "user_123",
            conversationId = "conv_456",
            fromUser = true,
            text = "Hello Avyakt",
            placeholderResponse = false,
            pictureUrl = null,
            pictureCaption = "   ",
            pictureSourceUrl = "https://en.wikipedia.org/wiki/India",
            cameraSnapshotNote = "",
        )
        assertFalse(payload.containsKey("id"))
        assertEquals("user_123", payload["userId"])
        assertEquals("conv_456", payload["conversationId"])
        assertEquals(true, payload["fromUser"])
        assertEquals("Hello Avyakt", payload["text"])
        assertEquals(false, payload["placeholderResponse"])
        assertFalse(payload.containsKey("pictureUrl"))
        assertFalse(payload.containsKey("pictureCaption"))
        assertTrue(payload.containsKey("pictureSourceUrl"))
        assertFalse(payload.containsKey("cameraSnapshotNote"))
        assertEquals(FieldValue.serverTimestamp(), payload["createdAt"])
    }
}
