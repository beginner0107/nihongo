package com.nihongo.conversation.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nihongo.conversation.domain.model.Conversation
import com.nihongo.conversation.domain.model.Message
import com.nihongo.conversation.domain.model.Scenario
import com.nihongo.conversation.domain.model.User

@Database(
    entities = [User::class, Scenario::class, Conversation::class, Message::class],
    version = 1,
    exportSchema = false
)
abstract class NihongoDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun scenarioDao(): ScenarioDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
