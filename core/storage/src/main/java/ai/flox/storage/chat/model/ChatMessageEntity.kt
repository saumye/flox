package ai.flox.storage.chat.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chatmessage")
data class ChatMessageEntity(
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "isUser") val isUser: Boolean,
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
)