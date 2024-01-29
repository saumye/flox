package ai.flox.chat.model

import ai.flox.conversation.model.Conversation
import java.util.Date
import java.util.UUID

data class ChatMessage(
    val message: String,
    val userId: String,
    val conversation: Conversation,
    val timestamp: Date,
    val id: String = UUID.randomUUID().toString(),
    val messageState: SyncStatus = SyncStatus.SYNC_NEEDED
) {
    fun isSelf()= userId.equals("self",ignoreCase = true)
}

