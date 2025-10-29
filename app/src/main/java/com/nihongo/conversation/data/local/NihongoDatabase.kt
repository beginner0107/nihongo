package com.nihongo.conversation.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nihongo.conversation.domain.model.Conversation
import com.nihongo.conversation.domain.model.Message
import com.nihongo.conversation.domain.model.Scenario
import com.nihongo.conversation.domain.model.User

@Database(
    entities = [User::class, Scenario::class, Conversation::class, Message::class],
    version = 2,
    exportSchema = false
)
abstract class NihongoDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun scenarioDao(): ScenarioDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add isCompleted column to conversations table
                database.execSQL("ALTER TABLE conversations ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
