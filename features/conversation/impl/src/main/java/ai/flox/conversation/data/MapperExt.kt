package ai.flox.conversation.data

import ai.flox.conversation.model.Conversation
import ai.flox.storage.conversation.model.ConversationEntity
import java.util.Date

fun ConversationEntity.toDomain(): ai.flox.conversation.model.Conversation {
    return ai.flox.conversation.model.Conversation(
        title = this.title,
        lastMessageTime = Date(this.lastMessageTime),
        id = this.id
    )
}

fun ai.flox.conversation.model.Conversation.toLocal(): ConversationEntity {
    return ConversationEntity(title = this.title, lastMessageTime = this.lastMessageTime.time, id = this.id)
}
