package ai.flox.storage

import ai.flox.storage.chat.ChatDAO
import ai.flox.storage.chat.model.ChatMessageEntity
import ai.flox.storage.conversation.ConversationDAO
import ai.flox.storage.conversation.model.ConversationEntity
import ai.flox.storage.news.NewsArticleEntity
import ai.flox.storage.news.NewsDAO
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.Date

@Database(entities = [ChatMessageEntity::class, ConversationEntity::class, NewsArticleEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDAO

    abstract fun conversationDAO(): ConversationDAO

    abstract fun newsDAO(): NewsDAO
}


object Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}