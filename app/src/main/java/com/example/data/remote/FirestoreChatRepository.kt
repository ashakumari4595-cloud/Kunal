package com.example.data.remote

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreChatRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {

    fun observeConversations(userId: String): Flow<List<FirestoreConversation>> {
        val path = "$USERS_COLLECTION/$userId/$CONVERSATIONS_COLLECTION"
        return db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(CONVERSATIONS_COLLECTION)
            .orderBy(FIELD_UPDATED_AT, Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreConversation::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                handleFirestoreError(rootError, OperationType.LIST, path, auth)
                throw rootError
            }
    }

    fun observeMessages(
        userId: String,
        conversationId: String,
    ): Flow<List<FirestoreMessage>> {
        val path = "$USERS_COLLECTION/$userId/$CONVERSATIONS_COLLECTION/$conversationId/$MESSAGES_COLLECTION"
        return db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_COLLECTION)
            .orderBy(FIELD_CREATED_AT, Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                handleFirestoreError(rootError, OperationType.LIST, path, auth)
                throw rootError
            }
    }

    suspend fun createConversation(
        title: String,
        initialPreview: String = "",
    ): Result<String> {
        var targetPath = "$USERS_COLLECTION/$CONVERSATIONS_COLLECTION"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().ifBlank { DEFAULT_CONVERSATION_TITLE }.take(MAX_TITLE_LENGTH)
            val cleanPreview = initialPreview.trim().take(MAX_PREVIEW_LENGTH)

            val docRef = db.collection(USERS_COLLECTION)
                .document(uid)
                .collection(CONVERSATIONS_COLLECTION)
                .document()
            targetPath = docRef.path

            val payload = FirestoreConversation.createPayload(
                userId = uid,
                title = cleanTitle,
                lastMessagePreview = cleanPreview,
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, targetPath, auth)
            Result.failure(e)
        }
    }

    suspend fun addMessage(
        conversationId: String,
        fromUser: Boolean,
        text: String,
        placeholderResponse: Boolean = false,
        pictureUrl: String? = null,
        pictureCaption: String? = null,
        pictureSourceUrl: String? = null,
        cameraSnapshotNote: String? = null,
    ): Result<String> {
        var targetPath = "$USERS_COLLECTION/$CONVERSATIONS_COLLECTION/$conversationId/$MESSAGES_COLLECTION"
        return try {
            val uid = requireUserId()
            require(conversationId.isNotBlank()) { "Conversation ID must not be blank." }
            val trimmedText = text.trim()
            require(trimmedText.isNotEmpty()) { "Message text must not be blank." }

            val convRef = db.collection(USERS_COLLECTION)
                .document(uid)
                .collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
            val msgRef = convRef.collection(MESSAGES_COLLECTION).document()
            targetPath = msgRef.path

            val messagePayload = FirestoreMessage.createPayload(
                userId = uid,
                conversationId = conversationId,
                fromUser = fromUser,
                text = trimmedText,
                placeholderResponse = placeholderResponse,
                pictureUrl = pictureUrl,
                pictureCaption = pictureCaption,
                pictureSourceUrl = pictureSourceUrl,
                cameraSnapshotNote = cameraSnapshotNote,
            )
            val conversationUpdates = mapOf<String, Any>(
                FIELD_LAST_MESSAGE_PREVIEW to trimmedText.take(MAX_PREVIEW_LENGTH),
                FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
            )

            db.batch().apply {
                set(msgRef, messagePayload)
                update(convRef, conversationUpdates)
            }.commit().await()

            Result.success(msgRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, targetPath, auth)
            Result.failure(e)
        }
    }

    suspend fun updateConversationTitle(
        conversationId: String,
        newTitle: String,
    ): Result<Unit> {
        var targetPath = "$USERS_COLLECTION/$CONVERSATIONS_COLLECTION/$conversationId"
        return try {
            val uid = requireUserId()
            require(conversationId.isNotBlank()) { "Conversation ID must not be blank." }
            val cleanTitle = newTitle.trim().take(MAX_TITLE_LENGTH)
            require(cleanTitle.isNotEmpty()) { "Conversation title must not be blank." }

            val convRef = db.collection(USERS_COLLECTION)
                .document(uid)
                .collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
            targetPath = convRef.path

            convRef.update(
                mapOf(
                    FIELD_TITLE to cleanTitle,
                    FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, targetPath, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(conversationId: String): Result<Unit> {
        var targetPath = "$USERS_COLLECTION/$CONVERSATIONS_COLLECTION/$conversationId"
        return try {
            val uid = requireUserId()
            require(conversationId.isNotBlank()) { "Conversation ID must not be blank." }

            val convRef = db.collection(USERS_COLLECTION)
                .document(uid)
                .collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
            targetPath = convRef.path

            val messagesSnapshot = convRef.collection(MESSAGES_COLLECTION).get().await()
            val messageDocs = messagesSnapshot.documents
            messageDocs.chunked(BATCH_LIMIT).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }

            convRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, targetPath, auth)
            Result.failure(e)
        }
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: error("User must be signed in with Google before accessing Firestore.")
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val CONVERSATIONS_COLLECTION = "conversations"
        private const val MESSAGES_COLLECTION = "messages"
        private const val FIELD_TITLE = "title"
        private const val FIELD_LAST_MESSAGE_PREVIEW = "lastMessagePreview"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val DEFAULT_CONVERSATION_TITLE = "New Conversation"
        private const val MAX_TITLE_LENGTH = 120
        private const val MAX_PREVIEW_LENGTH = 160
        private const val BATCH_LIMIT = 450

        fun create(
            databaseId: String,
            auth: FirebaseAuth = Firebase.auth,
        ): FirestoreChatRepository {
            require(databaseId.isNotBlank()) {
                "Firestore database ID must not be blank. Provision Firebase first."
            }
            return FirestoreChatRepository(
                db = FirebaseFirestore.getInstance(databaseId),
                auth = auth,
            )
        }
    }
}
