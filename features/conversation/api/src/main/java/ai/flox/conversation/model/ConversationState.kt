package ai.flox.conversation.model

import ai.flox.state.State

data class ConversationState(
    val recentConversationList: Map<String, Conversation> = mapOf(),
    val currentConversation: Conversation? = null
) : State {

    companion object {
        const val stateKey = "conversationState"
    }
}