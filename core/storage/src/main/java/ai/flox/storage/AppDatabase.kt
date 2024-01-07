package ai.flox.storage

import ai.flox.storage.chat.ChatDAO
import ai.flox.storage.chat.model.ChatMessageEntity
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ChatMessageEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDAO
}