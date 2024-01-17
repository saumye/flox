package ai.flox.storage.chat.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chatmessage")
data class ChatMessageEntity(
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "isUser") val isUser: Boolean,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
)