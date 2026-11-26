package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAtMillis DESC")
    fun observeAllConversations(): Flow<List<ConversationSessionEntity>>

    @Query("SELECT * FROM conversations WHERE id = :sessionId LIMIT 1")
    suspend fun getConversationById(sessionId: Long): ConversationSessionEntity?

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestampMillis ASC, id ASC")
    fun observeMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestampMillis ASC, id ASC")
    suspend fun getMessagesForSessionOnce(sessionId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(session: ConversationSessionEntity): Long

    @Query("UPDATE conversations SET title = :title, lastMessagePreview = :preview, updatedAtMillis = :updatedAt WHERE id = :sessionId")
    suspend fun updateConversationSummary(
        sessionId: Long,
        title: String,
        preview: String,
        updatedAt: Long
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM conversations WHERE id = :sessionId")
    suspend fun deleteConversationById(sessionId: Long)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}
