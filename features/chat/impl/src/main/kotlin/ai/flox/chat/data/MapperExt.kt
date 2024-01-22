package ai.flox.chat.data

import ai.flox.chat.model.ChatMessage
import ai.flox.network.openai.models.CompletionsResponse
import ai.flox.storage.chat.model.ChatMessageEntity
import java.util.Date

fun CompletionsResponse.toDomain(timestamp: Date): ChatMessage {
    return ChatMessage(message = choices[0].message.content, timestamp = timestamp, isUser = false)
}

fun ChatMessageEntity.toDomain(): ChatMessage {
    return ChatMessage(message = this.message, isUser = this.isUser, timestamp = Date(timestamp), id = id)
}

fun ChatMessage.toLocal(): ChatMessageEntity {
    return ChatMessageEntity(message = this.message, isUser = this.isUser, id = this.id, timestamp = timestamp.time)
}
