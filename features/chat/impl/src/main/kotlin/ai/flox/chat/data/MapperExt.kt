package ai.flox.chat.data

import ai.flox.chat.model.ChatMessage
import ai.flox.conversation.model.Conversation
import ai.flox.network.openai.models.CompletionsResponse
import ai.flox.storage.chat.model.ChatMessageEntity
import java.util.Date

fun CompletionsResponse.toDomain(timestamp: Date, conversation: Conversation): ChatMessage {
    return ChatMessage(message = choices[0].message.content, timestamp = timestamp, userId = "AI", conversation = conversation)
}

fun ChatMessageEntity.toDomain(conversation: Conversation): ChatMessage {
    return ChatMessage(message = this.message, userId = this.userId, timestamp = Date(timestamp), id = id, conversation = conversation)
}

fun ChatMessage.toLocal(conversation: Conversation): ChatMessageEntity {
    return ChatMessageEntity(message = this.message, userId = this.userId, id = this.id, timestamp = timestamp.time, conversationId = conversation.id)
}
