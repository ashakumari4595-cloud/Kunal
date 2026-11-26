package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class FirestoreConversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val lastMessagePreview: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    companion object {
        fun createPayload(
            userId: String,
            title: String,
            lastMessagePreview: String = "",
        ): Map<String, Any> = mapOf(
            "userId" to userId,
            "title" to title,
            "lastMessagePreview" to lastMessagePreview,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
    }
}

@Keep
@IgnoreExtraProperties
data class FirestoreMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val conversationId: String = "",
    val fromUser: Boolean = false,
    val text: String = "",
    val placeholderResponse: Boolean = false,
    val pictureUrl: String? = null,
    val pictureCaption: String? = null,
    val pictureSourceUrl: String? = null,
    val cameraSnapshotNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    companion object {
        fun createPayload(
            userId: String,
            conversationId: String,
            fromUser: Boolean,
            text: String,
            placeholderResponse: Boolean = false,
            pictureUrl: String? = null,
            pictureCaption: String? = null,
            pictureSourceUrl: String? = null,
            cameraSnapshotNote: String? = null,
        ): Map<String, Any> {
            val rawMap = mapOf<String, Any?>(
                "userId" to userId,
                "conversationId" to conversationId,
                "fromUser" to fromUser,
                "text" to text,
                "placeholderResponse" to placeholderResponse,
                "pictureUrl" to pictureUrl?.takeIf { it.isNotBlank() },
                "pictureCaption" to pictureCaption?.takeIf { it.isNotBlank() },
                "pictureSourceUrl" to pictureSourceUrl?.takeIf { it.isNotBlank() },
                "cameraSnapshotNote" to cameraSnapshotNote?.takeIf { it.isNotBlank() },
                "createdAt" to FieldValue.serverTimestamp(),
            )
            @Suppress("UNCHECKED_CAST")
            return rawMap.filterValues { it != null } as Map<String, Any>
        }
    }
}
