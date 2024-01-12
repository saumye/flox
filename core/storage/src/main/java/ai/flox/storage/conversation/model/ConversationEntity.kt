package ai.flox.storage.conversation.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation")
data class ConversationEntity(
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "lastMessage") val lastMessageTime: Long,
    @PrimaryKey @ColumnInfo(name = "id") val id: String,

)