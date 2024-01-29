package ai.flox.storage.conversation.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversation")
data class ConversationEntity(
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "lastMessageTime") val lastMessageTime: Long,
    @PrimaryKey @ColumnInfo(name = "id", index = true) val id: String,
)