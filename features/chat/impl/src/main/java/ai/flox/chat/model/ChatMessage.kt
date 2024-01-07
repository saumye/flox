package ai.flox.chat.model

import java.util.UUID

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val id: String = UUID.randomUUID().toString(),
    val messageState: SyncStatus = SyncStatus.SYNC_NEEDED
)