package ai.flox.chat.model

data class Conversation(
    val id: String,
    val title: String,
    val messageIds: List<String>
)