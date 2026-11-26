package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationSessionEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AvyaktDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AvyaktDatabase? = null

        fun getInstance(context: Context): AvyaktDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AvyaktDatabase::class.java,
                    "avyakt_assistant.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
