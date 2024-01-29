package ai.flox.storage.chat.model

import ai.flox.storage.conversation.model.ConversationEntity
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chatmessage", foreignKeys = [ForeignKey(
        entity = ConversationEntity::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("conversationId"),
        onDelete = ForeignKey.CASCADE
    )], indices = [Index(value = arrayOf("id", "conversationId"), unique = true)]
)
data class ChatMessageEntity(
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "userId") val userId: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "conversationId", index = true) val conversationId: String,
    @PrimaryKey @ColumnInfo(name = "id", index = true) val id: String,
)