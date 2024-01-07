package ai.flox.chat.model

import ai.flox.state.State

data class ChatState(
    val recentChatList: Map<String, ChatMessage>? = null,
    val composeState: ComposeState = ComposeState.LoadComplete()
) : State {

    sealed interface ComposeState {
        val userInput: String
        data class Loading(override val userInput: String = "") : ComposeState
        data class LoadComplete(override val userInput: String = "") : ComposeState
    }

    companion object {
        const val stateKey = "chatState"
    }
}