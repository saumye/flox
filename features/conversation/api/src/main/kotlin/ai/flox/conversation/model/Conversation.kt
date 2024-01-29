package ai.flox.conversation.model

import java.util.Date
import java.util.UUID

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val lastMessageTime: Date = Date(System.currentTimeMillis())
)
